/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, strlenwrite to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp.hdf5;

import ucar.ma2.*;

import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.iosp.Indexer;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.iosp.RegularSectionLayout;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.nc2.Group;

import java.io.IOException;
import java.nio.*;
import java.util.Date;

/**
 * HDF5 I/O
 *
 * @author caron
 */

public class H5iosp extends AbstractIOServiceProvider {
  static boolean debug = false;
  static boolean debugPos = false;
  static boolean debugHeap = false;
  static boolean debugFilter = false;
  static boolean debugFilterDetails = false;
  static boolean debugString = false;
  static boolean debugFilterIndexer = false;
  static boolean debugChunkIndexer = false;
  static boolean debugVlen = false;

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5iosp.class);

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debug = debugFlag.isSet("H5iosp/read");
    debugPos = debugFlag.isSet("H5iosp/filePos");
    debugHeap = debugFlag.isSet("H5iosp/Heap");
    debugFilter = debugFlag.isSet("H5iosp/filter");
    debugFilterIndexer = debugFlag.isSet("H5iosp/filterIndexer");
    debugChunkIndexer = debugFlag.isSet("H5iosp/chunkIndexer");
    debugVlen = debugFlag.isSet("H5iosp/vlen");

    H5header.setDebugFlags(debugFlag);
  }


  static public void setDebugOutputStream(java.io.PrintStream printStream) {
    H5header.debugOut = printStream;
  }

  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return H5header.isValidFile(raf);
  }

  //////////////////////////////////////////////////////////////////////////////////

  private NetcdfFile ncfile;
  private RandomAccessFile myRaf;
  private H5header headerParser;

  private boolean showBytes = false;
  private boolean showHeaderBytes = false;

  public void setSpecial(Object special) {
  }

  /////////////////////////////////////////////////////////////////////////////
  // reading

  public void open(RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
          ucar.nc2.util.CancelTask cancelTask) throws IOException {

    this.ncfile = ncfile;
    this.myRaf = raf;

    headerParser = new H5header(myRaf, ncfile, this);
    headerParser.read();

    Group root = ncfile.getRootGroup();
    Group eos = root.findGroup("HDFEOS_INFORMATION");
    if (null != eos)
      H5eos.parse(ncfile);

    ncfile.finish();
  }

  public Array readData(ucar.nc2.Variable v2, Section section) throws IOException, InvalidRangeException {
    H5header.Vinfo vinfo = (H5header.Vinfo) v2.getSPobject();
    return readData(v2, vinfo.dataPos, section);
  }

  // all the work is here, so can be called recursively
  private Array readData(ucar.nc2.Variable v2, long dataPos, Section wantSection) throws IOException, InvalidRangeException {
    H5header.Vinfo vinfo = (H5header.Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();
    Object data;

    //if (v2.getShortName().equals("cr"))
    //  System.out.println("hey");

    if (vinfo.useFillValue) { // fill value only
      Object pa = fillArray((int) wantSection.computeSize(), dataType, vinfo.getFillValue());
      return Array.factory(dataType.getPrimitiveClassType(), wantSection.getShape(), pa);
    }

    if (vinfo.mfp != null) { // filtered
      if (debugFilter) H5header.debugOut.println("read variable filtered " + v2.getName() + " vinfo = " + vinfo);
      assert vinfo.isChunked;

      H5chunkFilterLayout index = new H5chunkFilterLayout(v2, wantSection, myRaf, vinfo.mfp.getFilters());
      data = readFilteredData(v2, index, vinfo.getFillValue());

    } else { // normal case
      if (debug) H5header.debugOut.println("read variable " + v2.getName() + " vinfo = " + vinfo);

      DataType readDtype = v2.getDataType();
      int elemSize = v2.getElementSize();
      Object fillValue = vinfo.getFillValue();
      int byteOrder = vinfo.typeInfo.byteOrder;

      if (vinfo.typeInfo.hdfType == 2) { // time
        readDtype = vinfo.mdt.timeType;
        elemSize = readDtype.getSize();
        fillValue = vinfo.getFillValueDefault(readDtype);

      } else if (vinfo.typeInfo.hdfType == 8) { // enum
        H5header.TypeInfo baseInfo = vinfo.typeInfo.base;
        readDtype = baseInfo.dataType;
        elemSize = readDtype.getSize();
        fillValue = vinfo.getFillValueDefault(readDtype);
        byteOrder = baseInfo.byteOrder;

      } else if (vinfo.typeInfo.hdfType == 9) { // vlen
        elemSize = vinfo.typeInfo.byteSize;
        byteOrder = vinfo.typeInfo.byteOrder;
      }

      Indexer index;
      if (vinfo.isChunked) {
        index = new H5chunkLayout(v2, readDtype, wantSection);
      } else {
        index = RegularSectionLayout.factory(dataPos, elemSize, new Section(v2.getShape()), wantSection);
      }

      data = readData(vinfo.typeInfo, v2, index, readDtype, wantSection.getShape(), fillValue, byteOrder);
    }

    if (data instanceof Array)
      return (Array) data;
    else
      return Array.factory(dataType.getPrimitiveClassType(), wantSection.getShape(), data);
  }

  /**
   * Read data subset from file for a variable, create primitive array.
   *
   * @param v         the variable to read.
   * @param index     handles skipping around in the file.
   * @param dataType  dataType of the data to read
   * @param shape     the shape of the output
   * @param fillValue fill value as a wrapped primitive
   * @return primitive array or Array with data read in
   * @throws java.io.IOException            if read error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   */
  private Object readData(H5header.TypeInfo typeInfo, Variable v, Indexer index, DataType dataType, int[] shape,
          Object fillValue, int byteOrder) throws java.io.IOException, InvalidRangeException {

    // special processing
    if (typeInfo.hdfType == 2) { // time
      Object data = readDataPrimitive(index, dataType, fillValue, byteOrder);
      Array timeArray = Array.factory(dataType.getPrimitiveClassType(), shape, data);

      // now transform into an ISO Date String
      String[] stringData = new String[(int) timeArray.getSize()];
      int count = 0;
      IndexIterator ii = timeArray.getIndexIterator();
      while(ii.hasNext()) {
        long time = ii.getLongNext();
        stringData[count++] = headerParser.formatter.toDateTimeStringISO( new Date(time));
      }
      return Array.factory(String.class, shape, stringData);
    }

    if (typeInfo.hdfType == 8) { // enum
      Object data = readDataPrimitive(index, dataType, fillValue, byteOrder);
      Array codesArray = Array.factory(dataType.getPrimitiveClassType(), shape, data);

      // now transform into a String array
      String[] stringData = new String[(int) codesArray.getSize()];
      int count = 0;
      IndexIterator ii = codesArray.getIndexIterator();
      while(ii.hasNext()) {
        int code = ii.getIntNext();
        stringData[count++] = "N?A"; // vinfo.mdt.map.get(code);  LOOK
      }
      return Array.factory(String.class, shape, stringData);
    }

    if ((typeInfo.hdfType == 9) && !typeInfo.isVString) { // vlen (not string)
      DataType readType = dataType;
      if (typeInfo.isVString) // string
        readType = DataType.BYTE;
      else if (typeInfo.base.hdfType == 7) // reference
        readType = DataType.LONG;

      // general case is to read an array of vlen objects
      // each vlen generates an Array - so return ArrayObject of Array
      boolean scalar = index.getTotalNelems() == 1; // if scalar, return just the len Array
      Array[] data = new Array[(int) index.getTotalNelems()];
      int count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        for (int i = 0; i < chunk.getNelems(); i++) {
          long address = chunk.getFilePos() + index.getElemSize() * i;
          Array vlenArray = headerParser.getHeapDataArray(address, readType, byteOrder);
          data[count++] = (typeInfo.base.hdfType == 7) ? convertReference(vlenArray) : vlenArray;
        }
      }
      return (scalar) ? data[0] : Array.factory(Array.class, shape, data);
    }

    if (dataType == DataType.STRUCTURE) {
      Structure s = (Structure) v;
      ArrayStructureW asw = new ArrayStructureW(s.makeStructureMembers(), shape);

      int count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        for (int i = 0; i < chunk.getNelems(); i++) {
          if (debug) H5header.debugOut.println(" readStructure " + v.getName() + " chunk.getFilePos= " +
                  chunk.getFilePos() + " index.getElemSize= " + index.getElemSize());

          StructureData sdata = readStructure(s, asw, chunk.getFilePos() + index.getElemSize() * i);
          asw.setStructureData(sdata, count++);
        }
      }
      return asw;

    } else {
      return readDataPrimitive(index, dataType, fillValue, byteOrder);
    }
  }

  private Array convertReference(Array refArray) throws java.io.IOException, InvalidRangeException {
    int nelems = (int) refArray.getSize();
    Index ima = refArray.getIndex();
    String[] result = new String[nelems];
    for (int i = 0; i < nelems; i++) {
      long reference = refArray.getLong(ima.set(i));
      result[i] = headerParser.getDataObjectName( reference);
    }
    return Array.factory(String.class, new int[] {nelems}, result);
  }

  /**
   * Read data subset from file for a variable, create primitive array.
   *
   * @param index     handles skipping around in the file.
   * @param dataType  dataType of the variable
   * @param fillValue fill value as a wrapped primitive
   * @return primitive array with data read in
   * @throws java.io.IOException            if read error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   */
  private Object readDataPrimitive(Indexer index, DataType dataType, Object fillValue, int byteOrder) throws java.io.IOException, InvalidRangeException {
    int size = (int) index.getTotalNelems();

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.OPAQUE)) {
      byte[] pa = (byte[]) fillArray(size, dataType, fillValue);
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        myRaf.seek(chunk.getFilePos());
        myRaf.read(pa, (int) chunk.getStartElem(), chunk.getNelems()); // copy into primitive array
      }
      return (dataType == DataType.CHAR) ? convertByteToChar(pa) : pa;

    } else if (dataType == DataType.SHORT) {
      short[] pa = (short[]) fillArray(size, dataType, fillValue);
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        if (byteOrder >= 0) myRaf.order(byteOrder);
        myRaf.seek(chunk.getFilePos());
        myRaf.readShort(pa, (int) chunk.getStartElem(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.INT) {
      int[] pa = (int[]) fillArray(size, dataType, fillValue);
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        if (byteOrder >= 0) myRaf.order(byteOrder);
        myRaf.seek(chunk.getFilePos());
        myRaf.readInt(pa, (int) chunk.getStartElem(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.LONG) {
      long[] pa = (long[]) fillArray(size, dataType, fillValue);
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        if (byteOrder >= 0) myRaf.order(byteOrder);
        myRaf.seek(chunk.getFilePos());
        myRaf.readLong(pa, (int) chunk.getStartElem(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = (float[]) fillArray(size, dataType, fillValue);
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        if (byteOrder >= 0) myRaf.order(byteOrder);
        myRaf.seek(chunk.getFilePos());
        myRaf.readFloat(pa, (int) chunk.getStartElem(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double[]) fillArray(size, dataType, fillValue);
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        if (byteOrder >= 0) myRaf.order(byteOrder);
        myRaf.seek(chunk.getFilePos());
        myRaf.readDouble(pa, (int) chunk.getStartElem(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.STRING) {
      String[] sa = new String[size];
      int count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        for (int i = 0; i < chunk.getNelems(); i++) {
          sa[count++] = headerParser.readHeapString(chunk.getFilePos() + index.getElemSize() * i);
        }
      }
      return sa;
    }

    throw new IllegalStateException("H5iosp.readDataPrimitive: Unknown DataType "+dataType);
  }

  protected Object fillArray(int size, DataType dataType, Object fillValue) throws java.io.IOException, InvalidRangeException {

    if (dataType == DataType.BYTE) {
      byte[] pa = new byte[size];
      byte val = (Byte) fillValue;
      if (val != 0)
        for (int i = 0; i < size; i++) pa[i] = val;
      return pa;

    /*} else if (dataType == DataType.CHAR) {
        char[] pa = new char[size];
        byte val = (Byte) fillValue;
        if (val != 0)
          for (int i = 0; i < size; i++) pa[i] = val;
        return pa; */

    } else if (dataType == DataType.OPAQUE) {
      return new byte[size];

    } else if (dataType == DataType.SHORT) {
      short[] pa = new short[size];
      short val = (Short) fillValue;
      if (val != 0)
        for (int i = 0; i < size; i++) pa[i] = val;
      return pa;

    } else if (dataType == DataType.INT) {
      int[] pa = new int[size];
      int val = (Integer) fillValue;
      if (val != 0)
        for (int i = 0; i < size; i++) pa[i] = val;
      return pa;

    } else if (dataType == DataType.LONG) {
      long[] pa = new long[size];
      long val = (Long) fillValue;
      if (val != 0)
        for (int i = 0; i < size; i++) pa[i] = val;
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = new float[size];
      float val = (Float) fillValue;
      if (val != 0.0)
        for (int i = 0; i < size; i++) pa[i] = val;
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = new double[size];
      double val = (Double) fillValue;
      if (val != 0.0)
        for (int i = 0; i < size; i++) pa[i] = val;
      return pa;

    } else if ((dataType == DataType.STRING) || (dataType == DataType.ENUM)) {
      String[] pa = new String[size];
      for (int i = 0; i < size; i++) pa[i] = (String) fillValue;
      return pa;
    }

    throw new IllegalStateException();
  }

  private Object readFilteredData(Variable v, H5chunkFilterLayout index, Object fillValue)
          throws java.io.IOException, InvalidRangeException {
    DataType dataType = v.getDataType();
    int size = (int) index.getTotalNelems();

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.OPAQUE)) {
      byte[] pa = (byte[]) fillArray(size, dataType, fillValue);
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        ByteBuffer bb = index.getByteBuffer();
        bb.position((int) chunk.getFilePos());
        int pos = (int) chunk.getStartElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = bb.get();
      }
      return (dataType == DataType.CHAR) ? convertByteToChar(pa) : pa;

    } else if (dataType == DataType.SHORT) {
      short[] pa = (short[]) fillArray(size, dataType, fillValue);
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        ShortBuffer buff = index.getShortBuffer();
        buff.position((int) chunk.getFilePos() / 2);
        int pos = (int) chunk.getStartElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (dataType == DataType.INT) {
      int[] pa = (int[]) fillArray(size, dataType, fillValue);
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        IntBuffer buff = index.getIntBuffer();
        buff.position((int) chunk.getFilePos() / 4);
        int pos = (int) chunk.getStartElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (dataType == DataType.LONG) {
      long[] pa = (long[]) fillArray(size, dataType, fillValue);
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        LongBuffer buff = index.getLongBuffer();
        buff.position((int) chunk.getFilePos() / 8);
        int pos = (int) chunk.getStartElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = (float[]) fillArray(size, dataType, fillValue);
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        FloatBuffer buff = index.getFloatBuffer();
        buff.position((int) chunk.getFilePos() / 4);
        int pos = (int) chunk.getStartElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double[]) fillArray(size, dataType, fillValue);
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        DoubleBuffer buff = index.getDoubleBuffer();
        buff.position((int) chunk.getFilePos() / 8);
        int pos = (int) chunk.getStartElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } /* else if (dataType == DataType.STRING) {
      String[] sa = new String[size];
      int count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        for (int i=0; i< chunk.getNelems(); i++) {
          H5header.HeapIdentifier heapId = headerParser.getHeapIdentifier(chunk.getFilePos() + index.getElemSize()*i);
          if (debugString) H5header.debugOut.println("getHeapIdentifier= "+(chunk.getFilePos() + index.getElemSize()*i)+" chunk= "+chunk);
          H5header.GlobalHeap.HeapObject ho = heapId.getHeapObject();
          if (debugString) H5header.debugOut.println(" readString at HeapObject "+ho);

          byte[] ba = new byte[(int)ho.dataSize];
          myRaf.seek(ho.dataPos);
          myRaf.read(ba);
          sa[count++] = new String(ba);
        }
      }
      return sa;

    } else if (dataType == DataType.STRUCTURE) {
      Structure s = (Structure) v;
      ArrayStructureW asw = new ArrayStructureW( s.makeStructureMembers(), want.getShape());

      int count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        for (int i=0; i< chunk.getNelems(); i++) {
          if (debug) H5header.debugOut.println(" readStructure "+v.getName()+" chunk.getFilePos= "+
                                        chunk.getFilePos()+" index.getElemSize= "+index.getElemSize());

          StructureData sdata = readStructure( s, asw, chunk.getFilePos() + index.getElemSize()*i);
          asw.setStructureData( sdata, count++);
        }
      }
      return asw;

    } else if (dataType == DataType.ENUM) {  // LOOK - must be based on the parent type
      int[] pa = new int[size];
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        myRaf.seek ( chunk.getFilePos());
        myRaf.readInt( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return pa;
    }  */

    throw new IllegalStateException();
  }

  private StructureData readStructure(Structure s, ArrayStructureW asw, long dataPos) throws IOException, InvalidRangeException {
    StructureDataW sdata = new StructureDataW(asw.getStructureMembers());
    if (debug) H5header.debugOut.println(" readStructure " + s.getName() + " dataPos = " + dataPos);

    for (Variable v2 : s.getVariables()) {
      H5header.Vinfo vinfo = (H5header.Vinfo) v2.getSPobject();
      if (debug) H5header.debugOut.println(" readStructureMember " + v2.getName() + " vinfo = " + vinfo);
      Array dataArray = readData(v2, dataPos + vinfo.dataPos, v2.getShapeAsSection());
      sdata.setMemberData(v2.getShortName(), dataArray);
    }

    return sdata;
  }

  /* this converts a byte array to another primitive array
 protected Object convert( byte[] barray, DataType dataType, int nelems, int byteOrder) {

   if (dataType == DataType.BYTE) {
     return barray;
   }

   if (dataType == DataType.CHAR) {
     return convertByteToChar( barray);
   }

   ByteBuffer bbuff = ByteBuffer.wrap( barray);
   if (byteOrder >= 0)
     bbuff.order( byteOrder == RandomAccessFile.LITTLE_ENDIAN? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

   if (dataType == DataType.SHORT) {
     ShortBuffer tbuff = bbuff.asShortBuffer();
     short[] pa = new short[nelems];
     tbuff.get( pa);
     return pa;

   } else if (dataType == DataType.INT) {
     IntBuffer tbuff = bbuff.asIntBuffer();
     int[] pa = new int[nelems];
     tbuff.get( pa);
     return pa;

   } else if (dataType == DataType.FLOAT) {
     FloatBuffer tbuff = bbuff.asFloatBuffer();
     float[] pa = new float[nelems];
     tbuff.get( pa);
     return pa;

   } else if (dataType == DataType.DOUBLE) {
     DoubleBuffer tbuff = bbuff.asDoubleBuffer();
     double[] pa = new double[nelems];
     tbuff.get( pa);
     return pa;
   }

   throw new IllegalStateException();
 } */

  /* this converts a byte array to a wrapped primitive (Byte, Short, Integer, Double, Float, Long)
static Object convert( byte[] barray, DataType dataType, int byteOrder) {

  if (dataType == DataType.BYTE) {
    return new Byte( barray[0]);
  }

  if (dataType == DataType.CHAR) {
    return new Character((char) barray[0]);
  }

  ByteBuffer bbuff = ByteBuffer.wrap( barray);
  if (byteOrder >= 0)
    bbuff.order( byteOrder == RandomAccessFile.LITTLE_ENDIAN? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

  if (dataType == DataType.SHORT) {
    ShortBuffer tbuff = bbuff.asShortBuffer();
    return new Short(tbuff.get());

  } else if (dataType == DataType.INT) {
    IntBuffer tbuff = bbuff.asIntBuffer();
    return new Integer(tbuff.get());

  } else if (dataType == DataType.LONG) {
    LongBuffer tbuff = bbuff.asLongBuffer();
    return new Long(tbuff.get());

  } else if (dataType == DataType.FLOAT) {
    FloatBuffer tbuff = bbuff.asFloatBuffer();
    return new Float(tbuff.get());

  } else if (dataType == DataType.DOUBLE) {
    DoubleBuffer tbuff = bbuff.asDoubleBuffer();
    return new Double(tbuff.get());
  }

  throw new IllegalStateException();
}  */

  /*
   * Read data subset from file for a variable, create primitive array.
   * @param beginOffset variable's beginning byte offset in file.
   * @param index handles skipping around in the file.
   * @param source from this buye buffer
   * @param dataType dataType of the variable
   * @return primitive array with data read in
   *
 protected Object readData( long beginOffset, Indexer index, DataType dataType) throws java.io.IOException {
   int offset = 0;
   int chunk = index.getChunkSize();
   int size = index.getTotalSize();

   if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR)) {
     byte[] pa = new byte[size];
     while (index.hasNext()) {
       myRaf.seek ((long) beginOffset + index.next());
       myRaf.read( pa, offset, chunk); // copy into primitive array
       offset += chunk;
     }
     return (dataType == DataType.BYTE) ? pa : (Object) convertByteToChar( pa);

   } else if (dataType == DataType.SHORT) {
     short[] pa = new short[size];
     while (index.hasNext()) {
       myRaf.seek ((long) beginOffset + index.next());
       myRaf.readShort( pa, offset, chunk); // copy into primitive array
       offset += chunk;
     }
     return pa;

   } else if (dataType == DataType.INT) {
     int[] pa = new int[size];
     while (index.hasNext()) {
       myRaf.seek ((long) beginOffset + index.next());
       myRaf.readInt( pa, offset, chunk); // copy into primitive array
       offset += chunk;
     }
     return pa;

   } else if (dataType == DataType.FLOAT) {
     float[] pa = new float[size];
     while (index.hasNext()) {
      myRaf.seek ((long) beginOffset + index.next());
      myRaf.readFloat( pa, offset, chunk); // copy into primitive array
      offset += chunk;
     }
     return pa;

   } else if (dataType == DataType.DOUBLE) {
     double[] pa = new double[size];
     while (index.hasNext()) {
       myRaf.seek ((long) beginOffset + index.next());
       myRaf.readDouble( pa, offset, chunk); // copy into primitive array
       offset += chunk;
     }
     return pa;
   }

   throw new IllegalStateException();
 } */

  // convert byte array to char array

  protected char[] convertByteToChar(byte[] byteArray) {
    int size = byteArray.length;
    char[] cbuff = new char[size];
    for (int i = 0; i < size; i++)
      cbuff[i] = (char) byteArray[i];
    return cbuff;
  }

  // convert char array to byte array
  protected byte[] convertCharToByte(char[] from) {
    int size = from.length;
    byte[] to = new byte[size];
    for (int i = 0; i < size; i++)
      to[i] = (byte) from[i];
    return to;
  }

  /**
   * Read all the data from the netcdf file for this Variable and return a memory resident Array.
   * This Array has the same element type and shape as the Variable.
   * <p/>
   *
   * @return the requested data in a memory-resident Array.
   *         <p/>
   *         public Array readData(ucar.nc2.Variable v2) throws java.io.IOException {
   *         H5header.Vinfo = (H5header.Vinfo) v2.getSPobject();
   *         <p/>
   *         mapBuffer.position( (int) filePos);
   *         ByteBuffer slice = mapBuffer.slice();
   *         int size = (int) v.getSize();
   *         DataType type = v.getDataType();
   *         <p/>
   *         if (type == DataType.BYTE) {
   *         byte[] buff = new byte[size];
   *         slice.get(buff);
   *         return Array.factory( v.getElementType(), v.getShape(), buff);
   *         <p/>
   *         } else if (type == DataType.STRING) {
   *         byte[] buff = new byte[size];
   *         slice.get(buff); // why ??
   *         char[] cbuff = new char[size];
   *         for (int i=0; i<size; i++)
   *         cbuff[i] = (char) buff[i];
   *         return Array.factory( v.getElementType(), v.getShape(), cbuff);
   *         <p/>
   *         } else if (type == DataType.SHORT) {
   *         short[] buff = new short[size];
   *         ShortBuffer cb = slice.asShortBuffer();
   *         cb.get(buff);
   *         return Array.factory( v.getElementType(), v.getShape(), buff);
   *         <p/>
   *         } else if (type == DataType.INT) {
   *         int[] buff = new int[size];
   *         IntBuffer cb = slice.asIntBuffer();
   *         cb.get(buff);
   *         return Array.factory( v.getElementType(), v.getShape(), buff);
   *         <p/>
   *         } else if (type == DataType.FLOAT) {
   *         float[] buff = new float[size];
   *         FloatBuffer cb = slice.asFloatBuffer();
   *         cb.get(buff);
   *         return Array.factory( v.getElementType(), v.getShape(), buff);
   *         <p/>
   *         } else if (type == DataType.DOUBLE) {
   *         double[] buff = new double[size];
   *         DoubleBuffer cb = slice.asDoubleBuffer();
   *         cb.get(buff);
   *         return Array.factory( v.getElementType(), v.getShape(), buff);
   *         }
   *         <p/>
   *         throw new IllegalStateException();
   *         }
   */

  //////////////////////////////////////////////////////////////
  /* utilities

 static public void dump(PrintStream ps, String head, ByteBuffer buffer, int size) {
   int savePos = buffer.position();
   byte[] mess = new byte[size];
   buffer.get(mess);
   printBytes( ps, head, mess, size, false);
   buffer.position(savePos);
 }

 void dump(int pos, int size) {
   int savePos = mapBuffer.position();
   mapBuffer.position(pos);
   byte[] mess = new byte[size];
   mapBuffer.get(mess);
   printBytes( System.out, " mess", mess, size, false);
   mapBuffer.position(savePos);
 }

 static void dump(ByteBuffer buffer, int pos, int size) {
   int savePos = buffer.position();
   buffer.position(pos);
   byte[] mess = new byte[size];
   buffer.get(mess);
   printBytes( System.out, " mess", mess, size, false);
   buffer.position(savePos);
 }

 static void dumpWithCount(ByteBuffer buffer, int pos, int size) {
   int savePos = buffer.position();
   buffer.position(pos);
   byte[] mess = new byte[size];
   buffer.get(mess);
   printBytes( System.out, " mess", mess, size, true);
   buffer.position(savePos);
 }

 static void printBytes( PrintStream ps, String head, ByteBuffer buffer, int n, boolean count) {
   ps.print(head+" == ");
   for (int i=0; i<n; i++) {
     byte b = buffer.get();
     int ub = (b < 0) ? b + 256 : b;
     if (count) ps.print( i+":");
     ps.print( ub);
     if (!count) {
       ps.print( "(");
       ps.write(b);
       ps.print( ")");
     }
   ps.print( " ");    }
   ps.println();
 }

 static void printBytes( PrintStream ps, String head, byte[] buff, int n, boolean count) {
   ps.print(head+" == ");
   for (int i=0; i<n; i++) {
     byte b = buff[i];
     int ub = (b < 0) ? b + 256 : b;
     if (count) ps.print( i+":");
     ps.print( ub);
     if (!count) {
       ps.print( "(");
       ps.write(b);
       ps.print( ")");
     }
     ps.print( " ");
   }
   ps.println();
 } */

  //////////////////////////////////////////////////////////////////////////
  // utilities
  public boolean syncExtend() {
    return false;
  }

  public boolean sync() {
    return false;
  }

  /**
   * Flush all data buffers to disk.
   *
   * @throws IOException on io error
   */
  public void flush() throws IOException {
    myRaf.flush();
  }

  /**
   * Close the file.
   *
   * @throws IOException on io error
   */
  public void close() throws IOException {
    if (myRaf != null)
      myRaf.close();
    headerParser.close();
  }

  /**
   * Debug info for this object.
   */
  public String toStringDebug (Object o) {
    if (o instanceof Variable) {
      Variable v = (Variable) o;
      H5header.Vinfo vinfo = (H5header.Vinfo) v.getSPobject();
      return vinfo.toString();
    }
    return null;
  }

  public String getDetailInfo() {
    return "";
  }
}