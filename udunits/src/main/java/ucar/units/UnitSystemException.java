/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for errors with the system of units.
 * 
 * @author Steven R. Emmerson
 */
public class UnitSystemException extends UnitException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from an error message.
	 * 
	 * @param message
	 *            The error messsage.
	 */
	public UnitSystemException(final String message) {
		super(message);
	}

	/**
	 * Constructs from an error message and the exception that caused the error.
	 * 
	 * @param message
	 *            The error messsage.
	 * @param e
	 *            The exception that caused the problem.
	 */
	public UnitSystemException(final String message, final Exception e) {
		super(message, e);
	}
}
