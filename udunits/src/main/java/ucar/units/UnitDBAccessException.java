/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for failure to access unit database (e.g. RMI failure).
 * 
 * @author Steven R. Emmerson
 */
public class UnitDBAccessException extends UnitDBException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from a string.
	 * 
	 * @param reason
	 *            The reason for the failure.
	 */
	public UnitDBAccessException(final String reason) {
		super("Couldn't access unit database: " + reason);
	}
}
