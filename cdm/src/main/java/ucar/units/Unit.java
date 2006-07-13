// $Id: Unit.java 64 2006-07-12 22:30:50Z edavis $
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
 * Interface for units.
 *
 * @author Steven R. Emmerson
 * @version $Id: Unit.java 64 2006-07-12 22:30:50Z edavis $
 */
public interface
Unit
{
    /**
     * Gets the identifier of this unit.
     * @return			The identifier of this unit.  May be null.
     */
    public UnitName
    getUnitName();

    /**
     * Gets the name of this unit.
     * @return			The name of this unit.  May be null.
     */
    public String
    getName();

    /**
     * Gets the plural form of the name of this unit.
     * @return			The plural of the name of this unit.  May
     *				be null.
     */
    public String
    getPlural();

    /**
     * Gets the symbol of this unit.
     * @return			The symbol for this unit.  May be null.
     */
    public String
    getSymbol();

    /**
     * Returns the string representation of the unit.
     * @return			The string representation of the unit
     */
    public String
    toString();
    
    /**
     * Returns the canonical string representation of the unit.
     * @return                  The canonical string representation.
     */
    public String
    getCanonicalString();

    /**
     * Returns the derived unit that underlies this unit.
     * @return			The derived unit that underlies this unit.
     */
    public DerivedUnit
    getDerivedUnit();

    /**
     * Clones this unit, changing the identifier.
     * @param id		The identifier for the new unit.
     * @return			The new unit.
     */
    public Unit
    clone(UnitName id);

    /**
     * Multiplies this unit by another.
     * @param that		The other unit.
     * @return			The product of multiplying this unit by the
     *				other unit.
     * @throws MultiplyException	Can't multiply these units.
     */
    public Unit
    multiplyBy(Unit that)
	throws MultiplyException;

    /**
     * Divides this unit by another.
     * @param that		The other unit.
     * @return			The quotient of dividing this unit by the 
     *				other unit.
     * @throws OperationException	Can't divide these units.
     */
    public Unit
    divideBy(Unit that)
	throws OperationException;

    /**
     * Divides this unit into another.
     * @param that		The other unit.
     * @return			The quotient of dividing this unit into the
     *				other unit.
     * @throws OperationException	Can't divide these units.
     */
    public Unit
    divideInto(Unit that)
	throws OperationException;

    /**
     * Raises this unit to a power.
     * @param power		The power.
     * @return			This result of raising this unit to the power.
     * @throws RaiseException	Can't raise this unit to a power.
     */
    public Unit
    raiseTo(int power)
	throws RaiseException;

    /**
     * Gets a Converter that converts numeric values from this unit to
     * another, compatible unit.
     * @param outputUnit	The unit to which to convert the numerical
     *				values.
     * @return			A converter of numeric values from this unit to
     *				the other unit.
     * @throws ConversionException	The units aren't compatible.
     */
    public Converter
    getConverterTo(Unit outputUnit)
	throws ConversionException;

    /**
     * Converts a numerical value from this unit to another unit.
     * @param amount		The numerical value in this unit.
     * @param outputUnit	The unit to which to convert the numerical
     *				value.
     * @return			The numerical value in the output unit.
     * @throws ConversionException	The units aren't compatible.
     */
    public float
    convertTo(float amount, Unit outputUnit)
	throws ConversionException;

    /**
     * Converts a numerical value from this unit to another unit.
     * @param amount		The numerical value in this unit.
     * @param outputUnit	The unit to which to convert the numerical
     *				value.
     * @return			The numerical value in the output unit.
     * @throws ConversionException	The units aren't compatible.
     */
    public double
    convertTo(double amount, Unit outputUnit)
	throws ConversionException;

    /**
     * Converts numerical values from this unit to another unit.
     * @param amounts		The numerical values in this unit.
     * @param outputUnit	The unit to which to convert the numerical
     *				values.
     * @return			The numerical values in the output unit.
     *				in allocated space.
     * @throws ConversionException	The units aren't compatible.
     */
    public float[]
    convertTo(float[] amounts, Unit outputUnit)
	throws ConversionException;

    /**
     * Converts numerical values from this unit to another unit.
     * @param amounts		The numerical values in this unit.
     * @param outputUnit	The unit to which to convert the numerical
     *				values.
     * @return			The numerical values in the output unit.
     *				in allocated space.
     * @throws ConversionException	The units aren't compatible.
     */
    public double[]
    convertTo(double[] amounts, Unit outputUnit)
	throws ConversionException;

    /**
     * Converts numerical values from this unit to another unit.
     * @param input		The numerical values in this unit.
     * @param outputUnit	The unit to which to convert the numerical
     *				values.
     * @param output		The output numerical values.  May be the same
     *				array as the input values.
     * @return			<code>output</code>.
     * @throws ConversionException	The units aren't compatible.
     */
    public float[]
    convertTo(float[] input, Unit outputUnit, float[] output)
	throws ConversionException;

    /**
     * Converts numerical values from this unit to another unit.
     * @param input		The numerical values in this unit.
     * @param outputUnit	The unit to which to convert the numerical
     *				values.
     * @param output		The output numerical values.  May be the same
     *				array as the input values.
     * @return			<code>output</code>.
     * @throws ConversionException	The units aren't compatible.
     */
    public double[]
    convertTo(double[] input, Unit outputUnit, double[] output)
	throws ConversionException;

    /**
     * Indicates if this unit is compatible with another unit.
     * @param that		The other unit.
     * @return			True iff values in this unit are convertible
     *				to values in the other unit.
     */
    public boolean
    isCompatible(Unit that);

    /**
     * Indicates if this unit is semantically identical to an object.
     * @param object		The object.
     * @return			<code>true</code> if and only if this unit
     *				is semantically identical to the object.
     */
    public boolean
    equals(Object object);

    /**
     * Makes a label for a named quantity.
     * @param quantityID	An identifier of the quantity for which the
     *				label is intended (e.g. "altitude").
     * @return			A label (e.g. "altitude/km").
     */
    public String
    makeLabel(String quantityID);

    /**
     * Indicates if values in this unit are dimensionless.
     * @return			<code>true</code> if and only if this unit is
     *				dimensionless.
     */
    public boolean
    isDimensionless();
}
