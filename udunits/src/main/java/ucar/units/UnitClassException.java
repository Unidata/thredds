/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for a Unit that is an instance of an unknown class.
 * 
 * @author Steven R. Emmerson
 */
public final class UnitClassException extends UnitFormatException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from an error message.
	 * 
	 * @param msg
	 *            The error message.
	 */
	private UnitClassException(final String msg) {
		super(msg);
	}

	/**
	 * Constructs from the unit that's an instance of an unknown class.
	 * 
	 * @param unit
	 *            The unknown unit.
	 */
	public UnitClassException(final Unit unit) {
		this("\"" + unit.getClass().getName() + "\" is an unknown unit class");
	}
}
