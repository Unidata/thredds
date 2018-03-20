/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for general unit database failures.
 * 
 * @author Steven R. Emmerson
 */
public class UnitDBException extends UnitException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from nothing.
	 */
	protected UnitDBException() {
	}

	/**
	 * Constructs from an error message.
	 * 
	 * @param message
	 *            The error message.
	 */
	public UnitDBException(final String message) {
		super(message);
	}

	/**
	 * Constructs from a message and the exception that caused the failure.
	 * 
	 * @param message
	 *            The message.
	 * @param e
	 *            The exeception that cause the the failure.
	 */
	public UnitDBException(final String message, final Exception e) {
		super(message, e);
	}
}
