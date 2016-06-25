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
package ucar.nc2.iosp.hdf4;

import ucar.nc2.constants.CDM;
import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.*;
import ucar.ma2.*;
import ucar.unidata.util.Format;

import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;

/**
 * Read the tags of an HDF4 file, construct CDM objects.
 * All page references are to "HDF Specification and Developers Guide" version 4.1r5, nov 2001.
 *
 * @author caron
 * @since Jul 18, 2007
 */
public class    H4header {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H4header.class);

  static private final byte[] head = {0x0e, 0x03, 0x13, 0x01};
  static private final String shead = new String(head, CDM.utf8Charset);
  static private final long maxHeaderPos = 500000; // header's gotta be within this

  static boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    long pos = 0;
    long size = raf.length();

    // search forward for the header
    while ((pos < (size - head.length)) && (pos < maxHeaderPos)) {
      raf.seek(pos);
      String magic = raf.readString(head.length);
      if (magic.equals(shead))
        return true;
      pos = (pos == 0) ? 512 : 2 * pos;
    }

    return false;
  }

  /* replace space and / with underscore
  private static final char[] replace = new char[] {' ', '/'}; // , '.'};
  private static final String[] replaceWith = new String[] {"_", "_"}; // , ""};
  static String createValidObjectName(String name ) {
    return StringUtil2.replace( name, replace, replaceWith); // added 2/15/2010 , mod 2/18/2012
  }  */

  private static boolean debugDD = false; // DDH/DD
   private static boolean debugTag1 = false; // show tags after read(), before read2().
   private static boolean debugTag2 = false;  // show tags after everything is done.
   private static boolean debugTagDetail = false; // when showing tags, show detail or not
   private static boolean debugConstruct = false; // show CDM objects as they are constructed
   private static boolean debugAtt = false; // show CDM attributes as they are constructed
   private static boolean debugLinked = false; // linked data
   private static boolean debugChunkTable = false; // chunked data
   private static boolean debugChunkDetail = false; // chunked data
   private static boolean debugTracker = false; // memory tracker
   private static boolean warnings = false; // log messages

   private static boolean debugHdfEosOff = false; // allow to turn hdf eos processing off

   public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
     debugTag1 = debugFlag.isSet("H4header/tag1");
     debugTag2 = debugFlag.isSet("H4header/tag2");
     debugTagDetail = debugFlag.isSet("H4header/tagDetail");
     debugConstruct = debugFlag.isSet("H4header/construct");
     debugAtt = debugFlag.isSet("H4header/att");
     debugLinked = debugFlag.isSet("H4header/linked");
     debugChunkTable = debugFlag.isSet("H4header/chunkTable");
     debugChunkDetail = debugFlag.isSet("H4header/chunkDetail");
     debugTracker = debugFlag.isSet("H4header/memTracker");
     debugHdfEosOff = debugFlag.isSet("HdfEos/turnOff");
     if (debugFlag.isSet("HdfEos/showWork"))
       HdfEos.showWork = true;
   }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////

  private ucar.nc2.NetcdfFile ncfile;
  RandomAccessFile raf;
  private boolean isEos;

  private List<Tag> alltags;
  private Map<Integer, Tag> tagMap = new HashMap<>();
  private Map<Short, Vinfo> refnoMap = new HashMap<>();

  private MemTracker memTracker;
  //private PrintStream debugOut = System.out;
  private java.io.PrintWriter debugOut = new PrintWriter( new OutputStreamWriter(System.out, CDM.utf8Charset));

  public boolean isEos() {
    return isEos;
  }

  void read(RandomAccessFile myRaf, ucar.nc2.NetcdfFile ncfile) throws IOException {
    this.raf = myRaf;
    this.ncfile = ncfile;

    long actualSize = raf.length();
    memTracker = new MemTracker(actualSize);

    if (!isValidFile(myRaf))
      throw new IOException("Not an HDF4 file ");

    // now we are positioned right after the header
    memTracker.add("header", 0, raf.getFilePointer());

    // header information is in big endian byte order
    raf.order(RandomAccessFile.BIG_ENDIAN);
    if (debugConstruct)
      debugOut.println("H4header 0pened file to read:'" + raf.getLocation() + "', size=" + actualSize / 1000 + " Kb");

    // read the DDH and DD records
    alltags = new ArrayList<>();
    long link = raf.getFilePointer();
    while (link > 0)
      link = readDDH(alltags, link);

    // now read the individual tags where needed
    for (Tag tag : alltags) {//  LOOK could sort by file offset to minimize I/O
      tag.read();
      tagMap.put(tagid(tag.refno, tag.code), tag); // track all tags in a map, key is the "tag id".
      if (debugTag1) System.out.println(debugTagDetail ? tag.detail() : tag);
    }

    // construct the netcdf objects
    ncfile.setLocation(myRaf.getLocation());
    construct(ncfile, alltags);

    if (!debugHdfEosOff) {
      isEos = HdfEos.amendFromODL(ncfile, ncfile.getRootGroup());
      if (isEos) {
        adjustDimensions();
        String history = ncfile.findAttValueIgnoreCase(null, "_History", "");
        ncfile.addAttribute(null, new Attribute("_History", history + "; HDF-EOS StructMetadata information was read"));

      }
    }

    if (debugTag2) {
      for (Tag tag : alltags)
        debugOut.println(debugTagDetail ? tag.detail() : tag);
    }

    if (debugTracker) memTracker.report();
  }

  public void getEosInfo(Formatter f) throws IOException {
    HdfEos.getEosInfo(ncfile, ncfile.getRootGroup(), f);
  }

  static private int tagid(short refno, short code) {
    int result = (code & 0x3FFF) << 16;
    int result2 = (refno & 0xffff);
    result = result + result2;
    return result;
  }

  private void construct(ucar.nc2.NetcdfFile ncfile, List<Tag> alltags) throws IOException {
    List<Variable> vars = new ArrayList<>();
    List<Group> groups = new ArrayList<>();

    // pass 1 : Vgroups with special classes
    for (Tag t : alltags) {
      if (t.code == 306) { // raster image
        Variable v = makeImage((TagGroup) t);
        if (v != null) vars.add(v);

      } else if (t.code == 1965) { // Vgroup
        TagVGroup vgroup = (TagVGroup) t;
        if (vgroup.className.startsWith("Dim") || vgroup.className.startsWith("UDim"))
          makeDimension(vgroup);

        else if (vgroup.className.startsWith("Var")) {
          Variable v = makeVariable(vgroup);
          if (v != null) vars.add(v);

        } else if (vgroup.className.startsWith("CDF0.0"))
          addGlobalAttributes(vgroup);
      }
    }

    // pass 2 - VHeaders, NDG
    for (Tag t : alltags) {
      if (t.used) continue;

      if (t.code == 1962) { // VHeader
        TagVH tagVH = (TagVH) t;
        if (tagVH.className.startsWith("Data")) {
          Variable v = makeVariable(tagVH);
          if (v != null) vars.add(v);
        }

      } else if (t.code == 720) { // numeric data group
        Variable v = makeVariable((TagGroup) t);
        if (v != null) vars.add(v);
      }
    }

    // pass 3 - misc not claimed yet
    for (Tag t : alltags) {
      if (t.used) continue;

      if (t.code == 1962) { // VHeader
        TagVH vh = (TagVH) t;
        if (!vh.className.startsWith("Att") && !vh.className.startsWith("_HDF_CHK_TBL")) {
          Variable v = makeVariable(vh);
          if (v != null) vars.add(v);
        }
      }
    }

    // pass 4 - Groups
    for (Tag t : alltags) {
      if (t.used) continue;

      if (t.code == 1965) { // VGroup
        TagVGroup vgroup = (TagVGroup) t;
        Group g = makeGroup(vgroup, null);
        if (g != null) groups.add(g);
      }
    }
    for (Group g : groups) {
      if (g.getParentGroup() == ncfile.getRootGroup())
        ncfile.addGroup(null, g);
    }

    // not in a group
    Group root = ncfile.getRootGroup();
    for (Variable v : vars) {
      if ((v.getParentGroup() == root) && root.findVariable(v.getShortName()) == null)
        root.addVariable(v);
    }

    // annotations become attributes
    for (Tag t : alltags) {
      if (t instanceof TagAnnotate) {
        TagAnnotate ta = (TagAnnotate) t;
        Vinfo vinfo = refnoMap.get(ta.obj_refno);
        if (vinfo != null) {
          vinfo.v.addAttribute(new Attribute((t.code == 105) ? "description" : CDM.LONG_NAME, ta.text));
          t.used = true;
        }
      }
    }

    // misc global attributes
    ncfile.addAttribute(null, new Attribute("_History", "Direct read of HDF4 file through CDM library"));
    for (Tag t : alltags) {
      if (t.code == 30) {
        ncfile.addAttribute(null, new Attribute("HDF4_Version", ((TagVersion) t).value()));
        t.used = true;

      } else if (t.code == 100) {
        ncfile.addAttribute(null, new Attribute("Title-" + t.refno, ((TagText) t).text));
        t.used = true;

      } else if (t.code == 101) {
        ncfile.addAttribute(null, new Attribute("Description-" + t.refno, ((TagText) t).text));
        t.used = true;
      }
    }

  }

  private void adjustDimensions() {
    Map<Dimension, List<Variable>> dimUsedMap = new HashMap<>();
    findUsedDimensions(ncfile.getRootGroup(), dimUsedMap);
    Set<Dimension> dimUsed = dimUsedMap.keySet();

    // remove unused dimensions
    Iterator iter = ncfile.getRootGroup().getDimensions().iterator();
    while (iter.hasNext()) {
      Dimension dim = (Dimension) iter.next();
      if (!dimUsed.contains(dim))
        iter.remove();
    }

    // push used dimensions to the lowest group that contains all variables
    for (Dimension dim : dimUsed) {
      Group lowest = null;
      List<Variable> vlist = dimUsedMap.get(dim);
      for (Variable v : vlist) {
        if (lowest == null)
          lowest = v.getParentGroup();
        else {
          lowest = lowest.commonParent(v.getParentGroup());
        }
      }
      Group current = dim.getGroup();
      //if (current == null)
      //  System.out.println("HEY! current == null");
      if (lowest != null && current != lowest) {
        lowest.addDimension(dim);
        current.remove(dim);
      }
    }
  }

  private void findUsedDimensions(Group parent, Map<Dimension, List<Variable>> dimUsedMap) {
    for (Variable v : parent.getVariables()) {
      for (Dimension d : v.getDimensions()) {
        if (!d.isShared()) continue;
        List<Variable> vlist = dimUsedMap.get(d);
        if (vlist == null) {
          vlist = new ArrayList<>();
          dimUsedMap.put(d, vlist);
        }
        vlist.add(v);
      }
    }

    for (Group g : parent.getGroups())
      findUsedDimensions(g, dimUsedMap);
  }

  private void makeDimension(TagVGroup group) throws IOException {
    List<TagVH> dims = new ArrayList<>();

    Tag data = null;
    for (int i = 0; i < group.nelems; i++) {
      Tag tag = tagMap.get(tagid(group.elem_ref[i], group.elem_tag[i]));
      if (tag == null) throw new IllegalStateException();
      if (tag.code == 1962)
        dims.add((TagVH) tag);
      if (tag.code == 1963)
        data = tag;
    }
    if (dims.size() == 0)
      throw new IllegalStateException();

    int length = 0;
    if (data != null) {
      raf.seek(data.offset);
      length = raf.readInt();
      data.used = true;

    } else {

      for (TagVH vh : dims) {
        vh.used = true;
        data = tagMap.get(tagid(vh.refno, TagEnum.VS.getCode()));
        if (null != data) {
          data.used = true;
          raf.seek(data.offset);
          int length2 = raf.readInt();
          if (debugConstruct)
            System.out.println("dimension length=" + length2 + " for TagVGroup= " + group + " using data " + data.refno);
          if (length2 > 0) {
            length = length2;
            break;
          }
        }
      }
    }

    if (data == null) {
      log.error("**no data for dimension TagVGroup= " + group);
      return;
    }

    if (length <= 0) {
      log.warn("**dimension length=" + length + " for TagVGroup= " + group + " using data " + data.refno);
    }

    boolean isUnlimited = (length == 0);
    Dimension dim = new Dimension(group.name, length, true, isUnlimited, false);
    if (debugConstruct) System.out.println("added dimension " + dim + " from VG " + group.refno);
    ncfile.addDimension(null, dim);
  }

  private void addGlobalAttributes(TagVGroup group) throws IOException {

    // look for attributes
    for (int i = 0; i < group.nelems; i++) {
      Tag tag = tagMap.get(tagid(group.elem_ref[i], group.elem_tag[i]));
      if (tag == null) throw new IllegalStateException();
      if (tag.code == 1962) {
        TagVH vh = (TagVH) tag;
        if (vh.className.startsWith("Att")) {
          String lowername = vh.name.toLowerCase();
          if ((vh.nfields == 1) && (H4type.setDataType(vh.fld_type[0], null) == DataType.CHAR) &&
              ((vh.fld_isize[0] > 4000) || lowername.startsWith("archivemetadata") || lowername.startsWith("coremetadata")
                  || lowername.startsWith("productmetadata") || lowername.startsWith("structmetadata"))) {
            ncfile.addVariable(null, makeVariable(vh)); // // large EOS metadata - make into variable in root group
          } else {
            Attribute att = makeAttribute(vh);
            if (null != att) ncfile.addAttribute(null, att); // make into attribute in root group
          }
        }
      }
    }

    group.used = true;
  }

  private Attribute makeAttribute(TagVH vh) throws IOException {

    Tag data = tagMap.get(tagid(vh.refno, TagEnum.VS.getCode()));
    if (data == null)
      throw new IllegalStateException();

    // for now assume only 1
    if (vh.nfields != 1)
      throw new IllegalStateException();

    String name = vh.name;
    short type = vh.fld_type[0];
    int size = vh.fld_isize[0];
    int nelems = vh.nvert;
    vh.used = true;
    data.used = true;

    Attribute att = null;
    raf.seek(data.offset);
    switch (type) {
      case 3:
      case 4:
        if (nelems == 1)
          att = new Attribute(name, raf.readStringMax(size));
        else {
          String[] vals = new String[nelems];
          for (int i = 0; i < nelems; i++)
            vals[i] = raf.readStringMax(size);
          att = new Attribute(name, Array.factory(DataType.STRING.getPrimitiveClassType(), new int[]{nelems}, vals));
        }
        break;
      case 5:
        if (nelems == 1)
          att = new Attribute(name, raf.readFloat());
        else {
          float[] vals = new float[nelems];
          for (int i = 0; i < nelems; i++)
            vals[i] = raf.readFloat();
          att = new Attribute(name, Array.factory(DataType.FLOAT.getPrimitiveClassType(), new int[]{nelems}, vals));
        }
        break;
      case 6:
        if (nelems == 1)
          att = new Attribute(name, raf.readDouble());
        else {
          double[] vals = new double[nelems];
          for (int i = 0; i < nelems; i++)
            vals[i] = raf.readDouble();
          att = new Attribute(name, Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[]{nelems}, vals));
        }
        break;
      case 20:
      case 21:
        if (nelems == 1)
          att = new Attribute(name, raf.readByte());
        else {
          byte[] vals = new byte[nelems];
          for (int i = 0; i < nelems; i++)
            vals[i] = raf.readByte();
          att = new Attribute(name, Array.factory(DataType.BYTE.getPrimitiveClassType(), new int[]{nelems}, vals));
        }
        break;
      case 22:
      case 23:
        if (nelems == 1)
          att = new Attribute(name, raf.readShort());
        else {
          short[] vals = new short[nelems];
          for (int i = 0; i < nelems; i++)
            vals[i] = raf.readShort();
          att = new Attribute(name, Array.factory(DataType.SHORT.getPrimitiveClassType(), new int[]{nelems}, vals));
        }
        break;
      case 24:
      case 25:
        if (nelems == 1)
          att = new Attribute(name, raf.readInt());
        else {
          int[] vals = new int[nelems];
          for (int i = 0; i < nelems; i++)
            vals[i] = raf.readInt();
          att = new Attribute(name, Array.factory(DataType.INT.getPrimitiveClassType(), new int[]{nelems}, vals));
        }
        break;
      case 26:
      case 27:
        if (nelems == 1)
          att = new Attribute(name, raf.readLong());
        else {
          long[] vals = new long[nelems];
          for (int i = 0; i < nelems; i++)
            vals[i] = raf.readLong();
          att = new Attribute(name, Array.factory(DataType.LONG.getPrimitiveClassType(), new int[]{nelems}, vals));
        }
        break;
    }

    if (debugAtt) System.out.println("added attribute " + att);
    return att;
  }

  private Group makeGroup(TagVGroup tagGroup, Group parent) throws IOException {
    if (tagGroup.nelems < 1)
      return null;

    Group group = new Group(ncfile, parent, tagGroup.name);
    tagGroup.used = true;
    tagGroup.group = group;

    for (int i = 0; i < tagGroup.nelems; i++) {
      Tag tag = tagMap.get(tagid(tagGroup.elem_ref[i], tagGroup.elem_tag[i]));
      if (tag == null) {
        log.error("Reference tag missing= " + tagGroup.elem_ref[i] + "/" + tagGroup.elem_tag[i] +" for group "+tagGroup.refno);
        continue;
      }

      if (tag.code == 720) { // NG - prob var
        if (tag.vinfo != null) {
          Variable v = tag.vinfo.v;
          if (v != null)
            addVariableToGroup(group, v, tag);
          else
            log.error("Missing variable " + tag.refno);
        }
      }

      if (tag.code == 1962) { //Vheader - may be an att or a var
        TagVH vh = (TagVH) tag;
        if (vh.className.startsWith("Att")) {
          Attribute att = makeAttribute(vh);
          if (null != att) group.addAttribute(att);
        } else if (tag.vinfo != null) {
          Variable v = tag.vinfo.v;
          addVariableToGroup(group, v, tag);
        }
      }

      if (tag.code == 1965) {  // VGroup - prob a Group
        TagVGroup vg = (TagVGroup) tag;
        if ((vg.group != null) && (vg.group.getParentGroup() == ncfile.getRootGroup())) {
          addGroupToGroup(group, vg.group, vg);
          vg.group.setParentGroup(group);
        } else {
          Group nested = makeGroup(vg, group); // danger - loops
          if (nested != null) addGroupToGroup(group, nested, vg);
        }
      }
    }

    if (debugConstruct) {
      System.out.println("added group " + group.getFullName() + " from VG " + tagGroup.refno);
    }

    return group;
  }

  private void addVariableToGroup(Group g, Variable v, Tag tag) {
    Variable varExisting = g.findVariable(v.getShortName());
    if (varExisting != null) {
      //Vinfo vinfo = (Vinfo) v.getSPobject();
      //varExisting.setName(varExisting.getShortName()+vinfo.refno);
      v.setName(v.getShortName() + tag.refno);
    }
    g.addVariable(v);
  }

  private void addGroupToGroup(Group parent, Group g, Tag tag) {
    Group groupExisting = parent.findGroup(g.getShortName());
    if (groupExisting != null) {
      g.setName(g.getShortName() + tag.refno);
    }
    parent.addGroup(g);
  }

  private Variable makeImage(TagGroup group) {
    TagRIDimension dimTag = null;
    TagRIPalette palette;
    TagNumberType ntag;
    Tag data = null;

    Vinfo vinfo = new Vinfo(group.refno);
    group.used = true;

    // use the list of elements in the group to find the other tags
    for (int i = 0; i < group.nelems; i++) {
      Tag tag = tagMap.get(tagid(group.elem_ref[i], group.elem_tag[i]));
      if (tag == null) {
        log.warn("Image Group "+group.tag()+" has missing tag="+group.elem_ref[i]+"/"+group.elem_tag[i]);
        return null;
      }

      vinfo.tags.add(tag);
      tag.vinfo = vinfo; // track which variable this tag belongs to
      tag.used = true;  // assume if contained in Group, then used, to avoid redundant variables

      if (tag.code == 300)
        dimTag = (TagRIDimension) tag;
      if (tag.code == 302)
        data = tag;
      if (tag.code == 301)
        palette = (TagRIPalette) tag;
    }
    if (dimTag == null) {
      log.warn("Image Group "+group.tag()+" missing dimension tag");
      return null;
    }
    if (data == null)  {
      log.warn("Image Group "+group.tag()+" missing data tag");
      return null;
    }

    // get the NT tag, referred to from the dimension tag
    Tag tag = tagMap.get(tagid(dimTag.nt_ref, TagEnum.NT.getCode()));
    if (tag == null)   {
      log.warn("Image Group "+group.tag()+" missing NT tag");
      return null;
    }
    ntag = (TagNumberType) tag;

    if (debugConstruct) System.out.println("construct image " + group.refno);
    vinfo.start = data.offset;
    vinfo.tags.add(group);
    vinfo.tags.add(dimTag);
    vinfo.tags.add(data);
    vinfo.tags.add(ntag);

    // assume dimensions are not shared for now
    if (dimTag.dims == null) {
      dimTag.dims = new ArrayList<>();
      dimTag.dims.add(makeDimensionUnshared("ydim", dimTag.ydim));
      dimTag.dims.add(makeDimensionUnshared("xdim", dimTag.xdim));
    }

    Variable v = new Variable(ncfile, null, null, "Image-" + group.refno);
    H4type.setDataType(ntag.type, v);
    v.setDimensions(dimTag.dims);
    vinfo.setVariable(v);

    return v;
  }

  private Dimension makeDimensionUnshared(String dimName, int len) {
    // create new dimension and add it
    return new Dimension(dimName, len, false);
  }

  private Dimension makeDimensionShared(String dimName, int len) {
    Group root = ncfile.getRootGroup();
    Dimension d = root.findDimension(dimName);
    if ((d != null) && (d.getLength() == len))
      return d;

    if (d != null) { // different length
      dimName = dimName + len;
      d = root.findDimension(dimName);
      if ((d != null) && (d.getLength() == len))
        return d;
    }

    // create new dimension and add it
    return ncfile.addDimension(null, new Dimension(dimName, len));
  }

  private Variable makeVariable(TagVH vh) throws IOException {
    Vinfo vinfo = new Vinfo(vh.refno);
    vinfo.tags.add(vh);
    vh.vinfo = vinfo;
    vh.used = true;

    TagData data = (TagData) tagMap.get(tagid(vh.refno, TagEnum.VS.getCode()));
    if (data == null) {
      log.error("Cant find tag " + vh.refno + "/" + TagEnum.VS.getCode() + " for TagVH=" + vh.detail());
      return null;
    }
    vinfo.tags.add(data);
    data.used = true;
    data.vinfo = vinfo;

    if (vh.nfields < 1)
      throw new IllegalStateException();

    Variable v;
    if (vh.nfields == 1) {
      // String name = createValidObjectName(vh.name);
      v = new Variable(ncfile, null, null, vh.name);
      vinfo.setVariable(v);
      H4type.setDataType(vh.fld_type[0], v);

      try {
        if (vh.nvert > 1) {

          if (vh.fld_order[0] > 1)
            v.setDimensionsAnonymous(new int[]{vh.nvert, vh.fld_order[0]});
          else if (vh.fld_order[0] < 0)
            v.setDimensionsAnonymous(new int[]{vh.nvert, vh.fld_isize[0]});
          else
            v.setDimensionsAnonymous(new int[]{vh.nvert});

        } else {

          if (vh.fld_order[0] > 1)
            v.setDimensionsAnonymous(new int[]{vh.fld_order[0]});
          else if (vh.fld_order[0] < 0)
            v.setDimensionsAnonymous(new int[]{vh.fld_isize[0]});
          else
            v.setIsScalar();
        }

      } catch (InvalidRangeException e) {
        throw new IllegalStateException();
      }

      vinfo.setData(data, v.getElementSize());

    } else {

      Structure s;
      try {
        // String name = createValidObjectName(vh.name);
        s = new Structure(ncfile, null, null, vh.name);
        vinfo.setVariable(s);
        //vinfo.recsize = vh.ivsize;

        if (vh.nvert > 1)
          s.setDimensionsAnonymous(new int[]{vh.nvert});
        else
          s.setIsScalar();

        for (int fld = 0; fld < vh.nfields; fld++) {
          Variable m = new Variable(ncfile, null, s, vh.fld_name[fld]);
          short type = vh.fld_type[fld];
          short nelems = vh.fld_order[fld];
          H4type.setDataType(type, m);
          if (nelems > 1)
            m.setDimensionsAnonymous(new int[]{nelems});
          else
            m.setIsScalar();

          m.setSPobject(new Minfo(vh.fld_offset[fld]));
          s.addMemberVariable(m);
        }

      } catch (InvalidRangeException e) {
        throw new IllegalStateException(e.getMessage());
      }

      vinfo.setData(data, vh.ivsize);
      v = s;
    }

    if (debugConstruct) {
      System.out.println("added variable " + v.getNameAndDimensions() + " from VH " + vh);
    }

    return v;
  }

  // member info

  static class Minfo {
    int offset;

    Minfo(int offset) {
      this.offset = offset;
    }
  }

  private Variable makeVariable(TagVGroup group) throws IOException {
    Vinfo vinfo = new Vinfo(group.refno);
    vinfo.tags.add(group);
    group.used = true;

    TagSDDimension dim = null;
    TagNumberType ntag = null;
    TagData data = null;
    List<Dimension> dims = new ArrayList<>();
    for (int i = 0; i < group.nelems; i++) {
      Tag tag = tagMap.get(tagid(group.elem_ref[i], group.elem_tag[i]));
      if (tag == null) {
        log.error("Reference tag missing= " + group.elem_ref[i] + "/" + group.elem_tag[i]);
        continue;
      }

      vinfo.tags.add(tag);
      tag.vinfo = vinfo; // track which variable this tag belongs to
      tag.used = true;  // assume if contained in Vgroup, then not needed, to avoid redundant variables

      if (tag.code == 106)
        ntag = (TagNumberType) tag;
      if (tag.code == 701)
        dim = (TagSDDimension) tag;
      if (tag.code == 702)
        data = (TagData) tag;

      if (tag.code == 1965) {
        TagVGroup vg = (TagVGroup) tag;
        if (vg.className.startsWith("Dim") || vg.className.startsWith("UDim")) {
          String dimName = NetcdfFile.makeValidCdmObjectName(vg.name);
          Dimension d = ncfile.getRootGroup().findDimension(dimName);
          if (d == null)
            throw new IllegalStateException();
          dims.add(d);
        }
      }
    }
    if (ntag == null) {
      log.error("ntype tag missing vgroup= " + group.refno);
      return null;
    }
    if (dim == null) {
      log.error("dim tag missing vgroup= " + group.refno);
      return null;
    }
    if (data == null) {
      log.warn("data tag missing vgroup= " + group.refno + " " + group.name);
      //return null;
    }
    Variable v = new Variable(ncfile, null, null, group.name);
    v.setDimensions(dims);
    H4type.setDataType(ntag.type, v);

    vinfo.setVariable(v);
    vinfo.setData(data, v.getElementSize());

    // apparently the 701 SDdimension tag overrides the VGroup dimensions
    assert dim.shape.length == v.getRank();
    boolean ok = true;
    for (int i = 0; i < dim.shape.length; i++)
      if (dim.shape[i] != v.getDimension(i).getLength()) {
        if (warnings) log.info(dim.shape[i] + " != " + v.getDimension(i).getLength() + " for " + v.getFullName());
        ok = false;
      }

    if (!ok) {
      try {
        v.setDimensionsAnonymous(dim.shape);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
    }

    // look for attributes
    addVariableAttributes(group, vinfo);

    if (debugConstruct) {
      System.out.println("added variable " + v.getNameAndDimensions() + " from VG " + group.refno);
      System.out.println("  SDdim= " + dim.detail());
      System.out.print("  VGdim= ");
      for (Dimension vdim : dims) System.out.print(vdim + " ");
      System.out.println();
    }

    return v;
  }

  private Variable makeVariable(TagGroup group) throws IOException {
    Vinfo vinfo = new Vinfo(group.refno);
    vinfo.tags.add(group);
    group.used = true;

    TagSDDimension dim = null;
    TagData data = null;
    for (int i = 0; i < group.nelems; i++) {
      Tag tag = tagMap.get(tagid(group.elem_ref[i], group.elem_tag[i]));
      if (tag == null) {
        log.error("Cant find tag " + group.elem_ref[i] + "/" + group.elem_tag[i] + " for group=" + group.refno);
        continue;
      }
      vinfo.tags.add(tag);
      tag.vinfo = vinfo; // track which variable this tag belongs to
      tag.used = true;  // assume if contained in Group, then used, to avoid redundant variables

      if (tag.code == 701)
        dim = (TagSDDimension) tag;
      if (tag.code == 702)
        data = (TagData) tag;
    }
    if ((dim == null) || (data == null))
      throw new IllegalStateException();

    TagNumberType nt = (TagNumberType) tagMap.get(tagid(dim.nt_ref, TagEnum.NT.getCode()));
    if (null == nt) throw new IllegalStateException();

    Variable v = new Variable(ncfile, null, null, "SDS-" + group.refno);
    try {
      v.setDimensionsAnonymous(dim.shape);
    } catch (InvalidRangeException e) {
      throw new IllegalStateException();
    }
    DataType dataType = H4type.setDataType(nt.type, v);

    vinfo.setVariable(v);
    vinfo.setData(data, v.getElementSize());

    // now that we know n, read attribute tags
    for (int i = 0; i < group.nelems; i++) {
      Tag tag = tagMap.get(tagid(group.elem_ref[i], group.elem_tag[i]));
      if (tag == null) throw new IllegalStateException();

      if (tag.code == 704) {
        TagTextN labels = (TagTextN) tag;
        labels.read(dim.rank);
        tag.used = true;
        v.addAttribute(new Attribute(CDM.LONG_NAME, labels.getList()));
      }
      if (tag.code == 705) {
        TagTextN units = (TagTextN) tag;
        units.read(dim.rank);
        tag.used = true;
        v.addAttribute(new Attribute(CDM.UNITS, units.getList()));
      }
      if (tag.code == 706) {
        TagTextN formats = (TagTextN) tag;
        formats.read(dim.rank);
        tag.used = true;
        v.addAttribute(new Attribute("formats", formats.getList()));
      }
      if (tag.code == 707) {
        TagSDminmax minmax = (TagSDminmax) tag;
        tag.used = true;
        v.addAttribute(new Attribute("min", minmax.getMin(dataType)));
        v.addAttribute(new Attribute("max", minmax.getMax(dataType)));
      }
    }

    // look for VH style attributes - dunno if they are actually used
    addVariableAttributes(group, vinfo);

    if (debugConstruct) {
      System.out.println("added variable " + v.getNameAndDimensions() + " from Group " + group);
      System.out.println("  SDdim= " + dim.detail());
    }

    return v;
  }

  private void addVariableAttributes(TagGroup group, Vinfo vinfo) throws IOException {
    // look for attributes
    for (int i = 0; i < group.nelems; i++) {
      Tag tag = tagMap.get(tagid(group.elem_ref[i], group.elem_tag[i]));
      if (tag == null) throw new IllegalStateException();
      if (tag.code == 1962) {
        TagVH vh = (TagVH) tag;
        if (vh.className.startsWith("Att")) {
          Attribute att = makeAttribute(vh);
          if (null != att) {
            vinfo.v.addAttribute(att);
            if (att.getShortName().equals(CDM.FILL_VALUE))
              vinfo.setFillValue(att);
          }
        }
      }
    }
  }

  //////////////////////////////////////////////////////////////////////

  class Vinfo implements Comparable<Vinfo> {
    short refno;
    Variable v;
    List<Tag> tags = new ArrayList<>();

    // info about reading the data
    TagData data;
    int elemSize; // for Structures, this is recsize
    Object fillValue;

    // below is not set until setLayoutInfo() is called
    boolean isLinked, isCompressed, isChunked, hasNoData;

    // regular
    int start = -1;
    int length;

    // linked
    long[] segPos;
    int[] segSize;

    // chunked
    List<DataChunk> chunks;
    int[] chunkSize;

    Vinfo(short refno) {
      this.refno = refno;
      refnoMap.put(refno, this);
    }

    void setVariable(Variable v) {
      this.v = v;
      v.setSPobject(this);
    }

    public int compareTo(Vinfo o) {
      return Short.compare(refno, o.refno);
    }

    void setData(TagData data, int elemSize) throws IOException {
      this.data = data;
      this.elemSize = elemSize;
      hasNoData = (data == null);
    }

    void setFillValue(Attribute att) {
      // see IospHelper.makePrimitiveArray(int size, DataType dataType, Object fillValue)
      fillValue = (v.getDataType() == DataType.STRING) ? att.getStringValue() : att.getNumericValue();
    }

    // make sure needed info is present : call this when variable needs to be read
    // this allows us to defer getting layout info until then
    void setLayoutInfo() throws IOException {
      if (data == null) return;

      if (null != data.linked) {
        isLinked = true;
        setDataBlocks(data.linked.getLinkedDataBlocks(), elemSize);

      } else if (null != data.compress) {
        isCompressed = true;
        TagData compData = data.compress.getDataTag();
        tags.add(compData);
        isLinked = (compData.linked != null);
        if (isLinked)
          setDataBlocks(compData.linked.getLinkedDataBlocks(), elemSize);
        else {
          start = compData.offset;
          length = compData.length;
          hasNoData = (start < 0) || (length < 0);
        }

      } else if (null != data.chunked) {
        isChunked = true;
        chunks = data.chunked.getDataChunks();
        chunkSize = data.chunked.chunk_length;
        isCompressed = data.chunked.isCompressed;

      } else {
        start = data.offset;
        hasNoData = (start < 0);
      }
    }

    private void setDataBlocks(List<TagLinkedBlock> linkedBlocks, int elemSize) {
      int nsegs = linkedBlocks.size();
      segPos = new long[nsegs];
      segSize = new int[nsegs];
      int count = 0;
      for (TagLinkedBlock tag : linkedBlocks) {
        segPos[count] = tag.offset;

        // option 1
        // the size must be a multiple of elemSize - assume remaining is unused
        //int nelems = tag.length / elemSize;
        //segSize[count] = nelems * elemSize;

        // option 2
        // Stucture that requires requires full use of the block length.
        // structure size = 12, block size = 4096, has 4 extra bytes
        // E:/problem/AIRS.2007.10.17.L1B.Cal_Subset.v5.0.16.0.G07292194950.hdf#L1B_AIRS_Cal_Subset/Data Fields/radiances
        // LOOK do we sometimes need option 1) above ??
        segSize[count] = tag.length;
        count++;
      }
    }

    public String toString() {
      Formatter sbuff = new Formatter();
      sbuff.format("refno=%d name=%s fillValue=%s %n", refno, v.getShortName(), fillValue);
      sbuff.format(" isChunked=%s isCompressed=%s isLinked=%s hasNoData=%s %n", isChunked, isCompressed, isLinked, hasNoData);
      sbuff.format(" elemSize=%d data start=%d length=%s %n%n", elemSize, start, length);
      for (Tag t : tags)
        sbuff.format(" %s%n", t.detail());
      return sbuff.toString();
    }

  }

  //////////////////////////////////////////////////////////////////////


  private long readDDH(List<Tag> alltags, long start) throws IOException {
    raf.seek(start);

    int ndd = DataType.unsignedShortToInt(raf.readShort()); // number of DD blocks
    long link = DataType.unsignedIntToLong(raf.readInt()); // point to the next DDH; link == 0 means no more
    if (debugDD) System.out.println(" DDHeader ndd=" + ndd + " link=" + link);

    long pos = raf.getFilePointer();
    for (int i = 0; i < ndd; i++) {
      raf.seek(pos);
      Tag tag = factory();
      pos += 12; // tag usually changed the file pointer
      if (tag.code > 1)
        alltags.add(tag);
    }
    memTracker.add("DD block", start, raf.getFilePointer());
    return link;
  }

  ////////////////////////////////////////////////////////////////////////////////
  // Tags

  private Tag factory() throws IOException {
    short code = raf.readShort();
    int ccode = code & 0x3FFF;
    switch (ccode) {
      case 20:
        return new TagLinkedBlock(code);
      case 30:
        return new TagVersion(code);
      case 40:   // Compressed
      case 61:   // Chunk
      case 702:  // Scientific data
      case 1963: // VData
        return new TagData(code);
      case 100:
      case 101:
      case 708:
        return new TagText(code);
      case 104:
      case 105:
        return new TagAnnotate(code);
      case 106:
        return new TagNumberType(code);
      case 300:
      case 307:
      case 308:
        return new TagRIDimension(code);
      case 301:
        return new TagRIPalette(code);
      case 306:
      case 720:
        return new TagGroup(code);
      case 701:
        return new TagSDDimension(code);
      case 704:
      case 705:
      case 706:
        return new TagTextN(code);
      case 707:
        return new TagSDminmax(code);
      case 1962:
        return new TagVH(code);
      case 1965:
        return new TagVGroup(code);
      default:
        return new Tag(code);
    }
  }

  // public for debugging (ucar.nc2.ui.Hdf4Table)
  // Tag == "Data Descriptor" (DD) and (usually) a "Data Element" that the offset/length points to
  public class Tag {
    short code;
    short refno;
    boolean extended;
    int offset, length;
    TagEnum t;
    boolean used;
    Vinfo vinfo;

    // read just the DD part of the tag. see p 11
    private Tag(short code) throws IOException {
      this.extended = (code & 0x4000) != 0;
      this.code = (short) (code & 0x3FFF);
      refno = raf.readShort();
      offset = raf.readInt();
      length = raf.readInt();
      t = TagEnum.getTag(this.code);
      if ((code > 1) && debugTracker)
        memTracker.add(t.getName() + " " + refno, offset, offset + length);
      //if (extended)
      //  System.out.println("");
    }

    // read the offset/length part of the tag. overridden by subclasses
    protected void read() throws IOException {
    }

    public String detail() {
      return (used ? " " : "*") + "refno=" + refno + " tag= " + t + (extended ? " EXTENDED" : "") + " offset=" + offset + " length=" + length +
          (((vinfo != null) && (vinfo.v != null)) ? " VV=" + vinfo.v.getFullName() : "");
    }

    public String toString() {
      return (used ? " " : "*") + "refno=" + refno + " tag= " + t + (extended ? " EXTENDED" : "" + " length=" + length);
    }

    public String tag() {
      return refno + "/" + code;
    }

    public short getCode() {
      return code;
    }

    public short getRefno() {
      return refno;
    }

    public boolean isExtended() {
      return extended;
    }

    public int getOffset() {
      return offset;
    }

    public int getLength() {
      return length;
    }

    public String getType() {
      return t.toString();
    }

    public boolean isUsed() {
      return used;
    }

    public String getVinfo() {
      return (vinfo == null) ? "" : vinfo.toString();
    }

    public String getVClass() {
      if (this instanceof H4header.TagVGroup)
        return ((H4header.TagVGroup) this).className;
      if (this instanceof H4header.TagVH)
        return ((H4header.TagVH) this).className;
      return "";
    }
  }

  // 40 (not documented), 702 p 129
  class TagData extends Tag {
    short ext_type;
    SpecialLinked linked;
    SpecialComp compress;
    SpecialChunked chunked;
    int tag_len;

    TagData(short code) throws IOException {
      super(code);
    }

    protected void read() throws IOException {
      if (extended) {
        raf.seek(offset);
        ext_type = raf.readShort();  // note size wrong in doc

        if (ext_type == TagEnum.SPECIAL_LINKED) {
          linked = new SpecialLinked();
          linked.read();

        } else if (ext_type == TagEnum.SPECIAL_COMP) {
          compress = new SpecialComp();
          compress.read();

        } else if (ext_type == TagEnum.SPECIAL_CHUNKED) {
          chunked = new SpecialChunked();
          chunked.read();
        }
        tag_len = (int) (raf.getFilePointer() - offset);
      }
    }

    public String detail() {
      if (linked != null)
        return super.detail() + " ext_tag= " + ext_type + " tag_len= " + tag_len + " " + linked.detail();
      else if (compress != null)
        return super.detail() + " ext_tag= " + ext_type + " tag_len= " + tag_len + " " + compress.detail();
      else if (chunked != null)
        return super.detail() + " ext_tag= " + ext_type + " tag_len= " + tag_len + " " + chunked.detail();
      else
        return super.detail();
    }

  }

  // p 147-150
  private class SpecialChunked {
    byte version, flag;
    short chunk_tbl_tag, chunk_tbl_ref;
    int head_len, elem_tot_length, chunk_size, nt_size, ndims;

    int[] dim_length, chunk_length;
    byte[][] dim_flag;
    boolean isCompressed;

    short sp_tag_desc; // SPECIAL_XXX constant
    byte[] sp_tag_header;


    private void read() throws IOException {
      head_len = raf.readInt();
      version = raf.readByte();
      raf.skipBytes(3);
      flag = raf.readByte();
      elem_tot_length = raf.readInt();
      chunk_size = raf.readInt();
      nt_size = raf.readInt();

      chunk_tbl_tag = raf.readShort();
      chunk_tbl_ref = raf.readShort();
      raf.skipBytes(4);
      ndims = raf.readInt();

      dim_flag = new byte[ndims][4];
      dim_length = new int[ndims];
      chunk_length = new int[ndims];
      for (int i = 0; i < ndims; i++) {
        raf.readFully(dim_flag[i]);
        dim_length[i] = raf.readInt();
        chunk_length[i] = raf.readInt();
      }

      int fill_val_numtype = raf.readInt();
      byte[] fill_value = new byte[fill_val_numtype];
      raf.readFully(fill_value);

      // LOOK wuzzit stuff? "specialness"
      sp_tag_desc = raf.readShort();
      int sp_header_len = raf.readInt();
      sp_tag_header = new byte[sp_header_len];
      raf.readFully(sp_tag_header);
    }

    List<DataChunk> dataChunks = null;

    List<DataChunk> getDataChunks() throws IOException {
      if (dataChunks == null) {
        dataChunks = new ArrayList<>();

        // read the chunk table - stored as a Structure in the data
        if (debugChunkTable) System.out.println(" TagData getChunkedTable " + detail());
        TagVH chunkTableTag = (TagVH) tagMap.get(tagid(chunk_tbl_ref, chunk_tbl_tag));
        Structure s = (Structure) makeVariable(chunkTableTag);
        ArrayStructure sdata = (ArrayStructure) s.read();
        if (debugChunkDetail) System.out.println(NCdumpW.toString(sdata, "getChunkedTable", null));

        // construct the chunks
        StructureMembers members = sdata.getStructureMembers();
        StructureMembers.Member originM = members.findMember("origin");
        StructureMembers.Member tagM = members.findMember("chk_tag");
        StructureMembers.Member refM = members.findMember("chk_ref");
        int n = (int) sdata.getSize();
        if (debugChunkTable) System.out.println(" Reading " + n + " DataChunk tags");
        for (int i = 0; i < n; i++) {
          //if (i == 341)
          //System.out.println("HEYA");

          int[] origin = sdata.getJavaArrayInt(i, originM);
          short tag = sdata.getScalarShort(i, tagM);
          short ref = sdata.getScalarShort(i, refM);
          TagData data = (TagData) tagMap.get(tagid(ref, tag));
          dataChunks.add(new DataChunk(origin, chunk_length, data));
          data.used = true;
          if (data.compress != null) isCompressed = true;
        }
      }
      return dataChunks;
    }

    public String detail() {
      StringBuilder sbuff = new StringBuilder("SPECIAL_CHUNKED ");
      sbuff.append(" head_len=").append(head_len).append(" version=").append(version).append(" special =").append(flag).append(" elem_tot_length=").append(elem_tot_length);
      sbuff.append(" chunk_size=").append(chunk_size).append(" nt_size=").append(nt_size).append(" chunk_tbl_tag=").append(chunk_tbl_tag).append(" chunk_tbl_ref=").append(chunk_tbl_ref);
      sbuff.append("\n flag  dim  chunk\n");
      for (int i = 0; i < ndims; i++)
        sbuff.append(" ").append(dim_flag[i][2]).append(",").append(dim_flag[i][3]).append(" ").append(dim_length[i]).append(" ").append(chunk_length[i]).append("\n");
      sbuff.append(" special=").append(sp_tag_desc).append(" val=");
      for (int i = 0; i < sp_tag_header.length; i++)
        sbuff.append(" ").append(sp_tag_header[i]);
      return sbuff.toString();
    }
  }

  static class DataChunk {
    int origin[];
    TagData data;

    DataChunk(int[] origin, int[] chunk_length, TagData data) {
      // origin is in units of chunks - convert to indices
      assert origin.length == chunk_length.length;
      for (int i = 0; i < origin.length; i++)
        origin[i] *= chunk_length[i];
      this.origin = origin;

      this.data = data;
      if (debugChunkTable) {
        System.out.print(" Chunk origin=");
        for (int i = 0; i < origin.length; i++) System.out.print(origin[i] + " ");
        System.out.println(" data=" + data.detail());
      }
    }
  }

  // p 151
  class SpecialComp {
    short version, model_type, compress_type, data_ref;
    int uncomp_length;
    TagData dataTag;

    // compress_type == 2
    short signFlag, fillValue;
    int nt, startBit, bitLength;

    // compress_type == 4
    short deflateLevel;

    private void read() throws IOException {
      version = raf.readShort();
      uncomp_length = raf.readInt();
      data_ref = raf.readShort();
      model_type = raf.readShort();
      compress_type = raf.readShort();

      if (compress_type == TagEnum.COMP_CODE_NBIT) {
        nt = raf.readInt();
        signFlag = raf.readShort();
        fillValue = raf.readShort();
        startBit = raf.readInt();
        bitLength = raf.readInt();

      } else if (compress_type == TagEnum.COMP_CODE_DEFLATE) {
        deflateLevel = raf.readShort();
      }
    }

    TagData getDataTag() throws IOException {
      if (dataTag == null) {
        dataTag = (TagData) tagMap.get(tagid(data_ref, TagEnum.COMPRESSED.getCode()));
        if (dataTag == null)
          throw new IllegalStateException("TagCompress not found for " + detail());
        dataTag.used = true;
      }
      return dataTag;
    }

    public String detail() {
      StringBuilder sbuff = new StringBuilder("SPECIAL_COMP ");
      sbuff.append(" version=").append(version).append(" uncompressed length =").append(uncomp_length).append(" link_ref=").append(data_ref);
      sbuff.append(" model_type=").append(model_type).append(" compress_type=").append(compress_type);
      if (compress_type == TagEnum.COMP_CODE_NBIT) {
        sbuff.append(" nt=").append(nt).append(" signFlag=").append(signFlag).append(" fillValue=").append(fillValue).append(" startBit=").append(startBit).append(" bitLength=").append(bitLength);
      } else if (compress_type == TagEnum.COMP_CODE_DEFLATE) {
        sbuff.append(" deflateLevel=").append(deflateLevel);
      }
      return sbuff.toString();
    }

  }

  // p 145
  class SpecialLinked {
    int length, first_len;
    short blk_len, num_blk, link_ref;
    List<TagLinkedBlock> linkedDataBlocks;

    private void read() throws IOException {
      length = raf.readInt();
      first_len = raf.readInt();
      blk_len = raf.readShort(); // note size wrong in doc
      num_blk = raf.readShort(); // note size wrong in doc
      link_ref = raf.readShort();
    }

    List<TagLinkedBlock> getLinkedDataBlocks() throws IOException {
      if (linkedDataBlocks == null) {
        linkedDataBlocks = new ArrayList<>();
        if (debugLinked) System.out.println(" TagData readLinkTags " + detail());
        short next = link_ref; // (short) (link_ref & 0x3FFF);
        while (next != 0) {
          TagLinkedBlock tag = (TagLinkedBlock) tagMap.get(tagid(next, TagEnum.LINKED.getCode()));
          if (tag == null)
            throw new IllegalStateException("TagLinkedBlock not found for " + detail());
          tag.used = true;
          tag.read2(num_blk, linkedDataBlocks);
          next = tag.next_ref; // (short) (tag.next_ref & 0x3FFF);
        }
      }
      return linkedDataBlocks;
    }

    public String detail() {
      return "SPECIAL_LINKED length=" + length + " first_len=" + first_len + " blk_len=" + blk_len + " num_blk=" + num_blk + " link_ref=" + link_ref;
    }
  }

  // 20 p 146 Also used for data blocks, which has no next_ref! (!)
  class TagLinkedBlock extends Tag {
    short next_ref;
    short[] block_ref;
    int n;

    TagLinkedBlock(short code) throws IOException {
      super(code);
    }

    private void read2(int nb, List<TagLinkedBlock> dataBlocks) throws IOException {
      raf.seek(offset);
      next_ref = raf.readShort();
      block_ref = new short[nb];
      for (int i = 0; i < nb; i++) {
        block_ref[i] = raf.readShort();
        if (block_ref[i] == 0)
          break;
        n++;
      }

      if (debugLinked) System.out.println(" TagLinkedBlock read2 " + detail());
      for (int i = 0; i < n; i++) {
        TagLinkedBlock tag = (TagLinkedBlock) tagMap.get(tagid(block_ref[i], TagEnum.LINKED.getCode()));
        tag.used = true;
        dataBlocks.add(tag);
        if (debugLinked) System.out.println("   Linked data= " + tag.detail());
      }
    }

    public String detail() {
      if (block_ref == null) return super.detail();

      StringBuilder sbuff = new StringBuilder(super.detail());
      sbuff.append(" next_ref= ").append(next_ref);
      sbuff.append(" dataBlks= ");
      for (int i = 0; i < n; i++) {
        short ref = block_ref[i];
        sbuff.append(ref).append(" ");
      }
      return sbuff.toString();
    }

  }


  // 30
  private class TagVersion extends Tag {
    int major, minor, release;
    String name;

    TagVersion(short code) throws IOException {
      super(code);
    }

    protected void read() throws IOException {
      raf.seek(offset);
      major = raf.readInt();
      minor = raf.readInt();
      release = raf.readInt();
      name = raf.readStringMax(length - 12);
    }

    public String value() {
      return major + "." + minor + "." + release + " (" + name + ")";
    }

    public String detail() {
      return super.detail() + " version= " + major + "." + minor + "." + release + " (" + name + ")";
    }
  }

  // 100, 101
  private class TagText extends Tag {
    String text;

    TagText(short code) throws IOException {
      super(code);
    }

    protected void read() throws IOException {
      raf.seek(offset);
      text = raf.readStringMax(length);
    }

    public String detail() {
      String t = (text.length() < 60) ? text : text.substring(0, 59);
      return super.detail() + " text= " + t;
    }
  }

  // 104, 105
  private class TagAnnotate extends Tag {
    String text;
    short obj_tagno, obj_refno;

    TagAnnotate(short code) throws IOException {
      super(code);
    }

    protected void read() throws IOException {
      raf.seek(offset);
      obj_tagno = raf.readShort();
      obj_refno = raf.readShort();
      text = raf.readStringMax(length - 4).trim();
    }

    public String detail() {
      String t = (text.length() < 60) ? text : text.substring(0, 59);
      return super.detail() + " for=" + obj_refno + "/" + obj_tagno + " text=" + t;
    }
  }

  // 106
  private class TagNumberType extends Tag {
    byte version, type, nbits, type_class;

    TagNumberType(short code) throws IOException {
      super(code);
    }

    protected void read() throws IOException {
      raf.seek(offset);
      version = raf.readByte();
      type = raf.readByte();
      nbits = raf.readByte();
      type_class = raf.readByte();
    }

    public String detail() {
      return super.detail() + " version=" + version + " type=" + type + " nbits=" + nbits + " type_class=" + type_class;
    }

    public String toString() {
      return super.toString() + " type=" + H4type.setDataType(type, null) + " nbits=" + nbits;
    }

  }

  // 300, 307, 308 p119
  private class TagRIDimension extends Tag {
    int xdim, ydim;
    short nt_ref, nelems, interlace, compress, compress_ref;
    List<Dimension> dims;

    TagRIDimension(short code) throws IOException {
      super(code);
    }

    protected void read() throws IOException {
      raf.seek(offset);
      xdim = raf.readInt();
      ydim = raf.readInt();
      raf.skipBytes(2);
      nt_ref = raf.readShort();
      nelems = raf.readShort();
      interlace = raf.readShort();
      compress = raf.readShort();
      compress_ref = raf.readShort();
    }

    public String detail() {
      return super.detail() + " xdim=" + xdim + " ydim=" + ydim + " nelems=" + nelems +
          " nt_ref=" + nt_ref + " interlace=" + interlace + " compress=" +
          compress + " compress_ref=" + compress_ref;
    }
  }

  // 301 p121
  private class TagRIPalette extends Tag {
    int[] table;

    TagRIPalette(short code) throws IOException {
      super(code);
    }

    // cant read without info from other tags
    protected void read(int nx, int ny) throws IOException {
      raf.seek(offset);
      table = new int[nx * ny];
      raf.readInt(table, 0, nx * ny);
    }

  }

  // 701 p128
  private class TagSDDimension extends Tag {
    short rank, nt_ref;
    int[] shape;
    short[] nt_ref_scale;

    TagSDDimension(short code) throws IOException {
      super(code);
    }

    protected void read() throws IOException {
      raf.seek(offset);
      rank = raf.readShort();
      shape = new int[rank];
      for (int i = 0; i < rank; i++)
        shape[i] = raf.readInt();

      raf.skipBytes(2);
      nt_ref = raf.readShort();
      nt_ref_scale = new short[rank];
      for (int i = 0; i < rank; i++) {
        raf.skipBytes(2);
        nt_ref_scale[i] = raf.readShort();
      }

    }

    public String detail() {
      StringBuilder sbuff = new StringBuilder(super.detail());
      sbuff.append("   dims= ");
      for (int i = 0; i < rank; i++)
        sbuff.append(shape[i]).append(" ");
      sbuff.append("   nt= ").append(nt_ref).append(" nt_scale=");
      for (int i = 0; i < rank; i++)
        sbuff.append(nt_ref_scale[i]).append(" ");
      return sbuff.toString();
    }

    public String toString() {
      StringBuilder sbuff = new StringBuilder(super.toString());
      sbuff.append("   dims= ");
      for (int i = 0; i < rank; i++)
        sbuff.append(shape[i]).append(" ");
      sbuff.append("   nt= ").append(nt_ref).append(" nt_scale=");
      for (int i = 0; i < rank; i++)
        sbuff.append(nt_ref_scale[i]).append(" ");
      return sbuff.toString();
    }
  }

  // 704, 705, 706 p 130
  private class TagTextN extends Tag {
    String[] text;

    TagTextN(short code) throws IOException {
      super(code);
    }

    private List<String> getList() {
      List<String> result = new ArrayList<>(text.length);
      for (String s : text)
        if (s.trim().length() > 0)
          result.add(s.trim());
      return result;
    }


    protected void read(int n) throws IOException {
      text = new String[n];

      raf.seek(offset);
      byte[] b = new byte[length];
      raf.readFully(b);
      int count = 0;
      int start = 0;
      for (int i = 0; i < length; i++) {
        if (b[i] == 0) {
          text[count] = new String(b, start, i - start, CDM.utf8Charset);
          count++;
          if (count == n) break;
          start = i + 1;
        }
      }
    }
  }

  // 707, p132
  private class TagSDminmax extends Tag {
    ByteBuffer bb;
    DataType dt;

    TagSDminmax(short code) throws IOException {
      super(code);
    }

    protected void read() throws IOException {
      raf.seek(offset);
      byte[] buff = new byte[length];
      raf.readFully(buff);
      bb = ByteBuffer.wrap(buff);
    }

    Number getMin(DataType dataType) {
      dt = dataType;
      return get(dataType, 1);
    }

    Number getMax(DataType dataType) {
      dt = dataType;
      return get(dataType, 0);
    }

    Number get(DataType dataType, int index) {
      if (dataType == DataType.BYTE)
        return bb.get(index);
      if (dataType == DataType.SHORT)
        return bb.asShortBuffer().get(index);
      if (dataType == DataType.INT)
        return bb.asIntBuffer().get(index);
      if (dataType == DataType.LONG)
        return bb.asLongBuffer().get(index);
      if (dataType == DataType.FLOAT)
        return bb.asFloatBuffer().get(index);
      if (dataType == DataType.DOUBLE)
        return bb.asDoubleBuffer().get(index);
      return Double.NaN;
    }

    public String detail() {
      StringBuilder sbuff = new StringBuilder(super.detail());
      sbuff.append("   min= ").append(getMin(dt));
      sbuff.append("   max= ").append(getMax(dt));
      return sbuff.toString();
    }
  }

  // 306 p118; 720 p 127
  private class TagGroup extends Tag {
    int nelems;
    short[] elem_tag, elem_ref;

    TagGroup(short code) throws IOException {
      super(code);
    }

    protected void read() throws IOException {
      raf.seek(offset);
      nelems = length / 4;

      elem_tag = new short[nelems];
      elem_ref = new short[nelems];
      for (int i = 0; i < nelems; i++) {
        elem_tag[i] = raf.readShort();
        elem_ref[i] = raf.readShort();
      }
    }

    public String detail() {
      StringBuilder sbuff = new StringBuilder(super.detail());
      sbuff.append("\n");
      sbuff.append("   tag ref\n   ");
      for (int i = 0; i < nelems; i++) {
        sbuff.append(elem_tag[i]).append(" ");
        sbuff.append(elem_ref[i]).append(" ");
        sbuff.append("\n   ");
      }

      return sbuff.toString();
    }
  }

  // 1965 p135
  private class TagVGroup extends TagGroup {
    short extag, exref, version;
    String name, className;
    Group group;

    TagVGroup(short code) throws IOException {
      super(code);
    }

    protected void read() throws IOException {
      raf.seek(offset);
      nelems = raf.readShort();

      elem_tag = new short[nelems];
      for (int i = 0; i < nelems; i++)
        elem_tag[i] = raf.readShort();

      elem_ref = new short[nelems];
      for (int i = 0; i < nelems; i++)
        elem_ref[i] = raf.readShort();

      short len = raf.readShort();
      name = raf.readStringMax(len);
      len = raf.readShort();
      className = raf.readStringMax(len);

      extag = raf.readShort();
      exref = raf.readShort();
      version = raf.readShort();
    }

    public String toString() {
      return super.toString() + " class= " + className + " name= " + name;
    }

    public String detail() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append(used ? " " : "*").append("refno=").append(refno).append(" tag= ").append(t).append(extended ? " EXTENDED" : "")
          .append(" offset=").append(offset).append(" length=").append(length)
          .append(((vinfo != null) && (vinfo.v != null)) ? " VV=" + vinfo.v.getFullName() : "");
      sbuff.append(" class= ").append(className);
      sbuff.append(" extag= ").append(extag);
      sbuff.append(" exref= ").append(exref);
      sbuff.append(" version= ").append(version);
      sbuff.append("\n");
      sbuff.append(" name= ").append(name);
      sbuff.append("\n");
      sbuff.append("   tag ref\n   ");
      for (int i = 0; i < nelems; i++) {
        sbuff.append(elem_tag[i]).append(" ");
        sbuff.append(elem_ref[i]).append(" ");
        sbuff.append("\n   ");
      }

      return sbuff.toString();
    }
  }

  // 1962 p 136
  private class TagVH extends Tag {
    short interlace, nfields, extag, exref, version;
    int ivsize;
    short[] fld_type, fld_order;
    int[] fld_isize, fld_offset;
    String[] fld_name;
    int nvert; // number of entries in Vdata
    String name, className;
    int tag_len;

    TagVH(short code) throws IOException {
      super(code);
    }

    protected void read() throws IOException {
      raf.seek(offset);
      interlace = raf.readShort();
      nvert = raf.readInt();
      ivsize = DataType.unsignedShortToInt(raf.readShort());
      nfields = raf.readShort();

      fld_type = new short[nfields];
      for (int i = 0; i < nfields; i++)
        fld_type[i] = raf.readShort();

      fld_isize = new int[nfields];
      for (int i = 0; i < nfields; i++)
        fld_isize[i] = DataType.unsignedShortToInt(raf.readShort());

      fld_offset = new int[nfields];
      for (int i = 0; i < nfields; i++)
        fld_offset[i] = DataType.unsignedShortToInt(raf.readShort());

      fld_order = new short[nfields];
      for (int i = 0; i < nfields; i++)
        fld_order[i] = raf.readShort();

      fld_name = new String[nfields];
      for (int i = 0; i < nfields; i++) {
        short len = raf.readShort();
        fld_name[i] = raf.readStringMax(len);
      }

      short len = raf.readShort();
      name = raf.readStringMax(len);

      len = raf.readShort();
      className = raf.readStringMax(len);

      extag = raf.readShort();
      exref = raf.readShort();
      version = raf.readShort();

      tag_len = (int) (raf.getFilePointer() - offset);
    }

    public String toString() {
      return super.toString() + " class= " + className + " name= " + name;
    }

    public String detail() {
      StringBuilder sbuff = new StringBuilder(super.detail());
      sbuff.append(" class= ").append(className);
      sbuff.append(" interlace= ").append(interlace);
      sbuff.append(" nvert= ").append(nvert);
      sbuff.append(" ivsize= ").append(ivsize);
      sbuff.append(" extag= ").append(extag);
      sbuff.append(" exref= ").append(exref);
      sbuff.append(" version= ").append(version);
      sbuff.append(" tag_len= ").append(tag_len);
      sbuff.append("\n");
      sbuff.append(" name= ").append(name);
      sbuff.append("\n");
      sbuff.append("   name    type  isize  offset  order\n   ");
      for (int i = 0; i < nfields; i++) {
        sbuff.append(fld_name[i]).append(" ");
        sbuff.append(fld_type[i]).append(" ");
        sbuff.append(fld_isize[i]).append(" ");
        sbuff.append(fld_offset[i]).append(" ");
        sbuff.append(fld_order[i]).append(" ");
        sbuff.append("\n   ");
      }

      return sbuff.toString();
    }
  }

  //private String clean(String name) {
  //  return StringUtil2.remove(name.trim(), '.'); // just avoid the whole mess by removing "."
  //}

  /* private String readString(int len) throws IOException {
    byte[] b = new byte[len];
    raf.readFully(b);
    int count;
    for (count = 0; count < len; count++)
      if (b[count] == 0)
        break;
    return new String(b, 0, count, CDM.utf8Charset);
  } */

  private class MemTracker {
    private List<Mem> memList = new ArrayList<>();
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
      debugOut.println("======================================");
      debugOut.println("Memory used file size= " + fileSize);
      debugOut.println("  start    end   size   name");
      Collections.sort(memList);
      Mem prev = null;
      for (Mem m : memList) {
        if ((prev != null) && (m.start > prev.end))
          doOne('+', prev.end, m.start, m.start - prev.end, "*hole*");
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

    class Mem implements Comparable<Mem> {
      public String name;
      public long start, end;

      Mem(String name, long start, long end) {
        this.name = name;
        this.start = start;
        this.end = end;
      }

      public int compareTo(Mem m) {
        return Long.compare(start, m.start);
      }

    }
  }

  public List<Tag> getTags() {
    return alltags;
  }

}

