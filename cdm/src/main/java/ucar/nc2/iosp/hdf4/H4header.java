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

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
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
  private MemTracker memTracker;

  private PrintStream debugOut = System.out;
  private boolean debug = true, debugTracker = true;

  void read(RandomAccessFile myRaf, ucar.nc2.NetcdfFile ncfile) throws IOException {
    this.ncfile = ncfile;
    this.raf = myRaf;
    actualSize = raf.length();
    memTracker = new MemTracker(actualSize);

    if (!isValidFile(myRaf))
      throw new IOException("Not an HDF4 file ");
    // now we are positioned right after the header

    memTracker.add("header", 0, raf.getFilePointer());

    // header information is in le byte order
    raf.order(RandomAccessFile.BIG_ENDIAN);

    if (debug) debugOut.println("H4header 0pened file to read:'" + raf.getLocation() + "', size=" + actualSize);
    readDDH();

    if (debugTracker) memTracker.report();
  }

  private void readDDH() throws IOException {
    long start = raf.getFilePointer();

    short ndd = raf.readShort();
    int link = raf.readInt(); // link == 0 means no more
    System.out.println(" DDHeader ndd="+ndd+" link="+link);

    long pos = raf.getFilePointer();
    for (int i=0; i<ndd; i++) {
      raf.seek(pos);
      Tag tag = factory();
      pos += 12; // tag usually changed the file pointer
      if (debug && (tag.code > 1)) debugOut.println(tag);

    }
    memTracker.add("DD block", start, raf.getFilePointer());    
  }


  ////////////////////////////////////////////////////////////////////////////////
  // Tags

  Tag factory() throws IOException {
    short code = raf.readShort();
    switch (code) {
      case 30 : return new TagVersion(code);
      case 1962 : return new TagVH(code);
      case 1963 : return new TagVS(code);
      case 1965 : return new TagVG(code);
      default : return new Tag(code);
    }
  }

  private class Tag {
    short code, refno;
    int offset, length;
    TagEnum t;

    Tag(short code) throws IOException {
      this.code = code;
      refno = raf.readShort();
      offset = raf.readInt();
      length = raf.readInt();
      t = TagEnum.getTag(code);
      if (code > 1)
        memTracker.add(t.getName(), offset, offset+length);
    }

    public String toString() {
      if (t != null)
        return " tag= "+t+" refno="+refno+" offset="+offset+" length="+length;
      else
        return " tag= "+code+" refno="+refno+" offset="+offset+" length="+length;
    }
  }

  private class TagVersion extends Tag {
    int major, minor, release;
    String name;
    TagVersion(short code) throws IOException {
      super(code);
      raf.seek(offset);
      major = raf.readInt();
      minor = raf.readInt();
      release = raf.readInt();
      name = readString(length-12);
    }
    public String toString() {
      return super.toString()+" version= "+major+"."+minor+"."+release+" ("+name+")";
    }
  }

  private class TagVG extends Tag {
    short nelems, extag, exref, version;
    short[] elem_tag, elem_ref;
    String name, className;

    TagVG(short code) throws IOException {
      super(code);
      raf.seek(offset);
      nelems = raf.readShort();

      elem_tag = new short[nelems];
      for (int i=0; i<nelems; i++)
        elem_tag[i] = raf.readShort();

      elem_ref = new short[nelems];
      for (int i=0; i<nelems; i++)
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
      StringBuffer sbuff = new StringBuffer(super.toString());
      sbuff.append(" class= ").append(className);
      sbuff.append(" extag= ").append(extag);
      sbuff.append(" exref= ").append(exref);
      sbuff.append(" version= ").append(version);
      sbuff.append("\n");
      sbuff.append(" name= ").append(name);
      sbuff.append("\n");
      sbuff.append("   tag ref\n   ");
      for (int i=0; i<nelems; i++) {
        sbuff.append(elem_tag[i]).append(" ");
        sbuff.append(elem_ref[i]).append(" ");
        sbuff.append("\n   ");
      }      

      return sbuff.toString();
    }
  }

  private class TagVH extends Tag {
    short interlace, ivsize, nfields, extag, exref, version;
    short[] fld_type, fld_isize, fld_offset, fld_order;
    String[] fld_name;
    int nvert;
    String name, className;

    TagVH(short code) throws IOException {
      super(code);
      raf.seek(offset);
      interlace = raf.readShort();
      nvert = raf.readInt();
      ivsize = raf.readShort();
      nfields = raf.readShort();

      fld_type = new short[nfields];
      for (int i=0; i<nfields; i++)
        fld_type[i] = raf.readShort();

      fld_isize = new short[nfields];
      for (int i=0; i<nfields; i++)
        fld_isize[i] = raf.readShort();

      fld_offset = new short[nfields];
      for (int i=0; i<nfields; i++)
        fld_offset[i] = raf.readShort();

      fld_order = new short[nfields];
      for (int i=0; i<nfields; i++)
        fld_order[i] = raf.readShort();

      fld_name = new String[nfields];
      for (int i=0; i<nfields; i++) {
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
      StringBuffer sbuff = new StringBuffer(super.toString());
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
      for (int i=0; i<nfields; i++) {
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


  private class TagVS extends Tag {
    TagVS(short code) throws IOException {
      super(code);
    }
  }

  String readString(int len) throws IOException {
    byte[] b = new byte[len];
    raf.read(b);
    int count = len-1;
    while (b[count] == 0) count--;
    return new String(b, 0, count+1);
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

  static public void main( String args[]) throws IOException {
    String filename = "17766010.hdf";
    String filename2 = "balloon_sonde.o3_knmi000_de.bilt_s2_20060905t112100z_002.hdf";
    String filename3 = "TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF";
    RandomAccessFile raf = new RandomAccessFile("C:/data/hdf4/"+filename3,"r");
    //NetcdfFile ncfile = new NetcdfFile();
    H4header header = new H4header();
    header.read(raf, null);
  }
}
