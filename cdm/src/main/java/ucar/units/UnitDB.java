// $Id: UnitDB.java,v 1.5 2000/08/18 04:17:34 russ Exp $
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
import java.util.Iterator;

/**
 * Interface for a unit database.
 * 
 * @author Steven R. Emmerson
 * @version $Id: UnitDB.java,v 1.5 2000/08/18 04:17:34 russ Exp $
 */
public interface
UnitDB
{
    /**
     * Adds a unit to the database.
     * @param unit		The unit to be added.  Its getName() method
     *				must not return null.  Its getPlural() and
     *				getSymbol() methods may return null.
     * @throws NameException		Bad unit name.
     * @throws UnitExistsException	The unit is already in the database.
     * @throws UnitDBAccessException	Problem accessing unit database.
     */
    public void
    addUnit(Unit unit)
	throws
	    UnitExistsException,
	    UnitDBAccessException,
	    NameException;

    /**
     * Adds an alias for a unit to the database.
     * @param alias		The alias for the unit.
     * @param name		The name of the unit already in the database.
     * @throws NameException		Bad unit name.
     * @throws UnitExistsException	The unit is already in the database.
     * @throws UnitDBAccessException	Problem accessing unit database.
     * @throws NoSuchUnitException	The unit doesn't exist in the database.
     */
    public void
    addAlias(String alias, String name)
	throws
	    NoSuchUnitException,
	    UnitExistsException,
	    UnitDBAccessException,
	    NameException;

    /**
     * Adds an alias for a unit to the database.
     * @param alias		The alias for the unit.
     * @param name		The name of the unit already in the database.
     * @param symbol		The symbol for the unit.
     * @throws NameException		Bad unit name.
     * @throws UnitExistsException	The unit is already in the database.
     * @throws UnitDBAccessException	Problem accessing unit database.
     * @throws NoSuchUnitException	The unit doesn't exist in the database.
     */
    public void
    addAlias(String alias, String name, String symbol)
	throws
	    NoSuchUnitException,
	    UnitExistsException,
	    UnitDBAccessException,
	    NameException;

    /**
     * Adds an alias for a unit to the database.
     * @param alias		The alias for the unit.
     * @param name		The name of the unit already in the database.
     * @param symbol		The symbol of the alias.  May be <code>null
     *				</code>.
     * @param plural		The plural form of the alias.  May be <code>
     *				null</code> in which case regular plural-
     *				forming rules are followed.
     * @throws NameException		Bad unit name.
     * @throws UnitExistsException	The unit is already in the database.
     * @throws UnitDBAccessException	Problem accessing unit database.
     * @throws NoSuchUnitException	The unit doesn't exist in the database.
     */
    public void
    addAlias(String alias, String name, String symbol, String plural)
	throws
	    NoSuchUnitException,
	    UnitExistsException,
	    UnitDBAccessException,
	    NameException;

    /**
     * Adds an alias for a unit to the database.
     * @param id		The alias identifier.
     * @param name		The name of the unit already in the database.
     * @throws NoSuchUnitException	The unit doesn't exist in the database.
     * @throws UnitExistsException	The unit is already in the database.
     * @throws UnitDBAccessException	Problem accessing unit database.
     */
    public void
    addAlias(UnitID id, String name)
	throws
	    NoSuchUnitException,
	    UnitExistsException,
	    UnitDBAccessException;

    /**
     * Adds a symbol for a unit to the database.
     * @param symbol		The symbol for the unit.
     * @param name		The name of the unit already in the database.
     * @throws NameException		Bad unit name.
     * @throws UnitExistsException	The unit is already in the database.
     * @throws UnitDBAccessException	Problem accessing unit database.
     * @throws NoSuchUnitException	The unit doesn't exist in the database.
     */
    public void
    addSymbol(String symbol, String name)
	throws
	    NoSuchUnitException,
	    UnitExistsException,
	    UnitDBAccessException,
	    NameException;

    /**
     * Gets the unit in the database whose name, plural, or symbol, match
     * an identifier.  The order of matching is implementation defined.
     * @param id		The identifier for the unit.
     * @return			The matching unit in the database or <code>
     *				null</code> if no matching unit could be
     *				found.
     * @throws UnitDBAccessException	Problem accessing unit database.
     */
    public Unit
    get(String id)
	throws UnitDBAccessException;

    /**
     * Gets a unit in the database by name.
     * @param name		The name of the unit.
     * @return			The matching unit in the database or <code>
     *				null</code> if no matching unit could be
     *				found.
     * @throws UnitDBAccessException	Problem accessing unit database.
     */
    public Unit
    getByName(String name)
	throws UnitDBAccessException;

    /**
     * Gets a unit in the database by symbol.
     * @param symbol		The symbol for the unit.
     * @return			The matching unit in the database or <code>
     *				null</code> if no matching unit could be
     *				found.
     * @throws UnitDBAccessException	Problem accessing unit database.
     */
    public Unit
    getBySymbol(String symbol)
	throws UnitDBAccessException;

    /**
     * Returns the string representation of the database.
     * @return			The string representation of the database.
     */
    public String
    toString();

    /**
     * Returns an iterator over the units of the database.
     * @return			An iterator over the units of the database.
     *				The <code>next()</code> of the iterator returns
     *				objects of type <code>Unit</code>.
     */
    public Iterator
    getIterator();
}
