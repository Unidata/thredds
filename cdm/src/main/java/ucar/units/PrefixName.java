// $Id: PrefixName.java 64 2006-07-12 22:30:50Z edavis $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
package ucar.units;

/**
 * Provides support for prefix names and numeric values.
 *
 * Instances of this class are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: PrefixName.java 64 2006-07-12 22:30:50Z edavis $
 */
public final class
PrefixName
    extends	Prefix
{
    /**
     * Constructs from a name and a numeric value.
     * @param name		The name for the prefix.
     * @param value		The numeric value for the prefix.
     */
    public
    PrefixName(String name, double value)
    {
	super(name, value);
    }

    /**
     * Compares this PrefixName with another PrefixName.
     * @param obj		The other PrefixName.
     * @return			A negative value, zero, or a positive value
     *				depending on whether this PrefixName is less
     *				than, equal to, or greater than <code>
     *				obj</code>, respectively.
     */
    public final int
    compareTo(Object obj)
    {
	return getID().compareToIgnoreCase(((PrefixName)obj).getID());
    }

    /**
     * Compares this PrefixName with a string.
     * @param string		The string to compare this PrefixName against.
     * @return			A negative value, zero, or a positive value
     *				depending on whether this PrefixName is less
     *				than, equal to, or greater than <code>
     *				string</code>, respectively.
     */
    public final int
    compareTo(String string)
    {
	return getID().length() >= string.length()
		? getID().compareToIgnoreCase(string)
		: getID().compareToIgnoreCase(
		    string.substring(0, getID().length()));
    }
}
