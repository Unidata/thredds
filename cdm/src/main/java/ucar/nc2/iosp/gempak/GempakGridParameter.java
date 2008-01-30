/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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



package ucar.nc2.iosp.gempak;


import ucar.unidata.util.StringUtil;
import ucar.nc2.iosp.grid.GridParameter;


/**
 * Class which represents a GEMPAK grid parameter.  Add on decimal scale
 */

public class GempakGridParameter extends GridParameter {

    /**
     * decimal scale
     */
    private int decimalScale = 0;


    /**
     * Create a new GEMPAK grid parameter
     * @param number
     * @param name
     * @param description
     * @param unit of parameter
     * @param scale   decimal (10E*) scaling factor
     */
    public GempakGridParameter(int number, String name, String description,
                         String unit, int scale) {
        super(number, name, description, unit);
        decimalScale = scale;
    }

    /**
     * Get the decimal scale
     * @return the decimal scale
     */
    public int getDecimalScale() {
        return decimalScale;
    }

    /**
     * Return a String representation of this object
     *
     * @return a String representation of this object
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());
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
        if ((o == null) || !(o instanceof GempakGridParameter)) {
            return false;
        }
        GempakGridParameter that = (GempakGridParameter) o;
        return super.equals(that) &&
               decimalScale == that.decimalScale;
    }

    /**
     * Generate a hash code.
     *
     * @return  the hash code
     */
    public int hashCode() {
        return super.hashCode() + 17*decimalScale;
    }


}

