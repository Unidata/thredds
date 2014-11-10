/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib;

import net.jcip.annotations.Immutable;
import ucar.nc2.util.Misc;

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


