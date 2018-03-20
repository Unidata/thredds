/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for failures due to attempts to redefine an existing unit in
 * a unit database.
 * 
 * @author Steven R. Emmerson
 */
public class UnitExistsException extends UnitDBException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from the existing unit and the redefining unit.
	 * 
	 * @param oldUnit
	 *            The previously existing unit in the database.
	 * @param newUnit
	 *            The unit attempting the redefinition.
	 */
	public UnitExistsException(final Unit oldUnit, final Unit newUnit) {
		this("Attempt to replace \"" + oldUnit + "\" with \"" + newUnit
				+ "\" in unit database");
	}

	/**
	 * Constructs from an error message.
	 * 
	 * @param msg
	 *            The error message.
	 */
	public UnitExistsException(final String msg) {
		super(msg);
	}
}
