/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for errors in unit string specifications.
 * 
 * @author Steven R. Emmerson
 */
public class SpecificationException extends UnitException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from an error message.
	 * 
	 * @param reason
	 *            The error message.
	 */
	public SpecificationException(final String reason) {
		super("Specification error: " + reason);
	}

	/**
	 * Constructs from the string to be parsed and an error message.
	 * 
	 * @param spec
	 *            The string to be parsed.
	 * @param reason
	 *            The error message.
	 */
	public SpecificationException(final String spec, final String reason) {
		super("Specification error in \"" + spec + "\": " + reason);
	}

	public SpecificationException(final String message, final Throwable e) {
		super(message);
		initCause(e);
	}
}
