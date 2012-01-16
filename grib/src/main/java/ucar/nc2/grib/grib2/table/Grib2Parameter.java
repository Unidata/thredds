/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib2.table;

import ucar.nc2.grib.GribTables;
import ucar.nc2.iosp.grid.GridParameter;

/**
 * Describe
 *
 * @author caron
 * @since 1/9/12
 */
public class Grib2Parameter implements GribTables.Parameter, Comparable<Grib2Parameter> {
  public int discipline, category, number;
  public String name, unit, abbrev;

  public Grib2Parameter(int discipline, int category, int number, String name, String unit, String abbrev) {
    this.discipline = discipline;
    this.category = category;
    this.number = number;
    this.name = name.trim();
    this.abbrev = abbrev;
    this.unit = GridParameter.cleanupUnits(unit);
  }

  public String getId() {
    return discipline + "." + category + "." + number;
  }

  public int compareTo(Grib2Parameter o) {
    int c = discipline - o.discipline;
    if (c != 0) return c;
    c = category - o.category;
    if (c != 0) return c;
    return number - o.number;
  }

  @Override
  public int getDiscipline() {
    return discipline;
  }

  @Override
  public int getCategory() {
    return category;
  }

  @Override
  public int getNumber() {
    return number;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getUnit() {
    return unit;
  }

  public String getAbbrev() {
    return abbrev;
  }

  @Override
  public String toString() {
    return "Grib2Parameter{" +
            "discipline=" + discipline +
            ", category=" + category +
            ", number=" + number +
            ", name='" + name + '\'' +
            ", unit='" + unit + '\'' +
            ", abbrev='" + abbrev + '\'' +
            '}';
  }
}

