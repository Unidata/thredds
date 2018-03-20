/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for unit operation failures (ex: multiplication).
 * 
 * @author Steven R. Emmerson
 */
public abstract class OperationException extends UnitException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from nothing.
	 */
	public OperationException() {
	}

	/**
	 * Constructs from an error message.
	 * 
	 * @param message
	 *            The error message.
	 */
	protected OperationException(final String message) {
		super(message);
	}

	/**
	 * Constructs from an error message and the exception that caused the
	 * problem.
	 * 
	 * @param message
	 *            The error message.
	 * @param e
	 *            The exception that caused the problem.
	 */
	protected OperationException(final String message, final Exception e) {
		super(message, e);
	}
}
