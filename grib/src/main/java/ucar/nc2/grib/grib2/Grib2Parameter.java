/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2;

import javax.annotation.Nonnull;
import ucar.nc2.grib.GribTables;
import ucar.nc2.wmo.Util;

import javax.annotation.concurrent.Immutable;

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

  public int compareTo(@Nonnull Grib2Parameter o) {
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
  public String getOperationalStatus() {
    return null;
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
}

