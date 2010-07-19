/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.iosp.hdf5;

import ucar.ma2.*;

import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.iosp.*;
import ucar.nc2.iosp.hdf4.HdfEos;
import ucar.nc2.iosp.hdf4.H4header;
import ucar.nc2.*;

import java.io.IOException;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;
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
    H4header.setDebugFlags(debugFlag);
  }

  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return H5header.isValidFile(raf);
  }

  public String getFileTypeId() {
    return "HDF5";
  }

  public String getFileTypeDescription() {
    return "Hierarchical Data Format, version 5";
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
    Layout layout;

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
      layout = new H5tiledLayoutBB(v2, wantSection, myRaf, vinfo.mfp.getFilters(), bo);
      data = IospHelper.readDataFill((LayoutBB) layout, v2.getDataType(), vinfo.getFillValue());

    } else { // normal case
      if (debug) System.out.println("read variable " + v2.getName() + " vinfo = " + vinfo);

      DataType readDtype = v2.getDataType();
      int elemSize = v2.getElementSize();
      Object fillValue = vinfo.getFillValue();
      int byteOrder = vinfo.typeInfo.byteOrder;

      // fill in the wantSection
      wantSection = Section.fill(wantSection, v2.getShape());

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
        wantSection = wantSection.removeVlen(); // remove vlen dimension
      }

      if (vinfo.isChunked) {
        layout = new H5tiledLayout((H5header.Vinfo) v2.getSPobject(), readDtype, wantSection);
      } else {
        layout = new LayoutRegular(dataPos, elemSize, v2.getShape(), wantSection);
      }
      data = readData(vinfo, v2, layout, readDtype, wantSection.getShape(), fillValue, byteOrder);
    }

    if (data instanceof Array)
      return (Array) data;
    else if (dataType == DataType.STRUCTURE)
      return convertStructure((Structure) v2, layout, wantSection.getShape(), (byte[]) data);
    else
      return Array.factory(dataType.getPrimitiveClassType(), wantSection.getShape(), data);
  }

  /*
   * Read data subset from file for a variable, return Array or java primitive array.
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

    H5header.TypeInfo typeInfo = vinfo.typeInfo;

    // special processing
    if (typeInfo.hdfType == 2) { // time
      Object data = IospHelper.readDataFill(myRaf, layout, dataType, fillValue, byteOrder, true);
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
      Object data = IospHelper.readDataFill(myRaf, layout, dataType, fillValue, byteOrder);
      return Array.factory(dataType.getPrimitiveClassType(), shape, data);
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
      Object[] data = new Object[(int) layout.getTotalNelems()];
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
      return (scalar) ? data[0] : new ArrayObject(data[0].getClass(), shape, data);

    }

    /* if (dataType == DataType.STRUCTURE) {  // LOOK what about subset ?
      boolean hasStrings = false;
      Structure s = (Structure) v;

      // create StructureMembers - must set offsets
      StructureMembers sm = s.makeStructureMembers();
      for (StructureMembers.Member m : sm.getMembers()) {
        Variable v2 = s.findVariable(m.getName());
        H5header.Vinfo vm = (H5header.Vinfo) v2.getSPobject();
        if (vm.typeInfo.byteOrder >= 0) // apparently each member may have seperate byte order (!!!??)
          m.setDataObject(vm.typeInfo.byteOrder == RandomAccessFile.LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
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
        // copy bytes directly into the underlying byte[] LOOK : assumes contiguous layout ??
        myRaf.seek(chunk.getSrcPos());
        myRaf.read(byteArray, (int) chunk.getDestElem() * recsize, chunk.getNelems() * recsize);
      }

      // strings are stored on the heap, and must be read separately
      if (hasStrings) {
        int destPos = 0;
        for (int i = 0; i < layout.getTotalNelems(); i++) { // loop over each structure
          convertStrings(asbb, destPos, sm);
          destPos += layout.getElemSize();
        }
      }
      return asbb;
    } // */

    if (dataType == DataType.STRUCTURE) {  // LOOK what about subset ?
      int recsize = layout.getElemSize();
      long size = recsize * layout.getTotalNelems();
      byte[] byteArray = new byte[(int)size];
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null) continue;
        if (debugStructure)
          System.out.println(" readStructure " + v.getName() + " chunk= " + chunk + " index.getElemSize= " + layout.getElemSize());
        // copy bytes directly into the underlying byte[] LOOK : assumes contiguous layout ??
        myRaf.seek(chunk.getSrcPos());
        myRaf.read(byteArray, (int) chunk.getDestElem() * recsize, chunk.getNelems() * recsize);
      }

      // place data into an ArrayStructureBB
      return convertStructure((Structure) v, layout, shape, byteArray);
    }

    // normal case
    return readDataPrimitive(layout, dataType, shape, fillValue, byteOrder, true);
  }

  Array convertReference(Array refArray) throws java.io.IOException {
    int nelems = (int) refArray.getSize();
    Index ima = refArray.getIndex();
    String[] result = new String[nelems];
    for (int i = 0; i < nelems; i++) {
      long reference = refArray.getLong(ima.set(i));
      result[i] = headerParser.getDataObjectName(reference);
      if (debugVlen) System.out.printf(" convertReference 0x%x to %s %n", reference, result[i]);
    }
    return Array.factory(String.class, new int[]{nelems}, result);
  }

  private ArrayStructure convertStructure(Structure s, Layout layout, int[] shape, byte[] byteArray) throws IOException {
    boolean hasStrings = false;

    // create StructureMembers - must set offsets
    StructureMembers sm = s.makeStructureMembers();
    for (StructureMembers.Member m : sm.getMembers()) {
      Variable v2 = s.findVariable(m.getName());
      H5header.Vinfo vm = (H5header.Vinfo) v2.getSPobject();
      if (vm.typeInfo.byteOrder >= 0) // apparently each member may have seperate byte order (!!!??)
        m.setDataObject(vm.typeInfo.byteOrder == RandomAccessFile.LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
      m.setDataParam((int) (vm.dataPos)); // offset since start of Structure
      if (v2.getDataType() == DataType.STRING)
        hasStrings = true;
    }
    int recsize = layout.getElemSize();
    sm.setStructureSize(recsize); // gotta calculate this ourself

    // place data into an ArrayStructureBB
    ByteBuffer bb = ByteBuffer.wrap(byteArray);
    ArrayStructureBB asbb = new ArrayStructureBB(sm, shape, bb, 0);

    // strings are stored on the heap, and must be read separately
    if (hasStrings) {
      int destPos = 0;
      for (int i = 0; i < layout.getTotalNelems(); i++) { // loop over each structure
        convertStrings(asbb, destPos, sm);
        destPos += layout.getElemSize();
      }
    }
    return asbb;
  }

  void convertStrings(ArrayStructureBB asbb, int pos, StructureMembers sm) throws java.io.IOException {
    ByteBuffer bb = asbb.getByteBuffer();
    for (StructureMembers.Member m : sm.getMembers()) {
      if (m.getDataType() == DataType.STRING) {
        m.setDataObject(ByteOrder.nativeOrder()); // the string index is always written in "native order"
        int size = m.getSize();
        int destPos = pos + m.getDataParam();
        String[] result = new String[size];
        for (int i = 0; i < size; i++)
          result[i] = headerParser.readHeapString(bb, destPos + i * 16); // 16 byte "heap ids" are in the ByteBuffer

        int index = asbb.addObjectToHeap(result);
        bb.order(ByteOrder.nativeOrder()); // the string index is always written in "native order"
        bb.putInt(destPos, index); // overwrite with the index into the StringHeap
      }
    }
  }

  /*
   * Read data subset from file for a variable, create primitive array.
   *
   * @param layout     handles skipping around in the file.
   * @param dataType  dataType of the variable
   * @param shape     the shape of the output
   * @param fillValue fill value as a wrapped primitive
   * @param byteOrder byte order
   * @return primitive array with data read in
   * @throws java.io.IOException            if read error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   */
  Object readDataPrimitive(Layout layout, DataType dataType, int[] shape, Object fillValue, int byteOrder, boolean convertChar) throws java.io.IOException, InvalidRangeException {

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

    if (dataType == DataType.OPAQUE) {
      Array opArray = new ArrayObject(ByteBuffer.class, shape);
      assert (new Section(shape).computeSize() == layout.getTotalNelems());

      int count = 0;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null) continue;
        int recsize = layout.getElemSize();
        for (int i = 0; i < chunk.getNelems(); i++) {
          byte[] pa = new byte[recsize];
          myRaf.seek(chunk.getSrcPos() + i * recsize);
          myRaf.read(pa, 0, recsize);
          opArray.setObject(count++, ByteBuffer.wrap(pa));
        }
      }
      return opArray;
    }

    // normal case
    return IospHelper.readDataFill(myRaf, layout, dataType, fillValue, byteOrder, convertChar);
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
   *
   * @throws IOException on io error
   */
  public void close() throws IOException {
    if (myRaf != null) {
      myRaf.close();
      // log.warn("H5iosp.close called on "+myRaf.getLocation()+" for ncfile="+ncfile.hashCode()+" for iosp="+this.hashCode());
    }
    myRaf = null;
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
      NetcdfFile ncfile = new FakeNetcdfFile();
      H5header detailParser = new H5header(myRaf, ncfile, this);
      detailParser.read(new PrintStream(ff));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return ff.toString();
  }

  static private class FakeNetcdfFile extends NetcdfFile {
  }

  // debug
  public Object sendIospMessage(Object message) {
    if (message.toString().equals("header"))
      return headerParser;
    
    if (message.toString().equals("headerEmpty")) {
      NetcdfFile ncfile = new FakeNetcdfFile();
      H5header headerEmpty = new H5header(myRaf, ncfile, this);
      return headerEmpty;
    }

    return super.sendIospMessage(message);
  }

}