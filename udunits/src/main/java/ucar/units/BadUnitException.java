/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for failures due to attempts to redefine an existing unit in
 * a unit database.
 * 
 * @author Steven R. Emmerson
 */
public final class BadUnitException extends UnitDBException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from an error message.
	 * 
	 * @param msg
	 *            The error message.
	 */
	public BadUnitException(final String msg) {
		super(msg);
	}
}
