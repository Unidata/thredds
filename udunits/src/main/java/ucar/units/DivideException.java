/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for unit division failures.
 * 
 * @author Steven R. Emmerson
 */
public final class DivideException extends OperationException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from a unit that can't be divided.
	 * 
	 * @param unit
	 *            The unit that can't be divided.
	 */
	public DivideException(final Unit unit) {
		super("Can't divide unit \"" + unit + "\"");
	}

	/**
	 * Constructs from dividend and divisor units.
	 * 
	 * @param numerator
	 *            The unit attempting to be divided.
	 * @param denominator
	 *            The unit attempting to divide.
	 */
	public DivideException(final Unit numerator, final Unit denominator) {
		super("Can't divide unit \"" + numerator + "\" by unit \""
				+ denominator + "\"");
	}
}
