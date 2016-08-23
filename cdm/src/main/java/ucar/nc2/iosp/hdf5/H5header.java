/*
 * Copyright 1998-2016 University Corporation for Atmospheric Research/Unidata
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

import ucar.nc2.constants.CDM;
import ucar.nc2.util.Misc;
import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.*;
import ucar.nc2.EnumTypedef;
import ucar.nc2.iosp.netcdf4.Nc4;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.nc2.iosp.Layout;
import ucar.nc2.iosp.LayoutRegular;
import ucar.ma2.*;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.*;

/**
 * Read all of the metadata of an HD5 file.
 *
 * @author caron
 */

/* Implementation notes
 * any field called address is actually reletive to the base address.
 * any field called filePos or dataPos is a byte offset within the file.
 */
  /*
   * it appears theres no sure fire way to tell if the file was written by netcdf4 library
   *  1) if one of the the NETCF4-XXX atts are set
   *  2) dimension scales:
   *     1) all dimensions have a dimension scale
   *     2) they all have the same length as the dimension
   *     3) all variables' dimensions have a dimension scale
   */
public class H5header {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5header.class);

  // special attribute names in HDF5
  static public final String HDF5_CLASS            = "CLASS";
  static public final String HDF5_DIMENSION_LIST   = "DIMENSION_LIST";
  static public final String HDF5_DIMENSION_SCALE  = "DIMENSION_SCALE";
  static public final String HDF5_DIMENSION_LABELS = "DIMENSION_LABELS";
  static public final String HDF5_DIMENSION_NAME   = "NAME";
  static public final String HDF5_REFERENCE_LIST   = "REFERENCE_LIST";

  // debugging
  static private boolean debugEnum = false, debugVlen = false;
  static private boolean debug1 = false, debugDetail = false, debugPos = false, debugHeap = false, debugV = false;
  static private boolean debugGroupBtree = false, debugDataBtree = false, debugBtree2 = false;
  static private boolean debugContinueMessage = false, debugTracker = false, debugSoftLink = false, debugHardLink = false, debugSymbolTable = false;
  static private boolean warnings = false, debugReference = false, debugRegionReference = false, debugCreationOrder = false, debugStructure = false;
  static private boolean debugDimensionScales = false;

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debug1           = debugFlag.isSet("H5header/header");
    debugBtree2      = debugFlag.isSet("H5header/btree2");
    debugContinueMessage = debugFlag.isSet("H5header/continueMessage");
    debugDetail      = debugFlag.isSet("H5header/headerDetails");
    debugDataBtree   = debugFlag.isSet("H5header/dataBtree");
    debugGroupBtree  = debugFlag.isSet("H5header/groupBtree");
    debugHeap        = debugFlag.isSet("H5header/Heap");
    debugPos         = debugFlag.isSet("H5header/filePos");
    debugReference   = debugFlag.isSet("H5header/reference");
    debugSoftLink    = debugFlag.isSet("H5header/softLink");
    debugHardLink    = debugFlag.isSet("H5header/hardLink");
    debugSymbolTable = debugFlag.isSet("H5header/symbolTable");
    debugTracker     = debugFlag.isSet("H5header/memTracker");
    debugV           = debugFlag.isSet("H5header/Variable");
    debugStructure   = debugFlag.isSet("H5header/structure");
  }

  static private final byte[] head = {(byte) 0x89, 'H', 'D', 'F', '\r', '\n', 0x1a, '\n'};
  static private final String hdf5magic = new String(head, CDM.utf8Charset);
  static private final long maxHeaderPos = 50000; // header's gotta be within this
  static private final boolean transformReference = true;

  static public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    long filePos = 0;
    long size = raf.length();

    // search forward for the header
    while ((filePos < size - 8) && (filePos < maxHeaderPos)) {
      raf.seek(filePos);
      String magic = raf.readString(8);
      if (magic.equals(hdf5magic))
        return true;
      filePos = (filePos == 0) ? 512 : 2 * filePos;
    }

    return false;
  }

  ////////////////////////////////////////////////////////////////////////////////

  RandomAccessFile raf;
  ucar.nc2.NetcdfFile ncfile;
  private H5iosp h5iosp;

  private long baseAddress;
  byte sizeOffsets, sizeLengths;
  boolean isOffsetLong, isLengthLong;
  // boolean alreadyWarnNdimZero;

  /* Cant always tell if written with netcdf library. if all dimensions have coordinate variables, eg:
    Q:/cdmUnitTest/formats/netcdf4/ncom_relo_fukushima_1km_tmp_2011040800_t000.nc4
   */
  private boolean isNetcdf4 = false;
  //Map<Integer, DataObjectFacade> dimIds = null; // if isNetcdf4 and all dimension scales have _Netcdf4Dimid attribute

  private H5Group rootGroup;
  private Map<String, DataObjectFacade> symlinkMap = new HashMap<>(200);
  private Map<Long, DataObject> addressMap = new HashMap<>(200);
  private Map<Long, GlobalHeap> heapMap = new HashMap<>();
  private java.text.SimpleDateFormat hdfDateParser;

  private java.io.PrintWriter debugOut = new PrintWriter( new OutputStreamWriter(System.out, CDM.utf8Charset));
  private MemTracker memTracker;

  H5header(RandomAccessFile myRaf, ucar.nc2.NetcdfFile ncfile, H5iosp h5iosp) {
    this.ncfile = ncfile;
    this.raf = myRaf;
    this.h5iosp = h5iosp;
  }

  public byte getSizeOffsets() {
    return sizeOffsets;
  }

  boolean isNetcdf4() {
    return isNetcdf4;
  }

  public void read(java.io.PrintWriter debugPS) throws IOException {
    if (debugPS != null)
      debugOut = debugPS;

    long actualSize = raf.length();

    if (debugTracker) memTracker = new MemTracker(actualSize);  // LOOK WTF ??

    // find the superblock - no limits on how far in
    boolean ok = false;
    long filePos = 0;
    while ((filePos < actualSize-8)) {
      raf.seek(filePos);
      String magic = raf.readString(8);
      if (magic.equals(hdf5magic)) {
        ok = true;
        break;
      }
      filePos = (filePos == 0) ? 512 : 2 * filePos;
    }
    if (!ok) {
        throw new IOException("Not a netCDF4/HDF5 file ");
    }
    if (debug1) {
        log.debug("H5header opened file to read:'{}' size= {}", raf.getLocation(), actualSize);
    }
    // now we are positioned right after the header

    // header information is in le byte order
    raf.order(RandomAccessFile.LITTLE_ENDIAN);

    long superblockStart = raf.getFilePointer() - 8;
    if (debugTracker) memTracker.add("header", 0, superblockStart);

    // superblock version
    byte versionSB = raf.readByte();

    if (versionSB < 2) {
      readSuperBlock1(superblockStart, versionSB);
    } else if (versionSB == 2) {
      readSuperBlock2(superblockStart);
    } else {
      throw new IOException("Unknown superblock version= " + versionSB);
    }

    // now look for symbolic links LOOK this doesnt work; probably remove 10/27/14 jc
    replaceSymbolicLinks(rootGroup);

    // recursively run through all the dataObjects and add them to the ncfile
    boolean allSharedDimensions = makeNetcdfGroup(ncfile.getRootGroup(), rootGroup);
    if (allSharedDimensions) isNetcdf4 = true;

    /*if (debugReference) {
     log.debug("DataObjects");
     for (DataObject ob : addressMap.values())
       log.debug("  " + ob.name + " address= " + ob.address + " filePos= " + getFileOffset(ob.address));
   } */
    if (debugTracker) {
      Formatter f= new Formatter();
      memTracker.report(f);
      log.debug(f.toString());
    }
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
    if (debugDetail) {
      log.debug(" versionSB= " + versionSB + " versionFSS= " + versionFSS + " versionGroup= " + versionGroup +
              " versionSHMF= " + versionSHMF);
    }

    sizeOffsets = raf.readByte();
    isOffsetLong = (sizeOffsets == 8);

    sizeLengths = raf.readByte();
    isLengthLong = (sizeLengths == 8);
    if (debugDetail) {
        log.debug(" sizeOffsets= {} sizeLengths= {}", sizeOffsets, sizeLengths);
        log.debug(" isLengthLong= {} isOffsetLong= {}", isLengthLong, isOffsetLong);
    }

    raf.read(); // skip 1 byte
    //log.debug(" position="+mapBuffer.position());

    btreeLeafNodeSize = raf.readShort();
    btreeInternalNodeSize = raf.readShort();
    if (debugDetail) {
        log.debug(" btreeLeafNodeSize= {} btreeInternalNodeSize= {}", btreeLeafNodeSize, btreeInternalNodeSize);
    }
    //log.debug(" position="+mapBuffer.position());

    fileFlags = raf.readInt();
    if (debugDetail) {
        log.debug(" fileFlags= 0x{}", Integer.toHexString(fileFlags));
    }

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
        if (debugDetail) {
            log.debug(" baseAddress set to superblockStart");
        }
    }

    if (debugDetail) {
      log.debug(" baseAddress= 0x{}", Long.toHexString(baseAddress));
      log.debug(" global free space heap Address= 0x{}", Long.toHexString(heapAddress));
      log.debug(" eof Address={}", eofAddress);
      log.debug(" raf length= {}", raf.length());
      log.debug(" driver BlockAddress= 0x{}", Long.toHexString(driverBlockAddress));
      log.debug("");
    }
    if (debugTracker) memTracker.add("superblock", superblockStart, raf.getFilePointer());

    // look for file truncation
    long fileSize = raf.length();
    if (fileSize < eofAddress)
      throw new IOException("File is truncated should be= " + eofAddress + " actual = " + fileSize + "%nlocation= " + raf.getLocation());

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
    if (debugDetail) {
        log.debug(" sizeOffsets= {} sizeLengths= {}", sizeOffsets, sizeLengths);
        log.debug(" isLengthLong= {} isOffsetLong= {}", isLengthLong, isOffsetLong);
    }

    byte fileFlags = raf.readByte();
    if (debugDetail) {
        log.debug(" fileFlags= 0x{}", Integer.toHexString(fileFlags));
    }

    baseAddress = readOffset();
    long extensionAddress = readOffset();
    long eofAddress = readOffset();
    long rootObjectAddress = readOffset();
    int checksum = raf.readInt();

    if (debugDetail) {
      log.debug(" baseAddress= 0x{}", Long.toHexString(baseAddress));
      log.debug(" extensionAddress= 0x{}", Long.toHexString(extensionAddress));
      log.debug(" eof Address={}", eofAddress);
      log.debug(" rootObjectAddress= 0x{}", Long.toHexString(rootObjectAddress));
      log.debug("");
    }

    if (debugTracker) memTracker.add("superblock", superblockStart, raf.getFilePointer());

    if (baseAddress != superblockStart) {
        baseAddress = superblockStart;
        eofAddress += superblockStart;
        if (debugDetail) {
            log.debug(" baseAddress set to superblockStart");
        }
    }

    // look for file truncation
    long fileSize = raf.length();
    if (fileSize < eofAddress) {
      throw new IOException("File is truncated should be= " + eofAddress + " actual = " + fileSize);
    }

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
          log.warn(" WARNING Didnt find symbolic link={} from {}", dof.linkName, dof.name);
          objList.remove(count);
          continue;
        }

        // dont allow loops
        if (link.group != null) {
          if (group.isChildOf(link.group)) {
            log.warn(" ERROR Symbolic Link loop found ={}", dof.linkName);
            objList.remove(count);
            continue;
          }
        }

        // dont allow in the same group. better would be to replicate the group with the new name
        if (dof.parent == link.parent) {
          objList.remove(dof);
          count--; // negate the incr
        } else  // replace
          objList.set(count, link);

        if (debugSoftLink) {
            log.debug("  Found symbolic link={}", dof.linkName);
        }
      }

      count++;
    }
  }

  ///////////////////////////////////////////////////////////////
  // construct netcdf objects

  private boolean makeNetcdfGroup(ucar.nc2.Group ncGroup, H5Group h5group) throws IOException {
    // if (h5group == null) return true; // ??

  /* 6/21/2013 new algorithm for dimensions.
    1. find all objects with all CLASS = "DIMENSION_SCALE", make into a dimension. use shape(0) as length. keep in order
    2. if also a variable (NAME != "This is a ...") then first dim = itself, second matches length, if multiple match, use :_Netcdf4Coordinates = 0, 3 and order of dimensions.
    3. use DIMENSION_LIST to assign dimensions to data variables.
  */

    //  1. find all objects with all CLASS = "DIMENSION_SCALE", make into a dimension. use shape(0) as length. keep in order
    for (DataObjectFacade facade : h5group.nestedObjects) {
      if (facade.isVariable)
        findDimensionScales(ncGroup, h5group, facade);
    }

    // 2. if also a variable (NAME != "This is a ...") then first dim = itself, second matches length, if multiple match, use :_Netcdf4Coordinates = 0, 3 and order of dimensions.
    for (DataObjectFacade facade : h5group.nestedObjects) {
      if (facade.is2DCoordinate)
        findDimensionScales2D(h5group, facade);
    }

    // old way
    /* deal with multidim dimension scales - ugh!
    if (dimIds != null) {
      for (DataObjectFacade dimscale : dimIds.values()) {
        if (dimscale.dobj.mds.ndims > 1) {
          StringBuilder sbuff = new StringBuilder();
          Attribute att = dimscale.netcdf4CoordinatesAtt;
          for (int i=0; i<att.getLength(); i++) {
            int id = att.getNumericValue(i).intValue();
            DataObjectFacade ds2 = dimIds.get(id);
            String name = ds2.getName();
            int pos = name.lastIndexOf('/');
            String dimName = (pos >= 0) ? name.substring(pos + 1) : name;
            sbuff.append(dimName);
            sbuff.append(" ");
          }
          dimscale.dimList = sbuff.toString();
        }
      }
    }

    // deal with multidim dimension scales part two - double ugh!
    for (DataObjectFacade facade : h5group.nestedObjects) {
      if (facade.isVariable && facade.netcdf4CoordinatesAtt != null && facade.dimList.equals("%REDO%")) {
        Formatter f = new Formatter();
        for (int i=0 ;i<facade.netcdf4CoordinatesAtt.getLength(); i++) {
          int dimIndex = facade.netcdf4CoordinatesAtt.getNumericValue(i).intValue();
          f.format("%s ", h5group.dimList.get(dimIndex).getShortName());
        }
        facade.dimList = f.toString();
      }
    } */

    boolean allHaveSharedDimensions = true;

    // 3. use DIMENSION_LIST to assign dimensions to other variables.
    for (DataObjectFacade facade : h5group.nestedObjects) {
      if (facade.isVariable)
        allHaveSharedDimensions &= findSharedDimensions(ncGroup, h5group, facade);
    }

    createDimensions(ncGroup, h5group);

    // nested objects - groups and variables
    for (DataObjectFacade facadeNested : h5group.nestedObjects) {

      if (facadeNested.isGroup) {
        H5Group h5groupNested = new H5Group(facadeNested);
        if (facadeNested.group == null) // hard link with cycle
          continue;                     // just skip it
        ucar.nc2.Group nestedGroup = new ucar.nc2.Group(ncfile, ncGroup, facadeNested.name);
        ncGroup.addGroup(nestedGroup);
        allHaveSharedDimensions &= makeNetcdfGroup(nestedGroup, h5groupNested);
        if (debug1) {
            log.debug("--made Group " + nestedGroup.getFullName() + " add to " + ncGroup.getFullName());
        }

      } else if (facadeNested.isVariable) {
        if (debugReference && facadeNested.dobj.mdt.type == 7) {
            log.debug("{}", facadeNested);
        }

        Variable v = makeVariable(ncGroup, facadeNested);
        if ((v != null) && (v.getDataType() != null)) {
          v.setParentGroup(ncGroup);
          ncGroup.addVariable(v);

          if (v.getDataType().isEnum()) {
            EnumTypedef enumTypedef = v.getEnumTypedef();
            if (enumTypedef == null) {
              log.warn("EnumTypedef is missing for variable: {}", v.getFullName());
              throw new IllegalStateException("EnumTypedef is missing for variable: " + v.getFullName());
            }
            // This code apparently addresses the possibility of an anonymous enum LOOK ??
            String ename = enumTypedef.getShortName();
            if (ename == null || ename.length() == 0) {
              enumTypedef = ncGroup.findEnumeration(facadeNested.name);
              if (enumTypedef == null) {
                enumTypedef = new EnumTypedef(facadeNested.name, facadeNested.dobj.mdt.map);
                ncGroup.addEnumeration(enumTypedef);
              }
              v.setEnumTypedef(enumTypedef);
            }
          }

          Vinfo vinfo = (Vinfo) v.getSPobject();
            if (debugV) {
                log.debug("  made Variable " + v.getFullName() + "  vinfo= " + vinfo + "\n" + v);
            }
        }

      } else if (facadeNested.isTypedef) {
        if (debugReference && facadeNested.dobj.mdt.type == 7) {
            log.debug("{}", facadeNested);
        }

        if (facadeNested.dobj.mdt.map != null) {
          EnumTypedef enumTypedef = ncGroup.findEnumeration(facadeNested.name);
          if (enumTypedef == null) {
            DataType basetype;
            switch (facadeNested.dobj.mdt.byteSize) {
              case 1:
                basetype = DataType.ENUM1;
                break;
              case 2:
                basetype = DataType.ENUM2;
                break;
              case 4:
                basetype = DataType.ENUM4;
                break;
              default:
                basetype = DataType.ENUM4;
                break;
            }
            enumTypedef = new EnumTypedef(facadeNested.name, facadeNested.dobj.mdt.map, basetype);
            ncGroup.addEnumeration(enumTypedef);
          }
        }
        if (debugV) {
            log.debug("  made enumeration {}", facadeNested.name);
        }
      }

    } // loop over nested objects

    // create group attributes last. need enums to be found first
    List<MessageAttribute> fatts = filterAttributes(h5group.facade.dobj.attributes);
    for (MessageAttribute matt : fatts) {
      try {
        makeAttributes(null, matt, ncGroup);
      } catch (InvalidRangeException e) {
        throw new IOException(e.getMessage());
      }
    }

    // add system attributes
    processSystemAttributes(h5group.facade.dobj.messages, ncGroup);
    return allHaveSharedDimensions;
  }

  /////////////////////////
  /* from http://www.unidata.ucar.edu/software/netcdf/docs/netcdf.html#NetCDF_002d4-Format
  C.3.7 Attributes

  Attributes in HDF5 and netCDF-4 correspond very closely. Each attribute in an HDF5 file is represented as an attribute
  in the netCDF-4 file, with the exception of the attributes below, which are ignored by the netCDF-4 API.

  _Netcdf4Coordinates An integer array containing the dimension IDs of a variable which is a multi-dimensional coordinate variable.
  _nc3_strict         When this (scalar, H5T_NATIVE_INT) attribute exists in the root group of the HDF5 file, the netCDF API will enforce
                      the netCDF classic model on the data file.
  REFERENCE_LIST      This attribute is created and maintained by the HDF5 dimension scale API.
  CLASS               This attribute is created and maintained by the HDF5 dimension scale API.
  DIMENSION_LIST      This attribute is created and maintained by the HDF5 dimension scale API.
  NAME                This attribute is created and maintained by the HDF5 dimension scale API.

----------
  from dim_scales_wk9 - Nunes.ppt

  Attribute named "CLASS" with the value "DIMENSION_SCALE"
  Optional attribute named "NAME"
  Attribute references to any associated Dataset

-------------
  from http://www.unidata.ucar.edu/mailing_lists/archives/netcdfgroup/2008/msg00093.html

  Then comes the part you will have to do for your datasets. You open the data
  dataset, get an ID, DID variable here, open the latitude dataset, get its ID,
  DSID variable here, and "link" the 2 with this call

  if (H5DSattach_scale(did,dsid,DIM0) < 0)

  what this function does is to associated the dataset DSID (latitude) with the
  *dimension* specified by the parameter DIM0 (0, in this case, the first
  dimension of the 2D array) of the dataset DID

  If you open HDF Explorer and expand the attributes of the "data" dataset you
  will see an attribute called DIMENSION_LIST.
  This is done by this function. It is an array that contains 2 HDF5 references,
  one for the latitude dataset, other for the longitude)

  If you expand the "lat" dataset , you will see that it contains an attribute
  called REFERENCE_LIST. It is a compound type that contains
  1)      a reference to my "data" dataset
  2)      the index of the data dataset this scale is to be associated with (0
  for the lat, 1 for the lon)
  */

  // find the Dimension Scale objects, turn them into shared dimensions
  // always has attribute CLASS = "DIMENSION_SCALE"
  // note that we dont bother looking at their REFERENCE_LIST
  private void findDimensionScales(ucar.nc2.Group g, H5Group h5group, DataObjectFacade facade) throws IOException {
    Iterator<MessageAttribute> iter = facade.dobj.attributes.iterator();
    while (iter.hasNext()) {
      MessageAttribute matt = iter.next();
      if (matt.name.equals(HDF5_CLASS)) {
        Attribute att = makeAttribute(matt);
        if (att == null) throw new IllegalStateException();
        String val = att.getStringValue();
        if (val.equals(HDF5_DIMENSION_SCALE) && facade.dobj.mds.ndims > 0) {

          // create a dimension - always use the first dataspace length
          facade.dimList = addDimension(g, h5group, facade.name, facade.dobj.mds.dimLength[0], facade.dobj.mds.maxLength[0] == -1);
          facade.hasNetcdfDimensions = true;
          if (!h5iosp.includeOriginalAttributes) iter.remove();

          if (facade.dobj.mds.ndims > 1)
            facade.is2DCoordinate = true;

          /* old way
          findNetcdf4DimidAttribute(facade);
          if (facade.dobj.mds.ndims == 1) { // 1D dimension scale
            // create a dimension
            facade.dimList = addDimension(g, h5group, facade.name, facade.dobj.mds.dimLength[0], facade.dobj.mds.maxLength[0] == -1);
            if (! h5iosp.includeOriginalAttributes) iter.remove();
            if (debugDimensionScales)
              System.out.printf("Found dimScale %s for group '%s' matt=%s %n", facade.dimList, g.getFullName(), matt);
          } else {  // multiD dimension scale
            int dimIndex = findCoordinateDimensionIndex(facade, h5group);
            addDimension(g, h5group, facade.name, facade.dobj.mds.dimLength[dimIndex], facade.dobj.mds.maxLength[dimIndex] == -1);
            if (! h5iosp.includeOriginalAttributes) iter.remove();
            if (debugDimensionScales)
              System.out.printf("Found multidim dimScale %s for group '%s' matt=%s %n", facade.dimList, g.getFullName(), matt);
          }
          */

        }
      }
    }
  }

  private void findDimensionScales2D(H5Group h5group, DataObjectFacade facade) throws IOException {
    int[] lens = facade.dobj.mds.dimLength;
    if (lens.length > 2) {
      log.warn("DIMENSION_LIST: dimension scale > 2 = {}", facade.getName());
      return;
    }

    // first dimension is itself
    String name = facade.getName();
    int pos = name.lastIndexOf('/');
    String dimName = (pos >= 0) ? name.substring(pos + 1) : name;

    StringBuilder sbuff = new StringBuilder();
    sbuff.append(dimName);
    sbuff.append(" ");

    // second dimension is really an anonymous dimension, ironically now we go through amazing hoops to keep it shared
    // 1. use dimids if they exist
    // 2. if length matches and unique, use it
    // 3. if no length matches or multiple matches, then use anonymous

    int want_len = lens[1]; // second dimension
    Dimension match = null;
    boolean unique = true;
    for (Dimension d : h5group.dimList) {
      if (d.getLength() == want_len) {
        if (match == null) match = d;
        else unique = false;
      }
    }
    if (match != null && unique) {
      sbuff.append(match.getShortName()); // 2. if length matches and unique, use it

    } else {
      if (match == null) { // 3. if no length matches or multiple matches, then use anonymous
        log.warn("DIMENSION_LIST: dimension scale {} has second dimension {} but no match", facade.getName(), want_len);
        sbuff.append(Integer.toString(want_len));
      } else {
        log.warn("DIMENSION_LIST: dimension scale {} has second dimension {} but multiple matches", facade.getName(), want_len);
        sbuff.append(Integer.toString(want_len));
      }
    }

    facade.dimList = sbuff.toString();
  }

  /* private void findNetcdf4DimidAttribute(DataObjectFacade facade) throws IOException {
    for (MessageAttribute matt : facade.dobj.attributes) {
      if (matt.name.equals(Nc4.NETCDF4_DIMID)) {
        if (dimIds == null) dimIds = new HashMap<Integer, DataObjectFacade>();
        Attribute att_dimid = makeAttribute(matt);
        Integer dimid = (Integer) att_dimid.getNumericValue();
        dimIds.put(dimid, facade);
        return;
      }
    }
    if (dimIds != null) // supposed to all have them
      log.warn("Missing "+Nc4.NETCDF4_DIMID+" attribute on "+facade.getName());
  } */


  /* the case of multidimensional dimension scale. We need to identify which index to use as the dimension length.
     the pattern is, eg:
      _Netcdf4Coordinates = 6, 4
      _Netcdf4Dimid = 6
  *
  private int findCoordinateDimensionIndex(DataObjectFacade facade, H5Group h5group) throws IOException {
    Attribute att_coord = null;
    Attribute att_dimid = null;
    for (MessageAttribute matt : facade.dobj.attributes) {
      if (matt.name.equals(Nc4.NETCDF4_COORDINATES))
        att_coord = makeAttribute(matt);
      if (matt.name.equals(Nc4.NETCDF4_DIMID))
        att_dimid = makeAttribute(matt);
    }
    if (att_coord != null && att_dimid != null) {
      facade.netcdf4CoordinatesAtt = att_coord;
      Integer want = (Integer) att_dimid.getNumericValue();
      for (int i=0; i<att_coord.getLength(); i++) {
        Integer got = (Integer) att_dimid.getNumericValue(i);
        if (want.equals(got))
          return i;
      }
      log.warn("Multidimension dimension scale attributes "+Nc4.NETCDF4_COORDINATES+" and "+Nc4.NETCDF4_DIMID+" dont match. Assume Dimension is index 0 (!)");
      return 0;
    }
    if (att_coord != null) {
      facade.netcdf4CoordinatesAtt = att_coord;
      int n = h5group.dimList.size(); // how many dimensions are already defined
      facade.dimList = "%REDO%";  // token to create list when all dimensions found
      for (int i=0 ;i<att_coord.getLength(); i++) {
        if (att_coord.getNumericValue(i).intValue() == n) return i;
      }
      log.warn("Multidimension dimension scale attribute "+Nc4.NETCDF4_DIMID+" missing. Dimension ordering is not found. Assume index 0 (!)");
      return 0;
    }

    log.warn("Multidimension dimension scale doesnt have "+Nc4.NETCDF4_COORDINATES+" attribute. Assume Dimension is index 0 (!)");
    return 0;
  }  */

  // look for references to dimension scales, ie the variables that use them
  // return true if this variable is compatible with netcdf4 data model
  private boolean findSharedDimensions(ucar.nc2.Group g, H5Group h5group, DataObjectFacade facade) throws IOException {
    Iterator<MessageAttribute> iter = facade.dobj.attributes.iterator();
    while (iter.hasNext()) {
      MessageAttribute matt = iter.next();
      // find the dimensions - set length to maximum
      // DIMENSION_LIST contains, for each dimension, a list of references to Dimension Scales
      if (matt.name.equals(HDF5_DIMENSION_LIST)) { // references : may extend the dimension length
        Attribute att = makeAttribute(matt);       // this reads in the data
        if (att == null) {
          log.warn("DIMENSION_LIST: failed to read on variable {}", facade.getName());

        } else if (att.getLength() != facade.dobj.mds.dimLength.length) { // some attempts to writing hdf5 directly fail here
          log.warn("DIMENSION_LIST: must have same number of dimension scales as dimensions att={} on variable {}", att, facade.getName());

        } else {
          StringBuilder sbuff = new StringBuilder();
          for (int i = 0; i < att.getLength(); i++) {
            String name = att.getStringValue(i);
            String dimName = extendDimension(g, h5group, name, facade.dobj.mds.dimLength[i]);
            sbuff.append(dimName).append(" ");
          }
          facade.dimList = sbuff.toString();
          facade.hasNetcdfDimensions = true;
          if (debugDimensionScales) {
            log.debug("Found dimList '{}' for group '{}' matt={}", facade.dimList, g.getFullName(), matt);
          }
          if (!h5iosp.includeOriginalAttributes) iter.remove();
        }

      } else if (matt.name.equals(HDF5_DIMENSION_NAME)) {
        Attribute att = makeAttribute(matt);
        if (att == null) throw new IllegalStateException();
        String val = att.getStringValue();
        if (val.startsWith("This is a netCDF dimension but not a netCDF variable")) {
          facade.isVariable = false;
          isNetcdf4 = true;
        }
        if (!h5iosp.includeOriginalAttributes) iter.remove();
        if (debugDimensionScales) {
            log.debug("Found {}", val);
        }

      } else if (matt.name.equals(HDF5_REFERENCE_LIST))
        if (!h5iosp.includeOriginalAttributes) iter.remove();
    }
    return facade.hasNetcdfDimensions || facade.dobj.mds.dimLength.length == 0;

  }

  // add a dimension, return its name
  private String addDimension(ucar.nc2.Group g, H5Group h5group, String name, int length, boolean isUnlimited) {
    int pos = name.lastIndexOf('/');
    String dimName = (pos >= 0) ? name.substring(pos + 1) : name;

    Dimension d = h5group.dimMap.get(dimName); // first look in current group
    //if (d == null)
    //  d = g.findDimension(dimName); // then look in parent groups  LOOK

    if (d == null) { // create if not found
      d = new Dimension(dimName, length, true, isUnlimited, false);
      d.setGroup(g);
      h5group.dimMap.put(dimName, d);
      h5group.dimList.add(d);
        if (debugDimensionScales) {
            log.debug("addDimension name=" + name + " dim= " + d + " to group " + g);
        }

    } else { // check has correct length
      if (d.getLength() != length)
        throw new IllegalStateException("addDimension: DimScale has different length than dimension it references dimScale=" + dimName);
    }

    return d.getShortName();
  }

  // look for unlimited dimensions without dimension scale - must get length from the variable
  private String extendDimension(ucar.nc2.Group g, H5Group h5group, String name, int length) {
    int pos = name.lastIndexOf('/');
    String dimName = (pos >= 0) ? name.substring(pos + 1) : name;

    Dimension d = h5group.dimMap.get(dimName); // first look in current group
    if (d == null)
      d = g.findDimension(dimName); // then look in parent groups

    if (d != null) {
      if (d.isUnlimited() && (length > d.getLength()))
        d.setLength(length);

      if (!d.isUnlimited() && (length != d.getLength())) {
        throw new IllegalStateException("extendDimension: DimScale has different length than dimension it references dimScale=" + dimName);
      }
      return d.getShortName();
    }

    return dimName;
  }

  private void createDimensions(ucar.nc2.Group g, H5Group h5group) throws IOException {
    for (Dimension d : h5group.dimList) {
      g.addDimension(d);
    }
  }

  private List<MessageAttribute> filterAttributes(List<MessageAttribute> attList) {
    List<MessageAttribute> result = new ArrayList<>(attList.size());
    for (MessageAttribute matt : attList) {
      if (matt.name.equals(Nc4.NETCDF4_COORDINATES) || matt.name.equals(Nc4.NETCDF4_DIMID) || matt.name.equals(Nc4.NETCDF4_STRICT)) {
        isNetcdf4 = true;
      } else {
        result.add(matt);
      }
    }
    return result;
  }

  /**
   * Create Attribute objects from the MessageAttribute and add to list
   *
   * @param s            if attribute for a Structure, then deconstruct and add to member variables
   * @param matt         attribute message
   * @param attContainer add Attribute to this
   * @throws IOException                    on io error
   * @throws ucar.ma2.InvalidRangeException on shape error
   */
  private void makeAttributes(Structure s, MessageAttribute matt, AttributeContainer attContainer) throws IOException, InvalidRangeException {
    MessageDatatype mdt = matt.mdt;

    if (mdt.type == 6) { // structure
      Vinfo vinfo = new Vinfo(matt.mdt, matt.mds, matt.dataPos);
      ArrayStructure attData = (ArrayStructure) readAttributeData(matt, vinfo, DataType.STRUCTURE);

      if (null == s) {
        // flatten and add to list
        for (StructureMembers.Member sm : attData.getStructureMembers().getMembers()) {
          Array memberData = attData.extractMemberArray(sm);
          attContainer.addAttribute(new Attribute(matt.name + "." + sm.getName(), memberData));
        }

      } else if (matt.name.equals(CDM.FIELD_ATTS)) {
          // flatten and add to list
          for (StructureMembers.Member sm : attData.getStructureMembers().getMembers()) {
            String memberName = sm.getName();
            int pos = memberName.indexOf(":");
            if (pos < 0) continue; // LOOK
            String fldName = memberName.substring(0,pos);
            String attName = memberName.substring(pos+1);
            Array memberData = attData.extractMemberArray(sm);
            Variable v = s.findVariable(fldName);
            if (v == null) continue; // LOOK
            v.addAttribute(new Attribute(attName, memberData));
          }

      } else {  // assign separate attribute for each member
        StructureMembers attMembers = attData.getStructureMembers();
        for (Variable v : s.getVariables()) {
          StructureMembers.Member sm = attMembers.findMember(v.getShortName()); // does the compound attribute have a member with same name as nested variable ?
          if (null != sm) {
            Array memberData = attData.extractMemberArray(sm);                 // if so, add the att to the member variable, using the name of the compound attribute
            v.addAttribute(new Attribute(matt.name, memberData));              // LOOK want to check for missing values....
          }
        }

        // look for unassigned members, add to the list
        for (StructureMembers.Member sm : attData.getStructureMembers().getMembers()) {
          if (s.findVariable(sm.getName()) == null) {
            Array memberData = attData.extractMemberArray(sm);
            attContainer.addAttribute(new Attribute(matt.name + "." + sm.getName(), memberData));
          }
        }
      }

    } else {
      // make a single attribute
      Attribute att = makeAttribute(matt);
      if (att != null)
        attContainer.addAttribute(att);
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
      if (dtype == DataType.CHAR)
        return new Attribute(matt.name, ""); // empty char considered to be a 0 length string
      else
        return new Attribute(matt.name, dtype, vinfo.typeInfo.unsigned);
    }

    Array attData;
    try {
      attData = readAttributeData(matt, vinfo, dtype);
      attData.setUnsigned(matt.mdt.unsigned);

    } catch (InvalidRangeException e) {
      log.warn("failed to read Attribute " + matt.name + " HDF5 file=" + raf.getLocation());
      return null;
    }

    Attribute result;
    if (attData.getElementType() == Array.class) { // vlen LOOK
      List<Object> dataList = new ArrayList<>();
      while (attData.hasNext()) {
        Array nested = (Array) attData.next();
        while (nested.hasNext())
          dataList.add(nested.next());
      }
      result = new Attribute(matt.name, dataList);

    } else {
      result = new Attribute(matt.name, attData);
    }

    raf.order(RandomAccessFile.LITTLE_ENDIAN);
    return result;
  }

  // read attribute values without creating a Variable
  private Array readAttributeData(H5header.MessageAttribute matt, H5header.Vinfo vinfo, DataType dataType) throws IOException, InvalidRangeException {
    int[] shape = matt.mds.dimLength;

    // Structures
    if (dataType == DataType.STRUCTURE) {
      boolean hasStrings = false;

      StructureMembers sm = new StructureMembers(matt.name);
      for (H5header.StructureMember h5sm : matt.mdt.members) {

        // from tkunicki@usgs.gov 2/19/2010 - fix for compound attributes
        //DataType dt = getNCtype(h5sm.mdt.type, h5sm.mdt.byteSize);
        //StructureMembers.Member m = sm.addMember(h5sm.name, null, null, dt, new int[] {1});

        DataType dt;
        int[] dim;
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
            dim = new int[]{1};
            break;
        }
        StructureMembers.Member m = sm.addMember(h5sm.name, null, null, dt, dim);

        if (h5sm.mdt.endian >= 0) // apparently each member may have seperate byte order (!!!??)
          m.setDataObject(h5sm.mdt.endian == RandomAccessFile.LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        m.setDataParam((h5sm.offset)); // offset since start of Structure
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
        if (debugStructure) {
          log.debug(" readStructure " + matt.name + " chunk= " + chunk + " index.getElemSize= " + layout.getElemSize());
        }

        // copy bytes directly into the underlying byte[]
        raf.seek(chunk.getSrcPos());
        raf.readFully(byteArray, (int) chunk.getDestElem() * recsize, chunk.getNelems() * recsize);
      }

      // strings are stored on the heap, and must be read separately
      if (hasStrings) {
        int destPos = 0;
        for (int i = 0; i < layout.getTotalNelems(); i++) { // loop over each structure
          h5iosp.convertHeap(asbb, destPos, sm);
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
      int endian = vinfo.typeInfo.endian;
      DataType readType = dataType;
      if (vinfo.typeInfo.base.hdfType == 7) { // reference
        readType = DataType.LONG;
        endian = 1; // apparently always LE
      }

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
          Array vlenArray = getHeapDataArray(address, readType, endian);
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
    int endian = vinfo.typeInfo.endian;

    if (vinfo.typeInfo.hdfType == 2) { // time
      readDtype = vinfo.mdt.timeType;
      elemSize = readDtype.getSize();

    } else if (vinfo.typeInfo.hdfType == 3) { // char
      if (vinfo.mdt.byteSize > 1) {
        int[] newShape = new int[shape.length + 1];
        System.arraycopy(shape, 0, newShape, 0, shape.length);
        newShape[shape.length] = vinfo.mdt.byteSize;
        shape = newShape;
      }

    } else if (vinfo.typeInfo.hdfType == 5) { // opaque
      elemSize = vinfo.mdt.byteSize;

    } else if (vinfo.typeInfo.hdfType == 8) { // enum
      H5header.TypeInfo baseInfo = vinfo.typeInfo.base;
      readDtype = baseInfo.dataType;
      elemSize = readDtype.getSize();
      endian = baseInfo.endian;
    }

    Layout layout = new LayoutRegular(matt.dataPos, elemSize, shape, new Section(shape));
    Object data = h5iosp.readDataPrimitive(layout, dataType, shape, null, endian, false);
    Array dataArray;

    if ((dataType == DataType.CHAR)) {
      if (vinfo.mdt.byteSize > 1) { // chop back into pieces
        byte[] bdata = (byte[]) data;
        int strlen = vinfo.mdt.byteSize;
        int n = bdata.length / strlen;
        ArrayObject.D1 sarray = new ArrayObject.D1(String.class, n);
        for (int i = 0; i < n; i++) {
          String sval = convertString(bdata, i * strlen, strlen);
          sarray.set(i, sval);
        }
        dataArray = sarray;

      } else {
        String sval = convertString((byte[]) data);
        ArrayObject.D1 sarray = new ArrayObject.D1(String.class, 1);
        sarray.set(0, sval);
        dataArray = sarray;
      }

    } else {
      dataArray = (data instanceof Array) ? (Array) data : Array.factory(readDtype, shape, data);
    }

    // convert attributes to enum strings
    if ((vinfo.typeInfo.hdfType == 8) && (matt.mdt.map != null)) {
      dataArray = convertEnums(matt.mdt.map, dataType, dataArray);
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
    return new String(b, 0, count, CDM.utf8Charset); // all strings are considered to be UTF-8 unicode
  }

  private String convertString(byte[] b, int start, int len) throws UnsupportedEncodingException {
    // null terminates
    int count = start;
    while (count < start + len) {
      if (b[count] == 0) break;
      count++;
    }
    return new String(b, start, count - start, CDM.utf8Charset); // all strings are considered to be UTF-8 unicode
  }

  protected Array convertEnums(Map<Integer, String> map, DataType dataType, Array values) {
    Array result = Array.factory(DataType.STRING, values.getShape());
    IndexIterator ii = result.getIndexIterator();
    values.resetLocalIterator();
    while (values.hasNext()) {
      int ival;
      if (dataType == DataType.ENUM1)
        ival = (int) DataType.unsignedByteToShort(values.nextByte());
      else if (dataType == DataType.ENUM2)
        ival = DataType.unsignedShortToInt(values.nextShort());
      else
        ival = values.nextInt();
      String sval = map.get(ival);
      if (sval == null) sval = "Unknown enum value=" + ival;
      ii.setObjectNext(sval);
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
      log.debug("SKIPPING attribute " + attName + " for " + forWho + " with dataType= " + vinfo.typeInfo.hdfType);
      return null;
    }
    v.setSPobject(vinfo);
    vinfo.setOwner(v);
    v.setCaching(false);
    if (debug1) {
        log.debug("makeAttribute " + attName + " for " + forWho + "; vinfo= " + vinfo);
    }

    // read the data
    if ((mdt.type == 7) && attName.equals("DIMENSION_LIST")) { // convert to dimension names (LOOK is this netcdf4 specific?)
      if (mdt.referenceType == 0)
        data = readReferenceObjectNames(v);
      else {  // not doing reference regions here
        log.debug("SKIPPING attribute " + attName + " for " + forWho + " with referenceType= " + mdt.referenceType);
        return null;
      }

    } else {
      try {
        data = h5iosp.readData(v, v.getShapeAsSection());
      } catch (InvalidRangeException e) {
        log.error("H5header.makeAttribute", e);
        if (debug1) { log.debug("ERROR attribute " + e.getMessage()); }
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
     //log.debug("WARNING   Reference at "+dataPos+" referencedObjectPos = "+referencedObjectPos);

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
      log.debug("SKIPPING DataType= " + vinfo.typeInfo.hdfType + " for variable " + facade.name);
      return null;
    }

    // deal with filters, cant do SZIP
    if (facade.dobj.mfp != null) {
      for (Filter f : facade.dobj.mfp.filters) {
        if (f.id == 4) {
          log.debug("SKIPPING variable with SZIP Filter= " + facade.dobj.mfp + " for variable " + facade.name);
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
          fillAttribute = new Attribute(CDM.FILL_VALUE, (Number) fillValue, vinfo.typeInfo.unsigned);
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
      if (vname.startsWith(Nc4.NETCDF4_NON_COORD))
        vname = vname.substring(Nc4.NETCDF4_NON_COORD.length()); // skip prefix
      v = new Variable(ncfile, ncGroup, null, vname);
      if (!makeVariableShapeAndType(v, facade.dobj.mdt, facade.dobj.mds, vinfo, facade.dimList)) return null;
    }

    // special case of variable length strings
    if (v.getDataType() == DataType.STRING)
      v.setElementSize(16); // because the array has elements that are HeapIdentifier
    else if (v.getDataType() == DataType.OPAQUE) // special case of opaque
      v.setElementSize(facade.dobj.mdt.getBaseSize());

    v.setSPobject(vinfo);

    // look for attributes
    List<MessageAttribute> fatts = filterAttributes(facade.dobj.attributes);
    for (MessageAttribute matt : fatts) {
      try {
        makeAttributes(s, matt, v);
      } catch (InvalidRangeException e) {
        throw new IOException(e.getMessage());
      }
    }
    processSystemAttributes(facade.dobj.messages, v);
    if (fillAttribute != null && v.findAttribute(CDM.FILL_VALUE) == null)
      v.addAttribute(fillAttribute);
    if (vinfo.typeInfo.unsigned)
      v.addAttribute(new Attribute(CDM.UNSIGNED, "true"));
    if (facade.dobj.mdt.type == 5) {
      String desc = facade.dobj.mdt.opaque_desc;
      if ((desc != null) && (desc.length() > 0))
        v.addAttribute(new Attribute("_opaqueDesc", desc));
    }

    if (vinfo.isChunked) {// make the data btree, but entries are not read in
      vinfo.btree = new DataBTree(this, dataAddress, v.getShape(), vinfo.storageSize, memTracker);

      if (vinfo.isChunked) {  // add an attribute describing the chunk size
        List<Integer> chunksize = new ArrayList<>();
        for (int i = 0; i < vinfo.storageSize.length - 1; i++)  // skip last one - its the element size
          chunksize.add(vinfo.storageSize[i]);
        v.addAttribute(new Attribute(CDM.CHUNK_SIZES, chunksize));
      }
    }

    if (transformReference && (facade.dobj.mdt.type == 7) && (facade.dobj.mdt.referenceType == 0)) { // object reference
      // log.debug("transform object Reference: facade=" + facade.name +" variable name=" + v.getName());
      Array newData = findReferenceObjectNames(v.read());
      v.setDataType(DataType.STRING);
      v.setCachedData(newData, true); // so H5iosp.read() is never called
      v.addAttribute(new Attribute("_HDF5ReferenceType", "values are names of referenced Variables"));
    }

    if (transformReference && (facade.dobj.mdt.type == 7) && (facade.dobj.mdt.referenceType == 1)) { // region reference
      log.warn("transform region Reference: facade=" + facade.name + " variable name=" + v.getFullName());
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
    if ((vinfo.typeInfo.hdfType == 7) && warnings) {
      log.warn("  Variable " + facade.name + " is a Reference type");
    }
    if ((vinfo.mfp != null) && (vinfo.mfp.filters[0].id != 1) && warnings) {
      log.warn("  Variable " + facade.name + " has a Filter = " + vinfo.mfp);
    }
    if (debug1) {
      log.debug("makeVariable " + v.getFullName() + "; vinfo= " + vinfo);
    }

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
      if (dobj == null) {
        log.warn("readReferenceObjectNames cant find obj= {}", objId);
      }
      else {
        if (debugReference) {
            log.debug(" Referenced object= {}", dobj.who);
        }
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
        if (debug1) {
            log.debug("  made Member Variable " + v.getFullName() + "\n" + v);
        }
      }
    }
  }

  // Used for Structure Members
  private Variable makeVariableMember(Group g, Structure s, String name, long dataPos, MessageDatatype mdt)
          throws IOException {

    Variable v;
    Vinfo vinfo = new Vinfo(mdt, null, dataPos); // LOOK need mds
    if (vinfo.getNCDataType() == null) {
      log.debug("SKIPPING DataType= " + vinfo.typeInfo.hdfType + " for variable " + name);
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
    else if (v.getDataType() == DataType.OPAQUE) // special case of opaque
      v.setElementSize(mdt.getBaseSize());

    v.setSPobject(vinfo);
    vinfo.setOwner(v);

    if (vinfo.typeInfo.unsigned)
      v.addAttribute(new Attribute(CDM.UNSIGNED, "true"));

    return v;
  }

  private void processSystemAttributes(List<HeaderMessage> messages, AttributeContainer attContainer) {
    for (HeaderMessage mess : messages) {
      /* if (mess.mtype == MessageType.LastModified) {
        MessageLastModified m = (MessageLastModified) mess.messData;
        CalendarDate cd = CalendarDate.of((long) (m.secs * 1000));
        attributes.add(new Attribute("_lastModified", cd.toString()));

      } else if (mess.mtype == MessageType.LastModifiedOld) {
        MessageLastModifiedOld m = (MessageLastModifiedOld) mess.messData;
        try {
          Date d = getHdfDateFormatter().parse(m.datemod);
          CalendarDate cd = CalendarDate.of(d);
          attributes.add(new Attribute("_lastModified", cd.toString()));
        }
        catch (ParseException ex) {
          log.debug("ERROR parsing date from MessageLastModifiedOld = " + m.datemod);
        }

      } else */
      if (mess.mtype == MessageType.Comment) {
        MessageComment m = (MessageComment) mess.messData;
        attContainer.addAttribute(new Attribute("_comment", m.comment));
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

    boolean hasvlen = mdt.isVlen();

    // merge the shape for array type (10)
    if (mdt.type == 10) {
      int len = dim.length + mdt.dim.length;
      if (hasvlen) len++;
      int[] combinedDim = new int[len];
      System.arraycopy(dim, 0, combinedDim, 0, dim.length);
      System.arraycopy(mdt.dim, 0, combinedDim, dim.length, mdt.dim.length); // // type 10 is the inner dimensions
      if (hasvlen) combinedDim[len - 1] = -1;
      dim = combinedDim;
    }

    // set dimensions on the variable
    try {
      if (dims != null) { // dimensions were passed in
        if ((mdt.type == 9) && !mdt.isVString)
          v.setDimensions(dims + " *");
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

      } else if (mdt.isVlen()) { // variable length (not a string)

        if ((dim.length == 1) && (dim[0] == 1)) { // replace scalar with vlen
          int[] shape = new int[]{-1};
          v.setDimensionsAnonymous(shape);

        } else if (mdt.type != 10) { // add vlen dimension already done above for array
          int[] shape = new int[dim.length + 1];
          System.arraycopy(dim, 0, shape, 0, dim.length);
          shape[dim.length] = -1;
          v.setDimensionsAnonymous(shape);

        } else {
          v.setDimensionsAnonymous(dim);
        }

      } else { // all other cases

        v.setDimensionsAnonymous(dim);
      }

    } catch (InvalidRangeException ee) {
      log.error(ee.getMessage());
      log.debug("ERROR: makeVariableShapeAndType {}", ee.getMessage());
      return false;
    }

    // set the type
    DataType dt = vinfo.getNCDataType();
    if (dt == null) return false;
    v.setDataType(dt);

    // set the enumTypedef
    if (dt.isEnum()) {
      Group ncGroup = v.getParentGroup();
      EnumTypedef enumTypedef = ncGroup.findEnumeration(mdt.enumTypeName);
      if (enumTypedef == null) { // if shared object, wont have a name, shared version gets added later
        enumTypedef = new EnumTypedef(mdt.enumTypeName, mdt.map);
        // LOOK ncGroup.addEnumeration(enumTypedef);
      }
      v.setEnumTypedef(enumTypedef);
    }

    return true;
  }

  // Holder of all H5 specific information for a Variable, needed to do IO.
  public class Vinfo {
    Variable owner; // debugging
    DataObjectFacade facade; // debugging

    long dataPos; // for regular variables, needs to be absolute, with baseAddress added if needed
    // for member variables, is the offset from start of structure

    TypeInfo typeInfo;
    int[] storageSize;  // for type 1 (continuous) : mds.dimLength;
    // for type 2 (chunked)    : msl.chunkSize (last number is element size)
    // null for attributes

    boolean isvlen = false; // VLEN, but not vlenstring

    // chunked stuff
    boolean isChunked = false;
    DataBTree btree = null; // only if isChunked

    MessageDatatype mdt;
    MessageDataspace mds;
    MessageFilter mfp;

    boolean useFillValue = false;
    byte[] fillValue;

    public String getCompression() {
      if (mfp == null) return null;
      Formatter f = new Formatter();
      for (Filter filt : mfp.filters) {
        f.format("%s ", filt.name);
      }
      return f.toString();
    }

    public int[] getChunking() {
      return storageSize;
    }

    public boolean isChunked() {
      return isChunked;
    }

    public boolean useFillValue() {
      return useFillValue;
    }

    public long[] countStorageSize(Formatter f) throws IOException {
      long[] result = new long[2];
      if (btree == null) {
        if (f != null) f.format("btree is null%n");
        return result;
      }
      if (useFillValue) {
        if (f != null) f.format("useFillValue - no data is stored%n");
        return result;
      }

      int count = 0;
      long total = 0;
      DataBTree.DataChunkIterator iter = btree.getDataChunkIteratorFilter(null);
      while (iter.hasNext()) {
        DataBTree.DataChunk dc = iter.next();
        if (f != null) f.format(" %s%n", dc);
        total += dc.size;
        count++;
      }

      result[0] = total;
      result[1] = count;
      return result;
    }


    /**
     * Constructor
     *
     * @param facade DataObjectFacade: always has an mdt and an msl
     * @throws java.io.IOException on read error
     */
    Vinfo(DataObjectFacade facade) throws IOException {
      this.facade = facade;
      // LOOK if compact, do not use fileOffset
      this.dataPos = (facade.dobj.msl.type == 0) ? facade.dobj.msl.dataAddress : getFileOffset(facade.dobj.msl.dataAddress);
      this.mdt = facade.dobj.mdt;
      this.mds = facade.dobj.mds;
      this.mfp = facade.dobj.mfp;

      isvlen = this.mdt.isVlen();
      if (!facade.dobj.mdt.isOK && warnings) {
        log.debug("WARNING HDF5 file " + ncfile.getLocation() + " not handling " + facade.dobj.mdt);
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
     * @param mds     dataspace
     * @param dataPos start of data in file
     * @throws java.io.IOException on read error
     */
    Vinfo(MessageDatatype mdt, MessageDataspace mds, long dataPos) throws IOException {
      this.mdt = mdt;
      this.mds = mds;
      this.dataPos = dataPos;

      if (!mdt.isOK && warnings) {
        log.debug("WARNING HDF5 file " + ncfile.getLocation() + " not handling " + mdt);
        return; // not a supported datatype
      }

      isvlen = this.mdt.isVlen();

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
        tinfo.endian = ((flags[0] & 1) == 0) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;
        tinfo.unsigned = ((flags[0] & 8) == 0);

      } else if (hdfType == 1) { // floats, doubles
        tinfo.dataType = getNCtype(hdfType, byteSize);
        tinfo.endian = ((flags[0] & 1) == 0) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;

      } else if (hdfType == 2) { // time
        tinfo.dataType = DataType.STRING;
        tinfo.endian = ((flags[0] & 1) == 0) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;

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
        tinfo.endian = RandomAccessFile.LITTLE_ENDIAN;
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
          log.warn("Illegal byte suze for enum type = {}", tinfo.byteSize);
          throw new IllegalStateException("Illegal byte suze for enum type = " + tinfo.byteSize);
        }

        // enumMap = mdt.map;

      } else if (hdfType == 9) { // variable length array
        tinfo.isVString = mdt.isVString;
        tinfo.isVlen = mdt.isVlen;
        if (mdt.isVString) {
          tinfo.vpad = ((flags[0] >> 4) & 0xf);
          tinfo.dataType = DataType.STRING;
        } else {
          tinfo.dataType = getNCtype(mdt.getBaseType(), mdt.getBaseSize());
          tinfo.endian = mdt.base.endian;
          tinfo.unsigned = mdt.base.unsigned;
        }
      } else if (hdfType == 10) { // array : used for structure members
        tinfo.endian = (mdt.getFlags()[0] & 1) == 0 ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;
        if (mdt.isVString()) {
          tinfo.dataType = DataType.STRING;
        } else {
          int basetype = mdt.getBaseType();
          tinfo.dataType = getNCtype(basetype, mdt.getBaseSize());
        }
      } else if (warnings) {
        log.debug("WARNING not handling hdf dataType = " + hdfType + " size= " + byteSize);
      }

      if (mdt.base != null) {
        tinfo.base = calcNCtype(mdt.base);
      }
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
          log.debug("WARNING HDF5 file " + ncfile.getLocation() + " not handling hdf integer type (" + hdfType + ") with size= " + size);
          log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf integer type (" + hdfType + ") with size= " + size);
          return null;
        }

      } else if (hdfType == 1) {
        if (size == 4)
          return DataType.FLOAT;
        else if (size == 8)
          return DataType.DOUBLE;
        else if (warnings) {
          log.debug("WARNING HDF5 file " + ncfile.getLocation() + " not handling hdf float type with size= " + size);
          log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf float type with size= " + size);
          return null;
        }

      } else if (hdfType == 3) {  // fixed length strings. String is used for Vlen type = 1
        return DataType.CHAR;

      } else if (hdfType == 7) { // reference
        return DataType.LONG;

      } else if (warnings) {
        log.debug("WARNING not handling hdf type = " + hdfType + " size= " + size);
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
      if (typeInfo.endian >= 0)
        buff.append((typeInfo.endian == RandomAccessFile.LITTLE_ENDIAN) ? " LittleEndian" : " BigEndian");
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
      if (typeInfo.endian >= 0)
        bbuff.order(typeInfo.endian == RandomAccessFile.LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

      if ((typeInfo.dataType == DataType.SHORT) || (typeInfo.dataType == DataType.ENUM2)) {
        ShortBuffer tbuff = bbuff.asShortBuffer();
        return tbuff.get();

      } else if ((typeInfo.dataType == DataType.INT) || (typeInfo.dataType == DataType.ENUM4)) {
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
        log.debug("WARNING HDF5 file " + ncfile.getLocation() + " not handling hdf integer type (" + hdfType + ") with size= " + size);
        log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf integer type (" + hdfType + ") with size= " + size);
        return null;
      }

    } else if (hdfType == 1) {
      if (size == 4)
        return DataType.FLOAT;
      else if (size == 8)
        return DataType.DOUBLE;
      else if (warnings) {
        log.debug("WARNING HDF5 file " + ncfile.getLocation() + " not handling hdf float type with size= " + size);
        log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf float type with size= " + size);
        return null;
      }

    } else if (hdfType == 3) {  // fixed length strings. String is used for Vlen type = 1
      return DataType.CHAR;

    } else if (hdfType == 6) {
      return DataType.STRUCTURE;

    } else if (hdfType == 7) { // reference
      return DataType.LONG;

    } else if (warnings) {
      log.debug("WARNING not handling hdf type = " + hdfType + " size= " + size);
      log.warn("HDF5 file " + ncfile.getLocation() + " not handling hdf type = " + hdfType + " size= " + size);
    }
    return null;
  }

  public static class TypeInfo {
    int hdfType, byteSize;
    DataType dataType;
    int endian = -1;   // 1 = RandomAccessFile.LITTLE_ENDIAN || 0 = RandomAccessFile.BIG_ENDIAN
    boolean unsigned;
    boolean isVString; // is it a vlen string
    boolean isVlen;    // vlen but not string
    int vpad;          // string padding
    TypeInfo base;     // vlen, enum

    TypeInfo(int hdfType, int byteSize) {
      this.hdfType = hdfType;
      this.byteSize = byteSize;
    }

    public String toString() {
      StringBuilder buff = new StringBuilder();
      buff.append("hdfType=").append(hdfType).append(" byteSize=").append(byteSize).append(" dataType=").append(dataType);
      buff.append(" unsigned=").append(unsigned).append(" isVString=").append(isVString).append(" vpad=").append(vpad).append(" endian=").append(endian);
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
    boolean is2DCoordinate;
    boolean hasNetcdfDimensions;

    // is a group
    H5Group group;

    // or a variable
    String dimList; // list of dimension names for this variable

    // or a link
    String linkName = null;

    // _Netcdf4Coordinates att.
    // Attribute netcdf4CoordinatesAtt;

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
        log.debug("WARNING Unknown DataObjectFacade = {}", this);
        // return;
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
    List<DataObjectFacade> nestedObjects = new ArrayList<>(); // nested data objects
    Map<String, Dimension> dimMap = new HashMap<>();
    List<Dimension> dimList = new ArrayList<>(); // need to track dimension order

    // "Data Object Header" Level 2A
    // read a Data Object Header
    // no side effects, can be called multiple time for debugging
    private H5Group(DataObjectFacade facade) throws IOException {
      this.facade = facade;
      this.parent = facade.parent;
      this.name = facade.name;
      displayName = (name.length() == 0) ? "root" : name;

      // if has a "group message", then its an "old group"
      if (facade.dobj.groupMessage != null) {
        // check for hard links
        if (debugHardLink) {
            log.debug("HO look for group address = {}", facade.dobj.groupMessage.btreeAddress);
        }
        if (null != (facade.group = hashGroups.get(facade.dobj.groupMessage.btreeAddress))) {
          if (debugHardLink) {
            log.debug("WARNING hard link to group = {}", facade.group.getName());
          }
          if (parent.isChildOf(facade.group)) {
            if (debugHardLink) {
                log.debug("ERROR hard link to group create a loop = {}", facade.group.getName());
            }
            log.debug("Remove hard link to group that creates a loop = {}", facade.group.getName());
            facade.group = null;
            return;
          }
        }

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

    @Override
    public String toString() {
      return displayName;
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
      List<HeaderMessage> result = new ArrayList<>(100);
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
    List<HeaderMessage> messages = new ArrayList<>();
    List<MessageAttribute> attributes = new ArrayList<>();

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

    public void show(Formatter f) throws IOException {
      if (mdt != null) {
        f.format("%s ", mdt.getType());
      }
      f.format("%s", getName());
      if (mds != null) {
        f.format("(");
        for (int len : mds.dimLength)
          f.format("%d,", len);
        f.format(");%n");
      }
      for (H5header.MessageAttribute mess : getAttributes()) {
        Attribute att = mess.getNcAttribute();
        f.format("  :%s%n", att);
      }
      f.format("%n");
    }

    // "Data Object Header" Level 2A
    // read a Data Object Header
    // no side effects, can be called multiple time for debugging


    private DataObject(long address, String who) throws IOException {
      this.address = address;
      this.who = who;

      if (debug1) {
        log.debug("\n--> DataObject.read parsing <" + who + "> object ID/address=" + address);
      }
      if (debugPos) {
        log.debug("      DataObject.read now at position=" + raf.getFilePointer() + " for <" + who + "> reposition to " + getFileOffset(address));
      }
      //if (offset < 0) return null;
      raf.seek(getFileOffset(address));

      version = raf.readByte();
      if (version == 1) { // Level 2A1 (first part, before the messages)
        raf.readByte(); // skip byte
        short nmess = raf.readShort();
        if (debugDetail) {
            log.debug(" version=" + version + " nmess=" + nmess);
        }

        int referenceCount = raf.readInt();
        int headerSize = raf.readInt();
        if (debugDetail) {
            log.debug(" referenceCount=" + referenceCount + " headerSize=" + headerSize);
        }

        //if (referenceCount > 1)
        //  log.debug("WARNING referenceCount="+referenceCount);
        raf.skipBytes(4); // header messages multiples of 8

        long posMess = raf.getFilePointer();
        int count = readMessagesVersion1(posMess, nmess, Integer.MAX_VALUE, this.who);
        if (debugContinueMessage) {
            log.debug(" nmessages read = {}", count);
        }
        if (debugPos) {
            log.debug("<--done reading messages for <" + who + ">; position=" + raf.getFilePointer());
        }
        if (debugTracker) memTracker.addByLen("Object " + who, getFileOffset(address), headerSize + 16);

      } else { // level 2A2 (first part, before the messages)
        // first byte was already read
        String magic = raf.readString(3);
        if (!magic.equals("HDR"))
          throw new IllegalStateException("DataObject doesnt start with OHDR");

        version = raf.readByte();
        byte flags = raf.readByte(); // data object header flags (version 2)
        if (debugDetail) {
            log.debug(" version=" + version + " flags=" + Integer.toBinaryString(flags));
        }

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
        if (debugDetail) {
            log.debug(" sizeOfChunk=" + sizeOfChunk);
        }

        long posMess = raf.getFilePointer();
        int count = readMessagesVersion2(posMess, sizeOfChunk, (flags & 4) != 0, this.who);
        if (debugContinueMessage) {
            log.debug(" nmessages read = {}", count);
        }
        if (debugPos) {
            log.debug("<--done reading messages for <" + who + ">; position=" + raf.getFilePointer());
        }
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

      if (debug1) {
        log.debug("<-- end DataObject {}", who);
      }
    }

    private void processAttributeInfoMessage(MessageAttributeInfo attInfo, List<MessageAttribute> list) throws IOException {
      long btreeAddress = (attInfo.v2BtreeAddressCreationOrder > 0) ? attInfo.v2BtreeAddressCreationOrder : attInfo.v2BtreeAddress;
      if ((btreeAddress < 0) || (attInfo.fractalHeapAddress < 0))
        return;

      BTree2 btree = new BTree2(H5header.this, who, btreeAddress);
      FractalHeap fractalHeap = new FractalHeap(H5header.this, who, attInfo.fractalHeapAddress, memTracker);

      for (BTree2.Entry2 e : btree.entryList) {
        byte[] heapId;
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
        FractalHeap.DHeapId fractalHeapId = fractalHeap.getFractalHeapId(heapId);
        long pos = fractalHeapId.getPos();
        if (pos > 0) {
          MessageAttribute attMessage = new MessageAttribute();
          if (attMessage.read(pos))
            list.add(attMessage);
          if (debugBtree2) {
            log.debug("    attMessage={}", attMessage);
          }
        }
      }
    }

    // read messages, starting at pos, until you hit maxMess read, or maxBytes read
    // if you hit a continuation message, call recursively
    // return number of messaages read
    private int readMessagesVersion1(long pos, int maxMess, int maxBytes, String objectName) throws IOException {
      if (debugContinueMessage) {
        log.debug(" readMessages start at =" + pos + " maxMess= " + maxMess + " maxBytes= " + maxBytes);
      }

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
        if (debugContinueMessage) {
            log.debug("   count=" + count + " bytesRead=" + bytesRead);
        }

        // if we hit a continuation, then we go into nested reading
        if (mess.mtype == MessageType.ObjectHeaderContinuation) {
          MessageContinue c = (MessageContinue) mess.messData;
          if (debugContinueMessage) {
            log.debug(" ---ObjectHeaderContinuation--- ");
          }
          count += readMessagesVersion1(getFileOffset(c.offset), maxMess - count, (int) c.length, objectName);
          if (debugContinueMessage) {
            log.debug(" ---ObjectHeaderContinuation return --- ");
          }
        } else if (mess.mtype != MessageType.NIL) {
          messages.add(mess);
        }
      }
      return count;
    }

    private int readMessagesVersion2(long filePos, long maxBytes, boolean creationOrderPresent, String objectName) throws IOException {
      if (debugContinueMessage) {
        log.debug(" readMessages2 starts at ={} maxBytes= {}", filePos, maxBytes);
      }

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
        if (debugContinueMessage) {
          log.debug("   mess size=" + n + " bytesRead=" + bytesRead + " maxBytes=" + maxBytes);
        }

        // if we hit a continuation, then we go into nested reading
        if (mess.mtype == MessageType.ObjectHeaderContinuation) {
          MessageContinue c = (MessageContinue) mess.messData;
          long continuationBlockFilePos = getFileOffset(c.offset);
          if (debugContinueMessage) {
            log.debug(" ---ObjectHeaderContinuation filePos= {}", continuationBlockFilePos);
          }

          raf.seek(continuationBlockFilePos);
          String sig = readStringFixedLength(4);
          if (!sig.equals("OCHK"))
            throw new IllegalStateException(" ObjectHeaderContinuation Missing signature");

          count += readMessagesVersion2(continuationBlockFilePos + 4, (int) c.length - 8, creationOrderPresent, objectName);
          if (debugContinueMessage) {
            log.debug(" ---ObjectHeaderContinuation return --- ");
            log.debug("   continuationMessages =" + count + " bytesRead=" + bytesRead + " maxBytes=" + maxBytes);
          }
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

      if (debug1) { log.debug("   Continue offset=" + offset + " length=" + length); }
    } */

  } // DataObject

  // type safe enum
  static public class MessageType {
    private static int MAX_MESSAGE = 23;
    private static java.util.Map<String, MessageType> hash = new java.util.HashMap<>(10);
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
  public class HeaderMessage implements Comparable<HeaderMessage> {
    long start;
    byte headerMessageFlags;
    int size;
    short type, header_length;
    Named messData; // header message data

    public MessageType getMtype() {
      return mtype;
    }

    public String getName() {
      return messData.getName();
    }

    public int getSize() {
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
      if (debugPos) {
        log.debug("  --> Message Header starts at =" + raf.getFilePointer());
      }

      if (version == 1) {
        type = raf.readShort();
        size = DataType.unsignedShortToInt(raf.readShort());
        headerMessageFlags = raf.readByte();
        raf.skipBytes(3);
        header_length = 8;

      } else {
        type = (short) raf.readByte();
        size = DataType.unsignedShortToInt(raf.readShort());
        //if (size > Short.MAX_VALUE)
        //  log.debug("HEY");

        headerMessageFlags = raf.readByte();
        header_length = 4;
        if (creationOrderPresent) {
          creationOrder = raf.readShort();
          header_length += 2;
        }
      }
      mtype = MessageType.getType(type);
      if (debug1) {
        log.debug("  -->" + mtype + " messageSize=" + size + " flags = " + Integer.toBinaryString(headerMessageFlags));
        if (creationOrderPresent && debugCreationOrder) {
            log.debug("     creationOrder = " + creationOrder);
        }
      }
      if (debugPos) {
        log.debug("  --> Message Data starts at=" + raf.getFilePointer());
      }

      if ((headerMessageFlags & 2) != 0) { // shared
        messData = getSharedDataObject(mtype).mdt; // eg a shared datatype, eg enums
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
        data.read(raf.getFilePointer());
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
        log.debug("****UNPROCESSED MESSAGE type = " + mtype + " raw = " + type);
        log.warn("SKIP UNPROCESSED MESSAGE type = " + mtype + " raw = " + type);
        //throw new UnsupportedOperationException("****UNPROCESSED MESSAGE type = " + mtype + " raw = " + type);
      }

      return header_length + size;
    }

    public int compareTo(HeaderMessage o) {
      return Short.compare(type, o.type);
    }

    public String toString() {
      return "message type = " + mtype + "; " + messData;
    }

    // debugging
    public void showFractalHeap(Formatter f) {
      if (mtype != H5header.MessageType.AttributeInfo) {
        f.format("No fractal heap");
        return;
      }

      MessageAttributeInfo info = (MessageAttributeInfo) messData;
      info.showFractalHeap(f);
    }

    // debugging
    public void showCompression(Formatter f) {
      if (mtype != H5header.MessageType.AttributeInfo) {
        f.format("No fractal heap");
        return;
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
      if (debug1) {
        log.debug("     Shared Message " + sharedVersion + " type=" + sharedType + " heapId = " + heapId);
      }
      if (debugPos) {
        log.debug("  --> Shared Message reposition to =" + raf.getFilePointer());
      }
      // dunno where is the file's shared object header heap ??
      throw new UnsupportedOperationException("****SHARED MESSAGE type = " + mtype + " heapId = " + heapId);

    } else {
      long address = readOffset();
      if (debug1) {
        log.debug("     Shared Message " + sharedVersion + " type=" + sharedType + " address = " + address);
      }
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
      if (dimLength != null) {
        sbuff.format(" length=(");
        for (int size : dimLength) sbuff.format("%d,", size);
        sbuff.format(") ");
      }
      if (maxLength != null) {
        sbuff.format("max=(");
        for (int aMaxLength : maxLength) sbuff.format("%d,", aMaxLength);
        sbuff.format(")");
      }
      return sbuff.toString();
    }

    void read() throws IOException {
      if (debugPos) {
        log.debug("   *MessageSimpleDataspace start pos= " + raf.getFilePointer());
      }

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

      if (debug1) {
        log.debug("   SimpleDataspace version= " + version + " flags=" +
              Integer.toBinaryString(flags) + " ndims=" + ndims + " type=" + type);
      }

      /* if (ndims == 0 && !alreadyWarnNdimZero) {
        log.warn("ndims == 0 in HDF5 file= " + raf.getLocation());
        alreadyWarnNdimZero = true;
      }  */

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
        for (int i = 0; i < ndims; i++) {
          log.debug("    dim length = " + dimLength[i] + " max = " + maxLength[i]);
        }
      }
    }
  }

  // Message Type 17/0x11 "Old Group" or "Symbol Table"
  private class MessageGroup implements Named {
    long btreeAddress, nameHeapAddress;

    void read() throws IOException {
      btreeAddress = readOffset();
      nameHeapAddress = readOffset();
      if (debug1) {
        log.debug("   Group btreeAddress=" + btreeAddress + " nameHeapAddress=" + nameHeapAddress);
      }
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
          f.format("%n%n");
          FractalHeap fractalHeap = new FractalHeap(H5header.this, "", fractalHeapAddress, memTracker);
          fractalHeap.showDetails(f);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      return f.toString();
    }

    void read() throws IOException {
      if (debugPos) {
        log.debug("   *MessageGroupNew start pos= " + raf.getFilePointer());
      }
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

      if (debug1) {
        log.debug("   MessageGroupNew version= " + version + " flags = " + flags + this);
      }
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
      if (debugPos) {
        log.debug("   *MessageGroupInfo start pos= " + raf.getFilePointer());
      }
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

      if (debug1) {
        log.debug("   MessageGroupInfo version= " + version + " flags = " + flags + this);
      }
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
      if (debugPos) {
        log.debug("   *MessageLink start pos= {}", raf.getFilePointer());
      }
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

      if (debug1) {
        log.debug("   MessageLink version= " + version + " flags = " + Integer.toBinaryString(flags) + this);
      }
    }

    public String getName() {
      return linkName;
    }
  }

  // Message Type 3 : "Datatype"
  public class MessageDatatype implements Named {
    int type, version;
    byte[] flags = new byte[3];
    int byteSize;
    int endian; // 0 (LE) or 1 (BE) == RandomAccessFile.XXXXXX_ENDIAN
    boolean isOK = true;
    boolean unsigned;

    // time (2)
    DataType timeType;

    // opaque (5)
    String opaque_desc;

    // compound type (6)
    List<StructureMember> members;

    // reference (7)
    int referenceType; // 0 = object, 1 = region

    // enums (8)
    Map<Integer, String> map;
    String enumTypeName;

    // enum, variable-length, array types have "base" DataType
    MessageDatatype base;
    boolean isVString; // variable length (not a string)
    boolean isVlen; // vlen but not string

    // array (10)
    int[] dim;

    public String toString() {
      Formatter f = new Formatter();
      f.format(" datatype= %d", type);
      f.format(" byteSize= %d", byteSize);
      DataType dtype = getNCtype(type, byteSize);
      f.format(" NCtype= %s %s", dtype, unsigned ? "(unsigned)" : "");
      f.format(" flags= ");
      for (int i = 0; i < 3; i++) f.format(" %d", flags[i]);
      f.format(" endian= %s", endian == RandomAccessFile.BIG_ENDIAN ? "BIG" : "LITTLE");

      if (type == 2)
        f.format(" timeType= %s", timeType);
      else if (type == 6) {
        f.format("%n  members%n");
        for (StructureMember mm : members)
          f.format("   %s%n", mm);
      } else if (type == 7)
        f.format(" referenceType= %s", referenceType);
      else if (type == 9) {
        f.format(" isVString= %s", isVString);
        f.format(" isVlen= %s", isVlen);
      }
      if ((type == 9) || (type == 10))
        f.format(" parent base= {%s}", base);
      return f.toString();
    }

    public String getName() {
      DataType dtype = getNCtype(type, byteSize);
      if (dtype != null)
        return dtype.toString() + " size= " + byteSize;
      else
        return "type=" + Integer.toString(type) + " size= " + byteSize;
    }

    public String getType() {
      DataType dtype = getNCtype(type, byteSize);
      if (dtype != null)
        return dtype.toString();
      else
        return "type=" + Integer.toString(type) + " size= " + byteSize;
    }

    void read(String objectName) throws IOException {
      if (debugPos) {
        log.debug("   *MessageDatatype start pos= {}", raf.getFilePointer());
      }

      byte tandv = raf.readByte();
      type = (tandv & 0xf);
      version = ((tandv & 0xf0) >> 4);

      raf.readFully(flags);
      byteSize = raf.readInt();
      endian = ((flags[0] & 1) == 0) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;

      if (debug1) {
        log.debug("   Datatype type=" + type + " version= " + version + " flags = " +
              flags[0] + " " + flags[1] + " " + flags[2] + " byteSize=" + byteSize
              + " byteOrder=" + (endian == RandomAccessFile.BIG_ENDIAN ? "BIG" : "LITTLE"));
      }

      if (type == 0) {  // fixed point
        unsigned = ((flags[0] & 8) == 0);
        short bitOffset = raf.readShort();
        short bitPrecision = raf.readShort();
        if (debug1) {
          log.debug("   type 0 (fixed point): bitOffset= " + bitOffset + " bitPrecision= " + bitPrecision + " unsigned= " + unsigned);
        }
        isOK = (bitOffset == 0) && (bitPrecision % 8 == 0);

      } else if (type == 1) {  // floating point
        short bitOffset = raf.readShort();
        short bitPrecision = raf.readShort();
        byte expLocation = raf.readByte();
        byte expSize = raf.readByte();
        byte manLocation = raf.readByte();
        byte manSize = raf.readByte();
        int expBias = raf.readInt();
        if (debug1) {
          log.debug("   type 1 (floating point): bitOffset= " + bitOffset + " bitPrecision= " + bitPrecision +
                  " expLocation= " + expLocation + " expSize= " + expSize + " manLocation= " + manLocation +
                  " manSize= " + manSize + " expBias= " + expBias);
        }
      } else if (type == 2) {  // time
        short bitPrecision = raf.readShort();
        if (bitPrecision == 16)
          timeType = DataType.SHORT;
        else if (bitPrecision == 32)
          timeType = DataType.INT;
        else if (bitPrecision == 64)
          timeType = DataType.LONG;

        if (debug1) {
          log.debug("   type 2 (time): bitPrecision= " + bitPrecision + " timeType = " + timeType);
        }

      } else if (type == 3) {         // string  (I think a fixed length seq of chars)
        int ptype = flags[0] & 0xf;
        if (debug1) {
          log.debug("   type 3 (String): pad type= " + ptype);
        }

      } else if (type == 4) { // bit field
        short bitOffset = raf.readShort();
        short bitPrecision = raf.readShort();
        if (debug1) {
          log.debug("   type 4 (bit field): bitOffset= " + bitOffset + " bitPrecision= " + bitPrecision);
        }
        //isOK = (bitOffset == 0) && (bitPrecision % 8 == 0);  LOOK

      } else if (type == 5) { // opaque
        byte len = flags[0];
        opaque_desc = (len > 0) ? readString(raf).trim() : null;
        if (debug1) {
            log.debug("   type 5 (opaque): len= " + len + " desc= " + opaque_desc);
        }

      } else if (type == 6) { // compound
        int nmembers = makeUnsignedIntFromBytes(flags[1], flags[0]);
        if (debug1) {
            log.debug("   --type 6(compound): nmembers={}", nmembers);
        }
        members = new ArrayList<>();
        for (int i = 0; i < nmembers; i++) {
          members.add(new StructureMember(version, byteSize));
        }
        if (debugDetail) {
            log.debug("   --done with compound type");
        }

      } else if (type == 7) { // reference
        referenceType = flags[0] & 0xf;
        if (debug1 || debugReference) {
            log.debug("   --type 7(reference): type= {}", referenceType);
        }

      } else if (type == 8) { // enums
        int nmembers = makeUnsignedIntFromBytes(flags[1], flags[0]);
        boolean saveDebugDetail = debugDetail;
        if (debug1 || debugEnum) {
          log.debug("   --type 8(enums): nmembers={}", nmembers);
          debugDetail = true;
        }
        base = new MessageDatatype(); // base type
        base.read(objectName);
        debugDetail = saveDebugDetail;

        // read the enums

        String[] enumName = new String[nmembers];
        for (int i = 0; i < nmembers; i++) {
          if (version < 3)
            enumName[i] = readString8(raf); // padding
          else
            enumName[i] = readString(raf);  // no padding
        }

        // read the values; must switch to base byte order (!)
        if (base.endian >= 0) { raf.order(base.endian); }
        int[] enumValue = new int[nmembers];
        for (int i = 0; i < nmembers; i++) {
          enumValue[i] = (int) readVariableSizeUnsigned(base.byteSize); // assume size is 1, 2, or 4
        }
        raf.order(RandomAccessFile.LITTLE_ENDIAN);

        enumTypeName = objectName;
        map = new TreeMap<>();
        for (int i = 0; i < nmembers; i++)
          map.put(enumValue[i], enumName[i]);

        if (debugEnum) {
          for (int i = 0; i < nmembers; i++) {
            log.debug("   " + enumValue[i] + "=" + enumName[i]);
          }
        }

      } else if (type == 9) { // String (A variable-length sequence of characters) or Sequence (A variable-length sequence of any datatype)
        isVString = (flags[0] & 0xf) == 1;
        if (!isVString) { isVlen = true; }
        if (debug1) {
          log.debug("   type 9(variable length): type= {}",
                        ((isVString ? "string" : "sequence of type:")) );
        }
        base = new MessageDatatype(); // base type
        base.read(objectName);

      } else if (type == 10) { // array
        if (debug1) {
            debugOut.print("   type 10(array) lengths= ");
        }
        int ndims = (int) raf.readByte();
        if (version < 3) { raf.skipBytes(3); }

        dim = new int[ndims];
        for (int i = 0; i < ndims; i++) {
          dim[i] = raf.readInt();
          if (debug1) {
            debugOut.print(" " + dim[i]);
          }
        }

        if (version < 3) {  // not present in version 3, never used anyway
          int[] pdim = new int[ndims];
          for (int i = 0; i < ndims; i++)
            pdim[i] = raf.readInt();
        }
        if (debug1) {
            log.debug("");
        }

        base = new MessageDatatype(); // base type
        base.read(objectName);

      } else if (warnings) {
        log.debug(" WARNING not dealing with type= {}", type);
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

    boolean isVlen() {
      return (type == 10 ? base.isVlen() : isVlen);
    }

    boolean isVString() {
      return (type == 10 ? base.isVString() : isVString);
    }
  }

  private class StructureMember {
    String name;
    int offset;
    byte dims;
    MessageDatatype mdt;

    StructureMember(int version, int byteSize) throws IOException {
      if (debugPos) {
        log.debug("   *StructureMember now at position={}", raf.getFilePointer());
      }

      name = readString(raf);
      if (version < 3) {
        raf.skipBytes(padding(name.length() + 1, 8));
        offset = raf.readInt();
      } else {
        offset = (int) readVariableSizeMax(byteSize);
      }

      if (debug1) {
        log.debug("   Member name=" + name + " offset= " + offset);
      }

      if (version == 1) {
        dims = raf.readByte();
        raf.skipBytes(3);
        raf.skipBytes(24); // ignore dimension info for now
      }

      //HDFdumpWithCount(buffer, raf.getFilePointer(), 16);
      mdt = new MessageDatatype();
      mdt.read(name);
      if (debugDetail) {
        log.debug("   ***End Member name={}", name);
      }

      // ??
      //HDFdump(ncfile.out, "Member end", buffer, 16);
      //if (HDFdebug)  ncfile.log.debug("   Member pos="+raf.getFilePointer());
      //HDFpadToMultiple( buffer, 8);
      //if (HDFdebug)  ncfile.log.debug("   Member padToMultiple="+raf.getFilePointer());
      //raf.skipBytes( 4); // huh ??
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("StructureMember");
      sb.append("{name='").append(name).append('\'');
      sb.append(", offset=").append(offset);
      sb.append(", dims=").append(dims);
      sb.append(", mdt=").append(mdt);
      sb.append('}');
      return sb.toString();
    }
  }

  // Message Type 4 "Fill Value Old" : fill value is stored in the message
  private class MessageFillValueOld implements Named {
    byte[] value;
    int size;

    void read() throws IOException {
      size = raf.readInt();
      value = new byte[size];
      raf.readFully(value);

      if (debug1) {
        log.debug("{}", this);
      }
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
          raf.readFully(value);
          hasFillValue = true;
        } else {
          hasFillValue = false;
        }
      }

      if (debug1) {
        log.debug("{}", this);
      }
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
  class MessageLayout implements Named {
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

      if (debug1) {
        log.debug("   StorageLayout version= " + version + this);
      }
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

      if (debug1) {
        log.debug("   MessageFilter version=" + version + this);
      }
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

  private static final String[] filterName = new String[]{"", "deflate", "shuffle", "fletcher32", "szip", "nbit", "scaleoffset"};

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
      if ((version == 1) && (nValues & 1) != 0)   // check if odd
        raf.skipBytes(4);

      if (debug1) {
        log.debug("{}", this);
      }
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

  // Message Type 12/0xC "Attribute" : define an Attribute
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

    public long getDataPosAbsolute() {
      return dataPos;
    }

    public Attribute getNcAttribute() throws IOException {
      return makeAttribute(this);
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

    boolean read(long pos) throws IOException {
      raf.seek(pos);
      if (debugPos) {
        log.debug("   *MessageAttribute start pos= {}", raf.getFilePointer());
      }
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

      } else if (version == 72) {
        flags = raf.readByte();
        nameSize = raf.readShort();
        typeSize = raf.readShort();
        spaceSize = raf.readShort();
        log.error("HDF5 MessageAttribute found bad version " + version + " at filePos " + raf.getFilePointer());
        // G:/work/galibert/IMOS_ANMN-NSW_AETVZ_20131127T230000Z_PH100_FV01_PH100-1311-Workhorse-ADCP-109.5_END-20140306T010000Z_C-20140521T053527Z.nc
        // E:/work/antonio/2014_ch.nc
        // return false;
      } else {
        log.error("bad version " + version + " at filePos " + raf.getFilePointer()); // buggery, may be HDF5 "more than 8 attributes" error
        return false;
        // throw new IllegalStateException("MessageAttribute unknown version " + version);
      }

      // read the attribute name
      long filePos = raf.getFilePointer();
      name = readString(raf); // read at current pos
      if (version == 1) nameSize += padding(nameSize, 8);
      raf.seek(filePos + nameSize); // make it more robust for errors

      if (debug1) {
        log.debug("   MessageAttribute version= " + version + " flags = " + Integer.toBinaryString(flags) +
                " nameSize = " + nameSize + " typeSize=" + typeSize + " spaceSize= " + spaceSize + " name= " + name);
      }

      // read the datatype
      filePos = raf.getFilePointer();
      if (debugPos) {
        log.debug("   *MessageAttribute before mdt pos= {}", filePos);
      }
      boolean isShared = (flags & 1) != 0;
      if (isShared) {
        mdt = getSharedDataObject(MessageType.Datatype).mdt;
        if (debug1) {
            log.debug("    MessageDatatype: {}", mdt);
        }
      } else {
        mdt.read(name);
        if (version == 1) typeSize += padding(typeSize, 8);
      }
      raf.seek(filePos + typeSize); // make it more robust for errors

      // read the dataspace
      filePos = raf.getFilePointer();
      if (debugPos) {
        log.debug("   *MessageAttribute before mds = {}", filePos);
      }
      mds.read();
      if (version == 1) spaceSize += padding(spaceSize, 8);
      raf.seek(filePos + spaceSize); // make it more robust for errors

      // the data starts immediately afterward - ie in the message
      dataPos = raf.getFilePointer();   // note this is absolute position (no offset needed)
      if (debug1) {
        log.debug("   *MessageAttribute dataPos= {}", dataPos);
      }
      return true;
    }
  }  // MessageAttribute

  // Message Type 21/0x15 "Attribute Info" (version 2)
  private class MessageAttributeInfo implements Named {
    byte flags;
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
        f.format(" v2BtreeAddressCreationOrder=%d", v2BtreeAddressCreationOrder);

      showFractalHeap(f);

      return f.toString();
    }

    void showFractalHeap(Formatter f) {
      long btreeAddress = (v2BtreeAddressCreationOrder > 0) ? v2BtreeAddressCreationOrder : v2BtreeAddress;
      if ((fractalHeapAddress > 0) && (btreeAddress > 0)) {
        try {
          FractalHeap fractalHeap = new FractalHeap(H5header.this, "", fractalHeapAddress, memTracker);
          fractalHeap.showDetails(f);

          f.format(" Btree:%n");
          f.format("  type n m  offset size pos       attName%n");

          BTree2 btree = new BTree2(H5header.this, "", btreeAddress);
          for (BTree2.Entry2 e : btree.entryList) {
            byte[] heapId;
            switch (btree.btreeType) {
              case 8:
                heapId = ((BTree2.Record8) e.record).heapId;
                break;
              case 9:
                heapId = ((BTree2.Record9) e.record).heapId;
                break;
              default:
                f.format(" unknown btreetype %d%n", btree.btreeType);
                continue;
            }

            // the heapId points to an Attribute Message in the fractal Heap
            FractalHeap.DHeapId dh = fractalHeap.getFractalHeapId(heapId);
            f.format("   %2d %2d %2d %6d %4d %8d", dh.type, dh.n, dh.m, dh.offset, dh.size, dh.getPos());
            if (dh.getPos() > 0) {
              MessageAttribute attMessage = new MessageAttribute();
              attMessage.read(dh.getPos());
              f.format(" %-30s", trunc(attMessage.getName(), 30));
            }
            f.format(" heapId=:");
            Misc.showBytes(heapId, f);
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
      if (debugPos) {
        log.debug("   *MessageAttributeInfo start pos= {}", raf.getFilePointer());
      }
      byte version = raf.readByte();
      byte flags = raf.readByte();
      if ((flags & 1) != 0)
        maxCreationIndex = raf.readShort();

      fractalHeapAddress = readOffset();
      v2BtreeAddress = readOffset();

      if ((flags & 2) != 0)
        v2BtreeAddressCreationOrder = readOffset();

      if (debug1) {
        log.debug("   MessageAttributeInfo version= " + version + " flags = " + flags + this);
      }
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
      return new Date( (long) secs * 1000).toString();
    }

    public String getName() {
      return toString();
    }
  }

  // Message Type 14/0xE ("Last Modified (old)" : last modified date represented as a String YYMM etc. use message type 18 instead
  private class MessageLastModifiedOld implements Named {
    String datemod;

    void read() throws IOException {
      datemod = raf.readString(14);
      if (debug1) {
        log.debug("   MessageLastModifiedOld={}", datemod);
      }
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
      if (debug1) {
        log.debug("   Continue offset=" + offset + " length=" + length);
      }
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
      if (debug1) {
        log.debug("   ObjectReferenceCount={}", refCount);
      }
    }

    public String getName() {
      return Integer.toString(refCount);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  // Groups

  private void readGroupNew(H5Group group, MessageGroupNew groupNewMessage, DataObject dobj) throws IOException {
    if (debug1) {
        log.debug("\n--> GroupNew read <{}>", group.displayName);
    }

    if (groupNewMessage.fractalHeapAddress >= 0) {
      FractalHeap fractalHeap = new FractalHeap(H5header.this, group.displayName, groupNewMessage.fractalHeapAddress, memTracker);

      long btreeAddress = (groupNewMessage.v2BtreeAddressCreationOrder >= 0) ?
              groupNewMessage.v2BtreeAddressCreationOrder : groupNewMessage.v2BtreeAddress;
      if (btreeAddress < 0) throw new IllegalStateException("no valid btree for GroupNew with Fractal Heap");

      // read in btree and all entries
      BTree2 btree = new BTree2(H5header.this, group.displayName, btreeAddress);
      for (BTree2.Entry2 e : btree.entryList) {
        byte[] heapId;
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

        // the heapId points to a Link message in the Fractal Heap
        FractalHeap.DHeapId fractalHeapId = fractalHeap.getFractalHeapId(heapId);
        long pos = fractalHeapId.getPos();
        if (pos < 0) continue;
        raf.seek(pos);
        MessageLink linkMessage = new MessageLink();
        linkMessage.read();
        if (debugBtree2) {
            log.debug("    linkMessage={}", linkMessage);
        }

        group.nestedObjects.add(new DataObjectFacade(group, linkMessage.linkName, linkMessage.linkAddress));
      }

    } else {
      // look for link messages
      for (HeaderMessage mess : dobj.messages) {
        if (mess.mtype == MessageType.Link) {
          MessageLink linkMessage = (MessageLink) mess.messData;
          if (linkMessage.linkType == 0) { // hard link
            group.nestedObjects.add(new DataObjectFacade(group, linkMessage.linkName, linkMessage.linkAddress));
          }
        }
      }
    }

    /* now read all the entries in the btree
   for (SymbolTableEntry s : btree.getSymbolTableEntries()) {
     String sname = nameHeap.getString((int) s.getNameOffset());
     if (debugSymbolTable) log.debug("\n   Symbol name=" + sname);

     DataObject o;
     if (s.cacheType == 2) {
       String linkName = nameHeap.getString(s.linkOffset);
       if (debugSymbolTable) log.debug("   Symbolic link name=" + linkName);
       o = new DataObject(this, sname, linkName);

     } else {
       o = new DataObject(this, sname, s.getObjectAddress());
       o.read();
     }
     nestedObjects.add(o);
     hashDataObjects.put(o.getName(), o); // to look up symbolic links
   } */
    if (debug1) {
        log.debug("<-- end GroupNew read <" + group.displayName + ">");
    }
  }

  private Map<Long, H5Group> hashGroups = new HashMap<>();
  private void readGroupOld(H5Group group, long btreeAddress, long nameHeapAddress) throws IOException {
    // track by address for hard links
    hashGroups.put(btreeAddress, group);

    if (debug1) {
        log.debug("\n--> GroupOld read <" + group.displayName + ">");
    }
    LocalHeap nameHeap = new LocalHeap(group, nameHeapAddress);
    GroupBTree btree = new GroupBTree(group.displayName, btreeAddress);

    // now read all the entries in the btree : Level 1C
    for (SymbolTableEntry s : btree.getSymbolTableEntries()) {
      String sname = nameHeap.getString((int) s.getNameOffset());
      if (debugSoftLink) {
        log.debug("\n   Symbol name={}", sname);
      }
      if (s.cacheType == 2) {
        String linkName = nameHeap.getString(s.linkOffset);
        if (debugSoftLink) {
            log.debug("   Symbolic link name=" + linkName + " symbolName=" + sname);
        }
        group.nestedObjects.add(new DataObjectFacade(group, sname, linkName));
      } else {
        group.nestedObjects.add(new DataObjectFacade(group, sname, s.getObjectAddress()));
      }
    }
    if (debug1) {
        log.debug("<-- end GroupOld read <" + group.displayName + ">");
    }
  }

  // Level 1A
  // this just reads in all the entries into a list
  private class GroupBTree {
    protected String owner;
    protected int wantType = 0;
    private List<SymbolTableEntry> sentries = new ArrayList<>(); // list of type SymbolTableEntry

    // for DataBTree
    GroupBTree(String owner) {
      this.owner = owner;
    }

    GroupBTree(String owner, long address) throws IOException {
      this.owner = owner;

      List<Entry> entryList = new ArrayList<>();
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
      if (debugGroupBtree) {
        log.debug("\n--> GroupBTree read tree at position={}", raf.getFilePointer());
      }

      String magic = raf.readString(4);
      if (!magic.equals("TREE"))
        throw new IllegalStateException("BtreeGroup doesnt start with TREE");

      int type = raf.readByte();
      int level = raf.readByte();
      int nentries = raf.readShort();
      if (debugGroupBtree) {
        log.debug("    type=" + type + " level=" + level + " nentries=" + nentries);
      }
      if (type != wantType) {
        throw new IllegalStateException("BtreeGroup must be type " + wantType);
      }

      long size = 8 + 2 * sizeOffsets + nentries * (sizeOffsets + sizeLengths);
      if (debugTracker) memTracker.addByLen("Group BTree (" + owner + ")", address, size);

      long leftAddress = readOffset();
      long rightAddress = readOffset();
      if (debugGroupBtree) {
        log.debug("    leftAddress=" + leftAddress + " " + Long.toHexString(leftAddress) +
                " rightAddress=" + rightAddress + " " + Long.toHexString(rightAddress));
      }

      // read all entries in this Btree "Node"
      List<Entry> myEntries = new ArrayList<>();
      for (int i = 0; i < nentries; i++) {
        myEntries.add(new Entry());
      }

      if (level == 0)
        entryList.addAll(myEntries);
      else {
        for (Entry entry : myEntries) {
          if (debugDataBtree) {
            log.debug("  nonzero node entry at =" + entry.address);
          }
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
        if (debugGroupBtree) {
            log.debug("     GroupEntry key={} address={}", key, address);
        }
      }
    }

    // level 1B
    class GroupNode {
      long address;
      byte version;
      short nentries;
      List<SymbolTableEntry> symbols = new ArrayList<>(); // SymbolTableEntry

      GroupNode(long address) throws IOException {
        this.address = address;

        raf.seek(getFileOffset(address));
        if (debugDetail) {
            log.debug("--Group Node position={}", raf.getFilePointer());
        }

        // header
        String magic = raf.readString(4);
        if (!magic.equals("SNOD")) {
          throw new IllegalStateException(magic + " should equal SNOD");
        }

        version = raf.readByte();
        raf.readByte(); // skip byte
        nentries = raf.readShort();
        if (debugDetail) {
            log.debug("   version={} nentries={}", version, nentries);
        }

        long posEntry = raf.getFilePointer();
        for (int i = 0; i < nentries; i++) {
          SymbolTableEntry entry = new SymbolTableEntry(posEntry);
          posEntry += entry.getSize();
          if (entry.objectHeaderAddress != 0) { // LOOK: Probably a bug in HDF5 file format ?? jc July 16 2010
            if (debug1) {
                log.debug("   add {}", entry);
            }
            symbols.add(entry);
          } else {
            if (debug1) {
                log.debug("   BAD objectHeaderAddress==0 !! {}", entry);
            }
          }
        }
        if (debugDetail) {
            log.debug("-- Group Node end position={}", raf.getFilePointer());
        }
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
      if (debugSymbolTable) {
        log.debug("--> readSymbolTableEntry position={}", raf.getFilePointer());
      }

      nameOffset = readOffset();
      objectHeaderAddress = readOffset();
      cacheType = raf.readInt();
      raf.skipBytes(4);

      if (debugSymbolTable) {
        log.debug(" nameOffset={} objectHeaderAddress={} cacheType={}",
                nameOffset, objectHeaderAddress, cacheType);
      }

      // "scratch pad"
      posData = raf.getFilePointer();
      if (debugSymbolTable) dump("Group Entry scratch pad", posData, 16, false);

      if (cacheType == 1) {
        btreeAddress = readOffset();
        nameHeapAddress = readOffset();
        if (debugSymbolTable) {
            log.debug("btreeAddress={} nameHeadAddress={}", btreeAddress, nameHeapAddress);
        }
      }

      // check for symbolic link
      if (cacheType == 2) {
        linkOffset = raf.readInt(); // offset in local heap
        if (debugSymbolTable) {
            log.debug("WARNING Symbolic Link linkOffset={}", linkOffset);
        }
        isSymbolicLink = true;
      }

      /* if (cacheType == 1) {
       btreeAddress = mapBuffer.getLong();
       nameHeapAddress = mapBuffer.getLong();
       log.debug(" btreeAddress="+btreeAddress);
       log.debug(" nameHeapAddress="+nameHeapAddress);
       nameHeap = new LocalHeap();
       nameHeap.read(nameHeapAddress);

       btree = new Btree();
       btree.read(btreeAddress);

     } else if (cacheType == 2) {
       linkOffset = mapBuffer.getLong();
       log.debug(" linkOffset="+linkOffset);
     } else {
       for (int k=0; k<2; k++)
         log.debug( " "+k+" "+mapBuffer.getLong());
     } */

      if (debugSymbolTable) {
        log.debug("<-- end readSymbolTableEntry position={}", raf.getFilePointer());
      }

      if (debugTracker) memTracker.add("SymbolTableEntry", filePos, posData + 16);
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

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Heaps

  /**
   * Fetch a Vlen data array.
   *
   * @param globalHeapIdAddress address of the heapId, used to get the String out of the heap
   * @param dataType            type of data
   * @param endian              byteOrder of the data (0 = BE, 1 = LE)
   * @return the Array read from the heap
   * @throws IOException on read error
   */
  Array getHeapDataArray(long globalHeapIdAddress, DataType dataType, int endian) throws IOException, InvalidRangeException {
    HeapIdentifier heapId = new HeapIdentifier(globalHeapIdAddress);
    if (debugHeap) {
        log.debug(" heapId= {}", heapId);
    }
    return getHeapDataArray(heapId, dataType, endian);
    // Object pa = getHeapDataArray(heapId, dataType, endian);
    // return Array.factory(dataType.getPrimitiveClassType(), new int[]{heapId.nelems}, pa);
  }

  Array getHeapDataArray(HeapIdentifier heapId, DataType dataType, int endian) throws IOException, InvalidRangeException {
    GlobalHeap.HeapObject ho = heapId.getHeapObject();
    if (ho == null) {
      throw new InvalidRangeException("Illegal Heap address, HeapObject = " + heapId);
    }
    if (debugHeap) {
        log.debug(" HeapObject= {}", ho);
    }
    if (endian >= 0) { raf.order(endian); }

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
      raf.readFully(pa, 0, pa.length);
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
    if (ho == null) throw new IllegalStateException("Cant find Heap Object,heapId="+heapId);
    if (ho.dataSize > 1000 * 1000) return String.format("Bad HeapObject.dataSize=%s", ho);
    raf.seek(ho.dataPos);
    return readStringFixedLength((int) ho.dataSize);
  }

  /**
   * Fetch a String from the heap, when the heap identifier has already beed read into a ByteBuffer at given pos
   *
   * @param bb  heap id is here
   * @param pos at this position
   * @return String the String read from the heap
   * @throws IOException on read error
   */
  String readHeapString(ByteBuffer bb, int pos) throws IOException {
    H5header.HeapIdentifier heapId = new HeapIdentifier(bb, pos);
    H5header.GlobalHeap.HeapObject ho = heapId.getHeapObject();
    if (ho == null) throw new IllegalStateException("Cant find Heap Object,heapId="+heapId);
    // if (H5iosp.debugHeapStrings) System.out.printf("    readHeapString ho=%s%n", ho);
    raf.seek(ho.dataPos);
    return readStringFixedLength((int) ho.dataSize);
  }

  Array readHeapVlen(ByteBuffer bb, int pos, DataType dataType, int endian) throws IOException, InvalidRangeException {
    H5header.HeapIdentifier heapId = new HeapIdentifier(bb, pos);
    return getHeapDataArray(heapId, dataType, endian);
  }

  // debug - hdf5Table
  public List<DataObject> getDataObjects() {
    ArrayList<DataObject> result = new ArrayList<>(addressMap.values());
    Collections.sort(result, new java.util.Comparator<DataObject>() {
      public int compare(DataObject o1, DataObject o2) {
        // return (int) (o1.address - o2.address);
        return (o1.address < o2.address ? -1 : (o1.address == o2.address ? 0 : 1));
      }
    });
    return result;
  }

  /**
   * Get a data object's name, using the objectId you get from a reference (aka hard link).
   *
   * @param objId address of the data object
   * @return String the data object's name, or null if not found
   * @throws IOException on read error
   */
  String getDataObjectName(long objId) throws IOException {
    H5header.DataObject dobj = getDataObject(objId, null);
    if (dobj == null) {
      log.error("H5iosp.readVlenData cant find dataObject id= {}", objId);
      return null;
    } else {
      if (debugVlen) {
        log.debug(" Referenced object= {}", dobj.who);
      }
      return dobj.who;
    }
  }

  // see "Global Heap Id" in http://www.hdfgroup.org/HDF5/doc/H5.format.html
  class HeapIdentifier {
    private int nelems; // "number of 'base type' elements in the sequence in the heap"
    private long heapAddress;
    private int index;

    // address must be absolute, getFileOffset already added
    HeapIdentifier(long address) throws IOException {
      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(address);
      nelems = raf.readInt();
      heapAddress = readOffset();
      index = raf.readInt();
      if (debugDetail) {
        log.debug("   read HeapIdentifier address=" + address + this);
      }
      if (debugHeap) dump("heapIdentifier", getFileOffset(address), 16, true);
    }

    // the heap id is has already been read into a byte array at given pos
    HeapIdentifier(ByteBuffer bb, int pos) throws IOException {
      bb.order(ByteOrder.LITTLE_ENDIAN); // header information is in le byte order
      bb.position(pos); // reletive reading
      nelems = bb.getInt();
      heapAddress = isOffsetLong ? bb.getLong() : (long) bb.getInt();
      index = bb.getInt();
      if (debugDetail) {
        log.debug("   read HeapIdentifier from ByteBuffer={}", this);
      }
    }

    public String toString() {
      return " nelems=" + nelems + " heapAddress=" + heapAddress + " index=" + index;
    }

    public boolean isEmpty() {
      return (heapAddress == 0);
    }

    GlobalHeap.HeapObject getHeapObject() throws IOException {
      if (isEmpty()) return null;
      GlobalHeap gheap;
      if (null == (gheap = heapMap.get(heapAddress))) {
        gheap = new GlobalHeap(heapAddress);
        heapMap.put(heapAddress, gheap);
      }

      GlobalHeap.HeapObject ho = gheap.getHeapObject((short) index);
      if (ho == null)
        throw new IllegalStateException("cant find HeapObject");
      return ho;
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

      GlobalHeap.HeapObject want = gheap.getHeapObject( (short) index);
      if (debugRegionReference) {
        log.debug(" found ho={}", want);
      }
      /* - The offset of the object header of the object (ie. dataset) pointed to (yes, an object ID)
          - A serialized form of a dataspace _selection_ of elements (in the dataset pointed to).
          I don't have a formal description of this information now, but it's encoded in the H5S_<foo>_serialize() routines in
          src/H5S<foo>.c, where foo = {all, hyper, point, none}.
          There is _no_ datatype information stored for these kind of selections currently. */
      raf.seek(want.dataPos);
      long objId = raf.readLong();
      DataObject ndo = getDataObject(objId, null);
      // String what = (ndo == null) ? "none" : ndo.getName();
      if (debugRegionReference) {
        log.debug(" objId=" + objId + " DataObject= " + ndo);
      }
      if (null == ndo)
        throw new IllegalStateException("cant find data object at" + objId);
    }

  } // RegionReference

  // level 1E
  private class GlobalHeap {
    private byte version;
    private int sizeBytes;
    private Map<Short,HeapObject> hos = new HashMap<>();

    GlobalHeap(long address) throws IOException {
      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(getFileOffset(address));

      // header
      String magic = raf.readString(4);
      if (!magic.equals("GCOL"))
        throw new IllegalStateException(magic + " should equal GCOL");

      version = raf.readByte();
      raf.skipBytes(3);
      sizeBytes = raf.readInt();
      if (debugDetail) {
        log.debug("-- readGlobalHeap address=" + address + " version= " + version + " size = " + sizeBytes);
      //log.debug("-- readGlobalHeap address=" + address + " version= " + version + " size = " + sizeBytes);
      }
      raf.skipBytes(4); // pad to 8

      int count = 0;
      int countBytes = 0;
      while (true) {
        long startPos = raf.getFilePointer();
        HeapObject o = new HeapObject();
        o.id = raf.readShort();
        o.refCount = raf.readShort();
        raf.skipBytes(4);
        o.dataSize = readLength();
        o.dataPos = raf.getFilePointer();

        int dsize = ((int) o.dataSize) + padding((int) o.dataSize, 8);
        countBytes += dsize + 16;

        // System.out.printf("%d heapId=%d dataSize=%d countBytes=%d%n", count, o.id, o.dataSize, countBytes);
        if (o.id == 0) break; // ?? look
        if (o.dataSize < 0) break; // ran off the end, must be done
        if (countBytes < 0) break; // ran off the end, must be done
        if (countBytes > sizeBytes) break; // ran off the end

        if (debugDetail) {
          log.debug("   HeapObject  position=" + startPos + " id=" + o.id + " refCount= " + o.refCount +
                  " dataSize = " + o.dataSize + " dataPos = " + o.dataPos + " count= " + count + " countBytes= " + countBytes);
        }

        raf.skipBytes(dsize);
        hos.put(o.id, o);
        count++;

        if (countBytes + 16 >= sizeBytes) break; // ran off the end, must be done
      }

      if (debugDetail) {
        log.debug("-- endGlobalHeap position=" + raf.getFilePointer());
      }
      if (debugTracker) memTracker.addByLen("GlobalHeap", address, sizeBytes);
    }

    HeapObject getHeapObject(short id) {
      return hos.get(id);
    }

    class HeapObject {
      short id, refCount;
      long dataSize;
      long dataPos;

      @Override
      public String toString() {
        return "id=" + id +
                ", refCount=" + refCount +
                ", dataSize=" + dataSize +
                ", dataPos=" + dataPos;
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

      if (debugDetail) {
        log.debug("-- readLocalHeap position={}", raf.getFilePointer());
      }

      // header
      String magic = raf.readString(4);
      if (!magic.equals("HEAP")) {
        throw new IllegalStateException(magic + " should equal HEAP");
      }

      version = raf.readByte();
      raf.skipBytes(3);
      size = (int) readLength();
      freelistOffset = readLength();
      dataAddress = readOffset();
      if (debugDetail) {
        log.debug(" version=" + version + " size=" + size + " freelistOffset=" + freelistOffset + " heap starts at dataAddress=" + dataAddress);
      }
      if (debugPos) {
        log.debug("    *now at position={}", raf.getFilePointer());
      }

      // data
      raf.seek(getFileOffset(dataAddress));
      heap = new byte[size];
      raf.readFully(heap);
      //if (debugHeap) printBytes( out, "heap", heap, size, true);

      if (debugDetail) {
        log.debug("-- endLocalHeap position={}", raf.getFilePointer());
      }
      int hsize = 8 + 2 * sizeLengths + sizeOffsets;
      if (debugTracker) memTracker.addByLen("Group LocalHeap (" + group.displayName + ")", address, hsize);
      if (debugTracker) memTracker.addByLen("Group LocalHeapData (" + group.displayName + ")", dataAddress, size);
    }

    public String getString(int offset) {
      int count = 0;
      while (heap[offset + count] != 0) count++;
      return new String(heap, offset, count, CDM.utf8Charset);
    }

  } // LocalHeap

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
  private String readString(RandomAccessFile raf) throws IOException {
    long filePos = raf.getFilePointer();

    int count = 0;
    while (raf.readByte() != 0) count++;

    raf.seek(filePos);
    String result = raf.readString(count);
    raf.readByte(); // skip the zero byte! nn
    return result;
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
    raf.readFully(s);

    // skip to 8 byte boundary, note zero byte is skipped
    count++;
    count += padding(count, 8);
    raf.seek(filePos + count);

    return new String(s, CDM.utf8Charset); // all Strings are UTF-8 unicode
  }

  /**
   * Read a String of known length.
   *
   * @param size number of bytes
   * @return String result
   * @throws java.io.IOException on io error
   */
  private String readStringFixedLength(int size) throws IOException {
    return raf.readString(size);
  }

  long readLength() throws IOException {
    return isLengthLong ? raf.readLong() : (long) raf.readInt();
  }

  long readOffset() throws IOException {
    return isOffsetLong ? raf.readLong() : (long) raf.readInt();
  }

  long readAddress() throws IOException {
    return getFileOffset(readOffset());
  }

  // size of data depends on "maximum possible number"
  int getNumBytesFromMax(long maxNumber) {
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

  long readVariableSizeUnsigned(int size) throws IOException {
    long vv;
    if (size == 1) {
      vv = DataType.unsignedByteToShort(raf.readByte());
    } else if (size == 2) {
      if (debugPos) {
        log.debug("position={}", raf.getFilePointer());
      }
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
    throw new IllegalArgumentException("Dont support int size == " + size);
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

  long getFileOffset(long address) throws IOException {
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
    raf.readFully(mess);
    printBytes(head, mess, nbytes, false, debugOut);
    raf.seek(savePos);
  }

  static void printBytes(String head, byte[] buff, int n, boolean count, java.io.PrintWriter ps) {
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

  public void close() {
    if (debugTracker) {
      Formatter f= new Formatter();
      memTracker.report(f);
      log.debug("{}", f.toString());
    }
  }

}
