/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;


/**
 * Provides support for classes that parse and format unit specifications.
 *
 * Instances are thread-safe.
 * 
 * @author Steven R. Emmerson
 */
public abstract class UnitFormatImpl implements UnitFormat {

	private static final long	serialVersionUID	= 1L;
	private static final Object	MUTEX	= new Object();

	/**
	 * Parses a unit specification.  This method is thread-safe.
	 * 
	 * @param spec
	 *            The unit specification (e.g. "m/s");
	 * @return The unit corresponding to the specification.
	 * @throws NoSuchUnitException
	 *             A unit in the specification couldn't be found (e.g. the "m"
	 *             in the example).
	 * @throws UnitParseException
	 *             The specification is grammatically incorrect.
	 * @throws SpecificationException
	 *             The specification is incorrect somehow.
	 * @throws UnitDBException
	 *             Problem with the unit database.
	 * @throws PrefixDBException
	 *             Problem with the unit-prefix database.
	 * @throws UnitSystemException
	 *             Problem with the system of units.
	 */
	public final Unit parse(final String spec) throws NoSuchUnitException,
			UnitParseException, SpecificationException, UnitDBException,
			PrefixDBException, UnitSystemException {
		synchronized (MUTEX) {
			return parse(spec, UnitDBManager.instance());
		}
	}

	/**
	 * Formats a Factor (a base unit/exponent pair).
	 * 
	 * @param factor
	 *            The base unit/exponent pair.
	 * @return The formatted factor.
	 */
	public final String format(final Factor factor) {
		return format(factor, new StringBuffer(8)).toString();
	}

	/**
	 * Formats a unit. If the unit has a symbol or name, then one of them will
	 * be used; otherwise, a specification of the unit in terms of underlying
	 * units will be returned.
	 * 
	 * @param unit
	 *            The unit.
	 * @return The formatted unit.
	 */
	public final String format(final Unit unit) throws UnitClassException {
		return format(unit, new StringBuffer(80)).toString();
	}

	/**
	 * Formats a unit using a long form. This always returns a specification for
	 * the unit in terms of underlying units: it doesn't return the name or
	 * symbol of the unit unless the unit is a base unit.
	 * 
	 * @param unit
	 *            The unit.
	 * @return The formatted unit.
	 */
	public final String longFormat(final Unit unit) throws UnitClassException {
		return longFormat(unit, new StringBuffer(80)).toString();
	}
}
