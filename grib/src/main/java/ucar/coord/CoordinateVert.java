/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.coord;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.VertCoord;
import ucar.nc2.grib.grib1.Grib1ParamLevel;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.util.Counters;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Misc;

import java.util.*;

/**
 * Vertical GRIB coordinates
 * Effectively immutable; setName() can only be called once.
 *
 * @author caron
 * @since 11/27/13
 */
@Immutable
public class CoordinateVert implements Coordinate {

  private final List<VertCoord.Level> levelSorted;
  private final int code; // Grib1 - code table 3; Grib2 - Code table 4.5
  private String name;
  private final VertCoord.VertUnit vunit;
  private final boolean isLayer;

  public CoordinateVert(int code, VertCoord.VertUnit vunit, List<VertCoord.Level> levelSorted) {
    this.levelSorted = Collections.unmodifiableList(levelSorted);
    this.code = code;
    this.vunit = vunit;
    this.isLayer = levelSorted.get(0).isLayer();
  }

  public List<VertCoord.Level> getLevelSorted() {
    return levelSorted;
  }

  /* @Override
  public int findIndexContaining(double need) {
    if (isLayer()) {
      int idx = findSingleHit(need);
      if (idx >= 0) return idx;
      if (idx == -1) return -1;

      // multiple hits = choose closest to the midpoint
      return findClosest(need);

    } else {
      for (int i=0; i<levelSorted.size(); i++) {
        VertCoord.Level level = levelSorted.get(i);
        if (Misc.closeEnough(need, level.getValue1())) return i;
      }
      return -1;
    }
  }

  // return index if only one match, if no matches return -1, if > 1 match return -nhits
  private int findSingleHit(double target) {
    int hits = 0;
    int idxFound = -1;
    for (int i = 0; i < levelSorted.size(); i++) {
      VertCoord.Level level = levelSorted.get(i);
      if (contains(target, level)) {
        hits++;
        idxFound = i;
      }
    }
    if (hits == 1) return idxFound;
    if (hits == 0) return -1;
    return -hits;
  }

  // return index of closest value to target
  private int findClosest(double target) {
    double minDiff =  Double.MAX_VALUE;
    int idxFound = -1;
    for (int i = 0; i < levelSorted.size(); i++) {
      VertCoord.Level level = levelSorted.get(i);
      double midpoint = (level.getValue1()+level.getValue2()) /2;
      double diff =  Math.abs(midpoint - target);
      if (diff < minDiff) {
        minDiff = diff;
        idxFound = i;
      }
    }
    return idxFound;
  }

  private boolean contains(double target, VertCoord.Level level) {
    if (level.getValue1() <= target && target <= level.getValue2()) return true;
    return level.getValue1() >= target && target >= level.getValue2();
  } */

  @Override
  public List<? extends Object> getValues() {
    return levelSorted;
  }

  @Override
  public int getIndex(Object val) {
    return levelSorted.indexOf(val);
  }

  @Override
  public Object getValue(int idx) {
    if (idx >= levelSorted.size())
      return null;
    return levelSorted.get(idx);
  }

  @Override
  public int getSize() {
    return levelSorted.size();
  }

  @Override
  public int getNCoords() {
    return getSize();
  }

  @Override
  public Type getType() {
    return Type.vert;
  }

  @Override
  public int estMemorySize() {
    return 160 + getSize() * ( 40 + Misc.referenceSize);
  }

  @Override
  public String getUnit() {
    return vunit == null ? null : vunit.getUnits();
  }

  public VertCoord.VertUnit getVertUnit() {
    return vunit;
  }

  public boolean isLayer() {
    return isLayer;
  }

  public boolean isPositiveUp() {
    return vunit.isPositiveUp();
  }

  public int getCode() {
    return code;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (this.name != null) throw new IllegalStateException("Cant modify");
    this.name = name;
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s: ", indent, getType());
     for (VertCoord.Level level : levelSorted)
       info.format(" %s", level);
    info.format(" (%d)%n", levelSorted.size());
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Levels: (%s)%n", getUnit());
    for (VertCoord.Level level : levelSorted)
      info.format("   %s%n", level);
  }

  @Override
  public Counters calcDistributions() {
    ucar.nc2.util.Counters counters = new Counters();
    counters.add("resol");

    if (isLayer) {
      counters.add("intv");
      for (int i = 0; i < levelSorted.size(); i++) {
        double intv = (levelSorted.get(i)).getValue2() - (levelSorted.get(i)).getValue1();
        counters.count("intv", intv);
        if (i < levelSorted.size() - 1) {
          double resol = (levelSorted.get(i + 1)).getValue1() - (levelSorted.get(i)).getValue1();
          counters.count("resol", resol);
        }
      }

    } else {

      for (int i = 0; i < levelSorted.size() - 1; i++) {
        double diff = levelSorted.get(i + 1).getValue1() - levelSorted.get(i).getValue1();
        counters.count("resol", diff);
      }
    }

    return counters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CoordinateVert that = (CoordinateVert) o;

    if (code != that.code) return false;
    if (!levelSorted.equals(that.levelSorted)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = levelSorted.hashCode();
    result = 31 * result + code;
    return result;
  }

  //////////////////////////////////////////////////////////////

  /* public CoordinateBuilder makeBuilder() {
    return new Builder(code);
  } */

  static public class Builder2 extends CoordinateBuilderImpl<Grib2Record> {
    int code;
    VertCoord.VertUnit vunit;

    public Builder2(int code, VertCoord.VertUnit vunit) {
      this.code = code;
      this.vunit = vunit;
    }

    @Override
    public Object extract(Grib2Record gr) {
      Grib2Pds pds = gr.getPDS();
      if (Grib2Utils.isLayer(pds))
        return new VertCoord.Level(pds.getLevelValue1(), pds.getLevelValue2());
      else
        return new VertCoord.Level(pds.getLevelValue1());
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<VertCoord.Level> levelSorted = new ArrayList<>(values.size());
      for (Object val : values) levelSorted.add( (VertCoord.Level) val);
      Collections.sort(levelSorted);
      return new CoordinateVert(code, vunit, levelSorted);
    }
  }

  static public class Builder1 extends CoordinateBuilderImpl<Grib1Record> {
    int code;
    Grib1Customizer cust;

    public Builder1(Grib1Customizer cust, int code) {
      this.cust = cust;
      this.code = code;
    }

    @Override
    public Object extract(Grib1Record gr) {
      Grib1SectionProductDefinition pds = gr.getPDSsection();
      boolean isLayer = cust.isLayer(pds.getLevelType());
      Grib1ParamLevel plevel = cust.getParamLevel(pds);
      if (isLayer)
        return new VertCoord.Level(plevel.getValue1(), plevel.getValue2());
      else
        return new VertCoord.Level(plevel.getValue1());
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<VertCoord.Level> levelSorted = new ArrayList<>(values.size());
      for (Object val : values) levelSorted.add( (VertCoord.Level) val);
      Collections.sort(levelSorted);
      return new CoordinateVert(code, cust.getVertUnit(code), levelSorted);
    }
  }

}
