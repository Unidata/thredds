// $Id: UnitFormat.java,v 1.5 2000/08/18 04:17:36 russ Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */
package ucar.units;

/**
 * Interface for classes that parse and format unit specifications.
 *
 * @author Steven R. Emmerson
 * @version $Id: UnitFormat.java,v 1.5 2000/08/18 04:17:36 russ Exp $
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
