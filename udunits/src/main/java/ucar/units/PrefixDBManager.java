/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import java.io.Serializable;

/**
 * Provides support for managing a database of unit prefixes.
 * 
 * @author Steven R. Emmerson
 */
public final class PrefixDBManager implements Serializable {
	private static final long	serialVersionUID	= 1L;
	/**
	 * @serial
	 */
	private static PrefixDB		instance;

	/**
	 * Gets the current prefix database.
	 * 
	 * @return The current prefix database.
	 * @throws PrefixDBException
	 *             The current prefix database couldn't be created.
	 */
	public static synchronized PrefixDB instance() throws PrefixDBException {
		if (instance == null) {
					instance = StandardPrefixDB.instance();
		}
		return instance;
	}

	/**
	 * Sets the current prefix database.
	 * 
	 * @param instance
	 *            The prefix database to be made the current one.
	 */
	public static synchronized void setInstance(final PrefixDB instance) {
		PrefixDBManager.instance = instance;
	}
}
