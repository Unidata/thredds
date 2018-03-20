/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import javax.annotation.concurrent.Immutable;

/**
 * Provides support for a unit that is a mutiplicative factor of a reference
 * unit.
 * 
 * Instances of this class are immutable.
 * 
 * @author Steven R. Emmerson
 */
@Immutable
public final class ScaledUnit extends UnitImpl implements DerivableUnit {
    private static final long serialVersionUID = 1L;

    /**
     * The multiplicative factor.
     * 
     * @serial
     */
    private final double      _scale;

    /**
     * The reference unit.
     * 
     * @serial
     */
    private final Unit        _unit;

    /**
     * Constructs from a multiplicative factor. Returns a dimensionless unit
     * whose value is the multiplicative factor rather than unity.
     * 
     * @param scale
     *            The multiplicative factor.
     */
    public ScaledUnit(final double scale) {
        this(scale, DerivedUnitImpl.DIMENSIONLESS);
    }

    /**
     * Constructs from a multiplicative factor and a reference unit.
     * 
     * @param scale
     *            The multiplicative factor.
     * @param unit
     *            The reference unit.
     */
    public ScaledUnit(final double scale, final Unit unit) {
        this(scale, unit, null);
    }

    /**
     * Constructs from a multiplicative factor, a reference unit, and an
     * identifier.
     * 
     * @param scale
     *            The multiplicative factor.
     * @param unit
     *            The reference unit.
     * @param id
     *            The identifier for the unit.
     */
    public ScaledUnit(final double scale, final Unit unit, final UnitName id) {
        super(id);
        if (!(unit instanceof ScaledUnit)) {
            _unit = unit;
            _scale = scale;
        }
        else {
            _unit = ((ScaledUnit) unit)._unit;
            _scale = ((ScaledUnit) unit)._scale * scale;
        }
    }

    static Unit getInstance(final double scale, final Unit unit)
            throws MultiplyException {
        if (scale == 0) {
            throw new MultiplyException(scale, unit);
        }
        return scale == 1
                ? unit
                : new ScaledUnit(scale, unit);
    }

    /**
     * Returns the multiplicative factor.
     * 
     * @return The multiplicative factor.
     */
    public double getScale() {
        return _scale;
    }

    /**
     * Returns the reference unit.
     * 
     * @return The reference unit.
     */
    public Unit getUnit() {
        return _unit;
    }

    /*
     * From UnitImpl:
     */

    /**
     * Clones this unit, changing the identifier.
     * 
     * @param id
     *            The new identifier.
     * @return A ScaledUnit with the new identifier.
     */
    public Unit clone(final UnitName id) {
        return new ScaledUnit(_scale, getUnit(), id);
    }

    @Override
    public Unit multiplyBy(final double scale) throws MultiplyException {
        return getInstance(scale * _scale, _unit);
    }

    /**
     * Multiplies this unit by another unit.
     * 
     * @param that
     *            The other unit.
     * @return The product of this unit and the other unit.
     * @throws MultiplyException
     *             Can't multiply these units together.
     */
    @Override
    protected Unit myMultiplyBy(final Unit that) throws MultiplyException {
        return that instanceof ScaledUnit
                ? new ScaledUnit(getScale() * ((ScaledUnit) that).getScale(),
                        getUnit().multiplyBy(((ScaledUnit) that).getUnit()))
                : new ScaledUnit(getScale(), getUnit().multiplyBy(that));
    }

    /**
     * Divides this unit by another unit.
     * 
     * @param that
     *            The other unit.
     * @return The quotient of this unit divided by the other unit.
     * @throws OperationException
     *             Can't divide these units.
     */
    @Override
    protected Unit myDivideBy(final Unit that) throws OperationException {
        return that instanceof ScaledUnit
                ? new ScaledUnit(getScale() / ((ScaledUnit) that).getScale(),
                        getUnit().divideBy(((ScaledUnit) that).getUnit()))
                : new ScaledUnit(getScale(), getUnit().divideBy(that));
    }

    /**
     * Divides this unit into another unit.
     * 
     * @param that
     *            The other unit.
     * @return The quotient of this unit divided into the other unit.
     * @throws OperationException
     *             Can't divide these units.
     */
    @Override
    protected Unit myDivideInto(final Unit that) throws OperationException {
        return that instanceof ScaledUnit
                ? new ScaledUnit(((ScaledUnit) that).getScale() / getScale(),
                        getUnit().divideInto(((ScaledUnit) that).getUnit()))
                : new ScaledUnit(1 / getScale(), getUnit().divideInto(that));
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
    @Override
    protected Unit myRaiseTo(final int power) throws RaiseException {
        return new ScaledUnit(Math.pow(getScale(), power), getUnit().raiseTo(
                power));
    }

    /**
     * Gets the derived unit underlying this unit.
     * 
     * @return The derived unit which underlies this unit.
     */
    public DerivedUnit getDerivedUnit() {
        return getUnit().getDerivedUnit();
    }

    /**
     * Converts a numeric value from this unit to the underlying derived unit.
     * 
     * @param amount
     *            The numeric value in this unit.
     * @return The equivalent value in the underlying derived unit.
     * @throws ConversionException
     *             Can't convert value to the underlying derived unit.
     */
    public float toDerivedUnit(final float amount) throws ConversionException {
        return (float) toDerivedUnit((double) amount);
    }

    /**
     * Converts a numeric value from this unit to the underlying derived unit.
     * 
     * @param amount
     *            The numeric value in this unit.
     * @return The equivalent value in the underlying derived unit.
     * @throws ConversionException
     *             Can't convert value to the underlying derived unit.
     */
    public double toDerivedUnit(final double amount) throws ConversionException {
        if (!(_unit instanceof DerivableUnit)) {
            throw new ConversionException(this, getDerivedUnit());
        }
        return ((DerivableUnit) _unit).toDerivedUnit(amount * getScale());
    }

    /**
     * Converts numeric values from this unit to the underlying derived unit.
     * 
     * @param input
     *            The numeric values in this unit.
     * @param output
     *            The equivalent values in the underlying derived unit.
     * @return <code>output</code>.
     * @throws ConversionException
     *             Can't convert values to the underlying derived unit.
     */
    public float[] toDerivedUnit(final float[] input, final float[] output)
            throws ConversionException {
        final float scale = (float) getScale();
        for (int i = input.length; --i >= 0;) {
            output[i] = input[i] * scale;
        }
        if (!(_unit instanceof DerivableUnit)) {
            throw new ConversionException(this, getDerivedUnit());
        }
        return ((DerivableUnit) getUnit()).toDerivedUnit(output, output);
    }

    /**
     * Converts numeric values from this unit to the underlying derived unit.
     * 
     * @param input
     *            The numeric values in this unit.
     * @param output
     *            The equivalent values in the underlying derived unit.
     * @return <code>output</code>.
     * @throws ConversionException
     *             Can't convert values to the underlying derived unit.
     */
    public double[] toDerivedUnit(final double[] input, final double[] output)
            throws ConversionException {
        final double scale = getScale();
        for (int i = input.length; --i >= 0;) {
            output[i] = input[i] * scale;
        }
        if (!(_unit instanceof DerivableUnit)) {
            throw new ConversionException(this, getDerivedUnit());
        }
        return ((DerivableUnit) getUnit()).toDerivedUnit(output, output);
    }

    /**
     * Converts a numeric value from the underlying derived unit to this unit.
     * 
     * @param amount
     *            The numeric value in the underlying derived unit.
     * @return The equivalent value in this unit.
     * @throws ConversionException
     *             Can't convert value.
     */
    public float fromDerivedUnit(final float amount) throws ConversionException {
        return (float) fromDerivedUnit((double) amount);
    }

    /**
     * Converts a numeric value from the underlying derived unit to this unit.
     * 
     * @param amount
     *            The numeric value in the underlying derived unit.
     * @return The equivalent value in this unit.
     * @throws ConversionException
     *             Can't convert value.
     */
    public double fromDerivedUnit(final double amount)
            throws ConversionException {
        if (!(_unit instanceof DerivableUnit)) {
            throw new ConversionException(getDerivedUnit(), this);
        }
        return ((DerivableUnit) getUnit()).fromDerivedUnit(amount) / getScale();
    }

    /**
     * Converts numeric values from the underlying derived unit to this unit.
     * 
     * @param input
     *            The numeric values in the underlying derived unit.
     * @param output
     *            The equivalent values in this unit.
     * @return <code>output</code>.
     * @throws ConversionException
     *             Can't convert values.
     */
    public float[] fromDerivedUnit(final float[] input, final float[] output)
            throws ConversionException {
        if (!(_unit instanceof DerivableUnit)) {
            throw new ConversionException(getDerivedUnit(), this);
        }
        ((DerivableUnit) getUnit()).fromDerivedUnit(input, output);
        final float scale = (float) getScale();
        for (int i = input.length; --i >= 0;) {
            output[i] /= scale;
        }
        return output;
    }

    /**
     * Converts numeric values from the underlying derived unit to this unit.
     * 
     * @param input
     *            The numeric values in the underlying derived unit.
     * @param output
     *            The equivalent values in this unit.
     * @return <code>output</code>.
     * @throws ConversionException
     *             Can't convert values.
     */
    public double[] fromDerivedUnit(final double[] input, final double[] output)
            throws ConversionException {
        if (!(_unit instanceof DerivableUnit)) {
            throw new ConversionException(getDerivedUnit(), this);
        }
        ((DerivableUnit) getUnit()).fromDerivedUnit(input, output);
        final double scale = getScale();
        for (int i = input.length; --i >= 0;) {
            output[i] /= scale;
        }
        return output;
    }

    /**
     * Indicates if this unit is semantically identical to an object.
     * 
     * @param object
     *            The object.
     * @return <code>true</code> if an only if this unit is semantically
     *         identical to <code>object
     *				</code>.
     */
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (_scale == 1) {
            return object.equals(_unit);
        }
        if (!(object instanceof ScaledUnit)) {
            return false;
        }
        ScaledUnit that = (ScaledUnit) object;
        if (Double.compare(that._scale, _scale) != 0) return false;
        if (!_unit.equals(that._unit)) return false;
        return true;
    }

  /**
     * Returns the hash code of this instance.
     * 
     * @return The hash code of this instance.
     */
    @Override
    public int hashCode() {
        return (getScale() == 1
                ? 0
                : Double.valueOf(getScale()).hashCode()) ^ getUnit().hashCode();
    }

    /**
     * Indicates if this unit is dimensionless. A ScaledUnit is dimensionless if
     * and only if the reference unit is dimensionless.
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
        return DerivedUnitImpl.DIMENSIONLESS.equals(_unit)
                ? Double.toString(getScale())
                : Double.toString(getScale()) + " " + _unit.toString();
    }

    /**
     * Tests this class.
     */
    public static void main(final String[] args) throws Exception {
        final BaseUnit meter = BaseUnit.getOrCreate(UnitName.newUnitName(
                "meter", null, "m"), BaseQuantity.LENGTH);
        final ScaledUnit nauticalMile = new ScaledUnit(1852f, meter);
        System.out.println("nauticalMile.getUnit().equals(meter)="
                + nauticalMile.getUnit().equals(meter));
        final ScaledUnit nauticalMileMeter = (ScaledUnit) nauticalMile
                .multiplyBy(meter);
        System.out.println("nauticalMileMeter.divideBy(nauticalMile)="
                + nauticalMileMeter.divideBy(nauticalMile));
        System.out.println("meter.divideBy(nauticalMile)="
                + meter.divideBy(nauticalMile));
        System.out
                .println("nauticalMile.raiseTo(2)=" + nauticalMile.raiseTo(2));
        System.out.println("nauticalMile.toDerivedUnit(1.)="
                + nauticalMile.toDerivedUnit(1.));
        System.out
                .println("nauticalMile.toDerivedUnit(new float[]{1,2,3}, new float[3])[1]="
                        + nauticalMile.toDerivedUnit(new float[] { 1, 2, 3 },
                                new float[3])[1]);
        System.out.println("nauticalMile.fromDerivedUnit(1852.)="
                + nauticalMile.fromDerivedUnit(1852.));
        System.out
                .println("nauticalMile.fromDerivedUnit(new float[]{1852},new float[1])[0]="
                        + nauticalMile.fromDerivedUnit(new float[] { 1852 },
                                new float[1])[0]);
        System.out.println("nauticalMile.equals(nauticalMile)="
                + nauticalMile.equals(nauticalMile));
        final ScaledUnit nautical2Mile = new ScaledUnit(2, nauticalMile);
        System.out.println("nauticalMile.equals(nautical2Mile)="
                + nauticalMile.equals(nautical2Mile));
        System.out.println("nauticalMile.isDimensionless()="
                + nauticalMile.isDimensionless());
        final BaseUnit radian = BaseUnit.getOrCreate(UnitName.newUnitName(
                "radian", null, "rad"), BaseQuantity.PLANE_ANGLE);
        final ScaledUnit degree = new ScaledUnit(3.14159 / 180, radian);
        System.out.println("degree.isDimensionless()="
                + degree.isDimensionless());
    }
}
