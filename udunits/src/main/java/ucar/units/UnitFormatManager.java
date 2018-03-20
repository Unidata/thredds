/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import java.io.Serializable;

/**
 * Provides support for managing a default UnitFormat.
 * 
 * @author Steven R. Emmerson
 */
public final class UnitFormatManager implements Serializable {
	private static final long	serialVersionUID	= 1L;
	/**
	 * The singleton instance of the default unit format.
	 * 
	 * @serial
	 */
	private static UnitFormat	instance;

	/**
	 * Returns an instance of the default unit format.
	 * 
	 * @return An instance of the default unit format.
	 */
	public static synchronized UnitFormat instance() {
		if (instance == null) {
	    instance = StandardUnitFormat.instance();
		}
		return instance;
	}

	/**
	 * Sets the instance of the default unit format. You'd better know what
	 * you're doing if you call this method.
	 * 
	 * @param instance An instance of the new, default unit format.
	 */
	public static synchronized void setInstance(final UnitFormat instance) {
		UnitFormatManager.instance = instance;
	}
}
