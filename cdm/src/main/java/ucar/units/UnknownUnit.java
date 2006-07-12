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

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Provides support for unknown base units.  This can be used, for example,
 * to accomodate an unknown unit (e.g. "foo").  Values in such a unit will
 * only be convertible with units derived from "foo" (e.g. "20 foo").
 *
 * @author Steven R. Emmerson
 * @version $Id$
 */
public final class
UnknownUnit
    extends	BaseUnit
{
    /**
     * The name-to-unit map.
     * @serial
     */
    private static final SortedMap	map = new TreeMap();

    /**
     * Constructs from a name.
     * @param name		The name of the unit.
     */
    private
    UnknownUnit(String name)
	throws NameException
    {
	super(UnitName.newUnitName(name, null, name), BaseQuantity.UNKNOWN);
    }

    /**
     * Factory method for constructing an unknown unit from a name.
     * @param name		The name of the unit.
     * @return			The unknown unit.
     * @throws NameException	<code>name == null</code>.
     */
    public static UnknownUnit
    create(String name)
	throws NameException
    {
	UnknownUnit	unit;
	name = name.toLowerCase();
	synchronized(map)
	{
	    unit = (UnknownUnit)map.get(name);
	    if (unit == null)
	    {
		unit = new UnknownUnit(name);
		map.put(unit.getName(), unit);
		map.put(unit.getPlural(), unit);
	    }
	}
	return unit;
    }

    /*
     * From Unit:
     */

    /**
     * Indicates if this unit is semantically identical to an object.
     * @param object		The object.
     * @return			<code>true</code> if and ony if the object
     *				<em>is</em> this unit.
     */
    public boolean
    equals(Object object)
    {
	return
	    object instanceof UnknownUnit
		? getName().equalsIgnoreCase(((UnknownUnit)object).getName())
		: object.equals(this);
    }

    /**
     * Indicates if this unit is dimensionless.  An unknown unit is never
     * dimensionless.
     * @return			<code>false</code> always.
     */
    public boolean
    isDimensionless()
    {
	return false;
    }

    /**
     * Tests this class.
     */
    public static void
    main(String[] args)
	throws Exception
    {
    	UnknownUnit	unit1 = UnknownUnit.create("a");
	System.out.println("unit_a.equals(unit_a)=" + unit1.equals(unit1));
	System.out.println("unit_a.isDimensionless()=" + 
	    unit1.isDimensionless());
    	UnknownUnit	unit2 = UnknownUnit.create("b");
	System.out.println("unit_a.equals(unit_b)=" + unit1.equals(unit2));
    	unit2 = UnknownUnit.create("A");
	System.out.println("unit_a.equals(unit_A)=" + unit1.equals(unit2));
    }
}
