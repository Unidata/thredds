// $Id: Unit.java 64 2006-07-12 22:30:50Z edavis $
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

import java.util.Date;

/**
 * Interface for units.
 * 
 * @author Steven R. Emmerson
 * @version $Id: Unit.java 64 2006-07-12 22:30:50Z edavis $
 */
public interface Unit {
	/**
	 * Gets the identifier of this unit.
	 * 
	 * @return The identifier of this unit. May be null.
	 */
	public UnitName getUnitName();

	/**
	 * Gets the name of this unit.
	 * 
	 * @return The name of this unit. May be null.
	 */
	public String getName();

	/**
	 * Gets the plural form of the name of this unit.
	 * 
	 * @return The plural of the name of this unit. May be null.
	 */
	public String getPlural();

	/**
	 * Gets the symbol of this unit.
	 * 
	 * @return The symbol for this unit. May be null.
	 */
	public String getSymbol();

	/**
	 * Returns the string representation of the unit.
	 * 
	 * @return The string representation of the unit
	 */
	public String toString();

	/**
	 * Returns the canonical string representation of the unit.
	 * 
	 * @return The canonical string representation.
	 */
	public String getCanonicalString();

	/**
	 * Returns the derived unit that underlies this unit.
	 * 
	 * @return The derived unit that underlies this unit.
	 */
	public DerivedUnit getDerivedUnit();

	/**
	 * Clones this unit, changing the identifier.
	 * 
	 * @param id
	 *            The identifier for the new unit.
	 * @return The new unit.
	 */
	public Unit clone(UnitName id);

	/**
	 * Multiplies this unit by another.
	 * 
	 * @param that
	 *            The other unit.
	 * @return The product of multiplying this unit by the other unit.
	 * @throws MultiplyException
	 *             Can't multiply these units.
	 */
	public Unit multiplyBy(Unit that) throws MultiplyException;

	/**
	 * Multiplies this unit by a scale factor. For example, if {@code m} is a
	 * meter unit, then {@code m.multiplyBy(1e-2)} returns a centimeter unit.
	 * 
	 * @param scale
	 *            The scale factor.
	 * @return The result of multiplying this unit by the scale factor.
	 * @throws MultiplyException
	 *             if {@code scale} is zero.
	 */
	public Unit multiplyBy(double scale) throws MultiplyException;

	/**
	 * Divides this unit by another.
	 * 
	 * @param that
	 *            The other unit.
	 * @return The quotient of dividing this unit by the other unit.
	 * @throws OperationException
	 *             Can't divide these units.
	 */
	public Unit divideBy(Unit that) throws OperationException;

	/**
	 * Divides this unit into another.
	 * 
	 * @param that
	 *            The other unit.
	 * @return The quotient of dividing this unit into the other unit.
	 * @throws OperationException
	 *             Can't divide these units.
	 */
	public Unit divideInto(Unit that) throws OperationException;

	/**
	 * Raises this unit to a power.
	 * 
	 * @param power
	 *            The power.
	 * @return This result of raising this unit to the power.
	 * @throws RaiseException
	 *             Can't raise this unit to a power.
	 */
	public Unit raiseTo(int power) throws RaiseException;

	/**
	 * Returns a unit identical to this instance but whose origin (i.e., zero
	 * value) has been shifted to the given value. For example, if {@code degK}
	 * is a Kelvin unit, then {@code degK.shiftTo(273.15)} is a Celsius unit.
	 * 
	 * @param origin
	 *            The new origin in units of this instance.
	 * @return A unit convertible with this instance but whose zero value is
	 *         equal to the value {@code origin} of this instance.
	 * @throws ShiftException
	 *             if the corresponding new unit can't be created.
	 */
	public Unit shiftTo(double origin) throws ShiftException;

	/**
	 * Returns a unit identical to this instance but whose origin (i.e., zero
	 * value) has been shifted to the given time. For example, if {@code sec} is
	 * a second unit, then {@code sec.shiftTo(new Date(0L)} is the unit
	 * corresponding to seconds since the epoch (1970-01-01 00:00:00 UTC).
	 * 
	 * @param origin
	 *            The new origin.
	 * @return A unit whose zero value is the time given by {@code origin}.
	 * @throws ShiftException
	 *             if the corresponding new unit can't be created. For example,
	 *             if this instance isn't a unit of time.
	 */
	public Unit shiftTo(Date origin) throws ShiftException;

	/**
	 * Returns a logarithmic unit whose reference level is equal to this unit.
	 * For example, if {@code mW} is a milliwatt unit, then {@code mW.log(10.)}
	 * returns a base-ten logarithmic unit with a milliwatt reference level.
	 * 
	 * @param base
	 *            The logarithmic base. Must be one of {@code 2}, {@link Math#E}
	 *            , or {@code 10}.
	 * @throws IllegalArgumentException
	 *             if {@code base} isn't one of the allowed values.
	 */
	public Unit log(double base);

	/**
	 * Gets a Converter that converts numeric values from this unit to another,
	 * compatible unit.
	 * 
	 * @param outputUnit
	 *            The unit to which to convert the numerical values.
	 * @return A converter of numeric values from this unit to the other unit.
	 * @throws ConversionException
	 *             The units aren't compatible.
	 */
	public Converter getConverterTo(Unit outputUnit) throws ConversionException;

	/**
	 * Converts a numerical value from this unit to another unit.
	 * 
	 * @param amount
	 *            The numerical value in this unit.
	 * @param outputUnit
	 *            The unit to which to convert the numerical value.
	 * @return The numerical value in the output unit.
	 * @throws ConversionException
	 *             The units aren't compatible.
	 */
	public float convertTo(float amount, Unit outputUnit)
			throws ConversionException;

	/**
	 * Converts a numerical value from this unit to another unit.
	 * 
	 * @param amount
	 *            The numerical value in this unit.
	 * @param outputUnit
	 *            The unit to which to convert the numerical value.
	 * @return The numerical value in the output unit.
	 * @throws ConversionException
	 *             The units aren't compatible.
	 */
	public double convertTo(double amount, Unit outputUnit)
			throws ConversionException;

	/**
	 * Converts numerical values from this unit to another unit.
	 * 
	 * @param amounts
	 *            The numerical values in this unit.
	 * @param outputUnit
	 *            The unit to which to convert the numerical values.
	 * @return The numerical values in the output unit. in allocated space.
	 * @throws ConversionException
	 *             The units aren't compatible.
	 */
	public float[] convertTo(float[] amounts, Unit outputUnit)
			throws ConversionException;

	/**
	 * Converts numerical values from this unit to another unit.
	 * 
	 * @param amounts
	 *            The numerical values in this unit.
	 * @param outputUnit
	 *            The unit to which to convert the numerical values.
	 * @return The numerical values in the output unit. in allocated space.
	 * @throws ConversionException
	 *             The units aren't compatible.
	 */
	public double[] convertTo(double[] amounts, Unit outputUnit)
			throws ConversionException;

	/**
	 * Converts numerical values from this unit to another unit.
	 * 
	 * @param input
	 *            The numerical values in this unit.
	 * @param outputUnit
	 *            The unit to which to convert the numerical values.
	 * @param output
	 *            The output numerical values. May be the same array as the
	 *            input values.
	 * @return <code>output</code>.
	 * @throws ConversionException
	 *             The units aren't compatible.
	 */
	public float[] convertTo(float[] input, Unit outputUnit, float[] output)
			throws ConversionException;

	/**
	 * Converts numerical values from this unit to another unit.
	 * 
	 * @param input
	 *            The numerical values in this unit.
	 * @param outputUnit
	 *            The unit to which to convert the numerical values.
	 * @param output
	 *            The output numerical values. May be the same array as the
	 *            input values.
	 * @return <code>output</code>.
	 * @throws ConversionException
	 *             The units aren't compatible.
	 */
	public double[] convertTo(double[] input, Unit outputUnit, double[] output)
			throws ConversionException;

	/**
	 * Indicates if this unit is compatible with another unit.
	 * 
	 * @param that
	 *            The other unit.
	 * @return True iff values in this unit are convertible to values in the
	 *         other unit.
	 */
	public boolean isCompatible(Unit that);

	/**
	 * Indicates if this unit is semantically identical to an object.
	 * 
	 * @param object
	 *            The object.
	 * @return <code>true</code> if and only if this unit is semantically
	 *         identical to the object.
	 */
	public boolean equals(Object object);

	/**
	 * Makes a label for a named quantity.
	 * 
	 * @param quantityID
	 *            An identifier of the quantity for which the label is intended
	 *            (e.g. "altitude").
	 * @return A label (e.g. "altitude/km").
	 */
	public String makeLabel(String quantityID);

	/**
	 * Indicates if values in this unit are dimensionless.
	 * 
	 * @return <code>true</code> if and only if this unit is dimensionless.
	 */
	public boolean isDimensionless();
}
