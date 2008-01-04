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
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp.hdf4;

import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Format;
import ucar.nc2.*;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;
import ucar.ma2.Section;

import java.io.IOException;
import java.io.PrintStream;
import java.io.File;
import java.util.*;
import java.nio.ByteBuffer;

/**
 * Read the tags of an HDF4 file, construct CDM objects.
 * All page references are to "HDF Specification and Developers Guide" version 4.1r5, nov 2001.
 *
 * @author caron
 * @since Jul 18, 2007
 */
public class H4header {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H4header.class);

  static private final byte[] head = {0x0e, 0x03, 0x13, 0x01};
  static private final String shead = new String(head);
  static private final long maxHeaderPos = 500000; // header's gotta be within this

  static boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    long pos = 0;
    long size = raf.length();
    byte[] b = new byte[4];

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

  private ucar.nc2.NetcdfFile ncfile;
  private RandomAccessFile raf;
  private long actualSize;

  private Map<Integer, Tag> tagMap = new HashMap<Integer, Tag>();
  private Map<Short, Vinfo> refnoMap = new HashMap<Short, Vinfo>();

  private MemTracker memTracker;
  private PrintStream debugOut = System.out;
  private static boolean debugTag1 = false; // show tags after read(), before read2().
  private static boolean debugTag2 = false;  // show tags after everything is done.
  private static boolean debugTagDetail = false; // when showing tags, show detail or not
  private static boolean debugConstruct = false; // show CDM objects as they are constructed
  private static boolean debugAtt = false; // show CDM attributes as they are constructed
  private static boolean debugLinked = false; // linked data
  private static boolean debugTracker = false; // memory tracker
  private static boolean showFile = false ;

  void read(RandomAccessFile myRaf, ucar.nc2.NetcdfFile ncfile) throws IOException {
    this.raf = myRaf;
    this.ncfile = ncfile;

    actualSize = raf.length();
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
    List<Tag> alltags = new ArrayList<Tag>();
    readDDH(alltags, raf.getFilePointer());

    // now read the individual tags where needed
    for (Tag tag : alltags) {// LOOK could sort by file offset to minimize I/O
      tag.read();
      tagMap.put(tagid(tag.refno, tag.code), tag); // track all tags in a map, key is the "tag id".
      if (debugTag1) System.out.println(debugTagDetail ? tag.detail() : tag);
    }

    // construct the netcdf objects
    ncfile.setLocation(myRaf.getLocation());
    construct(ncfile, alltags);

    if (debugTag2) {
      for (Tag tag : alltags)
        debugOut.println(debugTagDetail ? tag.detail() : tag);
    }

    if (debugTracker) memTracker.report();
  }

  static private int tagid(short refno, short code) {
    int result = (code & 0x3FFF) << 16;
    return result + refno;
  }

  private void construct(ucar.nc2.NetcdfFile ncfile, List<Tag> alltags) throws IOException {

    // decide what netcdf objects are required
    // pass 1
    for (Tag t : alltags) {
      if (t.code == 306) { // raster image
        addImage((TagGroup) t);

      } else if (t.code == 1965) { // Vgroup
        TagVGroup vgroup = (TagVGroup) t;
        if (vgroup.className.startsWith("Dim") || vgroup.className.startsWith("UDim"))
          addDimension(vgroup);
        else if (vgroup.className.startsWith("Var"))
          addVariable(vgroup);
        else if (vgroup.className.startsWith("CDF0.0"))
          addGlobalAttributes(vgroup);
      }
    }

    // pass 2 - unused tags
    for (Tag t : alltags) {
      if (t.used) continue;

      if (t.code == 1962) { // VHeader
        TagVH tagVH = (TagVH) t;
        if (tagVH.className.startsWith("Data"))
          addStructure(tagVH);
      }
      if (t.code == 720) { // numeric data group
        addVariable((TagGroup) t);
      }
    }

    // annotations become attributes
    for (Tag t : alltags) {
      if (t instanceof TagAnnotate) {
        TagAnnotate ta = (TagAnnotate) t;
        Vinfo vinfo = refnoMap.get(ta.obj_refno);
        if (vinfo != null) {
          vinfo.v.addAttribute(new Attribute((t.code == 105) ? "description" : "long_name", ta.text));
          t.used = true;
        }
      }
    }

    // misc global attributes
    ncfile.addAttribute(null, new Attribute("History", "Direct read of HDF4 file through CDM library"));
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

  private boolean checkOffset(int offset) {
    boolean ok = (offset >= 0) && (offset < actualSize);
    if (!ok) log.error("**bad offset=" + offset);
    return ok;
  }

  private void addDimension(TagVGroup group) throws IOException {
    List<TagVH> dims = new ArrayList<TagVH>();
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
        Tag data2 = tagMap.get(tagid(vh.refno, TagEnum.VS.getCode()));
        if (null != data2) {
          raf.seek(data2.offset);
          int length2 = raf.readInt();
          if (debugConstruct)
            System.out.println("dimension length=" + length + " for TagVGroup= " + group + " using data " + data2.refno);
          if (length2 > 0)
            length = length2;
          data2.used = true;
        }
        vh.used = true;
      }
    }

    if (length <= 0) {
      log.error("**bad dimension length=" + length + " for TagVGroup= " + group + " using data " + data.refno);
      length = 1;
    }

    Dimension dim = new Dimension(group.name, length);
    if (debugConstruct) System.out.println("added dimension " + dim + " from VG " + group.refno);
    ncfile.addDimension(null, dim);
  }

  private void addVariable(TagVGroup group) throws IOException {
    Vinfo vinfo = new Vinfo(group.refno);
    vinfo.tags.add(group);

    TagSDDimension dim = null;
    TagNumberType ntag = null;
    TagData data = null;
    List<Dimension> dims = new ArrayList<Dimension>();
    for (int i = 0; i < group.nelems; i++) {
      Tag tag = tagMap.get(tagid(group.elem_ref[i], group.elem_tag[i]));
      if (tag == null) {
        log.error("Reference tag missing= " + group.elem_ref[i] + "/" + group.elem_tag[i]);
        continue;
      }

      vinfo.tags.add(tag);

      if (tag.code == 106)
        ntag = (TagNumberType) tag;

      if (tag.code == 701)
        dim = (TagSDDimension) tag;
      if (tag.code == 702)
        data = (TagData) tag;

      if (tag.code == 1965) {
        TagVGroup vg = (TagVGroup) tag;
        if (vg.className.startsWith("Dim") || vg.className.startsWith("UDim")) {
          Dimension d = ncfile.getRootGroup().findDimension(vg.name);
          if (d == null) throw new IllegalStateException();
          dims.add(d);
          vg.used = true;
        }
      }
      if (tag.code == 720) // assume if contained in Vgroup, then not needed, to avoid redundant variables
        tag.used = true;
    }
    if (ntag == null) {
      log.error("ntype tag missing vgroup= " + group.refno);
      return;
    }
    if (dim == null) {
      log.error("dim tag missing vgroup= " + group.refno);
      return;
    }
    if (data == null) {
      log.error("data tag missing vgroup= " + group.refno);
      return;
    }
    Variable v = new Variable(ncfile, null, null, group.name);
    v.setDimensions(dims);
    H4type.setDataType(ntag.type, v);
    ncfile.addVariable(null, v);

    vinfo.setVariable(v);
    vinfo.setData(data);

    // look for attributes
    for (int i = 0; i < group.nelems; i++) {
      Tag tag = tagMap.get(tagid(group.elem_ref[i], group.elem_tag[i]));
      if (tag == null) throw new IllegalStateException();
      if (tag.code == 1962) {
        TagVH vh = (TagVH) tag;
        if (vh.className.startsWith("Att")) {
          Attribute att = addAttribute(vh);
          if (null != att) v.addAttribute(att);
        }
      }
    }

    group.used = true;
    data.used = true;
    ntag.used = true;
    dim.used = true;
    if (debugConstruct) {
      System.out.println("added variable " + v.getNameAndDimensions() + " from VG " + group.refno);
      System.out.println("  SDdim= " + dim.detail());
      System.out.print("  VGdim= ");
      for (Dimension vdim : dims) System.out.print(vdim + " ");
      System.out.println();
    }
  }

  private void addVariable(TagVH vh) throws IOException {
    Vinfo vinfo = new Vinfo(vh.refno);
    vinfo.tags.add(vh);

    Tag data = tagMap.get(tagid(vh.refno, TagEnum.VS.getCode()));
    if (data == null) {
      log.error("Cant find tag " + vh.refno + "/" + TagEnum.VS.getCode() + " for TagVH=" + vh.detail());
      return;
    }

        // for now assume only 1
    if (vh.nfields != 1)
      throw new IllegalStateException();

    Variable v = new Variable(ncfile, null, null, vh.name);
    try {
      v.setDimensionsAnonymous(new int[] {vh.fld_isize[0]});
    } catch (InvalidRangeException e) {
      throw new IllegalStateException();
    }

    H4type.setDataType(vh.fld_type[0], v);
    ncfile.addVariable(null, v);

    vinfo.start = data.offset;
    vinfo.setVariable(v);

    vh.used = true;
    data.used = true;
  }


  private void addVariable(TagGroup group) throws IOException {
    Vinfo vinfo = new Vinfo(group.refno);
    vinfo.tags.add(group);

    TagSDDimension dim = null;
    TagData data = null;
    for (int i = 0; i < group.nelems; i++) {
      Tag tag = tagMap.get(tagid(group.elem_ref[i], group.elem_tag[i]));
      if (tag == null) {
        log.error("Cant find tag " + group.elem_ref[i] + "/" + group.elem_tag[i] + " for group=" + group.refno);
        continue;
      }
      vinfo.tags.add(tag);

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
    ncfile.addVariable(null, v);

    vinfo.setData(data);
    vinfo.setVariable(v);

    // now that we know n, read attribute tags
    for (int i = 0; i < group.nelems; i++) {
      Tag tag = tagMap.get(tagid(group.elem_ref[i], group.elem_tag[i]));
      if (tag == null) throw new IllegalStateException();
      vinfo.tags.add(tag);

      if (tag.code == 704) {
        TagTextN labels = (TagTextN) tag;
        labels.read(dim.rank);
        tag.used = true;
        v.addAttribute(new Attribute("long_name", labels.getList()));
      }
      if (tag.code == 705) {
        TagTextN units = (TagTextN) tag;
        units.read(dim.rank);
        tag.used = true;
        v.addAttribute(new Attribute("units", units.getList()));
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
    for (int i = 0; i < group.nelems; i++) {
      Tag tag = tagMap.get(tagid(group.elem_ref[i], group.elem_tag[i]));
      if (tag == null) throw new IllegalStateException();
      if (tag.code == 1962) {
        TagVH vh = (TagVH) tag;
        if (vh.className.startsWith("Att")) {
          Attribute att = addAttribute(vh);
          if (null != att) v.addAttribute(att);
        }
      }
    }

    group.used = true;
    data.used = true;
    dim.used = true;
    nt.used = true;
    if (debugConstruct) {
      System.out.println("added variable " + v.getNameAndDimensions() + " from VG " + group.refno);
      System.out.println("  SDdim= " + dim.detail());
      /*System.out.print("  VGdim= ");
      for (Dimension vdim : dims) System.out.print(vdim + " ");
      System.out.println(); */
    }
  }

  private void addGlobalAttributes(TagVGroup group) throws IOException {

    // look for attributes
    for (int i = 0; i < group.nelems; i++) {
      Tag tag = tagMap.get(tagid(group.elem_ref[i], group.elem_tag[i]));
      if (tag == null) throw new IllegalStateException();
      if (tag.code == 1962) {
        TagVH vh = (TagVH) tag;
        if (vh.className.startsWith("Att")) {
          if ((vh.nfields == 1) && (H4type.setDataType(vh.fld_type[0], null) == DataType.CHAR) &&
              (vh.fld_isize[0] > 4000)) { // large EOS metadata - make into variable
            addVariable(vh);
          } else {
            Attribute att = addAttribute(vh);
            if (null != att) ncfile.addAttribute(null, att);
          }
        }
      }
    }

    group.used = true;
  }

  private Attribute addAttribute(TagVH vh) throws IOException {

    Tag data = tagMap.get(tagid(vh.refno, TagEnum.VS.getCode()));
    if (data == null)
      throw new IllegalStateException();

    // for now assume only 1
    if (vh.nfields != 1)
      throw new IllegalStateException();

    String name = vh.name;
    short type = vh.fld_type[0];
    int size = vh.fld_isize[0];
    vh.used = true;
    data.used = true;

    Attribute att = null;
    raf.seek(data.offset);
    switch (type) {
      case 3:
      case 4:
        String val = readString(size);
        att = new Attribute(name, val);
        break;
      case 5:
        float f = raf.readFloat();
        att = new Attribute(name, f);
        break;
      case 6:
        double d = raf.readDouble();
        att = new Attribute(name, d);
        break;
      case 20:
      case 21:
        byte b = raf.readByte();
        att = new Attribute(name, b);
        break;
      case 22:
      case 23:
        short s = raf.readShort();
        att = new Attribute(name, s);
        break;
      case 24:
      case 25:
        int i = raf.readInt();
        att = new Attribute(name, i);
        break;
      case 26:
      case 27:
        long lval = raf.readLong();
        att = new Attribute(name, lval);
        break;
    }

    if (debugAtt) System.out.println("added attribute " + att);
    return att;
  }

  void addImage(TagGroup group) {
    TagRIDimension dimTag = null;
    TagRIPalette palette = null;
    TagNumberType ntag = null;
    Tag data = null;

    // use the list of elements in the group to find the other tags
    for (int i = 0; i < group.nelems; i++) {
      Tag tag = tagMap.get(tagid(group.elem_ref[i], group.elem_tag[i]));
      if (tag == null) throw new IllegalStateException();
      if (tag.code == 300)
        dimTag = (TagRIDimension) tag;
      if (tag.code == 302)
        data = tag;
      if (tag.code == 301)
        palette = (TagRIPalette) tag;
    }
    if ((dimTag == null) || (data == null))
      throw new IllegalStateException();

    // get the NT tag, refered to from the dimension tag
    Tag tag = tagMap.get(tagid(dimTag.nt_ref, TagEnum.NT.getCode()));
    if (tag == null) throw new IllegalStateException();
    ntag = (TagNumberType) tag;

    if (debugConstruct) System.out.println("construct image " + group.refno);
    Vinfo vinfo = new Vinfo(group.refno);
    vinfo.start = data.offset;
    vinfo.tags.add(group);
    vinfo.tags.add(dimTag);
    vinfo.tags.add(data);
    vinfo.tags.add(ntag);

    // assume dimensions are shared for now
    if (dimTag.dims == null) {
      dimTag.dims = new ArrayList<Dimension>();
      dimTag.dims.add(ncfile.addDimension(null, new Dimension("ydim", dimTag.ydim)));
      dimTag.dims.add(ncfile.addDimension(null, new Dimension("xdim", dimTag.xdim)));
    }

    Variable v = new Variable(ncfile, null, null, "Image-" + group.refno);
    H4type.setDataType(ntag.type, v);
    v.setDimensions(dimTag.dims);
    vinfo.setVariable(v);
    ncfile.addVariable(null, v);

    dimTag.used = true;
    ntag.used = true;
    group.used = true;
    data.used = true;
  }

  void addStructure(TagVH vheader) {
    if (debugConstruct) System.out.println("construct struct VH=" + vheader.refno + " name=" + vheader.name);

    Tag data = tagMap.get(tagid(vheader.refno, TagEnum.VS.getCode()));
    if (data == null) {
      log.error("No data for VH refid= " + vheader.refno);
      return;
    }

    Vinfo vinfo = new Vinfo(vheader.refno);
    vinfo.start = data.offset;
    vinfo.recsize = vheader.ivsize;
    vinfo.tags.add(vheader);
    vinfo.tags.add(data);

    try {
      Structure s = new Structure(ncfile, null, null, vheader.name);
      if (vheader.nvert > 1)
        s.setDimensionsAnonymous(new int[]{vheader.nvert});
      else
        s.setIsScalar();
      vinfo.setVariable(s);
      ncfile.addVariable(null, s);

      for (int fld = 0; fld < vheader.nfields; fld++) {
        Variable m = new Variable(ncfile, null, s, vheader.fld_name[fld]);
        short type = vheader.fld_type[fld];
        int size = vheader.fld_isize[fld];
        H4type.setDataType(type, m);
        if ((type == 3) || (type == 4) && (size > 1))
          m.setDimensionsAnonymous(new int[]{size});
        else
          m.setIsScalar();
        m.setSPobject(new Minfo(vheader.fld_offset[fld], size, vheader.fld_order[fld]));
        s.addMemberVariable(m);
      }

    } catch (InvalidRangeException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  // member info
  static class Minfo {
    short order;
    int offset, size;

    Minfo(int offset, int size, short order) {
      this.offset = offset;
      this.size = size;
      this.order = order;
    }
  }

  //////////////////////////////////////////////////////////////////////

  class Vinfo implements Comparable<Vinfo> {
    short refno;

    List<Tag> tags = new ArrayList<Tag>();
    long[] segPos;
    int[] segSize;
    boolean isLinked, isCompressed;

    int start = -1;
    int length;
    int recsize;
    Variable v;

    Vinfo(short refno) {
      this.refno = refno;
      refnoMap.put(refno, this);
    }

    void setVariable(Variable v) {
      this.v = v;
      v.setSPobject(this);
    }

    public int compareTo(Vinfo o) {
      return refno - o.refno;
    }

    void setData(TagData data) throws IOException {
      if (null != data.linked) {
        isLinked = true;
        setDataBlocks(data.linked.getLinkedDataBlocks());

    } else if (null != data.compress) {
        isCompressed = true;
        TagData compData =  data.compress.getDataTag();
        tags.add( compData);
        isLinked = (compData.linked != null);
        if (isLinked)
          setDataBlocks(compData.linked.getLinkedDataBlocks());
        else {
          start = compData.offset;
          length = compData.length;
        }

      } else {
        start = data.offset;
      }
    }

    void setDataBlocks(List<TagLinkedBlock> linkedBlocks) {
      int nsegs = linkedBlocks.size();
      segPos = new long[nsegs];
      segSize = new int[nsegs];
      int count = 0;
      for (TagLinkedBlock tag : linkedBlocks) {
        segPos[count] = tag.offset;
        segSize[count] = tag.length;
        count++;
      }
    }

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("refno=");
      sbuff.append(refno);
      sbuff.append(" variable=");
      sbuff.append(v.getShortName());
      sbuff.append(" data offset=");
      sbuff.append(start);
      sbuff.append("\n");
      for (Tag t : tags)
        sbuff.append(" ").append(t).append("\n");
      return sbuff.toString();
    }
  }

  class DataBlock {
    int offset, length;
    int starting_element;
    Section section;

    DataBlock(int offset, int length, int starting_element) {
      this.offset = offset;
      this.length = length;
      this.starting_element = starting_element;
    }
  }

  //////////////////////////////////////////////////////////////////////

  private void readDDH(List<Tag> alltags, long start) throws IOException {
    raf.seek(start);

    int ndd = DataType.unsignedShortToInt(raf.readShort()); // number of DD blocks
    long link = DataType.unsignedIntToLong(raf.readInt()); // point to the next DDH; link == 0 means no more
    if (debugConstruct) System.out.println(" DDHeader ndd=" + ndd + " link=" + link);

    long pos = raf.getFilePointer();
    for (int i = 0; i < ndd; i++) {
      raf.seek(pos);
      Tag tag = factory();
      pos += 12; // tag usually changed the file pointer
      if (tag.code > 1)
        alltags.add(tag);
    }
    memTracker.add("DD block", start, raf.getFilePointer());

    // any more links in the chain ?
    if (link > 0) {
      readDDH(alltags, link);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////
  // Tags

  Tag factory() throws IOException {
    short code = raf.readShort();
    int ccode = code & 0x3FFF;
    switch (ccode) {
      case 20:
        return new TagLinkedBlock(code);
      case 30:
        return new TagVersion(code);
      case 40:
      case 702:
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

  // Tag == "Data Descriptor" (DD) and (usually) a "Data Element" that the offset/length points to
  private class Tag {
    short code;
    short refno;
    boolean extended;
    int offset, length;
    TagEnum t;
    boolean used;

    // read just the DD part of the tag. see p 11
    Tag(short code) throws IOException {
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
    void read() throws IOException { }

    public String detail() {
      return (used ? " " : "*") + "refno=" + refno + " tag= " + t + (extended ? " EXTENDED" : "") + " offset=" + offset + " length=" + length;
    }

    public String toString() {
      return (used ? " " : "*") + "refno=" + refno + " tag= " + t + (extended ? " EXTENDED" : "" + " length=" + length);
    }
  }

  // 40 (not documented), 702 p 129
  private class TagData extends Tag {
    short ext_tag;
    SpecialLinked linked;
    SpecialComp compress;

    TagData(short code) throws IOException {
      super(code);
    }

    void read() throws IOException {
      if (extended) {
        raf.seek(offset);
        ext_tag = raf.readShort();  // note size wrong in doc

        if (ext_tag == TagEnum.SPECIAL_LINKED) {
          linked = new SpecialLinked();
          linked.read();

        } else if (ext_tag == TagEnum.SPECIAL_COMP) {
          compress = new SpecialComp();
          compress.read();
        }
      }
    }

    public String detail() {
      if (linked != null)
        return super.detail() + " ext_tag= " + ext_tag + " " + linked.detail();
      else if (compress != null)
        return super.detail() + " ext_tag= " + ext_tag + " " + compress.detail();
      else
        return super.detail();
    }

  }

  private class SpecialComp {
    short version, model_type, compress_type, link_ref;
    int uncomp_length;
    TagData dataTag;

    // compress_type == 2
    short signFlag, fillValue;
    int nt, startBit, bitLength;

    // compress_type == 4
    short deflateLevel;


    void read() throws IOException {
      version = raf.readShort();
      uncomp_length = raf.readInt();
      link_ref = raf.readShort();
      model_type = raf.readShort();
      compress_type = raf.readShort();

      if (compress_type == TagEnum.COMP_CODE_NBIT) {
        nt =  raf.readInt();
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
        dataTag = (TagData) tagMap.get(tagid(link_ref, TagEnum.COMPRESSED.getCode()));
        if (dataTag == null)
          throw new IllegalStateException("TagCompress not found for " + detail());
        dataTag.used = true;
      }
      return dataTag;
    }

    public String detail() {
      StringBuffer sbuff = new StringBuffer("SPECIAL_COMP ");
      sbuff.append(" version=" + version + " uncompressed length =" + uncomp_length + " link_ref=" + link_ref);
      sbuff.append(" model_type=" + model_type + " compress_type=" + compress_type);
      if (compress_type == TagEnum.COMP_CODE_NBIT)
        sbuff.append(" nt=" + nt + " signFlag=" + signFlag + " fillValue=" + fillValue + " startBit=" + startBit
        + " bitLength=" + bitLength);
      else if (compress_type == TagEnum.COMP_CODE_DEFLATE)
        sbuff.append(" deflateLevel=" + deflateLevel);
      return sbuff.toString();
    }

  }

  private class SpecialLinked {
    int length, first_len;
    short blk_len, num_blk, link_ref;
    List<TagLinkedBlock> linkedDataBlocks;

    void read() throws IOException {
      length = raf.readInt();
      first_len = raf.readInt();
      blk_len = raf.readShort(); // note size wrong in doc
      num_blk = raf.readShort(); // note size wrong in doc
      link_ref = raf.readShort();
    }

    List<TagLinkedBlock> getLinkedDataBlocks() throws IOException {
      if (linkedDataBlocks == null) {
        linkedDataBlocks = new ArrayList<TagLinkedBlock>();
        if (debugLinked) System.out.println(" TagData readLinkTags " + detail());
        short next = (short) (link_ref & 0x3FFF);
        while (next > 0) {
          TagLinkedBlock tag = (TagLinkedBlock) tagMap.get(tagid(next, TagEnum.LINKED.getCode()));
          if (tag == null)
            throw new IllegalStateException("TagLinkedBlock not found for " + detail());
          tag.used = true;
          tag.read2(num_blk, linkedDataBlocks);
          next = (short) (tag.next_ref & 0x3FFF);
        }
      }
      return linkedDataBlocks;
    }

    public String detail() {
      return "SPECIAL_LINKED length=" + length + " first_len=" + first_len + " blk_len=" + blk_len + " num_blk=" + num_blk + " link_ref=" + link_ref;
    }
  }

  // 20 p 146 Also used for data blocks, which has no next_ref! (!)
  private class TagLinkedBlock extends Tag {
    short next_ref;
    short[] block_ref;
    int n;

    TagLinkedBlock(short code) throws IOException {
      super(code);
    }

    void read2(int nb, List<TagLinkedBlock> dataBlocks) throws IOException {
      raf.seek(offset);
      next_ref = raf.readShort();
      block_ref = new short[nb];
      for (int i = 0; i < nb; i++) {
        block_ref[i] = raf.readShort();
        if (block_ref[i] == 0) break;
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

      StringBuffer sbuff = new StringBuffer(super.detail());
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

    void read() throws IOException {
      raf.seek(offset);
      major = raf.readInt();
      minor = raf.readInt();
      release = raf.readInt();
      name = readString(length - 12);
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

    void read() throws IOException {
      raf.seek(offset);
      text = readString(length);
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

    void read() throws IOException {
      raf.seek(offset);
      obj_tagno = raf.readShort();
      obj_refno = raf.readShort();
      text = readString(length - 4).trim();
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

    void read() throws IOException {
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

    void read() throws IOException {
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
          " nt_ref=" + nt_ref + " interlace=" + interlace + " compress=" + compress;
    }
  }

  // 301 p121
  private class TagRIPalette extends Tag {
    int[] table;

    TagRIPalette(short code) throws IOException {
      super(code);
    }

    // cant read without info from other tags
    void read(int nx, int ny) throws IOException {
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
    List<Dimension> dims;

    TagSDDimension(short code) throws IOException {
      super(code);
    }

    void read() throws IOException {
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
      StringBuffer sbuff = new StringBuffer(super.detail());
      sbuff.append("   dims= ");
      for (int i = 0; i < rank; i++)
        sbuff.append(shape[i]).append(" ");
      sbuff.append("   nt= " + nt_ref + " nt_scale=");
      for (int i = 0; i < rank; i++)
        sbuff.append(nt_ref_scale[i]).append(" ");
      return sbuff.toString();
    }

    public String toString() {
      StringBuffer sbuff = new StringBuffer(super.toString());
      sbuff.append("   dims= ");
      for (int i = 0; i < rank; i++)
        sbuff.append(shape[i]).append(" ");
      sbuff.append("   nt= " + nt_ref + " nt_scale=");
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
      List<String> result = new ArrayList<String>(text.length);
      for (String s : text)
        if (s.trim().length() > 0)
          result.add(s.trim());
      return result;
    }


    void read(int n) throws IOException {
      text = new String[n];

      raf.seek(offset);
      byte[] b = new byte[length];
      raf.read(b);
      int count = 0;
      int start = 0;
      for (int i = 0; i < length; i++) {
        if (b[i] == 0) {
          text[count] = new String(b, start, i - start, "UTF-8");
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

    void read() throws IOException {
      raf.seek(offset);
      byte[] buff = new byte[length];
      raf.read(buff);
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
      StringBuffer sbuff = new StringBuffer(super.detail());
      sbuff.append("   min= " + getMin(dt));
      sbuff.append("   max= " + getMax(dt));
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

    void read() throws IOException {
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
      StringBuffer sbuff = new StringBuffer(super.detail());
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
  private class TagVGroup extends Tag {
    short nelems, extag, exref, version;
    short[] elem_tag, elem_ref;
    String name, className;

    TagVGroup(short code) throws IOException {
      super(code);
    }

    void read() throws IOException {
      raf.seek(offset);
      nelems = raf.readShort();

      elem_tag = new short[nelems];
      for (int i = 0; i < nelems; i++)
        elem_tag[i] = raf.readShort();

      elem_ref = new short[nelems];
      for (int i = 0; i < nelems; i++)
        elem_ref[i] = raf.readShort();

      short len = raf.readShort();
      name = readString(len);
      len = raf.readShort();
      className = readString(len);

      extag = raf.readShort();
      exref = raf.readShort();
      version = raf.readShort();
    }

    public String toString() {
      return super.toString() + " class= " + className + " name= " + name;
    }

    public String detail() {
      StringBuffer sbuff = new StringBuffer(super.detail());
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
    int nvert;
    String name, className;

    TagVH(short code) throws IOException {
      super(code);
    }

    void read() throws IOException {
      raf.seek(offset);
      interlace = raf.readShort();
      nvert = raf.readInt();
      ivsize = DataType.unsignedShortToInt( raf.readShort());
      nfields = raf.readShort();

      fld_type = new short[nfields];
      for (int i = 0; i < nfields; i++)
        fld_type[i] = raf.readShort();

      fld_isize = new int[nfields];
      for (int i = 0; i < nfields; i++)
        fld_isize[i] = DataType.unsignedShortToInt( raf.readShort());

      fld_offset = new int[nfields];
      for (int i = 0; i < nfields; i++)
        fld_offset[i] = DataType.unsignedShortToInt( raf.readShort());

      fld_order = new short[nfields];
      for (int i = 0; i < nfields; i++)
        fld_order[i] = raf.readShort();

      fld_name = new String[nfields];
      for (int i = 0; i < nfields; i++) {
        short len = raf.readShort();
        fld_name[i] = readString(len);
      }

      short len = raf.readShort();
      name = readString(len);
      len = raf.readShort();
      className = readString(len);

      extag = raf.readShort();
      exref = raf.readShort();
      version = raf.readShort();
    }

    public String toString() {
      return super.toString() + " class= " + className + " name= " + name;
    }

    public String detail() {
      StringBuffer sbuff = new StringBuffer(super.detail());
      sbuff.append(" class= ").append(className);
      sbuff.append(" interlace= ").append(interlace);
      sbuff.append(" nvert= ").append(nvert);
      sbuff.append(" ivsize= ").append(ivsize);
      sbuff.append(" extag= ").append(extag);
      sbuff.append(" exref= ").append(exref);
      sbuff.append(" version= ").append(version);
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

  String readString(int len) throws IOException {
    if (len < 0)
      System.out.println("what");
    byte[] b = new byte[len];
    raf.read(b);
    int count;
    for (count = 0; count < len; count++)
      if (b[count] == 0)
        break;
    return new String(b, 0, count, "UTF-8");
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

  ///////////////////////////////////////////////////
  /// testing

  static class MyNetcdfFile extends NetcdfFile {
  }

  static void readAllDir(String dirName, boolean subdirs) throws IOException {
    System.out.println("---------------Reading directory " + dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID " + dirName);
      return;
    }

    for (File f : allFiles) {
      String name = f.getAbsolutePath();
      if (name.endsWith(".hdf"))
        test(name);
    }

    for (File f : allFiles) {
      if (f.isDirectory() && subdirs)
        readAllDir(f.getAbsolutePath(), subdirs);
    }
  }

  static void test(String filename) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    NetcdfFile ncfile = new MyNetcdfFile();
    H4header header = new H4header();
    header.read(raf, ncfile);
    if (showFile) System.out.println(" " + ncfile);
  }

  static public void main(String args[]) throws IOException {
    String filename1 = "17766010.hdf";
    String filename2 = "balloon_sonde.o3_knmi000_de.bilt_s2_20060905t112100z_002.hdf";
    String filename3 = "TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF";
    String filename4 = "f13_owsa_04010_09A.hdf";
    String filename5 = "olslit1995.oct_digital_12.hdf"; // multiple strips plus a raster - crappy
    String filename6 = "c402_rp_02.diag.sfc.20020122_0130z.hdf";  // COARDS, looks good
    String filename7 = "MOP03M-200501-L3V81.0.1.hdf";  // bad links
    String filename8 = "96108_08.hdf";  // bad links
    String filename9 = "eos/AsterSwath.hdf";
    //ucar.unidata.io.RandomAccessFile.setDebugAccess(true);
    test("C:/data/hdf4/" + filename9);
  }
}


/*--------------------------------------------------------------------------
 NAME
    HCPdecode_header -- Decode the compression header info from a memory buffer
 USAGE
    intn HCPdecode_header(model_type, model_info, coder_type, coder_info)
    void * buf;                  IN: encoded compression info header
    comp_model_t *model_type;   OUT: the type of modeling to use
    model_info *m_info;         OUT: Information needed for the modeling type chosen
    comp_coder_t *coder_type;   OUT: the type of encoding to use
    coder_info *c_info;         OUT: Information needed for the encoding type chosen

 RETURNS
    Return SUCCEED or FAIL
 DESCRIPTION
    Decodes the compression information from a block in memory.

 GLOBAL VARIABLES
 COMMENTS, BUGS, ASSUMPTIONS
 EXAMPLES
 REVISION LOG
--------------------------------------------------------------------------
intn
HCPdecode_header(uint8 *p, comp_model_t *model_type, model_info * m_info,
         comp_coder_t *coder_type, comp_info * c_info)
{
    CONSTR(FUNC, "HCPdecode_header");    /* for HERROR
    uint16 m_type, c_type;
    int32 ret_value=SUCCEED;

    /* clear error stack and validate args
    HEclear();
    if (p==NULL || model_type==NULL || m_info==NULL || coder_type==NULL || c_info==NULL)
        HGOTO_ERROR(DFE_ARGS, FAIL);

    UINT16DECODE(p, m_type);     /* get model type
    *model_type=(comp_model_t)m_type;
    UINT16DECODE(p, c_type);     /* get encoding type
    *coder_type=(comp_coder_t)c_type;

    /* read any additional information needed for modeling type
    switch (*model_type)
      {
          default:      /* no additional information needed
              break;
      }     /* end switch */

    /* read any additional information needed for coding type
    switch (*coder_type)
      {
          case COMP_CODE_NBIT:      /* N-bit coding needs info
              {
                  uint16      s_ext;    /* temp. var for sign extend
                  uint16      f_one;    /* temp. var for fill one
                  int32       m_off, m_len;     /* temp. var for mask offset and len

                  /* specify number-type of N-bit data
                  INT32DECODE(p, c_info->nbit.nt);
                  /* next is the flag to indicate whether to sign extend
                  UINT16DECODE(p, s_ext);
                  c_info->nbit.sign_ext = (intn) s_ext;
                  /* the flag to indicate whether to fill with 1's or 0's
                  UINT16DECODE(p, f_one);
                  c_info->nbit.fill_one = (intn) f_one;
                  /* the offset of the bits extracted
                  INT32DECODE(p, m_off);
                  c_info->nbit.start_bit = (intn) m_off;
                  /* the number of bits extracted
                  INT32DECODE(p, m_len);
                  c_info->nbit.bit_len = (intn) m_len;
              }     /* end case
              break;

          case COMP_CODE_SKPHUFF:   /* Skipping Huffman  coding needs info
              {
                  uint32      skp_size,     /* size of skipping unit
                              comp_size;    /* # of bytes to compress

                  /* specify skipping unit size
                  UINT32DECODE(p, skp_size);
                  /* specify # of bytes of skipping data to compress
                  UINT32DECODE(p, comp_size);   /* ignored for now
                  c_info->skphuff.skp_size = (intn) skp_size;
              }     /* end case
              break;

          case COMP_CODE_DEFLATE:   /* Deflation coding stores deflation level
              {
                  uint16      level;    /* deflation level

                  /* specify deflation level
                  UINT16DECODE(p, level);
                  c_info->deflate.level = (intn) level;
              }     /* end case
              break;

          case COMP_CODE_SZIP:   /* Szip coding stores the following values
	      {
                  UINT32DECODE(p, c_info->szip.pixels);
                  UINT32DECODE(p, c_info->szip.pixels_per_scanline);
                  UINT32DECODE(p, c_info->szip.options_mask);
                  c_info->szip.bits_per_pixel = *p++;
                  c_info->szip.pixels_per_block = *p++;
	      }
              break;

          default:      /* no additional information needed
              break;
      }     /* end switch

done:
    if(ret_value == FAIL)
      { /* Error condition cleanup

      } /* end if

    /* Normal function cleanup
    return ret_value;
} /* end HCPdecode_header() */

