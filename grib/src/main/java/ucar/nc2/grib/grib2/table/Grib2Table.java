/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.grib2.table;

/**
 * Seperate the table from the GRIB2Customiser
 *
 * @author caron
 * @since 8/1/2014
 */
public class Grib2Table {
  static public enum Type { wmo, dss, gempak, gsd, kma, ncep, ndfd}

  public final String name;
  public final Type type;
  public final int center, subCenter, masterVersion, localVersion, genProcessId;
  private String path;

  Grib2Table(String name, int center, int subCenter, int masterVersion, int localVersion, int genProcessId, String path, Type type) {
    this.name = name;
    this.path = path;
    this.type = type;

    this.center = center;
    this.subCenter = subCenter;
    this.masterVersion = masterVersion;
    this.localVersion = localVersion;
    this.genProcessId = genProcessId;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Grib2Table that = (Grib2Table) o;

    if (center != that.center) return false;
    if (genProcessId != that.genProcessId) return false;
    if (localVersion != that.localVersion) return false;
    if (masterVersion != that.masterVersion) return false;
    if (subCenter != that.subCenter) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = center;
    result = 31 * result + subCenter;
    result = 31 * result + masterVersion;
    result = 31 * result + localVersion;
    result = 31 * result + genProcessId;
    return result;
  }



}
