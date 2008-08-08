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
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.Formatter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
  static boolean debugStructure = false;

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

  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return H5header.isValidFile(raf);
  }

  //////////////////////////////////////////////////////////////////////////////////

  private RandomAccessFile myRaf;
  private H5header headerParser;

  /////////////////////////////////////////////////////////////////////////////
  // reading

  public void open(RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {

    this.myRaf = raf;
    headerParser = new H5header(myRaf, ncfile, this);
    headerParser.read(null);

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
    Object data;

    if (vinfo.useFillValue) { // fill value only
      Object pa = IospHelper.makePrimitiveArray((int) wantSection.computeSize(), dataType, vinfo.getFillValue());
      if (dataType == DataType.CHAR)
        pa = IospHelper.convertByteToChar((byte[]) pa);
      return Array.factory(dataType.getPrimitiveClassType(), wantSection.getShape(), pa);
    }

    if (vinfo.mfp != null) { // filtered
      if (debugFilter) System.out.println("read variable filtered " + v2.getName() + " vinfo = " + vinfo);
      assert vinfo.isChunked;
      ByteOrder bo = (vinfo.typeInfo.byteOrder == 0) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
      H5tiledLayoutBB layout = new H5tiledLayoutBB(v2, wantSection, myRaf, vinfo.mfp.getFilters(), bo);
      data = IospHelper.readDataFill(layout, v2.getDataType(), vinfo.getFillValue());

    } else { // normal case
      if (debug) System.out.println("read variable " + v2.getName() + " vinfo = " + vinfo);

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
        layout = new H5tiledLayout(v2, readDtype, wantSection);
      } else {
        layout = new LayoutRegular(dataPos, elemSize, v2.getShape(), wantSection);
      }
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
      Object data =  IospHelper.readDataFill(myRaf, layout, dataType, fillValue, byteOrder);
      Array timeArray = Array.factory(dataType.getPrimitiveClassType(), shape, data);

      // now transform into an ISO Date String
      String[] stringData = new String[(int) timeArray.getSize()];
      int count = 0;
      while (timeArray.hasNext()) {
        long time = timeArray.nextLong();
        stringData[count++] = headerParser.formatter.toDateTimeStringISO(new Date(time));
      }
      return Array.factory(String.class, shape, stringData);
    }

    if (typeInfo.hdfType == 8) { // enum
      Object data = IospHelper.readDataFill( myRaf, layout, dataType, fillValue, byteOrder);
      Array codesArray = Array.factory(dataType.getPrimitiveClassType(), shape, data);
      return codesArray;

      /* LOOK should it return the string, or the integer ??
      // now transform into a String array
      String[] stringData = new String[(int) codesArray.getSize()];
      int count = 0;
      while (codesArray.hasNext()) {
        int code = codesArray.nextInt();
        String s = vinfo.enumMap.get(code);
        stringData[count++] = (s == null) ? "" : s;
      }
      return Array.factory(String.class, shape, stringData); */
    }

    if ((typeInfo.hdfType == 9) && !typeInfo.isVString) { // vlen (not string)  LOOK NOT TESTED!!!
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

    if (dataType == DataType.STRUCTURE) {  // LOOK subset
      boolean hasStrings = false;
      Structure s = (Structure) v;

      // create StructureMembers - must set offsets
      StructureMembers sm = s.makeStructureMembers();
      for (StructureMembers.Member m : sm.getMembers()) {
        Variable v2 = s.findVariable(m.getName());
        H5header.Vinfo vm = (H5header.Vinfo) v2.getSPobject();
        if (typeInfo.byteOrder >= 0) // apparently each member may have seperate byte order (!!!??)
          m.setDataObject(typeInfo.byteOrder == RandomAccessFile.LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        m.setDataParam((int) (vm.dataPos)); // offset since start of Structure
        if (v2.getDataType() == DataType.STRING)
          hasStrings = true;
      }
      int recsize = layout.getElemSize();
      sm.setStructureSize(recsize); // gotta calculate this ourself

      // place data into an ArrayStructureBB for efficiency
      ArrayStructureBB asbb = new ArrayStructureBB(sm, shape);
      byte[] byteArray = asbb.getByteBuffer().array();
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null) continue;
        if (debugStructure)
          System.out.println(" readStructure " + v.getName() + " chunk= " + chunk + " index.getElemSize= " + layout.getElemSize());
        // copy bytes directly into the underlying byte[]
        myRaf.seek(chunk.getSrcPos());
        myRaf.read(byteArray, (int) chunk.getDestElem() * recsize, chunk.getNelems() * recsize);
      }

      // strings are stored on the heap, and must be read separately
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
          if (debugStructure) System.out.println(" readStructure " + v.getName() + " chunk.getSrcPos= " +
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
        m.setDataObject( ByteOrder.nativeOrder()); // the string index is always written in "native order"
        int size = m.getSize();
        int destPos = pos  + m.getDataParam();
        String[] result = new String[size];
        for (int i = 0; i < size; i++)
          result[i] = headerParser.readHeapString(bb, destPos + i * 16); // 16 byte "heap ids" are in the ByteBuffer

        int index = asbb.addObjectToHeap( result);
        bb.order( ByteOrder.nativeOrder()); // the string index is always written in "native order"
        bb.putInt(destPos, index); // overwrite with the index into the StringHeap
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

    if (dataType == DataType.STRING) {
      int size = (int) layout.getTotalNelems();
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

  // old way
  private StructureData readStructure(Structure s, ArrayStructureW asw, long dataPos) throws IOException, InvalidRangeException {
    StructureDataW sdata = new StructureDataW(asw.getStructureMembers());
    if (debug) System.out.println(" readStructure " + s.getName() + " dataPos = " + dataPos);

    for (Variable v2 : s.getVariables()) {
      H5header.Vinfo vinfo = (H5header.Vinfo) v2.getSPobject();
      if (debug) System.out.println(" readStructureMember " + v2.getName() + " vinfo = " + vinfo);
      Array dataArray = readData(v2, dataPos + vinfo.dataPos, v2.getShapeAsSection());
      sdata.setMemberData(v2.getShortName(), dataArray);
    }

    return sdata;
  }

  //////////////////////////////////////////////////////////////////////////
  // utilities

  /**
   * Close the file.
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

  public String getDetailInfo() {
    ByteArrayOutputStream ff = new ByteArrayOutputStream(100 * 1000);
    try {
      headerParser.read( new PrintStream(ff));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return ff.toString();
  }

}