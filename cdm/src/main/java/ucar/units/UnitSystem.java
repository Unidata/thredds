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
 * Interface for a system of units.
 *
 * @author Steven R. Emmerson
 * @version $Id$
 */
public interface
UnitSystem
{
    /**
     * Returns the database of base units.
     * @return			The database of base units.
     */
    public UnitDB
    getBaseUnitDB();

    /**
     * Returns the complete database of units (base units and 
     * derived units acceptable for use in the system of units.
     * @return			The complete database of units.
     */
    public UnitDB
    getUnitDB();

    /**
     * Returns the base unit corresponding to a base quantity.
     * @param quantity		A base quantity.
     * @return			The base unit corresponding to the base
     *				quantity or <code>null</code> if no such
     *				unit exists.
     */
    public BaseUnit
    getBaseUnit(BaseQuantity quantity);
}
