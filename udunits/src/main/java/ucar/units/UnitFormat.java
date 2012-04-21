// $Id: UnitFormat.java 64 2006-07-12 22:30:50Z edavis $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.units;

/**
 * Interface for classes that parse and format unit specifications.
 *
 * @author Steven R. Emmerson
 * @version $Id: UnitFormat.java 64 2006-07-12 22:30:50Z edavis $
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
    public Unit
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
    public Unit
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
    public String
    format(Factor factor);

    /**
     * Appends a formatted factor to a string buffer.  A factor is a
     * base unit/exponent pair).
     * @param factor		The base unit/exponent pair.
     * @param buffer		The string buffer to be appended to.
     * @return			The string buffer.
     */
    public StringBuffer
    format(Factor factor, StringBuffer buffer);

    /**
     * Formats a unit.  If the unit has a symbol or name, then one of them
     * will be used; otherwise, a specification of the unit in terms of
     * underlying units will be returned.
     * @param unit		The unit.
     * @return			The formatted unit.
     */
    public String
    format(Unit unit)
	throws UnitClassException;

    /**
     * Formats a unit using a long form.  This always returns a specification
     * for the unit in terms of underlying units: it doesn't return the name
     * or symbol of the unit unless the unit is a base unit.
     * @param unit		The unit.
     * @return			The formatted unit.
     */
    public String
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
    public StringBuffer
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
    public StringBuffer
    longFormat(Unit unit, StringBuffer buffer)
	throws UnitClassException;
}
