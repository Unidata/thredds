/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import javax.annotation.concurrent.Immutable;

/**
 * Provides support for units that are offset from reference units (ex: as the
 * unit "degree Celsius" is offset from the reference unit "kelvin"). Instances
 * of this class are immutable.
 * 
 * @author Steven R. Emmerson
 */
@Immutable
public final class OffsetUnit extends UnitImpl implements DerivableUnit {
    private static final long serialVersionUID = 1L;

    /**
     * The origin of this unit in terms of the reference unit.
     * 
     * @serial
     */
    private final double      _offset;

    /**
     * The reference unit.
     * 
     * @serial
     */
    private final Unit        _unit;

    /**
     * The derived unit that is convertible with this unit.
     * 
     * @serial
     */
    private final DerivedUnit       _derivedUnit;

    /**
     * Constructs from a reference unit and an offset.
     * 
     * @param unit
     *            The reference unit.
     * @param offset
     *            The origin of this unit in terms of the reference unit. For
     *            example, a degree Celsius unit would be created as "<code>new
     * 				OffsetUnit(kelvin, 273.15)</code>.
     */
    public OffsetUnit(final Unit unit, final double offset) {
        this(unit, offset, null);
    }

    /**
     * Constructs from a reference unit, and offset, and a unit identifier.
     * 
     * @param unit
     *            The reference unit.
     * @param offset
     *            The origin of this unit in terms of the reference unit. For
     *            example, a degree Celsius unit would be created as "<code>new
     * 				OffsetUnit(kelvin, 273.15)</code>.
     * @param id
     *            The identifier for the new unit.
     */
    public OffsetUnit(final Unit unit, final double offset, final UnitName id) {
        super(id);
        if (!(unit instanceof OffsetUnit)) {
            _unit = unit;
            _offset = offset;
        }
        else {
            _unit = ((OffsetUnit) unit)._unit;
            _offset = ((OffsetUnit) unit)._offset + offset;
        }
      _derivedUnit = _unit.getDerivedUnit();
    }

    static Unit getInstance(final Unit unit, final double origin) {
        return (origin == 0)
                ? unit
                : new OffsetUnit(unit, origin);
    }

    /**
     * Returns the reference unit.
     * 
     * @return The reference unit.
     */
    public Unit getUnit() {
        return _unit;
    }

    /**
     * Returns the offset. The offset is the location of the origin of this unit
     * in terms of the reference unit.
     * 
     * @return The origin of this unit in terms of the reference unit.
     */
    public double getOffset() {
        return _offset;
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
        return new OffsetUnit(getUnit(), getOffset(), id);
    }

    @Override
    public Unit multiplyBy(final double scale) throws MultiplyException {
        if (scale == 0) {
            throw new MultiplyException(scale, this);
        }
        return getInstance(_unit.multiplyBy(scale), _offset / scale);
    }

    @Override
    public Unit shiftTo(final double origin) {
        return getInstance(_unit, origin + _offset);
    }

    /**
     * Multiply this unit by another unit.
     * 
     * @param that
     *            The unit to multiply this unit by.
     * @return The product of this unit and <code>that</code>. The offset of
     *         this unit will be ignored; thus, for example
     *         "celsius.myMultiplyBy(day)" is equivalent to
     *         "kelvin.myMultiplyBy(day)".
     * @throws MultiplyException
     *             Can't multiply these units together.
     */
    @Override
    protected Unit myMultiplyBy(final Unit that) throws MultiplyException {
        return that instanceof OffsetUnit
                ? getUnit().multiplyBy(((OffsetUnit) that).getUnit())
                : getUnit().multiplyBy(that);
    }

    /**
     * Divide this unit by another unit.
     * 
     * @param that
     *            The unit to divide this unit by.
     * @return The quotient of this unit and <code>that</code>. The offset of
     *         this unit will be ignored; thus, for example
     *         "celsius.myDivideBy(day)" is equivalent to
     *         "kelvin.myDivideBy(day)".
     * @throws OperationException
     *             Can't divide these units.
     */
    @Override
    protected Unit myDivideBy(final Unit that) throws OperationException {
        return that instanceof OffsetUnit
                ? getUnit().divideBy(((OffsetUnit) that).getUnit())
                : getUnit().divideBy(that);
    }

    /**
     * Divide this unit into another unit.
     * 
     * @param that
     *            The unit to divide this unit into.
     * @return The quotient of <code>that</code> unit and this unit. The offset
     *         of this unit will be ignored; thus, for example
     *         "celsius.myDivideInto(day)" is equivalent to
     *         "kelvin.myDivideInto(day)".
     * @throws OperationException
     *             Can't divide these units.
     */
    @Override
    protected Unit myDivideInto(final Unit that) throws OperationException {
        return that instanceof OffsetUnit
                ? getUnit().divideInto(((OffsetUnit) that).getUnit())
                : getUnit().divideInto(that);
    }

    /**
     * Raise this unit to a power.
     * 
     * @param power
     *            The power to raise this unit by.
     * @return The result of raising this unit by the power <code>power</code>.
     *         The offset of this unit will be ignored; thus, for example
     *         "celsius.myRaiseTo(2)" is equivalent to "kelvin.myRaiseTo(2)".
     * @throws RaiseException
     *             Can't raise this unit to a power.
     */
    // Ignore offset (e.g. "Cel2" == "K2")
    @Override
    protected Unit myRaiseTo(final int power) throws RaiseException {
        return getUnit().raiseTo(power);
    }

    /**
     * Returns the derived unit that is convertible with this unit.
     * 
     * @return The derived unit that is convertible with this unit.
     */
    public DerivedUnit getDerivedUnit() {
        return _derivedUnit;
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
        if (!(_unit instanceof DerivableUnit)) {
            throw new ConversionException(this, getDerivedUnit());
        }
        return ((DerivableUnit) getUnit()).toDerivedUnit(amount + getOffset());
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
        if (!(_unit instanceof DerivableUnit)) {
            throw new ConversionException(this, getDerivedUnit());
        }
        final float origin = (float) getOffset();
        for (int i = input.length; --i >= 0;) {
            output[i] = input[i] + origin;
        }
        return ((DerivableUnit) getUnit()).toDerivedUnit(output, output);
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
        if (!(_unit instanceof DerivableUnit)) {
            throw new ConversionException(this, getDerivedUnit());
        }
        final double origin = getOffset();
        for (int i = input.length; --i >= 0;) {
            output[i] = input[i] + origin;
        }
        return ((DerivableUnit) getUnit()).toDerivedUnit(output, output);
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
        if (!(_unit instanceof DerivableUnit)) {
            throw new ConversionException(getDerivedUnit(), this);
        }
        return ((DerivableUnit) getUnit()).fromDerivedUnit(amount)
                - getOffset();
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
        if (!(_unit instanceof DerivableUnit)) {
            throw new ConversionException(getDerivedUnit(), this);
        }
        ((DerivableUnit) getUnit()).fromDerivedUnit(input, output);
        final float origin = (float) getOffset();
        for (int i = input.length; --i >= 0;) {
            output[i] -= origin;
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
        if (!(_unit instanceof DerivableUnit)) {
            throw new ConversionException(getDerivedUnit(), this);
        }
        ((DerivableUnit) getUnit()).fromDerivedUnit(input, output);
        final double origin = getOffset();
        for (int i = input.length; --i >= 0;) {
            output[i] -= origin;
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
        if (_offset == 0) {
            return object.equals(_unit);
        }
        if (!(object instanceof OffsetUnit)) {
            return false;
        }

        OffsetUnit that = (OffsetUnit) object;
        if (Double.compare(that._offset, _offset) != 0) return false;
        return _unit.equals(that._unit);
    }

    /**
     * Returns the hash code of this instance.
     * 
     * @return The hash code of this instance.
     */
    @Override
    public int hashCode() {
        return (getOffset() == 0
                ? 0
                : Double.valueOf(getOffset()).hashCode())
                ^ getUnit().hashCode();
    }

    /**
     * Indicates if this unit is dimensionless.
     * 
     * @return <code>true</code> if and only if this unit is dimensionless.
     */
    public boolean isDimensionless() {
        return getUnit().isDimensionless();
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
        return "(" + getUnit().toString() + ") @ " + getOffset();
    }
}
