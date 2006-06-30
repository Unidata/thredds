// $Id: PrefixDBImpl.java,v 1.6 2004/08/23 18:54:36 dmurray Exp $
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
import java.util.Comparator;
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
 * @version $Id: PrefixDBImpl.java,v 1.6 2004/08/23 18:54:36 dmurray Exp $
 */
public class
PrefixDBImpl
    implements	PrefixDB, Serializable
{
    /**
     * The set of prefix names.
     * @serial
     */
    private final SortedSet	nameSet;

    /**
     * The set of prefix symbols.
     * @serial
     */
    private final SortedSet	symbolSet;

    /**
     * The mapping between prefix values and prefixes.
     * @serial
     */
    private final Map		valueMap;

    /**
     * Constructs from nothing.
     */
    public
    PrefixDBImpl()
    {
	nameSet = new TreeSet();
	symbolSet = new TreeSet();
	valueMap = new TreeMap();
    }

    /**
     * Adds a prefix to the database by name.
     * @param name		The name of the prefix to be added.
     * @param value		The value of the prefix.
     * @throws PrefixExistsException	Another prefix with the same name 
     *					or value already exists in the database.
     */
    public void
    addName(String name, double value)
	throws PrefixExistsException
    {
	Prefix	prefix = new PrefixName(name, value);
	nameSet.add(prefix);
    }

    /**
     * Adds a prefix symbol to the database.
     * @param symbol		The symbol of the prefix to be added.
     * @param value		The value of the prefix.
     * @throws PrefixExistsException	Another prefix with the same symbol 
     *					or value already exists in the database.
     */
    public void
    addSymbol(String symbol, double value)
	throws PrefixExistsException
    {
	Prefix	prefix = new PrefixSymbol(symbol, value);
	symbolSet.add(prefix);
	valueMap.put(new Double(value), prefix);
    }

    /**
     * Gets a prefix by name.
     * @param string		The name to be matched.
     * @return			The prefix whose name matches or null.
     */
    public Prefix
    getPrefixByName(String string)
    {
	return getPrefix(string, nameSet);
    }

    /**
     * Gets a prefix by symbol.
     * @param string		The symbol to be matched.
     * @return			The prefix whose symbol matches or null.
     */
    public Prefix
    getPrefixBySymbol(String string)
    {
	return getPrefix(string, symbolSet);
    }

    /**
     * Gets a prefix by value.
     * @param value		The value to be matched.
     * @return			The prefix whose value matches or null.
     */
    public Prefix
    getPrefixByValue(double value)
    {
	return (Prefix)valueMap.get(new Double(value));
    }

    /**
     * Returns the prefix from the given set with the given identifier.
     * @param string		The prefix identifier.
     * @param set		The set to search.
     */
    private static Prefix
    getPrefix(String string, Set set)
    {
	int	stringLen = string.length();
	for (Iterator iter = set.iterator(); iter.hasNext(); )
	{
	    Prefix	prefix = (Prefix)iter.next();
	    int		comp = prefix.compareTo(string);
	    if (comp == 0)
		return prefix;
	    if (comp > 0)
		break;
	}
	return null;
    }

    /**
     * Returns a string representation of this database.
     * @return			A string representation of this database.
     */
    public String
    toString()
    {
	return
	    "nameSet=" + nameSet + 
	    "symbolSet=" + symbolSet + 
	    "valueMap=" + valueMap;
    }

    /**
     * Gets an iterator over the prefixes in the database.
     * @return			An iterator over the entries in the database.
     *				The objects returned by the iterator will be of
     *				type <code>Prefix</code>.
     */
    public Iterator
    iterator()
    {
	return nameSet.iterator();
    }

    /** 
     * Tests this class.
     */
    public static void
    main(String[] args)
	throws Exception
    {
	PrefixDB	db = new PrefixDBImpl();
	db.addName("mega", 1e6);
	System.out.println("mega=" + db.getPrefixByName("mega").getValue());
	db.addSymbol("m", 1e-3);
	System.out.println("m=" + db.getPrefixBySymbol("m").getValue());
	System.out.println("1e-3=" + db.getPrefixByValue(1e-3).getID());
    }
}
