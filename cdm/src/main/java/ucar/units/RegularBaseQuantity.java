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
 * Provides support for a base quantity that is dimensionfull.
 *
 * Instances of this class are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id$
 */
public final class
RegularBaseQuantity
    extends	BaseQuantity
{
    /**
     * Constructs from a name and symbol.
     * @param name		The name of the base unit.
     * @param symbol		The symbol of the base unit.
     */
    public
    RegularBaseQuantity(String name, String symbol)
	throws NameException
    {
	super(name, symbol);
    }

    /**
     * Constructs from a name and a symbol.  This is a trusted constructor
     * for use by the parent class only.
     * @param name		The name of the base unit.
     * @param symbol		The symbol of the base unit.
     */
    protected
    RegularBaseQuantity(String name, String symbol, boolean trusted)
    {
	super(name, symbol, trusted);
    }

    /**
     * Indicates if this base quantity is dimensionless.  Regular base
     * quantities are always dimensionfull.
     * @return			<code>false</code>.
     */
    public boolean
    isDimensionless()
    {
	return false;
    }
}
