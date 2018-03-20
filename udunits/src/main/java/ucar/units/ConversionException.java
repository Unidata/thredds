/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import java.io.Serializable;

/**
 * Provides support for unit conversion exceptions.
 * 
 * @author Steven R. Emmerson
 */
public final class ConversionException extends UnitException implements
		Serializable {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from nothing.
	 */
	public ConversionException() {
	}

	/**
	 * Constructs from a message.
	 * 
	 * @param message
	 *            The error message.
	 */
	private ConversionException(final String message) {
		super(message);
	}

	/**
	 * Constructs from a "from" unit and and "to" unit.
	 * 
	 * @param fromUnit
	 *            The unit from which a conversion was attempted.
	 * @param toUnit
	 *            The unit to which a conversion was attempted.
	 */
	public ConversionException(final Unit fromUnit, final Unit toUnit) {
		this("Can't convert from unit \"" + fromUnit + "\" to unit \"" + toUnit
				+ "\"");
	}
}
