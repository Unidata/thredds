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

import net.jcip.annotations.Immutable;

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
  private Map<Short, Descriptor> map;

  public TableB(String name, String location) {
    this.name = name;
    this.location = location;
    map = new HashMap<Short, Descriptor>();
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

  public Collection<Descriptor> getDescriptors() {
    return map.values();
  }

  public Collection<Short> getKeys() {
    return map.keySet();
  }

  public void show(Formatter out) {
    List<Short> sortKeys = new ArrayList<Short>(getKeys());
    Collections.sort(sortKeys);

    out.format("Table B %s %n", name);
    for (Short key : sortKeys) {
      Descriptor dd = getDescriptor(key);
      dd.show(out);
      out.format("%n");
    }
  }

  /**
   * Composite pattern - collection of TableB
   */
  static public class Composite extends TableB {
    List<TableB> list = new ArrayList<TableB>(3);

    public Composite(String name, String location) {
      super(name, location);
    }

    public void addTable(TableB b) {
      list.add(b);
    }

    @Override
    public Descriptor getDescriptor(short id) {
      for (TableB b : list) {
        Descriptor d = b.getDescriptor(id);
        if (d != null) return d;
      }
      return null;
    }

    @Override
    public Collection<Descriptor> getDescriptors() {
      ArrayList<Descriptor> result = new ArrayList<Descriptor>(3000);
      for (TableB b : list)
        result.addAll(b.getDescriptors());
      return result;
    }

    @Override
    public Collection<Short> getKeys() {
      ArrayList<Short> result = new ArrayList<Short>(3000);
      for (TableB b : list)
        result.addAll(b.getKeys());
      return result;
    }
  }

  // inner class
  @Immutable
  public class Descriptor implements Comparable<Descriptor> {

    private final short x, y;
    private final int scale;
    private final int refVal;
    private final int dataWidth;
    private final String units;
    private final String name;
    private final boolean numeric;
    private boolean localOverride;

    Descriptor(short x, short y, int scale, int refVal, int width, String name, String units) {
      this.x = x;
      this.y = y;
      this.scale = scale;
      this.refVal = refVal;
      this.dataWidth = width;
      this.name = name.trim();
      this.units = units.trim().intern();

      this.numeric = !this.units.startsWith("CCITT");
    }

    public int getScale() {
      return scale;
    }

    public int getRefVal() {
      return refVal;
    }

    public int getDataWidth() {
      return dataWidth;
    }

    public String getUnits() {
      return units;
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

    public boolean isLocal() {
      return ((x >= 48) || (y >= 192));
    }

    public void setLocalOverride( boolean isOverride) {
      this.localOverride = isOverride;
    }

    public boolean getLocalOverride() {
      return localOverride;
    }

     public String toString() {
      Formatter out = new Formatter();
      show(out);
      return out.toString();
    }

    void show(Formatter out) {
      out.format(" %8s scale=%d refVal=%d width=%d  units=(%s) name=(%s)",
              getFxy(), scale, refVal, dataWidth, units, name);
    }

    @Override
    public int compareTo(Descriptor o) {
      return getId() - o.getId();
    }
  }


}
