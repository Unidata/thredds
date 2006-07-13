// $Id: Prefix.java 64 2006-07-12 22:30:50Z edavis $
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
 * Provides support for unit prefixes (e.g. "centi", "c").
 *
 * Instances of this class are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: Prefix.java 64 2006-07-12 22:30:50Z edavis $
 */
public abstract class
Prefix
    implements	Comparable
{
    /**
     * The value of this prefix.
     * @serial
     */
    private final double	value;

    /**
     * The identifier of this prefix.
     * @serial
     */
    private final String	id;

    /**
     * Constructs from an identifier and a value.
     * @param id		The prefix identifier (e.g. "milli" or "m").
     * @param value		The prefix value (e.g. 1e-3).
     */
    protected
    Prefix(String id, double value)
    {
	this.id = id;
	this.value = value;
    }

    /**
     * Gets the prefix identifier.
     * @return			The prefix identifier.
     */
    public final String
    getID()
    {
	return id;
    }
    
    /**
     * Returns the string representation of this prefix.
     * @return			The string representation of this prefix.
     */
    public final String
    toString()
    {
	return getID();
    }

    /**
     * Gets the prefix value.
     * @return			The prefix value.
     */
    public final double
    getValue()
    {
	return value;
    }

    /**
     * Compares this prefix to another.
     * @param obj		The other prefix.
     * @return			A negative value, zero, or a positive value
     *				depending on whether this prefix is less than
     *				equal to, or greater than <code>obj</code>.
     */
    public abstract int
    compareTo(Object obj);

    /**
     * Compares this prefix to a string.
     * @param string		The string.
     * @return			A negative value, zero, or a positive value
     *				depending on whether this prefix is less than
     *				equal to, or greater than the string.
     */
    public abstract int
    compareTo(String string);

    /**
     * Return the length of the prefix identifier.
     * @return			The length of the prefix identifier.
     */
    public final int
    length()
    {
	return id.length();
    }
}
