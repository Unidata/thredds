// $Id: H5iosp.java,v 1.20 2006/05/12 20:19:28 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package ucar.nc2;

import ucar.ma2.*;

import ucar.unidata.io.RandomAccessFile;

import java.util.*;
import java.util.zip.*;
import java.io.IOException;
import java.nio.*;

/**
 * @author caron
 * @version $Revision: 1.21 $ $Date: 2006/05/08 02:47:36 $
 */

class H5iosp implements IOServiceProvider {
  static boolean debug = false;
  static boolean debugPos = false;
  static boolean debugHeap = false;
  static boolean debugFilter = false;
  static boolean debugFilterDetails = false;
  static boolean debugString = false;
  static boolean debugFilterIndexer = false;
  static boolean debugChunkIndexer = false;

  static void setDebugFlags( ucar.nc2.util.DebugFlags debugFlag) {
    debug =  debugFlag.isSet("H5iosp/read");
    debugPos =  debugFlag.isSet("H5iosp/filePos");
    debugHeap =  debugFlag.isSet("H5iosp/Heap");
    debugFilter =  debugFlag.isSet("H5iosp/filter");
    debugFilterIndexer =  debugFlag.isSet("H5iosp/filterIndexer");
    debugChunkIndexer =  debugFlag.isSet("H5iosp/chunkIndexer");
  }

  public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf) {
    return H5header.isValidFile( raf);
  }

 //////////////////////////////////////////////////////////////////////////////////

  private NetcdfFile ncfile;
  private RandomAccessFile myRaf;
  private H5header headerParser;

  private boolean showBytes = false;
  private boolean showHeaderBytes = false;

  public void setProperties( List iospProperties) { }

  /////////////////////////////////////////////////////////////////////////////
  // reading

  public void open(RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {

    this.ncfile = ncfile;
    this.myRaf = raf;

    headerParser = new H5header();
    headerParser.read(myRaf, ncfile);

    ncfile.finish();

    //channel = raf.getChannel();
    //if (debug) out.println ("Opened file to read:'" + ncfile.getPathName()+ "', size=" + channel.size());

    //mapBuffer = channel.map(FileChannel.MapMode.READ_ONLY, (long) 0, channel.size());
    //mapBuffer.order(ByteOrder.LITTLE_ENDIAN);
  }

  public Array readData(ucar.nc2.Variable v2, java.util.List sectionList) throws IOException, InvalidRangeException  {
    // subset
    Range[] section = Range.toArray( sectionList);
    int[] origin = new int[v2.getRank()];
    int[] shape = v2.getShape();
    if (section != null) {
      for (int i=0; i<section.length; i++ ) {
        origin[i] = section[i].first();
        shape[i] = section[i].length();
        if (section[i].stride() != 1)
          throw new UnsupportedOperationException("H5iosp doesnt yet support strides");
      }
    }
    H5header.Vinfo vinfo = (H5header.Vinfo) v2.getSPobject();
    return readData( v2, vinfo.dataPos, origin, shape);
  }

  // all the work is here, so can be called recursively
  private Array readData(ucar.nc2.Variable v2, long dataPos, int [] origin, int [] shape) throws IOException, InvalidRangeException  {
    if (origin == null) origin = new int[ v2.getRank()];
    if (shape == null) shape = v2.getShape();

    H5header.Vinfo vinfo = (H5header.Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();
    Object data;

    if (vinfo.useFillValue) {
      Array arr = Array.factory(dataType.getPrimitiveClassType(), shape);
      Object fillValue = convert( vinfo.fillValue, dataType, vinfo.byteOrder);
      IndexIterator ii = arr.getIndexIterator();
      while(ii.hasNext())
        ii.setObjectNext( fillValue);
      return arr;

    } else if (vinfo.hasFilter) {
      if (debugFilter) H5header.debugOut.println("read variable "+v2.getName()+" vinfo = "+vinfo);
      data = readFilterData( v2, vinfo);

      // LOOK what to do about origin/shape ??
      Array fullArray = Array.factory(dataType.getPrimitiveClassType(), v2.getShape(), data);
      return fullArray.sectionNoReduce( origin, shape, null);

    } else {
      if (debug) H5header.debugOut.println("read variable "+v2.getName()+" vinfo = "+vinfo);

      Indexer index;
      if (vinfo.isChunked) {
        if (vinfo.btree == null)
          vinfo.btree = headerParser.getDataBTreeAt( v2.getName(), vinfo.dataPos, vinfo.storageSize.length);
        index = new H5chunkIndexer( v2, origin, shape);

      } else {
        index = new RegularIndexer(v2.getShape(), v2.getElementSize(), dataPos, Range.factory(origin, shape), -1);
      }

      if (vinfo.byteOrder >= 0) {
        myRaf.order(vinfo.byteOrder);
        if (debug) H5header.debugOut.println("$$set byteOrder to  "+(vinfo.byteOrder == RandomAccessFile.LITTLE_ENDIAN ? " LittleEndian" : " BigEndian"));
      }
      data = readData( v2, index, dataType, shape);
    }

    if (data instanceof ArrayStructure)
      return (ArrayStructure) data;
    else
      return Array.factory(dataType.getPrimitiveClassType(), shape, data);
  }

   /**
    * Read data subset from file for a variable, create primitive array.
    * @param v the variable to read.
    * @param index handles skipping around in the file.
    * @param dataType dataType of the variable
    * @return primitive array with data read in
    */
  protected Object readData( Variable v, Indexer index, DataType dataType, int[] shape) throws java.io.IOException, InvalidRangeException {
    int size = index.getTotalNelems();

    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR)) {
      byte[] pa = new byte[size];
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        myRaf.seek ( chunk.getFilePos());
        myRaf.read( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return (dataType == DataType.BYTE) ? pa : (Object) convertByteToChar( pa);

    } else if (dataType == DataType.SHORT) {
      short[] pa = new short[size];
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        myRaf.seek ( chunk.getFilePos());
        myRaf.readShort( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.INT) {
      int[] pa = new int[size];
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        myRaf.seek ( chunk.getFilePos());
        myRaf.readInt( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.LONG) {
      long[] pa = new long[size];
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        myRaf.seek ( chunk.getFilePos());
        myRaf.readLong( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = new float[size];
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        myRaf.seek ( chunk.getFilePos());
        myRaf.readFloat( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = new double[size];
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        myRaf.seek ( chunk.getFilePos());
        myRaf.readDouble( pa, chunk.getIndexPos(), chunk.getNelems()); // copy into primitive array
      }
      return pa;

    } else if (dataType == DataType.STRING) {
      String[] sa = new String[size];
      int count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        for (int i=0; i< chunk.getNelems(); i++) {
          H5header.HeapIdentifier heapId = headerParser.getHeapIdentifier(chunk.getFilePos() + index.getElemSize()*i);
          H5header.GlobalHeap.HeapObject ho = heapId.getHeapObject();
          if (debugString) H5header.debugOut.println("readString at HeapObject "+ho);

          byte[] ba = new byte[(int)ho.dataSize];
          myRaf.seek(ho.dataPos);
          myRaf.read(ba);
          sa[count++] = new String(ba);
        }
      }
      return sa;

    } else if (dataType == DataType.STRUCTURE) {
      Structure s = (Structure) v;
      ArrayStructureW asw = new ArrayStructureW( s.makeStructureMembers(), shape);

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
    }

    throw new IllegalStateException();
  }

  public ucar.ma2.Array readNestedData(ucar.nc2.Variable v2, java.util.List section)
         throws java.io.IOException, ucar.ma2.InvalidRangeException {

    throw new UnsupportedOperationException("H5 IOSP does not support nested variables");

  }

  private StructureData readStructure(Structure s, ArrayStructureW asw, long dataPos) throws IOException, InvalidRangeException  {
    StructureDataW sdata = new StructureDataW(asw.getStructureMembers());
    if (debug) H5header.debugOut.println(" readStructure "+s.getName()+" dataPos = "+dataPos);

    Iterator viter = s.getVariables().iterator();
    while (viter.hasNext()) {
      Variable v2 = (Variable) viter.next();
      H5header.Vinfo vinfo = (H5header.Vinfo) v2.getSPobject();
      if (debug) H5header.debugOut.println(" readStructureMember "+v2.getName()+ " vinfo = "+vinfo);
      Array dataArray = readData(v2, dataPos + vinfo.dataPos, null, null);
      sdata.setMemberData( v2.getShortName(), dataArray);
    }

    return sdata;
  }

  // this reads the entire data array
  private Object readFilterData(ucar.nc2.Variable v2, H5header.Vinfo vinfo) throws IOException, InvalidRangeException  {

    // pretend everything is just bytes for now
    int elemSize = v2.getElementSize();
    int totalSize = (int) v2.getSize() * elemSize;
    int totalDone = 0;
    byte[] barray = new byte[ totalSize];
    if (debugFilter) H5header.debugOut.println("-----readFilterData totalSize = "+totalSize+
                                               " elemSize = "+elemSize);

    // read one chunk at a time
    int chunkSize = 1;
    for (int j=0; j<vinfo.storageSize.length; j++)
      chunkSize *= vinfo.storageSize[j];
    if (debugFilter) H5header.debugOut.println("      chunkSize = "+chunkSize);
    byte[] buff = new byte[chunkSize];

    // this handles where the chunk lives in the overall data array
    H5chunkFilterIndexer indexer = new H5chunkFilterIndexer( v2, vinfo.storageSize);

    // buffer for the compressed bytes
    byte[] cbuff = null; // compressed bytes
    int cbuffSize = 0;

    java.util.zip.Inflater inflater = new java.util.zip.Inflater( false);

    // loop over all the entries in the data btree structure
    if (vinfo.btree == null)
      vinfo.btree = headerParser.getDataBTreeAt( v2.getName(), vinfo.dataPos, vinfo.storageSize.length);

    ArrayList entries = vinfo.btree.getEntries();
    for (int i=0; i<entries.size(); i++) {
      H5header.DataBTree.DataEntry entry = (H5header.DataBTree.DataEntry) entries.get(i);
      if (debugFilter) H5header.debugOut.println("-----entry= = "+entry);
      if ((cbuff == null) || (cbuffSize < entry.size)) {
        cbuffSize = 2*entry.size;
        cbuff = new byte[cbuffSize];
      }

      // jump to the data
      myRaf.seek ( entry.address);

      if (entry.filterMask == 1) { // skip  decompress
        if (debugFilter) H5header.debugOut.println("skip inflate");
        myRaf.read(buff, 0, entry.size);

      } else {
        // read compressed bytes
        myRaf.read(cbuff, 0, entry.size);
        if (debugFilterDetails) H5header.printBytes( "  raw bytes=", buff, 0, entry.size);

        // decompress the bytes
        inflater.setInput(cbuff, 0, entry.size);
        int resultLength = 0;
        try {
          resultLength = inflater.inflate(buff, 0, chunkSize);
        }
        catch (DataFormatException ex) {
          System.out.println("ERROR on "+v2.getName());
          ex.printStackTrace();
          throw new IOException( ex.getMessage());
        }
        if (debugFilter) H5header.debugOut.println( "inflate finished="+ inflater.finished()+" "+resultLength+" bytes; totalDone= "+totalDone);
        if (debugFilterDetails) H5header.printBytes( "  bytes=", buff, 0, chunkSize);
        inflater.reset();
      }

      // copy decompressed bytes into the right place in the output array
      indexer.setChunkOffset( entry.offset);
      while (indexer.hasNext() && (totalDone < totalSize)) {
        Indexer.Chunk chunk = indexer.next();
        int n = Math.min( chunk.getNelems(), totalSize - totalDone);
        System.arraycopy(buff, chunk.getIndexPos(), barray, (int) chunk.getFilePos(), n);
        totalDone += chunk.getNelems();
      }

    }

    if (debugFilter)
      H5header.debugOut.println( "----- total elements read="+ totalDone+" bytes (should be "+ totalSize+")");

    // convert to the correct primitive type
    return convert( barray, v2.getDataType(), (int) v2.getSize(), vinfo.byteOrder);
  }

  // this converts a byte array to another primitive array
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
  }

  // this converts a byte array to a wrapped primitive (Byte, Short, Integer, Double, Float, Long)
  protected Object convert( byte[] barray, DataType dataType, int byteOrder) {

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
  }


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
  protected char[] convertByteToChar( byte[] byteArray) {
    int size = byteArray.length;
    char[] cbuff = new char[size];
    for (int i=0; i<size; i++)
      cbuff[i] = (char) byteArray[i];
    return cbuff;
  }

   // convert char array to byte array
  protected byte[] convertCharToByte( char[] from) {
    int size = from.length;
    byte[] to = new byte[size];
    for (int i=0; i<size; i++)
      to[i] = (byte) from[i];
    return to;
  }

  /**
   * Read all the data from the netcdf file for this Variable and return a memory resident Array.
   * This Array has the same element type and shape as the Variable.
   * <p>
   * @return the requested data in a memory-resident Array.
   *
  public Array readData(ucar.nc2.Variable v2) throws java.io.IOException {
    H5header.Vinfo = (H5header.Vinfo) v2.getSPobject();

    mapBuffer.position( (int) filePos);
    ByteBuffer slice = mapBuffer.slice();
    int size = (int) v.getSize();
    DataType type = v.getDataType();

    if (type == DataType.BYTE) {
      byte[] buff = new byte[size];
      slice.get(buff);
      return Array.factory( v.getElementType(), v.getShape(), buff);

    } else if (type == DataType.STRING) {
      byte[] buff = new byte[size];
      slice.get(buff); // why ??
      char[] cbuff = new char[size];
      for (int i=0; i<size; i++)
        cbuff[i] = (char) buff[i];
      return Array.factory( v.getElementType(), v.getShape(), cbuff);

    } else if (type == DataType.SHORT) {
      short[] buff = new short[size];
      ShortBuffer cb = slice.asShortBuffer();
      cb.get(buff);
      return Array.factory( v.getElementType(), v.getShape(), buff);

    } else if (type == DataType.INT) {
      int[] buff = new int[size];
      IntBuffer cb = slice.asIntBuffer();
      cb.get(buff);
      return Array.factory( v.getElementType(), v.getShape(), buff);

    } else if (type == DataType.FLOAT) {
      float[] buff = new float[size];
      FloatBuffer cb = slice.asFloatBuffer();
      cb.get(buff);
      return Array.factory( v.getElementType(), v.getShape(), buff);

    } else if (type == DataType.DOUBLE) {
      double[] buff = new double[size];
      DoubleBuffer cb = slice.asDoubleBuffer();
      cb.get(buff);
      return Array.factory( v.getElementType(), v.getShape(), buff);
    }

    throw new IllegalStateException();
  } */

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

  public boolean syncExtend() { return false; }
  public boolean sync() { return false; }

  /**
   * Flush all data buffers to disk.
   * @throws IOException
   */
  public void flush() throws IOException {
    myRaf.flush();
  }

  /**
   *  Close the file.
   * @throws IOException
   */
  public void close() throws IOException {
    if (myRaf != null)
      myRaf.close();
    headerParser.close();
  }

  /** Debug info for this object. */
  public String toStringDebug(Object o) {
    if (o instanceof Variable) {
      Variable v = (Variable) o;
      H5header.Vinfo vinfo = (H5header.Vinfo) v.getSPobject();
      return vinfo.toStringDebug( v.getName());
    }
    return null;
  }

  public String getDetailInfo() {
    return "";
  }
}