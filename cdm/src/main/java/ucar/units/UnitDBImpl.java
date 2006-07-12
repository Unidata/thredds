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
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Provides most of a concrete implementation of a database of units.
 *
 * @author Steven R. Emmerson
 * @version $Id$
 */
public class
UnitDBImpl
    implements	UnitDB, Serializable
{
    /**
     * The set of units.
     * @serial
     */
    private final Set		unitSet;

    /**
     * The name-to-unit map.
     * @serial
     */
    private final Map		nameMap;

    /**
     * The symbol-to-unit map.
     * @serial
     */
    private final Map		symbolMap;

    /**
     * Constructs from the expected number of names and symbols.  The sizes
     * will be used to construct the initial database but will not limit its
     * growth.
     * @param nameCount		The expected number of names (including plurals
     *				and aliases).
     * @param symbolCount	The expected number of symbols.
     */
    protected
    UnitDBImpl(int nameCount, int symbolCount)
    {
	unitSet =
	    new TreeSet(
		new Comparator() 
		{
		    public int
		    compare(Object obj1, Object obj2)
		    {
			return ((Unit)obj1).getName().compareTo(
			    ((Unit)obj2).getName());
		    }
		}
	    );
	nameMap = new Hashtable(nameCount+1);
	symbolMap = new Hashtable(symbolCount+1);
    }

    /**
     * Adds all the entries in another UnitDBImpl to this database.
     * @param that		The other UnitDBImpl.
     * @throws UnitExistsException	Attempt to redefine an existing entry.
     */
    public void
    add(UnitDBImpl that)
	throws UnitExistsException
    {
	unitSet.addAll(that.unitSet);
	nameMap.putAll(that.nameMap);
	symbolMap.putAll(that.symbolMap);
    }

    /**
     * Return the number of names in this database
     * @return			The total number of names, plurals, and aliases.
     */
    public int
    nameCount()
    {
	return nameMap.size();
    }

    /**
     * Return the number of symbols in this database.
     * @return			The number of symbols in this database.
     */
    public int
    symbolCount()
    {
	return symbolMap.size();
    }

    /**
     * Adds a unit to the database.
     * @param unit		The unit to be added.
     * @throws UnitExistsException	Another unit with the same name or 
     *					symbol already exists in the database.
     * @throws NameException		Bad unit name.
     */
    public void
    addUnit(Unit unit)
	throws UnitExistsException, NameException
    {
	if (unit.getName() == null)
	    throw new NameException("Unit name can't be null");
	addByName(unit.getName(), unit);
	addByName(unit.getPlural(), unit);
	addBySymbol(unit.getSymbol(), unit);
	unitSet.add(unit);
    }

    /**
     * Adds an alias for a unit already in the database.
     * @param alias		An alias for the unit.
     * @param name		The name of the unit already in the database.
     * @throws UnitExistsException	Another unit with the same name or 
     *					symbol already exists in the database.
     * @throws NoSuchUnitException	The unit isn't in the database.
     */
    public final void
    addAlias(String alias, String name)
	throws
	    NoSuchUnitException,
	    UnitExistsException
    {
	addAlias(alias, name, null);
    }

    /**
     * Adds an alias for a unit already in the database.
     * @param alias		An alias for the unit.
     * @param name		The name of the unit already in the database.
     * @param symbol		The symbol for the unit.
     * @throws UnitExistsException	Another unit with the same name or 
     *					symbol already exists in the database.
     * @throws NoSuchUnitException	The unit isn't in the database.
     */
    public final void
    addAlias(String alias, String name, String symbol)
	throws
	    NoSuchUnitException,
	    UnitExistsException
    {
	addAlias(alias, name, symbol, null);
    }

    /**
     * Adds a symbol for a unit already in the database.
     * @param symbol		The symbol for the unit.
     * @param name		The name of the unit already in the database.
     * @throws UnitExistsException	Another unit with the same name or 
     *					symbol already exists in the database.
     * @throws NoSuchUnitException	The unit isn't in the database.
     */
    public final void
    addSymbol(String symbol, String name)
	throws
	    NoSuchUnitException,
	    UnitExistsException
    {
	addAlias(null, name, symbol, null);
    }

    /**
     * Adds an alias for a unit already in the database.
     * @param alias		The alias to be added to the database.  May be
     *				null.
     * @param name		The name of the unit to have an alias added to
     *				the database.
     * @param symbol		The symbol to be added. May be null.
     * @param plural		The plural form of the alias.  If <code>null
     *				</code>, then regular plural-forming rules
     *				are followed.
     * @throws NoSuchUnitException	The unit is not in the database.
     * @throws UnitExistsException	Another unit with the same alias is 
     *					already in the database.
     */
    public final void
    addAlias(String alias, String name, String symbol, String plural)
	throws NoSuchUnitException, UnitExistsException
    {
	addAlias(UnitID.newUnitID(alias, plural, symbol), name);
    }

    /**
     * Adds an alias for a unit already in the database.
     * @param alias		The alias to be added to the database.
     * @param name		The name of the unit to have an alias added to
     *				the database.
     * @throws NoSuchUnitException	The unit is not in the database.
     * @throws UnitExistsException	Another unit with the same alias is 
     *					already in the database.
     */
    public final void
    addAlias(UnitID alias, String name)
	throws NoSuchUnitException, UnitExistsException
    {
	Unit	unit = getByName(name);
	if (unit == null)
	    throw new NoSuchUnitException(name);
	addByName(alias.getName(), unit);
	addByName(alias.getPlural(), unit);
	addBySymbol(alias.getSymbol(), unit);
    }

    /**
     * Gets a unit by either name, plural, or symbol.  Retrieving the
     * unit by symbol is attempted before retrieving the unit by name
     * because symbol comparisons are case sensitive and, hence, should
     * be more robust.
     * @param	id		The id to be matched.
     * @return			The unit whose name, plural, or symbol
     *				matches or <code>null</code> if no such unit
     *				was found.
     */
    public Unit
    get(String id)
    {
	Unit	unit = getBySymbol(id);
	if (unit == null)
	    unit = getByName(id);
	return unit;
    }

    /**
     * Gets a unit by name.
     * @param name		The name to be matched.
     * @return			The unit whose name, plural, or alias matches
     *				or <code>null</code> if no such unit was found.
     */
    public Unit
    getByName(String name)
    {
	return (Unit)nameMap.get(canonicalize(name));
    }

    /**
     * Returns the canonical form of a unit name.
     * @param name		A unit name.
     * @return			The canonical form of the name.
     */
    private static String
    canonicalize(String name)
    {
	return name.toLowerCase().replace(' ', '_');
    }

    /**
     * Gets a unit by symbol.
     * @param symbol		The symbol to be matched.
     * @return			The unit whose symbol matches or <code>null
     *				</code> if no such unit was found.
     */
    public Unit
    getBySymbol(String symbol)
    {
	return (Unit)symbolMap.get(symbol);
    }

    /**
     * Returns the string representation of this database.
     * @return			The string representation of this database.
     */
    public String
    toString()
    {
	return unitSet.toString();
    }

    /**
     * Gets an iterator over the units in the database.
     * @return			An iterator over the units in the database.
     *				The iterator's <code>next()</code> method 
     *				returns objects of type <code>Unit</code>.
     */
    public final Iterator
    getIterator()
    {
	return unitSet.iterator();
    }

    /**
     * Adds a unit to the database by name.
     * @param name		The name of the unit.  If <code>null</code>
     *				then the unit is not added.
     * @param newUnit		The unit to be added.
     * @throws UnitExistsException	Attempt to redefine an existing unit.
     */
    private final void
    addByName(String name, Unit newUnit)
	throws UnitExistsException
    {
	if (name != null)
	    addUnique(nameMap, canonicalize(name), newUnit);
    }

    /**
     * Adds a unit to the database by symbol.
     * @param symbol		The symbol for the unit.  If <code>null</code>
     *				then the unit is not added.
     * @param newUnit		The unit to be added.
     * @throws UnitExistsException	Attempt to redefine an existing unit.
     */
    private final void
    addBySymbol(String symbol, Unit newUnit)
	throws UnitExistsException
    {
	if (symbol != null)
	    addUnique(symbolMap, symbol, newUnit);
    }

    /**
     * Adds a unique unit to a map..
     * @param map		The map to be added to.
     * @param key		The key for the unit entry.
     * @param newUnit		The unit to be added.
     * @throws UnitExistsException	Attempt to redefine an existing unit.
     */
    private static final void
    addUnique(Map map, String key, Unit newUnit)
	throws UnitExistsException
    {
	Unit	oldUnit = (Unit)map.put(key, newUnit);
	if (oldUnit != null && !oldUnit.equals(newUnit))
	    throw new UnitExistsException(oldUnit, newUnit);
    }
}
