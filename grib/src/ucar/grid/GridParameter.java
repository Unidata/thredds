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



package ucar.grid;

import java.util.Formatter;

/**
 * Class which represents a grid parameter.
 * A parameter consists of a number that can be used to look up in a table,
 * a name( ie Temperature), a description( ie Temperature at 2 meters),
 * and Units( ie K ).
 */

public class GridParameter {

    /**
     * parameter number.
     */
    // TODO: reinstate private
    //private int number;
    protected int number;

    /**
     * name of parameter.
     */
    //private String name;
    protected String name;

    /**
     * description of parameter.
     */
    //private String description;
    protected String description;

    /**
     * unit of Parameter.
     */
    //private String unit;
    protected String unit;

    /**
     * constructor.
     */
    public GridParameter() {
        number      = -1;
        name        = "undefined";
        description = "undefined";
        unit        = "undefined";
    }

    /**
     * constructor.
     * @param number
     * @param name
     * @param description
     * @param unit of parameter
     */
    public GridParameter(int number, String name, String description, String unit) {
      this.number      = number;
        this.name        = name;
        this.description = description;
        this.unit        = unit;
    }

    /**
     * number of parameter.
     * @return number
     */
    public final int getNumber() {
        return number;
    }

    /**
     * name of parameter.
     * @return name
     */
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
     * @return unit
     */
    public final String getUnit() {
        return unit;
    }

    /**
     * sets number of parameter.
     * @param number of parameter
     */
    public final void setNumber(int number) {
        this.number = number;
    }

    /**
     * sets name of parameter.
     * @param name  of parameter
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * sets description of parameter.
     * @param description of parameter
     */
    public final void setDescription(String description) {
        this.description = description;
    }

    /**
     * sets unit of parameter.
     * @param unit of parameter
     */
    public final void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * Return a String representation of this object
     *
     * @return a String representation of this object
     */
    public String toString() {
      Formatter buf = new Formatter();
      buf.format("GridParameter: %4d %s [%s]", getNumber(), getName(), getUnit());
      return buf.toString();
    }

    /**
     * Check for equality
     *
     * @param o  the object in question
     *
     * @return  true if has the same parameters
     */
    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof GridParameter)) {
            return false;
        }
        GridParameter that = (GridParameter) o;
        return (number == that.number) && name.equals(that.name)
               && description.equals(that.description)
               && unit.equals(that.unit);
    }

    /**
     * Generate a hash code.
     *
     * @return  the hash code
     */
    public int hashCode() {
        return number + name.hashCode() + description.hashCode()
               + unit.hashCode();
    }


}
