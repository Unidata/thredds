/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import java.io.Serializable;

/**
 * Provides support for unit identifiers.
 * 
 * @author Steven R. Emmerson
 */
public abstract class UnitID implements Serializable {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Factory method for constructing an identifier from a name, plural, and
	 * symbol.
	 * 
	 * @param name
	 *            The name for the unit. May be <code>null
     *				</code>.
	 * @param plural
	 *            The plural form of the name. If <code>null
     *				</code> and
	 *            <code>name</code> is non-<code>
     *				null</code>, then regular
	 *            plural-forming rules are used on the name.
	 * @param symbol
	 *            The symbol for the unit. May be <code>null
     *				</code>.
	 */
	public static UnitID newUnitID(final String name, final String plural,
			final String symbol) {
		UnitID id;
		try {
			id = name == null
					? new UnitSymbol(symbol)
					: UnitName.newUnitName(name, plural, symbol);
		}
		catch (final NameException e) {
			id = null; // can't happen
		}
		return id;
	}

	/**
	 * Returns the name of the unit.
	 * 
	 * @return The name of the unit. May be <code>null</code>.
	 */
	public abstract String getName();

	/**
	 * Returns the plural form of the name of the unit.
	 * 
	 * @return The plural form of the name of the unit. May be <code>null</code>
	 *         .
	 */
	public abstract String getPlural();

	/**
	 * Returns the symbol for the unit.
	 * 
	 * @return The symbol for the unit. May be <code>null</code>.
	 */
	public abstract String getSymbol();

	/**
	 * Returns the string representation of this identifier.
	 * 
	 * @return The string representation of this identifier.
	 */
	@Override
	public abstract String toString();
}
