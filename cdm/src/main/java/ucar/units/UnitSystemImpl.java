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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Steven R. Emmerson
 * @version $Id$
 */
public class
UnitSystemImpl
    implements	UnitSystem, Serializable
{
    /**
     * The quantity-to-base-unit map.
     * @serial
     */
    private final HashMap	quantityMap;

    /**
     * The base unit database;
     * @serial
     */
    private final UnitDB	baseUnitDB;

    /**
     * The complete database;
     * @serial
     */
    private final UnitDBImpl	acceptableUnitDB;

    /**
     * Constructs from a base unit database and a derived unit database.
     * @param baseUnitDB	The base unit database.  Shall only contain
     *				base units.
     * @param derivedUnitDB	The derived unit database.  Shall not contain
     *				any base units.
     * @throws UnitExistsException	A unit with the same identifier exists
     *					in both databases.
     */
    protected
    UnitSystemImpl(UnitDBImpl baseUnitDB, UnitDBImpl derivedUnitDB)
	throws UnitExistsException
    {
	quantityMap = new HashMap(baseUnitDB.nameCount());
	for (Iterator iter = baseUnitDB.getIterator(); iter.hasNext(); )
	{
	    Unit	unit = (Unit)iter.next();
	    BaseUnit	baseUnit = (BaseUnit)unit;
	    quantityMap.put(baseUnit.getBaseQuantity(), baseUnit);
	}
	this.baseUnitDB = baseUnitDB;
	acceptableUnitDB =
	    new UnitDBImpl(
		baseUnitDB.nameCount() + derivedUnitDB.nameCount(),
		baseUnitDB.symbolCount() + derivedUnitDB.symbolCount());
	acceptableUnitDB.add(baseUnitDB);
	acceptableUnitDB.add(derivedUnitDB);
    }

    /**
     * Returns the base unit database.
     * @return			The base unit database.
     */
    public final UnitDB
    getBaseUnitDB()
    {
	return baseUnitDB;
    }

    /**
     * Returns the complete unit database.
     * @return			The complete unit database (both base units
     *				and derived units).
     */
    public final UnitDB
    getUnitDB()
    {
	return acceptableUnitDB;
    }

    /**
     * Returns the base unit corresponding to a base quantity.
     * @param quantity		The base quantity.
     * @return			The base unit corresponding to the base
     *				quantity in this system of units or <code>
     *				null</code> if no such unit exists.
     */
    public final BaseUnit
    getBaseUnit(BaseQuantity quantity)
    {
	return (BaseUnit)quantityMap.get(quantity);
    }
}
