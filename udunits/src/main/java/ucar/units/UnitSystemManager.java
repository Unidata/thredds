/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import java.io.Serializable;

/**
 * Provides support for managing a UnitSystem.
 * 
 * @author Steven R. Emmerson
 */
public final class UnitSystemManager implements Serializable {
	private static final long	serialVersionUID	= 1L;

	/**
	 * The singleton instance of the system of units.
	 * 
	 * @serial
	 */
	private static UnitSystem	instance;

	/**
	 * Returns an instance of the system of units.
	 * 
	 * @return An instance of the system of units.
	 */
	public static synchronized UnitSystem instance() throws UnitSystemException {
			if (instance == null) {
				instance = SI.instance();
		}
		return instance;
	}

	/**
	 * Sets the system of units. This must be called before any call to
	 * <code>instance()</code>.
	 * 
	 * @param instance
	 *            The system of units.
	 * @throws UnitSystemException
	 *             <code>instance()</code> was called earlier.
	 */
	public static synchronized void setInstance(final UnitSystem instance)
			throws UnitSystemException {
		if (instance != null) {
			throw new UnitSystemException("Unit system already used");
		}
		UnitSystemManager.instance = instance;
	}
}
