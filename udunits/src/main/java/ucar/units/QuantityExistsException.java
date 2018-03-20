/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for attempting to redefine a base quantity in a database.
 * 
 * @author Steven R. Emmerson
 */
public class QuantityExistsException extends UnitException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from the base quantity being redefined.
	 */
	public QuantityExistsException(final BaseQuantity quantity) {
		this("Base quantity \"" + quantity + " already exists");
	}

	/**
	 * Constructs from an error message.
	 * 
	 * @param msg
	 *            The error message.
	 */
	private QuantityExistsException(final String msg) {
		super(msg);
	}
}
