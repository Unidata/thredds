/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import java.io.Serializable;

/**
 * Provides support for base quantity exceptions.
 * 
 * @author Steven R. Emmerson
 */
public abstract class BaseQuantityException extends UnitException implements
		Serializable {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from nothing.
	 */
	public BaseQuantityException() {
	}

	/**
	 * Constructs from a message.
	 */
	public BaseQuantityException(final String message) {
		super(message);
	}
}
