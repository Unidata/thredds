// $Id: UnitDBImpl.java 64 2006-07-12 22:30:50Z edavis $
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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Provides most of a concrete implementation of a database of units.
 * 
 * @author Steven R. Emmerson
 * @version $Id: UnitDBImpl.java 64 2006-07-12 22:30:50Z edavis $
 */
public class UnitDBImpl implements UnitDB, Serializable {
	private static final long		serialVersionUID	= 1L;

	/**
	 * The set of units.
	 * 
	 * @serial
	 */
	private final Set<Unit>			unitSet;

	/**
	 * The name-to-unit map.
	 * 
	 * @serial
	 */
	private final Map<String, Unit>	nameMap;

	/**
	 * The symbol-to-unit map.
	 * 
	 * @serial
	 */
	private final Map<String, Unit>	symbolMap;

	/**
	 * Constructs from the expected number of names and symbols. The sizes will
	 * be used to construct the initial database but will not limit its growth.
	 * 
	 * @param nameCount
	 *            The expected number of names (including plurals and aliases).
	 * @param symbolCount
	 *            The expected number of symbols.
	 */
	protected UnitDBImpl(final int nameCount, final int symbolCount) {
		unitSet = new TreeSet<Unit>(new Comparator<Unit>() {
			public int compare(final Unit obj1, final Unit obj2) {
				return (obj1).getName().compareTo((obj2).getName());
			}
		});
		nameMap = new Hashtable<String, Unit>(nameCount + 1);
		symbolMap = new Hashtable<String, Unit>(symbolCount + 1);
	}

	/**
	 * Adds all the entries in another UnitDBImpl to this database.
	 * 
	 * @param that
	 *            The other UnitDBImpl.
	 * @throws UnitExistsException
	 *             Attempt to redefine an existing entry.
	 */
	public void add(final UnitDBImpl that) throws UnitExistsException {
		unitSet.addAll(that.unitSet);
		nameMap.putAll(that.nameMap);
		symbolMap.putAll(that.symbolMap);
	}

	/**
	 * Return the number of names in this database
	 * 
	 * @return The total number of names, plurals, and aliases.
	 */
	public int nameCount() {
		return nameMap.size();
	}

	/**
	 * Return the number of symbols in this database.
	 * 
	 * @return The number of symbols in this database.
	 */
	public int symbolCount() {
		return symbolMap.size();
	}

	/**
	 * Adds a unit to the database.
	 * 
	 * @param unit
	 *            The unit to be added.
	 * @throws UnitExistsException
	 *             Another unit with the same name or symbol already exists in
	 *             the database.
	 * @throws NameException
	 *             Bad unit name.
	 */
	public void addUnit(final Unit unit) throws UnitExistsException,
			NameException {
		if (unit.getName() == null) {
			throw new NameException("Unit name can't be null");
		}
		addByName(unit.getName(), unit);
		addByName(unit.getPlural(), unit);
		addBySymbol(unit.getSymbol(), unit);
		unitSet.add(unit);
	}

	/**
	 * Adds an alias for a unit already in the database.
	 * 
	 * @param alias
	 *            An alias for the unit.
	 * @param name
	 *            The name of the unit already in the database.
	 * @throws UnitExistsException
	 *             Another unit with the same name or symbol already exists in
	 *             the database.
	 * @throws NoSuchUnitException
	 *             The unit isn't in the database.
	 */
	public final void addAlias(final String alias, final String name)
			throws NoSuchUnitException, UnitExistsException {
		addAlias(alias, name, null);
	}

	/**
	 * Adds an alias for a unit already in the database.
	 * 
	 * @param alias
	 *            An alias for the unit.
	 * @param name
	 *            The name of the unit already in the database.
	 * @param symbol
	 *            The symbol for the unit.
	 * @throws UnitExistsException
	 *             Another unit with the same name or symbol already exists in
	 *             the database.
	 * @throws NoSuchUnitException
	 *             The unit isn't in the database.
	 */
	public final void addAlias(final String alias, final String name,
			final String symbol) throws NoSuchUnitException,
			UnitExistsException {
		addAlias(alias, name, symbol, null);
	}

	/**
	 * Adds a symbol for a unit already in the database.
	 * 
	 * @param symbol
	 *            The symbol for the unit.
	 * @param name
	 *            The name of the unit already in the database.
	 * @throws UnitExistsException
	 *             Another unit with the same name or symbol already exists in
	 *             the database.
	 * @throws NoSuchUnitException
	 *             The unit isn't in the database.
	 */
	public final void addSymbol(final String symbol, final String name)
			throws NoSuchUnitException, UnitExistsException {
		addAlias(null, name, symbol, null);
	}

	/**
	 * Adds an alias for a unit already in the database.
	 * 
	 * @param alias
	 *            The alias to be added to the database. May be null.
	 * @param name
	 *            The name of the unit to have an alias added to the database.
	 * @param symbol
	 *            The symbol to be added. May be null.
	 * @param plural
	 *            The plural form of the alias. If <code>null
     *				</code>, then regular
	 *            plural-forming rules are followed.
	 * @throws NoSuchUnitException
	 *             The unit is not in the database.
	 * @throws UnitExistsException
	 *             Another unit with the same alias is already in the database.
	 */
	public final void addAlias(final String alias, final String name,
			final String symbol, final String plural)
			throws NoSuchUnitException, UnitExistsException {
		addAlias(UnitID.newUnitID(alias, plural, symbol), name);
	}

	/**
	 * Adds an alias for a unit already in the database.
	 * 
	 * @param alias
	 *            The alias to be added to the database.
	 * @param name
	 *            The name of the unit to have an alias added to the database.
	 * @throws NoSuchUnitException
	 *             The unit is not in the database.
	 * @throws UnitExistsException
	 *             Another unit with the same alias is already in the database.
	 */
	public final void addAlias(final UnitID alias, final String name)
			throws NoSuchUnitException, UnitExistsException {
		final Unit unit = getByName(name);
		if (unit == null) {
			throw new NoSuchUnitException(name);
		}
		addByName(alias.getName(), unit);
		addByName(alias.getPlural(), unit);
		addBySymbol(alias.getSymbol(), unit);
	}

	/**
	 * Gets a unit by either name, plural, or symbol. Retrieving the unit by
	 * symbol is attempted before retrieving the unit by name because symbol
	 * comparisons are case sensitive and, hence, should be more robust.
	 * 
	 * @param id
	 *            The id to be matched.
	 * @return The unit whose name, plural, or symbol matches or
	 *         <code>null</code> if no such unit was found.
	 */
	public Unit get(final String id) {
		Unit unit = getBySymbol(id);
		if (unit == null) {
			unit = getByName(id);
		}
		return unit;
	}

	/**
	 * Gets a unit by name.
	 * 
	 * @param name
	 *            The name to be matched.
	 * @return The unit whose name, plural, or alias matches or
	 *         <code>null</code> if no such unit was found.
	 */
	public Unit getByName(final String name) {
		return nameMap.get(canonicalize(name));
	}

	/**
	 * Returns the canonical form of a unit name.
	 * 
	 * @param name
	 *            A unit name.
	 * @return The canonical form of the name.
	 */
	private static String canonicalize(final String name) {
		return name.toLowerCase().replace(' ', '_');
	}

	/**
	 * Gets a unit by symbol.
	 * 
	 * @param symbol
	 *            The symbol to be matched.
	 * @return The unit whose symbol matches or <code>null
     *				</code> if no such unit was
	 *         found.
	 */
	public Unit getBySymbol(final String symbol) {
		return symbolMap.get(symbol);
	}

	/**
	 * Returns the string representation of this database.
	 * 
	 * @return The string representation of this database.
	 */
	@Override
	public String toString() {
		return unitSet.toString();
	}

	/**
	 * Gets an iterator over the units in the database.
	 * 
	 * @return An iterator over the units in the database. The iterator's
	 *         <code>next()</code> method returns objects of type
	 *         <code>Unit</code>.
	 */
	@SuppressWarnings("unchecked")
	public final Iterator getIterator() {
		return unitSet.iterator();
	}

	/**
	 * Adds a unit to the database by name.
	 * 
	 * @param name
	 *            The name of the unit. If <code>null</code> then the unit is
	 *            not added.
	 * @param newUnit
	 *            The unit to be added.
	 * @throws UnitExistsException
	 *             Attempt to redefine an existing unit.
	 */
	private final void addByName(final String name, final Unit newUnit)
			throws UnitExistsException {
		if (name != null) {
			addUnique(nameMap, canonicalize(name), newUnit);
		}
	}

	/**
	 * Adds a unit to the database by symbol.
	 * 
	 * @param symbol
	 *            The symbol for the unit. If <code>null</code> then the unit is
	 *            not added.
	 * @param newUnit
	 *            The unit to be added.
	 * @throws UnitExistsException
	 *             Attempt to redefine an existing unit.
	 */
	private final void addBySymbol(final String symbol, final Unit newUnit)
			throws UnitExistsException {
		if (symbol != null) {
			addUnique(symbolMap, symbol, newUnit);
		}
	}

	/**
	 * Adds a unique unit to a map..
	 * 
	 * @param map
	 *            The map to be added to.
	 * @param key
	 *            The key for the unit entry.
	 * @param newUnit
	 *            The unit to be added.
	 * @throws UnitExistsException
	 *             Attempt to redefine an existing unit.
	 */
	private static final void addUnique(final Map<String, Unit> map,
			final String key, final Unit newUnit) throws UnitExistsException {
		final Unit oldUnit = map.put(key, newUnit);
		if (oldUnit != null && !oldUnit.equals(newUnit)) {
			throw new UnitExistsException(oldUnit, newUnit);
		}
	}
}
