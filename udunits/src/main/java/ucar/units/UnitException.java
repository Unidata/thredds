/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import java.io.Serializable;

/**
 * Provides support for general failures of this package.
 * 
 * @author Steven R. Emmerson
 */
public abstract class UnitException extends Exception implements Serializable {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from nothing.
	 */
	public UnitException() {
	}

	/**
	 * Constructs from an error message.
	 * 
	 * @param message
	 *            The error message.
	 */
	public UnitException(final String message) {
		super(message);
	}

	/**
	 * Constructs from a message and the exception that caused the failure.
	 * 
	 * @param message
	 *            The message.
	 * @param e
	 *            The exception that caused the failure.
	 */
	public UnitException(final String message, final Exception e) {
		super(message);
		initCause(e);
	}
}
