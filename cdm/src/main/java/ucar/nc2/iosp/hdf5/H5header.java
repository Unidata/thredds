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
import ucar.nc2.Enumeration;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.nc2.iosp.LayoutTiled;
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
class H5header {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5header.class);
  static private String utf8CharsetName = "UTF-8";
  static private Charset utf8Charset = Charset.forName(utf8CharsetName); // cant use until 1.6

  // debugging
  static private boolean debugEnum = false, debugVlen = false;
  static private boolean debug1 = false, debugDetail = false, debugPos = false, debugHeap = false, debugV = false;
  static private boolean debugGroupBtree = false, debugDataBtree = false, debugDataChunk = false, debugBtree2 = false;
  static private boolean debugContinueMessage = false, debugTracker = false, debugSoftLink = false, debugSymbolTable = false;
  static private boolean warnings = false, debugReference = false, debugRegionReference = false, debugCreationOrder = false, debugFractalHeap = false;
  static java.io.PrintStream debugOut = System.out;

  static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debug1 = debugFlag.isSet("H5header/header");
    debugBtree2 = debugFlag.isSet("H5header/btree2");
    debugContinueMessage = debugFlag.isSet("H5header/continueMessage");
    debugDetail = debugFlag.isSet("H5header/headerDetails");
    debugDataBtree = debugFlag.isSet("H5header/dataBtree");
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

  private long actualSize, baseAddress;
  private byte sizeOffsets, sizeLengths;
  private boolean isOffsetLong, isLengthLong;

  private H5Group rootGroup;
  private Map<String, DataObjectFacade> symlinkMap = new HashMap<String, DataObjectFacade>(200);
  private Map<Long, DataObject> addressMap = new HashMap<Long, DataObject>(200);

  private Map<Long, GlobalHeap> heapMap = new HashMap<Long, GlobalHeap>();
  private Map<Long, H5Group> hashGroups = new HashMap<Long, H5Group>(100);

  DateFormatter formatter = new DateFormatter();
  private java.text.SimpleDateFormat hdfDateParser;
  private MemTracker memTracker;

  H5header(RandomAccessFile myRaf, ucar.nc2.NetcdfFile ncfile, H5iosp h5iosp) {
    this.ncfile = ncfile;
    this.raf = myRaf;
    this.h5iosp = h5iosp;
  }

  void read() throws IOException {
    actualSize = raf.length();
    memTracker = new MemTracker(actualSize);

    if (!isValidFile(raf))
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

    // create group attributes
    filterAttributes(h5group.facade.dobj.attributes);
    for (MessageAttribute matt : h5group.facade.dobj.attributes) {
      makeAttributes(h5group.name, null, matt, ncGroup.getAttributes());
    }

    // add system attributes
    processSystemAttributes(h5group.facade.dobj.messages, ncGroup.getAttributes());

    // look for dimension scales
    for (DataObjectFacade facade : h5group.nestedObjects) {
      if (facade.isVariable)
        findDimensionScales(ncGroup, h5group, facade);
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

          Vinfo vinfo = (Vinfo) v.getSPobject();
          if (debugV) debugOut.println("  made Variable " + v.getName() + "  vinfo= " + vinfo + "\n" + v);
        }

      } else if (facadeNested.isTypedef) {
        if (debugReference && facadeNested.dobj.mdt.type == 7) debugOut.println(facadeNested);

        if (facadeNested.dobj.mdt.map != null)
          ncGroup.addEnumeration(new Enumeration(facadeNested.name, facadeNested.dobj.mdt.map));
        if (debugV) debugOut.println("  made enumeration " + facadeNested.name);

      } else if (!facadeNested.isDimensionNotVariable && warnings) {
        debugOut.println("WARN:  DataObject ndo " + facadeNested + " not a Group or a Variable");
      }

    } // loop over nested objects

  }

  private void findDimensionScales(ucar.nc2.Group g, H5Group h5group, DataObjectFacade facade) throws IOException {
    // first must look for coordinate variables (dimension scales)
    Iterator<MessageAttribute> iter = facade.dobj.attributes.iterator();
    while (iter.hasNext()) {
      MessageAttribute matt = iter.next();
      if (matt.name.equals("CLASS")) {
        Attribute att = makeAttribute(facade.name, matt.name, matt.mdt, matt.mds, matt.dataPos);
        String val = att.getStringValue();
        if (val.equals("DIMENSION_SCALE")) {
          String dimName = addDimension(g, h5group, facade.name, facade.dobj.mds.dimLength[0], facade.dobj.mds.maxLength[0] == -1);
          facade.dimList = dimName;
          iter.remove();
        }
      }
    }

    // now look for dimension lists and clean up the attributes
    iter = facade.dobj.attributes.iterator();
    while (iter.hasNext()) {
      MessageAttribute matt = iter.next();
      // find the dimensions - set length to maximum
      if (matt.name.equals("DIMENSION_LIST")) {
        Attribute att = makeAttribute(facade.name, matt.name, matt.mdt, matt.mds, matt.dataPos);
        StringBuffer sbuff = new StringBuffer();
        for (int i = 0; i < att.getLength(); i++) {
          String name = att.getStringValue(i);
          String dimName = addDimension(g, h5group, name, facade.dobj.mds.dimLength[i], facade.dobj.mds.maxLength[i] == -1);
          sbuff.append(dimName).append(" ");
        }
        facade.dimList = sbuff.toString();
        iter.remove();

      } else if (matt.name.equals("NAME")) {
        Attribute att = makeAttribute(facade.name, matt.name, matt.mdt, matt.mds, matt.dataPos);
        String val = att.getStringValue();
        if (val.startsWith("This is a netCDF dimension but not a netCDF variable")) {
          facade.isVariable = false;
          facade.isDimensionNotVariable = true;
        }
        iter.remove();
      } else if (matt.name.equals("REFERENCE_LIST"))
        iter.remove();
    }

  }

  private String addDimension(ucar.nc2.Group g, H5Group h5group, String name, int length, boolean isUnlimited) {
    int pos = name.lastIndexOf("/");
    String dimName = (pos > 0) ? name.substring(pos + 1) : name;

    Dimension d = h5group.dimMap.get(dimName); // first look in current group
    if (d == null)
      d = g.findDimension(dimName); // then look in parent groups

    if (d == null) { // create if not found
      d = new Dimension(dimName, length, true, isUnlimited, false);
      d.setGroup(g);
      h5group.dimMap.put(dimName, d);
      h5group.dimList.add(d);
      if (debug1) debugOut.println("addDimension name=" + name + " dim= " + d + " to group " + g);

    } else { // extend length if needed
      if (length > d.getLength())
        d.setLength(length);
    }

    return d.getName();
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
   * @param forWho  whose attribue?
   * @param s       if being addded to a Structure, else null
   * @param matt    attribute message
   * @param attList add Attribute to this list
   * @throws IOException on io error
   */
  private void makeAttributes(String forWho, Structure s, MessageAttribute matt, List<Attribute> attList) throws IOException {
    MessageDatatype mdt = matt.mdt;

    if (mdt.type == 6) { // structure : make seperate attribute for each member
      if (null == s) {
        debugOut.println("SKIPPING structure values attribute for " + forWho);
        return;
      }

      ArrayStructure attData = (ArrayStructure) getAttributeData(forWho, s, matt.name, matt.mdt, matt.mds, matt.dataPos);
      if (attData == null) return;

      StructureData sdata = attData.getStructureData(0);
      for (Variable v : s.getVariables()) {
        Array mdata = sdata.getArray(v.getShortName());
        if (null != mdata)
          v.addAttribute(new Attribute(matt.name, mdata));
        else
          debugOut.println("didnt find attribute for " + v.getName());
      }

    } else {
      Attribute att = makeAttribute(forWho, matt.name, matt.mdt, matt.mds, matt.dataPos);
      if (att != null)
        attList.add(att);
    }

    // reading attribute values might change byte order during a read
    // put back to little endian for further header processing
    raf.order(RandomAccessFile.LITTLE_ENDIAN);
  }

  private Attribute makeAttribute(String forWho, String attName, MessageDatatype mdt, MessageDataspace mds, long dataPos) throws IOException {
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
    }

    return data;
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
    for (Message mess : facade.dobj.messages) {
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
      makeAttributes(facade.name, s, matt, v.getAttributes());
    }
    processSystemAttributes(facade.dobj.messages, v.getAttributes());
    if (fillAttribute != null)
      v.addAttribute(fillAttribute);
    if (vinfo.typeInfo.unsigned)
      v.addAttribute(new Attribute("_unsigned", "true"));
    if (facade.dobj.mdt.type == 5) {
      String desc = facade.dobj.mdt.opaque_desc;
      if ((desc != null) && (desc.length() > 0))
        v.addAttribute(new Attribute("_opaqueDesc", desc));
    }

    if (vinfo.isChunked) // make the data btree, but entries are not read in
      vinfo.btree = new DataBTree(dataAddress, v.getShape(), vinfo.storageSize);

    if (transformReference && (facade.dobj.mdt.type == 7) && (facade.dobj.mdt.referenceType == 0)) { // object reference
      Array newData = readReferenceObjectNames(v);
      v.setDataType(DataType.STRING);
      v.setCachedData(newData, true); // so H5iosp.read() is never called
      v.addAttribute(new Attribute("_HDF5ReferenceType", "values are names of referenced Variables"));
    }

    if (transformReference && (facade.dobj.mdt.type == 7) && (facade.dobj.mdt.referenceType == 1)) { // region reference
      int nelems = (int) v.getSize();
      int heapIdSize = 12;
      for (int i = 0; i < nelems; i++) {
        H5header.RegionReference heapId = new RegionReference(vinfo.dataPos + heapIdSize * i);
      }

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

  private Array readReferenceObjectNames(Variable v) throws IOException {
    Array data = v.read();
    IndexIterator ii = data.getIndexIterator();

    Array newData = Array.factory(DataType.STRING, v.getShape());
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
      String vname = name;
      v = new Structure(ncfile, g, s, vname);
      makeVariableShapeAndType(v, mdt, null, vinfo, null);
      addMembersToStructure(g, (Structure) v, vinfo, mdt);
      v.setElementSize(mdt.byteSize);

    } else {
      String vname = name;
      v = new Variable(ncfile, g, s, vname);
      makeVariableShapeAndType(v, mdt, null, vinfo, null);
    }

    // special case of variable length strings
    if (v.getDataType() == DataType.STRING)
      v.setElementSize(16); // because the array has elements that are HeapIdentifier

    v.setSPobject(vinfo);
    vinfo.setOwner(v);

    if (vinfo.typeInfo.unsigned)
      v.addAttribute(new Attribute("_unsigned", "true"));

    return v;
  }

  private void processSystemAttributes(List<Message> messages, List<Attribute> attributes) {
    for (Message mess : messages) {
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

  private boolean makeVariableShapeAndType(Variable v, MessageDatatype mdt, MessageDataspace msd, Vinfo vinfo, String dims) {

    int[] dim = (msd != null) ? msd.dimLength : new int[0];
    if (dim == null) dim = new int[0]; // scaler

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
        v.setDimensions(dims);
        if ((mdt.type == 9) && !mdt.isVString) { // variable length (not a string)
          v.setVariableLength(true);
        }
        /*
         List<Dimension> dimList = new ArrayList<Dimension>(v.getDimensions());
         dimList.add(Dimension.VLEN);
         v.setDimensions(dimList);
        } */

      } else if (mdt.type == 3) { // fixed length string - DataType.CHAR, add string length

        if (mdt.byteSize == 1) // scalar string member variable
          v.setDimensionsAnonymous(dim);
        else {
          int[] shape = new int[dim.length + 1];
          System.arraycopy(dim, 0, shape, 0, dim.length);
          shape[dim.length] = mdt.byteSize;
          v.setDimensionsAnonymous(shape);
        }

        /* } else if ((mdt.type == 9) && !mdt.isVString) { // variable length (not a string)
       List<Dimension> dimList = new ArrayList<Dimension>(1);
       dimList.add(Dimension.VLEN);
       v.setDimensions(dimList);

     /* } else if (mdt.type == 10) { // array
       v.setShapeWithAnonDimensions(mdt.dim); */

      } else {

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

    Map<Integer, String> enumMap;

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
        tinfo.dataType = DataType.ENUM;
        enumMap = mdt.map;

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

    public String toString() {
      StringBuffer buff = new StringBuffer();
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
      StringBuffer buff = new StringBuffer();
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
      if (dtype == DataType.BYTE) return N3iosp.NC_FILL_BYTE;
      if (dtype == DataType.CHAR) return (byte) 0;
      if (dtype == DataType.SHORT) return N3iosp.NC_FILL_SHORT;
      if (dtype == DataType.INT) return N3iosp.NC_FILL_INT;
      if (dtype == DataType.LONG) return N3iosp.NC_FILL_LONG;
      if (dtype == DataType.FLOAT) return N3iosp.NC_FILL_FLOAT;
      if (dtype == DataType.DOUBLE) return N3iosp.NC_FILL_DOUBLE;
      return null;
    }

    Object getFillValueNonDefault() {
      if (fillValue == null) return null;

      if ((typeInfo.dataType == DataType.BYTE) || (typeInfo.dataType == DataType.CHAR))
        return fillValue[0];

      ByteBuffer bbuff = ByteBuffer.wrap(fillValue);
      if (typeInfo.byteOrder >= 0)
        bbuff.order(typeInfo.byteOrder == RandomAccessFile.LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

      if (typeInfo.dataType == DataType.SHORT) {
        ShortBuffer tbuff = bbuff.asShortBuffer();
        return tbuff.get();

      } else if (typeInfo.dataType == DataType.INT) {
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
      StringBuffer buff = new StringBuffer();
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
    List<Message> dimMessages = new ArrayList<Message>();

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
      StringBuffer sbuff = new StringBuffer();
      sbuff.append(getName());
      if (dobj == null) {
        sbuff.append(" dobj is NULL! ");
      } else {
        sbuff.append(" id= ").append(dobj.address);
        sbuff.append(" messages= ");
        for (Message message : dobj.messages)
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

  private class DataObject {
    long address; // aka object id : obviously unique
    String who;   // may be null, may not be unique
    List<Message> messages = new ArrayList<Message>();
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
        int count = readMessagesVersion1(posMess, nmess, Integer.MAX_VALUE);
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
        int count = readMessagesVersion2(posMess, sizeOfChunk, (flags & 4) != 0);
        if (debugContinueMessage) debugOut.println(" nmessages read = " + count);
        if (debugPos) debugOut.println("<--done reading messages for <" + name + ">; position=" + raf.getFilePointer());
      }

      // look for group or a datatype/dataspace/layout message
      for (Message mess : messages) {
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

        // the heapId points to a Attribute Message in the fractal Heap
        long pos = fractalHeap.getPos(heapId);
        raf.seek(pos);
        MessageAttribute attMessage = new MessageAttribute();
        attMessage.read();
        list.add(attMessage);
        if (debugBtree2) System.out.println("    attMessage=" + attMessage);
      }
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

          count += readMessagesVersion2(continuationBlockFilePos + 4, (int) c.length - 8, creationOrderPresent);
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

  // Header Message: Level 2A1 and 2A2 (part of Data Object)
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
     * @param filePos              at this filePos
     * @param version              header version
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
        messData = getSharedMessageData(mtype);
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

  private Object getSharedMessageData(MessageType mtype) throws IOException {
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
        return dobj.mdt;
      }
      throw new UnsupportedOperationException("****SHARED MESSAGE type = " + mtype);
    }
  }

  // Message Type 1 : "Simple Dataspace" = dimension list / shape
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

  // Message Type 17/0x11 "Old Group" or "Symbol Table"
  private class MessageGroup {
    long btreeAddress, nameHeapAddress;

    void read() throws IOException {
      btreeAddress = readOffset();
      nameHeapAddress = readOffset();
      if (debug1) debugOut.println("   Group btreeAddress=" + btreeAddress + " nameHeapAddress=" + nameHeapAddress);
    }
  }

  // Message Type 2 "New Group" or "Link Info" (version 2)
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
  }

  // Message Type 3 : "Datatype"
  class MessageDatatype {
    int type, version;
    byte[] flags = new byte[3];
    int byteSize, byteOrder;
    boolean isOK = true;

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

    // enum, variable-length, array types have "base" DataType
    MessageDatatype base;
    boolean isVString;

    // array (10)
    int[] dim;

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append(" datatype= ").append(type);
      if ((type == 0) || (type == 1))
        sbuff.append(" byteSize= ").append(byteSize);
      else if (type == 2)
        sbuff.append(" timeType= ").append(timeType);
      else if (type == 6)
        sbuff.append(" nmembers= ").append(nmembers);
      else if (type == 7)
        sbuff.append(" referenceType= ").append(referenceType);
      else if (type == 9)
        sbuff.append(" isVString= ").append(isVString);
      else if ((type == 9) || (type == 10))
        sbuff.append(" parent= ").append(base);
      return sbuff.toString();
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
        base.read();
        debugDetail = saveDebugDetail;

        // read the enums

        String[] enumName = new String[nmembers];
        for (int i = 0; i < nmembers; i++) {
          if (version < 3)
            enumName[i] = readString8(raf); //padding
          else
            enumName[i] = readString(raf); // no padding
        }

        // read the values; must switch to parent byte order (!)
        if (base.byteOrder >= 0) raf.order(base.byteOrder);
        int[] enumValue = new int[nmembers];
        for (int i = 0; i < nmembers; i++)
          enumValue[i] = (int) readVariableSize(base.byteSize); // assume unsigned integer type, fits into int
        raf.order(RandomAccessFile.LITTLE_ENDIAN);

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
        base.read();

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
        base.read();

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

  // Message Type 4 "Fill Value Old" : fill value is stored in the message
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
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("   FillValue version= ").append(version).append(" spaceAllocateTime = ").append(spaceAllocateTime).append(" fillWriteTime=").append(fillWriteTime).append(" hasFillValue= ").append(hasFillValue);
      sbuff.append("\n size = ").append(size).append(" value=");
      for (int i = 0; i < size; i++) sbuff.append(" ").append(value[i]);
      return sbuff.toString();
    }

  }

  // Message Type 8 "Data Storage Layout" : regular (contiguous), chunked, or compact (stored with the message)
  private class MessageLayout {
    byte type; // 0 = Compact, 1 = Contiguous, 2 = Chunked
    long dataAddress = -1; // -1 means "not allocated"
    long contiguousSize; // size of data allocated contiguous
    int[] chunkSize;  // only for chunked, otherwise must use Dataspace
    int dataSize;

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append(" type= ").append(+type + " (");
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

  // Message Type 12/0xC "Attribute" : define an Atribute
  private class MessageAttribute {
    byte version;
    //short typeSize, spaceSize;
    String name;
    MessageDatatype mdt = new MessageDatatype();
    MessageDataspace mds = new MessageDataspace();
    long dataPos; // pointer to the attribute data section, must be absolute file position

    public String toString() {
      return "name= " + name + " dataPos= " + dataPos;
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
        mdt = (MessageDatatype) getSharedMessageData(MessageType.Datatype);
        if (debug1) debugOut.println("    MessageDatatype: " + mdt);
      } else {
        mdt.read();
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
  private class MessageComment {
    String comment;

    void read() throws IOException {
      comment = readString(raf);
    }

    public String toString() {
      return comment;
    }
  }

  // Message Type 18/0x12 "Last Modified" : last modified date represented as secs since 1970
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

  // Message Type 14/0xE ("Last Modified (old)" : last modified date represented as a String YYMM etc. use message type 18 instead
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

  // Message Type 16/0x10 "Continue" : point to more messages
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
        long pos = fractalHeap.getPos(heapId);
        raf.seek(pos);
        MessageLink linkMessage = new MessageLink();
        linkMessage.read();
        if (debugBtree2) System.out.println("    linkMessage=" + linkMessage);

        group.nestedObjects.add(new DataObjectFacade(group, linkMessage.linkName, linkMessage.linkAddress));
      }

    } else {
      // look for link messages
      for (Message mess : dobj.messages) {
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
      String magic = new String(heapname);
      if (!magic.equals("BTHD"))
        throw new IllegalStateException(magic + " should equal BTHD");

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
          e.nrecords = readVariableSize(1); // readVariableSizeMax(maxNumRecords);
          if (depth > 1)
            e.totNrecords = readVariableSize(2); // readVariableSizeMax(maxNumRecordsPlusDesc);

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
       *
       * @param want skip any nodes that are before this section
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
        StringBuffer sbuff = new StringBuffer();
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
   * @return String the String read from the heap
   * @throws IOException on read error
   */
  Array getHeapDataArray(long globalHeapIdAddress, DataType dataType, int byteOrder) throws IOException {
    HeapIdentifier heapId = new HeapIdentifier(globalHeapIdAddress);
    if (debugHeap) H5header.debugOut.println(" heapId= " + heapId);
    GlobalHeap.HeapObject ho = heapId.getHeapObject();
    if (debugHeap) H5header.debugOut.println(" HeapObject= " + ho);
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

  private class HeapIdentifier {
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
  private class FractalHeap {
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
      if (debugTracker) memTracker.add("Group FractalHeap (" + forWho + ")", address, pos);

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

        // read in the direct blocks
        for (DataBlock dblock : doublingTable.blockList) {
          readDirectBlock(getFileOffset(dblock.address), address, dblock);
        }
      }

      doublingTable.assignSizes();
    }


    long getPos(byte[] heapId) throws IOException {
      int type = (heapId[0] & 0x30) >> 4;
      int n = maxHeapSize / 8;
      int m = getNumBytesFromMax(maxDirectBlockSize - 1);

      int offset = makeIntFromBytes(heapId, 1, n);
      int size = makeIntFromBytes(heapId, 1 + n, m);
      //System.out.println("Heap id =" + showBytes(heapId) + " type = " + type + " n= " + n + " m= " + m + " offset= " + offset + " size= " + size);

      return doublingTable.getPos(offset);
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
      String magic = new String(heapname);
      if (!magic.equals("FHIB"))
        throw new IllegalStateException(magic + " should equal FHIB");

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
          debugOut.println("  DirectChild " + i + " address= " + directChild.address);

        if (directChild.address >= 0)
          doublingTable.blockList.add(directChild);
      }

      // child indirect blocks LOOK not sure if order is correct, this is depth first...
      int nIndirectChildren = doublingTable.tableWidth * doublingTable.nIndirectRows;
      for (int i = 0; i < nIndirectChildren; i++) {
        long childIndirectAddress = readOffset();
        if (debugDetail || debugFractalHeap)
          debugOut.println("  InDirectChild " + i + " address= " + childIndirectAddress);
        if (childIndirectAddress >= 0)
          readIndirectBlock(childIndirectAddress, heapAddress, hasFilter);
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

      int nbytes = maxHeapSize / 8;
      if (maxHeapSize % 8 != 0) nbytes++;
      dblock.offset = readVariableSize(nbytes);
      dblock.dataPos = pos; // raf.getFilePointer();  // offsets are from the start of the block

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
   * Read a fixed length String.
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
      vv = readVariableSizeN(size);
    }
    return vv;
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
    printBytes(head, mess, nbytes, false);
    raf.seek(savePos);
  }

  static public String showBytes(byte[] buff) {
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