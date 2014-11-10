/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.coord;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.EnsCoord;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/**
 * Ensemble coordinates
 *
 * @author caron
 * @since 2/22/14
 */
@Immutable
public class CoordinateEns implements Coordinate {

  private final List<EnsCoord.Coord> ensSorted;
  private final int code;
  private String name ="ens";

  public CoordinateEns(int code, List<EnsCoord.Coord> ensSorted) {
    this.ensSorted = Collections.unmodifiableList(ensSorted);
    this.code = code;
  }

  public List<EnsCoord.Coord> getEnsSorted() {
    return ensSorted;
  }

  @Override
  public List<? extends Object> getValues() {
    return ensSorted;
  }

  @Override
  public int getIndex(Object val) {
    return ensSorted.indexOf(val);
  }

  @Override
  public Object getValue(int idx) {
    return ensSorted.get(idx);
  }

  @Override
  public int getSize() {
    return ensSorted.size();
  }

  @Override
  public int estMemorySize() {
    return 160 + getSize() * ( 8 + Misc.referenceSize);
  }

  @Override
  public Type getType() {
    return Type.ens;
  }

  @Override
  public String getUnit() {
    return ""; // LOOK
  }

  public int getCode() {
    return code;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (!this.name.equals("ens")) throw new IllegalStateException("Cant modify");
    this.name = name;
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s: ", indent, getType());
     for (EnsCoord.Coord level : ensSorted)
       info.format(" %s", level);
    info.format(" (%d)%n", ensSorted.size());
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Ensemble coords: (%s)%n", getUnit());
    for (EnsCoord.Coord level : ensSorted)
      info.format("   %s%n", level);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CoordinateEns that = (CoordinateEns) o;

    if (code != that.code) return false;
    if (!ensSorted.equals(that.ensSorted)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = ensSorted.hashCode();
    result = 31 * result + code;
    return result;
  }

  //////////////////////////////////////////////////////////////

  /* public CoordinateBuilder makeBuilder() {
    return new Builder(code);
  } */

  static public class Builder2 extends CoordinateBuilderImpl<Grib2Record> {
    int code;

    public Builder2(int code) {
      this.code = code;
    }

    @Override
    public Object extract(Grib2Record gr) {
      Grib2Pds pds = gr.getPDS();
      Grib2Pds.PdsEnsemble pdse = (Grib2Pds.PdsEnsemble) pds;
      return new EnsCoord.Coord(pdse.getPerturbationType(), pdse.getPerturbationNumber());
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<EnsCoord.Coord> levelSorted = new ArrayList<>(values.size());
      for (Object val : values) levelSorted.add( (EnsCoord.Coord) val);
      Collections.sort(levelSorted);
      return new CoordinateEns(code, levelSorted);
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
      return new EnsCoord.Coord(pds.getPerturbationType(), pds.getPerturbationNumber());
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<EnsCoord.Coord> levelSorted = new ArrayList<>(values.size());
      for (Object val : values) levelSorted.add( (EnsCoord.Coord) val);
      Collections.sort(levelSorted);
      return new CoordinateEns(code, levelSorted);
    }
  }

}
