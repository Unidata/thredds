/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for symbols for units.
 * 
 * @author Steven R. Emmerson
 */
public final class UnitSymbol extends UnitID {
	private static final long	serialVersionUID	= 1L;
	/**
	 * The symbol for the unit.
	 * 
	 * @serial
	 */
	private final String		symbol;

	/**
	 * Constructs from a symbol.
	 * 
	 * @param symbol
	 *            The symbol for the unit. Shall not be <code>
     *				null</code>.
	 */
	public UnitSymbol(final String symbol) throws NameException {
		if (symbol == null) {
			throw new NameException("Symbol can't be null");
		}
		this.symbol = symbol;
	}

	/**
	 * Returns the name of the unit. Always returns <code>null</code>.
	 * 
	 * @return <code>null</code>.
	 */
	@Override
	public String getName() {
		return null;
	}

	/**
	 * Returns the plural form of the name of the unit. Always returns
	 * <code>null</code>.
	 * 
	 * @return <code>null</code>.
	 */
	@Override
	public String getPlural() {
		return null;
	}

	/**
	 * Returns the symbol for the unit.
	 * 
	 * @return The symbol for the unit. Never <code>null
     *				</code>.
	 */
	@Override
	public String getSymbol() {
		return symbol;
	}

	/**
	 * Returns the string representation of this identifier.
	 * 
	 * @return The string representation of this identifier.
	 */
	@Override
	public String toString() {
		return getSymbol();
	}
}
