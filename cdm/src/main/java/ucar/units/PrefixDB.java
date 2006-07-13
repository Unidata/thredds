// $Id: PrefixDB.java 64 2006-07-12 22:30:50Z edavis $
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

import java.util.Iterator;

/**
 * Interface for a database of unit prefixes.
 * @author Steven R. Emmerson
 * @version $Id: PrefixDB.java 64 2006-07-12 22:30:50Z edavis $
 */
public interface
PrefixDB
{
    /**
     * Adds a prefix to the database by name.
     * @param name		The name of the prefix.
     * @param value		The value of the prefix.
     * @throws PrefixExistsException	A prefix with the same name already
     *					exists in the database.
     * @throws PrefixDBAccessException	Prefix database access failure.
     */
    public void
    addName(String name, double value)
	throws PrefixExistsException, PrefixDBAccessException;

    /**
     * Adds a prefix to the database by symbol.
     * @param symbol		The symbol for the prefix.
     * @param value		The value of the prefix.
     * @throws PrefixExistsException	A prefix with the same symbol already
     *					exists in the database.
     * @throws PrefixDBAccessException	Prefix database access failure.
     */
    public void
    addSymbol(String symbol, double value)
	throws PrefixExistsException, PrefixDBAccessException;

    /**
     * Gets a prefix from the database by name.
     * @param name		The name of the prefix.
     * @return prefix		The prefix or null.
     * @throws PrefixDBAccessException	Prefix database access failure.
     */
    public Prefix
    getPrefixByName(String name)
	throws PrefixDBAccessException;

    /**
     * Gets a prefix from the database by symbol.
     * @param symbol		The symbol for the prefix.
     * @return prefix		The prefix or null.
     * @throws PrefixDBAccessException	Prefix database access failure.
     */
    public Prefix
    getPrefixBySymbol(String symbol)
	throws PrefixDBAccessException;

    /**
     * Gets a prefix from the database by value.
     * @param value		The value for the prefix.
     * @return prefix		The prefix or null.
     * @throws PrefixDBAccessException	Prefix database access failure.
     */
    public Prefix
    getPrefixByValue(double value)
	throws PrefixDBAccessException;

    /**
     * Gets a string representation of this database.
     * @return			A string representation of this database.
     */
    public String
    toString();

    /**
     * Gets an iterator over the entries in the database.
     * @return			An iterator over the database.
     */
    public Iterator
    iterator();
}
