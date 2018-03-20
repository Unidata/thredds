/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.fmrc;

import ucar.nc2.dataset.CoordinateAxis1D;

import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * Represents a vertical coordinate shared among variables.
 */
public class VertCoord implements Comparable {
  private String name, units;
  private int id; // unique id
  private double[] values1, values2;

  VertCoord() {
  }

  VertCoord(CoordinateAxis1D axis) {
    // this.axis = axis;
    this.name = axis.getFullName();
    this.units = axis.getUnitsString();

    int n = (int) axis.getSize();
    if (axis.isInterval()) {
      values1 = axis.getBound1();
      values2 = axis.getBound2();
    } else {
      values1 = new double[n];
      for (int i = 0; i < axis.getSize(); i++)
        values1[i] = axis.getCoordValue(i);
    }
  }

  // copy constructor

  VertCoord(VertCoord vc) {
    this.name = vc.getName();
    this.units = vc.getUnits();
    this.id = vc.id;
    this.values1 = vc.getValues1().clone();
    this.values2 = (vc.getValues2() == null) ? null : vc.getValues2().clone();
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUnits() {
    return units;
  }

  public void setUnits(String units) {
    this.units = units;
  }

  public double[] getValues1() {
    return values1;
  }

  public void setValues1(double[] values) {
    this.values1 = values;
  }

  public double[] getValues2() {
    return values2;
  }

  public void setValues2(double[] values) {
    this.values2 = values;
  }

  public int getSize() {
    return values1.length;
  }

  public boolean equalsData(VertCoord other) {
    if (values1.length != other.values1.length)
      return false;

    for (int i = 0; i < values1.length; i++) {
      if (!ucar.nc2.util.Misc.nearlyEquals(values1[i], other.values1[i]))
        return false;
    }

    if ((values2 == null) && (other.values2 == null))
      return true;

    if ((values2 == null) || (other.values2 == null))
      return false;

    if (values2.length != other.values2.length)
      return false;
    for (int i = 0; i < values2.length; i++) {
      if (!ucar.nc2.util.Misc.nearlyEquals(values2[i], other.values2[i]))
        return false;
    }

    return true;
  }

  public int compareTo(Object o) {
    VertCoord other = (VertCoord) o;
    return name.compareTo(other.name);
  }

  @Override
  public String toString() {
    Formatter out = new Formatter();
    out.format("values=");
    if (values2 == null)
      for (double val : values1) out.format("%5f, ", val);
    else {
      for (int i = 0; i < values1.length; i++) out.format("(%6.3f,%6.3f) ", values1[i], values2[i]);
    }
    out.format("; name=%s", name);
    return out.toString();
  }

  ///////////////////////////////////////////////////////

  static public VertCoord findVertCoord(List<VertCoord> vertCoords, VertCoord want) {
    if (want == null) return null;

    for (VertCoord vc : vertCoords) {
      if (want.equalsData(vc))
        return vc;
    }

    // make a new one
    VertCoord result = new VertCoord(want);
    vertCoords.add(result);
    return result;
  }

  /**
   * Extend result with all the values in the list of VertCoord
   * Sort the values and recreate the double[] values array.
   *
   * @param result extend this coord
   * @param vcList list of VertCoord, may be empty
   */
  static public void normalize(VertCoord result, List<VertCoord> vcList) {
    // get all values into a HashSet of LevelCoord
    Set<LevelCoord> valueSet = new HashSet<>();
    addValues(valueSet, result.getValues1(), result.getValues2());
    for (VertCoord vc : vcList) {
      addValues(valueSet, vc.getValues1(), vc.getValues2());
    }

    // now create a sorted list, transfer to values array
    List<LevelCoord> valueList = Arrays.asList((LevelCoord[]) valueSet.toArray(new LevelCoord[valueSet.size()]));
    Collections.sort(valueList);
    double[] values1 = new double[valueList.size()];
    double[] values2 = new double[valueList.size()];
    boolean has_values2 = false;
    for (int i = 0; i < valueList.size(); i++) {
      LevelCoord lc = valueList.get(i);
      values1[i] = lc.value1;
      values2[i] = lc.value2;
      if (lc.value2 != 0.0)
        has_values2 = true;
    }
    result.setValues1(values1);
    if (has_values2)
      result.setValues2(values2);
  }

  static private void addValues(Set<LevelCoord> valueSet, double[] values1, double[] values2) {
    for (int i = 0; i < values1.length; i++) {
      double val2 = (values2 == null) ? 0.0 : values2[i];
      valueSet.add(new LevelCoord(values1[i], val2));
    }
  }

  @Immutable
  static private class LevelCoord implements Comparable {
    final double mid;
    final double value1;
    final double value2;

    LevelCoord(double value1, double value2) {
      this.value1 = value1;
      this.value2 = value2;
      mid = (value2 == 0) ? value1 : (value1 + value2) / 2;
    }

    public int compareTo(Object o) {
      LevelCoord other = (LevelCoord) o;
      //if (nearlyEquals(value1, other.value1) && nearlyEquals(value2, other.value2)) return 0;
      if (mid < other.mid) return -1;
      if (mid > other.mid) return 1;
      return 0;
    }

    public boolean equals2(Object oo) {
      if (this == oo) return true;
      if (!(oo instanceof LevelCoord)) return false;
      LevelCoord other = (LevelCoord) oo;
      return (ucar.nc2.util.Misc.nearlyEquals(value1, other.value1) && ucar.nc2.util.Misc.nearlyEquals(value2, other.value2));
    }

    public int hashCode2() {
      return (int) (value1 * 100000 + value2 * 100);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      LevelCoord that = (LevelCoord) o;

      if (!ucar.nc2.util.Misc.nearlyEquals(that.value1, value1)) return false;
      if (!ucar.nc2.util.Misc.nearlyEquals(that.value2, value2)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result;
      long temp;
      temp = Double.doubleToLongBits(value1);
      result = (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(value2);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      return result;
    }
  }


}
