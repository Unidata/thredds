/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for failure to raise a unit to a power.
 * 
 * @author Steven R. Emmerson
 */
public final class RaiseException extends OperationException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from the unit that couldn't be raised to a power.
	 * 
	 * @param unit
	 *            The unit that couldn't be raised to a power.
	 */
	public RaiseException(final Unit unit) {
		super("Can't exponentiate unit \"" + unit + "\"");
	}
}
