// $Id: QuantityDimension.java 64 2006-07-12 22:30:50Z edavis $
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
 * Provides support for the dimension of a quantity.  For example, the
 * dimension of the quantity "force" is "M.L.t-2".
 *
 * @author Steven R. Emmerson
 * @version $Id: QuantityDimension.java 64 2006-07-12 22:30:50Z edavis $
 * Immutable.
 */
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
