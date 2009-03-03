// $Id: StandardPrefixDB.java 64 2006-07-12 22:30:50Z edavis $
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

/**
 * Provides support for a database of standard unit prefixes.
 * 
 * @author Steven R. Emmerson
 * @version $Id: StandardPrefixDB.java 64 2006-07-12 22:30:50Z edavis $
 */
public final class StandardPrefixDB extends PrefixDBImpl {
	private static final long				serialVersionUID	= 1L;
	/**
	 * The singleton instance of this class.
	 * 
	 * @serial
	 */
	private static/* final */StandardPrefixDB	instance			= null;

	/**
	 * Constructs from nothing.
	 * 
	 * @throws PrefixExistsException
	 *             Attempt to redefine a prefix.
	 */
	private StandardPrefixDB() throws PrefixExistsException {
		/*
		 * SI prefixes:
		 */
		add("yotta", "Y", 1e24);
		add("zetta", "Z", 1e21);
		add("exa", "E", 1e18);
		add("peta", "P", 1e15);
		add("tera", "T", 1e12);
		add("giga", "G", 1e9); // 1st syllable pronounced "jig"
		// according to "ASTM Designation: E
		// 380 - 85: Standard for METRIC
		// PRACTICE".
		add("mega", "M", 1e6);
		add("kilo", "k", 1e3);
		add("hecto", "h", 1e2);
		add("deca", "da", 1e1); // Spelling according to "ISO 2955:
		// Information processing --
		// Representation of SI and other units
		// in systems with limited character
		// sets"
		addName("deka", 1e1); // Designation: E 380 - 85: Standard
		// for METRIC PRACTICE", "ANSI/IEEE Std
		// 260-1978 (Reaffirmed 1985): IEEE
		// Standard Letter Symbols for Units of
		// Measurement", and NIST Special
		// Publication 811, 1995 Edition:
		// "Guide for the Use of the
		// International System of Units (SI)".
		add("deci", "d", 1e-1);
		add("centi", "c", 1e-2);
		add("milli", "m", 1e-3);
		add("micro", "u", 1e-6);
		add("nano", "n", 1e-9);
		add("pico", "p", 1e-12);
		add("femto", "f", 1e-15);
		add("atto", "a", 1e-18);
		add("zepto", "z", 1e-21);
		add("yocto", "y", 1e-24);
	}

	/**
	 * Gets an instance of this database.
	 * 
	 * @return An instance of this database.
	 * @throws PrefixDBException
	 *             The instance couldn't be created.
	 */
	public static StandardPrefixDB instance() throws PrefixDBException {
		if (instance == null) {
			synchronized (StandardPrefixDB.class) {
				if (instance == null) {
					try {
						instance = new StandardPrefixDB();
					}
					catch (final Exception e) {
						throw new PrefixDBException(
								"Couldn't create standard prefix-database", e);
					}
				}
			}
		}
		return instance;
	}

	/**
	 * Adds a prefix to the database.
	 * 
	 * @param name
	 *            The name of the prefix.
	 * @param symbol
	 *            The symbol for the prefix.
	 * @param definition
	 *            The numeric value of the prefix.
	 * @throws PrefixExistsException
	 *             Attempt to redefine an existing prefix.
	 */
	private void add(final String name, final String symbol,
			final double definition) throws PrefixExistsException {
		addName(name, definition);
		addSymbol(symbol, definition);
	}

	/**
	 * Tests this class.
	 */
	public static void main(final String[] args) throws Exception {
		final PrefixDB db = StandardPrefixDB.instance();
		System.out.println("db.getPrefixBySymbol(\"cm\") = \""
				+ db.getPrefixBySymbol("cm") + '"');
		System.out.println("db.getPrefixBySymbol(\"dm\") = \""
				+ db.getPrefixBySymbol("dm") + '"');
	}
}
