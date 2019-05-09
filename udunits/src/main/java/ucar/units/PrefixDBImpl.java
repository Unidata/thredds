/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Provides a concrete implementation of a database of unit prefixes.
 * 
 * Instances of this class are modifiable.
 * 
 * @author Steven R. Emmerson
 */
public class PrefixDBImpl implements PrefixDB, Serializable {
	private static final long			serialVersionUID	= 1L;

	/**
	 * The set of prefix names.
	 * 
	 * @serial
	 */
	private final SortedSet<Prefix>		nameSet;

	/**
	 * The set of prefix symbols.
	 * 
	 * @serial
	 */
	private final SortedSet<Prefix>		symbolSet;

	/**
	 * The mapping between prefix values and prefixes.
	 * 
	 * @serial
	 */
	private final Map<Double, Prefix>	valueMap;

	/**
	 * Constructs from nothing.
	 */
	public PrefixDBImpl() {
		nameSet = new TreeSet<>();
		symbolSet = new TreeSet<>();
		valueMap = new TreeMap<>();
	}

	/**
	 * Adds a prefix to the database by name.
	 * 
	 * @param name
	 *            The name of the prefix to be added.
	 * @param value
	 *            The value of the prefix.
	 * @throws PrefixExistsException
	 *             Another prefix with the same name or value already exists in
	 *             the database.
	 */
	public void addName(final String name, final double value)
			throws PrefixExistsException {
		final Prefix prefix = new PrefixName(name, value);
		nameSet.add(prefix);
	}

	/**
	 * Adds a prefix symbol to the database.
	 * 
	 * @param symbol
	 *            The symbol of the prefix to be added.
	 * @param value
	 *            The value of the prefix.
	 * @throws PrefixExistsException
	 *             Another prefix with the same symbol or value already exists
	 *             in the database.
	 */
	public void addSymbol(final String symbol, final double value)
			throws PrefixExistsException {
		final Prefix prefix = new PrefixSymbol(symbol, value);
		symbolSet.add(prefix);
		valueMap.put(value, prefix);
	}

	/**
	 * Gets a prefix by name.
	 * 
	 * @param string
	 *            The name to be matched.
	 * @return The prefix whose name matches or null.
	 */
	public Prefix getPrefixByName(final String string) {
		return getPrefix(string, nameSet);
	}

	/**
	 * Gets a prefix by symbol.
	 * 
	 * @param string
	 *            The symbol to be matched.
	 * @return The prefix whose symbol matches or null.
	 */
	public Prefix getPrefixBySymbol(final String string) {
		return getPrefix(string, symbolSet);
	}

	/**
	 * Gets a prefix by value.
	 * 
	 * @param value
	 *            The value to be matched.
	 * @return The prefix whose value matches or null.
	 */
	public Prefix getPrefixByValue(final double value) {
		return valueMap.get(value);
	}

	/**
	 * Returns the prefix from the given set with the given identifier.
	 * 
	 * @param string
	 *            The prefix identifier.
	 * @param set
	 *            The set to search.
	 */
	private static Prefix getPrefix(final String string, final Set<Prefix> set) {
		for (final Prefix prefix : set) {
			final int comp = prefix.compareTo(string);
			if (comp == 0) {
				return prefix;
			}
			if (comp > 0) {
				break;
			}
		}
		return null;
	}

	/**
	 * Returns a string representation of this database.
	 * 
	 * @return A string representation of this database.
	 */
	@Override
	public String toString() {
		return "nameSet=" + nameSet + "symbolSet=" + symbolSet + "valueMap="
				+ valueMap;
	}

	/**
	 * Gets an iterator over the prefixes in the database.
	 * 
	 * @return An iterator over the entries in the database. The objects
	 *         returned by the iterator will be of type <code>Prefix</code>.
	 */
	@SuppressWarnings("unchecked")
	public Iterator iterator() {
		return nameSet.iterator();
	}

}
