/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.util.Date;

/**
 * Provides support for classes that implement units.
 * 
 * @author Steven R. Emmerson
 */
@Immutable
public abstract class UnitImpl implements Unit, Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * The unit identifier.
     * 
     * @serial
     */
    private final UnitName    id;

    /**
     * Constructs with no ID.
     */
    protected UnitImpl() {
        this(null);
    }

    /**
     * Constructs with the given ID.
     * 
     * @param id
     *            The id of the unit (e.g. "foot"). May be null.
     */
    protected UnitImpl(final UnitName id) {
        this.id = id;
    }

    /**
     * Gets the identifier of this unit.
     * 
     * @return The ID of this unit. May be <code>null</code>.
     */
    public final UnitName getUnitName() {
        return id;
    }

    /**
     * Gets the name of the unit.
     * 
     * @return The name of the unit. May be <code>null</code>.
     */
    public final String getName() {
        return id == null
                ? null
                : id.getName();
    }

    /**
     * Gets the plural form of the name of the unit.
     * 
     * @return The plural of the name of the unit. May be <code>null</code>.
     */
    public final String getPlural() {
        return id == null
                ? null
                : id.getPlural();
    }

    /**
     * Gets the symbol for the unit.
     * 
     * @return The symbol of the unit. May be <code>null</code>.
     */
    public final String getSymbol() {
        return id == null
                ? null
                : id.getSymbol();
    }

    public Unit shiftTo(final double origin) throws ShiftException {
        return OffsetUnit.getInstance(this, origin);
    }

    public Unit shiftTo(final Date origin) throws ShiftException {
        return TimeScaleUnit.getInstance(this, origin);
    }

    /**
     * Multiplies this unit by another.
     * 
     * @param that
     *            The other unit.
     * @return The product of this unit multiplied by the other unit.
     * @throws MultiplyException
     *             Can't multiply these units.
     */
    public final Unit multiplyBy(final Unit that) throws MultiplyException {
        return myMultiplyBy(that);
    }

    public Unit multiplyBy(final double scale) throws MultiplyException {
        return ScaledUnit.getInstance(scale, this);
    }

    /**
     * Multiplies this unit by another.
     * 
     * @param that
     *            The other unit.
     * @return The product of this unit multiplied by the other unit.
     * @throws MultiplyException
     *             Can't multiply these units.
     */
    protected abstract Unit myMultiplyBy(Unit that) throws MultiplyException;

    /**
     * Divides this unit by another.
     * 
     * @param that
     *            The other unit.
     * @return The quotient of this unit divided by the other unit.
     * @throws OperationException
     *             Can't divide these units.
     */
    public final Unit divideBy(final Unit that) throws OperationException {
        return myDivideBy(that);
    }

    /**
     * Divides this unit by another.
     * 
     * @param unit
     *            The other unit.
     * @return The quotient of this unit divided by the other unit.
     * @throws OperationException
     *             Can't divide these units.
     */
    protected abstract Unit myDivideBy(Unit unit) throws OperationException;

    /**
     * Divides this unit into another.
     * 
     * @param that
     *            The other unit.
     * @return The quotient of this unit divided into the other unit.
     * @throws OperationException
     *             Can't divide these units.
     */
    public final Unit divideInto(final Unit that) throws OperationException {
        return myDivideInto(that);
    }

    /**
     * Divides this unit into another.
     * 
     * @param unit
     *            The other unit.
     * @return The quotient of this unit divided into the other unit.
     * @throws OperationException
     *             Can't divide these units.
     */
    protected abstract Unit myDivideInto(Unit unit) throws OperationException;

    /**
     * Raises this unit to a power.
     * 
     * @param power
     *            The power.
     * @return The result of raising this unit to the power.
     * @throws RaiseException
     *             Can't raise this unit to a power.
     */
    public final Unit raiseTo(final int power) throws RaiseException {
        return myRaiseTo(power);
    }

    /**
     * Raises this unit to a power.
     * 
     * @param power
     *            The power.
     * @return The result of raising this unit to the power.
     * @throws RaiseException
     *             Can't raise this unit to a power.
     */
    protected abstract Unit myRaiseTo(int power) throws RaiseException;

    /*
     * Default implementation.
     * 
     * @see ucar.units.Unit#log(double)
     */
    public Unit log(final double base) {
        return LogarithmicUnit.getInstance(this, base);
    }

    /**
     * Provides support for converting numeric values from this unit to another
     * unit.
     */
    protected static class MyConverter extends ConverterImpl {
        private final DerivableUnit fromUnit;
        private final DerivableUnit toUnit;

        protected MyConverter(final Unit fromUnit, final Unit toUnit)
                throws ConversionException {
            super(fromUnit, toUnit);
            if (!(fromUnit instanceof DerivableUnit)
                    || !(toUnit instanceof DerivableUnit)) {
                throw new ConversionException(fromUnit, toUnit);
            }
            this.fromUnit = (DerivableUnit) fromUnit;
            this.toUnit = (DerivableUnit) toUnit;
        }

        public double convert(final double amount) {
            double output;
            try {
                output = toUnit.fromDerivedUnit(fromUnit.toDerivedUnit(amount));
            }
            catch (final ConversionException e) {
                output = 0;
            } // can't happen because isCompatible() vetted
            return output;
        }

        public float[] convert(final float[] input, final float[] output) {
            try {
                toUnit.fromDerivedUnit(fromUnit.toDerivedUnit(input, output),
                        output);
            }
            catch (final ConversionException e) {
            } // can't happen because isCompatible() vetted
            return output;
        }

        public double[] convert(final double[] input, final double[] output) {
            try {
                toUnit.fromDerivedUnit(fromUnit.toDerivedUnit(input, output),
                        output);
            }
            catch (final ConversionException e) {
            } // can't happen because isCompatible() vetted
            return output;
        }
    }

    /**
     * Gets a Converter for converting numeric values from this unit to another,
     * compatible unit.
     * 
     * @param outputUnit
     *            The unit to which to convert the numeric values.
     * @return A converter of values from this unit to the other unit.
     * @throws ConversionException
     *             The units aren't convertible.
     */
    public Converter getConverterTo(final Unit outputUnit)
            throws ConversionException {
        return new MyConverter(this, outputUnit);
    }

    /**
     * Converts a numeric value from this unit to another unit.
     * 
     * @param amount
     *            The numeric value.
     * @param outputUnit
     *            The unit to which to convert the numeric value.
     * @return The numeric value in the output unit.
     * @throws ConversionException
     *             The units aren't convertible.
     */
    public float convertTo(final float amount, final Unit outputUnit)
            throws ConversionException {
        return (float) convertTo((double) amount, outputUnit);
    }

    /**
     * Converts a numeric value from this unit to another unit.
     * 
     * @param amount
     *            The numeric value.
     * @param outputUnit
     *            The unit to which to convert the numeric value.
     * @return The numeric value in the output unit.
     * @throws ConversionException
     *             The units aren't convertible.
     */
    public double convertTo(final double amount, final Unit outputUnit)
            throws ConversionException {
        return getConverterTo(outputUnit).convert(amount);
    }

    /**
     * Converts numeric values from this unit to another unit.
     * 
     * @param amounts
     *            The numeric values.
     * @param outputUnit
     *            The unit to which to convert the numeric values.
     * @return The numeric values in the output unit in allocated space.
     * @throws ConversionException
     *             The units aren't convertible.
     */
    public float[] convertTo(final float[] amounts, final Unit outputUnit)
            throws ConversionException {
        return convertTo(amounts, outputUnit, new float[amounts.length]);
    }

    /**
     * Converts numeric values from this unit to another unit.
     * 
     * @param amounts
     *            The numeric values.
     * @param outputUnit
     *            The unit to which to convert the numeric values.
     * @return The numeric values in the output unit in allocated space.
     * @throws ConversionException
     *             The units aren't convertible.
     */
    public double[] convertTo(final double[] amounts, final Unit outputUnit)
            throws ConversionException {
        return convertTo(amounts, outputUnit, new double[amounts.length]);
    }

    /**
     * Converts numeric values from this unit to another unit.
     * 
     * @param input
     *            The input numeric values.
     * @param outputUnit
     *            The unit to which to convert the numeric values.
     * @param output
     *            The output numeric values. May be the same array as the input
     *            values.
     * @return The numeric values in the output unit.
     * @throws ConversionException
     *             The units aren't convertible.
     */
    public float[] convertTo(final float[] input, final Unit outputUnit,
            final float[] output) throws ConversionException {
        return getConverterTo(outputUnit).convert(input, output);
    }

    /**
     * Converts numeric values from this unit to another unit.
     * 
     * @param input
     *            The input numeric values.
     * @param outputUnit
     *            The unit to which to convert the numeric values.
     * @param output
     *            The output numeric values. May be the same array as the input
     *            values.
     * @return The numeric values in the output unit.
     * @throws ConversionException
     *             The units aren't convertible.
     */
    public double[] convertTo(final double[] input, final Unit outputUnit,
            final double[] output) throws ConversionException {
        return getConverterTo(outputUnit).convert(input, output);
    }

    /**
     * Indicates if numeric values in this unit are convertible with another
     * unit.
     * 
     * @param that
     *            The other unit.
     * @return <code>true</code> if and only if numeric values in this unit are
     *         convertible the other unit.
     */
    public boolean isCompatible(final Unit that) {
        // jeffm: for some reason just calling getDerivedUnit().equals(...)
        // with jikes 1.1.7 as the compiler causes the jvm to crash.
        // The Unit u1=... does not crash.
        final Unit u1 = getDerivedUnit();
        return u1.equals(that.getDerivedUnit());
        // return getDerivedUnit().equals(that.getDerivedUnit());
    }

    /**
     * Returns the hash code of this instance.
     * 
     * @return The hash code of this instance.
     */
    @Override
    public abstract int hashCode();

    /**
     * Indicates if two string are equal.
     * 
     * @param s1
     *            One string. May be <code>null</code>.
     * @param s2
     *            The other string. May be <code>null</code>.
     * @return <code>true</code> if an only if both strings are
     *         <code>null</code> or both strings are identical.
     */
    private static boolean equals(final String s1, final String s2) {
        return (s1 == null && s2 == null)
                || (s1 != null && s1.equals(s2));
    }

    /**
     * Indicates if two string are equal (ignoring case).
     * 
     * @param s1
     *            One string. May be <code>null</code>.
     * @param s2
     *            The other string. May be <code>null</code>.
     * @return <code>true</code> if an only if both strings are
     *         <code>null</code> or both strings are identical (ignoring case).
     */
    private static boolean equalsIgnoreCase(final String s1,
            final String s2) {
        return (s1 == null && s2 == null)
                || (s1 != null && s1.equalsIgnoreCase(s2));
    }

    /**
     * Returns the string representation of this unit.
     * 
     * @return The string representation of this unit.
     */
    @Override
    public String toString() {
        final String string = getSymbol();
        return string != null
                ? string
                : getName();
    }

    /**
     * Returns a label for a quantity in this unit.
     * 
     * @param quantityID
     *            The identifier for the quantity (e.g. "altitude").
     * @return The appropriate label (e.g. "altitude/m").
     */
    public String makeLabel(final String quantityID) {
        final StringBuilder buf = new StringBuilder(quantityID);
        if (quantityID.contains(" ")) {
            buf.insert(0, '(').append(')');
        }
        buf.append('/');
        final int start = buf.length();
        buf.append(toString());
        if (buf.substring(start).indexOf(' ') != -1) {
            buf.insert(start, '(').append(')');
        }
        return buf.toString();
    }
}
