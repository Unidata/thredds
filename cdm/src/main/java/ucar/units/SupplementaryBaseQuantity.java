// $Id: SupplementaryBaseQuantity.java,v 1.5 2000/08/18 04:17:33 russ Exp $
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

/**
 * Provides support for supplementary base quantities.  A supplementary 
 * base quantity is one that is dimensionless (e.g. solid angle).
 *
 * Instances of this class are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: SupplementaryBaseQuantity.java,v 1.5 2000/08/18 04:17:33 russ Exp $
 */
public final class
SupplementaryBaseQuantity
    extends	BaseQuantity
{
    /**
     * Constructs from a name and symbol.
     * @param name		The name of the quantity.
     * @param symbol		The symbol for the quantity.
     * @throws NameException	Bad quantity name.
     */
    public
    SupplementaryBaseQuantity(String name, String symbol)
	throws NameException
    {
	super(name, symbol);
    }

    /**
     * Constructs from a name and symbol.  This is a trusted constructor for
     * use by the superclass only.
     * @param name		The name of the quantity.
     * @param symbol		The symbol for the quantity.
     */
    protected
    SupplementaryBaseQuantity(String name, String symbol, boolean trusted)
    {
	super(name, symbol, trusted);
    }

    /**
     * Indicates whether or not this quantity is dimensionless.  Supplementary
     * base quantities are dimensionless by definition.
     * @return			<code>true</code>.
     */
    public boolean
    isDimensionless()
    {
	/*
	 * These quantities are dimensionless by definition.
	 */
	return true;
    }
}
