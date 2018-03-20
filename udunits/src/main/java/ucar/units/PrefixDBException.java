/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for the general class of prefix database failures.
 * 
 * @author Steven R. Emmerson
 */
public class PrefixDBException extends UnitException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from nothing.
	 */
	public PrefixDBException() {
		super("Prefix database exception");
	}

	/**
	 * Constructs from an error message.
	 * 
	 * @param message
	 *            The error message.
	 */
	public PrefixDBException(final String message) {
		super("Prefix database exception: " + message);
	}

	/**
	 * Constructs from the exception that caused this exception to be thrown.
	 * 
	 * @param e
	 *            The exception that caused this exception to be thrown.
	 */
	public PrefixDBException(final Exception e) {
		this("Prefix database exception", e);
	}

	/**
	 * Constructs from an error message and the exception that caused this
	 * exception to be thrown.
	 * 
	 * @param message
	 *            The error message.
	 * @param e
	 *            The exception that caused this exception to be thrown.
	 */
	public PrefixDBException(final String message, final Exception e) {
		super(message, e);
	}
}
