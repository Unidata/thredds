/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import javax.annotation.concurrent.Immutable;

/**
 * Provides support for units that are based on a logarithm of the ratio of a
 * physical quantity to an underlying reference level.
 * 
 * Instances of this class are immutable.
 * 
 * @author Steven R. Emmerson
 */
@Immutable
public final class LogarithmicUnit extends UnitImpl implements DerivableUnit {
    private static final long           serialVersionUID = 1L;

    /**
     * The logarithmic base.
     * 
     * @serial
     */
    private final double                base;

    private final transient double      lnBase;

    /**
     * The reference level.
     * 
     * @serial
     */
    private final DerivableUnit         reference;
    private final transient DerivedUnit derivedUnit;

    /**
     * Constructs from a reference level and a logarithmic base.
     * 
     * @param reference
     *            The reference level. Must be a {@link DerivableUnit}.
     * @param base
     *            The logarithmic base. Must be 2, {@link Math#E}, or 10.
     * @throws IllegalArgumentException
     *             if {@code reference} isn't a {@link DerivableUnit}.
     * @throws IllegalArgumentException
     *             if {@code base} isn't one of the allowed values.
     * @throws NullPointerException
     *             if {@code reference} is {@code null}.
     */
    public LogarithmicUnit(final Unit reference, final double base) {
        this(reference, base, null);
    }

    /**
     * Constructs from a reference level, a logarithmic base, and a unit
     * identifier.
     * 
     * @param reference
     *            The reference level. Must be a {@link DerivableUnit}.
     * @param base
     *            The logarithmic base. Must be 2, {@link Math#E}, or 10.
     * @param id
     *            The identifier for the new unit.
     * @throws IllegalArgumentException
     *             if {@code reference} isn't a {@link DerivableUnit}.
     * @throws IllegalArgumentException
     *             if {@code base} isn't one of the allowed values.
     * @throws NullPointerException
     *             if {@code reference} is {@code null}.
     */
    public LogarithmicUnit(final Unit reference, final double base,
            final UnitName id) {
        super(id);
        if (reference == null) {
            throw new NullPointerException("Null reference argument");
        }
        if (!(reference instanceof DerivableUnit)) {
            throw new IllegalArgumentException("Not a DerivableUnit: "
                    + reference);
        }
        this.reference = (DerivableUnit) reference;
        if (base != 2 && base != 10 && base != Math.E) {
            throw new IllegalArgumentException("Invalid base: " + base);
        }
        this.base = base;
        lnBase = base == Math.E
                ? 1
                : Math.log(base);
        derivedUnit = reference.getDerivedUnit();
    }

    static Unit getInstance(final Unit unit, final double base) {
        return new LogarithmicUnit(unit, base);
    }

    /**
     * Returns the reference level.
     * 
     * @return The reference level.
     */
    public DerivableUnit getReference() {
        return reference;
    }

    /**
     * Returns the logarithmic base.
     * 
     * @return The logarithmic base of this unit.
     */
    public double getBase() {
        return base;
    }

    /*
     * From UnitImpl:
     */

    /**
     * Clones this unit, changing the identifier.
     * 
     * @param id
     *            The identifier for the new unit.
     * @return This unit with its identifier changed.
     */
    public Unit clone(final UnitName id) {
        return new LogarithmicUnit((Unit) reference, getBase(), id);
    }

    /**
     * Multiply this unit by another unit.
     * 
     * @param that
     *            The unit to multiply this unit by. Must be dimensionless.
     * @return The product of this unit and <code>that</code>.
     * @throws MultiplyException
     *             Can't multiply these units together.
     */
    @Override
    protected Unit myMultiplyBy(final Unit that) throws MultiplyException {
        if (!that.isDimensionless()) {
            throw new MultiplyException(that);
        }
        return that instanceof ScaledUnit
                ? new ScaledUnit(((ScaledUnit) that).getScale(), this)
                : this;
    }

    /**
     * Divide this unit by another unit.
     * 
     * @param that
     *            The unit to divide this unit by.
     * @return The quotient of this unit and <code>that</code>.
     * @throws DivideException
     *             Can't divide these units.
     */
    @Override
    protected Unit myDivideBy(final Unit that) throws DivideException {
        if (!that.isDimensionless()) {
            throw new DivideException(that);
        }
        return that instanceof ScaledUnit
                ? new ScaledUnit(1.0 / ((ScaledUnit) that).getScale(), this)
                : this;
    }

    /**
     * Divide this unit into another unit.
     * 
     * @param that
     *            The unit to divide this unit into.
     * @return The quotient of <code>that</code> unit and this unit.
     * @throws DivideException
     *             Can't divide these units.
     */
    @Override
    protected Unit myDivideInto(final Unit that) throws OperationException {
        throw new DivideException(that);
    }

    /**
     * Raise this unit to a power.
     * 
     * @param power
     *            The power to raise this unit by. The only meaningful values
     *            are 0 and 1.
     * @return The result of raising this unit by the power <code>power</code>.
     * @throws RaiseException
     *             Can't raise this unit to {@code power}, which is neither 0
     *             nor 1.
     */
    @Override
    protected Unit myRaiseTo(final int power) throws RaiseException {
        if (power == 0) {
            return DerivedUnitImpl.DIMENSIONLESS;
        }
        if (power == 1) {
            return this;
        }
        throw new RaiseException(this);
    }

    /**
     * Returns the derived unit that is convertible with this unit.
     * 
     * @return The derived unit that is convertible with this unit.
     */
    public DerivedUnit getDerivedUnit() {
        return derivedUnit;
    }

    /**
     * Converts a value in this unit to the equivalent value in the convertible
     * derived unit.
     * 
     * @param amount
     *            The value in this unit.
     * @return The equivalent value in the convertible derived unit.
     * @throws ConversionException
     *             Can't convert between units.
     */
    public float toDerivedUnit(final float amount) throws ConversionException {
        return (float) toDerivedUnit((double) amount);
    }

    /**
     * Converts a value in this unit to the equivalent value in the convertible
     * derived unit.
     * 
     * @param amount
     *            The value in this unit.
     * @return The equivalent value in the convertible derived unit.
     * @throws ConversionException
     *             Can't convert between units.
     */
    public double toDerivedUnit(final double amount) throws ConversionException {
        return reference.toDerivedUnit(Math.exp(amount * lnBase));
    }

    /**
     * Converts values in this unit to the equivalent values in the convertible
     * derived unit.
     * 
     * @param input
     *            The values in this unit.
     * @param output
     *            The equivalent values in the convertible derived unit. May be
     *            the same array as <code>input</code>.
     * @return <code>output</code>.
     * @throws ConversionException
     *             Can't convert between units.
     */
    public float[] toDerivedUnit(final float[] input, final float[] output)
            throws ConversionException {
        for (int i = input.length; --i >= 0;) {
            output[i] = (float) (Math.exp(input[i] * lnBase));
        }
        return reference.toDerivedUnit(output, output);
    }

    /**
     * Converts values in this unit to the equivalent values in the convertible
     * derived unit.
     * 
     * @param input
     *            The values in this unit.
     * @param output
     *            The equivalent values in the convertible derived unit. May be
     *            the same array as <code>input</code>.
     * @return <code>output</code>.
     * @throws ConversionException
     *             Can't convert between units.
     */
    public double[] toDerivedUnit(final double[] input, final double[] output)
            throws ConversionException {
        for (int i = input.length; --i >= 0;) {
            output[i] = Math.exp(input[i] * lnBase);
        }
        return reference.toDerivedUnit(output, output);
    }

    /**
     * Converts a value in the convertible derived unit to the equivalent value
     * in this unit.
     * 
     * @param amount
     *            The value in the convertible derived unit.
     * @return The equivalent value in this unit.
     * @throws ConversionException
     *             Can't convert between units.
     */
    public float fromDerivedUnit(final float amount) throws ConversionException {
        return (float) fromDerivedUnit((double) amount);
    }

    /**
     * Converts a value in the convertible derived unit to the equivalent value
     * in this unit.
     * 
     * @param amount
     *            The value in the convertible derived unit.
     * @return The equivalent value in this unit.
     * @throws ConversionException
     *             Can't convert between units.
     */
    public double fromDerivedUnit(final double amount)
            throws ConversionException {
        return Math.log(reference.fromDerivedUnit(amount)) / lnBase;
    }

    /**
     * Converts values in the convertible derived unit to the equivalent values
     * in this unit.
     * 
     * @param input
     *            The values in the convertible derived unit.
     * @param output
     *            The equivalent values in this unit. May be the same array as
     *            <code>input</code>.
     * @return <code>output</code>.
     * @throws ConversionException
     *             Can't convert between units.
     */
    public float[] fromDerivedUnit(final float[] input, final float[] output)
            throws ConversionException {
        reference.fromDerivedUnit(input, output);
        for (int i = input.length; --i >= 0;) {
            output[i] = (float) (Math.log(output[i]) / lnBase);
        }
        return output;
    }

    /**
     * Converts values in the convertible derived unit to the equivalent values
     * in this unit.
     * 
     * @param input
     *            The values in the convertible derived unit.
     * @param output
     *            The equivalent values in this unit. May be the same array as
     *            <code>input</code>.
     * @return <code>output</code>.
     * @throws ConversionException
     *             Can't convert between units.
     */
    public double[] fromDerivedUnit(final double[] input, final double[] output)
            throws ConversionException {
        reference.fromDerivedUnit(input, output);
        for (int i = input.length; --i >= 0;) {
            output[i] = (float) (Math.log(output[i]) / lnBase);
        }
        return output;
    }

    /**
     * Indicates if this unit is semantically identical to an object.
     * 
     * @param object
     *            The object.
     * @return <code>true</code> if and only if this unit is semantically
     *         identical to <code>object
     *				</code>.
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof LogarithmicUnit)) {
            return false;
        }
        final LogarithmicUnit that = (LogarithmicUnit) object;
        return base == that.base && reference.equals(that.reference);
    }

    /**
     * Returns the hash code of this instance.
     * 
     * @return The hash code of this instance.
     */
    @Override
    public int hashCode() {
        return Double.valueOf(base).hashCode() ^ getReference().hashCode();
    }

    /**
     * Indicates if this unit is dimensionless.
     * 
     * @return <code>true</code>, always.
     */
    public boolean isDimensionless() {
        return true;
    }

    /**
     * Returns the string representation of this unit.
     * 
     * @return The string representation of this unit.
     */
    @Override
    public String toString() {
        final String string = super.toString(); // get symbol or name
        return string != null
                ? string
                : getCanonicalString();
    }

    /**
     * Returns the canonical string representation of the unit.
     * 
     * @return The canonical string representation.
     */
    public String getCanonicalString() {
        return (base == 2
                ? "lb"
                : base == Math.E
                        ? "ln"
                        : "lg") + "(re " + getReference().toString() + ")";
    }

}
