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

// $Id: Parameter.java,v 1.7 2006/05/05 19:19:36 jeffmc Exp $

package ucar.unidata.util;


/**
 * A parameter has a name and a
 * value that is String, a double, or an array of doubles.
 *
 * @author caron
 * @version $Revision: 1.7 $ $Date: 2006/05/05 19:19:36 $
 */

public class Parameter {

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
     * @return _more_
     */
    public String getName() {
        return name;
    }

    /**
     * True if value is a String ?
     *
     * @return _more_
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
     * _more_
     *
     * @param i _more_
     *
     * @return _more_
     */
    public double getNumericValue(int i) {
        return valueD[i];
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getLength() {
        return valueD.length;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double[] getNumericValues() {
        return valueD;
    }

    /**
     * Instances which have same content are equal.
     *
     * @param oo _more_
     *
     * @return _more_
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
     *
     * @return _more_
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

    /** _more_ */
    private volatile int hashCode = 0;

    /**
     * String representation
     *
     * @return _more_
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
     * _more_
     *
     * @param name _more_
     * @param from _more_
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
     * @param name _more_
     * @param val _more_
     */
    public Parameter(String name, String val) {
        this.name     = name;
        valueS        = val;
        this.isString = true;
    }

    /**
     *  Create a scalar double-valued param.
     *
     * @param name _more_
     * @param value _more_
     */
    public Parameter(String name, double value) {
        this.name = name;
        valueD    = new double[1];
        valueD[0] = value;
    }

    /**
     *  Create a array double-valued param.
     *
     * @param name _more_
     * @param value _more_
     */
    public Parameter(String name, double[] value) {
        this.name = name;
        valueD    = (double[]) value.clone();
    }

}

/*
 *  Change History:
 *  $Log: Parameter.java,v $
 *  Revision 1.7  2006/05/05 19:19:36  jeffmc
 *  Refactor some of the tabbedpane border methods.
 *  Also, since I ran jindent on everything to test may as well caheck it all in
 *
 *  Revision 1.6  2005/03/10 18:40:08  jeffmc
 *  jindent and javadoc
 *
 *  Revision 1.5  2005/01/20 21:21:00  caron
 *  javadoc cleanup
 *
 *  Revision 1.4  2004/12/22 13:28:17  dmurray
 *  Jindent.  somebody else should fix the _more_'s.
 *
 *  Revision 1.3  2004/12/07 01:51:56  caron
 *  make parameter names CF compliant.
 *
 *  Revision 1.2  2004/12/03 04:46:28  caron
 *  no message
 *
 *  Revision 1.1  2004/09/22 21:16:41  caron
 *  add Parameter.java
 *
 *  Revision 1.6  2004/09/09 22:47:39  caron
 *  station updates
 *
 *  Revision 1.5  2004/08/19 20:48:55  edavis
 *  Make public a number of methods for building an Parameter.
 *
 *  Revision 1.4  2004/08/16 20:53:44  caron
 *  2.2 alpha (2)
 *
 *  Revision 1.3  2004/07/12 23:40:16  caron
 *  2.2 alpha 1.0 checkin
 *
 *  Revision 1.2  2004/07/06 19:28:08  caron
 *  pre-alpha checkin
 *
 *  Revision 1.1.1.1  2003/12/04 21:05:27  caron
 *  checkin 2.2
 *
 */

