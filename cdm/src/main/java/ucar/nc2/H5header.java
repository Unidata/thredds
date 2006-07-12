// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Format;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.ma2.DataType;

import java.util.*;
import java.text.*;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

class H5header {
  // debugging
  static private boolean debug1 = false , debugDetail = false, debugPos = false, debugHeap = false;
  static private boolean debugGroupBtree = false, debugDataBtree = false;
  static private boolean debugContinueMessage = false, debugTracker = false, debugSymbolTable = false;
  static java.io.PrintStream debugOut = System.out;

  static void setDebugFlags( ucar.nc2.util.DebugFlags debugFlag) {
    debug1 =  debugFlag.isSet("H5header/header");
    debugDetail =  debugFlag.isSet("H5header/headerDetails");
    debugGroupBtree =  debugFlag.isSet("H5header/groupBtree");
    debugDataBtree =  debugFlag.isSet("H5header/dataBtree");
    debugPos =  debugFlag.isSet("H5header/filePos");
    debugHeap =  debugFlag.isSet("H5header/Heap");
    debugContinueMessage =  debugFlag.isSet("H5header/continueMessage");
    debugSymbolTable =  debugFlag.isSet("H5header/symbolTable");
    debugTracker =  debugFlag.isSet("H5header/memTracker");
  }

  static public void setDebugOutputStream( java.io.PrintStream printStream) {
    debugOut = printStream;
  }

  static private java.text.SimpleDateFormat dateFormat;
  static {
    dateFormat = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT")); // same as UTC
  }

  static private final byte[] head = {(byte)0x89,'H','D','F','\r','\n', 0x1a,'\n'};
  static private final String shead = new String(head);
  static private final long maxHeaderPos = 50000; // header's gotta be within this

  static boolean isValidFile( ucar.unidata.io.RandomAccessFile raf) {
    try {
      long pos = 0;
      long size = raf.length();
      byte[] b = new byte[8];

      // search forward for the header
      while ((pos < size) && (pos < maxHeaderPos)) {
        raf.seek( pos);
        raf.read(b);
        String magic = new String( b);
        if (magic.equals(shead))
          return true;
        pos += 512;
      }
    } catch (IOException ioe) { } // fall through

    return false;
  }

  ////////////////////////////////////////////////////////////////////////////////

  private RandomAccessFile raf;
  private ucar.nc2.NetcdfFile ncfile;
  private long actualSize, baseAddress;
  private byte sizeOffsets, sizeLengths;
  private boolean isOffsetLong, isLengthLong;
  private boolean v3mode = true;
  private HashMap hashDataObjects = new HashMap( 100);
  private HashMap hashGroups = new HashMap( 100);

  // netcdf 4 stuff
  private boolean isNetCDF4;
  private HashMap dimTable = new HashMap();
  private HashMap varTable = new HashMap();

  private MemTracker memTracker;

  void read(RandomAccessFile myRaf, ucar.nc2.NetcdfFile ncfile) throws IOException {
    this.ncfile = ncfile;
    this.raf = myRaf;
    actualSize = raf.length();
    memTracker = new MemTracker( actualSize);

    if (!isValidFile( myRaf))
      throw new IOException("Not a netCDF/HDF5 file ");
    // now we are positioned right after the header

    memTracker.add("header", 0, raf.getFilePointer());

    // header information is in le byte order
    raf.order(RandomAccessFile.LITTLE_ENDIAN);

    if (debug1) debugOut.println ("H5header 0pened file to read:'" + ncfile.getLocation()+ "', size=" + actualSize);
    readSuperBlock();

    if (debugTracker) memTracker.report();
  }

  void readSuperBlock() throws IOException {
    byte versionSB, versionFSS,  versionGroup, versionSHMF;
    short btreeLeafNodeSize, btreeInternalNodeSize, storageInternalNodeSize;
    int fileFlags;

    long heapAddress;
    long eofAddress;
    long driverBlockAddress;
    long start = raf.getFilePointer();

    // the "superblock"
    versionSB = raf.readByte();
    versionFSS = raf.readByte();
    versionGroup = raf.readByte();
    raf.readByte(); // skip 1 byte
    versionSHMF = raf.readByte();
    if (debugDetail) debugOut.println(" versionSB= "+versionSB+" versionFSS= "+versionFSS+" versionGroup= "+versionGroup+
      " versionSHMF= "+versionSHMF);

    sizeOffsets = raf.readByte();
    isOffsetLong = (sizeOffsets == 8);

    sizeLengths = raf.readByte();
    isLengthLong = (sizeLengths == 8);
    if (debugDetail) debugOut.println(" sizeOffsets= "+sizeOffsets+" sizeLengths= "+sizeLengths);
    if (debugDetail) debugOut.println(" isLengthLong= "+isLengthLong+" isOffsetLong= "+isOffsetLong);

    raf.read(); // skip 1 byte
    //debugOut.println(" position="+mapBuffer.position());

    btreeLeafNodeSize = raf.readShort();
    btreeInternalNodeSize = raf.readShort();
    if (debugDetail) debugOut.println(" btreeLeafNodeSize= "+btreeLeafNodeSize+" btreeInternalNodeSize= "+btreeInternalNodeSize);;
    //debugOut.println(" position="+mapBuffer.position());

    fileFlags = raf.readInt();
    if (debugDetail) debugOut.println(" fileFlags= 0x"+Integer.toHexString(fileFlags));

    if (versionSB == 1) {
      storageInternalNodeSize = raf.readShort();
      raf.skipBytes(2);
    }

    baseAddress = readOffset();
    heapAddress = readOffset();
    eofAddress = readOffset();
    driverBlockAddress = readOffset();
    if (debugDetail)  {
      debugOut.println(" baseAddress= 0x"+Long.toHexString(baseAddress));
      debugOut.println(" global free space heap Address= 0x"+Long.toHexString(heapAddress));
      debugOut.println(" eof Address="+eofAddress);
      debugOut.println(" driver BlockAddress= 0x"+Long.toHexString(driverBlockAddress));
      debugOut.println();
    }
    memTracker.add("superblock", start, raf.getFilePointer());

    // look for file truncation
    long fileSize = raf.length();
    if (fileSize < eofAddress)
      throw new IOException("File is truncated should be= "+eofAddress+" actual = "+fileSize);

    // next comes the root objext's SymbolTableEntry
    SymbolTableEntry rootEntry = new SymbolTableEntry( raf.getFilePointer());

    // extract the root group object, recursively read all objects
    long rootObjectAddress = rootEntry.getObjectAddress();
    DataObject rootObject = new DataObject( null, "", rootObjectAddress);
    rootObject.read();
    if (rootObject.group == null)
      throw new IllegalStateException("root object not a group");

    // now look for symbolic links
    findSymbolicLinks( rootObject.group);

    // recursively run through all the dataObjects and add them to the ncfile
    makeNetcdfGroup( ncfile.getRootGroup(), rootObject);
  }

  private void findSymbolicLinks( Group group) {
    List nolist = group.nestedObjects;
    for (int i=0; i<nolist.size(); i++) {
      DataObject ndo = (DataObject) nolist.get(i);

      if (ndo.group != null) {
        findSymbolicLinks( ndo.group);
      } else {

        if (ndo.linkName != null) { // its a symbolic link
          DataObject link = (DataObject) hashDataObjects.get(ndo.linkName);
          if (link == null) {
            debugOut.println(" WARNING Didnt find symbolic link=" + ndo.linkName+" from "+ndo.name);
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
          if (debugSymbolTable) debugOut.println("  Found symbolic link=" + ndo.linkName+" from "+ndo.name);
        }

      }
    }
  }


  /////////////////////////////////////////////////////////////////////

  /**
   * Holder of all H5 specific information for a Variable, needed to do IO.
   */
  class Vinfo {
    long pos = -1;  // data object position, aka id
    long dataPos; // LOOK for  regular variables, needs to be absolute, with baseAddress added if needed
                  // for member variables, is the offset from start of structure
    int hdfType;
    DataType dataType = null;
    int byteOrder = -1;
    boolean signed = true;
    int byteSize, vpad;
    int[] storageSize; // for type 1 (continuous) : (dims, elemSize)
                       // for type 2 (chunked)    : (chunkDim, elemSize)

    // chunked stuff
    boolean isChunked = false;
    DataBTree btree = null; // will get cached here if used

    boolean hasFilter = false;
    boolean useFillValue = false;
    byte[] fillValue;

    /**
     *  Constructor
     * @param mdt datatype
     * @param msl storage layout
     * @param dataPos start of data in file
     */
    Vinfo(MessageDatatype mdt, MessageStorageLayout msl, long dataPos) {
      this.byteSize = mdt.byteSize;
      this.dataPos = dataPos;

      if (!mdt.isOK) return; // not a supported datatype

      this.isChunked = (msl != null) && (msl.type == 2); // chunked vs. continuous storage
      if (msl != null)
        this.storageSize = msl.storageSize;

      hdfType = mdt.type;
      byte[] flags = mdt.flags;

      if (hdfType == 0) { // int, long, short, byte
        dataType = getNCtype(hdfType, byteSize);
        byteOrder =  ((flags[0] & 1) == 0) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;
        signed =  ((flags[0] & 8) != 0);
      }

      else if (hdfType == 1) { // floats, doubles
        dataType = getNCtype(hdfType, byteSize);
        byteOrder =  ((flags[0] & 1) == 0) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;
      }

      else if (hdfType == 3) { // string
        dataType = v3mode ? DataType.CHAR : DataType.STRING;
      }

      /* else if (hdfType == 4) { // bit field
        dataType = getNCtype(hdfType, byteSize);
      }

      else if (hdfType == 5) { // opaque
        dataType = DataType.BYTE;
      } */

      else if (hdfType == 6) { // structure
        dataType = DataType.STRUCTURE;
      }

      else if (hdfType == 9) { // variable length array
        int kind = (flags[0] & 0xf);
        vpad = ((flags[0] << 4) & 0xf);

        if (kind == 1)
          dataType = DataType.STRING;
        else
          dataType = getNCtype(mdt.getBaseType(), mdt.getBaseSize());
     }

      else if (hdfType == 10) { // array
        byteOrder =  (mdt.getFlags()[0] & 1) == 0 ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;

        if ((mdt.parent.type == 9) && mdt.parent.isVString) {
          dataType = DataType.STRING;
        } else
          dataType = getNCtype(mdt.getBaseType(), mdt.getBaseSize());

      }  else {

        debugOut.println("WARNING not handling hdf dataType = "+hdfType+" size= "+byteSize);
      }
    }

    private DataType getNCtype(int type, int size) {
      if ((type == 0) || (type == 4)) {
        if (size == 1)
          return DataType.BYTE;
        else if (size == 2)
          return DataType.SHORT;
        else if (size == 4)
          return DataType.INT;
        else if (size == 8)
          return DataType.LONG;
        else return null;
      }

      else if (type == 1) {
        if (size == 4)
          return DataType.FLOAT;
        else if (size == 8)
          return DataType.DOUBLE;
        else return null;
      }

      else if (type == 3) {
        return v3mode ? DataType.CHAR : DataType.STRING;
      }

      else {
        debugOut.println("WARNING not handling hdf type = "+type+" size= "+size);
        return null;
      }
    }

    public String toString() {
      StringBuffer buff = new StringBuffer();
      buff.append(
          "dataPos="+dataPos+
          " byteSize="+byteSize+
          " datatype="+dataType+" ("+hdfType+")");
      if (isChunked) {
        buff.append(" isChunked (");
        for (int j=0;j<storageSize.length;j++)
          buff.append(storageSize[j]+" ");
        buff.append(")");
      }
      if (hasFilter) buff.append( " hasFilter");
      buff.append("; // "+extraInfo());
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

    DataType getNCDataType() { return dataType; }

    public String toStringDebug(String name) {
      if (pos < 0) return null;

      java.io.PrintStream save = debugOut;

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      debugOut = new java.io.PrintStream( bos);
      DataObject o = new DataObject( null, name, pos);
      try {
        o.read();
      }
      catch (IOException ex) {
        ex.printStackTrace( debugOut);
      }

      debugOut.println("vinfo="+this);
      debugOut = save;
      return bos.toString();
    }

  }

  private class Vatt {
    String name;
    ArrayList dimList;
    Vatt( String name, ArrayList dimList) {
        this.name = name;
        this.dimList = dimList;
    }
  }

  HeapIdentifier getHeapIdentifier(long pos) throws IOException { return new HeapIdentifier( pos); }
  private HashMap heapMap = new HashMap();
  class HeapIdentifier {
    int nelems; // "number of 'base type' elements in the sequence in the heap"
    int index;
    long heapAddress;
    Object id;

    HeapIdentifier(long pos) throws IOException {
      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(pos);

      nelems = raf.readInt();
      heapAddress = readOffset();
      index = raf.readInt();
      if (debugDetail) debugOut.println("   read HeapIdentifier position="+pos+" nelems="+nelems+" heapAddress="+heapAddress+" index="+index);
      id = new Long(heapAddress);
      if (debugHeap) dump("heapId", pos, 16, true);
    }

    GlobalHeap.HeapObject getHeapObject() throws IOException {
      GlobalHeap gheap = null;
      if (null == (gheap = (GlobalHeap) heapMap.get(id))) {
        gheap = new GlobalHeap(heapAddress);
        heapMap.put( id, gheap);
      }

      List list = gheap.hos;
      for (int i=0; i<list.size(); i++) {
        GlobalHeap.HeapObject ho = (GlobalHeap.HeapObject) list.get(i);
        if (ho.id == index)
          return ho;
      }
      throw new IllegalStateException("cant find HeapObject");
    }

  } // HeapIdentifier

  private class StructureMember {
    String name;
    int offset;
    byte dims;
    MessageDatatype mdt;

    StructureMember(int version) throws IOException {
      if (debugPos) debugOut.println("   *StructureMember now at position="+raf.getFilePointer());

      name = readString( raf, -1);
      raf.skipBytes( padding(name.length()+1, 8)); // barf
      offset = raf.readInt();
      if (debug1)  debugOut.println("   Member name="+name+" offset= "+offset);
      if (version == 1) {
        dims = raf.readByte();
        raf.skipBytes( 3);
        raf.skipBytes( 24); // ignore dimension info for now
      }

      //HDFdumpWithCount(buffer, raf.getFilePointer(), 16);
      mdt = new MessageDatatype();
      mdt.read();
      if (debugDetail)  debugOut.println("   ***End Member name="+name);

      // ??
      //HDFdump(ncfile.out, "Member end", buffer, 16);
      //if (HDFdebug)  ncfile.debugOut.println("   Member pos="+raf.getFilePointer());
      //HDFpadToMultiple( buffer, 8);
      //if (HDFdebug)  ncfile.debugOut.println("   Member padToMultiple="+raf.getFilePointer());
      //raf.skipBytes( 4); // huh ??
    }
  }

  //////////////////////////////////////////////////////////////
  // 2A "data object header"

  private class DataObject {
    Group parent;
    String name;
    long pos; // aka object id

    String displayName;
    ArrayList messages = new ArrayList();

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
    MessageSimpleDataspace mds = null;
    MessageStorageLayout msl = null;
    MessageFilter mfp = null;

    DataObject( Group parent, String name, long pos) {
      this.parent = parent;
      this.name = name;
      this.pos = pos;

      displayName = (name.length() == 0) ? "root" : name;
    }

    DataObject( Group parent, String name, String linkName) {
      this.parent = parent;
      this.name = name;
      this.linkName = linkName;
      displayName = name;
    }

    private String getName() {
      return parent.getName() + "/" + name;
    }

    // is this a child of that ?
    private boolean isChildOf( Group that) {
      if (parent == that) return true;
      if (parent == null) return false;
      return parent.isChildOf( that);
    }

    // "Data Object Header" Level 2A
    // read a Data Object Header
    // no side effects, can be called multiple time for debugging
    private void read() throws IOException {

      if (debug1) debugOut.println("\n--> DataObject.read parsing <"+displayName+"> oid/pos="+pos);
      if (debugPos) debugOut.println ("      DataObject.read now at position="+raf.getFilePointer()+" for <"+displayName+"> reposition to "+pos);

      //if (offset < 0) return null;
      setOffset(pos);

      version = raf.readByte();
      raf.readByte(); // skip byte
      nmess = raf.readShort();
      if (debugDetail)  debugOut.println(" version="+version+" nmess="+nmess);

      referenceCount = raf.readInt();
      headerSize = raf.readInt();
      if (debugDetail)  debugOut.println(" referenceCount="+referenceCount+" headerSize="+headerSize);
      //if (referenceCount > 1)
      //  debugOut.println("WARNING referenceCount="+referenceCount);
      raf.skipBytes( 4); // header messages multiples of 8

      // read all the messages first
      long posMess = raf.getFilePointer();
      int count = readMessages(posMess, nmess, Integer.MAX_VALUE);
      if (debugContinueMessage) debugOut.println(" nmessages read = "+count);
      if (debugPos)  debugOut.println("<--done reading messages for <"+name+">; position="+raf.getFilePointer());

      // sort messages for deterministic processing
      // Collections.sort( messages);

      // look for group or a datatype/dataspace/layout message
      MessageGroup groupMessage = null;

      for (int i=0; i<messages.size(); i++) {
        Message mess = (Message) messages.get(i);
        if (debugTracker) memTracker.addByLen( "Message ("+displayName+") "+mess.mtype, mess.start, mess.size+8);

        if (mess.mtype == MessageType.SimpleDataspace)
          mds = (MessageSimpleDataspace) mess.messData;
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
        if ( null != (group = (Group) hashGroups.get( new Long(groupMessage.btreeAddress)))) {
          debugOut.println("WARNING hard link to group = "+group.getName());
          if (parent.isChildOf( group)) {
            debugOut.println("ERROR hard link to group create a loop = "+group.getName());
            group = null;
            return;
          }
        }

        // read the group, and its contained data objects.
        group = new Group( parent, name, groupMessage.btreeAddress, groupMessage.nameHeapAddress); // LOOK munge later
        group.read();
     }

      // if it has a Datatype and a StorageLayout, then its a Variable
      else if ((mdt != null) && (msl != null)) {
        isVariable = true;
      }

      else {
        debugOut.println("WARNING Unknown DataObject = "+displayName+" mdt = "+mdt+" msl  = "+msl);
        return;
      }

      if (debug1)  debugOut.println("<-- end DataObject "+name);
      if (debugTracker) memTracker.addByLen("Object "+displayName, pos, headerSize+16);
    }

    // read messages, starting at pos, until you hit maxMess read, or maxBytes read
    // if you hit a continuation message, call recursively
    // rturn number of messaages rad
    private int readMessages(long pos, int maxMess, int maxBytes) throws IOException {
      if (debugContinueMessage)
        debugOut.println(" readMessages start at ="+pos+" maxMess= "+maxMess+" maxBytes= "+maxBytes);

      int count = 0;
      int bytesRead = 0;
      while ((count < maxMess) && (bytesRead < maxBytes)) {
        /* LOOK: MessageContinue not correct ??
        if (posMess >= actualSize)
          break; */

        Message mess = new Message();
        messages.add( mess);
        int n = mess.read( pos);
        pos += n;
        bytesRead += n;
        count++;
        if (debugContinueMessage) debugOut.println("   count="+count+" bytesRead="+bytesRead);

          // if we hit a continuation, then we go into nested reading
        if (mess.mtype == MessageType.ObjectHeaderContinuation) {
          MessageContinue c = (MessageContinue) mess.messData;
          if (debugContinueMessage)  debugOut.println(" ---ObjectHeaderContinuation--- ");
          count += readMessages(c.offset, maxMess - count, (int) c.length);
          if (debugContinueMessage)  debugOut.println(" ---ObjectHeaderContinuation return --- ");
        }
      }
      return count;
    }

  }

  // type safe enum
  static private class MessageType {
    private static int MAX_MESSAGE = 19;
    private static java.util.HashMap hash = new java.util.HashMap(10);
    private static MessageType[] mess = new MessageType[ MAX_MESSAGE];

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
      hash.put( name, this);
      mess[num] = this;
    }

    /**
     * Find the MessageType that matches this name.
     * @param name find DataTYpe with this name.
     * @return DataType or null if no match.
     */
    public static MessageType getType(String name) {
      if (name == null) return null;
      return (MessageType) hash.get( name);
    }

    /**
     * Get the MessageType by number.
     * @param num message number.
     * @return the MessageType
     */
    public static MessageType getType(int num) {
      if ((num < 0) || (num >= MAX_MESSAGE)) return null;
      return mess[num];
    }

    /** Message name. */
     public String toString() { return name+"("+num+")"; }
    /** Message number. */
     public int getNum() { return num; }

  }

  // Header Message: Level 2A
  private class Message implements Comparable {
    long start;
    byte flags;
    short type, size;
    Object messData;
    MessageType mtype;

    int read(long pos) throws IOException {
      this.start = pos;
      raf.seek(pos);
      if (debugPos) debugOut.println("  --> Message Header starts at ="+raf.getFilePointer());

      type = raf.readShort();
      size = raf.readShort();
      flags = raf.readByte();
      raf.skipBytes(3); // skip 3 bytes
      mtype = MessageType.getType(type);
      if (debug1) debugOut.println("  -->"+mtype+" messageSize="+size+" flags = "+flags);
      if (debugPos) debugOut.println("  --> Message Data starts at="+raf.getFilePointer());
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
        debugOut.println("****UNPROCESSED MESSAGE type = "+mtype+" raw = "+type);
      }

      return 8 + size;
    }

    public int compareTo( Object o) {
      return type - ((Message)o).type;
    }
  }

  // 1
  private class MessageSimpleDataspace {
    byte version, ndims, flags;
    int[] dim, maxLength, permute;
    boolean isPermuted;

    void read() throws IOException {
     if (debugPos) debugOut.println("   *MessageSimpleDataspace start pos= "+raf.getFilePointer());
      byte version = raf.readByte();
      byte ndims = raf.readByte();
      byte flags = raf.readByte();
      raf.skipBytes(5); // skip 5 bytes

      if (debug1) debugOut.println("   SimpleDataspace version= "+version+" flags = "+
        flags+" ndims="+ndims);

      dim = new int[ ndims];
      for (int i=0; i<ndims; i++)
        dim[i] = (int) readLength();

      boolean hasMax = (flags & 0x01) != 0;
      maxLength = new int[ ndims];
      if (hasMax) {
        for (int i=0; i<ndims; i++)
          maxLength[i] = (int) readLength();
      } else {
         for (int i=0; i<ndims; i++)
           maxLength[i] = dim[i];
      }

      isPermuted = (flags & 0x02) != 0;
      permute = new int[ ndims];
      if (isPermuted) {
        for (int i=0; i<ndims; i++)
          permute[i] = (int) readLength();
      }

      if (debug1) {
        for (int i=0; i<ndims; i++)
          debugOut.println("    dim length = "+ dim[i]+" max = "+ maxLength[i]+" permute = "+permute[i]);
      }
    }
  }

  // 3
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
    ArrayList members;

    // variable-length, array types have "parent" DataType
    MessageDatatype parent;
    boolean isVString;

    boolean isOK = true;

    void read() throws IOException {
      if (debugPos) debugOut.println("   *MessageDatatype start pos= "+raf.getFilePointer());

      byte tandv = raf.readByte();
      type = (int)(tandv & 0xf);
      version = (int)((tandv & 0xf0) >> 4);
      raf.read( flags);
      byteSize = raf.readInt();
      byteOrder = ((flags[0] & 1) == 0) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;

      if (debugDetail)  debugOut.println("   Datatype type="+type+" version= "+version+" flags = "+
        flags[0]+" "+flags[1]+" "+flags[2]+" byteSize="+byteSize
        +" byteOrder="+(byteOrder==0?"BIG":"LITTLE"));

      if (type == 0){  // fixed point
        short bitOffset = raf.readShort();
        short bitPrecision = raf.readShort();
        if (debug1)  debugOut.println("   type 0 (fixed point): bitOffset= "+bitOffset+" bitPrecision= "+bitPrecision);
        isOK = (bitOffset == 0) && (bitPrecision % 8 == 0);
     }

      else if (type == 1){  // floating point
        short bitOffset = raf.readShort();
        short bitPrecision = raf.readShort();
        byte expLocation = raf.readByte();
        byte expSize = raf.readByte();
        byte manLocation = raf.readByte();
        byte manSize = raf.readByte();
        int expBias = raf.readInt();
        if (debug1)  debugOut.println("   type 1 (floating point): bitOffset= "+bitOffset+" bitPrecision= "+bitPrecision+
          " expLocation= "+expLocation+" expSize= "+expSize+" manLocation= "+manLocation+
          " manSize= "+manSize+" expBias= "+expBias);
      }

      else if (type == 4){ // bit field
        short bitOffset = raf.readShort();
        short bitPrecision = raf.readShort();
        if (debug1)  debugOut.println("   type 4 (bit field): bitOffset= "+bitOffset+" bitPrecision= "+bitPrecision);
        isOK = (bitOffset == 0) && (bitPrecision % 8 == 0);
      }

      else if (type == 5){ // opaque
        String desc = readString(raf, -1);
        if (debug1) debugOut.println("   type 5 (opaque): desc= "+desc);
      }

      else if (type == 6){ // compound
        int nmembers = flags[1]*256+ flags[0];
        if (debug1)  debugOut.println("   --type 6(compound): nmembers="+nmembers);
        members = new ArrayList();
        for (int i=0; i<nmembers; i++) {
          members.add( new StructureMember(version));
        }
        if (debugDetail)  debugOut.println("   --done with compound type");
     }

      else if (type == 7){ // reference
        referenceType = flags[0] & 0xf;
        if (debug1)  debugOut.println("   --type 7(reference): ="+referenceType);
     }

      else if (type == 8){ // enums
        int nmembers = flags[1]*256+ flags[0];
        if (debug1)  debugOut.println("   --type 8(enums): nmembers="+nmembers);
        parent = new MessageDatatype(); // base type
        parent.read();

        String[] enumName = new String[nmembers];
        int[] enumValue = new int[nmembers];
        for (int i=0; i<nmembers; i++) {
          enumName[i] = readString8(raf);
        }
        raf.order(parent.byteOrder); // !! unbelievable
        for (int i=0; i<nmembers; i++)
          enumValue[i] = raf.readInt();
        raf.order(RandomAccessFile.LITTLE_ENDIAN);
        if (debug1)  {
          for (int i = 0; i < nmembers; i++)
            debugOut.println("   " + enumValue[i] +"=" + enumName[i]);
        }
     }

      else if (type == 9){ // variable-length
        isVString = (flags[0] & 0xf) == 1;
        if (debug1)  debugOut.println("   type 9(variable length): type= "+(type==0?"sequence of type:":"string"));
        parent = new MessageDatatype(); // base type
        parent.read();
      }

      else if (type == 10){ // array
        if (debug1) debugOut.print("   type 10(array) lengths= ");
        int ndims = (int) raf.readByte();
        raf.skipBytes(3);
        dim = new int[ndims];
        for (int i=0; i<ndims; i++) {
          dim[i] = raf.readInt();
          if (debug1) debugOut.print(" "+dim[i]);
        }
        int[] pdim = new int[ndims];
        for (int i=0; i<ndims; i++)
          pdim[i] = raf.readInt();
        if (debug1) debugOut.println();

        parent = new MessageDatatype(); // base type
        parent.read();
      }
    }

    int getBaseType() { return (parent != null) ? parent.getBaseType() : type; }
    int getBaseSize() { return (parent != null) ? parent.getBaseSize() : byteSize; }
    byte[] getFlags() { return (parent != null) ? parent.getFlags() : flags; }
  }

  // 4
  private class MessageFillValueOld {
    byte[] value;
    int size;
    void read() throws IOException {
      size = raf.readInt();
      value = new byte[size];
      raf.read(value);

      if (debug1)  {
        debugOut.print("   FillValueOld size= "+size+" value=");
        for (int i=0;i<size;i++) debugOut.print(" "+value[i]);
        debugOut.println();
      }
    }
  }

  // 5
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
        raf.read(value);
      }

      if (debug1) {
        debugOut.print("   FillValue version= "+version+" spaceAllocateTime = "+
          spaceAllocateTime+" fillWriteTime="+fillWriteTime+" fillDefined= "+fillDefined+
          " size = "+size+" value=");
        for (int i=0;i<size;i++) debugOut.print(" "+value[i]);
        debugOut.println();
      }
    }
  }

  // 8
  private class MessageStorageLayout {
    byte version, ndims, type;
    long dataAddress = -1;
    int[] storageSize;

    void read() throws IOException {
      version = raf.readByte();
      ndims = raf.readByte();
      type = raf.readByte();
      raf.skipBytes(5); // skip 5 bytes

      boolean isCompact = (type == 0) || (type == 3);
      if (debug1) debugOut.print("   StorageLayout version= "+version+" type = "+
        type+ (isCompact? " (isCompact)" : "") + " ndims="+ndims+":");

      if (!isCompact) dataAddress = readOffset();

      storageSize = new int[ ndims];
      for (int i=0; i<ndims; i++) {
        storageSize[i] = raf.readInt();
        if (debug1)  debugOut.print(" "+ storageSize[i]);
      }

      if (isCompact) {
        int dataSize = raf.readInt();
        dataAddress = raf.getFilePointer();
      }

      if (debug1) debugOut.println(" dataAddress= "+dataAddress);
    }
  }

  // 11
  private class MessageFilter {
    byte version, nfilters;
    long btreeAddress, nameHeapAddress;
    Filter[] filters;

    void read() throws IOException {
      version = raf.readByte();
      nfilters = raf.readByte();
      raf.skipBytes(6); // skip byte
      if (debug1)  debugOut.println("   MessageFilter version="+version+" nfilters="+nfilters);

      filters = new Filter[ nfilters];
      for (int i=0; i<nfilters; i++) {
        filters[i] = new Filter();
      }
    }

    class Filter {
      short id, flags;
      String name;

      Filter() throws IOException {
        this.id = raf.readShort();
        short nameSize = raf.readShort();
        this.flags = raf.readShort();
        short nValues = raf.readShort();

        long pos = raf.getFilePointer();
        this.name = readString(raf, -1); // read at current pos
        nameSize += padding(nameSize, 8);
        raf.seek(pos + nameSize); // make it more robust for errors

       if (debug1)  debugOut.println("   Filter id= "+id+" flags = "+ flags +
          " nValues="+nValues+" name= "+name);
      }
    }
  }

  // 12
  private class MessageAttribute {
    byte version;
    short nameSize, typeSize, spaceSize;
    String name;
    MessageDatatype mdt = new MessageDatatype();
    MessageSimpleDataspace mds = new MessageSimpleDataspace();
    long dataPos; // pointer to the attribute data section

    void read() throws IOException {
      if (debugPos) debugOut.println("   *MessageAttribute start pos= "+raf.getFilePointer());
      version = raf.readByte();
      raf.read(); // skip byte
      nameSize = raf.readShort();
      typeSize = raf.readShort();
      spaceSize = raf.readShort();

      // read the name
      long pos = raf.getFilePointer();
      name = readString(raf, -1); // read at current pos
      nameSize += padding( nameSize, 8);
      raf.seek( pos+nameSize); // make it more robust for errors

      if (debug1) debugOut.println("   MessageAttribute version= "+version+" nameSize = "+
        nameSize+" typeSize="+typeSize+" spaceSize= "+spaceSize+" name= "+name);

      // read the datatype
      pos = raf.getFilePointer();
      if (debugPos) debugOut.println("   *MessageAttribute before mdt pos= "+raf.getFilePointer());
      mdt.read();
      typeSize += padding( typeSize, 8);
      raf.seek( pos+typeSize); // make it more robust for errors

      // read the dataspace
      pos = raf.getFilePointer();
      if (debugPos) debugOut.println("   *MessageAttribute before mds = "+raf.getFilePointer());
      mds.read();
      spaceSize += padding( spaceSize, 8);
      raf.seek( pos+spaceSize); // make it more robust for errors

      // heres where the data starts
      dataPos = raf.getFilePointer();
      if (debugPos) debugOut.println("   *MessageAttribute dataPos= "+dataPos);

      // deal with reference type
      if (mdt.type == 7) { // reference
        // datapos points to a position of the refrenced object, i think
        raf.seek( dataPos);
        long referencedObjectPos = readOffset();
        //debugOut.println("WARNING   Reference at "+dataPos+" referencedObjectPos = "+referencedObjectPos);

        // LOOK, should only read this once
        DataObject referencedObject = new DataObject(null, "att", referencedObjectPos);
        referencedObject.read();
        mdt = referencedObject.mdt;
        mds = referencedObject.mds;
        dataPos = referencedObject.msl.dataAddress;
      }
    }
  }

  // 13
  private class MessageComment {
    String name;
    void read() throws IOException {
      name = readString(raf, -1);
    }
  }

  // 14
  private class MessageLastModifiedOld {
    String datemod;
    void read() throws IOException {
      byte[] s = new byte[14];
      raf.read(s);
      datemod = new String(s);
      if (debug1)  debugOut.println("   MessageLastModifiedOld="+datemod);
    }
  }

  // 16
  private class MessageContinue {
    long offset, length;

    void read() throws IOException {
      offset = readOffset();
      length = readLength();
      if (debug1)  debugOut.println("   Continue offset="+offset+" length="+length);
    }
  }

  // 17
  private class MessageGroup {
    long btreeAddress, nameHeapAddress;

    void read() throws IOException {
      btreeAddress = readOffset();
      nameHeapAddress = readOffset();
      if (debug1)  debugOut.println("   Group btreeAddress="+btreeAddress+" nameHeapAddress="+nameHeapAddress);
    }
  }

  // 14
  private class MessageLastModified {
    byte version;
    int secs;
    void read() throws IOException {
      version = raf.readByte();
      raf.skipBytes(3); // skip byte
      secs = raf.readInt();
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

    private ArrayList nestedObjects = new ArrayList(); // nested data objects

    public Group(Group parent, String name, long btreeAddress, long nameHeapAddress) {
      this.parent = parent;
      this.name = name;
      this.dname = (name.length() == 0) ? "root" : name;
      this.btreeAddress = btreeAddress;
      this.nameHeapAddress = nameHeapAddress;
      isNetCDF4 = name.equals("_netCDF");

      // track by address for hard links
      hashGroups.put( new Long(btreeAddress), this);
      //debugOut.println("HEY group address added = "+btreeAddress);
    }

    void read() throws IOException {
      if (debug1) debugOut.println("\n--> Group read <"+dname+">");
      this.nameHeap = new LocalHeap(this, nameHeapAddress);
      this.btree = new GroupBTree( dname, btreeAddress);

      // now read all the entries in the btree
      List sentries = btree.getSymbolTableEntries();
      for (int i=0; i<sentries.size(); i++) {
        SymbolTableEntry s = (SymbolTableEntry) sentries.get(i);
        String sname = nameHeap.getString( (int)s.getNameOffset());
        if (debugSymbolTable) debugOut.println("\n   Symbol name="+sname);

        DataObject o;
        if (s.cacheType == 2) {
          String linkName = nameHeap.getString( (int) s.linkOffset);
          if (debugSymbolTable) debugOut.println("   Symbolic link name="+linkName);
          o = new DataObject(this, sname, linkName);

        } else {
          o = new DataObject(this, sname, s.getObjectAddress());
          o.read();
        }
        nestedObjects.add( o);
        hashDataObjects.put( o.getName(), o); // to look up symbolic links
      }
      if (debug1)  debugOut.println("<-- end Group read <"+dname+">");
    }

    private String getName() {
      return (parent == null) ? name : parent.getName() + "/" + name;
    }

    // is this a child of that ?
    private boolean isChildOf( Group that) {
      if (parent == that) return true;
      if (parent == null) return false;
      return parent.isChildOf( that);
    }

  } // Group

  // Level 1A
  // this just reads in all the entries into a list
  private class GroupBTree {
    protected String owner;
    protected int wantType = 0;
    protected ArrayList entries = new ArrayList(); // list of type Entry
    private ArrayList sentries = new ArrayList(); // list of type SymbolTableEntry

    // for DataBTree
    GroupBTree( String owner) {
      this.owner = owner;
    }

    GroupBTree(String owner, long pos) throws IOException {
      this.owner = owner;

      readAllEntries(pos);

      // now convert the entries to SymbolTableEntry
      for (int i = 0; i < entries.size(); i++) {
        Entry e = (Entry) entries.get(i);
        GroupNode node = new GroupNode(e.address);
        sentries.addAll( node.getSymbols());
      }
    }

    ArrayList getEntries() { return entries; }
    ArrayList getSymbolTableEntries() { return sentries; }

     // recursively read all entries, place them in order in list
    protected void readAllEntries(long pos) throws IOException {
      raf.seek(pos);
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
        debugOut.println("    type=" + type + " level=" + level + " nentries=" +nentries);
      if (type != wantType)
        throw new IllegalStateException("BtreeGroup must be type " + wantType);

      long size = 8 + 2 * sizeOffsets + nentries * (sizeOffsets + sizeLengths);
      if (debugTracker) memTracker.addByLen( "Group BTree ("+owner+")", pos, size);

      long leftAddress = readOffset();
      long rightAddress = readOffset();
      long entryPos = raf.getFilePointer();
      if (debugGroupBtree)
        debugOut.println("    leftAddress=" + leftAddress + " " + Long.toHexString(leftAddress) +
                    " rightAddress=" + rightAddress + " " +  Long.toHexString(rightAddress));

      // read all entries in this Btree "Node"
      ArrayList myEntries = new ArrayList();
      for (int i = 0; i < nentries; i++) {
        myEntries.add( new Entry());
      }

      if (level == 0)
        entries.addAll( myEntries);
      else {
        for (int i = 0; i < myEntries.size(); i++) {
          Entry entry = (Entry) myEntries.get(i);
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
      ArrayList symbols = new ArrayList(); // SymbolTableEntry

      GroupNode( long address) throws IOException {
        this.address = address;

        raf.seek( address);
        if (debugDetail)  debugOut.println("--Group Node position="+raf.getFilePointer());

        // header
        byte[] sig = new byte[4];
        raf.read(sig);
        String nameS = new String(sig);
        if (!nameS.equals("SNOD"))
          throw new IllegalStateException();

        version = raf.readByte();
        raf.readByte(); // skip byte
        nentries = raf.readShort();
        if (debugDetail)  debugOut.println("   version="+version+" nentries="+nentries);

        long posEntry = raf.getFilePointer();
        for (int i=0; i<nentries; i++) {
          SymbolTableEntry entry = new SymbolTableEntry( posEntry);
          posEntry += entry.getSize();
          symbols.add( entry);
        }
        if (debugDetail) debugOut.println("-- Group Node end position="+raf.getFilePointer());
        long size = 8 + nentries * 40;
        if (debugTracker) memTracker.addByLen("Group BtreeNode (" + owner + ")", address, size);
      }

      List getSymbols() { return symbols; }
    }


  }

  DataBTree getDataBTreeAt(String owner, long pos, int ndim) throws IOException {
    return new DataBTree( owner, pos, ndim);
  }

  /**
   * This holds info for chunked data storage.
   * level 1A
   */
  class DataBTree {
    private String owner;
    private long pos;
    private int ndim, wantType;
    private ArrayList entries = new ArrayList();

    DataBTree(String owner, long pos, int ndim) throws IOException {
      this.owner = owner;
      this.pos = pos;
      this.ndim = ndim;
      wantType = 1;

      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);

      readAllEntries(pos);
     }

    // type DataEntry
    ArrayList getEntries() { return entries; }

     // recursively read all entries, place them in order in list
    private void readAllEntries(long pos) throws IOException {
      if (debugDataBtree) {
        debugOut.println("\n--> DataBTree read tree at position=" + pos+" for "+owner);
      }
      raf.seek(pos);

      byte[] name = new byte[4];
      raf.read(name);
      String nameS = new String(name);
      if (!nameS.equals("TREE"))
        throw new IllegalStateException("DataBTree doesnt start with TREE");

      int type = raf.readByte();
      int level = raf.readByte();
      int nentries = raf.readShort();
      if (debugDataBtree)
        debugOut.println("    type=" + type + " level=" + level + " nentries=" +nentries);
      if (type != wantType)
        throw new IllegalStateException("DataBTree must be type " + wantType);

      long size = 8 + 2 * sizeOffsets + nentries * (8 + sizeOffsets + 8 + ndim);
      if (debugTracker) memTracker.addByLen( "Data BTree ("+owner+")", pos, size);

      long leftAddress = readOffset();
      long rightAddress = readOffset();
      long entryPos = raf.getFilePointer();
      if (debugDataBtree)
        debugOut.println("    leftAddress=" + leftAddress + " " + Long.toHexString(leftAddress) +
                    " rightAddress=" + rightAddress + " " +  Long.toHexString(rightAddress));

      // recursively read in the entire Btree
      // if (leftAddress >= 0) readAllEntries(leftAddress, entries);
      //raf.seek(entryPos);

      // read all entries in this Btree "Node"
      ArrayList myEntries = new ArrayList();
      for (int i = 0; i < nentries; i++) {
        myEntries.add( new DataEntry(ndim));
      }

      if (level == 0)
        entries.addAll( myEntries);
      else {
        for (int i = 0; i < myEntries.size(); i++) {
          DataEntry entry = (DataEntry) myEntries.get(i);
          if (debugDataBtree) debugOut.println("  nonzero node entry at =" + entry.address);
          readAllEntries(entry.address);
        }
      }

      //if (rightAddress >= 0) readAllEntries(rightAddress, entries);
    }

    void dump( DataType dt) {
      try {
        for (int i = 0; i < entries.size(); i++) {
          DataEntry node = (DataEntry) entries.get(i);
          if (dt == DataType.STRING) {
            HeapIdentifier heapId = new HeapIdentifier(node.address);
            GlobalHeap.HeapObject ho = heapId.getHeapObject();
            byte[] pa = new byte[ (int) ho.dataSize];
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
    class DataEntry {
      int size; // size of chunk in bytes; need storage layout dimensions to interpret
      int filterMask;
      long[] offset; // offset reletive to entire array
      long address;

      DataEntry(int ndim) throws IOException {
        this.size = raf.readInt();
        this.filterMask = raf.readInt();
        offset = new long[ndim];
        for (int i=0; i<ndim; i++) {
          offset[i] = raf.readLong();
        }
        this.address = readOffset();
        if (debugTracker) memTracker.addByLen("Chunked Data (" + owner + ")", address, size);

        if (debug1) debugOut.println(this);
      }

      public String toString() {
        StringBuffer sbuff = new StringBuffer();
        sbuff.append("  ChunkedDataNode size="+size+" filterMask="+filterMask+" address="+address+" offsets= ");
        for (int k=0; k<offset.length; k++) sbuff.append(offset[k]+" ");
        return sbuff.toString();
      }
    }

  }

  // level 1E
  class GlobalHeap {
    byte version;
    int size;
    ArrayList hos = new ArrayList();
    HeapObject freeSpace = null;

    GlobalHeap(long pos) throws IOException {
      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(pos);

      // header
      byte[] heapname = new byte[4];
      raf.read(heapname);
      String nameS = new String(heapname);
      if (!nameS.equals("GCOL"))
        throw new IllegalStateException(nameS+" should equal GCOL");

      version = raf.readByte();
      raf.skipBytes(3);
      size = raf.readInt();
      if (debugDetail) debugOut.println("-- readGlobalHeap position="+pos+" version= "+version+" size = "+size);
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
        if (debugDetail) debugOut.println("   HeapObject  position="+startPos+" id="+o.id+" refCount= "+o.refCount+
          " dataSize = "+o.dataSize+" dataPos = "+o.dataPos);
        if (o.id == 0) break;

        int nskip = ((int) o.dataSize) + padding((int)o.dataSize, 8);
        raf.skipBytes( nskip);
        hos.add( o);

        count += o.dataSize + 16;
      }

      if (debugDetail) debugOut.println("-- endGlobalHeap position="+raf.getFilePointer());
      if (debugTracker) memTracker.addByLen("GlobalHeap", pos, size);
    }

    class HeapObject {
      short id, refCount;
      long dataSize;
      long dataPos;

      public String toString() { return "dataPos = "+dataPos+" dataSize = "+dataSize; }
    }

  } // GlobalHeap

  // level 1D
  private class LocalHeap {
    Group group;
    int size;
    long freelistOffset, dataAddress;
    byte[] heap;
    byte version;

    LocalHeap( Group group, long pos) throws IOException {
      this.group = group;

      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(pos);

      if (debugDetail) debugOut.println("-- readLocalHeap position="+raf.getFilePointer());

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
      if (debugDetail) debugOut.println(" version="+version+" size="+size+" freelistOffset="+freelistOffset+" heap starts at dataAddress="+dataAddress);
      if (debugPos) debugOut.println("    *now at position="+raf.getFilePointer());

      // data
      raf.seek(dataAddress);
      heap = new byte[size];
      raf.read(heap);
      //if (debugHeap) printBytes( out, "heap", heap, size, true);

      if (debugDetail) debugOut.println("-- endLocalHeap position="+raf.getFilePointer());
      int hsize = 8 + 2 * sizeLengths + sizeOffsets;
      if (debugTracker) memTracker.addByLen("Group LocalHeap ("+group.dname+")", pos, hsize);
      if (debugTracker) memTracker.addByLen("Group LocalHeapData ("+group.dname+")", dataAddress, size);
    }

    public String getString(int offset) {
      int count = 0;
      while (heap[offset+count] != 0) count++;
      return new String(heap, offset, count);
    }

  } // LocalHeap

  // aka Group Entry "level 1C"
  private class SymbolTableEntry {
    long nameOffset, objectHeaderAddress;
    int cacheType, linkOffset;
    long posData;

    boolean isSymbolicLink = false;

    SymbolTableEntry(long pos) throws IOException {
      raf.seek(pos);
      if (debugSymbolTable) debugOut.println("--> readSymbolTableEntry position="+raf.getFilePointer());

      nameOffset = readOffset();
      objectHeaderAddress = readOffset();
      cacheType = raf.readInt();
      raf.skipBytes(4);

      if (debugSymbolTable) {
        debugOut.print(" nameOffset="+nameOffset);
        debugOut.print(" objectHeaderAddress="+objectHeaderAddress);
        debugOut.println(" cacheType="+cacheType);
      }

      // "scratch pad"
      posData = raf.getFilePointer();

      // check for symbolic link
      if (cacheType == 2) {
        linkOffset = raf.readInt(); // offset in local heap
        if (debugSymbolTable) debugOut.println("WARNING Symbolic Link linkOffset=" + linkOffset);
        isSymbolicLink = true;
      }

      //HDFdump( posData, 16);

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
        debugOut.println("<-- end readSymbolTableEntry position="+raf.getFilePointer());

      memTracker.add("SymbolTableEntry", pos, posData+16);
    }

    public int getSize() { return 40; }
    public long getObjectAddress() { return objectHeaderAddress; }
    public long getNameOffset() { return nameOffset; }
  } // SymbolTable


  ///////////////////////////////////////////////////////////////
  // construct netcdf objects

  private void makeNetcdfGroup( ucar.nc2.Group ncGroup, DataObject dataObject) throws IOException {
    Group h5group = dataObject.group;

    // create group attributes
    List messages = dataObject.messages;
    for (int i = 0; i < messages.size(); i++) {
      Message mess = (Message) messages.get(i);
      if (mess.mtype == MessageType.Attribute) {
        MessageAttribute matt = (MessageAttribute) mess.messData;
        makeAttributes(h5group.name, matt, ncGroup.attributes);
      }
    }

    addSystemAttributes( messages, ncGroup.attributes);

    // nested objects
    List nestedObjects = h5group.nestedObjects;
    for (int i=0; i<nestedObjects.size(); i++) {
      DataObject ndo = (DataObject) nestedObjects.get(i);

      if (ndo.group != null) {
        ucar.nc2.Group nestedGroup = new ucar.nc2.Group(ncfile, ncGroup, NetcdfFile.createValidNetcdfObjectName( ndo.group.name));
        ncGroup.addGroup( nestedGroup);
        if (debug1) debugOut.println("--made Group "+nestedGroup.getName()+" add to "+ncGroup.getName());

        makeNetcdfGroup( nestedGroup, ndo);

      } else {

        if (ndo.isVariable) {

          Variable v = makeVariable( ndo.name, ndo.messages, ndo.msl.dataAddress, ndo.mdt,
            ndo.msl, ndo.mds, ndo.mfp);

          if ((v != null) && (v.getDataType() != null)) {
            v.setParentGroup(ncGroup);
            ncGroup.addVariable(v);

            Vinfo vinfo = (Vinfo) v.getSPobject();
            vinfo.pos = ndo.pos; // for debugging

            if (debug1) debugOut.println("  made Variable "+v.getName()+"  vinfo= "+vinfo+"\n"+v);
          }
        }
      }
    } // loop over nested objects

  }

  /**
   * Create Attribute objects from the MessageAttribute and add to list
   * @param forWho whose attribue?
   * @param matt attribute message
   * @param attList add Attribute to this list
   * @throws IOException
   */
  private void makeAttributes( String forWho, MessageAttribute matt, List attList) throws IOException {
    MessageDatatype mdt = matt.mdt;

    if (isNetCDF4 && matt.name.startsWith("_ncdim_"))
      makeNC4Dimension( forWho, matt, mdt);

    else if (isNetCDF4 && matt.name.startsWith("_ncvar_"))
      makeNC4Variable( forWho, matt, mdt);

    else if (mdt.type == 6) { // structure : make seperate attribute for each member
      Iterator iter = mdt.members.iterator();
      while (iter.hasNext()) {
        StructureMember m = (StructureMember) iter.next();
        //String attName = matt.name + "." + m.name;
        //if ((prefix != null) && (prefix.length() > 0)) attName = prefix + "." + attName;
        String attName = matt.name;
        if (mdt.type != 6) // LOOK nested compound attributes
          //makeAttributes( forWho, MessageAttribute matt, attList);
        //else
          attList.add( makeAttribute(forWho, attName, m.mdt, matt.mds, matt.dataPos+ m.offset));
      }
    }

    else {
      // String attName = (prefix == null) || prefix.equals("") ? matt.name : prefix + "." + matt.name;
      String attName = matt.name;
      Attribute att = makeAttribute(forWho, attName, matt.mdt, matt.mds, matt.dataPos);
      if (att != null)
        attList.add( att);
    }

    // reading attribute values might change byte order during a read
    // put back to little endian for further header processing
    raf.order(RandomAccessFile.LITTLE_ENDIAN);
  }

  private void makeNC4Dimension( String varName, MessageAttribute matt, MessageDatatype mdt) throws IOException {
    Attribute attLen = null, attName = null;
    Iterator iter = mdt.members.iterator();
    while (iter.hasNext()) {
      StructureMember m = (StructureMember) iter.next();
      Attribute att = makeAttribute(varName, m.name, m.mdt, matt.mds, matt.dataPos+ m.offset);
      if (m.name.equals("len")) attLen = att; else attName = att;
    }
    int len = attLen.getNumericValue().intValue();
    String name = attName.getStringValue();
    Dimension dim = new Dimension( name, len, true);
    ncfile.addDimension(null, dim);
    // track it by dim id
    int dimId = -1;
    String dimIdS = matt.name.substring(7);
    try {
      dimId = Integer.parseInt( dimIdS);
    } catch ( NumberFormatException e) { }
    dimTable.put( new Integer(dimId), dim);
    if (debug1) debugOut.println("makeNC4Dimension "+matt.name);
  }

  private void makeNC4Variable( String forWho, MessageAttribute matt, MessageDatatype mdt) throws IOException {
    Attribute attDims = null, attName = null, attNDims = null;
    Iterator iter = mdt.members.iterator();
    while (iter.hasNext()) {
      StructureMember m = (StructureMember) iter.next();
      if (m.name.equals("name")) attName = makeAttribute(forWho, m.name, m.mdt, matt.mds, matt.dataPos+ m.offset);
      if (m.name.equals("ndims")) attNDims = makeAttribute(forWho, m.name, m.mdt, matt.mds, matt.dataPos+ m.offset);
      if (m.name.equals("dimids")) attDims = makeAttribute(forWho, m.name, m.mdt, matt.mds, matt.dataPos+ m.offset);
    }
    String name = attName.getStringValue().replace(' ','_');
    int ndims = attNDims.getNumericValue().intValue();
    ArrayList dimList  = new ArrayList();
    for (int i=0; i< ndims; i++) {
      int dimId = attDims.getNumericValue(i).intValue();
      Dimension dim = (Dimension) dimTable.get( new Integer(dimId));
      dimList.add( dim);
    }
    varTable.put( matt.name, new Vatt( name, dimList));
    if (debug1) debugOut.println("makeNC4Variable "+matt.name);
  }

  private Attribute makeAttribute( String forWho, String attName, MessageDatatype mdt, MessageSimpleDataspace mds, long dataPos) throws IOException {
    attName = NetcdfFile.createValidNetcdfObjectName( attName); // look cannot search by name
    Variable v = new Variable( ncfile, null, null, attName); // LOOK null group
    Vinfo vinfo = new Vinfo(mdt, null, dataPos);
    if (!makeVariableShapeAndType( v, mdt, mds, vinfo)) {
      debugOut.println("SKIPPING attribute "+attName+" for "+forWho+" with dataType= "+vinfo.hdfType);
      return null;
    }
    v.setSPobject( vinfo);
    v.setCaching(false);
    if (debug1) debugOut.println("makeAttribute "+attName+" for "+forWho+"; vinfo= "+vinfo);

    Attribute att = new Attribute(attName);
    att.setValues( v.read());

    return att;
  }

/*
   A dataset has Datatype, Dataspace, StorageLayout.
   A structure member only has Datatype.
   An array is specified through Datatype=10. Storage is speced in the parent.
*/
  private Variable makeVariable( String name, List messages, long dataPos,
        MessageDatatype mdt, MessageStorageLayout msl, MessageSimpleDataspace msd, MessageFilter mfp)
          throws IOException {

    Variable v;
    Vinfo vinfo = new Vinfo(mdt, msl, dataPos);
    if (vinfo.getNCDataType() == null) {
      debugOut.println("SKIPPING DataType= "+vinfo.hdfType+" for variable "+name);
      return null;
    }

    // deal with "filters"
    if (mfp != null) {
      if ((mfp.nfilters == 1) && (mfp.filters[0].id == 1)) {
        vinfo.hasFilter = true;
        //debugOut.println("OK variable with Filter= "+mfp+" for variable "+name);
     } else {
        debugOut.println("SKIPPING variable with Filter= "+mfp+" for variable "+name);
        return null;
      }
    }

    // deal with unallocated data
    if (dataPos == -1) {
      vinfo.useFillValue = true;

     // find fill value
      for (int i=0; i<messages.size(); i++) {
        Message mess = (Message) messages.get(i);
        if (mess.mtype == MessageType.FillValue) {
          MessageFillValue fvm = (MessageFillValue)  mess.messData;
          if (fvm.size > 0)
            vinfo.fillValue = fvm.value;

        } else if (mess.mtype == MessageType.FillValueOld) {
          MessageFillValueOld fvm = (MessageFillValueOld)  mess.messData;
          if (fvm.size > 0)
            vinfo.fillValue = fvm.value;
        }
      }

      // if didnt find, use zeroes !!
      if (vinfo.fillValue == null) {
        vinfo.fillValue = new byte[ vinfo.dataType.getSize()];
      }
    }

    // deal with netCDF4
    if (isNetCDF4 && name.startsWith("_ncvar_")) {
      Vatt vatt = (Vatt) varTable.get(name);
      v = new Variable(ncfile, null, null, vatt.name);  // LOOK null group
      v.setDataType( vinfo.getNCDataType());
      v.setDimensions( vatt.dimList);

    } else if (mdt.type == 6) {
      String vname = NetcdfFile.createValidNetcdfObjectName( name); // look cannot search by name
      v = new Structure(ncfile, null, null, vname); // LOOK null group
      makeVariableShapeAndType(v, mdt, msd, vinfo);
      addMembersToStructure( (Structure) v, mdt);
      v.setElementSize( mdt.byteSize);

    } else {
      String vname = NetcdfFile.createValidNetcdfObjectName( name); // look cannot search by name
      v = new Variable(ncfile, null, null, vname);  // LOOK null group
      makeVariableShapeAndType(v, mdt, msd, vinfo);
    }

    // special case of variable length strings
    if (v.getDataType() == DataType.STRING)
      v.setElementSize( 16); // because the array has elemnts that are HeapIdentifier

    v.setSPobject( vinfo);

    // look for attributes
    for (int i=0; i<messages.size(); i++) {
      Message mess = (Message) messages.get(i);

      if (mess.mtype == MessageType.Attribute) {
        MessageAttribute matt = (MessageAttribute) mess.messData;
        makeAttributes( name, matt, v.attributes);

      }
    }

    addSystemAttributes( messages, v.attributes);

    if (!vinfo.signed)
      v.attributes.add( new Attribute("_unsigned", "true"));

    return v;
  }

  private DateFormatter formatter = new DateFormatter();
  private void addSystemAttributes( List messages, List attributes) {
    for (int i=0; i<messages.size(); i++) {
      Message mess = (Message) messages.get(i);

      if (mess.mtype == MessageType.LastModified) {
        MessageLastModified m = (MessageLastModified) mess.messData;
        Date d = new Date(m.secs * 1000);
        attributes.add( new Attribute("_LastModified", formatter.toDateTimeStringISO( d)));
      }

      else if (mess.mtype == MessageType.LastModifiedOld) {
        MessageLastModifiedOld m = (MessageLastModifiedOld) mess.messData;
        try {
          Date d = dateFormat.parse(m.datemod);
          attributes.add( new Attribute("_LastModified", formatter.toDateTimeStringISO( d)));
        }
        catch (ParseException ex) {
          debugOut.println("ERROR parsing date from MessageLastModifiedOld = "+m.datemod);
        }
      }

      else if (mess.mtype == MessageType.Comment) {
        MessageComment m = (MessageComment) mess.messData;
        attributes.add( new Attribute("_Description", NetcdfFile.createValidNetcdfObjectName( m.name)));
      }
    }
  }


  private void addMembersToStructure( Structure s, MessageDatatype mdt) throws IOException {
    Iterator iter = mdt.members.iterator();
    while (iter.hasNext()) {
      StructureMember m = (StructureMember) iter.next();
      Variable v = makeVariable( m.name, new ArrayList(), m.offset, m.mdt, null, null, null);
      if (v != null) {
        s.addMemberVariable( v);
        if (debug1) debugOut.println("  made Member Variable "+v.getName()+"\n"+v);
      }
    }
  }

  private boolean makeVariableShapeAndType( Variable v, MessageDatatype mdt, MessageSimpleDataspace msd, Vinfo vinfo) {

    int[] dim = (msd != null) ? msd.dim : new int[0];
    if (mdt.type == 10) {
      int len = dim.length + mdt.dim.length;
      int[] combinedDim = new int[len];
      for (int i=0; i<dim.length; i++)
        combinedDim[i] = dim[i];  // the dataspace is the outer (slow) dimensions
      for (int i=0; i<mdt.dim.length; i++)
        combinedDim[dim.length+i] = mdt.dim[i];  // type 10 is the inner dimensions
      dim = combinedDim;
    }


    // do this in V3mode
    if ((mdt.type == 3) && v3mode) { // string - gotta add string dimensions
      if (dim == null) // scalar string member variable
        v.setDimensionsAnonymous( new int[] {mdt.byteSize} );
      else {
        int[] shapeOld = dim;
        int[] shape = new int[shapeOld.length + 1];
        for (int k = 0; k < shapeOld.length; k++)
          shape[k] = shapeOld[k];
        shape[shapeOld.length] = mdt.byteSize;
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

    DataType dt = vinfo.getNCDataType();
    if (dt == null) return false;
    v.setDataType( dt);
    return true;
  }




  //////////////////////////////////////////////////////////////
  // utilities

  /**
   * Read a zero terminated String. Leave file positioned after zero terminator byte.
   * @param raf from this file
   * @param pos starting here; if -1 then start at the current file position
   * @return String (dont include zero terminator)
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
   * @param raf from this file
   * @return String (dont include zero terminator)
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
    count += padding( count, 8);
    raf.seek(pos+count);

    return new String(s);
  }

  private long readLength() throws IOException {
    return isLengthLong ? raf.readLong() : (long) raf.readInt();
  }

  private long readOffset() throws IOException {
    return isOffsetLong ? raf.readLong() : (long) raf.readInt();
  }

  private void setOffset(long offset) throws IOException {
    raf.seek(baseAddress + offset);
  }

    // find number of bytes needed to pad to multipleOf byte boundary
  static private int padding( int nbytes, int multipleOf) {
    int pad = nbytes % multipleOf;
    if (pad != 0) pad = multipleOf - pad;
    return pad;
  }

  void dump(String head, long pos, int nbytes, boolean count) throws IOException {
    long savePos = raf.getFilePointer();
    if (pos >= 0) raf.seek(pos);
    byte[] mess = new byte[nbytes];
    raf.read(mess);
    printBytes( head, mess, nbytes, false);
    raf.seek(savePos);
  }

  static void printBytes( String head, byte[] buff, int n, boolean count) {
    debugOut.print(head+" == ");
    for (int i=0; i<n; i++) {
      byte b = buff[i];
      int ub = (b < 0) ? b + 256 : b;
      if (count) debugOut.print( i+":");
      debugOut.print( ub);
      if (!count) {
        debugOut.print( "(");
        debugOut.write(b);
        debugOut.print( ")");
      }
      debugOut.print( " ");
    }
    debugOut.println();
  }

  static void printBytes( String head, byte[] buff, int offset, int n) {
    debugOut.print(head+" == ");
    for (int i=0; i<n; i++) {
      byte b = buff[offset+i];
      int ub = (b < 0) ? b + 256 : b;
      debugOut.print( ub);
      debugOut.print( " ");
    }
    debugOut.println();
  }

  public void close() {
    if (debugTracker) memTracker.report();
  }

  private class MemTracker  {
    private ArrayList memList = new ArrayList();
    private StringBuffer sbuff = new StringBuffer();

    private long fileSize;
    MemTracker(long fileSize) {
      this.fileSize = fileSize;
    }

    void add( String name, long start, long end) {
      memList.add( new Mem( name, start, end));
    }

    void addByLen( String name, long start, long size) {
      memList.add( new Mem( name, start, start+size));
    }

    void report() {
      debugOut.println("Memory used file size= "+fileSize);
      debugOut.println("  start    end   size   name");
      Collections.sort(memList);
      Mem prev = null;
      for (int i = 0; i < memList.size(); i++) {
        Mem m = (Mem) memList.get(i);
        if ((prev != null) && (m.start > prev.end))
          doOne('+',prev.end, m.start, m.start-prev.end, "HOLE");
        char c = ((prev != null) && (prev.end != m.start)) ? '*' : ' ';
        doOne(c, m.start, m.end, m.end - m.start, m.name);
        prev = m;
      }
      debugOut.println();
    }

    private void doOne(char c, long start, long end, long size, String name) {
        sbuff.setLength(0);
        sbuff.append(c);
        sbuff.append( Format.l(start, 6));
        sbuff.append( " ");
        sbuff.append( Format.l(end, 6));
        sbuff.append( " ");
        sbuff.append( Format.l(size, 6));
        sbuff.append( " ");
        sbuff.append( name);
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