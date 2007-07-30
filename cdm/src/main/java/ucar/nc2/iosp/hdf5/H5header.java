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

class H5header {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5header.class);

  // debugging
  static private boolean debugEnum = false;
  static private boolean debug1 = false, debugDetail = false, debugPos = false, debugHeap = false, debugV = false;
  static private boolean debugGroupBtree = false, debugDataBtree = false, debugDataChunk = false;
  static private boolean debugContinueMessage = false, debugTracker = false, debugSymbolTable = false;
  static private boolean warnings = true, debugReference = false;
  static java.io.PrintStream debugOut = System.out;

  static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debug1 = debugFlag.isSet("H5header/header");
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
    long pos = 0;
    long size = raf.length();
    byte[] b = new byte[8];

    // search forward for the header
    while ((pos < size) && (pos < maxHeaderPos)) {
      raf.seek(pos);
      raf.read(b);
      String magic = new String(b);
      if (magic.equals(shead))
        return true;
      pos = (pos == 0) ? 512 : 2 * pos;
    }

    return false;
  }

  ////////////////////////////////////////////////////////////////////////////////

  private RandomAccessFile raf;
  private ucar.nc2.NetcdfFile ncfile;
  private long actualSize, baseAddress;
  private byte sizeOffsets, sizeLengths;
  private boolean isOffsetLong, isLengthLong;
  private Map<String, DataObject> hashDataObjects = new HashMap<String, DataObject>(100);
  private Map<Long, Group> hashGroups = new HashMap<Long, Group>(100);

  /* netcdf 4 stuff
  private boolean isNetCDF4;
  private Map<Integer, Dimension> dimTable = new HashMap<Integer, Dimension>();
  private Map<String, Vatt> varTable = new HashMap<String, Vatt>(); */
  //private boolean v3mode = false;

  private MemTracker memTracker;

  void read(RandomAccessFile myRaf, ucar.nc2.NetcdfFile ncfile) throws IOException {
    this.ncfile = ncfile;
    this.raf = myRaf;
    actualSize = raf.length();
    memTracker = new MemTracker(actualSize);

    if (!isValidFile(myRaf))
      throw new IOException("Not a netCDF4/HDF5 file ");
    // now we are positioned right after the header

    memTracker.add("header", 0, raf.getFilePointer());

    // header information is in le byte order
    raf.order(RandomAccessFile.LITTLE_ENDIAN);

    if (debug1) debugOut.println("H5header 0pened file to read:'" + ncfile.getLocation() + "', size=" + actualSize);
    readSuperBlock();

    if (debugReference) {
      System.out.println("DataObjects");
      for (DataObject ob : obsList)
        System.out.println("  "+ob.name+" address= "+ob.address+" filePos= "+ getFileOffset(ob.address));
    }
    if (debugTracker) memTracker.report();
  }

  private DataObject findDataObject( long id) {
    for (DataObject dobj : obsList)
      if (dobj.address == id) return dobj;
    return null;
  }


  private void readSuperBlock() throws IOException {
    byte versionSB, versionFSS = -1, versionGroup = -1, versionSHMF = -1;
    short btreeLeafNodeSize, btreeInternalNodeSize, storageInternalNodeSize;
    int fileFlags;

    long heapAddress;
    long eofAddress;
    long driverBlockAddress;
    long superblockStart = raf.getFilePointer() - 8;

    // the "superblock"
    versionSB = raf.readByte();
    if (versionSB < 2) {
      versionFSS = raf.readByte();
      versionGroup = raf.readByte();
      raf.readByte(); // skip 1 byte
      versionSHMF = raf.readByte();
    }
    if (debugDetail)
      debugOut.println(" version SuperBlock= " + versionSB + "\n version FileFreeSpace= " + versionFSS + "\n version Root Group Symbol Table= " + versionGroup +
          "\n version Shared Header Message Format= " + versionSHMF);

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

    if (debugDetail) {
      debugOut.println(" baseAddress= 0x" + Long.toHexString(baseAddress));
      debugOut.println(" global free space heap Address= 0x" + Long.toHexString(heapAddress));
      debugOut.println(" eof Address=" + eofAddress);
      debugOut.println(" driver BlockAddress= 0x" + Long.toHexString(driverBlockAddress));
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

    // next comes the root objext's SymbolTableEntry
    SymbolTableEntry rootEntry = new SymbolTableEntry(raf.getFilePointer());

    // extract the root group object, recursively read all objects
    long rootObjectAddress = rootEntry.getObjectAddress();
    DataObject rootObject = new DataObject(null, "", rootObjectAddress);
    rootObject.read();
    if (rootObject.group == null) {
      // if the root object doesnt have a group message, check if the rootEntry is cache type 2
      // LOOK this is crappy - refactor
      if (rootEntry.btreeAddress != 0) {
        rootObject.group = new Group(null, "", rootEntry.btreeAddress, rootEntry.nameHeapAddress);
        rootObject.group.read();
      } else {
        throw new IllegalStateException("root object not a group");
      }
    }

    // now look for symbolic links
    findSymbolicLinks(rootObject.group);

    // recursively run through all the dataObjects and add them to the ncfile
    makeNetcdfGroup(ncfile.getRootGroup(), rootObject);
  }

  private void findSymbolicLinks(Group group) {
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

    //long address = -1;  // data object address, aka id
    long dataPos; // LOOK for regular variables, needs to be absolute, with baseAddress added if needed
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
      //this.address = ndo.address; 
      this.hdfType = ndo.mdt.type;
      this.byteSize = ndo.mdt.byteSize;
      this.dataPos = getFileOffset(ndo.msl.dataAddress);
      this.mfp = ndo.mfp;

      if (!ndo.mdt.isOK) {
        debugOut.println("WARNING HDF5 file " + ncfile.getLocation() + " not handling " + ndo.mdt);
        return; // not a supported datatype
      }

      this.isChunked = (ndo.msl.type == 2); // chunked vs. continuous storage
      this.storageSize = ndo.msl.storageSize;

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
    Vinfo(MessageDatatype mdt, long dataPos) throws IOException {
      this.byteSize = mdt.byteSize;
      this.dataPos = dataPos;

      if (!mdt.isOK) {
        debugOut.println("WARNING HDF5 file " + ncfile.getLocation() + " not handling " + mdt);
        return; // not a supported datatype
      }

      // figure out the data type
      hdfType = mdt.type;
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
        dataType = DataType.LONG;

      } else if (hdfType == 8) { // enums
        dataType = DataType.ENUM;

      } else if (hdfType == 9) { // variable length array
        int kind = (flags[0] & 0xf);
        vpad = ((flags[0] << 4) & 0xf);

        if (kind == 1)
          dataType = DataType.STRING;
        else
          dataType = getNCtype(mdt.getBaseType(), mdt.getBaseSize());

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

    private DataType getNCtype(int type, int size) {
      if ((type == 0) || (type == 4)) { // integer, bit field
        if (size == 1)
          return DataType.BYTE;
        else if (size == 2)
          return DataType.SHORT;
        else if (size == 4)
          return DataType.INT;
        else if (size == 8)
          return DataType.LONG;
        else {
          debugOut.println("WARNING HDF5 file " + ncfile.getLocation() + " not handling hdf integer type (" + type + ") with size= " + size);
          log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf integer type (" + type + ") with size= " + size);
          return null;
        }

      } else if (type == 1) {
        if (size == 4)
          return DataType.FLOAT;
        else if (size == 8)
          return DataType.DOUBLE;
        else {
          debugOut.println("WARNING HDF5 file " + ncfile.getLocation() + " not handling hdf float type with size= " + size);
          log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf float type with size= " + size);
          return null;
        }

      } else if (type == 3) {  // fixed length strings. String is used for Vlen type = 1
        return DataType.CHAR;

      } else {
        debugOut.println("WARNING not handling hdf type = " + type + " size= " + size);
        log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf type = " + type + " size= " + size);
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
     * Get the Fill Value, if there is one.
     * @return  wrapped primitive (Byte, Short, Integer, Double, Float, Long), or null if none
     */
    Object getFillValue() {

      if (fillValue == null) {
        if (dataType == DataType.BYTE) return N3iosp.NC_FILL_BYTE;
        if (dataType == DataType.CHAR) return (byte) 0;
        if (dataType == DataType.SHORT) return N3iosp.NC_FILL_SHORT;
        if (dataType == DataType.INT) return N3iosp.NC_FILL_INT;       
        if (dataType == DataType.LONG) return N3iosp.NC_FILL_LONG;
        if (dataType == DataType.FLOAT) return N3iosp.NC_FILL_FLOAT;
        if (dataType == DataType.DOUBLE) return N3iosp.NC_FILL_DOUBLE;
        return null;
      }

      if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR)) {
        return fillValue[0];
      }

      ByteBuffer bbuff = ByteBuffer.wrap( fillValue);
      if (byteOrder >= 0)
        bbuff.order( byteOrder == RandomAccessFile.LITTLE_ENDIAN? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

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

      throw new IllegalStateException();
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
        debugOut.println("   read HeapIdentifier address=" + address + " nelems=" + nelems + " heapAddress=" + heapAddress + " index=" + index);
      if (debugHeap) dump("heapIdentifier", getFileOffset(address), 16, true);
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

    RegionReference(long fileOffset) throws IOException {
      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(fileOffset);
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
          System.out.println(" found ho="+ho);
 /* - The offset of the object header of the object (ie. dataset) pointed to (yes, an object ID)
    - A serialized form of a dataspace _selection_ of elements (in the dataset pointed to).
    I don't have a formal description of this information now, but it's encoded in the H5S_<foo>_serialize() routines in
    src/H5S<foo>.c, where foo = {all, hyper, point, none}.
    There is _no_ datatype information stored for these sort of selections currently. */
          raf.seek(ho.dataPos);
          long objId = raf.readLong();
          DataObject ndo = findDataObject(objId);
          String what = (ndo == null) ? "none" : ndo.getName();
          System.out.println(" objId="+objId+" DataObject= "+what);

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

    StructureMember(int version) throws IOException {
      if (debugPos) debugOut.println("   *StructureMember now at position=" + raf.getFilePointer());

      name = readString(raf, -1);
      raf.skipBytes(padding(name.length() + 1, 8)); // barf
      offset = raf.readInt();
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

  private List<DataObject> obsList = new ArrayList<DataObject>();
  private class DataObject {
    Group parent;
    String name;
    long address; // aka object id

    String displayName;
    List<Message> messages = new ArrayList<Message>();

    byte version;
    short nmess;
    int referenceCount;
    long headerSize;

    // its a
    Group group;

    // or link
    String linkName = null;

    // or
    boolean isVariable;
    MessageDatatype mdt = null;
    MessageSimpleDataspace msd = null;
    MessageStorageLayout msl = null;
    MessageFilter mfp = null;

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

    private String getName() {
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
      raf.readByte(); // skip byte
      nmess = raf.readShort();
      if (debugDetail) debugOut.println(" version=" + version + " nmess=" + nmess);

      referenceCount = raf.readInt();
      headerSize = raf.readInt();
      if (debugDetail) debugOut.println(" referenceCount=" + referenceCount + " headerSize=" + headerSize);
      //if (referenceCount > 1)
      //  debugOut.println("WARNING referenceCount="+referenceCount);
      raf.skipBytes(4); // header messages multiples of 8

      // read all the messages first
      long posMess = raf.getFilePointer();
      int count = readMessages(posMess, nmess, Integer.MAX_VALUE);
      if (debugContinueMessage) debugOut.println(" nmessages read = " + count);
      if (debugPos) debugOut.println("<--done reading messages for <" + name + ">; position=" + raf.getFilePointer());

      // look for group or a datatype/dataspace/layout message
      MessageGroup groupMessage = null;

      for (Message mess : messages) {
        if (debugTracker) memTracker.addByLen("Message (" + displayName + ") " + mess.mtype, mess.start, mess.size + 8);

        if (mess.mtype == MessageType.SimpleDataspace)
          msd = (MessageSimpleDataspace) mess.messData;
        else if (mess.mtype == MessageType.Datatype)
          mdt = (MessageDatatype) mess.messData;
        else if (mess.mtype == MessageType.Layout)
          msl = (MessageStorageLayout) mess.messData;
        else if (mess.mtype == MessageType.Group)
          groupMessage = (MessageGroup) mess.messData;
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
        group = new Group(parent, name, groupMessage.btreeAddress, groupMessage.nameHeapAddress); // LOOK munge later
        group.read();
      }

      // if it has a Datatype and a StorageLayout, then its a Variable
      else if ((mdt != null) && (msl != null)) {
        isVariable = true;
      } else {
        debugOut.println("WARNING Unknown DataObject = " + displayName + " mdt = " + mdt + " msl  = " + msl);
        return;
      }

      if (debug1) debugOut.println("<-- end DataObject " + name);
      if (debugTracker) memTracker.addByLen("Object " + displayName, getFileOffset(address), headerSize + 16);
    }

    // read messages, starting at pos, until you hit maxMess read, or maxBytes read
    // if you hit a continuation message, call recursively
    // return number of messaages read
    private int readMessages(long pos, int maxMess, int maxBytes) throws IOException {
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
        int n = mess.read(pos);
        pos += n;
        bytesRead += n;
        count++;
        if (debugContinueMessage) debugOut.println("   count=" + count + " bytesRead=" + bytesRead);

        // if we hit a continuation, then we go into nested reading
        if (mess.mtype == MessageType.ObjectHeaderContinuation) {
          MessageContinue c = (MessageContinue) mess.messData;
          if (debugContinueMessage) debugOut.println(" ---ObjectHeaderContinuation--- ");
          count += readMessages(getFileOffset(c.offset), maxMess - count, (int) c.length);
          if (debugContinueMessage) debugOut.println(" ---ObjectHeaderContinuation return --- ");
        } else if (mess.mtype != MessageType.NIL) {
          messages.add(mess);
        }
      }
      return count;
    }

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
    private static int MAX_MESSAGE = 19;
    private static java.util.Map<String, MessageType> hash = new java.util.HashMap<String, MessageType>(10);
    private static MessageType[] mess = new MessageType[MAX_MESSAGE];

    public final static MessageType NIL = new MessageType("NIL", 0);
    public final static MessageType SimpleDataspace = new MessageType("SimpleDataspace", 1);
    public final static MessageType Datatype = new MessageType("Datatype", 3);
    public final static MessageType FillValueOld = new MessageType("FillValueOld", 4);
    public final static MessageType FillValue = new MessageType("FillValue", 5);
    public final static MessageType Compact = new MessageType("Compact", 6);
    public final static MessageType ExternalDataFiles = new MessageType("ExternalDataFiles", 7);
    public final static MessageType Layout = new MessageType("Layout", 8);
    public final static MessageType FilterPipeline = new MessageType("FilterPipeline", 11);
    public final static MessageType Attribute = new MessageType("Attribute", 12);
    public final static MessageType Comment = new MessageType("Comment", 13);
    public final static MessageType LastModifiedOld = new MessageType("LastModifiedOld", 14);
    public final static MessageType SharedObject = new MessageType("SharedObject", 15);
    public final static MessageType ObjectHeaderContinuation = new MessageType("ObjectHeaderContinuation", 16);
    public final static MessageType Group = new MessageType("Group", 17);
    public final static MessageType LastModified = new MessageType("LastModified", 18);

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

  // Header Message: Level 2A (p 22)
  private class Message implements Comparable {
    long start;
    byte flags;
    short type, size;
    Object messData;
    MessageType mtype;

    int read(long pos) throws IOException {
      this.start = pos;
      raf.seek(pos);
      if (debugPos) debugOut.println("  --> Message Header starts at =" + raf.getFilePointer());

      type = raf.readShort();
      size = raf.readShort();
      flags = raf.readByte();
      raf.skipBytes(3); // skip 3 bytes
      mtype = MessageType.getType(type);
      if (debug1) debugOut.println("  -->" + mtype + " messageSize=" + size + " flags = " + flags);
      if (debugPos) debugOut.println("  --> Message Data starts at=" + raf.getFilePointer());
      // data
      //mess == HDFdump( buffer, raf.getFilePointer(), size);

      if (mtype == MessageType.NIL) { // 0
        // dont do nuttin

      } else if (mtype == MessageType.SimpleDataspace) { // 1
        MessageSimpleDataspace data = new MessageSimpleDataspace();
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

      } else if (mtype == MessageType.Layout) { // 8
        MessageStorageLayout data = new MessageStorageLayout();
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

      } else {
        debugOut.println("****UNPROCESSED MESSAGE type = " + mtype + " raw = " + type);
      }

      return 8 + size;
    }

    public int compareTo(Object o) {
      return type - ((Message) o).type;
    }

    public String toString() {
      return "  type = " + mtype + "; " + messData;
    }
  }

  // Message Type 1 (p 23) : "Simple Dataspace" = dimension list / shape
  private class MessageSimpleDataspace {
    byte version, ndims, flags;
    int[] dim, maxLength, permute;
    boolean isPermuted;

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append(" length=(");
      for (int size : dim) sbuff.append(size).append(",");
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
      byte ndims = raf.readByte();
      byte flags = raf.readByte();
      raf.skipBytes(5); // skip 5 bytes

      if (debug1) debugOut.println("   SimpleDataspace version= " + version + " flags = " +
          flags + " ndims=" + ndims);

      dim = new int[ndims];
      for (int i = 0; i < ndims; i++)
        dim[i] = (int) readLength();

      boolean hasMax = (flags & 0x01) != 0;
      maxLength = new int[ndims];
      if (hasMax) {
        for (int i = 0; i < ndims; i++)
          maxLength[i] = (int) readLength();
      } else {
        System.arraycopy(dim, 0, maxLength, 0, ndims);
      }

      isPermuted = (flags & 0x02) != 0;
      permute = new int[ndims];
      if (isPermuted) {
        for (int i = 0; i < ndims; i++)
          permute[i] = (int) readLength();
      }

      if (debug1) {
        for (int i = 0; i < ndims; i++)
          debugOut.println("    dim length = " + dim[i] + " max = " + maxLength[i] + " permute = " + permute[i]);
      }
    }
  }

  // Message Type 3 (p 26) : "Datatype"
  private class MessageDatatype {
    int type, version;
    byte[] flags = new byte[3];
    int byteSize, byteOrder;

    // reference
    int referenceType;

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
        String desc = readString(raf, -1);
        if (debug1) debugOut.println("   type 5 (opaque): desc= " + desc);

      } else if (type == 6) { // compound
        int nmembers = flags[1] * 256 + flags[0];
        if (debug1) debugOut.println("   --type 6(compound): nmembers=" + nmembers);
        members = new ArrayList<StructureMember>();
        for (int i = 0; i < nmembers; i++) {
          members.add(new StructureMember(version));
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

        String[] enumName = new String[nmembers];
        int[] enumValue = new int[nmembers];
        for (int i = 0; i < nmembers; i++) {
          enumName[i] = readString8(raf);
        }
        raf.order(parent.byteOrder); // !! unbelievable
        for (int i = 0; i < nmembers; i++)
          enumValue[i] = raf.readInt();
        raf.order(RandomAccessFile.LITTLE_ENDIAN);
        if (debugEnum) {
          for (int i = 0; i < nmembers; i++)
            debugOut.println("   " + enumValue[i] + "=" + enumName[i]);
        }

      } else if (type == 9) { // variable-length
        isVString = (flags[0] & 0xf) == 1;
        if (debug1)
          debugOut.println("   type 9(variable length): type= " + (type == 0 ? "sequence of type:" : "string"));
        parent = new MessageDatatype(); // base type
        parent.read();

      } else if (type == 10) { // array
        if (debug1) debugOut.print("   type 10(array) lengths= ");
        int ndims = (int) raf.readByte();
        raf.skipBytes(3);
        dim = new int[ndims];
        for (int i = 0; i < ndims; i++) {
          dim[i] = raf.readInt();
          if (debug1) debugOut.print(" " + dim[i]);
        }
        int[] pdim = new int[ndims];
        for (int i = 0; i < ndims; i++)
          pdim[i] = raf.readInt();
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

  // Message Type 5 ( p 39) "Fill Value New" : fill value is stored in the message, with extra metadata
  private class MessageFillValue {
    byte version, spaceAllocateTime, fillWriteTime, fillDefined;
    int size;
    byte[] value;

    void read() throws IOException {
      version = raf.readByte();
      spaceAllocateTime = raf.readByte();
      fillWriteTime = raf.readByte();
      fillDefined = raf.readByte();

      if ((version <= 1) || (fillDefined != 0)) {
        size = raf.readInt();
        value = new byte[size];
        if (size > 0)
          raf.read(value);
      }

      if (debug1) debugOut.println(this);
    }

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("   FillValue version= ").append(version).append(" spaceAllocateTime = ").append(spaceAllocateTime).append(" fillWriteTime=").append(fillWriteTime).append(" fillDefined= ").append(fillDefined).append(" size = ").append(size).append(" value=");
      for (int i = 0; i < size; i++) sbuff.append(" ").append(value[i]);
      return sbuff.toString();
    }

  }

  // Message Type 8 ( p 44) "Data Storage Layout" : regular, chunked, or compact (stored with the message)
  private class MessageStorageLayout {
    byte version, ndims, type;
    long dataAddress = -1;
    int[] storageSize;

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
      sbuff.append(" storageSize = (");
      for (int i = 0; i < ndims; i++) {
        if (i > 0) sbuff.append(",");
        sbuff.append(storageSize[i]);
      }
      sbuff.append(") dataAddress=").append(dataAddress);
      return sbuff.toString();
    }

    void read() throws IOException {
      version = raf.readByte();
      ndims = raf.readByte();
      type = raf.readByte();
      raf.skipBytes(5); // skip 5 bytes

      boolean isCompact = (type == 0) || (type == 3);
      if (debug1) debugOut.print("   StorageLayout version= " + version + " type = " +
          type + (isCompact ? " (isCompact)" : "") + " ndims=" + ndims + ":");

      if (!isCompact) dataAddress = readOffset();

      storageSize = new int[ndims];
      for (int i = 0; i < ndims; i++) {
        storageSize[i] = raf.readInt();
        if (debug1) debugOut.print(" " + storageSize[i]);
      }

      if (isCompact) {
        int dataSize = raf.readInt();
        dataAddress = raf.getFilePointer();
      }

      if (debug1) debugOut.println(" dataAddress= " + dataAddress);
    }
  }

  // Message Type 11/0xB ( p 50) "Filter Pipeline" : apply a filter to the "data stream"
  class MessageFilter {
    byte version, nfilters;
    Filter[] filters;

    void read() throws IOException {
      version = raf.readByte();
      nfilters = raf.readByte();
      raf.skipBytes(6); // skip byte
      if (debug1) debugOut.println("   MessageFilter version=" + version + " nfilters=" + nfilters);

      filters = new Filter[nfilters];
      for (int i = 0; i < nfilters; i++) {
        filters[i] = new Filter();
      }
    }

    public Filter[] getFilters() { return filters; }

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("   MessageFilter version=").append(version).append(" nfilters=").append(nfilters).append("\n");
      for (Filter f : filters)
        sbuff.append(" ").append(f).append("\n");
      return sbuff.toString();
    }
  }

  class Filter {
    short id;
    short flags;
    String name;
    short nValues;
    int[] data;

    Filter() throws IOException {
      this.id = raf.readShort();
      short nameSize = raf.readShort();
      this.flags = raf.readShort();
      nValues = raf.readShort();

      long pos = raf.getFilePointer();
      this.name = readString(raf, -1); // read at current pos
      nameSize += padding(nameSize, 8);
      raf.seek(pos + nameSize); // make it more robust for errors

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
    MessageSimpleDataspace mds = new MessageSimpleDataspace();
    long dataPos; // pointer to the attribute data section, must be absolute file position

    public String toString() {
      return "name= " + name;
    }

    void read() throws IOException {
      if (debugPos) debugOut.println("   *MessageAttribute start pos= " + raf.getFilePointer());
      version = raf.readByte();
      raf.read(); // skip byte
      short nameSize = raf.readShort();
      short typeSize = raf.readShort();
      short spaceSize = raf.readShort();

      // read the name
      long pos = raf.getFilePointer();
      name = readString(raf, -1); // read at current pos
      nameSize += padding(nameSize, 8);
      raf.seek(pos + nameSize); // make it more robust for errors

      if (debug1) debugOut.println("   MessageAttribute version= " + version + " nameSize = " +
          nameSize + " typeSize=" + typeSize + " spaceSize= " + spaceSize + " name= " + name);

      // read the datatype
      pos = raf.getFilePointer();
      if (debugPos) debugOut.println("   *MessageAttribute before mdt pos= " + raf.getFilePointer());
      mdt.read();
      typeSize += padding(typeSize, 8);
      raf.seek(pos + typeSize); // make it more robust for errors

      // read the dataspace
      pos = raf.getFilePointer();
      if (debugPos) debugOut.println("   *MessageAttribute before mds = " + raf.getFilePointer());
      mds.read();
      spaceSize += padding(spaceSize, 8);
      raf.seek(pos + spaceSize); // make it more robust for errors

      // heres where the data starts
      dataPos = raf.getFilePointer();
      if (debugPos) debugOut.println("   *MessageAttribute dataPos= " + dataPos);

      // deal with reference type
      /* Dataset region references are stored as a heap-ID which points to the following information within the file-heap:
           an offset of the object pointed to,
           number-type information (same format as header message),
           dimensionality information (same format as header message),
           sub-set start and end information (i.e. a coordinate location for each),
           and field start and end names (i.e. a [pointer to the] string indicating the first field included and a [pointer to the] string name for the last field). */
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
        dataPos = referencedObject.msl.dataAddress;
      }
    }
  }

  // Message Type 13/0xD ( p 54) "Object Comment" : "short description of an Object"
  private class MessageComment {
    String name;

    void read() throws IOException {
      name = readString(raf, -1);
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

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  // Group structures: Group, Btree, LocalHeap, SymbolTableEntry
  // these are all read before any data

  private class Group {
    private Group parent;
    private String name, dname;
    private LocalHeap nameHeap;
    private GroupBTree btree;
    private long btreeAddress, nameHeapAddress;

    private List<DataObject> nestedObjects = new ArrayList<DataObject>(); // nested data objects

    public Group(Group parent, String name, long btreeAddress, long nameHeapAddress) {
      this.parent = parent;
      this.name = name;
      this.dname = (name.length() == 0) ? "root" : name;
      this.btreeAddress = btreeAddress;
      this.nameHeapAddress = nameHeapAddress;
      //isNetCDF4 = name.equals("_netCDF");

      // track by address for hard links
      hashGroups.put(btreeAddress, this);
      //debugOut.println("HEY group address added = "+btreeAddress);
    }

    void read() throws IOException {
      if (debug1) debugOut.println("\n--> Group read <" + dname + ">");
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

    private String getName() {
      return (parent == null) ? name : parent.getName() + "/" + name;
    }

    // is this a child of that ?
    private boolean isChildOf(Group that) {
      if (parent == that) return true;
      if (parent == null) return false;
      return parent.isChildOf(that);
    }

  } // Group

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

  //DataBTree getDataBTreeAt(String owner, long pos, int ndim) throws IOException {
  //  return new DataBTree(owner, pos, ndim);
  //}

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
        raf.seek(address);
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
            raf.seek(getFileOffset(ho.dataPos));
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

    SymbolTableEntry(long pos) throws IOException {
      raf.seek(pos);
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

      memTracker.add("SymbolTableEntry", pos, posData + 16);
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

  ///////////////////////////////////////////////////////////////
  // construct netcdf objects

  private void makeNetcdfGroup(ucar.nc2.Group ncGroup, DataObject dataObject) throws IOException {
    Group h5group = dataObject.group;

    // create group attributes
    List<Message> messages = dataObject.messages;
    for (Message mess : dataObject.messages) {
      if (mess.mtype == MessageType.Attribute) {
        MessageAttribute matt = (MessageAttribute) mess.messData;
        makeAttributes(h5group.name, matt, ncGroup.getAttributes());
      }
    }

    addSystemAttributes(messages, ncGroup.getAttributes());

    // nested objects
    List<DataObject> nestedObjects = h5group.nestedObjects;
    for (DataObject ndo : nestedObjects) {
      if (ndo.group != null) {
        ucar.nc2.Group nestedGroup = new ucar.nc2.Group(ncfile, ncGroup, NetcdfFile.createValidNetcdfObjectName(ndo.group.name));
        ncGroup.addGroup(nestedGroup);
        if (debug1) debugOut.println("--made Group " + nestedGroup.getName() + " add to " + ncGroup.getName());

        makeNetcdfGroup(nestedGroup, ndo);

      } else {

        if (ndo.isVariable) {

          if (debugReference && ndo.mdt.type == 7)
            debugOut.println(ndo);

          Variable v = makeVariable(ndo);

          if ((v != null) && (v.getDataType() != null)) {
            v.setParentGroup(ncGroup);
            ncGroup.addVariable(v);

            Vinfo vinfo = (Vinfo) v.getSPobject();

            if (debugV) debugOut.println("  made Variable " + v.getName() + "  vinfo= " + vinfo + "\n" + v);
          }
        } else if (warnings) {
          debugOut.println("WARN:  DataObject ndo " + ndo + " not a Group of variable");
        }

      }
    } // loop over nested objects

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

    /* if (isNetCDF4 && matt.name.startsWith("_ncdim_"))
      makeNC4Dimension(forWho, matt, mdt);

    else if (isNetCDF4 && matt.name.startsWith("_ncvar_"))
      makeNC4Variable(forWho, matt, mdt);

    else */

    if (mdt.type == 6) { // structure : make seperate attribute for each member
      for (StructureMember m : mdt.members) {
        //String attName = matt.name + "." + m.name;
        //if ((prefix != null) && (prefix.length() > 0)) attName = prefix + "." + attName;
        String attName = matt.name;
        if (mdt.type != 6) // LOOK nested compound attributes
          //makeAttributes( forWho, MessageAttribute matt, attList);
          //else
          attList.add(makeAttribute(forWho, attName, m.mdt, matt.mds, matt.dataPos + m.offset));
      }
    } else {
      // String attName = (prefix == null) || prefix.equals("") ? matt.name : prefix + "." + matt.name;
      String attName = matt.name;
      Attribute att = makeAttribute(forWho, attName, matt.mdt, matt.mds, matt.dataPos);
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

  private Attribute makeAttribute(String forWho, String attName, MessageDatatype mdt, MessageSimpleDataspace mds, long dataPos) throws IOException {
    attName = NetcdfFile.createValidNetcdfObjectName(attName); // look cannot search by name
    Variable v = new Variable(ncfile, null, null, attName); // LOOK null group
    Vinfo vinfo = new Vinfo(mdt, dataPos);
    if (!makeVariableShapeAndType(v, mdt, mds, vinfo)) {
      debugOut.println("SKIPPING attribute " + attName + " for " + forWho + " with dataType= " + vinfo.hdfType);
      return null;
    }

    v.setSPobject(vinfo);
    vinfo.setOwner(v);
    v.setCaching(false);
    if (debug1) debugOut.println("makeAttribute " + attName + " for " + forWho + "; vinfo= " + vinfo);

    //Object data = H5iosp.readData( Variable v2, Indexer index, DataType dataType, int[] shape);

    ucar.ma2.Array data = v.read();
    return new Attribute(attName, data);
  }

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
        if (fvm.size > 0)
          vinfo.fillValue = fvm.value;

      } else if (mess.mtype == MessageType.FillValueOld) {
        MessageFillValueOld fvm = (MessageFillValueOld) mess.messData;
        if (fvm.size > 0)
          vinfo.fillValue = fvm.value;
      }

      if (vinfo.fillValue != null) {
        Object fillValue = vinfo.getFillValue();
        if (fillValue instanceof Number)
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
      makeVariableShapeAndType(v, ndo.mdt, ndo.msd, vinfo);
      addMembersToStructure((Structure) v, ndo.mdt);
      v.setElementSize(ndo.mdt.byteSize);

    } else {
      String vname = NetcdfFile.createValidNetcdfObjectName(ndo.name); // look cannot search by name
      v = new Variable(ncfile, null, null, vname);  // LOOK null group
      makeVariableShapeAndType(v, ndo.mdt, ndo.msd, vinfo);
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
    addSystemAttributes(ndo.messages, v.getAttributes());
    if (fillAttribute != null)
      v.addAttribute(fillAttribute);

    if (!vinfo.signed)
      v.addAttribute(new Attribute("_unsigned", "true"));

    if (vinfo.isChunked) // make the data btree, but entries are not read in
      vinfo.btree = new DataBTree(dataPos, v.getShape(), vinfo.storageSize);

    if (transformReference && (ndo.mdt.type == 7) && (ndo.mdt.referenceType == 0)) { // object reference
      Array data = v.read();
      IndexIterator ii = data.getIndexIterator();

      Array newData = Array.factory(DataType.STRING,  v.getShape());
      IndexIterator ii2 = newData.getIndexIterator();
      while (ii.hasNext()) {
        long objId = ii.getLongNext();
        DataObject dobj = findDataObject(objId);
        if (dobj == null) System.out.println("Cant find dobj= "+dobj);
        else System.out.println(" Referenced object= "+dobj.getName());
        ii2.setObjectNext(dobj.getName());
      }
      v.setDataType(DataType.STRING);
      v.setCachedData(newData, true); // so H5iosp.read() is never called
      v.addAttribute( new Attribute("_HDF5ReferenceType", "values are names of referenced Variables"));
    }

    if (transformReference && (ndo.mdt.type == 7) && (ndo.mdt.referenceType == 1)) { // region reference

      int nelems = (int) v.getSize();
      int heapIdSize = 12;
      for (int i=0; i< nelems; i++) {
        H5header.RegionReference heapId = new RegionReference(vinfo.dataPos + heapIdSize*i);
      }

      v.addAttribute( new Attribute("_HDF5ReferenceType", "values are regions of referenced Variables"));
    }


    // debugging
    vinfo.setOwner(v);
    if (vinfo.hdfType == 7) debugOut.println("WARN:  Variable " + ndo.name + " is a Reference type");
    if (vinfo.mfp != null) debugOut.println("WARN:  Variable " + ndo.name + " has a Filter");

    return v;
  }

  /*
     Used for Structure Members
  */
  private Variable makeMemberVariable(String name, long dataPos, MessageDatatype mdt)
      throws IOException {

    Variable v;
    Vinfo vinfo = new Vinfo(mdt, dataPos);
    if (vinfo.getNCDataType() == null) {
      debugOut.println("SKIPPING DataType= " + vinfo.hdfType + " for variable " + name);
      return null;
    }

    if (mdt.type == 6) {
      String vname = NetcdfFile.createValidNetcdfObjectName(name); // look cannot search by name
      v = new Structure(ncfile, null, null, vname); // LOOK null group
      makeVariableShapeAndType(v, mdt, null, vinfo);
      addMembersToStructure((Structure) v, mdt);
      v.setElementSize(mdt.byteSize);

    } else {
      String vname = NetcdfFile.createValidNetcdfObjectName(name); // look cannot search by name
      v = new Variable(ncfile, null, null, vname);  // LOOK null group
      makeVariableShapeAndType(v, mdt, null, vinfo);
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

  private void addSystemAttributes(List<Message> messages, List<Attribute> attributes) {
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

  private boolean makeVariableShapeAndType(Variable v, MessageDatatype mdt, MessageSimpleDataspace msd, Vinfo vinfo) {

    int[] dim = (msd != null) ? msd.dim : new int[0];
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
      // LOOK only for attributes ??
      if (mdt.type == 3) { // fixed length string - DataType.CHAR, add string length

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
      log.error("Cant happen " + ee);
    }

    DataType dt = vinfo.getNCDataType();
    if (dt == null) return false;
    v.setDataType(dt);
    return true;
  }

  //////////////////////////////////////////////////////////////
  // utilities

  /**
   * Read a zero terminated String. Leave file positioned after zero terminator byte.
   *
   * @param raf from this file
   * @param pos starting here; if -1 then start at the current file position
   * @return String (dont include zero terminator)
   * @throws java.io.IOException on io error
   */
  static String readString(RandomAccessFile raf, long pos) throws IOException {
    if (pos >= 0)
      raf.seek(pos);
    else
      pos = raf.getFilePointer();

    int count = 0;
    while (raf.readByte() != 0) count++;

    raf.seek(pos);
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
    long pos = raf.getFilePointer();

    int count = 0;
    while (raf.readByte() != 0) count++;

    raf.seek(pos);
    byte[] s = new byte[count];
    raf.read(s);

    // skip to 8 byte boundary, note zero byte is skipped
    count++;
    count += padding(count, 8);
    raf.seek(pos + count);

    return new String(s);
  }

  private long readLength() throws IOException {
    return isLengthLong ? raf.readLong() : (long) raf.readInt();
  }

  private long readOffset() throws IOException {
    return isOffsetLong ? raf.readLong() : (long) raf.readInt();
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

  void dump(String head, long pos, int nbytes, boolean count) throws IOException {
    long savePos = raf.getFilePointer();
    if (pos >= 0) raf.seek(pos);
    byte[] mess = new byte[nbytes];
    raf.read(mess);
    printBytes(head, mess, nbytes, false);
    raf.seek(savePos);
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