/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
package ucar.nc2.iosp.grid;

import ucar.nc2.wmo.Util;
import ucar.unidata.util.StringUtil2;

/**
 * Class which represents a grid parameter.
 * A parameter consists of a number that can be used to look up in a table,
 * a name( ie Temperature), a description( ie Temperature at 2 meters),
 * and Units( ie K ).
 */

public class GridParameter {

  protected int number;
  protected String name;
  protected String description;
  protected String unit;
  protected String cf_name; // CF standard name, if it exists

  public GridParameter() {
    number = -1;
    name = "undefined";
    description = "undefined";
    unit = "undefined";
  }

  public GridParameter(int number, String name, String description, String unit) {
    this.number = number;
    this.name = name;
    setDescription(description);
    setUnit(unit);
  }

  // unknown param
  public GridParameter(int center, int subcenter, int version, int number) {
    this.number = number;
    name = "undefined";
    description = "Unknown-C"+ center + "-S"+ subcenter + "-V"+ version + ":"+number;
    unit = "undefined";
  }

  public GridParameter(int number, String name, String description, String unit, String cf_name) {
    this.number = number;
    this.name = name;
    setDescription(description);
    setUnit(unit);
    this.cf_name = cf_name;
  }

  public final int getNumber() {
    return number;
  }

  public final String getName() {
    return name;
  }

  /**
   * description of parameter.
   *
   * @return description
   */
  public final String getDescription() {
    return description;
  }

  /**
   * unit of parameter.
   *
   * @return unit
   */
  public final String getUnit() {
    return unit;
  }

  public String getCFname() {
    return cf_name;
  }

  /**
   * sets number of parameter.
   *
   * @param number of parameter
   */
  public final void setNumber(int number) {
    this.number = number;
  }

  /**
   * sets name of parameter.
   *
   * @param name of parameter
   */
  public final void setName(String name) {
    this.name = name;
  }

  /**
   * sets description of parameter.
   *
   * @param description of parameter
   */
  public final void setDescription(String description) {
    this.description = Util.cleanName(description);
  }

  /**
   * sets unit of parameter.
   *
   * @param unit of parameter
   */
  public final void setUnit(String unit) {
    this.unit = Util.cleanUnit(unit);
  }

  /**
   * Return a String representation of this object
   *
   * @return a String representation of this object
   */
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

    GridParameter that = (GridParameter) o;

    if (number != that.number) return false;
    if (cf_name != null ? !cf_name.equals(that.cf_name) : that.cf_name != null) return false;
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
    result = 31 * result + (cf_name != null ? cf_name.hashCode() : 0);
    return result;
  }
}
