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
 * BUFR Table B - Data descriptors
 *
 * @author caron
 * @since Sep 25, 2008
 */
public class TableB {
  private String name;
  private String location;
  private Map<Short, Descriptor> map = new HashMap<Short, Descriptor>();

  public TableB(String name, String location) {
    this.name = name;
    this.location = location;
  }

  void addDescriptor(short x, short y, int scale, int refVal, int width, String name, String units) {
    short id = (short) ((x << 8) + y);
    map.put(id, new Descriptor(x, y, scale, refVal, width, name, units));
  }

  public String getName() {
    return name;
  }

  public String getLocation() {
    return location;
  }

  public Descriptor getDescriptor(short id) {
    return map.get(id);
  }

  Collection<Descriptor> getDescriptors() {
    return map.values();
  }

  public void show(Formatter out) {
    Collection<Short> keys = map.keySet();
    List<Short> sortKeys = new ArrayList(keys);
    Collections.sort(sortKeys);

    out.format("Table B %s %n", name);
    for (Short key : sortKeys) {
      Descriptor dd = map.get(key);
      dd.show(out);
      out.format("%n");
    }
  }

  // inner class
  public class Descriptor {

    private final short x, y;
    private final int scale;
    private final int refVal;
    private final int width;
    private final String units;
    private final String name;
    private final boolean numeric;

    Descriptor(short x, short y, int scale, int refVal, int width, String name, String units) {
      this.x = x;
      this.y = y;
      this.scale = scale;
      this.refVal = refVal;
      this.width = width;
      this.name = name;
      this.units = units;

      this.numeric = !units.equals("CCITT_IA5") && !units.equals("CCITT IA5");
    }


    /**
     * scale of descriptor.
     *
     * @return scale
     */
    public int getScale() {
      return scale;
    }

    /**
     * refVal of descriptor.
     *
     * @return refVal
     */
    public int getRefVal() {
      return refVal;
    }

    /**
     * width of descriptor.
     *
     * @return width
     */
    public int getWidth() {
      return width;
    }

    /**
     * units of descriptor.
     *
     * @return units
     */
    public String getUnits() {
      return units;
    }

    /**
     * short name of descriptor.
     *
     * @return name
     */
    public String getName() {
      return name;
    }

    /**
     * Get fxy as a short
     *
     * @return fxy encoded as a short
     */
    public short getId() {
      return (short) ((x << 8) + y);
    }

    /**
     * Get fxy as a String, eg 0-5-22
     *
     * @return fxy encoded as a String
     */
    public String getFxy() {
      return "0-" + x + "-" + y;
    }

    /**
     * is descriptor numeric or String
     *
     * @return true if numeric
     */
    public boolean isNumeric() {
      return numeric;
    }


    public String toString() {
      Formatter out = new Formatter();
      show(out);
      return out.toString();
    }
    //public String toString() { return getFxy()+": "+getName(); }

    void show(Formatter out) {
      out.format(" %8s scale=%d refVal=%d width=%d  units=(%s) name=(%s)",
              getFxy(), scale, refVal, width, units, name);
    }
  }


}
