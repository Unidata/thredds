/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import java.util.Date;

/**
 * Interface for units.
 * 
 * @author Steven R. Emmerson
 */
public interface Unit {
	/**
	 * Gets the identifier of this unit.
	 * 
	 * @return The identifier of this unit. May be null.
	 */
  UnitName getUnitName();

	/**
	 * Gets the name of this unit.
	 * 
	 * @return The name of this unit. May be null.
	 */
  String getName();

	/**
	 * Gets the plural form of the name of this unit.
	 * 
	 * @return The plural of the name of this unit. May be null.
	 */
  String getPlural();

	/**
	 * Gets the symbol of this unit.
	 * 
	 * @return The symbol for this unit. May be null.
	 */
  String getSymbol();

	/**
	 * Returns the string representation of the unit.
	 * 
	 * @return The string representation of the unit
	 */
  String toString();

	/**
	 * Returns the canonical string representation of the unit.
	 * 
	 * @return The canonical string representation.
	 */
  String getCanonicalString();

	/**
	 * Returns the derived unit that underlies this unit.
	 * 
	 * @return The derived unit that underlies this unit.
	 */
  DerivedUnit getDerivedUnit();

	/**
	 * Clones this unit, changing the identifier.
	 * 
	 * @param id
	 *            The identifier for the new unit.
	 * @return The new unit.
	 */
  Unit clone(UnitName id);

	/**
	 * Multiplies this unit by another.
	 * 
	 * @param that
	 *            The other unit.
	 * @return The product of multiplying this unit by the other unit.
	 * @throws MultiplyException
	 *             Can't multiply these units.
	 */
  Unit multiplyBy(Unit that) throws MultiplyException;

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
  Unit multiplyBy(double scale) throws MultiplyException;

	/**
	 * Divides this unit by another.
	 * 
	 * @param that
	 *            The other unit.
	 * @return The quotient of dividing this unit by the other unit.
	 * @throws OperationException
	 *             Can't divide these units.
	 */
  Unit divideBy(Unit that) throws OperationException;

	/**
	 * Divides this unit into another.
	 * 
	 * @param that
	 *            The other unit.
	 * @return The quotient of dividing this unit into the other unit.
	 * @throws OperationException
	 *             Can't divide these units.
	 */
  Unit divideInto(Unit that) throws OperationException;

	/**
	 * Raises this unit to a power.
	 * 
	 * @param power
	 *            The power.
	 * @return This result of raising this unit to the power.
	 * @throws RaiseException
	 *             Can't raise this unit to a power.
	 */
  Unit raiseTo(int power) throws RaiseException;

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
  Unit shiftTo(double origin) throws ShiftException;

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
  Unit shiftTo(Date origin) throws ShiftException;

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
  Unit log(double base);

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
  Converter getConverterTo(Unit outputUnit) throws ConversionException;

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
  float convertTo(float amount, Unit outputUnit)
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
  double convertTo(double amount, Unit outputUnit)
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
  float[] convertTo(float[] amounts, Unit outputUnit)
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
  double[] convertTo(double[] amounts, Unit outputUnit)
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
  float[] convertTo(float[] input, Unit outputUnit, float[] output)
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
  double[] convertTo(double[] input, Unit outputUnit, double[] output)
			throws ConversionException;

	/**
	 * Indicates if this unit is compatible with another unit.
	 * 
	 * @param that
	 *            The other unit.
	 * @return True iff values in this unit are convertible to values in the
	 *         other unit.
	 */
  boolean isCompatible(Unit that);

	/**
	 * Indicates if this unit is semantically identical to an object.
	 * 
	 * @param object
	 *            The object.
	 * @return <code>true</code> if and only if this unit is semantically
	 *         identical to the object.
	 */
  boolean equals(Object object);

	/**
	 * Makes a label for a named quantity.
	 * 
	 * @param quantityID
	 *            An identifier of the quantity for which the label is intended
	 *            (e.g. "altitude").
	 * @return A label (e.g. "altitude/km").
	 */
  String makeLabel(String quantityID);

	/**
	 * Indicates if values in this unit are dimensionless.
	 * 
	 * @return <code>true</code> if and only if this unit is dimensionless.
	 */
  boolean isDimensionless();
}
