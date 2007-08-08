/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
            StringBuffer sbuff = new StringBuffer();
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
        StringBuffer buff = new StringBuffer();
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