// $Id: UnitDB.java 64 2006-07-12 22:30:50Z edavis $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.units;

import java.util.Iterator;

/**
 * Interface for a unit database.
 * 
 * @author Steven R. Emmerson
 * @version $Id: UnitDB.java 64 2006-07-12 22:30:50Z edavis $
 */
public interface UnitDB {
	/**
	 * Adds a unit to the database.
	 * 
	 * @param unit
	 *            The unit to be added. Its getName() method must not return
	 *            null. Its getPlural() and getSymbol() methods may return null.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws UnitExistsException
	 *             The unit is already in the database.
	 * @throws UnitDBAccessException
	 *             Problem accessing unit database.
	 */
	public void addUnit(Unit unit) throws UnitExistsException,
			UnitDBAccessException, NameException;

	/**
	 * Adds an alias for a unit to the database.
	 * 
	 * @param alias
	 *            The alias for the unit.
	 * @param name
	 *            The name of the unit already in the database.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws UnitExistsException
	 *             The unit is already in the database.
	 * @throws UnitDBAccessException
	 *             Problem accessing unit database.
	 * @throws NoSuchUnitException
	 *             The unit doesn't exist in the database.
	 */
	public void addAlias(String alias, String name) throws NoSuchUnitException,
			UnitExistsException, UnitDBAccessException, NameException;

	/**
	 * Adds an alias for a unit to the database.
	 * 
	 * @param alias
	 *            The alias for the unit.
	 * @param name
	 *            The name of the unit already in the database.
	 * @param symbol
	 *            The symbol for the unit.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws UnitExistsException
	 *             The unit is already in the database.
	 * @throws UnitDBAccessException
	 *             Problem accessing unit database.
	 * @throws NoSuchUnitException
	 *             The unit doesn't exist in the database.
	 */
	public void addAlias(String alias, String name, String symbol)
			throws NoSuchUnitException, UnitExistsException,
			UnitDBAccessException, NameException;

	/**
	 * Adds an alias for a unit to the database.
	 * 
	 * @param alias
	 *            The alias for the unit.
	 * @param name
	 *            The name of the unit already in the database.
	 * @param symbol
	 *            The symbol of the alias. May be <code>null
     *				</code>.
	 * @param plural
	 *            The plural form of the alias. May be <code>
     *				null</code> in which
	 *            case regular plural- forming rules are followed.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws UnitExistsException
	 *             The unit is already in the database.
	 * @throws UnitDBAccessException
	 *             Problem accessing unit database.
	 * @throws NoSuchUnitException
	 *             The unit doesn't exist in the database.
	 */
	public void addAlias(String alias, String name, String symbol, String plural)
			throws NoSuchUnitException, UnitExistsException,
			UnitDBAccessException, NameException;

	/**
	 * Adds an alias for a unit to the database.
	 * 
	 * @param id
	 *            The alias identifier.
	 * @param name
	 *            The name of the unit already in the database.
	 * @throws NoSuchUnitException
	 *             The unit doesn't exist in the database.
	 * @throws UnitExistsException
	 *             The unit is already in the database.
	 * @throws UnitDBAccessException
	 *             Problem accessing unit database.
	 */
	public void addAlias(UnitID id, String name) throws NoSuchUnitException,
			UnitExistsException, UnitDBAccessException;

	/**
	 * Adds a symbol for a unit to the database.
	 * 
	 * @param symbol
	 *            The symbol for the unit.
	 * @param name
	 *            The name of the unit already in the database.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws UnitExistsException
	 *             The unit is already in the database.
	 * @throws UnitDBAccessException
	 *             Problem accessing unit database.
	 * @throws NoSuchUnitException
	 *             The unit doesn't exist in the database.
	 */
	public void addSymbol(String symbol, String name)
			throws NoSuchUnitException, UnitExistsException,
			UnitDBAccessException, NameException;

	/**
	 * Gets the unit in the database whose name, plural, or symbol, match an
	 * identifier. The order of matching is implementation defined.
	 * 
	 * @param id
	 *            The identifier for the unit.
	 * @return The matching unit in the database or <code>
     *				null</code> if no
	 *         matching unit could be found.
	 * @throws UnitDBAccessException
	 *             Problem accessing unit database.
	 */
	public Unit get(String id) throws UnitDBAccessException;

	/**
	 * Gets a unit in the database by name.
	 * 
	 * @param name
	 *            The name of the unit.
	 * @return The matching unit in the database or <code>
     *				null</code> if no
	 *         matching unit could be found.
	 * @throws UnitDBAccessException
	 *             Problem accessing unit database.
	 */
	public Unit getByName(String name) throws UnitDBAccessException;

	/**
	 * Gets a unit in the database by symbol.
	 * 
	 * @param symbol
	 *            The symbol for the unit.
	 * @return The matching unit in the database or <code>
     *				null</code> if no
	 *         matching unit could be found.
	 * @throws UnitDBAccessException
	 *             Problem accessing unit database.
	 */
	public Unit getBySymbol(String symbol) throws UnitDBAccessException;

	/**
	 * Returns the string representation of the database.
	 * 
	 * @return The string representation of the database.
	 */
	public String toString();

	/**
	 * Returns an iterator over the units of the database.
	 * 
	 * @return An iterator over the units of the database. The
	 *         <code>next()</code> of the iterator returns objects of type
	 *         <code>Unit</code>.
	 */
	public Iterator<?> getIterator();
}
