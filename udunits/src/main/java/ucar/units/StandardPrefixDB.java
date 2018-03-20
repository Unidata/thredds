/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for a database of standard unit prefixes.
 *
 * @author Steven R. Emmerson
 */
public final class StandardPrefixDB extends PrefixDBImpl {
  private static final long serialVersionUID = 1L;
  /**
   * The singleton instance of this class.
   *
   * @serial
   */
  private static/* final */ StandardPrefixDB instance = null;

  /**
   * Constructs from nothing.
   *
   * @throws PrefixExistsException Attempt to redefine a prefix.
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
   * @throws PrefixDBException The instance couldn't be created.
   */
  public static synchronized StandardPrefixDB instance() throws PrefixDBException {
    if (instance == null) {
      try {
        instance = new StandardPrefixDB();
      } catch (final Exception e) {
        throw new PrefixDBException(
                "Couldn't create standard prefix-database", e);
      }
    }
    return instance;
  }

  /**
   * Adds a prefix to the database.
   *
   * @param name       The name of the prefix.
   * @param symbol     The symbol for the prefix.
   * @param definition The numeric value of the prefix.
   * @throws PrefixExistsException Attempt to redefine an existing prefix.
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
