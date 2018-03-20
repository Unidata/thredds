/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for prefix database access failures. This is not used in the
 * default implementation but could be used for remote database implementations,
 * for example.
 * 
 * @author Steven R. Emmerson
 */
public class PrefixDBAccessException extends PrefixDBException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from an error message.
	 * 
	 * @param reason
	 *            The reason the database couldn't be accessed.
	 */
	public PrefixDBAccessException(final String reason) {
		super("Couldn't access unit-prefix database: " + reason);
	}
}
