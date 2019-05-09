/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import java.util.Iterator;

/**
 * Interface for a database of unit prefixes.
 * 
 * @author Steven R. Emmerson
 */
public interface PrefixDB {
	/**
	 * Adds a prefix to the database by name.
	 * 
	 * @param name
	 *            The name of the prefix.
	 * @param value
	 *            The value of the prefix.
	 * @throws PrefixExistsException
	 *             A prefix with the same name already exists in the database.
	 * @throws PrefixDBAccessException
	 *             Prefix database access failure.
	 */
  void addName(String name, double value)
			throws PrefixExistsException, PrefixDBAccessException;

	/**
	 * Adds a prefix to the database by symbol.
	 * 
	 * @param symbol
	 *            The symbol for the prefix.
	 * @param value
	 *            The value of the prefix.
	 * @throws PrefixExistsException
	 *             A prefix with the same symbol already exists in the database.
	 * @throws PrefixDBAccessException
	 *             Prefix database access failure.
	 */
  void addSymbol(String symbol, double value)
			throws PrefixExistsException, PrefixDBAccessException;

	/**
	 * Gets a prefix from the database by name.
	 * 
	 * @param name
	 *            The name of the prefix.
	 * @return prefix The prefix or null.
	 * @throws PrefixDBAccessException
	 *             Prefix database access failure.
	 */
  Prefix getPrefixByName(String name) throws PrefixDBAccessException;

	/**
	 * Gets a prefix from the database by symbol.
	 * 
	 * @param symbol
	 *            The symbol for the prefix.
	 * @return prefix The prefix or null.
	 * @throws PrefixDBAccessException
	 *             Prefix database access failure.
	 */
  Prefix getPrefixBySymbol(String symbol)
			throws PrefixDBAccessException;

	/**
	 * Gets a prefix from the database by value.
	 * 
	 * @param value
	 *            The value for the prefix.
	 * @return prefix The prefix or null.
	 * @throws PrefixDBAccessException
	 *             Prefix database access failure.
	 */
  Prefix getPrefixByValue(double value) throws PrefixDBAccessException;

	/**
	 * Gets a string representation of this database.
	 * 
	 * @return A string representation of this database.
	 */
  String toString();

	/**
	 * Gets an iterator over the entries in the database.
	 * 
	 * @return An iterator over the database.
	 */
  Iterator<?> iterator();
}
