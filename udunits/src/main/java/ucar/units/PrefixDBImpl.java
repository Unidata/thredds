// $Id: PrefixDBImpl.java 64 2006-07-12 22:30:50Z edavis $
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
 * @version $Id: PrefixDBImpl.java 64 2006-07-12 22:30:50Z edavis $
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
		nameSet = new TreeSet<Prefix>();
		symbolSet = new TreeSet<Prefix>();
		valueMap = new TreeMap<Double, Prefix>();
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
		valueMap.put(new Double(value), prefix);
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
		return valueMap.get(new Double(value));
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
		for (final Iterator<Prefix> iter = set.iterator(); iter.hasNext();) {
			final Prefix prefix = iter.next();
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

	/**
	 * Tests this class.
	 */
	public static void main(final String[] args) throws Exception {
		final PrefixDB db = new PrefixDBImpl();
		db.addName("mega", 1e6);
		System.out.println("mega=" + db.getPrefixByName("mega").getValue());
		db.addSymbol("m", 1e-3);
		System.out.println("m=" + db.getPrefixBySymbol("m").getValue());
		System.out.println("1e-3=" + db.getPrefixByValue(1e-3).getID());
	}
}
