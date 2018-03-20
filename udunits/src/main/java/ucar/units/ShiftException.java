/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import java.util.Date;

/**
 * Provides support for failure to raise a unit to a power.
 * 
 * @author Steven R. Emmerson
 */
public final class ShiftException extends OperationException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from the unit and the origin.
	 * 
	 * @param unit
	 *            The unit.
	 * @param origin
	 *            The desired origin.
	 */
	public ShiftException(final Unit unit, final double origin) {
		super("Can't shift origin of unit \"" + unit + "\" to " + origin);
	}

	/**
	 * Constructs from the unit and the origin.
	 * 
	 * @param unit
	 *            The unit.
	 * @param origin
	 *            The desired origin.
	 */
	public ShiftException(final Unit unit, final Date origin) {
		super("Can't shift origin of unit \"" + unit + "\" to " + origin);
	}
}
