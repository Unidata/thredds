/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for bad unit names.
 * 
 * @author Steven R. Emmerson
 */
public final class NameException extends UnitException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from a message.
	 * 
	 * @param msg
	 *            The message.
	 */
	public NameException(final String msg) {
		super(msg);
	}
}
