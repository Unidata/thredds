// $Id: PrefixSymbol.java,v 1.5 2000/08/18 04:17:30 russ Exp $
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
 * Provides support for prefix symbols.
 *
 * Instances of this class are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: PrefixSymbol.java,v 1.5 2000/08/18 04:17:30 russ Exp $
 */
public final class
PrefixSymbol
    extends	Prefix
{
    /**
     * Constructs from a name and a numeric value.
     * @param name		The name for the prefix.
     * @param value		The numeric value for the prefix.
     */
    public
    PrefixSymbol(String name, double value)
    {
	super(name, value);
    }

    /**
     * Compares this prefix against another PrefixSymbol.  The sort keys are
     * decreasing length (major) and increasing lexicality (minor).
     * @param obj		The other PrefixSymbol.
     * @return			A negative value, zero, or a positive value
     *				depending on whether this PrefixSymbol is less
     *				than, equal to, or greater than <code>
     *				obj</code>, respectively.
     */
    public final int
    compareTo(Object obj)
    {
	String	thatID = ((PrefixSymbol)obj).getID();
	int	comp = thatID.length() - getID().length();
	if (comp == 0)
	    comp = getID().compareTo(thatID);
	return comp;
    }

    /**
     * Compares this prefix against a String.  The sort keys are
     * decreasing length (major) and increasing lexicality (minor).
     * @param string		The string.
     * @return			A negative value, zero, or a positive value
     *				depending on whether this PrefixSymbol is less
     *				than, equal to, or greater than <code>
     *				string</code>, respectively.
     */
    public final int
    compareTo(String string)
    {
	int	comp = string.length() - getID().length();
	return comp < 0
		? comp
		: comp == 0
		    ? (getID().compareTo(string) == 0 ? 0 : -1)
		    : (getID().compareTo(string.substring(0, getID().length()))
			== 0
			    ? 0
			    : -1);
    }
}
