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

package ucar.unidata.util;

/**
 * A parameter has a name and a value that is String, a double, or an array of doubles.
 * A substitute for ucar.nc2.Attribute, to prevent dependencies of the ucar.unidata packages on ucar.nc2.
 *
 * @author caron
 */

public class Parameter implements java.io.Serializable {

    /** _more_ */
    private String name;

    /** _more_ */
    private String valueS;

    /** _more_ */
    private double[] valueD;

    /** _more_ */
    private boolean isString;

    /**
     * Get the name of this Parameter.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * True if value is a String.
     * @return if its String valued
     */
    public boolean isString() {
        return isString;
    }

    /**
     * Retrieve String value; only call if isString() is true.
     * @return String if this is a String valued attribute, else null.
     */
    public String getStringValue() {
        if (valueS == null) {
            StringBuilder sbuff = new StringBuilder();
            for (int i = 0; i < valueD.length; i++) {
                double v = valueD[i];
                sbuff.append(v + " ");
            }
            valueS = sbuff.toString();
        }
        return valueS;
    }

    /**
     * Retrieve numeric value, use if isString() is false.
     * Equivalent to <code>getNumericValue(0)</code>
     * @return the first element of the value array, or null if its a String.
     */
    public double getNumericValue() {
        return valueD[0];
    }

    /**
     * Get the ith numeric value.
     * @param i index
     * @return ith numeric value
     */
    public double getNumericValue(int i) {
        return valueD[i];
    }

    /**
     * Get the number of values.
     * @return the number of values.
     */
    public int getLength() {
        return valueD.length;
    }

    /**
     * Get array of numeric values as doubles.
     * Do not modify unless you own this object!
     * @return array of numeric values.
     */
    public double[] getNumericValues() {
        return valueD;
    }

    /**
     * Instances which have same content are equal.
     * @param oo compare to this Parameter.
     * @return true if equal.
     */
    public boolean equals(Object oo) {
        if (this == oo) {
            return true;
        }
        if ( !(oo instanceof Parameter)) {
            return false;
        }
        return hashCode() == oo.hashCode();
    }

    /**
     * Override Object.hashCode() to implement equals.
     * @return haschcode
     */
    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + getName().hashCode();
            if (valueS != null) {
                result = 37 * result + getStringValue().hashCode();
            }
            if (valueD != null) {
                for (int i = 0; i < valueD.length; i++) {
                    result += (int) 1000 * valueD[i];
                }
            }
            hashCode = result;
        }
        return hashCode;
    }

    private volatile int hashCode = 0;

    /**
     * String representation
     * @return nice String
     */
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append(getName());
        if (isString()) {
            buff.append(" = ");
            buff.append(valueS);
        } else {
            buff.append(" = ");
            for (int i = 0; i < getLength(); i++) {
                if (i != 0) {
                    buff.append(", ");
                }
                buff.append(getNumericValue(i));
            }
        }
        return buff.toString();
    }


    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Copy constructir, with new name.
     *
     * @param name name of new Parameter.
     * @param from copy values from here.
     */
    public Parameter(String name, Parameter from) {
        this.name     = name;
        this.valueS   = from.valueS;
        this.valueD   = from.valueD;
        this.isString = from.isString;
    }

    /**
     * Create a String-valued param.
     *
     * @param name name of new Parameter.
     * @param val value of Parameter
     */
    public Parameter(String name, String val) {
        this.name     = name;
        valueS        = val;
        this.isString = true;
    }

    /**
     *  Create a scalar double-valued param.
     *
     * @param name name of new Parameter.
     * @param value value of Parameter
     */
    public Parameter(String name, double value) {
        this.name = name;
        valueD    = new double[1];
        valueD[0] = value;
    }

    /**
     *  Create a array double-valued param.
     *
     * @param name name of new Parameter.
     * @param value value of Parameter
     */
    public Parameter(String name, double[] value) {
        this.name = name;
        valueD    = (double[]) value.clone();
    }

}