/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

  void addDescriptor(short x, short y, String name, List<Short> seq) {
    short id = (short) ((3 << 14) + (x << 8) + y);
    map.put( id, new Descriptor(x, y, name, seq));
  }

  public Descriptor getDescriptor(short id) {
    return map.get(Short.valueOf(id));
  }

  public void show( Formatter out) {
    Collection<Short> keys = map.keySet();
    List<Short> sortKeys = new ArrayList<Short>(keys);
    Collections.sort( sortKeys);

    out.format("Table D %s %n",name);
    for (Short key : sortKeys) {
      Descriptor dd = map.get(key);
      dd.show(out);
    }
  }

  public class Descriptor {
    private short x, y;
    private String name;
    private List<Short> seq;

    Descriptor(short x, short y, String name, List<Short> seq) {
      this.x = x;
      this.y = y;
      this.name = name;
      this.seq = seq;
    }

     public List<Short> getSequence() {
      return seq;
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
    
    void show(Formatter out) {
      out.format(" %8s: name=(%s) seq=", getFxy(), name);
      for (short s : seq)
        out.format(" %s,", ucar.nc2.iosp.bufr.Descriptor.makeString(s));
      out.format("%n");
    }

  }
}
