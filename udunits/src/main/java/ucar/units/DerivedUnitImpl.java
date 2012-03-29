// $Id: DerivedUnitImpl.java 64 2006-07-12 22:30:50Z edavis $
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
 * Provides support for a concrete implementation of derived units.
 * 
 * @author Steven R. Emmerson
 * @version $Id: DerivedUnitImpl.java 64 2006-07-12 22:30:50Z edavis $
 */
public class DerivedUnitImpl extends UnitImpl implements DerivedUnit,
        DerivableUnit {
    private static final long           serialVersionUID = 1L;

    /**
     * The dimensionless derived unit.
     */
    public static final DerivedUnitImpl DIMENSIONLESS    = new DerivedUnitImpl();

    /**
     * The dimension of this derived unit.
     * 
     * @serial
     */
    private/* final */UnitDimension     dimension;

    /**
     * Constructs a dimensionless derived unit from nothing.
     */
    protected DerivedUnitImpl() {
        // dimensionless derived unit
        this(new UnitDimension(), dimensionlessID());
    }

    /**
     * Returns the identifiers associated with the dimensionless, derived unit.
     * 
     * @return The identifiers of the dimensionless, derived unit.
     */
    private static UnitName dimensionlessID() {
        UnitName id;
        try {
            id = UnitName.newUnitName("1", "1", "1");
        }
        catch (final NameException e) {
            id = null;
        }
        return id;
    }

    /**
     * Constructs from a unit dimension. This is a trusted constructor for use
     * by subclasses only.
     * 
     * @param dimension
     *            The unit dimension.
     */
    protected DerivedUnitImpl(final UnitDimension dimension) {
        this(dimension, null);
    }

    /**
     * Constructs from identifiers. This is a trusted constructor for use by
     * subclasses only.
     * 
     * @param id
     *            The identifiers for the unit.
     */
    protected DerivedUnitImpl(final UnitName id) {
        this(null, id);
    }

    /**
     * Constructs from a unit dimension and identifiers. This is a trusted
     * constructor for use by subclasses only.
     * 
     * @param dimension
     *            The unit dimension.
     * @param id
     *            The identifiers for the unit.
     */
    protected DerivedUnitImpl(final UnitDimension dimension, final UnitName id) {
        super(id);
        this.dimension = dimension;
    }

    /**
     * Sets the unit dimension of this derived unit. This is a trusted method
     * for use by subclasses only and should be called only once immediately
     * after construction of this superinstance.
     * 
     * @param dimension
     *            The unit dimension.
     */
    protected void setDimension(final UnitDimension dimension) {
        this.dimension = dimension;
    }

    /**
     * Returns the unit dimension of this derived unit.
     * 
     * @return The unit dimension of this derived unit.
     */
    public final UnitDimension getDimension() {
        return dimension;
    }

    /**
     * Returns the quantity dimension of this derived unit.
     * 
     * @return The quantity dimension of this derived unit.
     */
    public final QuantityDimension getQuantityDimension() {
        return getDimension().getQuantityDimension();
    }

    /*
     * From DerivedUnit:
     */

    /**
     * Indicates if this derived unit is the reciprocal of another derived unit
     * (e.g. "second" and "hertz").
     * 
     * @param that
     *            The other, derived unit.
     */
    public final boolean isReciprocalOf(final DerivedUnit that) {
        return dimension.isReciprocalOf(that.getDimension());
    }

    /*
     * From UnitImpl:
     */

    /**
     * Returns the derived unit that is convertible with this unit. Obviously,
     * the method returns this derived unit.
     * 
     * @return <code>this</code>.
     */
    public final DerivedUnit getDerivedUnit() {
        return this;
    }

    /**
     * Clones the derived unit changing the identifiers.
     * 
     * @param id
     *            The identifiers for the new unit.
     * @return The new unit.
     */
    public final Unit clone(final UnitName id) {
        return new DerivedUnitImpl(dimension, id);
    }

    /**
     * Multiplies this derived unit by another.
     * 
     * @param that
     *            The other unit.
     * @return The product of the two units.
     * @throws MultiplyException
     *             Can't multiply these units.
     */
    @Override
    protected Unit myMultiplyBy(final Unit that) throws MultiplyException {
        Unit result;
        if (dimension.getRank() == 0) {
            result = that;
        }
        else {
            if (!(that instanceof DerivedUnit)) {
                result = that.multiplyBy(this);
            }
            else {
                final UnitDimension thatDimension = ((DerivedUnit) that)
                        .getDimension();
                result = thatDimension.getRank() == 0
                        ? this
                        : new DerivedUnitImpl(dimension
                                .multiplyBy(thatDimension));
            }
        }
        return result;
    }

    /**
     * Divides this derived unit by another.
     * 
     * @param that
     *            The other unit.
     * @return The quotient of the two units.
     * @throws OperationException
     *             Can't divide these units.
     */
    @Override
    protected Unit myDivideBy(final Unit that) throws OperationException {
        Unit result;
        if (dimension.getRank() == 0) {
            result = that.raiseTo(-1);
        }
        else {
            if (!(that instanceof DerivedUnit)) {
                result = that.divideInto(this);
            }
            else {
                final UnitDimension thatDimension = ((DerivedUnit) that)
                        .getDimension();
                result = thatDimension.getRank() == 0
                        ? this
                        : new DerivedUnitImpl(dimension.divideBy(thatDimension));
            }
        }
        return result;
    }

    /**
     * Divides this derived unit into another.
     * 
     * @param that
     *            The other unit.
     * @return The quotient of the two units.
     * @throws OperationException
     *             Can't divide these units.
     */
    @Override
    protected Unit myDivideInto(final Unit that) throws OperationException {
        return that.divideBy(this);
    }

    /**
     * Raises this derived unit to a power.
     * 
     * @param power
     *            The power.
     * @return This derived unit raised to the given power.
     */
    @Override
    protected Unit myRaiseTo(final int power) {
        return power == 1
                ? this
                : new DerivedUnitImpl(dimension.raiseTo(power));
    }

    /**
     * Converts a numerical value from this unit to the derived unit. Obviously,
     * the numerical value is unchanged.
     * 
     * @param amount
     *            The numerical values in this unit.
     * @return The numerical value in the derived unit.
     */
    public final float toDerivedUnit(final float amount) {
        return amount;
    }

    /**
     * Converts a numerical value from this unit to the derived unit. Obviously,
     * the numerical value is unchanged.
     * 
     * @param amount
     *            The numerical values in this unit.
     * @return The numerical value in the derived unit.
     */
    public final double toDerivedUnit(final double amount) {
        return amount;
    }

    /**
     * Converts numerical values from this unit to the derived unit. Obviously,
     * the numerical values are unchanged.
     * 
     * @param input
     *            The numerical values in this unit.
     * @param output
     *            The numerical values in the derived unit. May be the same
     *            array as <code>input</code>.
     * @return <code>output</code>.
     */
    public final float[] toDerivedUnit(final float[] input, final float[] output) {
        if (input != output) {
            System.arraycopy(input, 0, output, 0, input.length);
        }
        return output;
    }

    /**
     * Converts numerical values from this unit to the derived unit. Obviously,
     * the numerical values are unchanged.
     * 
     * @param input
     *            The numerical values in this unit.
     * @param output
     *            The numerical values in the derived unit. May be the same
     *            array as <code>input</code>.
     * @return <code>output</code>.
     */
    public final double[] toDerivedUnit(final double[] input,
            final double[] output) {
        if (input != output) {
            System.arraycopy(input, 0, output, 0, input.length);
        }
        return output;
    }

    /**
     * Converts a numerical value to this unit from the derived unit. Obviously,
     * the numerical value is unchanged.
     * 
     * @param amount
     *            The numerical values in the derived unit.
     * @return The numerical value in this unit.
     */
    public final float fromDerivedUnit(final float amount) {
        return amount;
    }

    /**
     * Converts a numerical value to this unit from the derived unit. Obviously,
     * the numerical value is unchanged.
     * 
     * @param amount
     *            The numerical values in the derived unit.
     * @return The numerical value in this unit.
     */
    public final double fromDerivedUnit(final double amount) {
        return amount;
    }

    /**
     * Converts numerical values to this unit from the derived unit. Obviously,
     * the numerical values are unchanged.
     * 
     * @param input
     *            The numerical values in the derived unit.
     * @param output
     *            The numerical values in this unit. May be the same array as
     *            <code>input</code>.
     * @return <code>output</code>.
     */
    public final float[] fromDerivedUnit(final float[] input,
            final float[] output) {
        return toDerivedUnit(input, output);
    }

    /**
     * Converts numerical values to this unit from the derived unit. Obviously,
     * the numerical values are unchanged.
     * 
     * @param input
     *            The numerical values in the derived unit.
     * @param output
     *            The numerical values in this unit. May be the same array as
     *            <code>input</code>.
     * @return <code>output</code>.
     */
    public final double[] fromDerivedUnit(final double[] input,
            final double[] output) {
        return toDerivedUnit(input, output);
    }

    /**
     * Indicates if values in this unit are convertible with another unit.
     * 
     * @param that
     *            The other unit.
     * @return <code>true</code> if and only if values in this unit are
     *         convertible to values in <code>
     *				that</code>.
     */
    @Override
    public final boolean isCompatible(final Unit that) {
        final DerivedUnit unit = that.getDerivedUnit();
        return equals(unit) || isReciprocalOf(unit);
    }

    /**
     * Indicates if this derived unit is semantically identical to an object.
     * 
     * @param object
     *            The object
     * @return <code>true</code> if and only if this derived unit is
     *         semantically identical to <code>
     *				object</code>.
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DerivedUnit)) {
            return false;
        }
        final DerivedUnit that = (DerivedUnit) object;
        return (this instanceof BaseUnit && that instanceof BaseUnit)
                ? false
                : dimension.equals(that.getDimension());
    }

    /**
     * Returns the hash code of this instance.
     * 
     * @return The hash code of this instance.
     */
    @Override
    public int hashCode() {
        return this instanceof BaseUnit
                ? System.identityHashCode(this)
                : dimension.hashCode();
    }

    /**
     * Indicates if this derived unit is dimensionless.
     * 
     * @return <code>true</code> if and only if this derived unit is
     *         dimensionless.
     */
    public boolean isDimensionless() {
        return dimension.isDimensionless();
    }

    /**
     * Returns a string representation of this unit. If the symbol or name is
     * available, then that is returned; otherwise, the corresponding expression
     * in base units is returned.
     * 
     * @return The string expression for this derived unit.
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
        return dimension.toString();
    }

    /**
     * Tests this class.
     */
    public static void main(final String[] args) throws Exception {
        final BaseUnit second = BaseUnit.getOrCreate(UnitName.newUnitName(
                "second", null, "s"), BaseQuantity.TIME);
        System.out.println("second = \"" + second + '"');
        final BaseUnit meter = BaseUnit.getOrCreate(UnitName.newUnitName(
                "meter", null, "m"), BaseQuantity.LENGTH);
        System.out.println("meter = \"" + meter + '"');
        final DerivedUnitImpl meterSecond = (DerivedUnitImpl) meter
                .myMultiplyBy(second);
        System.out.println("meterSecond = \"" + meterSecond + '"');
        final DerivedUnitImpl meterPerSecond = (DerivedUnitImpl) meter
                .myDivideBy(second);
        System.out.println("meterPerSecond = \"" + meterPerSecond + '"');
        final DerivedUnitImpl secondPerMeter = (DerivedUnitImpl) second
                .myDivideBy(meter);
        System.out.println("secondPerMeter = \"" + secondPerMeter + '"');
        System.out.println("meterPerSecond.isReciprocalOf(secondPerMeter)="
                + meterPerSecond.isReciprocalOf(secondPerMeter));
        System.out.println("meter.toDerivedUnit(1.0)="
                + meter.toDerivedUnit(1.0));
        System.out
                .println("meter.toDerivedUnit(new double[] {1,2,3}, new double[3])[1]="
                        + meter.toDerivedUnit(new double[] { 1, 2, 3 },
                                new double[3])[1]);
        System.out.println("meter.fromDerivedUnit(1.0)="
                + meter.fromDerivedUnit(1.0));
        System.out
                .println("meter.fromDerivedUnit(new double[] {1,2,3}, new double[3])[2]="
                        + meter.fromDerivedUnit(new double[] { 1, 2, 3 },
                                new double[3])[2]);
        System.out.println("meter.isCompatible(meter)="
                + meter.isCompatible(meter));
        System.out.println("meter.isCompatible(second)="
                + meter.isCompatible(second));
        System.out.println("meter.equals(meter)=" + meter.equals(meter));
        System.out.println("meter.equals(second)=" + meter.equals(second));
        System.out
                .println("meter.isDimensionless()=" + meter.isDimensionless());
        final Unit sPerS = second.myDivideBy(second);
        System.out.println("sPerS = \"" + sPerS + '"');
        System.out
                .println("sPerS.isDimensionless()=" + sPerS.isDimensionless());
        meterPerSecond.raiseTo(2);
        meter.myDivideBy(meterPerSecond);
    }
}
