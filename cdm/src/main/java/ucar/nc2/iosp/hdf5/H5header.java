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

import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Format;
import ucar.unidata.util.SpecialMathFunction;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.*;
import ucar.nc2.EnumTypedef;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.nc2.iosp.LayoutTiled;
import ucar.nc2.iosp.Layout;
import ucar.nc2.iosp.LayoutRegular;
import ucar.ma2.*;

import java.util.*;
import java.text.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.*;
import java.nio.charset.Charset;

/**
 * Read all of the metadata of an HD5 file.
 *
 * @author caron
 */

/* Implementation notes
 * any field called address is actually reletive to the base address.
 * any field called filePos or dataPos is a byte offset within the file.
 */
public class H5header {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5header.class);
  static private String utf8CharsetName = "UTF-8";
  static private Charset utf8Charset = Charset.forName(utf8CharsetName); // cant use until 1.6

  // debugging
  static private boolean debugEnum = false, debugVlen = false;
  static private boolean debug1 = false, debugDetail = false, debugPos = false, debugHeap = false, debugV = false;
  static private boolean debugGroupBtree = false, debugDataBtree = false, debugDataChunk = false, debugBtree2 = false;
  static private boolean debugContinueMessage = false, debugTracker = false, debugSoftLink = false, debugSymbolTable = false;
  static private boolean warnings = false, debugReference = false, debugRegionReference = false, debugCreationOrder = false, debugFractalHeap = false;
  static private boolean debugDimensionScales = false;

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debug1 = debugFlag.isSet("H5header/header");
    debugBtree2 = debugFlag.isSet("H5header/btree2");
    debugContinueMessage = debugFlag.isSet("H5header/continueMessage");
    debugDetail = debugFlag.isSet("H5header/headerDetails");
    debugDataBtree = debugFlag.isSet("H5header/dataBtree");
    debugDataChunk = debugFlag.isSet("H5header/dataChunk");
    debugGroupBtree = debugFlag.isSet("H5header/groupBtree");
    debugFractalHeap = debugFlag.isSet("H5header/fractalHeap");
    debugHeap = debugFlag.isSet("H5header/Heap");
    debugPos = debugFlag.isSet("H5header/filePos");
    debugReference = debugFlag.isSet("H5header/reference");
    debugSoftLink = debugFlag.isSet("H5header/softLink");
    debugSymbolTable = debugFlag.isSet("H5header/symbolTable");
    debugTracker = debugFlag.isSet("H5header/memTracker");
    debugV = debugFlag.isSet("H5header/Variable");
  }

  static private final byte[] head = {(byte) 0x89, 'H', 'D', 'F', '\r', '\n', 0x1a, '\n'};
  static private final String hdf5magic = new String(head);
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
      if (magic.equals(hdf5magic))
        return true;
      filePos = (filePos == 0) ? 512 : 2 * filePos;
    }

    return false;
  }

  ////////////////////////////////////////////////////////////////////////////////

  private RandomAccessFile raf;
  private ucar.nc2.NetcdfFile ncfile;
  private H5iosp h5iosp;

  private long baseAddress;
  private byte sizeOffsets, sizeLengths;
  private boolean isOffsetLong, isLengthLong;

  private H5Group rootGroup;
  private Map<String, DataObjectFacade> symlinkMap = new HashMap<String, DataObjectFacade>(200);
  private Map<Long, DataObject> addressMap = new HashMap<Long, DataObject>(200);

  private Map<Long, GlobalHeap> heapMap = new HashMap<Long, GlobalHeap>();
  //private Map<Long, H5Group> hashGroups = new HashMap<Long, H5Group>(100);

  DateFormatter formatter = new DateFormatter();
  private java.text.SimpleDateFormat hdfDateParser;

  private java.io.PrintStream debugOut = System.out;
  //private Formatter debugOut = new Formatter(System.out);
  private MemTracker memTracker;

  H5header(RandomAccessFile myRaf, ucar.nc2.NetcdfFile ncfile, H5iosp h5iosp) {
    this.ncfile = ncfile;
    this.raf = myRaf;
    this.h5iosp = h5iosp;
  }

  public void read(java.io.PrintStream debugPS) throws IOException {
    if (debugPS != null)
      debugOut = debugPS;

    long actualSize = raf.length();
    memTracker = new MemTracker(actualSize);

    if (!isValidFile(raf))
      throw new IOException("Not a netCDF4/HDF5 file ");
    // if (debug1) debugOut.format("H5header 0pened file to read:'%s' size= %d %n", ncfile.getLocation(), + actualSize);
    if (debug1) debugOut.println("H5header 0pened file to read:'" + raf.getLocation() +"' size= " + actualSize);
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
    replaceSymbolicLinks(rootGroup);

    // recursively run through all the dataObjects and add them to the ncfile
    makeNetcdfGroup(ncfile.getRootGroup(), rootGroup);

    /*if (debugReference) {
     System.out.println("DataObjects");
     for (DataObject ob : addressMap.values())
       System.out.println("  " + ob.name + " address= " + ob.address + " filePos= " + getFileOffset(ob.address));
   } */
    if (debugTracker) memTracker.report();
  }

  private void readSuperBlock1(long superblockStart, byte versionSB) throws IOException {
    byte versionFSS, versionGroup, versionSHMF;
    short btreeLeafNodeSize, btreeInternalNodeSize;
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

    //debugOut.println(" position="+mapBuffer.position());

    fileFlags = raf.readInt();
    if (debugDetail) debugOut.println(" fileFlags= 0x" + Integer.toHexString(fileFlags));

    if (versionSB == 1) {
      short storageInternalNodeSize = raf.readShort();
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
    DataObjectFacade f = new DataObjectFacade(null, "", rootObjectAddress);
    rootGroup = new H5Group(f);

    /* LOOK what is this crap ??
    if (rootGroup.group == null) {
      // if the root object doesnt have a group message, check if the rootEntry is cache type 2
      if (rootEntry.btreeAddress != 0) {
        rootGroup.group = new GroupOld(null, "", rootEntry.btreeAddress, rootEntry.nameHeapAddress);
      } else {
        throw new IllegalStateException("root object not a group");
      }
    } */
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

    DataObjectFacade f = new DataObjectFacade(null, "", rootObjectAddress);
    rootGroup = new H5Group(f);
  }

  private void replaceSymbolicLinks(H5Group group) {
    if (group == null) return;

    List<DataObjectFacade> objList = group.nestedObjects;
    int count = 0;
    while (count < objList.size()) {
      DataObjectFacade dof = objList.get(count);

      if (dof.group != null) {           // group - recurse
        replaceSymbolicLinks(dof.group);

      } else if (dof.linkName != null) { // symbolic links
        DataObjectFacade link = symlinkMap.get(dof.linkName);
        if (link == null) {
          log.error(" WARNING Didnt find symbolic link=" + dof.linkName + " from " + dof.name);
          objList.remove(count);
          continue;
        }

        // dont allow loops
        if (link.group != null) {
          if (group.isChildOf(link.group)) {
            log.error(" ERROR Symbolic Link loop found =" + dof.linkName);
            objList.remove(count);
            continue;
          }
        }

        objList.set(count, link);
        if (debugSoftLink) debugOut.println("  Found symbolic link=" + dof.linkName);
      }

      count++;
    }
  }

  ///////////////////////////////////////////////////////////////
  // construct netcdf objects

  private void makeNetcdfGroup(ucar.nc2.Group ncGroup, H5Group h5group) throws IOException {
    if (h5group == null) return;

    // look for dimension scales
    for (DataObjectFacade facade : h5group.nestedObjects) {
      if (facade.isVariable)
        findDimensionScales(ncGroup, h5group, facade);
    }
    for (DataObjectFacade facade : h5group.nestedObjects) {
      if (facade.isVariable)
        findDimensionLists(ncGroup, h5group, facade);
    }
    createDimensions(ncGroup, h5group);

    // nested objects - groups and variables
    for (DataObjectFacade facadeNested : h5group.nestedObjects) {

      if (facadeNested.isGroup) {
        ucar.nc2.Group nestedGroup = new ucar.nc2.Group(ncfile, ncGroup, facadeNested.name);
        ncGroup.addGroup(nestedGroup);
        if (debug1) debugOut.println("--made Group " + nestedGroup.getName() + " add to " + ncGroup.getName());
        H5Group h5groupNested = new H5Group(facadeNested);
        makeNetcdfGroup(nestedGroup, h5groupNested);

      } else if (facadeNested.isVariable) {
        if (debugReference && facadeNested.dobj.mdt.type == 7) debugOut.println(facadeNested);

        Variable v = makeVariable(ncGroup, facadeNested);
        if ((v != null) && (v.getDataType() != null)) {
          v.setParentGroup(ncGroup);
          ncGroup.addVariable(v);

          if (v.getDataType().isEnum()) {
            EnumTypedef enumTypedef = ncGroup.findEnumeration(facadeNested.name);
            if (enumTypedef == null) {
              enumTypedef = new EnumTypedef(facadeNested.name, facadeNested.dobj.mdt.map);
              ncGroup.addEnumeration(enumTypedef);
            }
            v.setEnumTypedef(enumTypedef);
          }

          Vinfo vinfo = (Vinfo) v.getSPobject();
          if (debugV) debugOut.println("  made Variable " + v.getName() + "  vinfo= " + vinfo + "\n" + v);
        }

      } else if (facadeNested.isTypedef) {
        if (debugReference && facadeNested.dobj.mdt.type == 7) debugOut.println(facadeNested);

        if (facadeNested.dobj.mdt.map != null) {
          EnumTypedef enumTypedef = ncGroup.findEnumeration(facadeNested.name);
          if (enumTypedef == null) {
            enumTypedef = new EnumTypedef(facadeNested.name, facadeNested.dobj.mdt.map);
            ncGroup.addEnumeration(enumTypedef);
          }
        }
        if (debugV) debugOut.println("  made enumeration " + facadeNested.name);

      } else if (!facadeNested.isDimensionNotVariable && warnings) {
        debugOut.println("WARN:  DataObject ndo " + facadeNested + " not a Group or a Variable");
      }

    } // loop over nested objects

    // create group attributes last. need enums to be found first
    filterAttributes(h5group.facade.dobj.attributes);
    for (MessageAttribute matt : h5group.facade.dobj.attributes) {
      try {
        makeAttributes(null, matt, ncGroup.getAttributes());
      } catch (InvalidRangeException e) {
        throw new IOException(e.getMessage());
      }
    }

    // add system attributes
    processSystemAttributes(h5group.facade.dobj.messages, ncGroup.getAttributes());


  }

  private void findDimensionScales(ucar.nc2.Group g, H5Group h5group, DataObjectFacade facade) throws IOException {
    // first must look for coordinate variables (dimension scales)
    Iterator<MessageAttribute> iter = facade.dobj.attributes.iterator();
    while (iter.hasNext()) {
      MessageAttribute matt = iter.next();
      if (matt.name.equals("CLASS")) {
        Attribute att = makeAttribute(matt);
        String val = att.getStringValue();
        if (val.equals("DIMENSION_SCALE")) { // create a dimension
          facade.dimList = addDimension(g, h5group, facade.name, facade.dobj.mds.dimLength[0], facade.dobj.mds.maxLength[0] == -1);
          iter.remove();
          if (debugDimensionScales) System.out.printf("Found dimScale %s for group '%s' matt=%s %n",
                  facade.dimList, g.getName(), matt);
        }
      }
    }
  }

  private void findDimensionLists(ucar.nc2.Group g, H5Group h5group, DataObjectFacade facade) throws IOException {
    // now look for dimension lists and clean up the attributes
    Iterator<MessageAttribute> iter = facade.dobj.attributes.iterator();
    while (iter.hasNext()) {
      MessageAttribute matt = iter.next();
      // find the dimensions - set length to maximum
      if (matt.name.equals("DIMENSION_LIST")) { // references : may extend the dimension length
        Attribute att = makeAttribute(matt);
        StringBuilder sbuff = new StringBuilder();
        for (int i = 0; i < att.getLength(); i++) {
          String name = att.getStringValue(i);
          String dimName = extendDimension(g, h5group, name, facade.dobj.mds.dimLength[i]);
          sbuff.append(dimName).append(" ");
        }
        facade.dimList = sbuff.toString();
        if (debugDimensionScales) System.out.printf("Found dimList '%s' for group '%s' matt=%s %n",
                facade.dimList, g.getName(), matt);
        iter.remove();

      } else if (matt.name.equals("NAME")) {
        Attribute att = makeAttribute(matt);
        String val = att.getStringValue();
        if (val.startsWith("This is a netCDF dimension but not a netCDF variable")) {
          facade.isVariable = false;
          facade.isDimensionNotVariable = true;
        }
        iter.remove();
        if (debugDimensionScales) System.out.printf("Found %s %n", val);

      } else if (matt.name.equals("REFERENCE_LIST"))
        iter.remove();
    }

  }

   /* private String addDimension(ucar.nc2.Group g, H5Group h5group, String name, int length, boolean isUnlimited) {
    int pos = name.lastIndexOf("/");
    String dimName = (pos > 0) ? name.substring(pos + 1) : name;
    Dimension d = h5group.dimMap.get(dimName); // first look in current group
    if (d == null) {
      d = new Dimension(dimName, length, true, isUnlimited, false);
      d.setGroup(g);
      h5group.dimMap.put(dimName, d);
      h5group.dimList.add(d);
      if (debugDimensionScales) debugOut.println("addDimension name=" + name + " dim= " + d + " to group " + g);
    }

    return d.getName();
  }  */

  private String addDimension(ucar.nc2.Group g, H5Group h5group, String name, int length, boolean isUnlimited) {
    int pos = name.lastIndexOf("/");
    String dimName = (pos > 0) ? name.substring(pos + 1) : name;

    Dimension d = h5group.dimMap.get(dimName); // first look in current group
    //if (d == null)
    //  d = g.findDimension(dimName); // then look in parent groups

    if (d == null) { // create if not found
      d = new Dimension(dimName, length, true, isUnlimited, false);
      d.setGroup(g);
      h5group.dimMap.put(dimName, d);
      h5group.dimList.add(d);
      if (debugDimensionScales) debugOut.println("addDimension name=" + name + " dim= " + d + " to group " + g);

    }

    return d.getName();
  }

  private String extendDimension(ucar.nc2.Group g, H5Group h5group, String name, int length) {
    int pos = name.lastIndexOf("/");
    String dimName = (pos > 0) ? name.substring(pos + 1) : name;

    Dimension d = h5group.dimMap.get(dimName); // first look in current group
    if (d == null)
      d = g.findDimension(dimName); // then look in parent groups

    if (d != null) {
      if (length > d.getLength())
        d.setLength(length);
      return d.getName();
    }

    return null;
  }

  private void createDimensions(ucar.nc2.Group g, H5Group h5group) throws IOException {
    for (Dimension d : h5group.dimList) {
      g.addDimension(d);
    }
  }

  private void filterAttributes(List<MessageAttribute> attList) {
    Iterator<MessageAttribute> iter = attList.iterator();
    while (iter.hasNext()) {
      MessageAttribute matt = iter.next();
      if (matt.name.equals("_nc3_strict")) iter.remove();
    }
  }

  /**
   * Create Attribute objects from the MessageAttribute and add to list
   *
   * @param s       if attribute for a Structure, then deconstruct and add to member variables
   * @param matt    attribute message
   * @param attList add Attribute to this list
   * @throws IOException on io error
   * @throws ucar.ma2.InvalidRangeException on shape error
   */
  private void makeAttributes(Structure s, MessageAttribute matt, List<Attribute> attList) throws IOException, InvalidRangeException {
    MessageDatatype mdt = matt.mdt;

    if (mdt.type == 6) { // structure
      Vinfo vinfo = new Vinfo(matt.mdt, matt.mds, matt.dataPos);
      ArrayStructure attData = (ArrayStructure) readAttributeData(matt, vinfo, DataType.STRUCTURE);

      if (null == s) {
        // flatten and add to list
        for (StructureMembers.Member sm : attData.getStructureMembers().getMembers()) {
          Array memberData = attData.extractMemberArray(sm);
          attList.add(new Attribute(matt.name+"."+sm.getName(), memberData));
        }

      } else {  // assign seperate attribute for each member
        StructureMembers smember = attData.getStructureMembers();
        for (Variable v : s.getVariables()) {
          StructureMembers.Member sm = smember.findMember(v.getShortName());
          if (null != sm) {
            Array memberData = attData.extractMemberArray(sm);
            v.addAttribute(new Attribute(matt.name, memberData));
          }
        }

        // look for unassigned membres, add to the list
        for (StructureMembers.Member sm : attData.getStructureMembers().getMembers()) {
          if (s.findVariable(sm.getName()) == null) {
            Array memberData = attData.extractMemberArray(sm);
            attList.add(new Attribute(matt.name+"."+sm.getName(), memberData));
          }
        }
      }

    } else {
      // make a single attribute
      attList.add( makeAttribute(matt));
    }

    // reading attribute values might change byte order during a read
    // put back to little endian for further header processing
    raf.order(RandomAccessFile.LITTLE_ENDIAN);
  }

  private Attribute makeAttribute(MessageAttribute matt) throws IOException {
    Vinfo vinfo = new Vinfo(matt.mdt, matt.mds, matt.dataPos);
    DataType dtype = vinfo.getNCDataType();

    // check for empty attribute case
    if (matt.mds.type == 2) {
      return new Attribute(matt.name, dtype);
    }

    Array attData = null;
    try {
      attData = readAttributeData(matt, vinfo, dtype);
      attData.setUnsigned(matt.mdt.unsigned);
      
    } catch (InvalidRangeException e) {
      log.error("failed to read Attribute "+matt.name, e);
      return null;
    }

    Attribute result;
    if (attData.getElementType() == Array.class) { // vlen LOOK
      List<Object> dataList = new ArrayList<Object>();
      while (attData.hasNext()) {
        Array nested = (Array) attData.next();
        while (nested.hasNext())
          dataList.add(nested.next());
      }
      result = new Attribute(matt.name, dataList);

    } else {
      result =  new Attribute(matt.name, attData);
    }

    raf.order(RandomAccessFile.LITTLE_ENDIAN);
    return result;
  }

  // read attribute values without creating a Variable
  private Array readAttributeData(H5header.MessageAttribute matt, H5header.Vinfo vinfo, DataType dataType) throws IOException, InvalidRangeException {
    boolean debugStructure = false;
    int[] shape = matt.mds.dimLength;

    // Structures
    if (dataType == DataType.STRUCTURE) {
      boolean hasStrings = false;

      StructureMembers sm = new StructureMembers(matt.name);
      for (H5header.StructureMember h5sm : matt.mdt.members) {

        // from tkunicki@usgs.gov 2/19/2010 - fix for compound attributes
        //DataType dt = getNCtype(h5sm.mdt.type, h5sm.mdt.byteSize);
        //StructureMembers.Member m = sm.addMember(h5sm.name, null, null, dt, new int[] {1});

        DataType dt = null;
        int[] dim = null;
        switch (h5sm.mdt.type) {
          case 9:  // STRING
            dt = DataType.STRING;
            dim = new int[]{1};
            break;
          case 10: // ARRAY
            dt = getNCtype(h5sm.mdt.base.type, h5sm.mdt.base.byteSize);
            dim = h5sm.mdt.dim;
            break;
          default: // PRIMITIVE
            dt = getNCtype(h5sm.mdt.type, h5sm.mdt.byteSize);
            dim = new int[] { 1 };
            break;
        }
        StructureMembers.Member m = sm.addMember(h5sm.name, null, null, dt, dim);

        if (h5sm.mdt.byteOrder >= 0) // apparently each member may have seperate byte order (!!!??)
          m.setDataObject(h5sm.mdt.byteOrder == RandomAccessFile.LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        m.setDataParam((int) (h5sm.offset)); // offset since start of Structure
        if (dt == DataType.STRING)
          hasStrings = true;
      }


      int recsize = matt.mdt.byteSize;
      Layout layout = new LayoutRegular(matt.dataPos, recsize, shape, new Section(shape));
      sm.setStructureSize(recsize);

      // place data into an ArrayStructureBB for efficiency
      ArrayStructureBB asbb = new ArrayStructureBB(sm, shape);
      byte[] byteArray = asbb.getByteBuffer().array();
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null) continue;
        if (debugStructure)
          System.out.println(" readStructure " + matt.name + " chunk= " + chunk + " index.getElemSize= " + layout.getElemSize());
        // copy bytes directly into the underlying byte[]
        raf.seek(chunk.getSrcPos());
        raf.read(byteArray, (int) chunk.getDestElem() * recsize, chunk.getNelems() * recsize);
      }

      // strings are stored on the heap, and must be read separately
      if (hasStrings) {
        int destPos = 0;
        for (int i = 0; i< layout.getTotalNelems(); i++) { // loop over each structure
          h5iosp.convertStrings(asbb, destPos, sm);
          destPos += layout.getElemSize();
        }
      }
      return asbb;
    } // Structure case

    // Strings
    if ((vinfo.typeInfo.hdfType == 9) && (vinfo.typeInfo.isVString)) {
      Layout layout = new LayoutRegular(matt.dataPos, matt.mdt.byteSize, shape, new Section(shape));
      ArrayObject.D1 data = new ArrayObject.D1(String.class, (int) layout.getTotalNelems());
      int count = 0;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null) continue;
        for (int i = 0; i < chunk.getNelems(); i++) {
          long address = chunk.getSrcPos() + layout.getElemSize() * i;
          String sval = readHeapString(address);
          data.set(count++, sval);
        }
      }
      return data;
    } // vlen case

    // Vlen (non-String)
    if (vinfo.typeInfo.hdfType == 9) { // vlen
      DataType readType = dataType;
      if (vinfo.typeInfo.base.hdfType == 7) // reference
        readType = DataType.LONG;
      // LOOK: other cases tested ???

      Layout layout = new LayoutRegular(matt.dataPos, matt.mdt.byteSize, shape, new Section(shape));

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
          Array vlenArray = getHeapDataArray(address, readType, vinfo.typeInfo.byteOrder);
          if (vinfo.typeInfo.base.hdfType == 7)
            data[count++] = h5iosp.convertReference(vlenArray);
          else
            data[count++] = vlenArray;
        }
      }
      return (scalar) ? data[0] : Array.factory(Array.class, shape, data);
    } // vlen case

    // NON-STRUCTURE CASE
    DataType readDtype = dataType;
    int elemSize = dataType.getSize();
    int byteOrder = vinfo.typeInfo.byteOrder;

    if (vinfo.typeInfo.hdfType == 2) { // time
      readDtype = vinfo.mdt.timeType;
      elemSize = readDtype.getSize();

    } else if (vinfo.typeInfo.hdfType == 3) { // char
      if (vinfo.mdt.byteSize > 1) {
        int[] newShape = new int[shape.length + 1];
        System.arraycopy(shape, 0, newShape, 0, shape.length);
        newShape[shape.length] = vinfo.mdt.byteSize ;
        shape = newShape;
      }

    } else if (vinfo.typeInfo.hdfType == 5) { // opaque
      elemSize = vinfo.mdt.byteSize;

    } else if (vinfo.typeInfo.hdfType == 8) { // enum
      H5header.TypeInfo baseInfo = vinfo.typeInfo.base;
      readDtype = baseInfo.dataType;
      elemSize = readDtype.getSize();
      byteOrder = baseInfo.byteOrder;
    }

    Layout layout = new LayoutRegular(matt.dataPos, elemSize, shape, new Section(shape));
    Object data = h5iosp.readDataPrimitive(layout, dataType, shape, null, byteOrder, false);
    Array dataArray = null;

    if ((dataType == DataType.CHAR)) {
      if (vinfo.mdt.byteSize > 1) { // chop back into pieces
        byte [] bdata = (byte[]) data;
        int strlen = vinfo.mdt.byteSize;
        int n = bdata.length / strlen;
        ArrayObject.D1 sarray = new ArrayObject.D1( String.class, n);
        for (int i=0; i<n; i++) {
          String sval = convertString( bdata, i*strlen, strlen);
          sarray.set(i, sval);
        }
        dataArray = sarray;

      } else {
        String sval = convertString( (byte[]) data);
        ArrayObject.D1 sarray = new ArrayObject.D1( String.class, 1);
        sarray.set(0, sval);
        dataArray = sarray;
      }
      
    } else {
      dataArray = (data instanceof Array) ? (Array) data : Array.factory( readDtype, shape, data);
    }

    // convert attributes to enum strings
    if ((vinfo.typeInfo.hdfType == 8) && (matt.mdt.map != null)) {
      dataArray = convertEnums( matt.mdt.map, dataArray);
    }

    return dataArray;
  }

  private String convertString(byte[] b) throws UnsupportedEncodingException {
    // null terminates
    int count = 0;
    while (count < b.length) {
      if (b[count] == 0) break;
      count++;
    }
    return new String(b, 0, count, "UTF-8"); // all strings are considered to be UTF-8 unicode
  }

  private String convertString(byte[] b, int start, int len) throws UnsupportedEncodingException {
    // null terminates
    int count = start;
    while (count < start + len) {
      if (b[count] == 0) break;
      count++;
    }
    return new String(b, start, count-start, "UTF-8"); // all strings are considered to be UTF-8 unicode
  }

  protected Array convertEnums(Map<Integer, String> map, Array values) {
    Array result = Array.factory(DataType.STRING, values.getShape());
    IndexIterator ii = result.getIndexIterator();
    values.resetLocalIterator();
    while (values.hasNext()) {
      String sval = map.get(values.nextInt());
      ii.setObjectNext(sval == null ? "Unknown" : sval);
    }
    return result;
  }

  /* private Attribute makeAttribute(String forWho, String attName, MessageDatatype mdt, MessageDataspace mds, long dataPos) throws IOException {
    ucar.ma2.Array data = getAttributeData(forWho, null, attName, mdt, mds, dataPos);

    if (data.getElementType() == Array.class) { // vlen
      List dataList = new ArrayList();
      while (data.hasNext()) {
        Array nested = (Array) data.next();
        while (nested.hasNext())
          dataList.add(nested.next());
      }
      return new Attribute(attName, dataList);
    }
    return (data == null) ? null : new Attribute(attName, data);
  }

  private Array getAttributeData(String forWho, Structure s, String attName, MessageDatatype mdt, MessageDataspace mds, long dataPos) throws IOException {
    ucar.ma2.Array data;

    // make a temporary variable, so we can use H5iosp to read the data
    Variable v;
    if (mdt.type == 6) {
      Structure satt = new Structure(ncfile, null, null, attName);
      satt.setMemberVariables(s.getVariables());
      v = satt;
      v.setElementSize(mdt.byteSize);

    } else {
      v = new Variable(ncfile, null, null, attName);
    }

    // make its Vinfo object
    Vinfo vinfo = new Vinfo(mdt, mds, dataPos);
    if (!makeVariableShapeAndType(v, mdt, mds, vinfo, null)) {
      debugOut.println("SKIPPING attribute " + attName + " for " + forWho + " with dataType= " + vinfo.typeInfo.hdfType);
      return null;
    }
    v.setSPobject(vinfo);
    vinfo.setOwner(v);
    v.setCaching(false);
    if (debug1) debugOut.println("makeAttribute " + attName + " for " + forWho + "; vinfo= " + vinfo);

    // read the data
    if ((mdt.type == 7) && attName.equals("DIMENSION_LIST")) { // convert to dimension names (LOOK is this netcdf4 specific?)
      if (mdt.referenceType == 0)
        data = readReferenceObjectNames(v);
      else {  // not doing reference regions here
        debugOut.println("SKIPPING attribute " + attName + " for " + forWho + " with referenceType= " + mdt.referenceType);
        return null;
      }

    } else {
      try {
        data = h5iosp.readData(v, v.getShapeAsSection());
      } catch (InvalidRangeException e) {
        log.error("H5header.makeAttribute", e);
        if (debug1) debugOut.println("ERROR attribute " + e.getMessage());
        return null;
      }

      // convert attributes to enum strings
      if (v.getDataType().isEnum()) {
        // LOOK EnumTypedef enumTypedef = ncfile.getRootGroup().findEnumeration( mdt.enumTypeName);
        EnumTypedef enumTypedef = v.getEnumTypedef();
        if (enumTypedef != null)
          data = convertEnums( enumTypedef, data); 
      }
    }

    return data;
  }

  protected Array convertEnums(EnumTypedef enumTypedef, Array values) {
    Array result = Array.factory(DataType.STRING, values.getShape());
    IndexIterator ii = result.getIndexIterator();
    while (values.hasNext()) {
      String sval = enumTypedef.lookupEnumString(values.nextInt());
      ii.setObjectNext(sval);
    }
    return result;
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

  private Variable makeVariable(ucar.nc2.Group ncGroup, DataObjectFacade facade) throws IOException {

    Vinfo vinfo = new Vinfo(facade);
    if (vinfo.getNCDataType() == null) {
      debugOut.println("SKIPPING DataType= " + vinfo.typeInfo.hdfType + " for variable " + facade.name);
      return null;
    }

    // deal with filters, cant do SZIP
    if (facade.dobj.mfp != null) {
      for (Filter f : facade.dobj.mfp.filters) {
        if (f.id == 4) {
          debugOut.println("SKIPPING variable with SZIP Filter= " + facade.dobj.mfp + " for variable " + facade.name);
          return null;
        }
      }
    }

    // find fill value
    Attribute fillAttribute = null;
    for (HeaderMessage mess : facade.dobj.messages) {
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
        Object defFillValue = vinfo.getFillValueDefault(vinfo.typeInfo.dataType);
        if (!fillValue.equals(defFillValue))
          fillAttribute = new Attribute("_FillValue", (Number) fillValue);
      }
    }

    long dataAddress = facade.dobj.msl.dataAddress;

    // deal with unallocated data
    if (dataAddress == -1) {
      vinfo.useFillValue = true;

      // if didnt find, use zeroes !!
      if (vinfo.fillValue == null) {
        vinfo.fillValue = new byte[vinfo.typeInfo.dataType.getSize()];
      }
    }

    Variable v;
    Structure s = null;
    if (facade.dobj.mdt.type == 6) { // Compound
      String vname = facade.name;
      s = new Structure(ncfile, ncGroup, null, vname);
      v = s;
      if (!makeVariableShapeAndType(v, facade.dobj.mdt, facade.dobj.mds, vinfo, facade.dimList)) return null;
      addMembersToStructure(ncGroup, s, vinfo, facade.dobj.mdt);
      v.setElementSize(facade.dobj.mdt.byteSize);

    } else {
      String vname = facade.name;
      v = new Variable(ncfile, ncGroup, null, vname);
      if (!makeVariableShapeAndType(v, facade.dobj.mdt, facade.dobj.mds, vinfo, facade.dimList)) return null;
    }

    // special case of variable length strings
    if (v.getDataType() == DataType.STRING)
      v.setElementSize(16); // because the array has elements that are HeapIdentifier

    v.setSPobject(vinfo);

    // look for attributes
    for (MessageAttribute matt : facade.dobj.attributes) {
      try {
        makeAttributes(s, matt, v.getAttributes());
      } catch (InvalidRangeException e) {
        throw new IOException(e.getMessage());
      }
    }
    processSystemAttributes(facade.dobj.messages, v.getAttributes());
    if (fillAttribute != null && v.findAttribute("_FillValue") == null)
      v.addAttribute(fillAttribute);
    if (vinfo.typeInfo.unsigned)
      v.addAttribute(new Attribute("_Unsigned", "true"));
    if (facade.dobj.mdt.type == 5) {
      String desc = facade.dobj.mdt.opaque_desc;
      if ((desc != null) && (desc.length() > 0))
        v.addAttribute(new Attribute("_opaqueDesc", desc));
    }

    if (vinfo.isChunked) // make the data btree, but entries are not read in
      vinfo.btree = new DataBTree(dataAddress, v.getShape(), vinfo.storageSize);

    if (transformReference && (facade.dobj.mdt.type == 7) && (facade.dobj.mdt.referenceType == 0)) { // object reference
      // System.out.println("transform object Reference: facade=" + facade.name +" variable name=" + v.getName());
      Array newData = findReferenceObjectNames(v.read());
      v.setDataType(DataType.STRING);
      v.setCachedData(newData, true); // so H5iosp.read() is never called
      v.addAttribute(new Attribute("_HDF5ReferenceType", "values are names of referenced Variables"));
    }

    if (transformReference && (facade.dobj.mdt.type == 7) && (facade.dobj.mdt.referenceType == 1)) { // region reference
      System.out.println("transform region Reference: facade=" + facade.name +" variable name=" + v.getName());
      int nelems = (int) v.getSize();
      int heapIdSize = 12;
      /* doesnt work yet
      for (int i = 0; i < nelems; i++) {
        H5header.RegionReference heapId = new RegionReference(vinfo.dataPos + heapIdSize * i); // LOOK doesnt work
      } */

      // fake data for now
      v.setDataType(DataType.LONG);
      Array newData = Array.factory(DataType.LONG, v.getShape());
      v.setCachedData(newData, true); // so H5iosp.read() is never called
      v.addAttribute(new Attribute("_HDF5ReferenceType", "values are regions of referenced Variables"));
    }

    // debugging
    vinfo.setOwner(v);
    if ((vinfo.typeInfo.hdfType == 7) && warnings)
      debugOut.println("WARN:  Variable " + facade.name + " is a Reference type");
    if ((vinfo.mfp != null) && (vinfo.mfp.filters[0].id != 1) && warnings)
      debugOut.println("WARN:  Variable " + facade.name + " has a Filter = " + vinfo.mfp);
    if (debug1) debugOut.println("makeVariable " + v.getName() + "; vinfo= " + vinfo);

    return v;
  }

  // convert an array of lons which are data object references to an array of strings,
  // the names of the data objects (dobj.who)
  private Array findReferenceObjectNames(Array data) throws IOException {
    //Array data = v.read();
    IndexIterator ii = data.getIndexIterator();

    Array newData = Array.factory(DataType.STRING, data.getShape());
    IndexIterator ii2 = newData.getIndexIterator();
    while (ii.hasNext()) {
      long objId = ii.getLongNext();
      DataObject dobj = getDataObject(objId, null);
      if (dobj == null)
        log.error("readReferenceObjectNames cant find obj= " + objId);
      else {
        if (debugReference) System.out.println(" Referenced object= " + dobj.who);
        ii2.setObjectNext(dobj.who);
      }
    }
    return newData;
  }

  private void addMembersToStructure(Group g, Structure s, Vinfo parentVinfo, MessageDatatype mdt) throws IOException {
    for (StructureMember m : mdt.members) {
      Variable v = makeVariableMember(g, s, m.name, m.offset, m.mdt);
      if (v != null) {
        s.addMemberVariable(v);
        if (debug1) debugOut.println("  made Member Variable " + v.getName() + "\n" + v);
      }
    }
  }

  // Used for Structure Members
  private Variable makeVariableMember(Group g, Structure s, String name, long dataPos, MessageDatatype mdt)
      throws IOException {

    Variable v;
    Vinfo vinfo = new Vinfo(mdt, null, dataPos); // LOOK need mds
    if (vinfo.getNCDataType() == null) {
      debugOut.println("SKIPPING DataType= " + vinfo.typeInfo.hdfType + " for variable " + name);
      return null;
    }

    if (mdt.type == 6) {
      v = new Structure(ncfile, g, s, name);
      makeVariableShapeAndType(v, mdt, null, vinfo, null);
      addMembersToStructure(g, (Structure) v, vinfo, mdt);
      v.setElementSize(mdt.byteSize);

    } else {
      v = new Variable(ncfile, g, s, name);
      makeVariableShapeAndType(v, mdt, null, vinfo, null);
    }

    // special case of variable length strings
    if (v.getDataType() == DataType.STRING)
      v.setElementSize(16); // because the array has elements that are HeapIdentifier

    v.setSPobject(vinfo);
    vinfo.setOwner(v);

    if (vinfo.typeInfo.unsigned)
      v.addAttribute(new Attribute("_Unsigned", "true"));

    return v;
  }

  private void processSystemAttributes(List<HeaderMessage> messages, List<Attribute> attributes) {
    for (HeaderMessage mess : messages) {
      if (mess.mtype == MessageType.LastModified) {
        MessageLastModified m = (MessageLastModified) mess.messData;
        Date d = new Date((long) m.secs * 1000);
        attributes.add(new Attribute("_lastModified", formatter.toDateTimeStringISO(d)));

      } else if (mess.mtype == MessageType.LastModifiedOld) {
        MessageLastModifiedOld m = (MessageLastModifiedOld) mess.messData;
        try {
          Date d = getHdfDateFormatter().parse(m.datemod);
          attributes.add(new Attribute("_lastModified", formatter.toDateTimeStringISO(d)));
        }
        catch (ParseException ex) {
          debugOut.println("ERROR parsing date from MessageLastModifiedOld = " + m.datemod);
        }

      } else if (mess.mtype == MessageType.Comment) {
        MessageComment m = (MessageComment) mess.messData;
        attributes.add(new Attribute("_comment", m.comment));
      }
    }
  }

  private java.text.SimpleDateFormat getHdfDateFormatter() {
    if (hdfDateParser == null) {
      hdfDateParser = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
      hdfDateParser.setTimeZone(java.util.TimeZone.getTimeZone("GMT")); // same as UTC
    }
    return hdfDateParser;
  }

  // set the type and shape of the Variable
  private boolean makeVariableShapeAndType(Variable v, MessageDatatype mdt, MessageDataspace msd, Vinfo vinfo, String dims) {

    int[] dim = (msd != null) ? msd.dimLength : new int[0];
    if (dim == null) dim = new int[0]; // scaler

    // merge the shape for array type (10)
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
      if (dims != null) { // dimensions were passed in
        if ((mdt.type == 9) && !mdt.isVString)
          v.setDimensions(dims+" *");
        else
          v.setDimensions(dims);

      } else if (mdt.type == 3) { // fixed length string - DataType.CHAR, add string length

        if (mdt.byteSize == 1) // scalar string member variable
          v.setDimensionsAnonymous(dim);
        else {
          int[] shape = new int[dim.length + 1];
          System.arraycopy(dim, 0, shape, 0, dim.length);
          shape[dim.length] = mdt.byteSize;
          v.setDimensionsAnonymous(shape);
        }

       } else if ((mdt.type == 9) && !mdt.isVString) { // variable length (not a string)

        if ((dim.length == 1) && (dim[0] == 1)) { // replace scalar with vlen
          int[] shape = new int[] {-1};
          v.setDimensionsAnonymous(shape);
        } else {                                  // add vlen dimension
          int[] shape = new int[dim.length + 1];
          System.arraycopy(dim, 0, shape, 0, dim.length);
          shape[dim.length] = -1;
          v.setDimensionsAnonymous(shape);
        }  

     /* } else if (mdt.type == 10) { // array
       v.setShapeWithAnonDimensions(mdt.dim);  */

      } else { // all other cases

        v.setDimensionsAnonymous(dim);
      }

    } catch (InvalidRangeException ee) {
      log.error(ee.getMessage());
      debugOut.println("ERROR: makeVariableShapeAndType " + ee.getMessage());
      return false;
    }

    // set the type
    DataType dt = vinfo.getNCDataType();
    if (dt == null) return false;
    v.setDataType(dt);

    // set the enumTypedef
    if (dt.isEnum()) {
      Group ncGroup = v.getParentGroup();
      EnumTypedef enumTypedef = ncGroup.findEnumeration( mdt.enumTypeName);
      if (enumTypedef == null) { // if shared object, wont have a name, shared version gets added later
        enumTypedef = new EnumTypedef( mdt.enumTypeName, mdt.map);
        // LOOK ncGroup.addEnumeration(enumTypedef);
      }
      v.setEnumTypedef(enumTypedef);
    }

    return true;
  }

  // Holder of all H5 specific information for a Variable, needed to do IO.
  class Vinfo {
    Variable owner; // debugging
    DataObjectFacade facade; // debugging

    long dataPos; // for regular variables, needs to be absolute, with baseAddress added if needed
    // for member variables, is the offset from start of structure

    TypeInfo typeInfo;
    int[] storageSize;  // for type 1 (continuous) : (varDims, elemSize)
    // for type 2 (chunked)    : (chunkDim, elemSize)
    // null for attributes

    // chunked stuff
    boolean isChunked = false;
    DataBTree btree = null; // only if isChunked

    MessageDatatype mdt;
    MessageDataspace mds;
    MessageFilter mfp;

    boolean useFillValue = false;
    byte[] fillValue;

    // Map<Integer, String> enumMap;

    /**
     * Constructor
     *
     * @param facade DataObjectFacade: always has an mdt and an msl
     * @throws java.io.IOException on read error
     */
    Vinfo(DataObjectFacade facade) throws IOException {
      this.facade = facade;
      this.dataPos = getFileOffset(facade.dobj.msl.dataAddress);
      this.mdt = facade.dobj.mdt;
      this.mds = facade.dobj.mds;
      this.mfp = facade.dobj.mfp;

      if (!facade.dobj.mdt.isOK && warnings) {
        debugOut.println("WARNING HDF5 file " + ncfile.getLocation() + " not handling " + facade.dobj.mdt);
        return; // not a supported datatype
      }

      this.isChunked = (facade.dobj.msl.type == 2);
      if (isChunked) {
        this.storageSize = facade.dobj.msl.chunkSize;
      } else {
        this.storageSize = facade.dobj.mds.dimLength;
      }

      // figure out the data type
      this.typeInfo = calcNCtype(facade.dobj.mdt);
    }

    /**
     * Constructor, used for reading attributes
     *
     * @param mdt     datatype
     * @param mds dataspace
     * @param dataPos start of data in file
     * @throws java.io.IOException on read error
     */
    Vinfo(MessageDatatype mdt, MessageDataspace mds, long dataPos) throws IOException {
      this.mdt = mdt;
      this.mds = mds;
      this.dataPos = dataPos;

      if (!mdt.isOK && warnings) {
        debugOut.println("WARNING HDF5 file " + ncfile.getLocation() + " not handling " + mdt);
        return; // not a supported datatype
      }

      // figure out the data type
      //this.hdfType = mdt.type;
      this.typeInfo = calcNCtype(mdt);
    }

    void setOwner(Variable owner) {
      this.owner = owner;
      if (btree != null) btree.setOwner(owner);
    }

    /* TypeInfo getBaseType() {
      MessageDatatype want = mdt;
      while (want.base != null) want = want.base;
      return calcNCtype(want);
    } */

    private TypeInfo calcNCtype(MessageDatatype mdt) {
      int hdfType = mdt.type;
      int byteSize = mdt.byteSize;
      byte[] flags = mdt.flags;

      TypeInfo tinfo = new TypeInfo(hdfType, byteSize);

      if (hdfType == 0) { // int, long, short, byte
        tinfo.dataType = getNCtype(hdfType, byteSize);
        tinfo.byteOrder = ((flags[0] & 1) == 0) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;
        tinfo.unsigned = ((flags[0] & 8) == 0);

      } else if (hdfType == 1) { // floats, doubles
        tinfo.dataType = getNCtype(hdfType, byteSize);
        tinfo.byteOrder = ((flags[0] & 1) == 0) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;

      } else if (hdfType == 2) { // time
        tinfo.dataType = DataType.STRING;
        tinfo.byteOrder = ((flags[0] & 1) == 0) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;

      } else if (hdfType == 3) { // fixed length strings map to CHAR. String is used for Vlen type = 1.
        tinfo.dataType = DataType.CHAR;
        tinfo.vpad = (flags[0] & 0xf);
        // when elem length = 1, there is a problem with dimensionality.
        // eg char cr(2); has a storage_size of [1,1].

      } else if (hdfType == 4) { // bit field
        tinfo.dataType = getNCtype(hdfType, byteSize);

      } else if (hdfType == 5) { // opaque
        tinfo.dataType = DataType.OPAQUE;

      } else if (hdfType == 6) { // structure
        tinfo.dataType = DataType.STRUCTURE;

      } else if (hdfType == 7) { // reference
        tinfo.byteOrder = RandomAccessFile.LITTLE_ENDIAN;
        tinfo.dataType = DataType.LONG;  // file offset of the referenced object
        // LOOK - should get the object, and change type to whatever it is (?)

      } else if (hdfType == 8) { // enums
        if (tinfo.byteSize == 1)
          tinfo.dataType = DataType.ENUM1;
        else if (tinfo.byteSize == 2)
          tinfo.dataType = DataType.ENUM2;
        else if (tinfo.byteSize == 4)
          tinfo.dataType = DataType.ENUM4;
        else {
          log.warn("Illegal byte suze for enum type = "+tinfo.byteSize);
          throw new IllegalStateException("Illegal byte suze for enum type = "+tinfo.byteSize);
        }

        // enumMap = mdt.map;

      } else if (hdfType == 9) { // variable length array
        tinfo.isVString = mdt.isVString;
        if (mdt.isVString) {
          tinfo.vpad = ((flags[0] >> 4) & 0xf);
          tinfo.dataType = DataType.STRING;
        } else {
          tinfo.dataType = getNCtype(mdt.getBaseType(), mdt.getBaseSize());
        }
      } else if (hdfType == 10) { // array : used for structure members
        tinfo.byteOrder = (mdt.getFlags()[0] & 1) == 0 ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;
        if ((mdt.base.type == 9) && mdt.base.isVString) {
          tinfo.dataType = DataType.STRING;
        } else
          tinfo.dataType = getNCtype(mdt.getBaseType(), mdt.getBaseSize());

      } else if (warnings) {
        debugOut.println("WARNING not handling hdf dataType = " + hdfType + " size= " + byteSize);
      }

      if (mdt.base != null)
        tinfo.base = calcNCtype(mdt.base);
      return tinfo;
    }

    /* private DataType getNCtype(int hdfType, int size) {
      if ((hdfType == 0) || (hdfType == 4)) { // integer, bit field
        if (size == 1)
          return DataType.BYTE;
        else if (size == 2)
          return DataType.SHORT;
        else if (size == 4)
          return DataType.INT;
        else if (size == 8)
          return DataType.LONG;
        else if (warnings) {
          debugOut.println("WARNING HDF5 file " + ncfile.getLocation() + " not handling hdf integer type (" + hdfType + ") with size= " + size);
          log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf integer type (" + hdfType + ") with size= " + size);
          return null;
        }

      } else if (hdfType == 1) {
        if (size == 4)
          return DataType.FLOAT;
        else if (size == 8)
          return DataType.DOUBLE;
        else if (warnings) {
          debugOut.println("WARNING HDF5 file " + ncfile.getLocation() + " not handling hdf float type with size= " + size);
          log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf float type with size= " + size);
          return null;
        }

      } else if (hdfType == 3) {  // fixed length strings. String is used for Vlen type = 1
        return DataType.CHAR;

      } else if (hdfType == 7) { // reference
        return DataType.LONG;

      } else if (warnings) {
        debugOut.println("WARNING not handling hdf type = " + hdfType + " size= " + size);
        log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf type = " + hdfType + " size= " + size);
      }
      return null;
    }  */

    public String toString() {
      StringBuilder buff = new StringBuilder();
      buff.append("dataPos=").append(dataPos).append(" datatype=").append(typeInfo);
      if (isChunked) {
        buff.append(" isChunked (");
        for (int size : storageSize) buff.append(size).append(" ");
        buff.append(")");
      }
      if (mfp != null) buff.append(" hasFilter");
      buff.append("; // ").append(extraInfo());
      if (null != facade)
        buff.append("\n").append(facade);

      return buff.toString();
    }

    public String extraInfo() {
      StringBuilder buff = new StringBuilder();
      if ((typeInfo.dataType != DataType.CHAR) && (typeInfo.dataType != DataType.STRING))
        buff.append(typeInfo.unsigned ? " unsigned" : " signed");
      if (typeInfo.byteOrder >= 0)
        buff.append((typeInfo.byteOrder == RandomAccessFile.LITTLE_ENDIAN) ? " LittleEndian" : " BigEndian");
      if (useFillValue)
        buff.append(" useFillValue");
      return buff.toString();
    }

    DataType getNCDataType() {
      return typeInfo.dataType;
    }

    /**
     * Get the Fill Value, return default if one was not set.
     *
     * @return wrapped primitive (Byte, Short, Integer, Double, Float, Long), or null if none
     */
    Object getFillValue() {
      return (fillValue == null) ? getFillValueDefault(typeInfo.dataType) : getFillValueNonDefault();
    }

    Object getFillValueDefault(DataType dtype) {
      if ((dtype == DataType.BYTE) || (dtype == DataType.ENUM1)) return N3iosp.NC_FILL_BYTE;
      if (dtype == DataType.CHAR) return (byte) 0;
      if ((dtype == DataType.SHORT) || (dtype == DataType.ENUM2)) return N3iosp.NC_FILL_SHORT;
      if ((dtype == DataType.INT) || (dtype == DataType.ENUM4)) return N3iosp.NC_FILL_INT;
      if (dtype == DataType.LONG) return N3iosp.NC_FILL_LONG;
      if (dtype == DataType.FLOAT) return N3iosp.NC_FILL_FLOAT;
      if (dtype == DataType.DOUBLE) return N3iosp.NC_FILL_DOUBLE;
      return null;
    }

    Object getFillValueNonDefault() {
      if (fillValue == null) return null;

      if ((typeInfo.dataType == DataType.BYTE) || (typeInfo.dataType == DataType.CHAR) || (typeInfo.dataType == DataType.ENUM1))
        return fillValue[0];

      ByteBuffer bbuff = ByteBuffer.wrap(fillValue);
      if (typeInfo.byteOrder >= 0)
        bbuff.order(typeInfo.byteOrder == RandomAccessFile.LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

      if ((typeInfo.dataType == DataType.SHORT) || (typeInfo.dataType == DataType.ENUM2))  {
        ShortBuffer tbuff = bbuff.asShortBuffer();
        return tbuff.get();

      } else if ((typeInfo.dataType == DataType.INT) || (typeInfo.dataType == DataType.ENUM4))  {
        IntBuffer tbuff = bbuff.asIntBuffer();
        return tbuff.get();

      } else if (typeInfo.dataType == DataType.LONG) {
        LongBuffer tbuff = bbuff.asLongBuffer();
        return tbuff.get();

      } else if (typeInfo.dataType == DataType.FLOAT) {
        FloatBuffer tbuff = bbuff.asFloatBuffer();
        return tbuff.get();

      } else if (typeInfo.dataType == DataType.DOUBLE) {
        DoubleBuffer tbuff = bbuff.asDoubleBuffer();
        return tbuff.get();
      }

      return null;
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
      else if (warnings) {
        debugOut.println("WARNING HDF5 file " + ncfile.getLocation() + " not handling hdf integer type (" + hdfType + ") with size= " + size);
        log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf integer type (" + hdfType + ") with size= " + size);
        return null;
      }

    } else if (hdfType == 1) {
      if (size == 4)
        return DataType.FLOAT;
      else if (size == 8)
        return DataType.DOUBLE;
      else if (warnings) {
        debugOut.println("WARNING HDF5 file " + ncfile.getLocation() + " not handling hdf float type with size= " + size);
        log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf float type with size= " + size);
        return null;
      }

    } else if (hdfType == 3) {  // fixed length strings. String is used for Vlen type = 1
      return DataType.CHAR;

    } else if (hdfType == 7) { // reference
      return DataType.LONG;

    } else if (warnings) {
      debugOut.println("WARNING not handling hdf type = " + hdfType + " size= " + size);
      log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf type = " + hdfType + " size= " + size);
    }
    return null;
  }

  class TypeInfo {
    int hdfType, byteSize;
    DataType dataType;
    int byteOrder = -1; // RandomAccessFile.LITTLE_ENDIAN || RandomAccessFile.BIG_ENDIAN
    boolean unsigned;
    boolean isVString; // is it a vlen string ?
    int vpad;          // string padding
    TypeInfo base;     // vlen, enum

    TypeInfo(int hdfType, int byteSize) {
      this.hdfType = hdfType;
      this.byteSize = byteSize;
    }

    public String toString() {
      StringBuilder buff = new StringBuilder();
      buff.append("hdfType=").append(hdfType).append(" byteSize=").append(byteSize).append(" dataType=").append(dataType);
      buff.append(" unsigned=").append(unsigned).append(" isVString=").append(isVString).append(" vpad=").append(vpad).append(" byteOrder=").append(byteOrder);
      if (base != null)
        buff.append("\n   base=").append(base);
      return buff.toString();
    }
  }

  //////////////////////////////////////////////////////////////
  // Internal organization of Data Objects

  /**
   * All access to data objects come through here, so we can cache.
   * Look in cache first; read if not in cache.
   *
   * @param address object address (aka id)
   * @param name    optional name
   * @return DataObject
   * @throws IOException on read error
   */
  private DataObject getDataObject(long address, String name) throws IOException {
    // find it
    DataObject dobj = addressMap.get(address);
    if (dobj != null) {
      if ((dobj.who == null) && name != null) dobj.who = name;
      return dobj;
    }
    // if (name == null) return null; // ??

    // read it
    dobj = new DataObject(address, name);
    addressMap.put(address, dobj); // look up by address (id)
    return dobj;
  }

  /**
   * A DataObjectFacade can be:
   * 1) a DataObject with a specific group/name.
   * 2) a SymbolicLink to a DataObject.
   * DataObjects can be pointed to from multiple places.
   * A DataObjectFacade is in a specific group and has a name specific to that group.
   * A DataObject's name is one of its names.
   */
  private class DataObjectFacade {
    H5Group parent;
    String name, displayName;
    DataObject dobj;

    boolean isGroup;
    boolean isVariable;
    boolean isTypedef;
    boolean isDimensionNotVariable;

    // is a group
    H5Group group;

    // or a variable
    String dimList;
    List<HeaderMessage> dimMessages = new ArrayList<HeaderMessage>();

    // or a link
    String linkName = null;

    DataObjectFacade(H5Group parent, String name, String linkName) {
      this.parent = parent;
      this.name = name;
      this.linkName = linkName;
    }

    DataObjectFacade(H5Group parent, String name, long address) throws IOException {
      this.parent = parent;
      this.name = name;
      displayName = (name.length() == 0) ? "root" : name;
      dobj = getDataObject(address, displayName);

      // hash for soft link lookup
      symlinkMap.put(getName(), this); // LOOK does getName() match whats stored in soft link ??

      // if has a "group message", then its a group
      if ((dobj.groupMessage != null) || (dobj.groupNewMessage != null)) { // if has a "groupNewMessage", then its a groupNew
        isGroup = true;

        // if it has a Datatype and a StorageLayout, then its a Variable
      } else if ((dobj.mdt != null) && (dobj.msl != null)) {
        isVariable = true;

        // if it has only a Datatype, its a Typedef
      } else if (dobj.mdt != null) {
        isTypedef = true;

      } else if (warnings) { // we dont know what it is
        debugOut.println("WARNING Unknown DataObjectFacade = " + this);
        return;
      }

    }

    String getName() {
      return (parent == null) ? name : parent.getName() + "/" + name;
    }

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append(getName());
      if (dobj == null) {
        sbuff.append(" dobj is NULL! ");
      } else {
        sbuff.append(" id= ").append(dobj.address);
        sbuff.append(" messages= ");
        for (HeaderMessage message : dobj.messages)
          sbuff.append("\n  ").append(message);
      }

      return sbuff.toString();
    }

  }

  private class H5Group {
    H5Group parent;
    String name, displayName;
    DataObjectFacade facade;
    List<DataObjectFacade> nestedObjects = new ArrayList<DataObjectFacade>(); // nested data objects
    Map<String, Dimension> dimMap = new HashMap<String, Dimension>();
    List<Dimension> dimList = new ArrayList<Dimension>(); // need to track dimension order

    // "Data Object Header" Level 2A
    // read a Data Object Header
    // no side effects, can be called multiple time for debugging
    private H5Group(DataObjectFacade facade) throws IOException {
      this.facade = facade;
      this.parent = facade.parent;
      this.name = facade.name;
      displayName = (name.length() == 0) ? "root" : name;

      // if has a "group message", then its an old group
      if (facade.dobj.groupMessage != null) {
        // check for hard links
        // debugOut.println("HO look for group address = "+groupMessage.btreeAddress);
        /* if (null != (group = hashGroups.get(groupMessage.btreeAddress))) {
          debugOut.println("WARNING hard link to group = " + group.getName());
          if (parent.isChildOf(group)) {
            debugOut.println("ERROR hard link to group create a loop = " + group.getName());
            group = null;
            return;
          }
        } */

        // read the group, and its contained data objects.
        readGroupOld(this, facade.dobj.groupMessage.btreeAddress, facade.dobj.groupMessage.nameHeapAddress);

      } else if (facade.dobj.groupNewMessage != null) { // if has a "groupNewMessage", then its a groupNew
        // read the group, and its contained data objects.
        readGroupNew(this, facade.dobj.groupNewMessage, facade.dobj);

      } else { // we dont know what it is
        throw new IllegalStateException("H5Group needs group messages " + facade.getName());
      }

      facade.group = this;
    }

    String getName() {
      return (parent == null) ? name : parent.getName() + "/" + name;
    }

    // is this a child of that ?
    boolean isChildOf(H5Group that) {
      if (parent == null) return false;
      if (parent == that) return true;
      return parent.isChildOf(that);
    }
  }

  //////////////////////////////////////////////////////////////
  // HDF5 primitive objects

  //////////////////////////////////////////////////////////////
  // Level 2A "data object header"

  public class DataObject implements Named {
    // debugging
    public long getAddress() {
      return address;
    }

    public String getName() {
      return who;
    }

    public List<HeaderMessage> getMessages() {
      List<HeaderMessage> result = new ArrayList<HeaderMessage>(100);
      for (HeaderMessage m : messages)
        if (!(m.messData instanceof MessageAttribute))
          result.add(m);
      return result;
    }

    public List<MessageAttribute> getAttributes() {
     /* List<MessageAttribute> result = new ArrayList<MessageAttribute>(100);
      for (HeaderMessage m : messages)
        if (m.messData instanceof MessageAttribute)
          result.add((MessageAttribute)m.messData);
      result.addAll(attributes); */
      return attributes;
    }

    long address; // aka object id : obviously unique
    String who;   // may be null, may not be unique
    List<HeaderMessage> messages = new ArrayList<HeaderMessage>();
    List<MessageAttribute> attributes = new ArrayList<MessageAttribute>();

    // need to look for these
    MessageGroup groupMessage = null;
    MessageGroupNew groupNewMessage = null;
    MessageDatatype mdt = null;
    MessageDataspace mds = null;
    MessageLayout msl = null;
    MessageFilter mfp = null;

    byte version; // 1 or 2
    //short nmess;
    //int referenceCount;
    //long headerSize;

    // "Data Object Header" Level 2A
    // read a Data Object Header
    // no side effects, can be called multiple time for debugging


    private DataObject(long address, String who) throws IOException {
      this.address = address;
      this.who = who;

      if (debug1) debugOut.println("\n--> DataObject.read parsing <" + who + "> object ID/address=" + address);
      if (debugPos)
        debugOut.println("      DataObject.read now at position=" + raf.getFilePointer() + " for <" + who + "> reposition to " + getFileOffset(address));
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
        int count = readMessagesVersion1(posMess, nmess, Integer.MAX_VALUE, this.who);
        if (debugContinueMessage) debugOut.println(" nmessages read = " + count);
        if (debugPos) debugOut.println("<--done reading messages for <" + who + ">; position=" + raf.getFilePointer());
        if (debugTracker) memTracker.addByLen("Object " + who, getFileOffset(address), headerSize + 16);

      } else { // level 2A2 (first part, before the messages)
        // first byte was already read
        byte[] name = new byte[3];
        raf.read(name);
        String magic = new String(name);
        if (!magic.equals("HDR"))
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
        int count = readMessagesVersion2(posMess, sizeOfChunk, (flags & 4) != 0, this.who);
        if (debugContinueMessage) debugOut.println(" nmessages read = " + count);
        if (debugPos) debugOut.println("<--done reading messages for <" + name + ">; position=" + raf.getFilePointer());
      }

      // look for group or a datatype/dataspace/layout message
      for (HeaderMessage mess : messages) {
        if (debugTracker) memTracker.addByLen("Message (" + who + ") " + mess.mtype, mess.start, mess.size + 8);

        if (mess.mtype == MessageType.Group)
          groupMessage = (MessageGroup) mess.messData;
        else if (mess.mtype == MessageType.GroupNew)
          groupNewMessage = (MessageGroupNew) mess.messData;
        else if (mess.mtype == MessageType.SimpleDataspace)
          mds = (MessageDataspace) mess.messData;
        else if (mess.mtype == MessageType.Datatype)
          mdt = (MessageDatatype) mess.messData;
        else if (mess.mtype == MessageType.Layout)
          msl = (MessageLayout) mess.messData;
        else if (mess.mtype == MessageType.Group)
          groupMessage = (MessageGroup) mess.messData;
        else if (mess.mtype == MessageType.FilterPipeline)
          mfp = (MessageFilter) mess.messData;
        else if (mess.mtype == MessageType.Attribute)
          attributes.add((MessageAttribute) mess.messData);
        else if (mess.mtype == MessageType.AttributeInfo)
          processAttributeInfoMessage((MessageAttributeInfo) mess.messData, attributes);
      }

      if (debug1) debugOut.println("<-- end DataObject " + who);
    }

    private void processAttributeInfoMessage(MessageAttributeInfo attInfo, List<MessageAttribute> list) throws IOException {
      long btreeAddress = (attInfo.v2BtreeAddressCreationOrder > 0) ? attInfo.v2BtreeAddressCreationOrder : attInfo.v2BtreeAddress;
      if ((btreeAddress < 0) || (attInfo.fractalHeapAddress < 0))
        return;

      BTree2 btree = new BTree2(who, btreeAddress);
      FractalHeap fractalHeap = new FractalHeap(who, attInfo.fractalHeapAddress);

      for (BTree2.Entry2 e : btree.entryList) {
        byte[] heapId = null;
        switch (btree.btreeType) {
          case 8:
            heapId = ((BTree2.Record8) e.record).heapId;
            break;
          case 9:
            heapId = ((BTree2.Record9) e.record).heapId;
            break;
          default:
            continue;
        }

        // the heapId points to an Attribute Message in the fractal Heap
        long pos = fractalHeap.getHeapId(heapId).getPos();
        if (pos > 0) {
          raf.seek(pos);
          MessageAttribute attMessage = new MessageAttribute();
          if (attMessage.read())
            list.add(attMessage);
          if (debugBtree2) System.out.println("    attMessage=" + attMessage);
        }
      }
    }

    // read messages, starting at pos, until you hit maxMess read, or maxBytes read
    // if you hit a continuation message, call recursively
    // return number of messaages read
    private int readMessagesVersion1(long pos, int maxMess, int maxBytes, String objectName) throws IOException {
      if (debugContinueMessage)
        debugOut.println(" readMessages start at =" + pos + " maxMess= " + maxMess + " maxBytes= " + maxBytes);

      int count = 0;
      int bytesRead = 0;
      while ((count < maxMess) && (bytesRead < maxBytes)) {
        /* LOOK: MessageContinue not correct ??
        if (posMess >= actualSize)
          break; */

        HeaderMessage mess = new HeaderMessage();
        //messages.add( mess);
        int n = mess.read(pos, 1, false, objectName);
        pos += n;
        bytesRead += n;
        count++;
        if (debugContinueMessage) debugOut.println("   count=" + count + " bytesRead=" + bytesRead);

        // if we hit a continuation, then we go into nested reading
        if (mess.mtype == MessageType.ObjectHeaderContinuation) {
          MessageContinue c = (MessageContinue) mess.messData;
          if (debugContinueMessage) debugOut.println(" ---ObjectHeaderContinuation--- ");
          count += readMessagesVersion1(getFileOffset(c.offset), maxMess - count, (int) c.length, objectName);
          if (debugContinueMessage) debugOut.println(" ---ObjectHeaderContinuation return --- ");
        } else if (mess.mtype != MessageType.NIL) {
          messages.add(mess);
        }
      }
      return count;
    }

    private int readMessagesVersion2(long filePos, long maxBytes, boolean creationOrderPresent, String objectName) throws IOException {
      if (debugContinueMessage)
        debugOut.println(" readMessages2 starts at =" + filePos + " maxBytes= " + maxBytes);

      // maxBytes is number of bytes of messages to be read. however, a message is at least 4 bytes long, so
      // we are done if we have read > maxBytes - 4. There appears to be an "off by one" possibility
      maxBytes -= 3;

      int count = 0;
      int bytesRead = 0;
      while (bytesRead < maxBytes) {

        HeaderMessage mess = new HeaderMessage();
        //messages.add( mess);
        int n = mess.read(filePos, 2, creationOrderPresent, objectName);
        filePos += n;
        bytesRead += n;
        count++;
        if (debugContinueMessage)
          debugOut.println("   mess size=" + n + " bytesRead=" + bytesRead + " maxBytes=" + maxBytes);

        // if we hit a continuation, then we go into nested reading
        if (mess.mtype == MessageType.ObjectHeaderContinuation) {
          MessageContinue c = (MessageContinue) mess.messData;
          long continuationBlockFilePos = getFileOffset(c.offset);
          if (debugContinueMessage)
            debugOut.println(" ---ObjectHeaderContinuation filePos= " + continuationBlockFilePos);

          raf.seek(continuationBlockFilePos);
          String sig = readStringFixedLength(4);
          if (!sig.equals("OCHK"))
            throw new IllegalStateException(" ObjectHeaderContinuation Missing signature");

          count += readMessagesVersion2(continuationBlockFilePos + 4, (int) c.length - 8, creationOrderPresent, objectName);
          if (debugContinueMessage) debugOut.println(" ---ObjectHeaderContinuation return --- ");
          if (debugContinueMessage)
            debugOut.println("   continuationMessages =" + count + " bytesRead=" + bytesRead + " maxBytes=" + maxBytes);

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

  } // DataObject

  // type safe enum
  static public class MessageType {
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

  // Header Message: Level 2A1 and 2A2 (part of Data Object)
  public class HeaderMessage implements Comparable {
    long start;
    byte headerMessageFlags;
    short type, size, header_length;
    Named messData; // header message data

    public MessageType getMtype() {
      return mtype;
    }

    public String getName() {
      return messData.getName();
    }

    public short getSize() {
      return size;
    }

    public short getType() {
      return type;
    }

    public byte getFlags() {
      return headerMessageFlags;
    }

    public long getStart() {
      return start;
    }

    MessageType mtype;

    short creationOrder = -1;

    /**
     * Read a message
     *
     * @param filePos              at this filePos
     * @param version              header version
     * @param creationOrderPresent true if bit2 of data object header flags is set
     * @return number of bytes read
     * @throws IOException of read error
     */
    int read(long filePos, int version, boolean creationOrderPresent, String objectName) throws IOException {
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
        messData = getSharedDataObject(mtype).mdt; // LOOK ??
        return header_length + size;
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
        data.read(objectName);
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
      return type - ((HeaderMessage) o).type;
    }

    public String toString() {
      return "  type = " + mtype + "; " + messData;
    }

    public void showFractalHeap(Formatter f) {
      if (mtype !=  H5header.MessageType.AttributeInfo) {
        f.format("No fractal heap");return;
      }

      MessageAttributeInfo info = (MessageAttributeInfo) messData;
      info.showFractalHeap(f);
    }

  }

  private DataObject getSharedDataObject(MessageType mtype) throws IOException {
    byte sharedVersion = raf.readByte();
    byte sharedType = raf.readByte();
    if (sharedVersion == 1) raf.skipBytes(6);
    if ((sharedVersion == 3) && (sharedType == 1)) {
      long heapId = raf.readLong();
      if (debug1)
        debugOut.println("     Shared Message " + sharedVersion + " type=" + sharedType + " heapId = " + heapId);
      if (debugPos) debugOut.println("  --> Shared Message reposition to =" + raf.getFilePointer());
      // dunno where is the file's shared object header heap ??
      throw new UnsupportedOperationException("****SHARED MESSAGE type = " + mtype + " heapId = " + heapId);

    } else {
      long address = readOffset();
      if (debug1)
        debugOut.println("     Shared Message " + sharedVersion + " type=" + sharedType + " address = " + address);
      DataObject dobj = getDataObject(address, null);
      if (null == dobj)
        throw new IllegalStateException("cant find data object at" + address);
      if (mtype == MessageType.Datatype) {
        return dobj;
      }
      throw new UnsupportedOperationException("****SHARED MESSAGE type = " + mtype);
    }
  }

  abstract interface Named {
    String getName();
  }

  // Message Type 1 : "Simple Dataspace" = dimension list / shape
  public class MessageDataspace implements Named {
    byte ndims, flags;
    byte type;  // 0	A scalar dataspace, i.e. a dataspace with a single, dimensionless element.
                // 1	A simple dataspace, i.e. a dataspace with a a rank > 0 and an appropriate # of dimensions.
                // 2	A null dataspace, i.e. a dataspace with no elements.
    int[] dimLength, maxLength; // , permute;
    // boolean isPermuted;

    public String getName() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("(");
      for (int size : dimLength) sbuff.append(size).append(",");
      sbuff.append(")");
      return sbuff.toString();
    }

    public String toString() {
      Formatter sbuff = new Formatter();
      sbuff.format(" ndims=%d flags=%x type=%d ", ndims, flags, type);
      sbuff.format(" length=(");
      for (int size : dimLength) sbuff.format("%d,", size);
      sbuff.format(") max=(");
      for (int aMaxLength : maxLength) sbuff.format("%d,", aMaxLength);
      sbuff.format(")");
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

      if (debug1) {
        for (int i = 0; i < ndims; i++)
          debugOut.println("    dim length = " + dimLength[i] + " max = " + maxLength[i]);
      }
    }
  }

  // Message Type 17/0x11 "Old Group" or "Symbol Table"
  private class MessageGroup implements Named {
    long btreeAddress, nameHeapAddress;

    void read() throws IOException {
      btreeAddress = readOffset();
      nameHeapAddress = readOffset();
      if (debug1) debugOut.println("   Group btreeAddress=" + btreeAddress + " nameHeapAddress=" + nameHeapAddress);
    }

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append(" btreeAddress=").append(btreeAddress);
      sbuff.append(" nameHeapAddress=").append(nameHeapAddress);
      return sbuff.toString();
    }

    public String getName() {
      return Long.toString(btreeAddress);
    }

  }

  // Message Type 2 "New Group" or "Link Info" (version 2)
  private class MessageGroupNew implements Named {
    byte version, flags;
    long maxCreationIndex = -2, fractalHeapAddress, v2BtreeAddress, v2BtreeAddressCreationOrder = -2;

    public String toString() {
      Formatter f = new Formatter();
      f.format("   GroupNew fractalHeapAddress=%d v2BtreeAddress=%d ", fractalHeapAddress, v2BtreeAddress);
      if (v2BtreeAddressCreationOrder > -2)
        f.format(" v2BtreeAddressCreationOrder=%d ", v2BtreeAddressCreationOrder);
      if (maxCreationIndex > -2)
        f.format(" maxCreationIndex=%d", maxCreationIndex);
      f.format(" %n%n");

      if (fractalHeapAddress > 0) {
        try {
          f.format("\n\n");
          FractalHeap fractalHeap = new FractalHeap("", fractalHeapAddress);
          fractalHeap.showDetails(f);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      return f.toString();
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

    public String getName() {
      return Long.toString(fractalHeapAddress);
    }

  }

  // Message Type 10/0xA "Group Info" (version 2)
  private class MessageGroupInfo implements Named {
    byte flags;
    short maxCompactValue = -1, minDenseValue = -1, estNumEntries = -1, estLengthEntryName = -1;

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("   MessageGroupInfo ");
      if ((flags & 1) != 0)
        sbuff.append(" maxCompactValue=").append(maxCompactValue).append(" minDenseValue=").append(minDenseValue);
      if ((flags & 2) != 0)
        sbuff.append(" estNumEntries=").append(estNumEntries).append(" estLengthEntryName=").append(estLengthEntryName);
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

    public String getName() {
      return "";
    }
  }

  // Message Type 6 "Link" (version 2)
  private class MessageLink implements Named {
    byte version, flags, encoding;
    byte linkType; // 0=hard, 1=soft, 64 = external
    long creationOrder;
    String linkName, link;
    long linkAddress;

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("   MessageLink ");
      sbuff.append(" name=").append(linkName).append(" type=").append(linkType);
      if (linkType == 0)
        sbuff.append(" linkAddress=" + linkAddress);
      else
        sbuff.append(" link=").append(link);

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
      linkName = readStringFixedLength(linkNameLength);

      if (linkType == 0) {
        linkAddress = readOffset();

      } else if (linkType == 1) {
        short len = raf.readShort();
        link = readStringFixedLength(len);

      } else if (linkType == 64) {
        short len = raf.readShort();
        link = readStringFixedLength(len); // actually 2 strings - see docs
      }

      if (debug1)
        debugOut.println("   MessageLink version= " + version + " flags = " + Integer.toBinaryString(flags) + this);
    }

    public String getName() {
      return linkName;
    }
  }

  // Message Type 3 : "Datatype"
  public class MessageDatatype implements Named {
    int type, version;
    byte[] flags = new byte[3];
    int byteSize, byteOrder;
    boolean isOK = true;
    boolean unsigned;

    // time (2)
    DataType timeType;

    // opaque (5)
    String opaque_desc;

    // compound type (6)
    short nmembers;
    List<StructureMember> members;

    // reference (7)
    int referenceType; // 0 = object, 1 = region

    // enums (8)
    Map<Integer, String> map;
    String enumTypeName;

    // enum, variable-length, array types have "base" DataType
    MessageDatatype base;
    boolean isVString; // variable length (not a string)

    // array (10)
    int[] dim;

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append(" datatype= ").append(type);
      sbuff.append(" byteSize= ").append(byteSize);
      DataType dtype = getNCtype(type, byteSize);
      sbuff.append(" NCtype= ").append(dtype);
      sbuff.append(" flags= ");
      for (int i=0; i<3; i++) sbuff.append(flags[i]).append(" ");
      if (type == 2)
        sbuff.append(" timeType= ").append(timeType);
      else if (type == 6)
        sbuff.append(" nmembers= ").append(nmembers);
      else if (type == 7)
        sbuff.append(" referenceType= ").append(referenceType);
      else if (type == 9)
        sbuff.append(" isVString= ").append(isVString);
      if ((type == 9) || (type == 10))
        sbuff.append(" parent= ").append(base);
      return sbuff.toString();
    }

    public String getName() {
      DataType dtype = getNCtype(type, byteSize);
      if (dtype != null)
        return dtype.toString() + " size= "+byteSize;
      else
        return "type="+Integer.toString(type) + " size= "+byteSize;
    }

    void read(String objectName) throws IOException {
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
        unsigned = ((flags[0] & 8) == 0);
        short bitOffset = raf.readShort();
        short bitPrecision = raf.readShort();
        if (debug1)
          debugOut.println("   type 0 (fixed point): bitOffset= " + bitOffset + " bitPrecision= " + bitPrecision+ " unsigned= " + unsigned);
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
        if (bitPrecision == 16)
          timeType = DataType.SHORT;
        else if (bitPrecision == 32)
          timeType = DataType.INT;
        else if (bitPrecision == 64)
          timeType = DataType.LONG;

        if (debug1)
          debugOut.println("   type 2 (time): bitPrecision= " + bitPrecision + " timeType = " + timeType);

      } else if (type == 3) { // string
        int ptype = flags[0] & 0xf;
        if (debug1)
          debugOut.println("   type 3 (String): pad type= " + ptype);

      } else if (type == 4) { // bit field
        short bitOffset = raf.readShort();
        short bitPrecision = raf.readShort();
        if (debug1)
          debugOut.println("   type 4 (bit field): bitOffset= " + bitOffset + " bitPrecision= " + bitPrecision);
        //isOK = (bitOffset == 0) && (bitPrecision % 8 == 0);  LOOK

      } else if (type == 5) { // opaque
        byte len = flags[0];
        opaque_desc = (len > 0) ? readString(raf).trim() : null;
        if (debug1) debugOut.println("   type 5 (opaque): len= " + len + " desc= " + opaque_desc);

      } else if (type == 6) { // compound
        int nmembers = makeUnsignedIntFromBytes(flags[1], flags[0]);
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
        int nmembers = makeUnsignedIntFromBytes(flags[1], flags[0]);
        boolean saveDebugDetail = debugDetail;
        if (debug1 || debugEnum) {
          debugOut.println("   --type 8(enums): nmembers=" + nmembers);
          debugDetail = true;
        }
        base = new MessageDatatype(); // base type
        base.read(objectName);
        debugDetail = saveDebugDetail;

        // read the enums

        String[] enumName = new String[nmembers];
        for (int i = 0; i < nmembers; i++) {
          if (version < 3)
            enumName[i] = readString8(raf); //padding
          else
            enumName[i] = readString(raf); // no padding
        }

        // read the values; must switch to base byte order (!)
        if (base.byteOrder >= 0) raf.order(base.byteOrder);
        int[] enumValue = new int[nmembers];
        for (int i = 0; i < nmembers; i++)
          enumValue[i] = readVariableSize(base.byteSize); // assume size is 1, 2, or 4
        raf.order(RandomAccessFile.LITTLE_ENDIAN);

        enumTypeName = objectName;
        map = new TreeMap<Integer, String>();
        for (int i = 0; i < nmembers; i++)
          map.put(enumValue[i], enumName[i]);

        if (debugEnum) {
          for (int i = 0; i < nmembers; i++)
            debugOut.println("   " + enumValue[i] + "=" + enumName[i]);
        }

      } else if (type == 9) { // variable-length
        isVString = (flags[0] & 0xf) == 1;
        if (debug1)
          debugOut.println("   type 9(variable length): type= " + (isVString ? "string" : "sequence of type:"));
        base = new MessageDatatype(); // base type
        base.read(objectName);

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

        base = new MessageDatatype(); // base type
        base.read(objectName);

      } else if (warnings) {
        debugOut.println(" WARNING not dealing with type= " + type);
      }
    }

    int getBaseType() {
      return (base != null) ? base.getBaseType() : type;
    }

    int getBaseSize() {
      return (base != null) ? base.getBaseSize() : byteSize;
    }

    byte[] getFlags() {
      return (base != null) ? base.getFlags() : flags;
    }
  }

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
      mdt.read( name);
      if (debugDetail) debugOut.println("   ***End Member name=" + name);

      // ??
      //HDFdump(ncfile.out, "Member end", buffer, 16);
      //if (HDFdebug)  ncfile.debugOut.println("   Member pos="+raf.getFilePointer());
      //HDFpadToMultiple( buffer, 8);
      //if (HDFdebug)  ncfile.debugOut.println("   Member padToMultiple="+raf.getFilePointer());
      //raf.skipBytes( 4); // huh ??
    }
  }

  // Message Type 4 "Fill Value Old" : fill value is stored in the message
  private class MessageFillValueOld implements Named {
    byte[] value;
    int size;

    void read() throws IOException {
      size = raf.readInt();
      value = new byte[size];
      raf.read(value);

      if (debug1) debugOut.println(this);
    }

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("   FillValueOld size= ").append(size).append(" value=");
      for (int i = 0; i < size; i++) sbuff.append(" ").append(value[i]);
      return sbuff.toString();
    }

    public String getName() {
      StringBuilder sbuff = new StringBuilder();
      for (int i = 0; i < size; i++) sbuff.append(" ").append(value[i]);
      return sbuff.toString();
    }
  }

  // Message Type 5 "Fill Value New" : fill value is stored in the message, with extra metadata
  private class MessageFillValue implements Named {
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
        hasFillValue = raf.readByte() != 0;

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
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("   FillValue version= ").append(version).append(" spaceAllocateTime = ").append(spaceAllocateTime).append(" fillWriteTime=").append(fillWriteTime).append(" hasFillValue= ").append(hasFillValue);
      sbuff.append("\n size = ").append(size).append(" value=");
      for (int i = 0; i < size; i++) sbuff.append(" ").append(value[i]);
      return sbuff.toString();
    }

     public String getName() {
      StringBuilder sbuff = new StringBuilder();
      for (int i = 0; i < size; i++) sbuff.append(" ").append(value[i]);
      return sbuff.toString();
    }

  }

  // Message Type 8 "Data Storage Layout" : regular (contiguous), chunked, or compact (stored with the message)
  private class MessageLayout implements Named {
    byte type; // 0 = Compact, 1 = Contiguous, 2 = Chunked
    long dataAddress = -1; // -1 means "not allocated"
    long contiguousSize; // size of data allocated contiguous
    int[] chunkSize;  // only for chunked, otherwise must use Dataspace
    int dataSize;

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append(" type= ").append(+type).append(" (");
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
          sbuff.append("unknown type= ").append(type);
      }
      sbuff.append(")");

      if (chunkSize != null) {
        sbuff.append(" storageSize = (");
        for (int i = 0; i < chunkSize.length; i++) {
          if (i > 0) sbuff.append(",");
          sbuff.append(chunkSize[i]);
        }
        sbuff.append(")");
      }

      sbuff.append(" dataSize=").append(dataSize);
      sbuff.append(" dataAddress=").append(dataAddress);
      return sbuff.toString();
    }

    public String getName() {
      StringBuilder sbuff = new StringBuilder();
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
          sbuff.append("unknown type= ").append(type);
      }

      if (chunkSize != null) {
        sbuff.append(" chunk = (");
        for (int i = 0; i < chunkSize.length; i++) {
          if (i > 0) sbuff.append(",");
          sbuff.append(chunkSize[i]);
        }
        sbuff.append(")");
      }

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
          dataSize = raf.readInt();
          dataAddress = raf.getFilePointer();
        }

      } else {
        type = raf.readByte();

        if (type == 0) {
          dataSize = raf.readShort();
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

  // Message Type 11/0xB "Filter Pipeline" : apply a filter to the "data stream"
  class MessageFilter implements Named {
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
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("   MessageFilter filters=\n");
      for (Filter f : filters)
        sbuff.append(" ").append(f).append("\n");
      return sbuff.toString();
    }

    public String getName() {
      StringBuilder sbuff = new StringBuilder();
      for (Filter f : filters)
        sbuff.append(f.name).append(", ");
      return sbuff.toString();
    }
  }

  private String[] filterName = new String[] {"", "deflate", "shuffle", "fletcher32", "szip", "nbit", "scaleoffset"};
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
        this.name = (nameSize > 0) ? readString8(raf) : getFilterName(id); // null terminated, pad to 8 bytes
      else
        this.name = (nameSize > 0) ? readStringFixedLength(nameSize) : getFilterName(id); // non-null terminated

      data = new int[nValues];
      for (int i = 0; i < nValues; i++)
        data[i] = raf.readInt();
      if (nValues % 2 == 1)
        raf.skipBytes(4);

      if (debug1) debugOut.println(this);
    }

    String getFilterName(int id) {
      return (id < filterName.length) ? filterName[id] : "StandardFilter " + id;
    }

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("   Filter id= ").append(id).append(" flags = ").append(flags).append(" nValues=").append(nValues).append(" name= ").append(name).append(" data = ");
      for (int i = 0; i < nValues; i++)
        sbuff.append(data[i]).append(" ");
      return sbuff.toString();
    }
  }

  // Message Type 12/0xC "Attribute" : define an Atribute
  public class MessageAttribute implements Named {
    byte version;
    //short typeSize, spaceSize;
    String name;
    MessageDatatype mdt = new MessageDatatype();
    MessageDataspace mds = new MessageDataspace();

    public byte getVersion() {
      return version;
    }

    public MessageDatatype getMdt() {
      return mdt;
    }

    public MessageDataspace getMds() {
      return mds;
    }

    public long getDataPos() {
      return dataPos;
    }

    long dataPos; // pointer to the attribute data section, must be absolute file position

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("   Name= ").append(name);
      sbuff.append(" dataPos = ").append(dataPos);
      if (mdt != null) {
        sbuff.append("\n mdt=");
        sbuff.append(mdt.toString());
      }
      if (mds != null) {
        sbuff.append("\n mds=");
        sbuff.append(mds.toString());
      }
      return sbuff.toString();
    }

    public String getName() {
      return name;
    }

    boolean read() throws IOException {
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
        log.error("bad version "+version+" at filePos " + raf.getFilePointer()); // buggery, may be HDF5 "more than 8 attributes" error
        return false;
        // throw new IllegalStateException("MessageAttribute unknown version " + version);
      }

      // read the attribute name
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
      boolean isShared = (flags & 1) != 0;
      if (isShared) {
        mdt = getSharedDataObject(MessageType.Datatype).mdt;
        if (debug1) debugOut.println("    MessageDatatype: " + mdt);
      } else {
        mdt.read( name);
        if (version == 1) typeSize += padding(typeSize, 8);
      }
      raf.seek(filePos + typeSize); // make it more robust for errors

      // read the dataspace
      filePos = raf.getFilePointer();
      if (debugPos) debugOut.println("   *MessageAttribute before mds = " + filePos);
      mds.read();
      if (version == 1) spaceSize += padding(spaceSize, 8);
      raf.seek(filePos + spaceSize); // make it more robust for errors

      // heres where the data starts
      dataPos = raf.getFilePointer();
      if (debug1) debugOut.println("   *MessageAttribute dataPos= " + dataPos);
      return true;
    }
  }

  // Message Type 21/0x15 "Attribute Info" (version 2)
  private class MessageAttributeInfo implements Named {
    byte version, flags;
    short maxCreationIndex = -1;
    long fractalHeapAddress = -2, v2BtreeAddress = -2, v2BtreeAddressCreationOrder = -2;

    public String getName() {
      long btreeAddress = (v2BtreeAddressCreationOrder > 0) ? v2BtreeAddressCreationOrder : v2BtreeAddress;
      return Long.toString(btreeAddress);
    }

    public String toString() {
      Formatter f = new Formatter();
      f.format("   MessageAttributeInfo ");
      if ((flags & 1) != 0)
        f.format(" maxCreationIndex=" + maxCreationIndex);
      f.format(" fractalHeapAddress=%d v2BtreeAddress=%d", fractalHeapAddress, v2BtreeAddress);
      if ((flags & 2) != 0)
        f.format(" v2BtreeAddressCreationOrder=%d",v2BtreeAddressCreationOrder);

      showFractalHeap(f);

      return f.toString();
    }

    void showFractalHeap(Formatter f) {
      long btreeAddress = (v2BtreeAddressCreationOrder > 0) ? v2BtreeAddressCreationOrder : v2BtreeAddress;
      if ((fractalHeapAddress > 0) && (btreeAddress > 0)) {
        try {
          FractalHeap fractalHeap = new FractalHeap("", fractalHeapAddress);
          fractalHeap.showDetails(f);

          f.format(" Btree:%n");
          f.format("  type n m  offset size pos       attName%n");

          BTree2 btree = new BTree2("", btreeAddress);
          for (BTree2.Entry2 e : btree.entryList) {
            byte[] heapId = null;
            switch (btree.btreeType) {
              case 8:
                heapId = ((BTree2.Record8) e.record).heapId;
                break;
              case 9:
                heapId = ((BTree2.Record9) e.record).heapId;
                break;
              default:
                f.format(" unknown btreetype %d\n", btree.btreeType);
                continue;
            }

            // the heapId points to an Attribute Message in the fractal Heap
            FractalHeap.DHeapId dh = fractalHeap.getHeapId(heapId);
            f.format("   %2d %2d %2d %6d %4d %8d",  dh.type, dh.n, dh.m, dh.offset, dh.size, dh.getPos());
            if (dh.getPos() > 0) {
              raf.seek(dh.getPos());
              MessageAttribute attMessage = new MessageAttribute();
              attMessage.read();
              f.format(" %-30s", trunc(attMessage.getName(), 30));
            }
            f.format(" heapId=:");
            showBytes(heapId, f);
            f.format("%n");
          }

        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    String trunc(String s, int max) {
      if (s == null) return null;
      if (s.length() < max) return s;
      return s.substring(0, max);
    }

    void read() throws IOException {
      if (debugPos) debugOut.println("   *MessageAttributeInfo start pos= " + raf.getFilePointer());
      byte version = raf.readByte();
      byte flags = raf.readByte();
      if ((flags & 1) != 0)
        maxCreationIndex = raf.readShort();

      fractalHeapAddress = readOffset();
      v2BtreeAddress = readOffset();

      if ((flags & 2) != 0)
        v2BtreeAddressCreationOrder = readOffset();

      if (debug1) debugOut.println("   MessageAttributeInfo version= " + version + " flags = " + flags + this);
    }
  }

  // Message Type 13/0xD ("Object Comment" : "short description of an Object"
  private class MessageComment implements Named {
    String comment;

    void read() throws IOException {
      comment = readString(raf);
    }

    public String toString() {
      return comment;
    }

    public String getName() {
      return comment;
    }

  }

  // Message Type 18/0x12 "Last Modified" : last modified date represented as secs since 1970
  private class MessageLastModified implements Named {
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

    public String getName() {
      return toString();
    }
  }

  // Message Type 14/0xE ("Last Modified (old)" : last modified date represented as a String YYMM etc. use message type 18 instead
  private class MessageLastModifiedOld implements Named {
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

    public String getName() {
      return toString();
    }
  }

  // Message Type 16/0x10 "Continue" : point to more messages
  private class MessageContinue implements Named {
    long offset, length;

    void read() throws IOException {
      offset = readOffset();
      length = readLength();
      if (debug1) debugOut.println("   Continue offset=" + offset + " length=" + length);
    }

    public String getName() {
      return "";
    }
  }

  // Message Type 22/0x11 Object Reference COunt
  private class MessageObjectReferenceCount implements Named {
    int refCount;

    void read() throws IOException {
      int version = raf.readByte();
      refCount = raf.readInt();
      if (debug1) debugOut.println("   ObjectReferenceCount=" + refCount);
    }

    public String getName() {
      return Integer.toString(refCount);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  // Groups

  private void readGroupNew(H5Group group, MessageGroupNew groupNewMessage, DataObject dobj) throws IOException {
    if (debug1) debugOut.println("\n--> GroupNew read <" + group.displayName + ">");

    if (groupNewMessage.fractalHeapAddress >= 0) {
      FractalHeap fractalHeap = new FractalHeap(group.displayName, groupNewMessage.fractalHeapAddress);

      long btreeAddress = (groupNewMessage.v2BtreeAddressCreationOrder >= 0) ?
          groupNewMessage.v2BtreeAddressCreationOrder : groupNewMessage.v2BtreeAddress;
      if (btreeAddress < 0) throw new IllegalStateException("no valid btree for GroupNew with Fractal Heap");

      // read in btree and all entries
      BTree2 btree = new BTree2(group.displayName, btreeAddress);
      for (BTree2.Entry2 e : btree.entryList) {
        byte[] heapId = null;
        switch (btree.btreeType) {
          case 5:
            heapId = ((BTree2.Record5) e.record).heapId;
            break;
          case 6:
            heapId = ((BTree2.Record6) e.record).heapId;
            break;
          default:
            continue;
        }

        // the heapId points to a Linkmessage in the Fractal Heap
        long pos = fractalHeap.getHeapId(heapId).getPos();
        if (pos < 0) continue;
        raf.seek(pos);
        MessageLink linkMessage = new MessageLink();
        linkMessage.read();
        if (debugBtree2) System.out.println("    linkMessage=" + linkMessage);

        group.nestedObjects.add(new DataObjectFacade(group, linkMessage.linkName, linkMessage.linkAddress));
      }

    } else {
      // look for link messages
      for (HeaderMessage mess : dobj.messages) {
        if (mess.mtype == MessageType.Link) {
          MessageLink linkMessage = (MessageLink) mess.messData;
          if (linkMessage.linkType == 0) {
            group.nestedObjects.add(new DataObjectFacade(group, linkMessage.linkName, linkMessage.linkAddress));
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
    if (debug1) debugOut.println("<-- end GroupNew read <" + group.displayName + ">");
  }

  private void readGroupOld(H5Group group, long btreeAddress, long nameHeapAddress) throws IOException {
    // track by address for hard links
    //hashGroups.put(btreeAddress, this);

    if (debug1) debugOut.println("\n--> GroupOld read <" + group.displayName + ">");
    LocalHeap nameHeap = new LocalHeap(group, nameHeapAddress);
    GroupBTree btree = new GroupBTree(group.displayName, btreeAddress);

    // now read all the entries in the btree : Level 1C
    for (SymbolTableEntry s : btree.getSymbolTableEntries()) {
      String sname = nameHeap.getString((int) s.getNameOffset());
      if (debugSoftLink) debugOut.println("\n   Symbol name=" + sname);

      if (s.cacheType == 2) {
        String linkName = nameHeap.getString(s.linkOffset);
        if (debugSoftLink) debugOut.println("   Symbolic link name=" + linkName);
        group.nestedObjects.add(new DataObjectFacade(group, sname, linkName));
      } else {
        group.nestedObjects.add(new DataObjectFacade(group, sname, s.getObjectAddress()));
      }
    }
    if (debug1) debugOut.println("<-- end GroupOld read <" + group.displayName + ">");
  }

  // Level 1A
  // this just reads in all the entries into a list
  private class GroupBTree {
    protected String owner;
    protected int wantType = 0;
    private List<SymbolTableEntry> sentries = new ArrayList<SymbolTableEntry>(); // list of type SymbolTableEntry

    // for DataBTree
    GroupBTree(String owner) {
      this.owner = owner;
    }

    GroupBTree(String owner, long address) throws IOException {
      this.owner = owner;

      List<Entry> entryList = new ArrayList<Entry>();
      readAllEntries(address, entryList);

      // now convert the entries to SymbolTableEntry
      for (Entry e : entryList) {
        GroupNode node = new GroupNode(e.address);
        sentries.addAll(node.getSymbols());
      }
    }

    List<SymbolTableEntry> getSymbolTableEntries() {
      return sentries;
    }

    // recursively read all entries, place them in order in list
    protected void readAllEntries(long address, List<Entry> entryList) throws IOException {
      raf.seek(getFileOffset(address));
      if (debugGroupBtree) debugOut.println("\n--> GroupBTree read tree at position=" + raf.getFilePointer());

      byte[] name = new byte[4];
      raf.read(name);
      String magic = new String(name);
      if (!magic.equals("TREE"))
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
      if (debugGroupBtree)
        debugOut.println("    leftAddress=" + leftAddress + " " + Long.toHexString(leftAddress) +
            " rightAddress=" + rightAddress + " " + Long.toHexString(rightAddress));

      // read all entries in this Btree "Node"
      List<Entry> myEntries = new ArrayList<Entry>();
      for (int i = 0; i < nentries; i++) {
        myEntries.add(new Entry());
      }

      if (level == 0)
        entryList.addAll(myEntries);
      else {
        for (Entry entry : myEntries) {
          if (debugDataBtree) debugOut.println("  nonzero node entry at =" + entry.address);
          readAllEntries(entry.address, entryList);
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
        String magic = new String(sig);
        if (!magic.equals("SNOD"))
          throw new IllegalStateException(magic + " should equal SNOD");

        version = raf.readByte();
        raf.readByte(); // skip byte
        nentries = raf.readShort();
        if (debugDetail) debugOut.println("   version=" + version + " nentries=" + nentries);

        long posEntry = raf.getFilePointer();
        for (int i = 0; i < nentries; i++) {
          SymbolTableEntry entry = new SymbolTableEntry(posEntry);
          posEntry += entry.getSize();
          if (entry.objectHeaderAddress != 0)  { // LOOK: Probably a bug in HDF5 file format ?? jc July 16 2010
            if (debug1) debugOut.printf("   add %s%n", entry);
            symbols.add(entry);
        }  else {
            if (debug1) debugOut.printf("   BAD objectHeaderAddress==0 !! %s%n", entry);
          }
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
      return isOffsetLong ? 40 : 32;
    }

    public long getObjectAddress() {
      return objectHeaderAddress;
    }

    public long getNameOffset() {
      return nameOffset;
    }

    @Override
    public String toString() {
      return "SymbolTableEntry{" +
              "nameOffset=" + nameOffset +
              ", objectHeaderAddress=" + objectHeaderAddress +
              ", btreeAddress=" + btreeAddress +
              ", nameHeapAddress=" + nameHeapAddress +
              ", cacheType=" + cacheType +
              ", linkOffset=" + linkOffset +
              ", posData=" + posData +
              ", isSymbolicLink=" + isSymbolicLink +
              '}';
    }
  } // SymbolTableEntry

  // Level 1A2
  private class BTree2 {
    private String owner;
    private byte btreeType;
    private int nodeSize; // size in bytes of btree nodes
    private short recordSize; // size in bytes of btree records
    private short numRecordsRootNode;

    private List<Entry2> entryList = new ArrayList<Entry2>();

    BTree2(String owner, long address) throws IOException {
      this.owner = owner;

      raf.seek(getFileOffset(address));

      // header
      byte[] heapname = new byte[4];
      raf.read(heapname);
      String magic = new String(heapname);
      if (!magic.equals("BTHD"))
        throw new IllegalStateException(magic + " should equal BTHD");

      byte version = raf.readByte();
      btreeType = raf.readByte();
      nodeSize = raf.readInt();
      recordSize = raf.readShort();
      short treeDepth = raf.readShort();
      byte split = raf.readByte();
      byte merge = raf.readByte();
      long rootNodeAddress = readOffset();
      numRecordsRootNode = raf.readShort();
      long totalRecords = readLength(); // total in entire btree
      int checksum = raf.readInt();

      if (debugBtree2) {
        debugOut.println("BTree2 version=" + version + " type=" + btreeType + " treeDepth=" + treeDepth);
        debugOut.println(" nodeSize=" + nodeSize + " recordSize=" + recordSize + " numRecordsRootNode="
            + numRecordsRootNode + " totalRecords=" + totalRecords + " rootNodeAddress=" + rootNodeAddress);
      }

      if (treeDepth > 0) {
        InternalNode node = new InternalNode(rootNodeAddress, numRecordsRootNode, recordSize, treeDepth);
        node.recurse();
      } else {
        LeafNode leaf = new LeafNode(rootNodeAddress, numRecordsRootNode);
        leaf.addEntries(entryList);
      }
    }

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
        String magic = new String(sig);
        if (!magic.equals("BTIN"))
          throw new IllegalStateException(magic + " should equal BTIN");

        byte version = raf.readByte();
        byte nodeType = raf.readByte();
        if (nodeType != btreeType)
          throw new IllegalStateException();

        if (debugBtree2)
          debugOut.println("   BTree2 InternalNode version=" + version + " type=" + nodeType + " nrecords=" + nrecords);

        entries = new Entry2[nrecords + 1]; // did i mention theres actually n+1 children?
        for (int i = 0; i < nrecords; i++) {
          entries[i] = new Entry2();
          entries[i].record = readRecord(btreeType);
        }
        entries[nrecords] = new Entry2();

        int maxNumRecords = nodeSize / recordSize; // LOOK ?? guessing
        int maxNumRecordsPlusDesc = nodeSize / recordSize; // LOOK ?? guessing
        for (int i = 0; i < nrecords + 1; i++) {
          Entry2 e = entries[i];
          e.childAddress = readOffset();
          e.nrecords = readVariableSizeUnsigned(1); // readVariableSizeMax(maxNumRecords);
          if (depth > 1)
            e.totNrecords = readVariableSizeUnsigned(2); // readVariableSizeMax(maxNumRecordsPlusDesc);

          if (debugBtree2)
            debugOut.println(" BTree2 entry childAddress=" + e.childAddress + " nrecords=" + e.nrecords + " totNrecords=" + e.totNrecords);
        }

        int checksum = raf.readInt();
      }

      void recurse() throws IOException {
        for (Entry2 e : entries) {
          if (depth > 1) {
            InternalNode node = new InternalNode(e.childAddress, (short) e.nrecords, recordSize, depth - 1);
            node.recurse();
          } else {
            long nrecs = e.nrecords;
            LeafNode leaf = new LeafNode(e.childAddress, (short) nrecs);
            leaf.addEntries(entryList);
          }
          if (e.record != null) // last one is null
            entryList.add(e);
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
        String magic = new String(sig);
        if (!magic.equals("BTLF"))
          throw new IllegalStateException(magic + " should equal BTLF");

        byte version = raf.readByte();
        byte nodeType = raf.readByte();
        if (nodeType != btreeType)
          throw new IllegalStateException();

        if (debugBtree2)
          debugOut.println("   BTree2 LeafNode version=" + version + " type=" + nodeType + " nrecords=" + nrecords);

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

        if (debugBtree2)
          debugOut.println("  record5 nameHash=" + nameHash + " heapId=" + showBytes(heapId));
      }
    }

    class Record6 {
      long creationOrder;
      byte[] heapId = new byte[7];

      Record6() throws IOException {
        creationOrder = raf.readLong();
        raf.read(heapId);
        if (debugBtree2)
          debugOut.println("  record6 creationOrder=" + creationOrder + " heapId=" + showBytes(heapId));
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
      byte[] heapId = new byte[8];

      Record8() throws IOException {
        raf.read(heapId);
        flags = raf.readByte();
        creationOrder = raf.readInt();
        nameHash = raf.readInt();
        if (debugBtree2)
          debugOut.println("  record8 creationOrder=" + creationOrder + " heapId=" + showBytes(heapId));
      }
    }

    class Record9 {
      byte flags;
      int creationOrder;
      byte[] heapId = new byte[8];

      Record9() throws IOException {
        raf.read(heapId);
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

    LayoutTiled.DataChunkIterator getDataChunkIterator2(Section want, int nChunkDim) throws IOException {
      return new DataChunkIterator2(want, nChunkDim);
    }

    // An Iterator over the DataChunks in the btree.
    // returns only the actual data from the btree leaf (level 0) nodes.
    class DataChunkIterator2 implements LayoutTiled.DataChunkIterator {
      private Node root;
      private int nChunkDim;

      /**
       * Constructor
       * @param want skip any nodes that are before this section
       * @param nChunkDim number of chunk dimensions - may be less than the offset[] length
       * @throws IOException on error
       */
      DataChunkIterator2(Section want, int nChunkDim) throws IOException {
        this.nChunkDim = nChunkDim;
        root = new Node(rootNodeAddress, -1); // should we cache the nodes ???
        int[] wantOrigin = (want != null) ? want.getOrigin() : null;
        root.first(wantOrigin);
      }

      public boolean hasNext() {
        return root.hasNext(); //  && !node.greaterThan(wantOrigin);
      }

      public LayoutTiled.DataChunk next() throws IOException {
        DataChunk dc = root.next();
        int[] offset = dc.offset;
        if (offset.length > nChunkDim) { // may have to eliminate last offset
          offset = new int[nChunkDim];
          System.arraycopy(dc.offset, 0, offset, 0, nChunkDim);
        }
        return new LayoutTiled.DataChunk( offset, dc.filePos);
      }
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
            " owner= " + owner.getNameAndDimensions());

        raf.order(RandomAccessFile.LITTLE_ENDIAN); // header information is in le byte order
        raf.seek(getFileOffset(address));
        this.address = address;

        byte[] name = new byte[4];
        raf.read(name);
        String magic = new String(name);
        if (!magic.equals("TREE"))
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
          // note nentries-1 - assume dont skip the last one
          for (currentEntry = 0; currentEntry < nentries-1; currentEntry++) {
            DataChunk entry = myEntries.get(currentEntry + 1);
            if ((wantOrigin == null) || tiling.compare(wantOrigin, entry.offset) < 0) break;
          }

        } else {
          currentNode = null;
          for (currentEntry = 0; currentEntry < nentries; currentEntry++) {
            if ((wantOrigin == null) || tiling.compare(wantOrigin, offset[currentEntry + 1]) < 0) {
              currentNode = new Node(childPointer[currentEntry], this.address);
              currentNode.first(wantOrigin);
              break;
            }
          }

          // heres the case where its the last entry we want; the tiling.compare() above may fail
          if (currentNode == null) {
            currentEntry = nentries-1;
            currentNode = new Node(childPointer[currentEntry], this.address);
            currentNode.first(wantOrigin);
          }
        }

        //if (currentEntry >= nentries)
        //  System.out.println("hah");
        assert (nentries == 0) || (currentEntry < nentries) : currentEntry +" >= "+ nentries;
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

    /* private void dump(DataType dt, List<DataChunk> entries) {
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
    } */

    // these are part of the level 1A data structure, type 1
    // see "Key" field (type 1) p 10
    // this is only for level 0

    class DataChunk {
      int size; // size of chunk in bytes; need storage layout dimensions to interpret
      int filterMask; // bitfield indicating which filters have been skipped for this chunk
      int[] offset; // offset index of this chunk, reletive to entire array
      long filePos; // filePos of a single raw data chunk

      DataChunk(int ndim, boolean last) throws IOException {
        this.size = raf.readInt();
        this.filterMask = raf.readInt();
        offset = new int[ndim];
        for (int i = 0; i < ndim; i++) {
          long loffset = raf.readLong();
          assert loffset < Integer.MAX_VALUE;
          offset[i] = (int) loffset;
        }
        this.filePos = last ? -1 : getFileOffset(readOffset());
        if (debugTracker) memTracker.addByLen("Chunked Data (" + owner + ")", filePos, size);
      }

      public String toString() {
        StringBuilder sbuff = new StringBuilder();
        sbuff.append("  ChunkedDataNode size=").append(size).append(" filterMask=").append(filterMask).append(" filePos=").append(filePos).append(" offsets= ");
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
  // Heaps

  /**
   * Fetch a Vlen data array.
   *
   * @param globalHeapIdAddress address of the heapId, used to get the String out of the heap
   * @param dataType type of data
   * @param byteOrder byteOrder of the data
   * @return String the String read from the heap
   * @throws IOException on read error
   */
  Array getHeapDataArray(long globalHeapIdAddress, DataType dataType, int byteOrder) throws IOException {
    HeapIdentifier heapId = new HeapIdentifier(globalHeapIdAddress);
    if (debugHeap) debugOut.println(" heapId= " + heapId);
    GlobalHeap.HeapObject ho = heapId.getHeapObject();
    if (debugHeap) debugOut.println(" HeapObject= " + ho);
    if (byteOrder >= 0) raf.order(byteOrder);

    if (DataType.FLOAT == dataType) {
      float[] pa = new float[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readFloat(pa, 0, pa.length);
      return Array.factory(dataType.getPrimitiveClassType(), new int[]{pa.length}, pa);

    } else if (DataType.DOUBLE == dataType) {
      double[] pa = new double[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readDouble(pa, 0, pa.length);
      return Array.factory(dataType.getPrimitiveClassType(), new int[]{pa.length}, pa);

    } else if (DataType.BYTE == dataType) {
      byte[] pa = new byte[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.read(pa, 0, pa.length);
      return Array.factory(dataType.getPrimitiveClassType(), new int[]{pa.length}, pa);

    } else if (DataType.SHORT == dataType) {
      short[] pa = new short[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readShort(pa, 0, pa.length);
      return Array.factory(dataType.getPrimitiveClassType(), new int[]{pa.length}, pa);

    } else if (DataType.INT == dataType) {
      int[] pa = new int[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readInt(pa, 0, pa.length);
      return Array.factory(dataType.getPrimitiveClassType(), new int[]{pa.length}, pa);

    } else if (DataType.LONG == dataType) {
      long[] pa = new long[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readLong(pa, 0, pa.length);
      return Array.factory(dataType.getPrimitiveClassType(), new int[]{pa.length}, pa);
    }

    throw new UnsupportedOperationException("getHeapDataAsArray dataType=" + dataType);
  }

  /**
   * Fetch a String from the heap.
   *
   * @param heapIdAddress address of the heapId, used to get the String out of the heap
   * @return String the String read from the heap
   * @throws IOException on read error
   */
  String readHeapString(long heapIdAddress) throws IOException {
    H5header.HeapIdentifier heapId = new HeapIdentifier(heapIdAddress);
    H5header.GlobalHeap.HeapObject ho = heapId.getHeapObject();
    raf.seek(ho.dataPos);
    return readStringFixedLength((int) ho.dataSize);
  }

  /**
   * Fetch a String from the heap, when the heap identifier has already beed read into a ByteBuffer at given pos
   * @param bb heap id is here
   * @param pos at this position
   * @return String the String read from the heap
   * @throws IOException on read error
   */
  String readHeapString(ByteBuffer bb, int pos) throws IOException {
    H5header.HeapIdentifier heapId = new HeapIdentifier(bb, pos);
    H5header.GlobalHeap.HeapObject ho = heapId.getHeapObject();
    raf.seek(ho.dataPos);
    return readStringFixedLength((int) ho.dataSize);
  }

  // debug - hdf5Table
  public List<DataObject> getDataObjects() {
    ArrayList<DataObject> result = new ArrayList<DataObject>(addressMap.values());
    Collections.sort( result, new java.util.Comparator<DataObject>() {
      public int compare(DataObject o1, DataObject o2) {
        // return (int) (o1.address - o2.address);
        return (o1.address<o2.address ? -1 : (o1.address==o2.address ? 0 : 1));
      }
    });
    return result;
  }

  /**
   * Get a data object's name, using the objectId your get from a reference (aka hard link).
   *
   * @param objId address of the data object
   * @return String the data object's name, or null if not found
   * @throws IOException on read error
   */
  String getDataObjectName(long objId) throws IOException {
    H5header.DataObject dobj = getDataObject(objId, null);
    if (dobj == null) {
      log.error("H5iosp.readVlenData cant find dataObject id= " + objId);
      return null;
    } else {
      if (debugVlen) System.out.println(" Referenced object= " + dobj.who);
      return dobj.who;
    }
  }

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

    // the heap id is has already beed read into a byte array at given pos
    HeapIdentifier(ByteBuffer bb, int pos) throws IOException {
      bb.order(ByteOrder.LITTLE_ENDIAN); // header information is in le byte order
      bb.position(pos); // reletive reading
      nelems = bb.getInt();
      heapAddress = isOffsetLong ? bb.getLong() : (long) bb.getInt();
      index = bb.getInt();
      if (debugDetail)
        debugOut.println("   read HeapIdentifier from ByteBuffer=" + this);
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

  private class RegionReference {
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
          if (debugRegionReference) System.out.println(" found ho=" + ho);
          /* - The offset of the object header of the object (ie. dataset) pointed to (yes, an object ID)
- A serialized form of a dataspace _selection_ of elements (in the dataset pointed to).
I don't have a formal description of this information now, but it's encoded in the H5S_<foo>_serialize() routines in
src/H5S<foo>.c, where foo = {all, hyper, point, none}.
There is _no_ datatype information stored for these sort of selections currently. */
          raf.seek(ho.dataPos);
          long objId = raf.readLong();
          DataObject ndo = getDataObject(objId, null);
          // String what = (ndo == null) ? "none" : ndo.getName();
          if (debugRegionReference) System.out.println(" objId=" + objId + " DataObject= " + ndo);
          if (null == ndo)
            throw new IllegalStateException("cant find data object at" + objId);
          return;
        }
      }
      throw new IllegalStateException("cant find HeapObject");
    }

  } // RegionReference

  // level 1E
  private class GlobalHeap {
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
      String magic = new String(heapname);
      if (!magic.equals("GCOL"))
        throw new IllegalStateException(magic + " should equal GCOL");

      version = raf.readByte();
      raf.skipBytes(3);
      size = raf.readInt();
      if (debugDetail)
        debugOut.println("-- readGlobalHeap address=" + address + " version= " + version + " size = " + size);
      raf.skipBytes(4); // pad to 8

      int count = 0;
      while (count + 16 < size) {  // guess that there must be room for a global heap object and some data.
                                  // see globalHeapOverrun in netcdf4 test directory
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
    H5Group group;
    int size;
    long freelistOffset, dataAddress;
    byte[] heap;
    byte version;

    LocalHeap(H5Group group, long address) throws IOException {
      this.group = group;

      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(getFileOffset(address));

      if (debugDetail) debugOut.println("-- readLocalHeap position=" + raf.getFilePointer());

      // header
      byte[] heapname = new byte[4];
      raf.read(heapname);
      String magic = new String(heapname);
      if (!magic.equals("HEAP"))
        throw new IllegalStateException(magic + " should equal HEAP");

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
      if (debugTracker) memTracker.addByLen("Group LocalHeap (" + group.displayName + ")", address, hsize);
      if (debugTracker) memTracker.addByLen("Group LocalHeapData (" + group.displayName + ")", dataAddress, size);
    }

    public String getString(int offset) {
      int count = 0;
      while (heap[offset + count] != 0) count++;
      try {
        return new String(heap, offset, count, utf8CharsetName);
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException(e.getMessage());
      }
    }

  } // LocalHeap

  // level 1E "Fractal Heap" used for both Global and Local heaps in 1.8.0+
  /*
  1) the root indirect block knows how many rows it has from the header, which i can divide into
direct and indirect using:

 int maxrows_directBlocks = (log2(maxDirectBlockSize) - log2(startingBlockSize)) + 2;

in the example file i have, maxDirectBlockSize = 216, startingBlockSize = 2^10, tableWidth = 4, so
maxrows = 8. So I will see 8 rows, with direct sizes:
	2^10, 2^10, 2^11, 2^12, 2^13, 2^14, 2^15, 2^16

So if nrows > 8, I will see indirect rows of size
	2^17, 2^18, .....

this value is the <indirect block size>.

2) now read a 1st level indirect block of size 217:

<iblock_nrows> = lg2(<indirect block size>) - lg2(<starting block size) - lg2(<doubling_table_width>)) + 1

<iblock_nrows> = 17 - 10 - 2 + 1 = 6.

 All indirect blocks of "size" 2^17 will have: (for the parameters above)
        row 0: (direct blocks): 4 x 2^10 = 2^12
        row 1: (direct blocks): 4 x 2^10 = 2^12
        row 2: (direct blocks): 4 x 2^11 = 2^13
        row 3: (direct blocks): 4 x 2^12 = 2^14
        row 4: (direct blocks): 4 x 2^13 = 2^15
        row 5: (direct blocks): 4 x 2^14 = 2^16
                    ===============
                       Total size: 2^17

Then there are 7 rows for indirect block of size 218, 8 rows for indirect block of size 219, etc.
An indirect block of size 2^20 will have nine rows, the last one of which are indirect blocks that are size 2^17,
an indirect block of size 2^21 will have ten rows, the last two rows of which are indirect blocks that are size
2^17 & 2^18, etc.

One still uses

 int maxrows_directBlocks = (log2(maxDirectBlockSize) - log2(startingBlockSize)) + 2

Where startingBlockSize is from the header, ie the same for all indirect blocks.


*/
  private class FractalHeap {
    int version;
    short heapIdLen;
    byte flags;
    int maxSizeOfObjects;
    long nextHugeObjectId, freeSpace, managedSpace, allocatedManagedSpace, offsetDirectBlock,
        nManagedObjects, sizeHugeObjects, nHugeObjects, sizeTinyObjects, nTinyObjects;
    long btreeAddress, freeSpaceTrackerAddress;

    short maxHeapSize, startingNumRows, currentNumRows;
    long maxDirectBlockSize;
    short tableWidth;
    long startingBlockSize;

    long rootBlockAddress;
    IndirectBlock rootBlock;

    // filters
    short ioFilterLen;
    long sizeFilteredRootDirectBlock;
    int ioFilterMask;
    byte[] ioFilterInfo;

    DoublingTable doublingTable;

    FractalHeap(String forWho, long address) throws IOException {

      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(getFileOffset(address));

      if (debugDetail) debugOut.println("-- readFractalHeap position=" + raf.getFilePointer());

      // header
      byte[] heapname = new byte[4];
      raf.read(heapname);
      String magic = new String(heapname);
      if (!magic.equals("FRHP"))
        throw new IllegalStateException(magic + " should equal FRHP");

      version = raf.readByte();
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
      offsetDirectBlock = readLength(); // linear heap offset where next direct block should be allocated
      nManagedObjects = readLength();  // number of managed objects in the heap
      sizeHugeObjects = readLength(); // total size of huge objects in the heap (in bytes)
      nHugeObjects = readLength(); // number huge objects in the heap
      sizeTinyObjects = readLength(); // total size of tiny objects packed in heap Ids (in bytes)
      nTinyObjects = readLength(); // number of tiny objects packed in heap Ids

      tableWidth = raf.readShort(); // number of columns in the doubling table for managed blocks, must be power of 2
      startingBlockSize = readLength(); // starting direct block size in bytes, must be power of 2
      maxDirectBlockSize = readLength(); // maximum direct block size in bytes, must be power of 2
      maxHeapSize = raf.readShort(); // log2 of the maximum size of heap's linear address space, in bytes
      startingNumRows = raf.readShort(); // starting number of rows of the root indirect block, 0 = maximum needed
      rootBlockAddress = readOffset(); // can be undefined if no data
      currentNumRows = raf.readShort(); // current number of rows of the root indirect block, 0 = direct block

      boolean hasFilters = (ioFilterLen > 0);
      if (hasFilters) {
        sizeFilteredRootDirectBlock = readLength();
        ioFilterMask = raf.readInt();
        ioFilterInfo = new byte[ioFilterLen];
        raf.read(ioFilterInfo);
      }
      int checksum = raf.readInt();

      if (debugDetail || debugFractalHeap) {
        debugOut.println("FractalHeap for "+ forWho+" version=" + version + " heapIdLen=" + heapIdLen + " ioFilterLen=" + ioFilterLen + " flags= " + flags);
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
      if (debugTracker) memTracker.add("Group FractalHeap (" + forWho + ")", address, pos);

      doublingTable = new DoublingTable(tableWidth, startingBlockSize, allocatedManagedSpace, maxDirectBlockSize);

      // data
      rootBlock = new IndirectBlock(currentNumRows, startingBlockSize);

      if (currentNumRows == 0) {
        DataBlock dblock = new DataBlock();
        doublingTable.blockList.add(dblock);
        readDirectBlock(getFileOffset(rootBlockAddress), address, dblock);
        dblock.size = startingBlockSize - dblock.extraBytes;
        rootBlock.add( dblock);

      } else {

        readIndirectBlock(rootBlock, getFileOffset(rootBlockAddress), address, hasFilters);

        // read in the direct blocks
        for (DataBlock dblock : doublingTable.blockList) {
          if (dblock.address > 0) {
            readDirectBlock(getFileOffset(dblock.address), address, dblock);
            dblock.size -= dblock.extraBytes;
          }
        }
      }

    }

    void showDetails(Formatter f) {
      f.format("FractalHeap version=" + version + " heapIdLen=" + heapIdLen + " ioFilterLen=" + ioFilterLen + " flags= " + flags+"\n");
      f.format(" maxSizeOfObjects=" + maxSizeOfObjects + " nextHugeObjectId=" + nextHugeObjectId + " btreeAddress="
          + btreeAddress + " managedSpace=" + managedSpace + " allocatedManagedSpace=" + allocatedManagedSpace + " freeSpace=" + freeSpace+"\n");
      f.format(" nManagedObjects=" + nManagedObjects + " nHugeObjects= " + nHugeObjects + " nTinyObjects=" + nTinyObjects +
          " maxDirectBlockSize=" + maxDirectBlockSize + " maxHeapSize= 2^" + maxHeapSize+"\n");
      f.format(" rootBlockAddress=" + rootBlockAddress + " startingNumRows=" + startingNumRows + " currentNumRows=" + currentNumRows+"\n\n");
      rootBlock.showDetails(f);
      // doublingTable.showDetails(f);
  }


    DHeapId getHeapId(byte[] heapId) throws IOException {
      return new DHeapId(heapId);
    }

    private class DHeapId {
      int type;
      int n,m;
      int offset;
      int size;

      DHeapId (byte[] heapId) throws IOException {
        type = (heapId[0] & 0x30) >> 4;
        n = maxHeapSize / 8;
        m = getNumBytesFromMax(maxDirectBlockSize - 1);

        offset = makeIntFromBytes(heapId, 1, n);
        size = makeIntFromBytes(heapId, 1 + n, m);
        // System.out.println("Heap id =" + showBytes(heapId) + " type = " + type + " n= " + n + " m= " + m + " offset= " + offset + " size= " + size);
      }

      long getPos() {
        return doublingTable.getPos(offset);
      }

      public String toString() {
        return type+" "+n+" "+m+" "+offset+" "+size;
      }
    }

    private class DoublingTable {
      int tableWidth;
      long startingBlockSize, managedSpace, maxDirectBlockSize;
      // int nrows, nDirectRows, nIndirectRows;
      List<DataBlock> blockList;

      DoublingTable(int tableWidth, long startingBlockSize, long managedSpace, long maxDirectBlockSize ) {
        this.tableWidth = tableWidth;
        this.startingBlockSize = startingBlockSize;
        this.managedSpace = managedSpace;
        this.maxDirectBlockSize = maxDirectBlockSize;

        /* nrows = calcNrows(managedSpace);
        int maxDirectRows = calcNrows(maxDirectBlockSize);
        if (nrows > maxDirectRows) {
          nDirectRows = maxDirectRows;
          nIndirectRows = nrows - maxDirectRows;
        } else {
          nDirectRows = nrows;
          nIndirectRows = 0;
        } */

        blockList = new ArrayList<DataBlock>(tableWidth * currentNumRows);
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

      private void assignSizes() {
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
          if (db.address < 0) continue;
          if ((offset >= db.offset) && (offset < db.offset + db.size)) {
            long localOffset = offset - db.offset;
            //System.out.println("   heap ID find block= "+block+" db.dataPos " + db.dataPos+" localOffset= "+localOffset);
            return db.dataPos + localOffset;
          }
          block++;
        }

        log.error("DoublingTable: illegal offset=" + offset);
        return -1; // LOOK temporary skip
        // throw new IllegalStateException("offset=" + offset);
      }

    void showDetails(Formatter f) {
      f.format(" DoublingTable: tableWidth= %d startingBlockSize = %d managedSpace=%d maxDirectBlockSize=%d%n",
              tableWidth ,startingBlockSize, managedSpace, maxDirectBlockSize);
      //sbuff.append(" nrows=" + nrows + " nDirectRows=" + nDirectRows + " nIndirectRows=" + nIndirectRows+"\n");
      f.format(" DataBlocks:\n");
      f.format("  address            dataPos            offset size\n");
      for (DataBlock dblock : blockList) {
        f.format("  %#-18x %#-18x %5d  %4d%n", dblock.address, dblock.dataPos, dblock.offset, dblock.size);
      }
    }
  }

    private class IndirectBlock {
      long size;
      int nrows, directRows, indirectRows;
      List<DataBlock> directBlocks;
      List<IndirectBlock> indirectBlocks;

      IndirectBlock(int nrows, long iblock_size ) {
        this.nrows = nrows;
        this.size = iblock_size;

        if (nrows < 0) {
          double n = SpecialMathFunction.log2(iblock_size) - SpecialMathFunction.log2(startingBlockSize*tableWidth) + 1;
          nrows = (int) n;
        }

        int maxrows_directBlocks = (int) (SpecialMathFunction.log2(maxDirectBlockSize) - SpecialMathFunction.log2(startingBlockSize)) + 2;
        if (nrows < maxrows_directBlocks) {
          directRows = nrows;
          indirectRows = 0;
        } else {
          directRows = maxrows_directBlocks;
          indirectRows = (nrows - maxrows_directBlocks);
        }
        if (debugFractalHeap)
          debugOut.println("  readIndirectBlock directChildren" + directRows + " indirectChildren= " + indirectRows);
      }

      void add(DataBlock dblock) {
        if (directBlocks == null)
          directBlocks = new ArrayList<DataBlock>();
        directBlocks.add(dblock);
      }

      void add(IndirectBlock iblock) {
        if (indirectBlocks == null)
          indirectBlocks = new ArrayList<IndirectBlock>();
        indirectBlocks.add(iblock);
      }

      void showDetails(Formatter f) {
        f.format("%n IndirectBlock: nrows= %d directRows = %d indirectRows=%d startingSize=%d%n", 
                nrows, directRows, indirectRows, size);
        //sbuff.append(" nrows=" + nrows + " nDirectRows=" + nDirectRows + " nIndirectRows=" + nIndirectRows+"\n");
        f.format(" DataBlocks:\n");
        f.format("  address            dataPos            offset size end\n");
        if (directBlocks != null)
          for (DataBlock dblock : directBlocks)
            f.format("  %#-18x %#-18x %5d  %4d %5d %n", dblock.address, dblock.dataPos, dblock.offset, dblock.size,
                    (dblock.offset + dblock.size));
        if (indirectBlocks != null)
          for (IndirectBlock iblock : indirectBlocks)
            iblock.showDetails(f);
      }
    }

    private class DataBlock {
      long address;
      long sizeFilteredDirectBlock;
      int filterMask;

      long dataPos;
      long offset;
      long size;
      int extraBytes;
    }

    void readIndirectBlock(IndirectBlock iblock, long pos, long heapAddress, boolean hasFilter) throws IOException {
      raf.seek(pos);

      // header
      byte[] heapname = new byte[4];
      raf.read(heapname);
      String magic = new String(heapname);
      if (!magic.equals("FHIB"))
        throw new IllegalStateException(magic + " should equal FHIB");

      byte version = raf.readByte();
      long heapHeaderAddress = readOffset();
      if (heapAddress != heapHeaderAddress)
        throw new IllegalStateException();

      int nbytes = maxHeapSize / 8;
      if (maxHeapSize % 8 != 0) nbytes++;
      long blockOffset = readVariableSizeUnsigned(nbytes);

      if (debugDetail || debugFractalHeap) {
        debugOut.println(" -- FH IndirectBlock version=" + version + " blockOffset= " + blockOffset);
      }

      long npos = raf.getFilePointer();
      if (debugPos) debugOut.println("    *now at position=" + npos);

      // child direct blocks
      long blockSize = startingBlockSize;
      for (int row=0; row < iblock.directRows; row++) {

        if (row > 1)
          blockSize *= 2;

        for (int i = 0; i < doublingTable.tableWidth; i++) {
          DataBlock directBlock = new DataBlock();
          iblock.add(directBlock);

          directBlock.address = readOffset();
          if (hasFilter) {
            directBlock.sizeFilteredDirectBlock = readLength();
            directBlock.filterMask = raf.readInt();
          }
          if (debugDetail || debugFractalHeap)
            debugOut.println("  DirectChild " + i + " address= " + directBlock.address);

          directBlock.size = blockSize;

          //if (directChild.address >= 0)
            doublingTable.blockList.add(directBlock);
        }
      }

      // child indirect blocks
      for (int row = 0; row < iblock.indirectRows; row++) {
        blockSize *= 2;
        for (int i = 0; i < doublingTable.tableWidth; i++) {
          IndirectBlock iblock2 = new IndirectBlock(-1, blockSize);
          iblock.add(iblock2);

          long childIndirectAddress = readOffset();
          if (debugDetail || debugFractalHeap)
            debugOut.println("  InDirectChild " + row + " address= " + childIndirectAddress);
          if (childIndirectAddress >= 0)
            readIndirectBlock(iblock2, childIndirectAddress, heapAddress, hasFilter);
        }
      }

    }

    void readDirectBlock(long pos, long heapAddress, DataBlock dblock) throws IOException {
      raf.seek(pos);

      // header
      byte[] heapname = new byte[4];
      raf.read(heapname);
      String magic = new String(heapname);
      if (!magic.equals("FHDB"))
        throw new IllegalStateException(magic + " should equal FHDB");

      byte version = raf.readByte();
      long heapHeaderAddress = readOffset();
      if (heapAddress != heapHeaderAddress)
        throw new IllegalStateException();

      dblock.extraBytes = 5; // keep track of how much room is taken out of blocak size
      dblock.extraBytes += isOffsetLong ? 8 : 4;

      int nbytes = maxHeapSize / 8;
      if (maxHeapSize % 8 != 0) nbytes++;
      dblock.offset = readVariableSizeUnsigned(nbytes);
      dblock.dataPos = pos; // raf.getFilePointer();  // offsets are from the start of the block

      dblock.extraBytes += nbytes;
      if ((flags & 2) != 0) dblock.extraBytes += 4; // ?? size of checksum
      //dblock.size -= size; // subtract space used by other fields

      if (debugDetail || debugFractalHeap)
        debugOut.println("  DirectBlock offset= " + dblock.offset + " dataPos = " + dblock.dataPos);
    }

  } // FractalHeap

  //////////////////////////////////////////////////////////////
  // utilities

  private int makeIntFromBytes(byte[] bb, int start, int n) {
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
  private String readString(RandomAccessFile raf) throws IOException {
    long filePos = raf.getFilePointer();

    int count = 0;
    while (raf.readByte() != 0) count++;

    raf.seek(filePos);
    byte[] s = new byte[count];
    raf.read(s);
    raf.readByte(); // skip the zero byte! nn
    return new String(s, utf8CharsetName); // all Strings are UTF-8 unicode
  }

  /**
   * Read a zero terminated String at current position; advance file to a multiple of 8.
   *
   * @param raf from this file
   * @return String (dont include zero terminator)
   * @throws java.io.IOException on io error
   */
  private String readString8(RandomAccessFile raf) throws IOException {
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

    try {
      return new String(s, utf8CharsetName); // all Strings are UTF-8 unicode
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  /**
   * Read a String of known length.
   *
   * @param size number of bytes
   * @return String result
   * @throws java.io.IOException on io error
   */
  private String readStringFixedLength(int size) throws IOException {
    byte[] s = new byte[size];
    raf.read(s);
    return new String(s, utf8CharsetName); // all Strings are UTF-8 unicode
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
    return readVariableSizeUnsigned(size);
  }

  private long readVariableSizeFactor(int sizeFactor) throws IOException {
    int size = (int) Math.pow(2, sizeFactor);
    return readVariableSizeUnsigned(size);
  }

  private long readVariableSizeUnsigned(int size) throws IOException {
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
      vv = readVariableSizeN(size);
    }
    return vv;
  }

  private int readVariableSize(int size) throws IOException {
    long vv;
    if (size == 1) {
      return raf.readByte();
    } else if (size == 2) {
      return raf.readShort();
    } else if (size == 4) {
      return raf.readInt();
    }
    throw new IllegalArgumentException("Dont support int size == "+size);
  }

  // Little endian
  private long readVariableSizeN(int nbytes) throws IOException {
    int[] ch = new int[nbytes];
    for (int i = 0; i < nbytes; i++)
      ch[i] = raf.read();

    long result = ch[nbytes - 1];
    for (int i = nbytes - 2; i >= 0; i--) {
      result = result << 8;
      result += ch[i];
    }

    return result;
  }

  private long getFileOffset(long address) throws IOException {
    return baseAddress + address;
  }

  private int makeUnsignedIntFromBytes(byte upper, byte lower) {
    return ucar.ma2.DataType.unsignedByteToShort(upper) * 256 + ucar.ma2.DataType.unsignedByteToShort(lower);
  }

  // find number of bytes needed to pad to multipleOf byte boundary
  private int padding(int nbytes, int multipleOf) {
    int pad = nbytes % multipleOf;
    if (pad != 0) pad = multipleOf - pad;
    return pad;
  }

  void dump(String head, long filePos, int nbytes, boolean count) throws IOException {
    long savePos = raf.getFilePointer();
    if (filePos >= 0) raf.seek(filePos);
    byte[] mess = new byte[nbytes];
    raf.read(mess);
    printBytes(head, mess, nbytes, false, debugOut);
    raf.seek(savePos);
  }

  static public String showBytes(byte[] buff) {
    StringBuilder sbuff = new StringBuilder();
    for (int i = 0; i < buff.length; i++) {
      byte b = buff[i];
      int ub = (b < 0) ? b + 256 : b;
      if (i > 0) sbuff.append(" ");
      sbuff.append(ub);
    }
    return sbuff.toString();
  }

  static public void showBytes(byte[] buff, Formatter f) {
    for (int i = 0; i < buff.length; i++) {
      byte b = buff[i];
      int ub = (b < 0) ? b + 256 : b;
      f.format("%3d ", ub);
    }
  }

  static void printBytes(String head, byte[] buff, int n, boolean count, java.io.PrintStream ps) {
    ps.print(head + " == ");
    for (int i = 0; i < n; i++) {
      byte b = buff[i];
      int ub = (b < 0) ? b + 256 : b;
      if (count) ps.print(i + ":");
      ps.print(ub);
      if (!count) {
        ps.print("(");
        ps.print(b);
        ps.print(")");
      }
      ps.print(" ");
    }
    ps.println();
  }

  static void printBytes(String head, byte[] buff, int offset, int n, java.io.PrintStream ps) {
    ps.print(head + " == ");
    for (int i = 0; i < n; i++) {
      byte b = buff[offset + i];
      int ub = (b < 0) ? b + 256 : b;
      ps.print(ub);
      ps.print(" ");
    }
    ps.println();
  }

  public void close() {
    if (debugTracker) memTracker.report();
  }

  private class MemTracker {
    private List<Mem> memList = new ArrayList<Mem>();
    private StringBuilder sbuff = new StringBuilder();

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