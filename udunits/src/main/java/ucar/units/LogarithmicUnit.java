// $Id: OffsetUnit.java 64 2006-07-12 22:30:50Z edavis $
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
 * Provides support for units that are based on a logarithm of the ratio of a
 * physical quantity to an underlying reference level.
 * 
 * Instances of this class are immutable.
 * 
 * @author Steven R. Emmerson
 * @version $Id: OffsetUnit.java 64 2006-07-12 22:30:50Z edavis $
 */
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

    /**
     * Tests this class.
     */
    public static void main(final String[] args) throws Exception {
        final BaseUnit meter = BaseUnit.getOrCreate(UnitName.newUnitName(
                "meter", null, "m"), BaseQuantity.LENGTH);
        final ScaledUnit micron = new ScaledUnit(1e-6, meter);
        final Unit cubicMicron = micron.raiseTo(3);
        final LogarithmicUnit Bz = new LogarithmicUnit(cubicMicron, 10.0);
        assert Bz.isDimensionless();
        assert Bz.equals(Bz);
        assert Bz.getReference().equals(cubicMicron);
        assert Bz.getBase() == 10.0;
        assert !Bz.equals(cubicMicron);
        assert !Bz.equals(micron);
        assert !Bz.equals(meter);
        try {
            Bz.multiplyBy(meter);
            assert false;
        }
        catch (final MultiplyException e) {
        }
        try {
            Bz.divideBy(meter);
            assert false;
        }
        catch (final DivideException e) {
        }
        try {
            Bz.raiseTo(2);
            assert false;
        }
        catch (final RaiseException e) {
        }
        double value = Bz.toDerivedUnit(0);
        assert 0.9e-18 < value && value < 1.1e-18 : value;
        value = Bz.toDerivedUnit(1);
        assert 0.9e-17 < value && value < 1.1e-17 : value;
        value = Bz.fromDerivedUnit(1e-18);
        assert -0.1 < value && value < 0.1 : value;
        value = Bz.fromDerivedUnit(1e-17);
        assert 0.9 < value && value < 1.1 : value;
        final String string = Bz.toString();
        assert string.equals("lg(re 9.999999999999999E-19 m3)") : string;
    }
}
