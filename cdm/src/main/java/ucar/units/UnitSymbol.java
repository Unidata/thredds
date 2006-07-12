// $Id$
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
 * Provides support for symbols for units.
 *
 * @author Steven R. Emmerson
 * @version $Id$
 */
public final class
UnitSymbol
    extends	UnitID
{
    /**
     * The symbol for the unit.
     * @serial
     */
    private final String	symbol;

    /**
     * Constructs from a symbol.
     * @param symbol		The symbol for the unit.  Shall not be <code>
     *				null</code>.
     */
    public
    UnitSymbol(String symbol)
	throws NameException
    {
	if (symbol == null)
	    throw new NameException("Symbol can't be null");
	this.symbol = symbol;
    }

    /**
     * Returns the name of the unit.  Always returns <code>null</code>.
     * @return			<code>null</code>.
     */
    public String
    getName()
    {
	return null;
    }

    /**
     * Returns the plural form of the name of the unit.  Always returns
     * <code>null</code>.
     * @return			<code>null</code>.
     */
    public String
    getPlural()
    {
	return null;
    }

    /**
     * Returns the symbol for the unit.
     * @return			The symbol for the unit.  Never <code>null
     *				</code>.
     */
    public String
    getSymbol()
    {
	return symbol;
    }

    /**
     * Returns the string representation of this identifier.
     * @return			The string representation of this identifier.
     */
    public String
    toString()
    {
	return getSymbol();
    }
}
