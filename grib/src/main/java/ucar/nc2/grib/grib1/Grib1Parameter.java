/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib1;

import java.util.Objects;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.grib1.tables.Grib1ParamTableReader;
import ucar.unidata.util.StringUtil2;

import javax.annotation.concurrent.Immutable;

/**
 * A Grib-1 Parameter
 *
 * @author John
 * @since 9/8/11
 */
@Immutable
public class Grib1Parameter implements GribTables.Parameter {

  private final Grib1ParamTableReader table;  // which table did this come from ?
  private final int number;
  private final String name;
  private final String description;
  private final String unit;
  private final String cfName; // CF standard name, if it exists

  public Grib1Parameter(Grib1ParamTableReader table, int number, String name, String description, String unit) {
    this.table = table;
    this.number = number;
    this.name = setName(name);
    this.description = setDescription(description);
    this.unit = unit; // cleanupUnit(unit);
    this.cfName = null;
  }

  public Grib1Parameter(Grib1ParamTableReader table, int number, String name, String description, String unit, String cf_name) {
    this.table = table;
    this.number = number;
    this.name = setName(name);
    this.description = setDescription(description);
    this.unit = unit; // cleanupUnit(unit);
    this.cfName = cf_name;
  }

  public Grib1ParamTableReader getTable() {
    return table;
  }

  @Override
  public int getDiscipline() {
    return 0;
  }

  @Override
  public int getCategory() {
    return 0;
  }

  @Override
  public int getNumber() {
    return number;
  }

  @Override
  public String getName() {
    return name;
  }

  public boolean useName() {
    return (name != null) && table.useParamName();
  }

  @Override
  public final String getDescription() {
    return description;
  }

  @Override
  public String getId() {
    return table.getCenter_id() + "." + table.getSubcenter_id() + "." + number;
  }

  @Override
  public final String getUnit() {
    return unit;
  }

  @Override
  public String getAbbrev() {
    return null;
  }

  @Override
  public Float getFill() {
    return null;
  }

  @Override
  public Float getMissing() {
    return Float.NaN;
  }

  @Override
  public String getOperationalStatus() {
    return null;
  }

  public String getCFname() {
    return cfName;
  }

  private String setName(String name) {
    if (name == null) name = "";
    return StringUtil2.replace(name, ' ', "_"); // replace blanks
  }

  private String setDescription(String description) {
    return GribUtils.cleanupDescription(description);
  }

  private String cleanupUnit(String unit) {
    return GribUtils.cleanupUnits(unit);
  }

  @Override
  public String toString() {
    return "GridParameter{" +
            "number=" + number +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", unit='" + unit + '\'' +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Grib1Parameter that = (Grib1Parameter) o;

    if (number != that.number) return false;
    if (!Objects.equals(cfName, that.cfName)) return false;
    if (!Objects.equals(description, that.description)) return false;
    if (!Objects.equals(name, that.name)) return false;
    if (!Objects.equals(unit, that.unit)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = number;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (unit != null ? unit.hashCode() : 0);
    result = 31 * result + (cfName != null ? cfName.hashCode() : 0);
    return result;
  }
}

