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

package ucar.nc2.grib.grib2;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.wmo.Util;
import ucar.unidata.util.StringUtil2;

import java.util.Formatter;
import java.util.List;

/**
 * A Grib-2 parameter
 *
 * @author caron
 * @since 1/9/12
 */
@Immutable
public class Grib2Parameter implements GribTables.Parameter, Comparable<Grib2Parameter> {
  public final int discipline, category, number;
  public final String name, unit, abbrev, desc;
  public final Float fill, missing;

  public Grib2Parameter(int discipline, int category, int number, String name,
                        String unit, String abbrev, String desc, float fill,
                        float missing) {
    this.discipline = discipline;
    this.category = category;
    this.number = number;
    this.name = name.trim();
    this.abbrev = abbrev;
    this.unit = Util.cleanUnit(unit);
    this.desc = desc;
    this.fill = fill;
    this.missing = missing;
  }

  public Grib2Parameter(int discipline, int category, int number, String name, String unit, String abbrev, String desc) {
    this.discipline = discipline;
    this.category = category;
    this.number = number;
    this.name = name.trim();
    this.abbrev = abbrev;
    this.unit = Util.cleanUnit(unit);
    this.desc = desc;
    this.fill = null;
    this.missing = Float.NaN;
  }

  public Grib2Parameter(Grib2Parameter from, String name, String unit) {
    this.discipline = from.discipline;
    this.category = from.category;
    this.number = from.number;
    this.desc = from.desc;
    this.abbrev = from.abbrev;

    this.name = name.trim();
    this.unit = Util.cleanUnit(unit);
    this.fill = null;
    this.missing = Float.NaN;
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
  public int getValue() {
    return -1;
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

  @Override
  public String getAbbrev() {
    return abbrev;
  }

  @Override
  public String getDescription() {
    return desc;
  }

  @Override
  public Float getMissing() {
    return missing;
  }

  @Override
  public Float getFill() {
    return fill;
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
            ", desc='" + desc + '\'' +
            ", fill='" + fill + '\'' +
            ", missing='" + missing + '\'' +
            '}';
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  public static void compareTables(String name1, String name2, List<? extends GribTables.Parameter> test, Grib2Customizer reference, Formatter f) {

    int extra = 0;
    int udunits = 0;
    int conflict = 0;
    f.format("Table 1 : %s%n", name1);
    f.format("Table 2 : %s%n", name2);
    for (GribTables.Parameter p1 : test) {
      Grib2Customizer.Parameter  p2 = reference.getParameter(p1.getDiscipline(), p1.getCategory(), p1.getNumber());
      if (p2 == null) {
        if (p1.getCategory() < 192 && p1.getNumber() < 192) {
          extra++;
          f.format("  WMO missing %s%n", p1);
        }

      } else {
        String p1n = Util.cleanName(p1.getName());
        String p2n = Util.cleanName(p2.getName());

        if (!p1n.equalsIgnoreCase(p2n)) {
          f.format("  p1=%10s %40s %15s %15s %s%n", p1.getId(), p1.getName(), p1.getUnit(), p1.getAbbrev(), p1.getDescription());
          f.format("  p2=%10s %40s %15s %15s %s%n%n", p2.getId(), p2.getName(), p2.getUnit(), p2.getAbbrev(), p2.getDescription());
          conflict++;
        }

        if (!p1.getUnit().equalsIgnoreCase(p2.getUnit())) {
          String cu1 = Util.cleanUnit(p1.getUnit());
          String cu2 = Util.cleanUnit(p2.getUnit());

          // eliminate common non-udunits
          boolean isUnitless1 = isUnitless(cu1);
          boolean isUnitless2 = isUnitless(cu2);

          if (isUnitless1 != isUnitless2) {
            f.format("  ud=%10s %s != %s for %s (%s)%n%n", p1.getId(), cu1, cu2, p1.getId(), p1.getName());
            udunits++;

          } else if (!isUnitless1) {

            try {
              SimpleUnit su1 = SimpleUnit.factoryWithExceptions(cu1);
              if (!su1.isCompatible(cu2)) {
                f.format("  ud=%10s %s (%s) != %s for %s (%s)%n%n", p1.getId(), cu1, su1, cu2, p1.getId(), p1.getName());
                udunits++;
              }
            } catch (Exception e) {
              f.format("  udunits cant parse=%10s %15s %15s%n", p1.getId(), cu1, cu2);
            }
          }

        }
      }
    }
    f.format("Conflicts=%d extra=%d udunits=%d%n%n", conflict, extra, udunits);

    f.format("Parameters in %s not in %s%n", name1, name2);
    int local = 0;
    for (GribTables.Parameter p1 : test) {
      Grib2Customizer.Parameter  p2 = reference.getParameter(p1.getDiscipline(), p1.getCategory(), p1.getNumber());
      if (p2 == null) {
        local++;
        f.format("  %s%n", p1);
      }
    }
    f.format(" missing=%d%n%n", local);

  }

  static boolean isUnitless(String unit) {
    if (unit == null) return true;
    String munge = unit.toLowerCase().trim();
    munge = StringUtil2.remove(munge, '(');
    return munge.length()  == 0 ||
            munge.startsWith("numeric") || munge.startsWith("non-dim") || munge.startsWith("see") ||
            munge.startsWith("proportion") || munge.startsWith("code") || munge.startsWith("0=") ||
            munge.equals("1") ;
  }

}

