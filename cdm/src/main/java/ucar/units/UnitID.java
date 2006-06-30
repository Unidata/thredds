// $Id: UnitID.java,v 1.5 2000/08/18 04:17:37 russ Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
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

import java.io.Serializable;

/**
 * Provides support for unit identifiers.
 *
 * @author Steven R. Emmerson
 * @version $Id: UnitID.java,v 1.5 2000/08/18 04:17:37 russ Exp $
 */
public abstract class
UnitID
    implements	Serializable
{
    /**
     * Factory method for constructing an identifier from a name, plural,
     * and symbol.
     * @param name		The name for the unit.  May be <code>null
     *				</code>.
     * @param plural		The plural form of the name.  If <code>null
     *				</code> and <code>name</code> is non-<code>
     *				null</code>, then regular plural-forming rules
     *				are used on the name.
     * @param symbol		The symbol for the unit.  May be <code>null
     *				</code>.
     */
    public static UnitID
    newUnitID(String name, String plural, String symbol)
    {
	UnitID	id;
	try
	{
	    id = name == null
		    ? (UnitID)new UnitSymbol(symbol)
		    : (UnitID)UnitName.newUnitName(name, plural, symbol);
	}
	catch (NameException e)
	{
	    id = null;	// can't happen
	}
	return id;
    }

    /**
     * Returns the name of the unit.
     * @return			The name of the unit.  May be <code>null</code>.
     */
    public abstract String
    getName();

    /**
     * Returns the plural form of the name of the unit.
     * @return			The plural form of the name of the unit.
     *				May be <code>null</code>.
     */
    public abstract String
    getPlural();

    /**
     * Returns the symbol for the unit.
     * @return			The symbol for the unit.  May be
     *				<code>null</code>.
     */
    public abstract String
    getSymbol();

    /**
     * Returns the string representation of this identifier.
     * @return			The string representation of this identifier.
     */
    public abstract String
    toString();
}
