/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Interface for classes that parse and format unit specifications.
 *
 * @author Steven R. Emmerson
 */
public interface
UnitFormat
{
    /**
     * Parses a unit specification.
     * @param spec		The unit specification (e.g. "m/s");
     * @return			The unit corresponding to the specification.
     * @throws NoSuchUnitException	A unit in the specification couldn't be
     *					found (e.g. the "m" in the example).
     * @throws UnitParseException	The specification is grammatically
     *					incorrect.
     * @throws SpecificationException	The specification is incorrect somehow.
     * @throws UnitDBException		Problem with the unit database.
     * @throws PrefixDBException	Problem with the unit-prefix database.
     * @throws UnitSystemException	Problem with the system of units.
     */
    Unit
    parse(String spec)
	throws NoSuchUnitException,
	    UnitParseException,
	    SpecificationException,
	    UnitDBException,
	    PrefixDBException,
	    UnitSystemException;

    /**
     * Parses a unit specification.
     * @param spec		The unit specification (e.g. "m/s");
     * @param unitDB		The unit database.
     * @return			The unit corresponding to the specification.
     * @throws NoSuchUnitException	A unit in the specification couldn't be
     *					found (e.g. the "m" in the example).
     * @throws UnitParseException	The specification is grammatically
     *					incorrect.
     * @throws SpecificationException	The specification is incorrect somehow.
     * @throws UnitDBException		Problem with the unit database.
     * @throws PrefixDBException	Problem with the unit-prefix database.
     * @throws UnitSystemException	Problem with the system of units.
     */
    Unit
    parse(String spec, UnitDB unitDB)
	throws NoSuchUnitException,
	    UnitParseException,
	    SpecificationException,
	    UnitDBException,
	    PrefixDBException,
	    UnitSystemException;

    /**
     * Formats a Factor (a base unit/exponent pair).
     * @param factor		The base unit/exponent pair.
     * @return			The formatted factor.
     */
    String
    format(Factor factor);

    /**
     * Appends a formatted factor to a string buffer.  A factor is a
     * base unit/exponent pair).
     * @param factor		The base unit/exponent pair.
     * @param buffer		The string buffer to be appended to.
     * @return			The string buffer.
     */
    StringBuffer
    format(Factor factor, StringBuffer buffer);

    /**
     * Formats a unit.  If the unit has a symbol or name, then one of them
     * will be used; otherwise, a specification of the unit in terms of
     * underlying units will be returned.
     * @param unit		The unit.
     * @return			The formatted unit.
     */
    String
    format(Unit unit)
	throws UnitClassException;

    /**
     * Formats a unit using a long form.  This always returns a specification
     * for the unit in terms of underlying units: it doesn't return the name
     * or symbol of the unit unless the unit is a base unit.
     * @param unit		The unit.
     * @return			The formatted unit.
     */
    String
    longFormat(Unit unit)
	throws UnitClassException;

    /**
     * Appends a formatted unit to a string buffer.  This is similar to
     * <code>format(Unit)</code> but it appends the specification to a
     * string buffer.
     * @param unit		The unit.
     * @param buffer		The string buffer to be appended to.
     * @return			The string buffer.
     */
    StringBuffer
    format(Unit unit, StringBuffer buffer)
	throws UnitClassException;

    /**
     * Appends a unit formatted according to the long form to a string buffer.
     * This is similar to <code>longFormat(Unit)</code> but it appends the
     * specification to a string buffer.
     * @param unit		The unit.
     * @param buffer		The string buffer to be appended to.
     * @return			The string buffer.
     */
    StringBuffer
    longFormat(Unit unit, StringBuffer buffer)
	throws UnitClassException;
}
