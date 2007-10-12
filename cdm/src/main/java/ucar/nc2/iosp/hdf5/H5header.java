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

import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Format;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.*;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.ma2.*;

import java.util.*;
import java.text.*;
import java.io.IOException;
import java.nio.*;

/**
 * Read all of the metadata of an HD5 file.
 *
 * @author caron
 */

/* Implementation notes
 * any field called address is actually reletive to the base address.
 * any field called filePos or dataPos is a byte offset withing the file.
 */
class H5header {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5header.class);

  // debugging
  static private boolean debugEnum = true;
  static private boolean debug1 = false, debugDetail = false, debugPos = false, debugHeap = false, debugV = false;
  static private boolean debugGroupBtree = false, debugDataBtree = false, debugDataChunk = false, debugBtree2 = false, debugFractalHeap = false;
  static private boolean debugContinueMessage = false, debugTracker = false, debugSymbolTable = false;
  static private boolean warnings = true, debugReference = false, debugCreationOrder = false;
  static java.io.PrintStream debugOut = System.out;

  static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debug1 = debugFlag.isSet("H5header/header");
    debugBtree2 = debugFlag.isSet("H5header/btree2");
    debugContinueMessage = debugFlag.isSet("H5header/continueMessage");
    debugDetail = debugFlag.isSet("H5header/headerDetails");
    debugDataBtree = debugFlag.isSet("H5header/dataBtree");
    debugGroupBtree = debugFlag.isSet("H5header/groupBtree");
    debugHeap = debugFlag.isSet("H5header/Heap");
    debugPos = debugFlag.isSet("H5header/filePos");
    debugReference = debugFlag.isSet("H5header/reference");
    debugSymbolTable = debugFlag.isSet("H5header/symbolTable");
    debugTracker = debugFlag.isSet("H5header/memTracker");
    debugV = debugFlag.isSet("H5header/Variable");
  }

  static private final byte[] head = {(byte) 0x89, 'H', 'D', 'F', '\r', '\n', 0x1a, '\n'};
  static private final String shead = new String(head);
  static private final long maxHeaderPos = 500000; // header's gotta be within this
  static private boolean transformReference = true;

  static boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    long filePos = 0;
    long size = raf.length();
    byte[] b = new byte[8];

    // search forward for the header
    while ((filePos < size) && (filePos < maxHeaderPos)) {
      raf.seek(filePos);
      raf.read(b);
      String magic = new String(b);
      if (magic.equals(shead))
        return true;
      filePos = (filePos == 0) ? 512 : 2 * filePos;
    }

    return false;
  }

  ////////////////////////////////////////////////////////////////////////////////

  private RandomAccessFile raf;
  private ucar.nc2.NetcdfFile ncfile;
  private H5iosp h5iosp;

  private long actualSize, baseAddress;
  private byte sizeOffsets, sizeLengths;
  private boolean isOffsetLong, isLengthLong;

  private DataObject rootObject;
  private Map<String, DataObject> hashDataObjects = new HashMap<String, DataObject>(100);
  private List<DataObject> obsList = new ArrayList<DataObject>();

  private Map<Long, Group> hashGroups = new HashMap<Long, Group>(100);

  private MemTracker memTracker;

  H5header(RandomAccessFile myRaf, ucar.nc2.NetcdfFile ncfile, H5iosp h5iosp)  {
    this.ncfile = ncfile;
    this.raf = myRaf;
    this.h5iosp = h5iosp;
  }

  void read() throws IOException {
    actualSize = raf.length();
    memTracker = new MemTracker(actualSize);

    if (!isValidFile( raf))
      throw new IOException("Not a netCDF4/HDF5 file ");
    if (debug1) debugOut.println("H5header 0pened file to read:'" + ncfile.getLocation() + "', size=" + actualSize);
    // now we are positioned right after the header

    // header information is in le byte order
    raf.order(RandomAccessFile.LITTLE_ENDIAN);

    long superblockStart = raf.getFilePointer() - 8;
    memTracker.add("header", 0, superblockStart);

    // superblock version
    byte versionSB = raf.readByte();

    if (versionSB < 2) {
      readSuperBlock1(superblockStart, versionSB);
    } else if (versionSB == 2) {
      readSuperBlock2(superblockStart);
    } else {
      throw new IOException("Unknown superblock version= " + versionSB);
    }

    // now look for symbolic links
    findSymbolicLinks(rootObject.group);

    // recursively run through all the dataObjects and add them to the ncfile
    makeNetcdfGroup(ncfile.getRootGroup(), rootObject);

    if (debugReference) {
      System.out.println("DataObjects");
      for (DataObject ob : obsList)
        System.out.println("  " + ob.name + " address= " + ob.address + " filePos= " + getFileOffset(ob.address));
    }
    if (debugTracker) memTracker.report();
  }

  DataObject findDataObject(long id) {
    for (DataObject dobj : obsList)
      if (dobj.address == id) return dobj;
    return null;
  }

  private void readSuperBlock2(long superblockStart) throws IOException {
    sizeOffsets = raf.readByte();
    isOffsetLong = (sizeOffsets == 8);

    sizeLengths = raf.readByte();
    isLengthLong = (sizeLengths == 8);
    if (debugDetail) debugOut.println(" sizeOffsets= " + sizeOffsets + " sizeLengths= " + sizeLengths);
    if (debugDetail) debugOut.println(" isLengthLong= " + isLengthLong + " isOffsetLong= " + isOffsetLong);

    byte fileFlags = raf.readByte();
    if (debugDetail) debugOut.println(" fileFlags= 0x" + Integer.toHexString(fileFlags));

    baseAddress = readOffset();
    long extensionAddress = readOffset();
    long eofAddress = readOffset();
    long rootObjectAddress = readOffset();
    int checksum = raf.readInt();

    if (debugDetail) {
      debugOut.println(" baseAddress= 0x" + Long.toHexString(baseAddress));
      debugOut.println(" extensionAddress= 0x" + Long.toHexString(extensionAddress));
      debugOut.println(" eof Address=" + eofAddress);
      debugOut.println(" rootObjectAddress= 0x" + Long.toHexString(rootObjectAddress));
      debugOut.println();
    }

    memTracker.add("superblock", superblockStart, raf.getFilePointer());

    if (baseAddress != superblockStart) {
      baseAddress = superblockStart;
      eofAddress += superblockStart;
      if (debugDetail) debugOut.println(" baseAddress set to superblockStart");
    }

    // look for file truncation
    long fileSize = raf.length();
    if (fileSize < eofAddress)
      throw new IOException("File is truncated should be= " + eofAddress + " actual = " + fileSize);

    rootObject = new DataObject(null, "", rootObjectAddress);
    rootObject.read();
  }

  void readSuperBlock1(long superblockStart, byte versionSB) throws IOException {
    byte versionFSS, versionGroup, versionSHMF;
    short btreeLeafNodeSize, btreeInternalNodeSize, storageInternalNodeSize;
    int fileFlags;

    long heapAddress;
    long eofAddress;
    long driverBlockAddress;

    versionFSS = raf.readByte();
    versionGroup = raf.readByte();
    raf.readByte(); // skip 1 byte
    versionSHMF = raf.readByte();
    if (debugDetail)
      debugOut.println(" versionSB= " + versionSB + " versionFSS= " + versionFSS + " versionGroup= " + versionGroup +
              " versionSHMF= " + versionSHMF);

    sizeOffsets = raf.readByte();
    isOffsetLong = (sizeOffsets == 8);

    sizeLengths = raf.readByte();
    isLengthLong = (sizeLengths == 8);
    if (debugDetail) debugOut.println(" sizeOffsets= " + sizeOffsets + " sizeLengths= " + sizeLengths);
    if (debugDetail) debugOut.println(" isLengthLong= " + isLengthLong + " isOffsetLong= " + isOffsetLong);

    raf.read(); // skip 1 byte
    //debugOut.println(" position="+mapBuffer.position());

    btreeLeafNodeSize = raf.readShort();
    btreeInternalNodeSize = raf.readShort();
    if (debugDetail)
      debugOut.println(" btreeLeafNodeSize= " + btreeLeafNodeSize + " btreeInternalNodeSize= " + btreeInternalNodeSize);
    ;
    //debugOut.println(" position="+mapBuffer.position());

    fileFlags = raf.readInt();
    if (debugDetail) debugOut.println(" fileFlags= 0x" + Integer.toHexString(fileFlags));

    if (versionSB == 1) {
      storageInternalNodeSize = raf.readShort();
      raf.skipBytes(2);
    }

    baseAddress = readOffset();
    heapAddress = readOffset();
    eofAddress = readOffset();
    driverBlockAddress = readOffset();

    if (baseAddress != superblockStart) {
      baseAddress = superblockStart;
      eofAddress += superblockStart;
      if (debugDetail) debugOut.println(" baseAddress set to superblockStart");
    }

    if (debugDetail) {
      debugOut.println(" baseAddress= 0x" + Long.toHexString(baseAddress));
      debugOut.println(" global free space heap Address= 0x" + Long.toHexString(heapAddress));
      debugOut.println(" eof Address=" + eofAddress);
      debugOut.println(" driver BlockAddress= 0x" + Long.toHexString(driverBlockAddress));
      debugOut.println();
    }
    memTracker.add("superblock", superblockStart, raf.getFilePointer());

    // look for file truncation
    long fileSize = raf.length();
    if (fileSize < eofAddress)
      throw new IOException("File is truncated should be= " + eofAddress + " actual = " + fileSize);

    // next comes the root objext's SymbolTableEntry
    SymbolTableEntry rootEntry = new SymbolTableEntry(raf.getFilePointer());

    // extract the root group object, recursively read all objects
    long rootObjectAddress = rootEntry.getObjectAddress();
    rootObject = new DataObject(null, "", rootObjectAddress);
    rootObject.read();

    // LOOK what is this ??
    if (rootObject.group == null) {
      // if the root object doesnt have a group message, check if the rootEntry is cache type 2
      if (rootEntry.btreeAddress != 0) {
        rootObject.group = new GroupOld(null, "", rootEntry.btreeAddress, rootEntry.nameHeapAddress);
      } else {
        throw new IllegalStateException("root object not a group");
      }
    }
  }

  private void findSymbolicLinks(Group group) {
    if (group == null) return;

    List<DataObject> nolist = group.nestedObjects;
    for (int i = 0; i < nolist.size(); i++) {
      DataObject ndo = nolist.get(i);

      if (ndo.group != null) {
        findSymbolicLinks(ndo.group);
      } else {

        if (ndo.linkName != null) { // its a symbolic link
          DataObject link = hashDataObjects.get(ndo.linkName);
          if (link == null) {
            debugOut.println(" WARNING Didnt find symbolic link=" + ndo.linkName + " from " + ndo.name);
            nolist.remove(i);
            continue;
          }

          // dont allow loops
          if (link.group != null) {
            if (ndo.isChildOf(link.group)) {
              debugOut.println(" ERROR No loops allowed=" + ndo.name);
              nolist.remove(i);
              continue;
            }
          }

          nolist.set(i, link);
          if (debugSymbolTable) debugOut.println("  Found symbolic link=" + ndo.linkName + " from " + ndo.name);
        }

      }
    }
  }

  /////////////////////////////////////////////////////////////////////

  /**
   * Holder of all H5 specific information for a Variable, needed to do IO.
   */
  class Vinfo {
    Variable owner; // debugging
    DataObject ndo; // debugging

    long dataPos; // for regular variables, needs to be absolute, with baseAddress added if needed
    // for member variables, is the offset from start of structure

    int hdfType;
    DataType dataType = null;
    int byteOrder = -1;
    boolean signed = true;
    int byteSize, vpad;
    int[] storageSize;  // for type 1 (continuous) : (varDims, elemSize)
    // for type 2 (chunked)    : (chunkDim, elemSize)
    // null for attributs

    // chunked stuff
    boolean isChunked = false;
    DataBTree btree = null; // only if isChunked

    MessageDatatype mdt;
    MessageDataspace mds;
    MessageFilter mfp;

    boolean useFillValue = false;
    byte[] fillValue;

    /**
     * Constructor
     *
     * @param ndo DataObject. always has an mdt and an msl
     * @throws java.io.IOException on read error
     */
    Vinfo(DataObject ndo) throws IOException {
      this.ndo = ndo;
      this.hdfType = ndo.mdt.type;
      this.byteSize = ndo.mdt.byteSize;
      this.dataPos = getFileOffset(ndo.msl.dataAddress);
      this.mdt = ndo.mdt;
      this.mds = ndo.mds;
      this.mfp = ndo.mfp;

      if (!ndo.mdt.isOK) {
        debugOut.println("WARNING HDF5 file " + ncfile.getLocation() + " not handling " + ndo.mdt);
        return; // not a supported datatype
      }

      this.isChunked = (ndo.msl.type == 2); // chunked vs. continuous storage
      this.storageSize = (isChunked) ? ndo.msl.chunkSize : ndo.mds.dimLength;

      // figure out the data type
      calcNCtype(ndo.mdt);
    }

    /**
     * Constructor, used for reading attributes
     *
     * @param mdt     datatype
     * @param dataPos start of data in file
     * @throws java.io.IOException on read error
     */
    Vinfo(MessageDatatype mdt, MessageDataspace mds, long dataPos) throws IOException {
      this.mdt = mdt;
      this.mds = mds;
      this.byteSize = mdt.byteSize;
      this.dataPos = dataPos;

      if (!mdt.isOK) {
        debugOut.println("WARNING HDF5 file " + ncfile.getLocation() + " not handling " + mdt);
        return; // not a supported datatype
      }

      // figure out the data type
      this.hdfType = mdt.type;
      calcNCtype(mdt);
    }

    void setOwner(Variable owner) {
      this.owner = owner;
      if (btree != null) btree.setOwner(owner);
    }

    private void calcNCtype(MessageDatatype mdt) {
      int hdfType = mdt.type;
      int byteSize = mdt.byteSize;
      byte[] flags = mdt.flags;

      if (hdfType == 0) { // int, long, short, byte
        dataType = getNCtype(hdfType, byteSize);
        byteOrder = ((flags[0] & 1) == 0) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;
        signed = ((flags[0] & 8) != 0);

      } else if (hdfType == 1) { // floats, doubles
        dataType = getNCtype(hdfType, byteSize);
        byteOrder = ((flags[0] & 1) == 0) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;

      } else if (hdfType == 3) { // fixed length strings. String is used for Vlen type = 1
        dataType = DataType.CHAR;

      } else if (hdfType == 4) { // bit field
        dataType = getNCtype(hdfType, byteSize);

      } else if (hdfType == 5) { // opaque
        dataType = DataType.OPAQUE;

      } else if (hdfType == 6) { // structure
        dataType = DataType.STRUCTURE;

      } else if (hdfType == 7) { // reference
        byteOrder = RandomAccessFile.LITTLE_ENDIAN;
        dataType = DataType.STRING;

      } else if (hdfType == 8) { // enums
        dataType = DataType.ENUM;

      } else if (hdfType == 9) { // variable length array
        if (mdt.isVString) {
          vpad = ((flags[0] >> 4) & 0xf);
          dataType = DataType.STRING;
        } else {
          dataType = getNCtype(mdt.getBaseType(), mdt.getBaseSize());
        }
      } else if (hdfType == 10) { // array
        byteOrder = (mdt.getFlags()[0] & 1) == 0 ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;
        if ((mdt.parent.type == 9) && mdt.parent.isVString) {
          dataType = DataType.STRING;
        } else
          dataType = getNCtype(mdt.getBaseType(), mdt.getBaseSize());

      } else {
        debugOut.println("WARNING not handling hdf dataType = " + hdfType + " size= " + byteSize);
      }
    }

    private DataType getNCtype(int hdfType, int size) {
      if ((hdfType == 0) || (hdfType == 4)) { // integer, bit field
        if (size == 1)
          return DataType.BYTE;
        else if (size == 2)
          return DataType.SHORT;
        else if (size == 4)
          return DataType.INT;
        else if (size == 8)
          return DataType.LONG;
        else {
          debugOut.println("WARNING HDF5 file " + ncfile.getLocation() + " not handling hdf integer type (" + hdfType + ") with size= " + size);
          log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf integer type (" + hdfType + ") with size= " + size);
          return null;
        }

      } else if (hdfType == 1) {
        if (size == 4)
          return DataType.FLOAT;
        else if (size == 8)
          return DataType.DOUBLE;
        else {
          debugOut.println("WARNING HDF5 file " + ncfile.getLocation() + " not handling hdf float type with size= " + size);
          log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf float type with size= " + size);
          return null;
        }

      } else if (hdfType == 3) {  // fixed length strings. String is used for Vlen type = 1
        return DataType.CHAR;

      } else if (hdfType == 7) { // reference
        return DataType.STRING;
        
      } else {
        debugOut.println("WARNING not handling hdf type = " + hdfType + " size= " + size);
        log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf type = " + hdfType + " size= " + size);
        return null;
      }
    }

    public String toString() {
      StringBuffer buff = new StringBuffer();
      buff.append("dataPos=").append(dataPos).append(" byteSize=").append(byteSize).append(" datatype=").append(dataType).append(" (").append(hdfType).append(")");
      if (isChunked) {
        buff.append(" isChunked (");
        for (int size : storageSize) buff.append(size).append(" ");
        buff.append(")");
      }
      if (mfp != null) buff.append(" hasFilter");
      buff.append("; // ").append(extraInfo());
      if (null != ndo)
        buff.append("\n").append(ndo);

      return buff.toString();
    }

    public String extraInfo() {
      StringBuffer buff = new StringBuffer();
      if ((dataType != DataType.CHAR) && (dataType != DataType.STRING))
        buff.append(signed ? " signed" : " unsigned");
      if (byteOrder >= 0)
        buff.append((byteOrder == RandomAccessFile.LITTLE_ENDIAN) ? " LittleEndian" : " BigEndian");
      if (useFillValue)
        buff.append(" useFillValue");
      return buff.toString();
    }

    DataType getNCDataType() {
      return dataType;
    }


    /**
     * Get the Fill Value, return default if one was not set.
     * @return wrapped primitive (Byte, Short, Integer, Double, Float, Long), or null if none
     */
    Object getFillValue() {
      return (fillValue == null) ? getFillValueDefault() : getFillValueNonDefault();
    }

    Object getFillValueDefault() {
      if (dataType == DataType.BYTE) return N3iosp.NC_FILL_BYTE;
      if (dataType == DataType.CHAR) return (byte) 0;
      if (dataType == DataType.SHORT) return N3iosp.NC_FILL_SHORT;
      if (dataType == DataType.INT) return N3iosp.NC_FILL_INT;
      if (dataType == DataType.LONG) return N3iosp.NC_FILL_LONG;
      if (dataType == DataType.FLOAT) return N3iosp.NC_FILL_FLOAT;
      if (dataType == DataType.DOUBLE) return N3iosp.NC_FILL_DOUBLE;
      return null;
    }

    Object getFillValueNonDefault() {
      if (fillValue == null) return null;

      if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR))
        return fillValue[0];

      ByteBuffer bbuff = ByteBuffer.wrap(fillValue);
      if (byteOrder >= 0)
        bbuff.order(byteOrder == RandomAccessFile.LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

      if (dataType == DataType.SHORT) {
        ShortBuffer tbuff = bbuff.asShortBuffer();
        return tbuff.get();

      } else if (dataType == DataType.INT) {
        IntBuffer tbuff = bbuff.asIntBuffer();
        return tbuff.get();

      } else if (dataType == DataType.LONG) {
        LongBuffer tbuff = bbuff.asLongBuffer();
        return tbuff.get();

      } else if (dataType == DataType.FLOAT) {
        FloatBuffer tbuff = bbuff.asFloatBuffer();
        return tbuff.get();

      } else if (dataType == DataType.DOUBLE) {
        DoubleBuffer tbuff = bbuff.asDoubleBuffer();
        return tbuff.get();
      }

      return null;
    }


  } // Vinfo

  /* private class Vatt {
    String name;
    List<Dimension> dimList;

    Vatt(String name, List<Dimension> dimList) {
      this.name = name;
      this.dimList = dimList;
    }
  } */

  HeapIdentifier getHeapIdentifier(long address) throws IOException {
    return new HeapIdentifier(address);
  }

  private Map<Long, GlobalHeap> heapMap = new HashMap<Long, GlobalHeap>();

  class HeapIdentifier {
    private int nelems; // "number of 'base type' elements in the sequence in the heap"
    private long heapAddress;
    private int index;

    HeapIdentifier(long address) throws IOException {
      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(getFileOffset(address));

      nelems = raf.readInt();
      heapAddress = readOffset();
      index = raf.readInt();
      if (debugDetail)
        debugOut.println("   read HeapIdentifier address=" + address + this);
      if (debugHeap) dump("heapIdentifier", getFileOffset(address), 16, true);
    }

    public String toString() {
      return " nelems=" + nelems + " heapAddress=" + heapAddress + " index=" + index;
    }

    GlobalHeap.HeapObject getHeapObject() throws IOException {
      GlobalHeap gheap;
      if (null == (gheap = heapMap.get(heapAddress))) {
        gheap = new GlobalHeap(heapAddress);
        heapMap.put(heapAddress, gheap);
      }

      for (GlobalHeap.HeapObject ho : gheap.hos) {
        if (ho.id == index)
          return ho;
      }
      throw new IllegalStateException("cant find HeapObject");
    }

  } // HeapIdentifier

  class RegionReference {
    private long heapAddress;
    private int index;

    RegionReference(long filePos) throws IOException {
      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(filePos);
      heapAddress = readOffset();
      index = raf.readInt();

      GlobalHeap gheap;
      if (null == (gheap = heapMap.get(heapAddress))) {
        gheap = new GlobalHeap(heapAddress);
        heapMap.put(heapAddress, gheap);
      }

      GlobalHeap.HeapObject want;
      for (GlobalHeap.HeapObject ho : gheap.hos) {
        if (ho.id == index) {
          want = ho;
          System.out.println(" found ho=" + ho);
          /* - The offset of the object header of the object (ie. dataset) pointed to (yes, an object ID)
- A serialized form of a dataspace _selection_ of elements (in the dataset pointed to).
I don't have a formal description of this information now, but it's encoded in the H5S_<foo>_serialize() routines in
src/H5S<foo>.c, where foo = {all, hyper, point, none}.
There is _no_ datatype information stored for these sort of selections currently. */
          raf.seek(ho.dataPos);
          long objId = raf.readLong();
          DataObject ndo = findDataObject(objId);
          String what = (ndo == null) ? "none" : ndo.getName();
          System.out.println(" objId=" + objId + " DataObject= " + what);

          return;
        }
      }
      throw new IllegalStateException("cant find HeapObject");
    }

  } // RegionReference

  private class StructureMember {
    String name;
    int offset;
    byte dims;
    MessageDatatype mdt;

    StructureMember(int version, int byteSize) throws IOException {
      if (debugPos) debugOut.println("   *StructureMember now at position=" + raf.getFilePointer());

      name = readString(raf);
      if (version < 3) {
        raf.skipBytes(padding(name.length() + 1, 8));
        offset = raf.readInt();
      } else {
        offset = (int) readVariableSizeMax(byteSize);
      }

      if (debug1) debugOut.println("   Member name=" + name + " offset= " + offset);
      if (version == 1) {
        dims = raf.readByte();
        raf.skipBytes(3);
        raf.skipBytes(24); // ignore dimension info for now
      }

      //HDFdumpWithCount(buffer, raf.getFilePointer(), 16);
      mdt = new MessageDatatype();
      mdt.read();
      if (debugDetail) debugOut.println("   ***End Member name=" + name);

      // ??
      //HDFdump(ncfile.out, "Member end", buffer, 16);
      //if (HDFdebug)  ncfile.debugOut.println("   Member pos="+raf.getFilePointer());
      //HDFpadToMultiple( buffer, 8);
      //if (HDFdebug)  ncfile.debugOut.println("   Member padToMultiple="+raf.getFilePointer());
      //raf.skipBytes( 4); // huh ??
    }
  }

  //////////////////////////////////////////////////////////////
  // 2A "data object header" section IV.A (p 19)
  // A Group, a link or a Variable

  class DataObject {
    Group parent;
    String name;
    long address; // aka object id

    String displayName;
    List<Message> messages = new ArrayList<Message>();
    List<Message> dimMessages = new ArrayList<Message>();

    byte version; // 1 or 2
    //short nmess;
    //int referenceCount;
    //long headerSize;

    // its a
    private Group group;

    // or link
    String linkName = null;

    // or
    boolean isVariable;
    boolean isDimensionNotVariable;
    MessageDatatype mdt = null;
    MessageDataspace mds = null;
    MessageLayout msl = null;
    MessageFilter mfp = null;
    String dimList;

    DataObject(Group parent, String name, long address) {
      this.parent = parent;
      this.name = name;
      this.address = address;
      obsList.add(this);

      displayName = (name.length() == 0) ? "root" : name;
    }

    DataObject(Group parent, String name, String linkName) {
      this.parent = parent;
      this.name = name;
      this.linkName = linkName;
      displayName = name;
    }

    String getName() {
      return parent.getName() + "/" + name;
    }

    // is this a child of that ?
    private boolean isChildOf(Group that) {
      if (parent == that) return true;
      if (parent == null) return false;
      return parent.isChildOf(that);
    }

    // "Data Object Header" Level 2A
    // read a Data Object Header
    // no side effects, can be called multiple time for debugging
    private void read() throws IOException {
      if (debug1) debugOut.println("\n--> DataObject.read parsing <" + displayName + "> object ID/address=" + address);
      if (debugPos)
        debugOut.println("      DataObject.read now at position=" + raf.getFilePointer() + " for <" + displayName + "> reposition to " + getFileOffset(address));

      //if (offset < 0) return null;
      raf.seek(getFileOffset(address));

      version = raf.readByte();
      if (version == 1) { // Level 2A1 (first part, before the messages)
        raf.readByte(); // skip byte
        short nmess = raf.readShort();
        if (debugDetail) debugOut.println(" version=" + version + " nmess=" + nmess);

        int referenceCount = raf.readInt();
        int headerSize = raf.readInt();
        if (debugDetail) debugOut.println(" referenceCount=" + referenceCount + " headerSize=" + headerSize);

        //if (referenceCount > 1)
        //  debugOut.println("WARNING referenceCount="+referenceCount);
        raf.skipBytes(4); // header messages multiples of 8

        long posMess = raf.getFilePointer();
        int count = readMessagesVersion1(posMess, nmess, Integer.MAX_VALUE);
        if (debugContinueMessage) debugOut.println(" nmessages read = " + count);
        if (debugPos) debugOut.println("<--done reading messages for <" + name + ">; position=" + raf.getFilePointer());
        if (debugTracker) memTracker.addByLen("Object " + displayName, getFileOffset(address), headerSize + 16);

      } else { // level 2A2 (first part, before the messages)
        // first byte was already read
        byte[] name = new byte[3];
        raf.read(name);
        String nameS = new String(name);
        if (!nameS.equals("HDR"))
          throw new IllegalStateException("DataObject doesnt start with OHDR");

        version = raf.readByte();
        byte flags = raf.readByte(); // data object header flags (version 2)
        if (debugDetail) debugOut.println(" version=" + version + " flags=" + Integer.toBinaryString(flags));

        //raf.skipBytes(2);
        if (((flags >> 5) & 1) == 1) {
          int accessTime = raf.readInt();
          int modTime = raf.readInt();
          int changeTime = raf.readInt();
          int birthTime = raf.readInt();
        }
        if (((flags >> 4) & 1) == 1) {
          short maxCompactAttributes = raf.readShort();
          short minDenseAttributes = raf.readShort();
        }

        long sizeOfChunk = readVariableSizeFactor(flags & 3);
        if (debugDetail) debugOut.println(" sizeOfChunk=" + sizeOfChunk);

        long posMess = raf.getFilePointer();
        int count = readMessagesVersion2(posMess, sizeOfChunk, (flags & 4) != 0);
        if (debugContinueMessage) debugOut.println(" nmessages read = " + count);
        if (debugPos) debugOut.println("<--done reading messages for <" + name + ">; position=" + raf.getFilePointer());
      }

      // look for group or a datatype/dataspace/layout message
      MessageGroup groupMessage = null;
      MessageGroupNew groupNewMessage = null;

      for (Message mess : messages) {
        if (debugTracker) memTracker.addByLen("Message (" + displayName + ") " + mess.mtype, mess.start, mess.size + 8);

        if (mess.mtype == MessageType.SimpleDataspace)
          mds = (MessageDataspace) mess.messData;
        else if (mess.mtype == MessageType.Datatype)
          mdt = (MessageDatatype) mess.messData;
        else if (mess.mtype == MessageType.Layout)
          msl = (MessageLayout) mess.messData;
        else if (mess.mtype == MessageType.Group)
          groupMessage = (MessageGroup) mess.messData;
        else if (mess.mtype == MessageType.GroupNew)
          groupNewMessage = (MessageGroupNew) mess.messData;
        else if (mess.mtype == MessageType.FilterPipeline)
          mfp = (MessageFilter) mess.messData;
      }

      // if has a "group message", then its a group
      if (groupMessage != null) {
        // check for hard links
        // debugOut.println("HO look for group address = "+groupMessage.btreeAddress);
        if (null != (group = hashGroups.get(groupMessage.btreeAddress))) {
          debugOut.println("WARNING hard link to group = " + group.getName());
          if (parent.isChildOf(group)) {
            debugOut.println("ERROR hard link to group create a loop = " + group.getName());
            group = null;
            return;
          }
        }

        // read the group, and its contained data objects.
        group = new GroupOld(parent, name, groupMessage.btreeAddress, groupMessage.nameHeapAddress); // LOOK munge later

      }  // if has a "groupNewMessage", then its a groupNew
      else if (groupNewMessage != null) {
        // read the group, and its contained data objects.
        group = new GroupNew(parent, name, groupNewMessage, messages);
      }

      // if it has a Datatype and a StorageLayout, then its a Variable
      else if ((mdt != null) && (msl != null)) {
        isVariable = true;

      } else { // we dont know what it is
        debugOut.println("WARNING Unknown DataObject = " + displayName + " mdt = " + mdt + " msl  = " + msl);
        return;
      }

      if (debug1) debugOut.println("<-- end DataObject " + name);
    }

    // read messages, starting at pos, until you hit maxMess read, or maxBytes read
    // if you hit a continuation message, call recursively
    // return number of messaages read
    private int readMessagesVersion1(long pos, int maxMess, int maxBytes) throws IOException {
      if (debugContinueMessage)
        debugOut.println(" readMessages start at =" + pos + " maxMess= " + maxMess + " maxBytes= " + maxBytes);

      int count = 0;
      int bytesRead = 0;
      while ((count < maxMess) && (bytesRead < maxBytes)) {
        /* LOOK: MessageContinue not correct ??
        if (posMess >= actualSize)
          break; */

        Message mess = new Message();
        //messages.add( mess);
        int n = mess.read(pos, 1, false);
        pos += n;
        bytesRead += n;
        count++;
        if (debugContinueMessage) debugOut.println("   count=" + count + " bytesRead=" + bytesRead);

        // if we hit a continuation, then we go into nested reading
        if (mess.mtype == MessageType.ObjectHeaderContinuation) {
          MessageContinue c = (MessageContinue) mess.messData;
          if (debugContinueMessage) debugOut.println(" ---ObjectHeaderContinuation--- ");
          count += readMessagesVersion1(getFileOffset(c.offset), maxMess - count, (int) c.length);
          if (debugContinueMessage) debugOut.println(" ---ObjectHeaderContinuation return --- ");
        } else if (mess.mtype != MessageType.NIL) {
          messages.add(mess);
        }
      }
      return count;
    }

    private int readMessagesVersion2(long filePos, long maxBytes, boolean creationOrderPresent) throws IOException {
      if (debugContinueMessage)
        debugOut.println(" readMessages2 starts at =" + filePos + " maxBytes= " + maxBytes);

      // maxBytes is number of bytes of messages to be read. however, a message is at least 4 bytes long, so
      // we are done if we have read > maxBytes - 4. There appears to be an "off by one" possibility
      maxBytes -= 3;

      int count = 0;
      int bytesRead = 0;
      while (bytesRead < maxBytes) {

        Message mess = new Message();
        //messages.add( mess);
        int n = mess.read(filePos, 2, creationOrderPresent);
        filePos += n;
        bytesRead += n;
        count++;
        if (debugContinueMessage) debugOut.println("   mess size=" + n + " bytesRead=" + bytesRead+ " maxBytes="+maxBytes);

        // if we hit a continuation, then we go into nested reading
        if (mess.mtype == MessageType.ObjectHeaderContinuation) {
          MessageContinue c = (MessageContinue) mess.messData;
          long continuationBlockFilePos = getFileOffset(c.offset);
          if (debugContinueMessage) debugOut.println(" ---ObjectHeaderContinuation filePos= "+continuationBlockFilePos);

          raf.seek(continuationBlockFilePos);
          String sig = readStringFixedLength(4);
          if (!sig.equals("OCHK"))
            throw new IllegalStateException(" ObjectHeaderContinuation Missing signature");

          count += readMessagesVersion2(continuationBlockFilePos + 4, (int) c.length - 8, creationOrderPresent);
          if (debugContinueMessage) debugOut.println(" ---ObjectHeaderContinuation return --- ");
          if (debugContinueMessage) debugOut.println("   continuationMessages =" + count + " bytesRead=" + bytesRead+ " maxBytes="+maxBytes);

        } else if (mess.mtype != MessageType.NIL) {
          messages.add(mess);
        }
      }
      return count;
    }

    /*     void read() throws IOException {
      long pos = raf.getFilePointer();
      String sig = readString(4);
      if (sig.equals("OCHK")) {

      } else {
        raf.seek(pos);
        offset = readOffset();
        length = readLength();
      }

      if (debug1) debugOut.println("   Continue offset=" + offset + " length=" + length);
    } */

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("DataObject= ").append(name).append(" id= ").append(address);
      sbuff.append(" messages= ");
      for (Message message : messages)
        sbuff.append("\n  ").append(message);

      return sbuff.toString();
    }

  } // DataObject

  // type safe enum
  static private class MessageType {
    private static int MAX_MESSAGE = 23;
    private static java.util.Map<String, MessageType> hash = new java.util.HashMap<String, MessageType>(10);
    private static MessageType[] mess = new MessageType[MAX_MESSAGE];

    public final static MessageType NIL = new MessageType("NIL", 0);
    public final static MessageType SimpleDataspace = new MessageType("SimpleDataspace", 1);
    public final static MessageType GroupNew = new MessageType("GroupNew", 2);
    public final static MessageType Datatype = new MessageType("Datatype", 3);
    public final static MessageType FillValueOld = new MessageType("FillValueOld", 4);
    public final static MessageType FillValue = new MessageType("FillValue", 5);
    public final static MessageType Link = new MessageType("Link", 6);
    public final static MessageType ExternalDataFiles = new MessageType("ExternalDataFiles", 7);
    public final static MessageType Layout = new MessageType("Layout", 8);
    public final static MessageType GroupInfo = new MessageType("GroupInfo", 10);
    public final static MessageType FilterPipeline = new MessageType("FilterPipeline", 11);
    public final static MessageType Attribute = new MessageType("Attribute", 12);
    public final static MessageType Comment = new MessageType("Comment", 13);
    public final static MessageType LastModifiedOld = new MessageType("LastModifiedOld", 14);
    public final static MessageType SharedObject = new MessageType("SharedObject", 15);
    public final static MessageType ObjectHeaderContinuation = new MessageType("ObjectHeaderContinuation", 16);
    public final static MessageType Group = new MessageType("Group", 17);
    public final static MessageType LastModified = new MessageType("LastModified", 18);
    public final static MessageType AttributeInfo = new MessageType("AttributeInfo", 21);
    public final static MessageType ObjectReferenceCount = new MessageType("ObjectReferenceCount", 22);

    private String name;
    private int num;

    private MessageType(String name, int num) {
      this.name = name;
      this.num = num;
      hash.put(name, this);
      mess[num] = this;
    }

    /**
     * Find the MessageType that matches this name.
     *
     * @param name find DataTYpe with this name.
     * @return DataType or null if no match.
     */
    public static MessageType getType(String name) {
      if (name == null) return null;
      return hash.get(name);
    }

    /**
     * Get the MessageType by number.
     *
     * @param num message number.
     * @return the MessageType
     */
    public static MessageType getType(int num) {
      if ((num < 0) || (num >= MAX_MESSAGE)) return null;
      return mess[num];
    }

    /**
     * Message name.
     */
    public String toString() {
      return name + "(" + num + ")";
    }

    /**
     * @return Message number.
     */
    public int getNum() {
      return num;
    }

  }

  // Header Message: Level 2A1 and 2A2
  private class Message implements Comparable {
    long start;
    byte headerMessageFlags;
    short type, size, header_length;
    Object messData; // header message data
    MessageType mtype;

    short creationOrder = -1;

    /**
     * Read a message
     *
     * @param filePos at this filePos
     * @param version header version
     * @param creationOrderPresent true if bit2 of data object header flags is set
     * @return number of bytes read
     * @throws IOException of read error
     */
    int read(long filePos, int version, boolean creationOrderPresent) throws IOException {
      this.start = filePos;
      raf.seek(filePos);
      if (debugPos) debugOut.println("  --> Message Header starts at =" + raf.getFilePointer());

      if (version == 1) {
        type = raf.readShort();
        size = raf.readShort();
        headerMessageFlags = raf.readByte();
        raf.skipBytes(3);
        header_length = 8;

      } else {
        type = (short) raf.readByte();
        size = raf.readShort();
        headerMessageFlags = raf.readByte();
        header_length = 4;
        if (creationOrderPresent) {
          creationOrder = raf.readShort();
          header_length += 2;
        }
      }
      mtype = MessageType.getType(type);
      if (debug1) {
        debugOut.println("  -->" + mtype + " messageSize=" + size + " flags = " + Integer.toBinaryString(headerMessageFlags));
        if (creationOrderPresent && debugCreationOrder) debugOut.println("     creationOrder = " + creationOrder);
      }
      if (debugPos) debugOut.println("  --> Message Data starts at=" + raf.getFilePointer());

      if ((headerMessageFlags & 2) != 0) { // shared
        byte sharedVersion = raf.readByte();
        byte sharedType = raf.readByte();
        if (sharedVersion == 1) raf.skipBytes(6);
        if ((sharedVersion == 3) && (sharedType == 1)) {
          long heapId = raf.readLong();
          if (debug1) debugOut.println("     Shared Message " + sharedVersion + " type=" + sharedType + " heapId = " + heapId);
          if (debugPos) debugOut.println("  --> Shared Message reposition to =" + raf.getFilePointer());
          // dunno where is the file's shared object header heap ??
          throw new UnsupportedOperationException("****SHARED MESSAGE type = " + mtype + " heapId = " + heapId);

        } else {
          long address = readOffset();
          if (debug1) debugOut.println("     Shared Message " + sharedVersion + " type=" + sharedType + " address = " + address);
          DataObject dobj = findDataObject(address);
          if (null == dobj)
            throw new IllegalStateException("cant find data object at"+address);
          if (mtype == MessageType.Datatype) {
            messData = dobj.mdt;
            return header_length + size;
          }

        }
      }

      if (mtype == MessageType.NIL) { // 0
        // dont do nuttin

      } else if (mtype == MessageType.SimpleDataspace) { // 1
        MessageDataspace data = new MessageDataspace();
        data.read();
        messData = data;

      } else if (mtype == MessageType.GroupNew) { // 2
        MessageGroupNew data = new MessageGroupNew();
        data.read();
        messData = data;

      } else if (mtype == MessageType.Datatype) { // 3
        MessageDatatype data = new MessageDatatype();
        data.read();
        messData = data;

      } else if (mtype == MessageType.FillValueOld) { // 4
        MessageFillValueOld data = new MessageFillValueOld();
        data.read();
        messData = data;

      } else if (mtype == MessageType.FillValue) { // 5
        MessageFillValue data = new MessageFillValue();
        data.read();
        messData = data;

      } else if (mtype == MessageType.Link) { // 6
        MessageLink data = new MessageLink();
        data.read();
        messData = data;

      } else if (mtype == MessageType.Layout) { // 8
        MessageLayout data = new MessageLayout();
        data.read();
        messData = data;

      } else if (mtype == MessageType.GroupInfo) { // 10
        MessageGroupInfo data = new MessageGroupInfo();
        data.read();
        messData = data;

      } else if (mtype == MessageType.FilterPipeline) { // 11
        MessageFilter data = new MessageFilter();
        data.read();
        messData = data;

      } else if (mtype == MessageType.Attribute) { // 12
        MessageAttribute data = new MessageAttribute();
        data.read();
        messData = data;

      } else if (mtype == MessageType.Comment) { // 13
        MessageComment data = new MessageComment();
        data.read();
        messData = data;

      } else if (mtype == MessageType.LastModifiedOld) { // 14
        MessageLastModifiedOld data = new MessageLastModifiedOld();
        data.read();
        messData = data;

      } else if (mtype == MessageType.ObjectHeaderContinuation) { // 16
        MessageContinue data = new MessageContinue();
        data.read();
        messData = data;

      } else if (mtype == MessageType.Group) { // 17
        MessageGroup data = new MessageGroup();
        data.read();
        messData = data;

      } else if (mtype == MessageType.LastModified) { // 18
        MessageLastModified data = new MessageLastModified();
        data.read();
        messData = data;

      } else if (mtype == MessageType.AttributeInfo) { // 21
        MessageAttributeInfo data = new MessageAttributeInfo();
        data.read();
        messData = data;

      } else if (mtype == MessageType.ObjectReferenceCount) { // 21
        MessageObjectReferenceCount data = new MessageObjectReferenceCount();
        data.read();
        messData = data;

      } else {
        debugOut.println("****UNPROCESSED MESSAGE type = " + mtype + " raw = " + type);
        throw new UnsupportedOperationException("****UNPROCESSED MESSAGE type = " + mtype + " raw = " + type);
      }

      return header_length + size;
    }

    public int compareTo(Object o) {
      return type - ((Message) o).type;
    }

    public String toString() {
      return "  type = " + mtype + "; " + messData;
    }
  }

  // Message Type 1 (p 23) : "Simple Dataspace" = dimension list / shape
  class MessageDataspace {
    byte ndims, flags, type;
    int[] dimLength, maxLength, permute;
    boolean isPermuted;

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append(" length=(");
      for (int size : dimLength) sbuff.append(size).append(",");
      sbuff.append(") max=(");
      for (int aMaxLength : maxLength) sbuff.append(aMaxLength).append(",");
      sbuff.append(") permute=(");
      for (int aPermute : permute) sbuff.append(aPermute).append(",");
      sbuff.append(")");
      return sbuff.toString();
    }

    void read() throws IOException {
      if (debugPos) debugOut.println("   *MessageSimpleDataspace start pos= " + raf.getFilePointer());

      byte version = raf.readByte();
      if (version == 1) {
        ndims = raf.readByte();
        flags = raf.readByte();
        type = (byte) ((ndims == 0) ? 0 : 1);
        raf.skipBytes(5); // skip 5 bytes

      } else if (version == 2) {
        ndims = raf.readByte();
        flags = raf.readByte();
        type = raf.readByte();

      } else {
        throw new IllegalStateException("MessageDataspace: unknown version= " + version);
      }

      if (debug1) debugOut.println("   SimpleDataspace version= " + version + " flags=" +
              Integer.toBinaryString(flags) + " ndims=" + ndims + " type=" + type);

      dimLength = new int[ndims];
      for (int i = 0; i < ndims; i++)
        dimLength[i] = (int) readLength();

      boolean hasMax = (flags & 0x01) != 0;
      maxLength = new int[ndims];
      if (hasMax) {
        for (int i = 0; i < ndims; i++)
          maxLength[i] = (int) readLength();
      } else {
        System.arraycopy(dimLength, 0, maxLength, 0, ndims);
      }

      isPermuted = (flags & 0x02) != 0;
      permute = new int[ndims];
      if (isPermuted) {
        for (int i = 0; i < ndims; i++)
          permute[i] = (int) readLength();
      }

      if (debug1) {
        for (int i = 0; i < ndims; i++)
          debugOut.println("    dim length = " + dimLength[i] + " max = " + maxLength[i]);
      }
    }
  }

  // Message Type 17/0x11 ( p 58) "Group" : makes this object into a Group
  private class MessageGroup {
    long btreeAddress, nameHeapAddress;

    void read() throws IOException {
      btreeAddress = readOffset();
      nameHeapAddress = readOffset();
      if (debug1) debugOut.println("   Group btreeAddress=" + btreeAddress + " nameHeapAddress=" + nameHeapAddress);
    }
  }

  // Message Type 18/0x12 ( p 59) "Last Modified" : last modified date represented as secs since 1970
  private class MessageLastModified {
    byte version;
    int secs;

    void read() throws IOException {
      version = raf.readByte();
      raf.skipBytes(3); // skip byte
      secs = raf.readInt();
    }

    public String toString() {
      return new Date(secs * 1000).toString();
    }
  }

  // Message Type 2 "Link Info" (version 2)
  private class MessageGroupNew {
    byte version, flags;
    long maxCreationIndex = -2, fractalHeapAddress, v2BtreeAddress, v2BtreeAddressCreationOrder = -2;

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("   GroupNew fractalHeapAddress=" + fractalHeapAddress + " v2BtreeAddress=" + v2BtreeAddress);
      if (v2BtreeAddressCreationOrder > -2) sbuff.append(" v2BtreeAddressCreationOrder=" + v2BtreeAddressCreationOrder);
      if (maxCreationIndex > -2) sbuff.append(" maxCreationIndex=" + maxCreationIndex);
      return sbuff.toString();
    }

    void read() throws IOException {
      if (debugPos) debugOut.println("   *MessageGroupNew start pos= " + raf.getFilePointer());
      byte version = raf.readByte();
      byte flags = raf.readByte();
      if ((flags & 1) != 0) {
        maxCreationIndex = raf.readLong();
      }

      fractalHeapAddress = readOffset();
      v2BtreeAddress = readOffset(); // aka name index

      if ((flags & 2) != 0) {
        v2BtreeAddressCreationOrder = readOffset();
      }

      if (debug1) debugOut.println("   MessageGroupNew version= " + version + " flags = " + flags + this);
    }
  }

  // Message Type 10/0xA "Group Info" (version 2)
  private class MessageGroupInfo {
    byte flags;
    short maxCompactValue = -1, minDenseValue = -1, estNumEntries = -1, estLengthEntryName = -1;

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("   MessageGroupInfo ");
      if ((flags & 1) != 0) sbuff.append(" maxCompactValue=" + maxCompactValue + " minDenseValue=" + minDenseValue);
      if ((flags & 2) != 0)
        sbuff.append(" estNumEntries=" + estNumEntries + " estLengthEntryName=" + estLengthEntryName);
      return sbuff.toString();
    }

    void read() throws IOException {
      if (debugPos) debugOut.println("   *MessageGroupInfo start pos= " + raf.getFilePointer());
      byte version = raf.readByte();
      flags = raf.readByte();

      if ((flags & 1) != 0) {
        maxCompactValue = raf.readShort();
        minDenseValue = raf.readShort();
      }

      if ((flags & 2) != 0) {
        estNumEntries = raf.readShort();
        estLengthEntryName = raf.readShort();
      }

      if (debug1) debugOut.println("   MessageGroupInfo version= " + version + " flags = " + flags + this);
    }
  }

  // Message Type 6 "Link" (version 2)
  private class MessageLink {
    byte version, flags, encoding;
    byte linkType; // 0=hard, 1=soft, 64 = external
    long creationOrder;
    String linkName, link;
    long linkAddress;

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("   MessageLink ");
      sbuff.append(" name=" + linkName + " type=" + linkType);
      if (linkType == 0)
        sbuff.append(" linkAddress=" + linkAddress);
      else
        sbuff.append(" link=" + link);

      if ((flags & 4) != 0)
        sbuff.append(" creationOrder=" + creationOrder);
      if ((flags & 0x10) != 0)
        sbuff.append(" encoding=" + encoding);
      return sbuff.toString();
    }

    void read() throws IOException {
      if (debugPos) debugOut.println("   *MessageLink start pos= " + raf.getFilePointer());
      version = raf.readByte();
      flags = raf.readByte();

      if ((flags & 8) != 0)
        linkType = raf.readByte();

      if ((flags & 4) != 0)
        creationOrder = raf.readLong();

      if ((flags & 0x10) != 0)
        encoding = raf.readByte();

      int linkNameLength = (int) readVariableSizeFactor(flags & 3);
      byte[] b = new byte[linkNameLength];
      raf.read(b);
      linkName = new String(b);

      if (linkType == 0) {
        linkAddress = readOffset();

      } else if (linkType == 1) {
        short len = raf.readShort();
        b = new byte[len];
        raf.read(b);
        link = new String(b);

      } else if (linkType == 64) {
        short len = raf.readShort();
        b = new byte[len];
        raf.read(b);
        link = new String(b); // actually 2 strings - see docs
      }

      if (debug1)
        debugOut.println("   MessageLink version= " + version + " flags = " + Integer.toBinaryString(flags) + this);
    }
  }

  // Message Type 3 : "Datatype"
  class MessageDatatype {
    int type, version;
    byte[] flags = new byte[3];
    int byteSize, byteOrder;

    // reference
    int referenceType; // 0 = object, 1 = region

    // array
    int[] dim;

    // compound type
    short nmembers;
    List<StructureMember> members;

    // variable-length, array types have "parent" DataType
    MessageDatatype parent;
    boolean isVString;

    boolean isOK = true;

    public String toString() {
      return " datatype = " + type + " reference type = " + referenceType;
    }

    void read() throws IOException {
      if (debugPos) debugOut.println("   *MessageDatatype start pos= " + raf.getFilePointer());

      byte tandv = raf.readByte();
      type = (tandv & 0xf);
      version = ((tandv & 0xf0) >> 4);

      raf.read(flags);
      byteSize = raf.readInt();
      byteOrder = ((flags[0] & 1) == 0) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;

      if (debug1) debugOut.println("   Datatype type=" + type + " version= " + version + " flags = " +
              flags[0] + " " + flags[1] + " " + flags[2] + " byteSize=" + byteSize
              + " byteOrder=" + (byteOrder == 0 ? "BIG" : "LITTLE"));

      if (type == 0) {  // fixed point
        short bitOffset = raf.readShort();
        short bitPrecision = raf.readShort();
        if (debug1)
          debugOut.println("   type 0 (fixed point): bitOffset= " + bitOffset + " bitPrecision= " + bitPrecision);
        isOK = (bitOffset == 0) && (bitPrecision % 8 == 0);

      } else if (type == 1) {  // floating point
        short bitOffset = raf.readShort();
        short bitPrecision = raf.readShort();
        byte expLocation = raf.readByte();
        byte expSize = raf.readByte();
        byte manLocation = raf.readByte();
        byte manSize = raf.readByte();
        int expBias = raf.readInt();
        if (debug1)
          debugOut.println("   type 1 (floating point): bitOffset= " + bitOffset + " bitPrecision= " + bitPrecision +
                  " expLocation= " + expLocation + " expSize= " + expSize + " manLocation= " + manLocation +
                  " manSize= " + manSize + " expBias= " + expBias);

      } else if (type == 2) {  // time
        short bitPrecision = raf.readShort();
        if (debug1)
          debugOut.println("   type 2 (time): bitPrecision= " + bitPrecision);
        isOK = false;

      } else if (type == 4) { // bit field
        short bitOffset = raf.readShort();
        short bitPrecision = raf.readShort();
        if (debug1)
          debugOut.println("   type 4 (bit field): bitOffset= " + bitOffset + " bitPrecision= " + bitPrecision);
        isOK = (bitOffset == 0) && (bitPrecision % 8 == 0);

      } else if (type == 5) { // opaque
        byte len = flags[0];
        String desc = (len > 0) ? readString(raf) : null;
        if (debug1) debugOut.println("   type 5 (opaque): len= "+len+" desc= " + desc);

      } else if (type == 6) { // compound
        int nmembers = flags[1] * 256 + flags[0];
        if (debug1) debugOut.println("   --type 6(compound): nmembers=" + nmembers);
        members = new ArrayList<StructureMember>();
        for (int i = 0; i < nmembers; i++) {
          members.add(new StructureMember(version, byteSize));
        }
        if (debugDetail) debugOut.println("   --done with compound type");

      } else if (type == 7) { // reference
        referenceType = flags[0] & 0xf;
        if (debug1 || debugReference) debugOut.println("   --type 7(reference): type= " + referenceType);

      } else if (type == 8) { // enums
        int nmembers = flags[1] * 256 + flags[0];
        boolean saveDebugDetail = debugDetail;
        if (debug1 || debugEnum) {
          debugOut.println("   --type 8(enums): nmembers=" + nmembers);
          debugDetail = true;
        }
        parent = new MessageDatatype(); // base type
        parent.read();
        debugDetail = saveDebugDetail;

        // read the enum names
        String[] enumName = new String[nmembers];
        int[] enumValue = new int[nmembers];
        for (int i = 0; i < nmembers; i++) {
          if (version < 3)
            enumName[i] = readString8(raf); //padding
          else
            enumName[i] = readString(raf); // no padding
        }

        // read the values; must switch to parent byte order (!)
        raf.order(parent.byteOrder);
        for (int i = 0; i < nmembers; i++)
          enumValue[i] = (int) readVariableSize(parent.byteSize); // assume unsigned integer type, fits into int
        raf.order(RandomAccessFile.LITTLE_ENDIAN);

        if (debugEnum) {
          for (int i = 0; i < nmembers; i++)
            debugOut.println("   " + enumValue[i] + "=" + enumName[i]);
        }

      } else if (type == 9) { // variable-length
        isVString = (flags[0] & 0xf) == 1;
        if (debug1)
          debugOut.println("   type 9(variable length): type= " + (isVString ? "string" : "sequence of type:"));
        parent = new MessageDatatype(); // base type
        parent.read();

      } else if (type == 10) { // array
        if (debug1) debugOut.print("   type 10(array) lengths= ");
        int ndims = (int) raf.readByte();
        if (version < 3) raf.skipBytes(3);

        dim = new int[ndims];
        for (int i = 0; i < ndims; i++) {
          dim[i] = raf.readInt();
          if (debug1) debugOut.print(" " + dim[i]);
        }

        if (version < 3) {  // not present in version 3, never used anyway
          int[] pdim = new int[ndims];
          for (int i = 0; i < ndims; i++)
            pdim[i] = raf.readInt();
        }
        if (debug1) debugOut.println();

        parent = new MessageDatatype(); // base type
        parent.read();
      }
    }

    int getBaseType() {
      return (parent != null) ? parent.getBaseType() : type;
    }

    int getBaseSize() {
      return (parent != null) ? parent.getBaseSize() : byteSize;
    }

    byte[] getFlags() {
      return (parent != null) ? parent.getFlags() : flags;
    }
  }

  // Message Type 4 ( p 38) "Fill Value Old" : fill value is stored in the message
  private class MessageFillValueOld {
    byte[] value;
    int size;

    void read() throws IOException {
      size = raf.readInt();
      value = new byte[size];
      raf.read(value);

      if (debug1) debugOut.println(this);
    }

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("   FillValueOld size= ").append(size).append(" value=");
      for (int i = 0; i < size; i++) sbuff.append(" ").append(value[i]);
      return sbuff.toString();
    }
  }

  // Message Type 5 "Fill Value New" : fill value is stored in the message, with extra metadata
  private class MessageFillValue {
    byte version; // 1,2,3
    byte spaceAllocateTime; // 1= early, 2=late, 3=incremental
    byte fillWriteTime;
    int size;
    byte[] value;
    boolean hasFillValue = false;

    byte flags;

    void read() throws IOException {
      version = raf.readByte();

      if (version < 3) {
        spaceAllocateTime = raf.readByte();
        fillWriteTime = raf.readByte();
        hasFillValue = (version <= 1) || raf.readByte() != 0;

      } else {
        flags = raf.readByte();
        spaceAllocateTime = (byte) (flags & 3);
        fillWriteTime = (byte) ((flags >> 2) & 3);
        hasFillValue = (flags & 32) != 0;
      }

      if (hasFillValue) {
        size = raf.readInt();
        if (size > 0) {
          value = new byte[size];
          raf.read(value);
          hasFillValue = true;
        } else {
          hasFillValue = false;
        }
      }

      if (debug1) debugOut.println(this);
    }

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("   FillValue version= ").append(version).append(" spaceAllocateTime = ").append(spaceAllocateTime).append(" fillWriteTime=").append(fillWriteTime).append(" hasFillValue= ").append(hasFillValue);
      sbuff.append("\n size = ").append(size).append(" value=");
      for (int i = 0; i < size; i++) sbuff.append(" ").append(value[i]);
      return sbuff.toString();
    }

  }

  // Message Type 8 ( p 44) "Data Storage Layout" : regular (contiguous), chunked, or compact (stored with the message)
  private class MessageLayout {
    byte type; // 0 = Compact, 1 = Contiguous, 2 = Chunked
    long dataAddress = -1; // -1 means "not allocated"
    long contiguousSize; // size of data allocated contiguous
    int[] chunkSize;  // only for chunked, otherwise must use Dataspace

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      switch (type) {
        case 0:
          sbuff.append("compact");
          break;
        case 1:
          sbuff.append("contiguous");
          break;
        case 2:
          sbuff.append("chunked");
          break;
        default:
          sbuff.append("unkown type= ").append(type);
      }

      if (chunkSize != null) {
        sbuff.append(" storageSize = (");
        for (int i = 0; i < chunkSize.length; i++) {
          if (i > 0) sbuff.append(",");
          sbuff.append(chunkSize[i]);
        }
        sbuff.append(")");
      }

      sbuff.append(" dataAddress=").append(dataAddress);
      return sbuff.toString();
    }

    void read() throws IOException {
      int ndims;

      byte version = raf.readByte();
      if (version < 3) {
        ndims = raf.readByte();
        type = raf.readByte();
        raf.skipBytes(5); // skip 5 bytes

        boolean isCompact = (type == 0);
        if (!isCompact) dataAddress = readOffset();
        chunkSize = new int[ndims];
        for (int i = 0; i < ndims; i++)
          chunkSize[i] = raf.readInt();

        if (isCompact) {
          int dataSize = raf.readInt();
          dataAddress = raf.getFilePointer();
        }

      } else {
        type = raf.readByte();

        if (type == 0) {
          int dataSize = raf.readShort();
          dataAddress = raf.getFilePointer();

        } else if (type == 1) {
          dataAddress = readOffset();
          contiguousSize = readLength();

        } else if (type == 2) {
          ndims = raf.readByte();
          dataAddress = readOffset();
          chunkSize = new int[ndims];
          for (int i = 0; i < ndims; i++)
            chunkSize[i] = raf.readInt();
        }
      }

      if (debug1) debugOut.println("   StorageLayout version= " + version + this);
    }
  }

  // Message Type 11/0xB ( p 50) "Filter Pipeline" : apply a filter to the "data stream"
  class MessageFilter {
    Filter[] filters;

    void read() throws IOException {
      byte version = raf.readByte();
      byte nfilters = raf.readByte();
      if (version == 1) raf.skipBytes(6);

      filters = new Filter[nfilters];
      for (int i = 0; i < nfilters; i++)
        filters[i] = new Filter(version);

      if (debug1) debugOut.println("   MessageFilter version=" + version + this);
    }

    public Filter[] getFilters() {
      return filters;
    }

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("   MessageFilter filters=\n");
      for (Filter f : filters)
        sbuff.append(" ").append(f).append("\n");
      return sbuff.toString();
    }
  }

  class Filter {
    short id; // 1=deflate, 2=shuffle, 3=fletcher32, 4=szip, 5=nbit, 6=scaleoffset
    short flags;
    String name;
    short nValues;
    int[] data;

    Filter(byte version) throws IOException {
      this.id = raf.readShort();
      short nameSize = ((version > 1) && (id < 256)) ? 0 : raf.readShort(); // if the filter id < 256 then this field is not stored
      this.flags = raf.readShort();
      nValues = raf.readShort();
      if (version == 1)
        this.name = (nameSize > 0) ? readString8(raf) : "StandardFilter " + id; // null terminated, pad to 8 bytes
      else
        this.name = (nameSize > 0) ? readStringFixedLength(nameSize) : "StandardFilter " + id; // non-null terminated

      data = new int[nValues];
      for (int i = 0; i < nValues; i++)
        data[i] = raf.readInt();
      if (nValues % 2 == 1)
        raf.skipBytes(4);

      if (debug1) debugOut.println(this);
    }


    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("   Filter id= ").append(id).append(" flags = ").append(flags).append(" nValues=").append(nValues).append(" name= ").append(name).append(" data = ");
      for (int i = 0; i < nValues; i++)
        sbuff.append(data[i]).append(" ");
      return sbuff.toString();
    }
  }

  // Message Type 12/0xC ( p 52) "Attribute" : define an Atribute
  private class MessageAttribute {
    byte version;
    //short typeSize, spaceSize;
    String name;
    MessageDatatype mdt = new MessageDatatype();
    MessageDataspace mds = new MessageDataspace();
    long dataPos; // pointer to the attribute data section, must be absolute file position

    public String toString() {
      return "name= " + name+ "dataPos= "+dataPos;
    }

    void read() throws IOException {
      if (debugPos) debugOut.println("   *MessageAttribute start pos= " + raf.getFilePointer());
      short nameSize, typeSize, spaceSize;
      byte flags = 0;
      byte encoding = 0; // 0 = ascii, 1 = UTF-8

      version = raf.readByte();
      if (version == 1) {
        raf.read(); // skip byte
        nameSize = raf.readShort();
        typeSize = raf.readShort();
        spaceSize = raf.readShort();

      } else if ((version == 2) || (version == 3)) {
        flags = raf.readByte();
        nameSize = raf.readShort();
        typeSize = raf.readShort();
        spaceSize = raf.readShort();
        if (version == 3) encoding = raf.readByte();
      } else {
        throw new IllegalStateException("MessageAttribute unknown version " + version);
      }

      // read the name
      long filePos = raf.getFilePointer();
      name = readString(raf); // read at current pos
      if (version == 1) nameSize += padding(nameSize, 8);
      raf.seek(filePos + nameSize); // make it more robust for errors

      if (debug1)
        debugOut.println("   MessageAttribute version= " + version + " flags = " + Integer.toBinaryString(flags) +
                " nameSize = " + nameSize + " typeSize=" + typeSize + " spaceSize= " + spaceSize + " name= " + name);

      // read the datatype
      filePos = raf.getFilePointer();
      if (debugPos) debugOut.println("   *MessageAttribute before mdt pos= " + filePos);
      mdt.read();
      if (version == 1) typeSize += padding(typeSize, 8);
      raf.seek(filePos + typeSize); // make it more robust for errors

      // read the dataspace
      filePos = raf.getFilePointer();
      if (debugPos) debugOut.println("   *MessageAttribute before mds = " + filePos);
      mds.read();
      if (version == 1) spaceSize += padding(spaceSize, 8);
      raf.seek(filePos + spaceSize); // make it more robust for errors

      // heres where the data starts
      dataPos = raf.getFilePointer();
      if (debugPos) debugOut.println("   *MessageAttribute dataPos= " + dataPos);
    }
  }

  // Message Type 21/0x15 "Attribute Info" (version 2)
  private class MessageAttributeInfo {
    byte version, flags;
    short maxCreationIndex = -1;
    long fractalHeapAddress = -2, v2BtreeAddress = -2, v2BtreeAddressCreationOrder = -2;

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("   MessageAttributeInfo ");
      if ((flags & 1) != 0) sbuff.append(" maxCreationIndex=" + maxCreationIndex);
      sbuff.append(" fractalHeapAddress=" + fractalHeapAddress + " v2BtreeAddress=" + v2BtreeAddress);
      if ((flags & 2) != 0) sbuff.append(" v2BtreeAddressCreationOrder=" + v2BtreeAddressCreationOrder);
      return sbuff.toString();
    }

    void read() throws IOException {
      if (debugPos) debugOut.println("   *MessageAttributeInfo start pos= " + raf.getFilePointer());
      byte version = raf.readByte();
      byte flags = raf.readByte();
      if ((flags & 1) != 0) {
        maxCreationIndex = raf.readShort();
      }

      fractalHeapAddress = readOffset();
      v2BtreeAddress = readOffset();

      if ((flags & 2) != 0) {
        v2BtreeAddressCreationOrder = readOffset();
      }

      if (debug1) debugOut.println("   MessageAttributeInfo version= " + version + " flags = " + flags + this);
    }
  }

  // Message Type 13/0xD ( p 54) "Object Comment" : "short description of an Object"
  private class MessageComment {
    String name;

    void read() throws IOException {
      name = readString(raf);
    }

    public String toString() {
      return name;
    }
  }

  // Message Type 14/0xE ( p 55) "Last Modified (old)" : last modified date represented as a String YYMM etc. use message type 18 instead
  private class MessageLastModifiedOld {
    String datemod;

    void read() throws IOException {
      byte[] s = new byte[14];
      raf.read(s);
      datemod = new String(s);
      if (debug1) debugOut.println("   MessageLastModifiedOld=" + datemod);
    }

    public String toString() {
      return datemod;
    }
  }

  // Message Type 16/0x10 ( p 57) "Continue" : point to more messages
  private class MessageContinue {
    long offset, length;

    void read() throws IOException {
      offset = readOffset();
      length = readLength();
      if (debug1) debugOut.println("   Continue offset=" + offset + " length=" + length);
    }
  }

  // Message Type 22/0x11 Object Reference COunt
  private class MessageObjectReferenceCount {
    int refCount;

    void read() throws IOException {
      int version = raf.readByte();
      refCount = raf.readInt();
      if (debug1) debugOut.println("   ObjectReferenceCount=" + refCount);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  // Groups

  private abstract class Group {
    Group parent;
    String name, dname;
    List<DataObject> nestedObjects = new ArrayList<DataObject>(); // nested data objects

    public Group(Group parent, String name) {
      this.parent = parent;
      this.name = name;
      this.dname = (name.length() == 0) ? "root" : name;
    }

    String getName() {
      return (parent == null) ? name : parent.getName() + "/" + name;
    }

    // is this a child of that ?
    boolean isChildOf(Group that) {
      if (parent == that) return true;
      if (parent == null) return false;
      return parent.isChildOf(that);
    }
  }

  private class GroupNew extends Group {
    private FractalHeap fractalHeap;
    private BTree2 btree;

    public GroupNew(Group parent, String name, MessageGroupNew groupNewMessage, List<Message> messages) throws IOException {
      super(parent, name);
      if (debug1) debugOut.println("\n--> GroupNew read <" + dname + ">");

      if (groupNewMessage.fractalHeapAddress >= 0) {
        this.fractalHeap = new FractalHeap(this, groupNewMessage.fractalHeapAddress);

        long btreeAddress = (groupNewMessage.v2BtreeAddressCreationOrder >= 0) ?
                groupNewMessage.v2BtreeAddressCreationOrder : groupNewMessage.v2BtreeAddress;
        if (btreeAddress < 0) throw new IllegalStateException("no valid btree for GroupNew with Fractal Heap");

        // read in btree and all entries (!)
        this.btree = new BTree2(dname, btreeAddress);
        for (BTree2.Entry2 e : this.btree.entryList) {
          byte[] heapId = null;
          switch (btree.btreeType) {
            case 5: heapId = ((BTree2.Record5) e.record).heapId; break;
            case 6: heapId = ((BTree2.Record6) e.record).heapId; break;
            default: continue;
          }
          MessageLink linkMessage = fractalHeap.getLink(heapId);
          DataObject dobj = new DataObject(this, linkMessage.linkName, linkMessage.linkAddress);
          dobj.read();
          nestedObjects.add(dobj);
        }

      } else {
        // look for link messages
        for (Message mess : messages) {
          if (mess.mtype == MessageType.Link) {
            MessageLink linkMessage = (MessageLink) mess.messData;
            if (linkMessage.linkType == 0) {
              DataObject dobj = new DataObject(this, linkMessage.linkName, linkMessage.linkAddress);
              dobj.read();
              nestedObjects.add(dobj);
            }
          }
        }
      }

      /* now read all the entries in the btree
     for (SymbolTableEntry s : btree.getSymbolTableEntries()) {
       String sname = nameHeap.getString((int) s.getNameOffset());
       if (debugSymbolTable) debugOut.println("\n   Symbol name=" + sname);

       DataObject o;
       if (s.cacheType == 2) {
         String linkName = nameHeap.getString(s.linkOffset);
         if (debugSymbolTable) debugOut.println("   Symbolic link name=" + linkName);
         o = new DataObject(this, sname, linkName);

       } else {
         o = new DataObject(this, sname, s.getObjectAddress());
         o.read();
       }
       nestedObjects.add(o);
       hashDataObjects.put(o.getName(), o); // to look up symbolic links
     } */
      if (debug1) debugOut.println("<-- end Group read <" + dname + ">");
    }


  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  // Group structures: Group, Btree, LocalHeap, SymbolTableEntry
  // these are all read before any data

  private class GroupOld extends Group {
    private LocalHeap nameHeap;
    private GroupBTree btree;
    private long btreeAddress, nameHeapAddress;

    public GroupOld(Group parent, String name, long btreeAddress, long nameHeapAddress) throws IOException {
      super(parent, name);
      this.btreeAddress = btreeAddress;
      this.nameHeapAddress = nameHeapAddress;

      // track by address for hard links
      hashGroups.put(btreeAddress, this);

      if (debug1) debugOut.println("\n--> GroupOld read <" + dname + ">");
      this.nameHeap = new LocalHeap(this, nameHeapAddress);
      this.btree = new GroupBTree(dname, btreeAddress);

      // now read all the entries in the btree
      for (SymbolTableEntry s : btree.getSymbolTableEntries()) {
        String sname = nameHeap.getString((int) s.getNameOffset());
        if (debugSymbolTable) debugOut.println("\n   Symbol name=" + sname);

        DataObject o;
        if (s.cacheType == 2) {
          String linkName = nameHeap.getString(s.linkOffset);
          if (debugSymbolTable) debugOut.println("   Symbolic link name=" + linkName);
          o = new DataObject(this, sname, linkName);

        } else {
          o = new DataObject(this, sname, s.getObjectAddress());
          o.read();
        }
        nestedObjects.add(o);
        hashDataObjects.put(o.getName(), o); // to look up symbolic links
      }
      if (debug1) debugOut.println("<-- end Group read <" + dname + ">");
    }

  } // GroupOld

  // Level 1A (Section III.A p 9)
  // this just reads in all the entries into a list
  private class GroupBTree {
    protected String owner;
    protected int wantType = 0;
    protected List<Entry> entries = new ArrayList<Entry>(); // list of type Entry
    private List<SymbolTableEntry> sentries = new ArrayList<SymbolTableEntry>(); // list of type SymbolTableEntry

    // for DataBTree
    GroupBTree(String owner) {
      this.owner = owner;
    }

    GroupBTree(String owner, long address) throws IOException {
      this.owner = owner;

      readAllEntries(address);

      // now convert the entries to SymbolTableEntry
      for (Entry e : entries) {
        GroupNode node = new GroupNode(e.address);
        sentries.addAll(node.getSymbols());
      }
    }

    List<Entry> getEntries() {
      return entries;
    }

    List<SymbolTableEntry> getSymbolTableEntries() {
      return sentries;
    }

    // recursively read all entries, place them in order in list
    protected void readAllEntries(long address) throws IOException {
      raf.seek(getFileOffset(address));
      if (debugGroupBtree) debugOut.println("\n--> GroupBTree read tree at position=" + raf.getFilePointer());

      byte[] name = new byte[4];
      raf.read(name);
      String nameS = new String(name);
      if (!nameS.equals("TREE"))
        throw new IllegalStateException("BtreeGroup doesnt start with TREE");

      int type = raf.readByte();
      int level = raf.readByte();
      int nentries = raf.readShort();
      if (debugGroupBtree)
        debugOut.println("    type=" + type + " level=" + level + " nentries=" + nentries);
      if (type != wantType)
        throw new IllegalStateException("BtreeGroup must be type " + wantType);

      long size = 8 + 2 * sizeOffsets + nentries * (sizeOffsets + sizeLengths);
      if (debugTracker) memTracker.addByLen("Group BTree (" + owner + ")", address, size);

      long leftAddress = readOffset();
      long rightAddress = readOffset();
      long entryPos = raf.getFilePointer();
      if (debugGroupBtree)
        debugOut.println("    leftAddress=" + leftAddress + " " + Long.toHexString(leftAddress) +
                " rightAddress=" + rightAddress + " " + Long.toHexString(rightAddress));

      // read all entries in this Btree "Node"
      List<Entry> myEntries = new ArrayList<Entry>();
      for (int i = 0; i < nentries; i++) {
        myEntries.add(new Entry());
      }

      if (level == 0)
        entries.addAll(myEntries);
      else {
        for (Entry entry : myEntries) {
          if (debugDataBtree) debugOut.println("  nonzero node entry at =" + entry.address);
          readAllEntries(entry.address);
        }
      }

    }

    // these are part of the level 1A data structure, type = 0
    class Entry {
      long key, address;

      Entry() throws IOException {
        this.key = readLength();
        this.address = readOffset();
        if (debugGroupBtree) debugOut.println("     GroupEntry key=" + key + " address=" + address);
      }
    }

    // level 1B
    class GroupNode {
      long address;
      byte version;
      short nentries;
      List<SymbolTableEntry> symbols = new ArrayList<SymbolTableEntry>(); // SymbolTableEntry

      GroupNode(long address) throws IOException {
        this.address = address;

        raf.seek(getFileOffset(address));
        if (debugDetail) debugOut.println("--Group Node position=" + raf.getFilePointer());

        // header
        byte[] sig = new byte[4];
        raf.read(sig);
        String nameS = new String(sig);
        if (!nameS.equals("SNOD"))
          throw new IllegalStateException();

        version = raf.readByte();
        raf.readByte(); // skip byte
        nentries = raf.readShort();
        if (debugDetail) debugOut.println("   version=" + version + " nentries=" + nentries);

        long posEntry = raf.getFilePointer();
        for (int i = 0; i < nentries; i++) {
          SymbolTableEntry entry = new SymbolTableEntry(posEntry);
          posEntry += entry.getSize();
          symbols.add(entry);
        }
        if (debugDetail) debugOut.println("-- Group Node end position=" + raf.getFilePointer());
        long size = 8 + nentries * 40;
        if (debugTracker) memTracker.addByLen("Group BtreeNode (" + owner + ")", address, size);
      }

      List<SymbolTableEntry> getSymbols() {
        return symbols;
      }
    }


  } // GroupBTree

  // Level 1A2
  private class BTree2 {
    private String owner;
    private byte btreeType;
    private int nodeSize; // size in bytes of btree nodes
    private short recordSize; // size in bytes of btree records
    private short treeDepth, numRecordsRootNode;
    private long rootNodeAddress;

    private List<Entry2> entryList = new ArrayList<Entry2>();

    BTree2(String owner, long address) throws IOException {
      this.owner = owner;

      raf.seek(getFileOffset(address));

      // header
      byte[] heapname = new byte[4];
      raf.read(heapname);
      String nameS = new String(heapname);
      if (!nameS.equals("BTHD"))
        throw new IllegalStateException();

      byte version = raf.readByte();
      btreeType = raf.readByte();
      nodeSize = raf.readInt();
      recordSize = raf.readShort();
      treeDepth = raf.readShort();
      byte split = raf.readByte();
      byte merge = raf.readByte();
      rootNodeAddress = readOffset();
      numRecordsRootNode = raf.readShort();
      long totalRecords = readLength(); // total in entire btree
      int checksum = raf.readInt();

      if (debugBtree2) {
        debugOut.println("BTree2 version=" + version + " type=" + btreeType + " treeDepth=" + treeDepth);
        debugOut.println(" nodeSize=" + nodeSize + " recordSize=" + recordSize + " numRecordsRootNode="
                + numRecordsRootNode + " totalRecords=" + totalRecords + " rootNodeAddress=" + rootNodeAddress);
      }

      readAllEntries();
    }

    protected void readAllEntries() throws IOException {
      InternalNode node = new InternalNode(rootNodeAddress, numRecordsRootNode, recordSize, treeDepth);
      node.recurse();
    }

    /* recursively read all entries, place them in order in list
    protected void readAllEntries(long address, short nrecs, short recSize, short depth) throws IOException {
      InternalNode node = new InternalNode(address, nrecs, recSize, depth);

      /* raf.seek(getFileOffset(address));
      if (debugGroupBtree) debugOut.println("\n--> GroupBTree read tree at position=" + raf.getFilePointer());

      byte[] name = new byte[4];
      raf.read(name);
      String nameS = new String(name);
      if (!nameS.equals("TREE"))
        throw new IllegalStateException("BtreeGroup doesnt start with TREE");

      int type = raf.readByte();
      int level = raf.readByte();
      int nentries = raf.readShort();
      if (debugGroupBtree)
        debugOut.println("    type=" + type + " level=" + level + " nentries=" + nentries);
      if (type != wantType)
        throw new IllegalStateException("BtreeGroup must be type " + wantType);

      long size = 8 + 2 * sizeOffsets + nentries * (sizeOffsets + sizeLengths);
      if (debugTracker) memTracker.addByLen("Group BTree (" + owner + ")", address, size);

      long leftAddress = readOffset();
      long rightAddress = readOffset();
      long entryPos = raf.getFilePointer();
      if (debugGroupBtree)
        debugOut.println("    leftAddress=" + leftAddress + " " + Long.toHexString(leftAddress) +
            " rightAddress=" + rightAddress + " " + Long.toHexString(rightAddress));

      // read all entries in this Btree "Node"
      List<Entry> myEntries = new ArrayList<Entry>();
      for (int i = 0; i < nentries; i++) {
        myEntries.add(new Entry());
      }

      if (level == 0)
        entries.addAll(myEntries);
      else {
        for (Entry entry : myEntries) {
          if (debugDataBtree) debugOut.println("  nonzero node entry at =" + entry.address);
          readAllEntries(entry.address);
        }
      }

    }  */

    // these are part of the level 1A data structure, type = 0

    class Entry2 {
      long childAddress, nrecords, totNrecords;
      Object record;
    }

    class InternalNode {
      Entry2[] entries;
      int depth;

      InternalNode(long address, short nrecords, short recordSize, int depth) throws IOException {
        this.depth = depth;
        raf.seek(getFileOffset(address));

        if (debugPos) debugOut.println("--Btree2 InternalNode position=" + raf.getFilePointer());

        // header
        byte[] sig = new byte[4];
        raf.read(sig);
        String nameS = new String(sig);
        if (!nameS.equals("BTIN"))
          throw new IllegalStateException();

        byte version = raf.readByte();
        byte nodeType = raf.readByte();
        if (nodeType != btreeType)
          throw new IllegalStateException();

        if (debugBtree2)
          debugOut.println("   InternalNode version=" + version + " type=" + nodeType + " nrecords=" + nrecords);

        entries = new Entry2[nrecords];
        for (int i = 0; i < nrecords; i++) {
          entries[i] = new Entry2();
          entries[i].record = readRecord(btreeType);
        }

        int maxNumRecords = nodeSize / recordSize; // LOOK ?? guessing
        int maxNumRecordsPlusDesc = nodeSize / recordSize; // LOOK ?? guessing
        for (int i = 0; i < nrecords; i++) {
          Entry2 e = entries[i];
          e.childAddress = readOffset();
          e.nrecords = readVariableSize(1); // readVariableSizeMax(maxNumRecords);
          if (depth > 1)
            e.totNrecords = readVariableSize(2); // readVariableSizeMax(maxNumRecordsPlusDesc);

          if (debugBtree2)
            debugOut.println(" entry childAddress=" + e.childAddress + " nrecords=" + e.nrecords + " totNrecords=" + e.totNrecords);
        }

        int checksum = raf.readInt();
      }

      void recurse() throws IOException {
        for (int i = 0; i < entries.length; i++) {
          Entry2 e = entries[i];
          if (depth > 1) {
            InternalNode node = new InternalNode(e.childAddress, (short) e.nrecords, recordSize, depth - 1);
            node.recurse();
          } else {
            LeafNode leaf = new LeafNode(e.childAddress, (short) e.nrecords);
            leaf.addEntries(entryList);
          }

        }
      }
    }

    class LeafNode {
      Entry2[] entries;

      LeafNode(long address, short nrecords) throws IOException {
        raf.seek(getFileOffset(address));

        if (debugPos) debugOut.println("--Btree2 InternalNode position=" + raf.getFilePointer());

        // header
        byte[] sig = new byte[4];
        raf.read(sig);
        String nameS = new String(sig);
        if (!nameS.equals("BTLF"))
          throw new IllegalStateException();

        byte version = raf.readByte();
        byte nodeType = raf.readByte();
        if (nodeType != btreeType)
          throw new IllegalStateException();

        entries = new Entry2[nrecords];
        for (int i = 0; i < nrecords; i++) {
          entries[i] = new Entry2();
          entries[i].record = readRecord(btreeType);
        }

        int checksum = raf.readInt();
      }

      void addEntries(List<Entry2> list) {
        for (int i = 0; i < entries.length; i++) {
          list.add(entries[i]);
        }

      }
    }

    Object readRecord(int type) throws IOException {
      switch (type) {
        case 1:
          return new Record1();
        case 2:
          return new Record2();
        case 3:
          return new Record3();
        case 4:
          return new Record4();
        case 5:
          return new Record5();
        case 6:
          return new Record6();
        case 7: {
          return new Record70();  // LOOK wrong
        }
        case 8:
          return new Record8();
        case 9:
          return new Record9();
        default:
          throw new IllegalStateException();
      }
    }

    class Record1 {
      long hugeObjectAddress, hugeObjectLength, hugeObjectID;

      Record1() throws IOException {
        hugeObjectAddress = readOffset();
        hugeObjectLength = readLength();
        hugeObjectID = readLength();
      }
    }

    class Record2 {
      long hugeObjectAddress, hugeObjectLength, hugeObjectID, hugeObjectSize;
      int filterMask;

      Record2() throws IOException {
        hugeObjectAddress = readOffset();
        hugeObjectLength = readLength();
        filterMask = raf.readInt();
        hugeObjectSize = readLength();
        hugeObjectID = readLength();
      }
    }

    class Record3 {
      long hugeObjectAddress, hugeObjectLength;

      Record3() throws IOException {
        hugeObjectAddress = readOffset();
        hugeObjectLength = readLength();
      }
    }

    class Record4 {
      long hugeObjectAddress, hugeObjectLength, hugeObjectID, hugeObjectSize;
      int filterMask;

      Record4() throws IOException {
        hugeObjectAddress = readOffset();
        hugeObjectLength = readLength();
        filterMask = raf.readInt();
        hugeObjectSize = readLength();
      }
    }

    class Record5 {
      int nameHash;
      byte[] heapId = new byte[7];

      Record5() throws IOException {
        nameHash = raf.readInt();
        raf.read(heapId);

        //if (debugBtree2)
        //  debugOut.println("  record5 nameHash=" + nameHash + " heapId=" + showBytes(heapId));
      }
    }

    class Record6 {
      long creationOrder;
      byte[] heapId = new byte[7];

      Record6() throws IOException {
        creationOrder = raf.readLong();
        raf.read(heapId);
      }
    }

    class Record70 {
      byte location;
      int refCount;
      byte[] id = new byte[8];

      Record70() throws IOException {
        location = raf.readByte();
        refCount = raf.readInt();
        raf.read(id);
      }
    }

    class Record71 {
      byte location, messtype;
      short index;
      long address;

      Record71() throws IOException {
        location = raf.readByte();
        raf.readByte(); // skip a byte
        messtype = raf.readByte();
        index = raf.readShort();
        address = readOffset();
      }
    }

    class Record8 {
      byte flags;
      int creationOrder, nameHash;
      byte[] id = new byte[8];

      Record8() throws IOException {
        raf.read(id);
        flags = raf.readByte();
        creationOrder = raf.readInt();
        nameHash = raf.readInt();
      }
    }

    class Record9 {
      byte flags;
      int creationOrder;
      byte[] id = new byte[8];

      Record9() throws IOException {
        raf.read(id);
        flags = raf.readByte();
        creationOrder = raf.readInt();
      }
    }

  } // BTree2

  /**
   * This holds info for chunked data storage.
   * level 1A
   */
  class DataBTree {
    private Variable owner;
    private long rootNodeAddress;
    private Tiling tiling;
    private int ndimStorage, wantType;

    DataBTree(long rootNodeAddress, int[] varShape, int[] storageSize) throws IOException {
      this.rootNodeAddress = rootNodeAddress;
      this.tiling = new Tiling(varShape, storageSize);
      this.ndimStorage = storageSize.length;
      wantType = 1;
    }

    void setOwner(Variable owner) {
      this.owner = owner;
    }

    DataChunkIterator getDataChunkIterator(Section want) throws IOException {
      return new DataChunkIterator(want);
    }

    // An Iterator over the DataChunks in the btree.
    // returns only the actual data from the btree leaf (level 0) nodes.
    class DataChunkIterator {
      private Node root;
      private int[] wantOrigin;

      /**
       * Constructor
       *
       * @param want skip any nodes that are before this section
       * @throws IOException on error
       */
      DataChunkIterator(Section want) throws IOException {
        root = new Node(rootNodeAddress, -1); // should we cache the nodes ???
        wantOrigin = (want != null) ? want.getOrigin() : null;
        root.first(wantOrigin);
      }

      public boolean hasNext() {
        return root.hasNext(); //  && !node.greaterThan(wantOrigin);
      }

      public DataChunk next() throws IOException {
        return root.next();
      }
    }

    class Node {
      private long address;
      private int level, nentries;
      private Node currentNode;

      // level 0 only
      private List<DataChunk> myEntries;
      // level > 0 only
      private int[][] offset; // int[nentries][ndim]; // other levels
      private long[] childPointer; // long[nentries];

      private int currentEntry; // track iteration

      Node(long address, long parent) throws IOException {
        if (debugDataBtree) debugOut.println("\n--> DataBTree read tree at address=" + address + " parent= " + parent +
                " owner= " + owner.getNameAndDimensions() + " vinfo= " + owner.getSPobject());

        raf.order(RandomAccessFile.LITTLE_ENDIAN); // header information is in le byte order
        raf.seek(getFileOffset(address));
        this.address = address;

        byte[] name = new byte[4];
        raf.read(name);
        String nameS = new String(name);
        if (!nameS.equals("TREE"))
          throw new IllegalStateException("DataBTree doesnt start with TREE");

        int type = raf.readByte();
        level = raf.readByte();
        nentries = raf.readShort();
        if (type != wantType)
          throw new IllegalStateException("DataBTree must be type " + wantType);

        long size = 8 + 2 * sizeOffsets + nentries * (8 + sizeOffsets + 8 + ndimStorage);
        if (debugTracker) memTracker.addByLen("Data BTree (" + owner + ")", address, size);
        if (debugDataBtree)
          debugOut.println("    type=" + type + " level=" + level + " nentries=" + nentries + " size = " + size);

        long leftAddress = readOffset();
        long rightAddress = readOffset();
        if (debugDataBtree)
          debugOut.println("    leftAddress=" + leftAddress + " =0x" + Long.toHexString(leftAddress) +
                  " rightAddress=" + rightAddress + " =0x" + Long.toHexString(rightAddress));


        if (level == 0) {
          // read all entries as a DataChunk
          myEntries = new ArrayList<DataChunk>();
          for (int i = 0; i <= nentries; i++) {
            DataChunk dc = new DataChunk(ndimStorage, (i == nentries));
            myEntries.add(dc);
            if (debugDataChunk) debugOut.println(dc);
          }
        } else { // just track the offsets and node addresses
          offset = new int[nentries + 1][ndimStorage];
          childPointer = new long[nentries + 1];
          for (int i = 0; i <= nentries; i++) {
            raf.skipBytes(8); // skip size, filterMask
            for (int j = 0; j < ndimStorage; j++) {
              long loffset = raf.readLong();
              assert loffset < Integer.MAX_VALUE;
              offset[i][j] = (int) loffset;
            }
            this.childPointer[i] = (i == nentries) ? -1 : readOffset();
            if (debugDataBtree) {
              debugOut.print("    childPointer=" + childPointer[i] + " =0x" + Long.toHexString(childPointer[i]));
              for (long anOffset : offset[i]) debugOut.print(" " + anOffset);
              debugOut.println();
            }
          }
        }
      }

      // this finds the first entry we dont want to skip.
      // entry i goes from [offset(i),offset(i+1))
      // we want to skip any entries we dont need, namely those where want >= offset(i+1)
      // so keep skipping until want < offset(i+1)
      void first(int[] wantOrigin) throws IOException {
        if (level == 0) {
          for (currentEntry = 0; currentEntry < nentries; currentEntry++) {
            DataChunk entry = myEntries.get(currentEntry + 1);
            if ((wantOrigin == null) || tiling.compare(wantOrigin, entry.offset) < 0) break;
          }

        } else {
          for (currentEntry = 0; currentEntry < nentries; currentEntry++) {
            if ((wantOrigin == null) || tiling.compare(wantOrigin, offset[currentEntry + 1]) < 0) {
              currentNode = new Node(childPointer[currentEntry], this.address);
              currentNode.first(wantOrigin);
              break;
            }
          }
          assert currentNode != null;
        }
        //if (nentries == 0)
        // System.out.println("hah");
        assert (nentries == 0) || (currentEntry < nentries);
      }

      // LOOK - wouldnt be a bad idea to terminate if possible instead of running through all subsequent entries
      boolean hasNext() {
        if (level == 0) {
          return currentEntry < nentries;

        } else {
          if (currentNode.hasNext()) return true;
          return currentEntry < nentries - 1;
        }
      }

      DataChunk next() throws IOException {
        if (level == 0) {
          return myEntries.get(currentEntry++);

        } else {
          if (currentNode.hasNext())
            return currentNode.next();

          currentEntry++;
          currentNode = new Node(childPointer[currentEntry], this.address);
          currentNode.first(null);
          return currentNode.next();
        }
      }
    }

    private void dump(DataType dt, List<DataChunk> entries) {
      try {
        for (DataChunk node : entries) {
          if (dt == DataType.STRING) {
            HeapIdentifier heapId = new HeapIdentifier(node.address);
            GlobalHeap.HeapObject ho = heapId.getHeapObject();
            byte[] pa = new byte[(int) ho.dataSize];
            raf.seek(ho.dataPos);
            raf.read(pa);
            debugOut.println(" data at " + ho.dataPos + " = " + new String(pa));
          }
        }
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }

    // these are part of the level 1A data structure, type 1
    // see "Key" field (type 1) p 10
    // this is only for level 0
    class DataChunk {
      int size; // size of chunk in bytes; need storage layout dimensions to interpret
      int filterMask; // bitfield indicating which filters have been skipped for this chunk
      int[] offset; // offset index of this chunk, reletive to entire array
      long address; // address of a single raw data chunk

      DataChunk(int ndim, boolean last) throws IOException {
        this.size = raf.readInt();
        this.filterMask = raf.readInt();
        offset = new int[ndim];
        for (int i = 0; i < ndim; i++) {
          long loffset = raf.readLong();
          assert loffset < Integer.MAX_VALUE;
          offset[i] = (int) loffset;
        }
        this.address = last ? -1 : readOffset();
        if (debugTracker) memTracker.addByLen("Chunked Data (" + owner + ")", address, size);
      }

      public String toString() {
        StringBuffer sbuff = new StringBuffer();
        sbuff.append("  ChunkedDataNode size=").append(size).append(" filterMask=").append(filterMask).append(" address=").append(address).append(" offsets= ");
        for (long anOffset : offset) sbuff.append(anOffset).append(" ");
        return sbuff.toString();
      }
    }

    /* is this offset less than the "wantOrigin" ?
   private boolean lessThan(int[] wantOrigin, long[] offset) {
     if (wantOrigin == null) return true;
     int n = Math.min(offset.length, wantOrigin.length);
     for (int i=0; i<n; i++) {
       if (wantOrigin[i] < offset[i]) return true;
     }
     return false;
   } */


  } // DataBtree

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // level 1E

  class GlobalHeap {
    byte version;
    int size;
    List<HeapObject> hos = new ArrayList<HeapObject>();
    HeapObject freeSpace = null;

    GlobalHeap(long address) throws IOException {
      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(getFileOffset(address));

      // header
      byte[] heapname = new byte[4];
      raf.read(heapname);
      String nameS = new String(heapname);
      if (!nameS.equals("GCOL"))
        throw new IllegalStateException(nameS + " should equal GCOL");

      version = raf.readByte();
      raf.skipBytes(3);
      size = raf.readInt();
      if (debugDetail)
        debugOut.println("-- readGlobalHeap address=" + address + " version= " + version + " size = " + size);
      raf.skipBytes(4); // pad to 8

      int count = 0;
      while (count < size) {
        long startPos = raf.getFilePointer();
        HeapObject o = new HeapObject();
        o.id = raf.readShort();
        o.refCount = raf.readShort();
        raf.skipBytes(4);
        o.dataSize = readLength();
        o.dataPos = raf.getFilePointer();
        if (debugDetail)
          debugOut.println("   HeapObject  position=" + startPos + " id=" + o.id + " refCount= " + o.refCount +
                  " dataSize = " + o.dataSize + " dataPos = " + o.dataPos);
        if (o.id == 0) break;

        int nskip = ((int) o.dataSize) + padding((int) o.dataSize, 8);
        raf.skipBytes(nskip);
        hos.add(o);

        count += o.dataSize + 16;
      }

      if (debugDetail) debugOut.println("-- endGlobalHeap position=" + raf.getFilePointer());
      if (debugTracker) memTracker.addByLen("GlobalHeap", address, size);
    }

    class HeapObject {
      short id, refCount;
      long dataSize;
      long dataPos;

      public String toString() {
        return "dataPos = " + dataPos + " dataSize = " + dataSize;
      }
    }

  } // GlobalHeap

  // level 1D
  private class LocalHeap {
    Group group;
    int size;
    long freelistOffset, dataAddress;
    byte[] heap;
    byte version;

    LocalHeap(Group group, long address) throws IOException {
      this.group = group;

      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(getFileOffset(address));

      if (debugDetail) debugOut.println("-- readLocalHeap position=" + raf.getFilePointer());

      // header
      byte[] heapname = new byte[4];
      raf.read(heapname);
      String nameS = new String(heapname);
      if (!nameS.equals("HEAP"))
        throw new IllegalStateException();

      version = raf.readByte();
      raf.skipBytes(3);
      size = (int) readLength();
      freelistOffset = readLength();
      dataAddress = readOffset();
      if (debugDetail)
        debugOut.println(" version=" + version + " size=" + size + " freelistOffset=" + freelistOffset + " heap starts at dataAddress=" + dataAddress);
      if (debugPos) debugOut.println("    *now at position=" + raf.getFilePointer());

      // data
      raf.seek(getFileOffset(dataAddress));
      heap = new byte[size];
      raf.read(heap);
      //if (debugHeap) printBytes( out, "heap", heap, size, true);

      if (debugDetail) debugOut.println("-- endLocalHeap position=" + raf.getFilePointer());
      int hsize = 8 + 2 * sizeLengths + sizeOffsets;
      if (debugTracker) memTracker.addByLen("Group LocalHeap (" + group.dname + ")", address, hsize);
      if (debugTracker) memTracker.addByLen("Group LocalHeapData (" + group.dname + ")", dataAddress, size);
    }

    public String getString(int offset) {
      int count = 0;
      while (heap[offset + count] != 0) count++;
      return new String(heap, offset, count);
    }

  } // LocalHeap

  // aka Group Entry "level 1C"
  private class SymbolTableEntry {
    long nameOffset, objectHeaderAddress;
    long btreeAddress, nameHeapAddress;
    int cacheType, linkOffset;
    long posData;

    boolean isSymbolicLink = false;

    SymbolTableEntry(long filePos) throws IOException {
      raf.seek(filePos);
      if (debugSymbolTable) debugOut.println("--> readSymbolTableEntry position=" + raf.getFilePointer());

      nameOffset = readOffset();
      objectHeaderAddress = readOffset();
      cacheType = raf.readInt();
      raf.skipBytes(4);

      if (debugSymbolTable) {
        debugOut.print(" nameOffset=" + nameOffset);
        debugOut.print(" objectHeaderAddress=" + objectHeaderAddress);
        debugOut.println(" cacheType=" + cacheType);
      }

      // "scratch pad"
      posData = raf.getFilePointer();
      if (debugSymbolTable) dump("Group Entry scratch pad", posData, 16, false);

      if (cacheType == 1) {
        btreeAddress = readOffset();
        nameHeapAddress = readOffset();
        if (debugSymbolTable) debugOut.println("btreeAddress=" + btreeAddress + " nameHeadAddress=" + nameHeapAddress);
      }

      // check for symbolic link
      if (cacheType == 2) {
        linkOffset = raf.readInt(); // offset in local heap
        if (debugSymbolTable) debugOut.println("WARNING Symbolic Link linkOffset=" + linkOffset);
        isSymbolicLink = true;
      }

      /* if (cacheType == 1) {
       btreeAddress = mapBuffer.getLong();
       nameHeapAddress = mapBuffer.getLong();
       debugOut.println(" btreeAddress="+btreeAddress);
       debugOut.println(" nameHeapAddress="+nameHeapAddress);
       nameHeap = new LocalHeap();
       nameHeap.read(nameHeapAddress);

       btree = new Btree();
       btree.read(btreeAddress);

     } else if (cacheType == 2) {
       linkOffset = mapBuffer.getLong();
       debugOut.println(" linkOffset="+linkOffset);
     } else {
       for (int k=0; k<2; k++)
         debugOut.println( " "+k+" "+mapBuffer.getLong());
     } */

      if (debugSymbolTable)
        debugOut.println("<-- end readSymbolTableEntry position=" + raf.getFilePointer());

      memTracker.add("SymbolTableEntry", filePos, posData + 16);
    }

    public int getSize() {
      return 40;
    }

    public long getObjectAddress() {
      return objectHeaderAddress;
    }

    public long getNameOffset() {
      return nameOffset;
    }
  } // SymbolTable

  // level 1E "Fractal Heap" used for both Global and Local heaps in 1.8.0+
  private class FractalHeap {
    Group group;
    short heapIdLen;
    byte flags;
    int maxSizeOfObjects;
    long nextHugeObjectId, freeSpace, managedSpace, allocatedManagedSpace, offsetDirectBlock,
            nManagedObjects, sizeHugeObjects, nHugeObjects, sizeTinyObjects, nTinyObjects;
    long btreeAddress, freeSpaceTrackerAddress;

    short maxHeapSize, startingNumRows, currentNumRows;
    long maxDirectBlockSize;

    long rootBlockAddress;

    // filters
    short ioFilterLen;
    long sizeFilteredRootDirectBlock;
    int ioFilterMask;
    byte[] ioFilterInfo;

    DoublingTable doublingTable;

    FractalHeap(Group group, long address) throws IOException {
      this.group = group;

      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(getFileOffset(address));

      if (debugDetail) debugOut.println("-- readFractalHeap position=" + raf.getFilePointer());

      // header
      byte[] heapname = new byte[4];
      raf.read(heapname);
      String nameS = new String(heapname);
      if (!nameS.equals("FRHP"))
        throw new IllegalStateException();

      byte version = raf.readByte();
      heapIdLen = raf.readShort(); // bytes
      ioFilterLen = raf.readShort();  // bytes
      flags = raf.readByte();

      maxSizeOfObjects = raf.readInt(); // greater than this are huge objects
      nextHugeObjectId = readLength(); // next id to use for a huge object
      btreeAddress = readOffset(); // v2 btee to track huge objects
      freeSpace = readLength();  // total free space in managed direct blocks
      freeSpaceTrackerAddress = readOffset();
      managedSpace = readLength(); // total amount of managed space in the heap
      allocatedManagedSpace = readLength(); // total amount of managed space in the heap actually allocated
      offsetDirectBlock = readLength(); // linear heap offset where next direct clock should be allocated
      nManagedObjects = readLength();  // number of managed objects in the heap
      sizeHugeObjects = readLength(); // total size of huge objects in the heap (in bytes)
      nHugeObjects = readLength(); // number huge objects in the heap
      sizeTinyObjects = readLength(); // total size of tiny objects packed in heap Ids (in bytes)
      nTinyObjects = readLength(); // number of tiny objects packed in heap Ids

      short tableWidth = raf.readShort(); // number of columns in the doubling table for managed blocks, must be power of 2
      long startingBlockSize = readLength(); // starting direct block size in bytes, must be power of 2
      maxDirectBlockSize = readLength(); // maximum direct block size in bytes, must be power of 2
      maxHeapSize = raf.readShort(); // log2 of the maximum size of heap's linear address space, in bytes
      startingNumRows = raf.readShort(); // starting number of rows of the root indirect block, 0 = maximum needed
      rootBlockAddress = readOffset(); // can be undefined if no data
      currentNumRows = raf.readShort(); // starting number of rows of the root indirect block, 0 = direct block

      boolean hasFilters = (ioFilterLen > 0);
      if (hasFilters) {
        sizeFilteredRootDirectBlock = readLength();
        ioFilterMask = raf.readInt();
        ioFilterInfo = new byte[ioFilterLen];
        raf.read(ioFilterInfo);
      }
      int checksum = raf.readInt();

      if (debugDetail || debugFractalHeap) {
        debugOut.println("FractalHeap version=" + version + " heapIdLen=" + heapIdLen + " ioFilterLen=" + ioFilterLen + " flags= " + flags);
        debugOut.println(" maxSizeOfObjects=" + maxSizeOfObjects + " nextHugeObjectId=" + nextHugeObjectId + " btreeAddress="
                + btreeAddress + " managedSpace=" + managedSpace + " allocatedManagedSpace=" + allocatedManagedSpace + " freeSpace=" + freeSpace);
        debugOut.println(" nManagedObjects=" + nManagedObjects + " nHugeObjects= " + nHugeObjects + " nTinyObjects=" + nTinyObjects +
                " maxDirectBlockSize=" + maxDirectBlockSize + " maxHeapSize= 2^" + maxHeapSize);
        debugOut.println(" DoublingTable: tableWidth=" + tableWidth + " startingBlockSize=" + startingBlockSize);
        debugOut.println(" rootBlockAddress=" + rootBlockAddress + " startingNumRows=" + startingNumRows + " currentNumRows=" + currentNumRows);
      }
      if (debugPos) debugOut.println("    *now at position=" + raf.getFilePointer());

      long pos = raf.getFilePointer();
      if (debugDetail) debugOut.println("-- end FractalHeap position=" + raf.getFilePointer());
      int hsize = 8 + 2 * sizeLengths + sizeOffsets;
      if (debugTracker) memTracker.add("Group FractalHeap (" + group.dname + ")", address, pos);

      doublingTable = new DoublingTable(tableWidth, startingBlockSize, allocatedManagedSpace, maxDirectBlockSize);

      // data
      if (currentNumRows == 0) {
        DataBlock dblock = new DataBlock();
        doublingTable.blockList.add(dblock);
        readDirectBlock(getFileOffset(rootBlockAddress), address, dblock);

      } else {
        //int nrows = SpecialMathFunction.log2(iblock_size - SpecialMathFunction.log2(startingBlockSize*tableWidth))+1;
        //int maxrows_directBlocks = (int) (SpecialMathFunction.log2(maxDirectBlockSize) - SpecialMathFunction.log2(startingBlockSize)) + 2;

        readIndirectBlock(getFileOffset(rootBlockAddress), address, hasFilters);
      }

      // read in the direct blocks
      for (DataBlock dblock : doublingTable.blockList) {
        readDirectBlock(getFileOffset(dblock.address), address, dblock);
      }

      doublingTable.assignSizes();
    }


    MessageLink getLink(byte[] heapId) throws IOException {
      int type = (heapId[0] & 0x30) >> 4;
      int n = maxHeapSize / 8;
      int m = getNumBytesFromMax(maxDirectBlockSize - 1);

      int offset = makeIntFromBytes(heapId, 1, n);
      int size = makeIntFromBytes(heapId, 1 + n, m);
      //System.out.println("Heap id =" + showBytes(heapId) + " type = " + type + " n= " + n + " m= " + m + " offset= " + offset + " size= " + size);

      long pos = doublingTable.getPos(offset);
      /* byte[] data = new byte[size];
      raf.seek(pos);
      raf.read(data);
      System.out.println(" heap ID pos="+pos+" vals = " + new String(data) + " == " + showBytes(data));  */

      raf.seek(pos);
      MessageLink lm = new MessageLink();
      lm.read();
      if (debugBtree2) System.out.println("    linkMessage="+lm);
      return lm;
    }

    private class DoublingTable {
      int tableWidth;
      long startingBlockSize, managedSpace, maxDirectBlockSize;
      int nrows, nDirectRows, nIndirectRows;
      List<DataBlock> blockList;

      DoublingTable(int tableWidth, long startingBlockSize, long managedSpace, long maxDirectBlockSize) {
        this.tableWidth = tableWidth;
        this.startingBlockSize = startingBlockSize;
        this.managedSpace = managedSpace;

        nrows = calcNrows(managedSpace);
        int maxDirectRows = calcNrows(maxDirectBlockSize);
        if (nrows > maxDirectRows) {
          nDirectRows = maxDirectRows;
          nIndirectRows = nrows - maxDirectRows;
        } else {
          nDirectRows = nrows;
          nIndirectRows = 0;
        }

        blockList = new ArrayList<DataBlock>(tableWidth * nrows);
      }

      private int calcNrows(long max) {
        int n = 0;
        long sizeInBytes = 0;
        long blockSize = startingBlockSize;
        while (sizeInBytes < max) {
          sizeInBytes += blockSize * tableWidth;
          n++;
          if (n > 1) blockSize *= 2;
        }
        return n;
      }

      void assignSizes() {
        int block = 0;
        long blockSize = startingBlockSize;
        for (DataBlock db : blockList) {
          db.size = blockSize;
          block++;
          if ((block % tableWidth == 0) && (block / tableWidth > 1))
            blockSize *= 2;
        }
      }

      long getPos(long offset) {
        int block = 0;
        for (DataBlock db : blockList) {
          if ((offset >= db.offset) && (offset < db.offset + db.size)) {
            long localOffset = offset - db.offset;
            //System.out.println("   heap ID find block= "+block+" db.dataPos " + db.dataPos+" localOffset= "+localOffset);
            return db.dataPos + localOffset;
          }
          block++;
        }
        throw new IllegalStateException("offset=" + offset);
      }

    }

    private class DataBlock {
      long address;
      long sizeFilteredDirectBlock;
      int filterMask;

      long dataPos;
      long offset;
      long size;
    }

    void readIndirectBlock(long pos, long heapAddress, boolean hasFilter) throws IOException {
      raf.seek(pos);

      // header
      byte[] heapname = new byte[4];
      raf.read(heapname);
      String nameS = new String(heapname);
      if (!nameS.equals("FHIB"))
        throw new IllegalStateException();

      byte version = raf.readByte();
      long heapHeaderAddress = readOffset();
      if (heapAddress != heapHeaderAddress)
        throw new IllegalStateException();

      int nbytes = maxHeapSize / 8;
      if (maxHeapSize % 8 != 0) nbytes++;
      long blockOffset = readVariableSize(nbytes);

      if (debugDetail || debugFractalHeap) {
        debugOut.println(" -- FH IndirectBlock version=" + version + " blockOffset= " + blockOffset);
      }

      long npos = raf.getFilePointer();
      if (debugPos) debugOut.println("    *now at position=" + npos);

      // child direct blocks
      int nDirectChildren = doublingTable.tableWidth * doublingTable.nDirectRows;
      for (int i = 0; i < nDirectChildren; i++) {
        DataBlock directChild = new DataBlock();
        directChild.address = readOffset();
        if (hasFilter) {
          directChild.sizeFilteredDirectBlock = readLength();
          directChild.filterMask = raf.readInt();
        }
        if (debugDetail || debugFractalHeap)
          debugOut.println("  DirectChild "+i+" address= " + directChild.address);

        if (directChild.address >= 0)
          doublingTable.blockList.add(directChild);
      }

      // child indirect blocks LOOK not sure if order is correct, this is depth first...
      int nIndirectChildren = doublingTable.tableWidth * doublingTable.nIndirectRows;
      for (int i = 0; i < nIndirectChildren; i++) {
        long childIndirectAddress = readOffset();
        if (debugDetail || debugFractalHeap)
          debugOut.println("  InDirectChild "+i+" address= " + childIndirectAddress);
        if (childIndirectAddress >= 0)
          readIndirectBlock(childIndirectAddress, heapAddress, hasFilter);
      }

    }

    void readDirectBlock(long pos, long heapAddress, DataBlock dblock) throws IOException {
      raf.seek(pos);

      // header
      byte[] heapname = new byte[4];
      raf.read(heapname);
      String nameS = new String(heapname);
      if (!nameS.equals("FHDB"))
        throw new IllegalStateException();

      byte version = raf.readByte();
      long heapHeaderAddress = readOffset();
      if (heapAddress != heapHeaderAddress)
        throw new IllegalStateException();

      int nbytes = maxHeapSize / 8;
      if (maxHeapSize % 8 != 0) nbytes++;
      dblock.offset = readVariableSize(nbytes);
      dblock.dataPos = pos; // raf.getFilePointer();

      if (debugDetail || debugFractalHeap)
        debugOut.println("  DirectBlock offset= " + dblock.offset + " dataPos = " + dblock.dataPos);
    }

  } // FractalHeap

  ///////////////////////////////////////////////////////////////
  // construct netcdf objects

  private void makeNetcdfGroup(ucar.nc2.Group ncGroup, DataObject dataObject) throws IOException {
    Group h5group = dataObject.group;
    if (h5group == null) return;

    // create group attributes
    for (Message mess : dataObject.messages) {
      if (mess.mtype == MessageType.Attribute) {
        MessageAttribute matt = (MessageAttribute) mess.messData;
        makeAttributes(h5group.name, matt, ncGroup.getAttributes());
      }
    }

    // add system attributes
    processSystemAttributes(dataObject.messages, ncGroup.getAttributes());

    // look for dimension scales
    List<DataObject> nestedObjects = h5group.nestedObjects;
    for (DataObject ndo : nestedObjects) {
      if (ndo.isVariable)
        findDimensionScales( ncGroup, ndo);
    }
    createDimensions( ncGroup);

    // nested objects - groups and variables
    for (DataObject ndo : nestedObjects) {

      if (ndo.group != null) {
        ucar.nc2.Group nestedGroup = new ucar.nc2.Group(ncfile, ncGroup, NetcdfFile.createValidNetcdfObjectName(ndo.group.name));
        ncGroup.addGroup(nestedGroup);
        if (debug1) debugOut.println("--made Group " + nestedGroup.getName() + " add to " + ncGroup.getName());
        makeNetcdfGroup(nestedGroup, ndo);

      } else {

        if (ndo.isVariable) {
          if (debugReference && ndo.mdt.type == 7) debugOut.println(ndo);

          Variable v = makeVariable(ndo);
          if ((v != null) && (v.getDataType() != null)) {
            v.setParentGroup(ncGroup);
            ncGroup.addVariable(v);

            Vinfo vinfo = (Vinfo) v.getSPobject();
            if (debugV) debugOut.println("  made Variable " + v.getName() + "  vinfo= " + vinfo + "\n" + v);
          }
        } else if (!ndo.isDimensionNotVariable && warnings) {
          debugOut.println("WARN:  DataObject ndo " + ndo + " not a Group or a Variable");
        }

      }
    } // loop over nested objects

  }

  private Map<String, Dimension> dimMap = new HashMap<String, Dimension>();
  private void findDimensionScales(ucar.nc2.Group g, DataObject ndo) throws IOException {
    Iterator<Message> iter = ndo.messages.iterator();
    while (iter.hasNext()) {
      Message mess = iter.next();
      if (mess.mtype != MessageType.Attribute) continue;
      MessageAttribute matt = (MessageAttribute) mess.messData;

      // find the dimensions - set length to maximum
      if (matt.name.equals("DIMENSION_LIST")) {
        Attribute att = makeAttribute(ndo.name, matt.name, matt.mdt, matt.mds, matt.dataPos);
        StringBuffer sbuff = new StringBuffer();
        for (int i = 0; i < att.getLength(); i++) {
          String name = att.getStringValue(i);
          int pos = name.lastIndexOf("/");
          if (pos >= 0)
            name = name.substring(pos+1);
          sbuff.append(name).append(" ");
          addDimension(name, ndo.mds.dimLength[i], ndo.mds.maxLength[i] == -1);
        }
        ndo.dimList = sbuff.toString();
        iter.remove();

      } else if (matt.name.equals("NAME")) {
        Attribute att = makeAttribute(ndo.name, matt.name, matt.mdt, matt.mds, matt.dataPos);
        String val = att.getStringValue();
        if (val.startsWith("This is a netCDF dimension but not a netCDF variable")) {
          ndo.isVariable = false;
          ndo.isDimensionNotVariable = true;
        } 
        iter.remove();
      }
      else if (matt.name.equals("CLASS")) {
        Attribute att = makeAttribute(ndo.name, matt.name, matt.mdt, matt.mds, matt.dataPos);
        String val = att.getStringValue();
        if (val.equals("DIMENSION_SCALE")) {
          addDimension(ndo.name, ndo.mds.dimLength[0], ndo.mds.maxLength[0] == -1);
          ndo.dimList = ndo.name;
          iter.remove();
        }
      }
      else if (matt.name.equals("REFERENCE_LIST")) iter.remove();
    }
  }

  private void addDimension(String name, int length, boolean isUnlimited) {
    Dimension d = dimMap.get(name);
    if (d == null) {
      d = new Dimension(name, length, true, isUnlimited, false);
      dimMap.put(name, d);
    } else {
      if (length > d.getLength())
        d.setLength(length);
    }
  }

  private void createDimensions(ucar.nc2.Group g) throws IOException {
    for (Dimension d : dimMap.values()) {
      g.addDimension(d);
    }
  }

  /**
   * Create Attribute objects from the MessageAttribute and add to list
   *
   * @param forWho  whose attribue?
   * @param matt    attribute message
   * @param attList add Attribute to this list
   * @throws IOException on io error
   */
  private void makeAttributes(String forWho, MessageAttribute matt, List<Attribute> attList) throws IOException {
    MessageDatatype mdt = matt.mdt;

    /* if (mdt.type == 6) { // structure : make seperate attribute for each member
      for (StructureMember m : mdt.members) {
        String attName = matt.name + "." + m.name;
        //if ((prefix != null) && (prefix.length() > 0)) attName = prefix + "." + attName;
        //String attName = matt.name;

        /* if (m.mdt.type == 6) // LOOK nested compound attributes
            makeAttributes( forWho, m.matt, attList);
          else
            attList.add(makeAttribute(forWho, attName, m.mdt, matt.mds, matt.dataPos + m.offset));
      }
    } else { */

    if (mdt.type != 6) {
      // String attName = (prefix == null) || prefix.equals("") ? matt.name : prefix + "." + matt.name;
      //String attName = matt.name;
      Attribute att = makeAttribute(forWho, matt.name, matt.mdt, matt.mds, matt.dataPos);
      if (att != null)
        attList.add(att);
    }

    // reading attribute values might change byte order during a read
    // put back to little endian for further header processing
    raf.order(RandomAccessFile.LITTLE_ENDIAN);
  }

  /* private void makeNC4Dimension(String varName, MessageAttribute matt, MessageDatatype mdt) throws IOException {
    Attribute attLen = null, attName = null;
    for (StructureMember m : mdt.members) {
      Attribute att = makeAttribute(varName, m.name, m.mdt, matt.mds, matt.dataPos + m.offset);
      if (m.name.equals("len")) attLen = att;
      else attName = att;
    }
    int len = attLen.getNumericValue().intValue();
    String name = attName.getStringValue();
    Dimension dim = new Dimension(name, len, true);
    ncfile.addDimension(null, dim);
    // track it by dim id
    int dimId = -1;
    String dimIdS = matt.name.substring(7);
    try {
      dimId = Integer.parseInt(dimIdS);
    } catch (NumberFormatException e) {
    }
    dimTable.put(dimId, dim);
    if (debug1) debugOut.println("makeNC4Dimension " + matt.name);
  }

  private void makeNC4Variable(String forWho, MessageAttribute matt, MessageDatatype mdt) throws IOException {
    Attribute attDims = null, attName = null, attNDims = null;
    for (StructureMember m : mdt.members) {
      if (m.name.equals("name")) attName = makeAttribute(forWho, m.name, m.mdt, matt.mds, matt.dataPos + m.offset);
      if (m.name.equals("ndims")) attNDims = makeAttribute(forWho, m.name, m.mdt, matt.mds, matt.dataPos + m.offset);
      if (m.name.equals("dimids")) attDims = makeAttribute(forWho, m.name, m.mdt, matt.mds, matt.dataPos + m.offset);
    }
    String name = attName.getStringValue().replace(' ', '_');
    int ndims = attNDims.getNumericValue().intValue();
    List<Dimension> dimList = new ArrayList<Dimension>();
    for (int i = 0; i < ndims; i++) {
      int dimId = attDims.getNumericValue(i).intValue();
      Dimension dim = dimTable.get(dimId);
      dimList.add(dim);
    }
    varTable.put(matt.name, new Vatt(name, dimList));
    if (debug1) debugOut.println("makeNC4Variable " + matt.name);
  } */

  private Attribute makeAttribute(String forWho, String attName, MessageDatatype mdt, MessageDataspace mds, long dataPos) throws IOException {
    ucar.ma2.Array data;
    attName = NetcdfFile.createValidNetcdfObjectName(attName); // this means that cannot search by name

    Variable v = new Variable(ncfile, null, null, attName); // LOOK null group
    Vinfo vinfo = new Vinfo(mdt, mds, dataPos);
    if (!makeVariableShapeAndType(v, mdt, mds, vinfo, null)) {
      debugOut.println("SKIPPING attribute " + attName + " for " + forWho + " with dataType= " + vinfo.hdfType);
      return null;
    }

    /* if (mdt.type == 9) {
      int nelems = mds.dimLength[0];
      for (int i = 0; i < nelems; i++) {
        long address = dataPos + mdt.byteSize * i;
        H5header.HeapIdentifier heapId = getHeapIdentifier(address);
        debugOut.println("HeapIdentifier address=" + address + heapId);
        GlobalHeap.HeapObject ho = heapId.getHeapObject();
        debugOut.println(" readString at HeapObject " + ho);      
      }

      debugOut.println("SKIPPING attribute " + attName + " for " + forWho + " with dataType= " + mdt.type);
      return null;

    } else */

    if (mdt.type == 7) {
      if (mdt.referenceType == 0)
        data = readReferenceObjectNames(v);
      else {
        debugOut.println("SKIPPING attribute " + attName + " for " + forWho + " with referenceType= " + mdt.referenceType);
        return null;
      }

    } else {

      v.setSPobject(vinfo);
      vinfo.setOwner(v);
      v.setCaching(false);
      if (debug1) debugOut.println("makeAttribute " + attName + " for " + forWho + "; vinfo= " + vinfo);

      try {
        data = h5iosp.readData( v, v.getShapeAsSection());
        //ucar.ma2.Array data = v.read();

      } catch (InvalidRangeException e) {
        log.error("H5header.makeAttribute", e);
        return null;
      }
    }

    return new Attribute(attName, data);

  }

  /* private Array readAttributeData() {

      // deal with reference type
      /* Dataset region references are stored as a heap-ID which points to the following information within the file-heap:
           an offset of the object pointed to,
           number-type information (same format as header message),
           dimensionality information (same format as header message),
           sub-set start and end information (i.e. a coordinate location for each),
           and field start and end names (i.e. a [pointer to the] string indicating the first field included and a [pointer to the] string name for the last field).
      if (mdt.type == 7) { // reference
        // datapos points to a position of the refrenced object, i think
        raf.seek(dataPos);
        long referencedObjectPos = readOffset();
        //debugOut.println("WARNING   Reference at "+dataPos+" referencedObjectPos = "+referencedObjectPos);

        // LOOK, should only read this once
        DataObject referencedObject = new DataObject(null, "att", referencedObjectPos);
        referencedObject.read();
        mdt = referencedObject.mdt;
        mds = referencedObject.msd;
        dataPos = referencedObject.msl.dataAddress; // LOOK - should this be converted to filePos?
      }

    try {
      ucar.ma2.Array data = h5iosp.readData( v, v.getShapeAsSection());
      //ucar.ma2.Array data = v.read();
      return new Attribute(attName, data);

    } catch (InvalidRangeException e) {
      log.error("H5header.makeAttribute", e);
      return null;
    }

  }   */

  /*
     A dataset has Datatype, Dataspace, StorageLayout.
     A structure member only has Datatype.
     An array is specified through Datatype=10. Storage is speced in the parent.
     dataPos must be absolute.

               Variable v = makeVariable(ndo.name, ndo.messages, getFileOffset(ndo.msl.dataAddress), ndo.mdt,
              ndo.msl, ndo.mds, ndo.mfp)
  */
  private Variable makeVariable(DataObject ndo) throws IOException {

    Vinfo vinfo = new Vinfo(ndo);
    if (vinfo.getNCDataType() == null) {
      debugOut.println("SKIPPING DataType= " + vinfo.hdfType + " for variable " + ndo.name);
      return null;
    }

    // deal with filters, cant do SZIP
    if (ndo.mfp != null) {
      for (Filter f : ndo.mfp.filters) {
        if (f.id == 4) {
          debugOut.println("SKIPPING variable with SZIP Filter= " + ndo.mfp + " for variable " + ndo.name);
          return null;
        }
      }
    }

    // find fill value
    Attribute fillAttribute = null;
    for (Message mess : ndo.messages) {
      if (mess.mtype == MessageType.FillValue) {
        MessageFillValue fvm = (MessageFillValue) mess.messData;
        if (fvm.hasFillValue)
          vinfo.fillValue = fvm.value;

      } else if (mess.mtype == MessageType.FillValueOld) {
        MessageFillValueOld fvm = (MessageFillValueOld) mess.messData;
        if (fvm.size > 0)
          vinfo.fillValue = fvm.value;
      }

      Object fillValue = vinfo.getFillValueNonDefault();
      if (fillValue != null) {
        Object defFillValue = vinfo.getFillValueDefault();
        if (!fillValue.equals(defFillValue))
          fillAttribute = new Attribute("_FillValue", (Number) fillValue);
      }
    }

    // deal with unallocated data
    long dataPos = getFileOffset(ndo.msl.dataAddress);
    if (dataPos == -1) {
      vinfo.useFillValue = true;

      // if didnt find, use zeroes !!
      if (vinfo.fillValue == null) {
        vinfo.fillValue = new byte[vinfo.dataType.getSize()];
      }
    }

    Variable v;
    if (ndo.mdt.type == 6) { // Compound
      String vname = NetcdfFile.createValidNetcdfObjectName(ndo.name); // look cannot search by name
      v = new Structure(ncfile, null, null, vname); // LOOK null group
      if (!makeVariableShapeAndType(v, ndo.mdt, ndo.mds, vinfo, ndo.dimList)) return null;
      addMembersToStructure((Structure) v, ndo.mdt);
      v.setElementSize(ndo.mdt.byteSize);

    } else {
      String vname = NetcdfFile.createValidNetcdfObjectName(ndo.name); // look cannot search by name
      v = new Variable(ncfile, null, null, vname);  // LOOK null group
      if (!makeVariableShapeAndType(v, ndo.mdt, ndo.mds, vinfo, ndo.dimList)) return null;
    }

    // special case of variable length strings
    if (v.getDataType() == DataType.STRING)
      v.setElementSize(16); // because the array has elements that are HeapIdentifier

    v.setSPobject(vinfo);

    // look for attributes
    for (Message mess : ndo.messages) {
      if (mess.mtype == MessageType.Attribute) {
        MessageAttribute matt = (MessageAttribute) mess.messData;
        makeAttributes(ndo.name, matt, v.getAttributes());
      }
    }
    processSystemAttributes(ndo.messages, v.getAttributes());
    if (fillAttribute != null)
      v.addAttribute(fillAttribute);

    if (!vinfo.signed)
      v.addAttribute(new Attribute("_unsigned", "true"));

    if (vinfo.isChunked) // make the data btree, but entries are not read in
      vinfo.btree = new DataBTree(dataPos, v.getShape(), vinfo.storageSize);

    if (transformReference && (ndo.mdt.type == 7) && (ndo.mdt.referenceType == 0)) { // object reference
      Array newData = readReferenceObjectNames(v);
      v.setDataType(DataType.STRING);
      v.setCachedData(newData, true); // so H5iosp.read() is never called
      v.addAttribute(new Attribute("_HDF5ReferenceType", "values are names of referenced Variables"));
    }

    if (transformReference && (ndo.mdt.type == 7) && (ndo.mdt.referenceType == 1)) { // region reference
      int nelems = (int) v.getSize();
      int heapIdSize = 12;
      for (int i = 0; i < nelems; i++) {
        H5header.RegionReference heapId = new RegionReference(vinfo.dataPos + heapIdSize * i);
      }

      v.addAttribute(new Attribute("_HDF5ReferenceType", "values are regions of referenced Variables"));
    }

    // debugging
    vinfo.setOwner(v);
    if (vinfo.hdfType == 7) debugOut.println("WARN:  Variable " + ndo.name + " is a Reference type");
    if (vinfo.mfp != null) debugOut.println("WARN:  Variable " + ndo.name + " has a Filter");

    return v;
  }

  private Array readReferenceObjectNames(Variable v) throws IOException {
    Array data = v.read();
    IndexIterator ii = data.getIndexIterator();

    Array newData = Array.factory(DataType.STRING, v.getShape());
    IndexIterator ii2 = newData.getIndexIterator();
    while (ii.hasNext()) {
      long objId = ii.getLongNext();
      DataObject dobj = findDataObject(objId);
      if (dobj == null)
        System.out.println("Cant find dobj= " + dobj);
      else {
        System.out.println(" Referenced object= " + dobj.getName());
        ii2.setObjectNext(dobj.getName());
      }
    }
    return newData;
  }  

  /*
     Used for Structure Members
  */
  private Variable makeMemberVariable(String name, long dataPos, MessageDatatype mdt)
          throws IOException {

    Variable v;
    Vinfo vinfo = new Vinfo(mdt, null, dataPos); // LOOK need mds
    if (vinfo.getNCDataType() == null) {
      debugOut.println("SKIPPING DataType= " + vinfo.hdfType + " for variable " + name);
      return null;
    }

    if (mdt.type == 6) {
      String vname = NetcdfFile.createValidNetcdfObjectName(name); // look cannot search by name
      v = new Structure(ncfile, null, null, vname); // LOOK null group
      makeVariableShapeAndType(v, mdt, null, vinfo, null);
      addMembersToStructure((Structure) v, mdt);
      v.setElementSize(mdt.byteSize);

    } else {
      String vname = NetcdfFile.createValidNetcdfObjectName(name); // look cannot search by name
      v = new Variable(ncfile, null, null, vname);  // LOOK null group
      makeVariableShapeAndType(v, mdt, null, vinfo, null);
    }

    // special case of variable length strings
    if (v.getDataType() == DataType.STRING)
      v.setElementSize(16); // because the array has elements that are HeapIdentifier

    v.setSPobject(vinfo);
    vinfo.setOwner(v);

    if (!vinfo.signed)
      v.addAttribute(new Attribute("_unsigned", "true"));

    return v;
  }

  private DateFormatter formatter = new DateFormatter();
  private java.text.SimpleDateFormat hdfDateParser;

  private java.text.SimpleDateFormat getDateFormatter() {
    if (hdfDateParser == null) {
      hdfDateParser = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
      hdfDateParser.setTimeZone(java.util.TimeZone.getTimeZone("GMT")); // same as UTC
    }
    return hdfDateParser;
  }

  private void processSystemAttributes(List<Message> messages, List<Attribute> attributes) {
    for (Message mess : messages) {
      if (mess.mtype == MessageType.LastModified) {
        MessageLastModified m = (MessageLastModified) mess.messData;
        Date d = new Date((long) m.secs * 1000);
        attributes.add(new Attribute("_LastModified", formatter.toDateTimeStringISO(d)));

      } else if (mess.mtype == MessageType.LastModifiedOld) {
        MessageLastModifiedOld m = (MessageLastModifiedOld) mess.messData;
        try {
          getDateFormatter();
          Date d = getDateFormatter().parse(m.datemod);
          attributes.add(new Attribute("_LastModified", formatter.toDateTimeStringISO(d)));
        }
        catch (ParseException ex) {
          debugOut.println("ERROR parsing date from MessageLastModifiedOld = " + m.datemod);
        }

      } else if (mess.mtype == MessageType.Comment) {
        MessageComment m = (MessageComment) mess.messData;
        attributes.add(new Attribute("_Description", NetcdfFile.createValidNetcdfObjectName(m.name)));
      }
    }
  }


  private void addMembersToStructure(Structure s, MessageDatatype mdt) throws IOException {
    for (StructureMember m : mdt.members) {
      Variable v = makeMemberVariable(m.name, m.offset, m.mdt);
      if (v != null) {
        s.addMemberVariable(v);
        if (debug1) debugOut.println("  made Member Variable " + v.getName() + "\n" + v);
      }
    }
  }

  private boolean makeVariableShapeAndType(Variable v, MessageDatatype mdt, MessageDataspace msd, Vinfo vinfo, String dims) {

    int[] dim = (msd != null) ? msd.dimLength : new int[0];
    if (mdt.type == 10) {
      int len = dim.length + mdt.dim.length;
      int[] combinedDim = new int[len];
      for (int i = 0; i < dim.length; i++)
        combinedDim[i] = dim[i];  // the dataspace is the outer (slow) dimensions
      for (int i = 0; i < mdt.dim.length; i++)
        combinedDim[dim.length + i] = mdt.dim[i];  // type 10 is the inner dimensions
      dim = combinedDim;
    }

    try {
      if (dims != null) {
        v.setDimensions( dims);

      } else if (mdt.type == 3) { // fixed length string - DataType.CHAR, add string length

        if (dim == null) // scalar string member variable
          v.setDimensionsAnonymous(new int[]{mdt.byteSize});
        else {
          int[] shape = new int[dim.length + 1];
          System.arraycopy(dim, 0, shape, 0, dim.length);
          shape[dim.length] = mdt.byteSize;
          v.setDimensionsAnonymous(shape);
        }

        /* }  else if (mdt.type == 9) { // variable length string
       int[] shape = new int[1];
       shape[0] = vinfo.byteSize;
       v.setShapeWithAnonDimensions(shape);

     } else if (mdt.type == 10) { // array
       v.setShapeWithAnonDimensions(mdt.dim); */

      } else {
        if (dim == null) dim = new int[0]; // scaler
        v.setDimensionsAnonymous(dim);
      }
    } catch (InvalidRangeException ee) {
      log.error(ee.getMessage());
      debugOut.println("ERROR: makeVariableShapeAndType " + ee.getMessage());
      return false;
    }

    DataType dt = vinfo.getNCDataType();
    if (dt == null) return false;
    v.setDataType(dt);
    return true;
  }

  //////////////////////////////////////////////////////////////
  // utilities

  int makeIntFromBytes(byte[] bb, int start, int n) {
    int result = 0;
    for (int i = start + n - 1; i >= start; i--) {
      result <<= 8;
      byte b = bb[i];
      result += (b < 0) ? b + 256 : b;
    }
    return result;
  }

  /**
   * Read a zero terminated String. Leave file positioned after zero terminator byte.
   *
   * @param raf from this file
   * @return String (dont include zero terminator)
   * @throws java.io.IOException on io error
   */
  static String readString(RandomAccessFile raf) throws IOException {
    long filePos = raf.getFilePointer();

    int count = 0;
    while (raf.readByte() != 0) count++;

    raf.seek(filePos);
    byte[] s = new byte[count];
    raf.read(s);
    raf.readByte(); // skip the zero byte!
    return new String(s);
  }

  /**
   * Read a zero terminated String at current position; advance file to a multiple of 8.
   *
   * @param raf from this file
   * @return String (dont include zero terminator)
   * @throws java.io.IOException on io error
   */
  static String readString8(RandomAccessFile raf) throws IOException {
    long filePos = raf.getFilePointer();

    int count = 0;
    while (raf.readByte() != 0) count++;

    raf.seek(filePos);
    byte[] s = new byte[count];
    raf.read(s);

    // skip to 8 byte boundary, note zero byte is skipped
    count++;
    count += padding(count, 8);
    raf.seek(filePos + count);

    return new String(s);
  }

  /**
   * Read a fixed length String.
   *
   * @param size number of bytes
   * @return String result
   * @throws java.io.IOException on io error
   */
  String readStringFixedLength(int size) throws IOException {
    byte[] s = new byte[size];
    raf.read(s);
    return new String(s);
  }


  private long readLength() throws IOException {
    return isLengthLong ? raf.readLong() : (long) raf.readInt();
  }

  private long readOffset() throws IOException {
    return isOffsetLong ? raf.readLong() : (long) raf.readInt();
  }

  // size of data depends on "maximum possible number"
  private int getNumBytesFromMax(long maxNumber) {
    int size = 0;
    while (maxNumber != 0) {
      size++;
      maxNumber >>>= 8;  // right shift with zero extension
    }
    return size;
  }

  // size of data depends on "maximum possible number"
  private long readVariableSizeMax(int maxNumber) throws IOException {
    int size = getNumBytesFromMax(maxNumber);
    return readVariableSize(size);
  }

  private long readVariableSizeFactor(int sizeFactor) throws IOException {
    int size = (int) Math.pow(2, sizeFactor);
    return readVariableSize(size);
  }

  private long readVariableSize(int size) throws IOException {
    long vv;
    if (size == 1) {
      vv = DataType.unsignedByteToShort(raf.readByte());
    } else if (size == 2) {
      if (debugPos) debugOut.println("position=" + raf.getFilePointer());
      short s = raf.readShort();
      vv = DataType.unsignedShortToInt(s);
    } else if (size == 4) {
      vv = DataType.unsignedIntToLong(raf.readInt());
    } else if (size == 8) {
      vv = raf.readLong();
    } else {
      throw new IllegalStateException("bad sizeOfChunk=" + size);
    }
    return vv;
  }

  private long getFileOffset(long address) throws IOException {
    return baseAddress + address;
  }

  // find number of bytes needed to pad to multipleOf byte boundary
  static private int padding(int nbytes, int multipleOf) {
    int pad = nbytes % multipleOf;
    if (pad != 0) pad = multipleOf - pad;
    return pad;
  }

  void dump(String head, long filePos, int nbytes, boolean count) throws IOException {
    long savePos = raf.getFilePointer();
    if (filePos >= 0) raf.seek(filePos);
    byte[] mess = new byte[nbytes];
    raf.read(mess);
    printBytes(head, mess, nbytes, false);
    raf.seek(savePos);
  }

  static String showBytes(byte[] buff) {
    StringBuffer sbuff = new StringBuffer();
    for (int i = 0; i < buff.length; i++) {
      byte b = buff[i];
      int ub = (b < 0) ? b + 256 : b;
      if (i > 0) sbuff.append(" ");
      sbuff.append(ub);
    }
    return sbuff.toString();
  }


  static void printBytes(String head, byte[] buff, int n, boolean count) {
    debugOut.print(head + " == ");
    for (int i = 0; i < n; i++) {
      byte b = buff[i];
      int ub = (b < 0) ? b + 256 : b;
      if (count) debugOut.print(i + ":");
      debugOut.print(ub);
      if (!count) {
        debugOut.print("(");
        debugOut.print(b);
        debugOut.print(")");
      }
      debugOut.print(" ");
    }
    debugOut.println();
  }

  static void printBytes(String head, byte[] buff, int offset, int n) {
    debugOut.print(head + " == ");
    for (int i = 0; i < n; i++) {
      byte b = buff[offset + i];
      int ub = (b < 0) ? b + 256 : b;
      debugOut.print(ub);
      debugOut.print(" ");
    }
    debugOut.println();
  }

  public void close() {
    if (debugTracker) memTracker.report();
  }

  private class MemTracker {
    private List<Mem> memList = new ArrayList<Mem>();
    private StringBuffer sbuff = new StringBuffer();

    private long fileSize;

    MemTracker(long fileSize) {
      this.fileSize = fileSize;
    }

    void add(String name, long start, long end) {
      memList.add(new Mem(name, start, end));
    }

    void addByLen(String name, long start, long size) {
      memList.add(new Mem(name, start, start + size));
    }

    void report() {
      debugOut.println("Memory used file size= " + fileSize);
      debugOut.println("  start    end   size   name");
      Collections.sort(memList);
      Mem prev = null;
      for (Mem m : memList) {
        if ((prev != null) && (m.start > prev.end))
          doOne('+', prev.end, m.start, m.start - prev.end, "HOLE");
        char c = ((prev != null) && (prev.end != m.start)) ? '*' : ' ';
        doOne(c, m.start, m.end, m.end - m.start, m.name);
        prev = m;
      }
      debugOut.println();
    }

    private void doOne(char c, long start, long end, long size, String name) {
      sbuff.setLength(0);
      sbuff.append(c);
      sbuff.append(Format.l(start, 6));
      sbuff.append(" ");
      sbuff.append(Format.l(end, 6));
      sbuff.append(" ");
      sbuff.append(Format.l(size, 6));
      sbuff.append(" ");
      sbuff.append(name);
      debugOut.println(sbuff.toString());
    }

    class Mem implements Comparable {
      public String name;
      public long start, end;

      public Mem(String name, long start, long end) {
        this.name = name;
        this.start = start;
        this.end = end;
      }

      public int compareTo(Object o1) {
        Mem m = (Mem) o1;
        return (int) (start - m.start);
      }

    }
  }
}