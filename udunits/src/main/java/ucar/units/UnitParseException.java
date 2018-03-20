/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for errors in unit specifications.
 * 
 * @author Steven R. Emmerson
 */
public class UnitParseException extends SpecificationException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from a reason.
	 * 
	 * @param reason
	 *            The reason for the failure.
	 */
	public UnitParseException(final String reason) {
		super("Parse error: " + reason);
	}

	/**
	 * Constructs from the string to be parsed and a reason.
	 * 
	 * @param spec
	 *            The string to be parsed.
	 * @param reason
	 *            The reason for the failure.
	 */
	public UnitParseException(final String spec, final String reason) {
		super("Couldn't parse \"" + spec + "\": " + reason);
	}

	public UnitParseException(final String message, final Throwable e) {
		super(message);
		initCause(e);
	}
}
