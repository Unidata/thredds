/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for failure to find a unit.
 * 
 * @author Steven R. Emmerson
 */
public class NoSuchUnitException extends SpecificationException {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from a unit identifier.
	 * 
	 * @param id
	 *            The identifier of the unit that couldn't be found.
	 */
	public NoSuchUnitException(final UnitID id) {
		this(id.toString());
	}

	/**
	 * Constructs from a unit identifier.
	 * 
	 * @param id
	 *            The identifier of the unit that couldn't be found.
	 */
	public NoSuchUnitException(final String id) {
		super("Unit \"" + id + "\" not in database");
	}
}
