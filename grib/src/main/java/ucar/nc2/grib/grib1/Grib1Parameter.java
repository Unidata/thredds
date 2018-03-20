/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib1;

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
    this.unit = unit; // setUnit(unit);
    this.cfName = null;
  }

  public Grib1Parameter(Grib1ParamTableReader table, int number, String name, String description, String unit, String cf_name) {
    this.table = table;
    this.number = number;
    this.name = setName(name);
    this.description = setDescription(description);
    this.unit = unit; // setUnit(unit);
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
  public int getValue() {
    return -1;
  }

  @Override
  public String getName() {
    return name;
  }

  public boolean useName() {
    return (name != null) && table.useParamName();
  }

   /**
   * description of parameter.
   *
   * @return description
   */
   @Override
  public final String getDescription() {
    return description;
  }

  @Override
  public String getId() {
    return table.getCenter_id() + "." + table.getSubcenter_id() + "." + number;
  }

  /**
   * unit of parameter.
   *
   * @return unit
   */
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

  public String getCFname() {
    return cfName;
  }

  private String setName(String name) {
    if (name == null) return null;
    return StringUtil2.replace(name, ' ', "_"); // replace blanks
  }

  private String setDescription(String description) {
    return GribUtils.cleanupDescription(description);
  }

  private String setUnit(String unit) {
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
    if (cfName != null ? !cfName.equals(that.cfName) : that.cfName != null) return false;
    if (description != null ? !description.equals(that.description) : that.description != null) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (unit != null ? !unit.equals(that.unit) : that.unit != null) return false;

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

