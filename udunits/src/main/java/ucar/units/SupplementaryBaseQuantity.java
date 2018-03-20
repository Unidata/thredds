/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for supplementary base quantities. A supplementary base
 * quantity is one that is dimensionless (e.g. solid angle).
 * 
 * Instances of this class are immutable.
 * 
 * @author Steven R. Emmerson
 */
public final class SupplementaryBaseQuantity extends BaseQuantity {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from a name and symbol.
	 * 
	 * @param name
	 *            The name of the quantity.
	 * @param symbol
	 *            The symbol for the quantity.
	 * @throws NameException
	 *             Bad quantity name.
	 */
	public SupplementaryBaseQuantity(final String name, final String symbol)
			throws NameException {
		super(name, symbol);
	}

	/**
	 * Constructs from a name and symbol. This is a trusted constructor for use
	 * by the superclass only.
	 * 
	 * @param name
	 *            The name of the quantity.
	 * @param symbol
	 *            The symbol for the quantity.
	 */
	protected SupplementaryBaseQuantity(final String name, final String symbol,
			final boolean trusted) {
		super(name, symbol, trusted);
	}

	/**
	 * Indicates whether or not this quantity is dimensionless. Supplementary
	 * base quantities are dimensionless by definition.
	 * 
	 * @return <code>true</code>.
	 */
	@Override
	public boolean isDimensionless() {
		/*
		 * These quantities are dimensionless by definition.
		 */
		return true;
	}
}
