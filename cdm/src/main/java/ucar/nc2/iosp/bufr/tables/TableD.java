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
package ucar.nc2.iosp.bufr.tables;

import java.util.*;

/**
 *  BUFR Table D - Data sequences
 *
 * @author caron
 * @since Sep 25, 2008
 */
public class TableD {
  private String name;
  private String location;
  private Map<Short, Descriptor> map;

  public TableD(String name, String location) {
    this.name = name;
    this.location = location;
    map = new HashMap<Short, Descriptor>();
  }

  public String getName() {
    return name;
  }

  public String getLocation() {
    return location;
  }

  Descriptor addDescriptor(short x, short y, String name, List<Short> seq) {
    short id = (short) ((3 << 14) + (x << 8) + y);
    Descriptor d = new Descriptor(x, y, name, seq);
    map.put( id, d);
    return d;
  }

  public Descriptor getDescriptor(short id) {
    return map.get(Short.valueOf(id));
  }

  public Collection<Descriptor> getDescriptors() {
    return map.values();
  }

  public void show( Formatter out) {
    Collection<Short> keys = map.keySet();
    List<Short> sortKeys = new ArrayList<Short>(keys);
    Collections.sort( sortKeys);

    out.format("Table D %s %n",name);
    for (Short key : sortKeys) {
      Descriptor dd = map.get(key);
      dd.show(out, true);
    }
  }

  public class Descriptor implements Comparable<Descriptor> {
    private short x, y;
    private String name;
    private List<Short> seq;
    private boolean localOverride;

    Descriptor(short x, short y, String name, List<Short> seq) {
      this.x = x;
      this.y = y;
      this.name = name;
      this.seq = seq;
    }

    public List<Short> getSequence() {
     return seq;
   }

    public void addFeature(short f) {
     seq.add(f);
   }

    public String getName() {
      return name;
    }

    /**
     * Get fxy as a short
     *
     * @return fxy encoded as a short
     */
    public short getId() {
      return (short) ((3 << 14) + (x << 8) + y);
    }

    /**
     * Get fxy as a String, eg 3-4-22
     *
     * @return fxy encoded as a String
     */
    public String getFxy() {
      return "3-"+x+"-"+y;
    }

    public String toString() { return getFxy()+" "+getName(); }
    
    public void show(Formatter out, boolean oneline) {
      out.format(" %8s: name=(%s) seq=", getFxy(), name);
      if (oneline) {
        for (short s : seq)
          out.format(" %s,", ucar.nc2.iosp.bufr.Descriptor.makeString(s));
        out.format("%n");
      } else {
        for (short s : seq)
          out.format("    %s%n", ucar.nc2.iosp.bufr.Descriptor.makeString(s));
      }
    }

        @Override
    public int compareTo(Descriptor o) {
      return getId() - o.getId();
    }


    public boolean isLocal() {
      return ((x >= 48) || (y >= 192));
    }

    public void setLocalOverride( boolean isOverride) {
      this.localOverride = isOverride;
    }

    public boolean getLocalOverride() {
      return localOverride;
    }

  }
}
