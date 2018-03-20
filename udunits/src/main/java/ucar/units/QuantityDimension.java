/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import javax.annotation.concurrent.Immutable;

/**
 * Provides support for the dimension of a quantity.  For example, the
 * dimension of the quantity "force" is "M.L.t-2".
 *
 * @author Steven R. Emmerson
 * Immutable.
 */
@Immutable
public final class
QuantityDimension
    extends	Dimension
{
    /**
     * Constructs from nothing.  Constructs a dimensionless dimension.
     */
    public
    QuantityDimension()
    {
	super();
    }

    /**
     * Constructs from a base quantity.
     * @param baseQuantity	The base quantity constituting the dimension.
     */
    public
    QuantityDimension(BaseQuantity baseQuantity)
    {
	super(new Factor(baseQuantity));
    }

    /**
     * Constructs from an array of Factor-s.  This is a trusted constructor
     * for use by subclasses only.
     * @param factors		The Factor-s constituting the dimension.
     */
    protected
    QuantityDimension(Factor[] factors)
    {
	super(factors);
    }

    /**
     * Multiplies this quantity dimension by another quantity dimension.
     * @param that		The other quantity dimension.
     * @return			The product of this quantity dimension with
     *				the other quantity dimension.
     */
    public QuantityDimension
    multiplyBy(QuantityDimension that)
    {
	return new QuantityDimension(mult(that));
    }

    /**
     * Divides this quantity dimension by another quantity dimension.
     * @param that		The quantity dimension to divide this
     *				quantity dimension by.
     * @return			The quotient of this quantity dimension and
     *				the other quantity dimension.
     */
    public QuantityDimension
    divideBy(QuantityDimension that)
    {
	return multiplyBy(that.raiseTo(-1));
    }

    /**
     * Raises this quantity dimension to a power.
     * @param power		The power to raise this quantity dimension by.
     * @return			The result of raising this quantity dimension
     *				to the power <code>power</code>.
     */
    public QuantityDimension
    raiseTo(int power)
    {
	return new QuantityDimension(pow(power));
    }

    /**
     * Tests this class.
     */
    public static void
    main(String[] args)
	throws	Exception
    {
	System.out.println("new QuantityDimension() = \"" +
	    new QuantityDimension() + '"');
	QuantityDimension	timeDimension =
	    new QuantityDimension(BaseQuantity.TIME);
	System.out.println("timeDimension = \"" + timeDimension + '"');
	QuantityDimension	lengthDimension =
	    new QuantityDimension(BaseQuantity.LENGTH);
	System.out.println("lengthDimension = \"" + lengthDimension + '"');
	System.out.println(
	    "lengthDimension.isReciprocalOf(timeDimension) = \"" +
	    lengthDimension.isReciprocalOf(timeDimension) + '"');
	QuantityDimension	hertzDimension = timeDimension.raiseTo(-1);
	System.out.println("hertzDimension = \"" + hertzDimension + '"');
	System.out.println(
	    "hertzDimension.isReciprocalOf(timeDimension) = \"" +
	    hertzDimension.isReciprocalOf(timeDimension) + '"');
	System.out.println("lengthDimension.divideBy(timeDimension) = \"" +
	    lengthDimension.divideBy(timeDimension) + '"');
	System.out.println(
	    "lengthDimension.divideBy(timeDimension).raiseTo(2) = \"" +
	    lengthDimension.divideBy(timeDimension).raiseTo(2) + '"');
    }
}
