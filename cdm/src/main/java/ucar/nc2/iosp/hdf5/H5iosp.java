/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
import ucar.nc2.iosp.*;
import ucar.nc2.iosp.hdf4.HdfEos;
import ucar.nc2.*;

import java.io.IOException;
import java.util.Date;
import java.nio.ByteBuffer;

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
  static boolean debugStructure = true;

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

  private RandomAccessFile myRaf;
  private H5header headerParser;

  //private boolean showBytes = false;
  //private boolean showHeaderBytes = false;

  ///public void setSpecial(Object special) {
  //}

  /////////////////////////////////////////////////////////////////////////////
  // reading

  public void open(RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {

    this.myRaf = raf;

    headerParser = new H5header(myRaf, ncfile, this);
    headerParser.read();

    // check if its an HDF5-EOS file
    Group eosInfo = ncfile.getRootGroup().findGroup("HDFEOS INFORMATION");
    if (eosInfo != null) {
      HdfEos.amendFromODL(ncfile, eosInfo);
    }

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
    Object data = null;

    //if (v2.getShortName().equals("cr"))
    //  System.out.println("hey");

    if (vinfo.useFillValue) { // fill value only
      Object pa = IospHelper.makePrimitiveArray((int) wantSection.computeSize(), dataType, vinfo.getFillValue());
      if (dataType == DataType.CHAR)
        pa = IospHelper.convertByteToChar((byte[]) pa);
      return Array.factory(dataType.getPrimitiveClassType(), wantSection.getShape(), pa);
    }

    if (vinfo.mfp != null) { // filtered
      if (debugFilter) H5header.debugOut.println("read variable filtered " + v2.getName() + " vinfo = " + vinfo);
      assert vinfo.isChunked;

      //H5chunkFilterLayout index = new H5chunkFilterLayout(v2, wantSection, myRaf, vinfo.mfp.getFilters());
      //data = readFilteredData(v2, index, vinfo.getFillValue());
      //System.out.println("***************H5chunkFilterLayout");
      H5tiledLayoutBB layout = new H5tiledLayoutBB(v2, wantSection, myRaf, vinfo.mfp.getFilters());
      data = IospHelper.readDataFill(layout, v2.getDataType(), vinfo.getFillValue());

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

      Layout layout;
      if (vinfo.isChunked) {
        //index = new H5chunkLayout(v2, readDtype, wantSection);
        //data = readData(vinfo, v2, index, readDtype, wantSection.getShape(), fillValue, byteOrder); // LOOK! byteOrder
        //System.out.println("************H5chunkLayout");
        layout = new H5tiledLayout(v2, readDtype, wantSection);
      } else {
        //Indexer index = RegularSectionLayout.factory(dataPos, elemSize, new Section(v2.getShape()), wantSection); // LOOK why RegularSectionLayout ??
        //data = readData(vinfo, v2, index, readDtype, wantSection.getShape(), fillValue, byteOrder);
        // System.out.println("************RegularSectionLayout for "+v2.getShortName());
        layout = new LayoutRegular(dataPos, elemSize, v2.getShape(), wantSection);
      }
      if (byteOrder >= 0) myRaf.order(byteOrder);
      data = readData(vinfo, v2, layout, readDtype, wantSection.getShape(), fillValue, byteOrder);
    }

    if (data instanceof Array)
      return (Array) data;
    else
      return Array.factory(dataType.getPrimitiveClassType(), wantSection.getShape(), data);
  }

  /*
   * Read data subset from file for a variable, create primitive array.
   *
   * @param v         the variable to read.
   * @param layout     handles skipping around in the file.
   * @param dataType  dataType of the data to read
   * @param shape     the shape of the output
   * @param fillValue fill value as a wrapped primitive
   * @return primitive array or Array with data read in
   * @throws java.io.IOException            if read error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   */
  private Object readData(H5header.Vinfo vinfo, Variable v, Layout layout, DataType dataType, int[] shape,
                          Object fillValue, int byteOrder) throws java.io.IOException, InvalidRangeException {

    // special processing
    H5header.TypeInfo typeInfo = vinfo.typeInfo;
    if (typeInfo.hdfType == 2) { // time
      Object data = readDataPrimitive(layout, dataType, fillValue, byteOrder);
      //Object data = IospHelper.readDataFill( myRaf, layout, dataType, fillValue);
      Array timeArray = Array.factory(dataType.getPrimitiveClassType(), shape, data);

      // now transform into an ISO Date String
      String[] stringData = new String[(int) timeArray.getSize()];
      int count = 0;
      IndexIterator ii = timeArray.getIndexIterator();
      while (ii.hasNext()) {
        long time = ii.getLongNext();
        stringData[count++] = headerParser.formatter.toDateTimeStringISO(new Date(time));
      }
      return Array.factory(String.class, shape, stringData);
    }

    if (typeInfo.hdfType == 8) { // enum
      Object data = readDataPrimitive(layout, dataType, fillValue, byteOrder);
      //Object data = IospHelper.readDataFill( myRaf, layout, dataType, fillValue);
      Array codesArray = Array.factory(dataType.getPrimitiveClassType(), shape, data);

      // now transform into a String array
      String[] stringData = new String[(int) codesArray.getSize()];
      int count = 0;
      IndexIterator ii = codesArray.getIndexIterator();
      while (ii.hasNext()) {
        int code = ii.getIntNext();
        String s = vinfo.enumMap.get(code);
        stringData[count++] = (s == null) ? "" : s;
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
      boolean scalar = layout.getTotalNelems() == 1; // if scalar, return just the len Array
      Array[] data = new Array[(int) layout.getTotalNelems()];
      int count = 0;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null) continue;
        for (int i = 0; i < chunk.getNelems(); i++) {
          long address = chunk.getSrcPos() + layout.getElemSize() * i;
          Array vlenArray = headerParser.getHeapDataArray(address, readType, byteOrder);
          data[count++] = (typeInfo.base.hdfType == 7) ? convertReference(vlenArray) : vlenArray;
        }
      }
      return (scalar) ? data[0] : Array.factory(Array.class, shape, data);
    }

    if (dataType == DataType.STRUCTURE) {
      boolean hasStrings = false;
      Structure s = (Structure) v;

      // create StructureMembers - must set offsets
      StructureMembers sm = s.makeStructureMembers();
      for (StructureMembers.Member m : sm.getMembers()) {
        Variable v2 = s.findVariable(m.getName());
        H5header.Vinfo vm = (H5header.Vinfo) v2.getSPobject();
        m.setDataParam((int) (vm.dataPos)); // offset since start of Structure
        if (v2.getDataType() == DataType.STRING)
          hasStrings = true;
      }
      int recsize = layout.getElemSize();
      sm.setStructureSize(recsize);

      // place data into an ArrayStructureBB for efficiency
      ArrayStructureBB asbb = new ArrayStructureBB(sm, shape);
      byte[] byteArray = (byte[]) asbb.getStorage();
      ByteBuffer bb = asbb.getByteBuffer();
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null) continue;
        if (debugStructure)
          H5header.debugOut.println(" readStructure " + v.getName() + " chunk= " + chunk + " index.getElemSize= " + layout.getElemSize());
        // copy bytes directly into the underlying byte[]
        myRaf.seek(chunk.getSrcPos());
        myRaf.read(byteArray, (int) chunk.getDestElem() * recsize, chunk.getNelems() * recsize);
      }

      // strings are stored on the heap, and must be read seperately
      if (hasStrings) {
        int destPos = 0;
        for (int i = 0; i< layout.getTotalNelems(); i++) { // loop over each structure
          convertStrings(asbb, destPos, sm);
          destPos += layout.getElemSize();
        }
      }


      return asbb;
    } // */

    /* if (dataType == DataType.STRUCTURE) {
      Structure s = (Structure) v;
      ArrayStructureW asw = new ArrayStructureW(s.makeStructureMembers(), shape);

      int count = 0;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null) continue;
        for (int i = 0; i < chunk.getNelems(); i++) {
          if (debugStructure) H5header.debugOut.println(" readStructure " + v.getName() + " chunk.getSrcPos= " +
                  chunk.getSrcPos() + " index.getElemSize= " + layout.getElemSize());

          StructureData sdata = readStructure(s, asw, chunk.getSrcPos() + layout.getElemSize() * i);
          asw.setStructureData(sdata, count++);
        }
      }
      return asw;
    } // */

    // normal case
    return readDataPrimitive(layout, dataType, fillValue, byteOrder);
  }

  private Array convertReference(Array refArray) throws java.io.IOException {
    int nelems = (int) refArray.getSize();
    Index ima = refArray.getIndex();
    String[] result = new String[nelems];
    for (int i = 0; i < nelems; i++) {
      long reference = refArray.getLong(ima.set(i));
      result[i] = headerParser.getDataObjectName(reference);
    }
    return Array.factory(String.class, new int[]{nelems}, result);
  }

  private void convertStrings(ArrayStructureBB asbb, int pos, StructureMembers sm) throws java.io.IOException {
    ByteBuffer bb = asbb.getByteBuffer();
    for (StructureMembers.Member m : sm.getMembers()) {
      if (m.getDataType() == DataType.STRING) {
        int size = m.getSize();
        int destPos = pos  + m.getDataParam();
        for (int i = 0; i < size; i++)  { // 16 byte "heap ids" are in the ByteBuffer
          String s = headerParser.readHeapString(bb, destPos + i * 16);
          int index = asbb.addStringToHeap(s);
          bb.putInt(destPos + i * 4, index); // overwrite with the index into the StringHeap
        }
      }
    }
  }

  /*
   * Read data subset from file for a variable, create primitive array.
   *
   * @param index     handles skipping around in the file.
   * @param dataType  dataType of the variable
   * @param fillValue fill value as a wrapped primitive
   * @return primitive array with data read in
   * @throws java.io.IOException            if read error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   */
  private Object readDataPrimitive(Layout layout, DataType dataType, Object fillValue, int byteOrder) throws java.io.IOException, InvalidRangeException {
    int size = (int) layout.getTotalNelems();

    if (dataType == DataType.STRING) {
      String[] sa = new String[size];
      int count = 0;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null) continue;
        for (int i = 0; i < chunk.getNelems(); i++) { // 16 byte "heap ids"
          sa[count++] = headerParser.readHeapString(chunk.getSrcPos() + layout.getElemSize() * i);
        }
      }
      return sa;
    }

    // normal case
    return IospHelper.readDataFill(myRaf, layout, dataType, fillValue, byteOrder);
  }

  /*
 * Read data subset from file for a variable, create primitive array.
 *
 * @param index     handles skipping around in the file.
 * @param dataType  dataType of the variable
 * @param fillValue fill value as a wrapped primitive
 * @return primitive array with data read in
 * @throws java.io.IOException            if read error
 * @throws ucar.ma2.InvalidRangeException if invalid section
 *
private Object readDataPrimitive(Indexer index, DataType dataType, Object fillValue, int byteOrder) throws java.io.IOException, InvalidRangeException {
  int size = (int) index.getTotalNelems();

  if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.OPAQUE)) {
    byte[] pa = (byte[]) IospHelper.makePrimitiveArray(size, dataType, fillValue);
    while (index.hasNext()) {
      Indexer.Chunk chunk = index.next();
      if (chunk == null) continue;
      myRaf.seek(chunk.getFilePos());
      myRaf.read(pa, (int) chunk.getStartElem(), chunk.getNelems()); // copy into primitive array
    }
    return (dataType == DataType.CHAR) ? IospHelper.convertByteToChar(pa) : pa;

  } else if (dataType == DataType.SHORT) {
    short[] pa = (short[]) IospHelper.makePrimitiveArray(size, dataType, fillValue);
    while (index.hasNext()) {
      Indexer.Chunk chunk = index.next();
      if (chunk == null) continue;
      if (byteOrder >= 0) myRaf.order(byteOrder);
      myRaf.seek(chunk.getFilePos());
      myRaf.readShort(pa, (int) chunk.getStartElem(), chunk.getNelems()); // copy into primitive array
    }
    return pa;

  } else if (dataType == DataType.INT) {
    int[] pa = (int[]) IospHelper.makePrimitiveArray(size, dataType, fillValue);
    while (index.hasNext()) {
      Indexer.Chunk chunk = index.next();
      if (chunk == null) continue;
      if (byteOrder >= 0) myRaf.order(byteOrder);
      myRaf.seek(chunk.getFilePos());
      myRaf.readInt(pa, (int) chunk.getStartElem(), chunk.getNelems()); // copy into primitive array
    }
    return pa;

  } else if (dataType == DataType.LONG) {
    long[] pa = (long[]) IospHelper.makePrimitiveArray(size, dataType, fillValue);
    while (index.hasNext()) {
      Indexer.Chunk chunk = index.next();
      if (chunk == null) continue;
      if (byteOrder >= 0) myRaf.order(byteOrder);
      myRaf.seek(chunk.getFilePos());
      myRaf.readLong(pa, (int) chunk.getStartElem(), chunk.getNelems()); // copy into primitive array
    }
    return pa;

  } else if (dataType == DataType.FLOAT) {
    float[] pa = (float[]) IospHelper.makePrimitiveArray(size, dataType, fillValue);
    while (index.hasNext()) {
      Indexer.Chunk chunk = index.next();
      if (chunk == null) continue;
      if (byteOrder >= 0) myRaf.order(byteOrder);
      myRaf.seek(chunk.getFilePos());
      myRaf.readFloat(pa, (int) chunk.getStartElem(), chunk.getNelems()); // copy into primitive array
    }
    return pa;

  } else if (dataType == DataType.DOUBLE) {
    double[] pa = (double[]) IospHelper.makePrimitiveArray(size, dataType, fillValue);
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
}  */

  /* private Object readFilteredData(Variable v, H5chunkFilterLayout index, Object fillValue)
          throws java.io.IOException, InvalidRangeException {
    DataType dataType = v.getDataType();
    int size = (int) index.getTotalNelems();

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR) || (dataType == DataType.OPAQUE)) {
      byte[] pa = (byte[]) IospHelper.makePrimitiveArray(size, dataType, fillValue);
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        if (chunk == null) continue;
        ByteBuffer bb = index.getByteBuffer();
        bb.position((int) chunk.getFilePos());
        int pos = (int) chunk.getStartElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = bb.get();
      }
      return (dataType == DataType.CHAR) ? IospHelper.convertByteToChar(pa) : pa;

    } else if (dataType == DataType.SHORT) {
      short[] pa = (short[]) IospHelper.makePrimitiveArray(size, dataType, fillValue);
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
      int[] pa = (int[]) IospHelper.makePrimitiveArray(size, dataType, fillValue);
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
      long[] pa = (long[]) IospHelper.makePrimitiveArray(size, dataType, fillValue);
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
      float[] pa = (float[]) IospHelper.makePrimitiveArray(size, dataType, fillValue);
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
      double[] pa = (double[]) IospHelper.makePrimitiveArray(size, dataType, fillValue);
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
    }  

    throw new IllegalStateException();
  } */

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

  //////////////////////////////////////////////////////////////////////////
  // utilities

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
  public String toStringDebug(Object o) {
    if (o instanceof Variable) {
      Variable v = (Variable) o;
      H5header.Vinfo vinfo = (H5header.Vinfo) v.getSPobject();
      return vinfo.toString();
    }
    return null;
  }

}