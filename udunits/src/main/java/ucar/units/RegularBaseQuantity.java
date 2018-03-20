/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for a base quantity that is dimensionfull.
 * 
 * Instances of this class are immutable.
 * 
 * @author Steven R. Emmerson
 */
public final class RegularBaseQuantity extends BaseQuantity {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from a name and symbol.
	 * 
	 * @param name
	 *            The name of the base unit.
	 * @param symbol
	 *            The symbol of the base unit.
	 */
	public RegularBaseQuantity(final String name, final String symbol)
			throws NameException {
		super(name, symbol);
	}

	/**
	 * Constructs from a name and a symbol. This is a trusted constructor for
	 * use by the parent class only.
	 * 
	 * @param name
	 *            The name of the base unit.
	 * @param symbol
	 *            The symbol of the base unit.
	 */
	protected RegularBaseQuantity(final String name, final String symbol,
			final boolean trusted) {
		super(name, symbol, trusted);
	}

	/**
	 * Indicates if this base quantity is dimensionless. Regular base quantities
	 * are always dimensionfull.
	 * 
	 * @return <code>false</code>.
	 */
	@Override
	public boolean isDimensionless() {
		return false;
	}
}
