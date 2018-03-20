/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for general failures with unit format classes.
 * 
 * @author Steven R. Emmerson
 */
public class UnitFormatException extends UnitException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from nothing.
	 */
	public UnitFormatException() {
	}

	/**
	 * Constructs from an error message.
	 * 
	 * @param message
	 *            The error message.
	 */
	public UnitFormatException(final String message) {
		super(message);
	}
}
