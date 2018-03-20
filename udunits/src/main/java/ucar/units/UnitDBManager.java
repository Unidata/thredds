/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import java.io.Serializable;

/**
 * Provides support for managing a default unit database.
 * 
 * @author Steven R. Emmerson
 */
public final class UnitDBManager implements Serializable {
	private static final long	serialVersionUID	= 1L;
	/**
	 * The singleton instance of the default unit database.
	 * 
	 * @serial
	 */
	private static UnitDB		instance;

	/**
	 * Gets the default unit database.
	 * 
	 * @return The default unit database.
	 * @throws UnitDBException
	 *             The default unit database couldn't be created.
	 */
	public static synchronized UnitDB instance() throws UnitDBException {
			if (instance == null) {
				instance = StandardUnitDB.instance();
			}
		return instance;
	}

	/**
	 * Sets the default unit database. You'd better know what you're doing if
	 * you call this method!
	 * 
	 * @param instance
	 *            The unit database to be made the default one.
	 */
	public static synchronized void setInstance(final UnitDB instance) {
		UnitDBManager.instance = instance;
	}
}
