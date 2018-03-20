/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for unit multiplication failures.
 * 
 * @author Steven R. Emmerson
 */
public final class MultiplyException extends OperationException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from a unit that can't be multiplied.
	 * 
	 * @param unit
	 *            The unit that can't be multiplied.
	 */
	public MultiplyException(final Unit unit) {
		super("Can't multiply unit \"" + unit + '"');
	}

	/**
	 * Constructs from two units.
	 * 
	 * @param A
	 *            A unit attempting to be multiplied.
	 * @param B
	 *            The other unit attempting to be multiplied.
	 */
	public MultiplyException(final Unit A, final Unit B) {
		super("Can't multiply unit \"" + A + "\" by unit \"" + B + '"');
	}

	/**
	 * Constructs from a scale factor and a unit.
	 * 
	 * @param scale
	 *            The scale factor.
	 * @param unit
	 *            The unit.
	 */
	public MultiplyException(final double scale, final Unit unit) {
		super("Can't multiply unit \"" + unit + "\" by " + scale);
	}
}
