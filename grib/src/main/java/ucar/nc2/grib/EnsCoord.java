/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import ucar.nc2.util.Misc;

import javax.annotation.concurrent.Immutable;
import java.util.Formatter;
import java.util.List;

/**
 * Generalized Ensemble Coordinate
 *
 * @author caron
 * @since 4/1/11
 */
@Immutable
public class EnsCoord  {
  private final List<Coord> coords;

  public EnsCoord(List<Coord> coords) {
    this.coords = coords;
  }

  public int getSize() {
    return coords.size();
  }

  public List<Coord> getCoords() {
    return coords;
  }

  public boolean equalsData(EnsCoord other) {

    if (coords.size() != other.coords.size())
      return false;

    for (int i = 0; i < coords.size(); i++) {
      if (!coords.get(i).equals(other.coords.get(i)))
        return false;
    }

    return true;
  }

  public int findIdx(EnsCoord.Coord coord) {
    for (int i = 0; i < coords.size(); i++) { // LOOK linear search
      if (coords.get(i).equals(coord))
        return i;
    }
    return -1;
  }

  @Override
  public String toString() {
    Formatter out = new Formatter();
    for (Coord lev : coords) out.format("%s, ", lev.toString());
    return out.toString();
  }

  ///////////////////////////////////////////////////////

  static public int findCoord(List<EnsCoord> ensCoords, EnsCoord want) {
    if (want == null) return -1;

    for (int i = 0; i < ensCoords.size(); i++) {
      if (want.equalsData(ensCoords.get(i)))
        return i;
    }

    // make a new one
    ensCoords.add(want);
    return ensCoords.size() - 1;
  }

  @Immutable
  static public class Coord implements Comparable<Coord> {
    final int code;
    final int ensMember;

    public Coord(int code, int ensMember) {
      this.code = code;
      this.ensMember = ensMember;
    }

    public int getCode() {
      return code;
    }

    public int getEnsMember() {
      return ensMember;
    }

    @Override
    public int compareTo(Coord o) {
      int r = Misc.compare(code, o.code);
      if (r != 0) return r;
      return Misc.compare(ensMember, o.ensMember);
    }

    @Override
    public boolean equals(Object oo) {
      if (this == oo) return true;
      if (!(oo instanceof Coord)) return false;
      Coord other = (Coord) oo;
      return (ensMember == other.ensMember) &&  (code == other.code);
    }

    @Override
    public int hashCode() {
      int result = 17;
      result += 31 * ensMember;
      result += 31 * code;
      return result;
    }

    public String toString() {
      Formatter out = new Formatter();
      out.format("(%d %d)", code, ensMember);
      return out.toString();
    }
  }

}


