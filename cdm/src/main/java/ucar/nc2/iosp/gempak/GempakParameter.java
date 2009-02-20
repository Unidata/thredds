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




package ucar.nc2.iosp.gempak;


import ucar.grid.GridParameter;

import ucar.unidata.util.StringUtil;


/**
 * Class which represents a GEMPAK parameter.  Add on decimal scale
 * and numeric flag to the superclass.
 */

public class GempakParameter extends GridParameter {

    /**
     * decimal scale
     */
    private int decimalScale = 0;


    /**
     * numeric or char
     */
    private boolean isNumeric = true;


    /**
     * Create a new numeric GEMPAK parameter
     * @param number
     * @param name
     * @param description
     * @param unit of parameter
     * @param scale   decimal (10E*) scaling factor
     */
    public GempakParameter(int number, String name, String description,
                           String unit, int scale) {
        this(number, name, description, unit, scale, true);
    }

    /**
     * Create a new GEMPAK parameter
     * @param number
     * @param name
     * @param description
     * @param unit of parameter
     * @param scale   decimal (10E*) scaling factor
     * @param isNumeric  flag for numeric
     */
    public GempakParameter(int number, String name, String description,
                           String unit, int scale, boolean isNumeric) {
        super(number, name, description, unit);
        decimalScale   = scale;
        this.isNumeric = isNumeric;
    }

    /**
     * Get the decimal scale
     * @return the decimal scale
     */
    public int getDecimalScale() {
        return decimalScale;
    }


    /**
     * Get whether this is numeric or not
     * @return true if numeric
     */
    public boolean getIsNumeric() {
        return isNumeric;
    }


    /**
     * Set whether this is numeric or not
     * @param yesorno true if numeric
     */
    public void setIsNumeric(boolean yesorno) {
        isNumeric = yesorno;
    }

    /**
     * Return a String representation of this object
     *
     * @return a String representation of this object
     * public String toString() {
     *   StringBuffer buf = new StringBuffer(super.toString());
     *   buf.append(" scale: ");
     *   buf.append(getDecimalScale());
     *   return buf.toString();
     * }
     */

    /**
     * Return a String representation of this object
     *
     * @return a String representation of this object
     */
    public String toString() {
        StringBuilder buf = new StringBuilder("GridParameter: ");
        buf.append(StringUtil.padLeft(String.valueOf(getNumber()), 4));
        buf.append(" ");
        String param = getName() + " (" + getDescription() + ")";
        buf.append(StringUtil.padRight(param, 40));
        buf.append(" [");
        buf.append(getUnit());
        buf.append("]");
        buf.append(" scale: ");
        buf.append(getDecimalScale());
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
        if ((o == null) || !(o instanceof GempakParameter)) {
            return false;
        }
        GempakParameter that = (GempakParameter) o;
        return super.equals(that) && (decimalScale == that.decimalScale);
    }

    /**
     * Generate a hash code.
     *
     * @return  the hash code
     */
    public int hashCode() {
        return super.hashCode() + 17 * decimalScale;
    }


}

