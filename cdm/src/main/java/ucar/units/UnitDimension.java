// $Id: UnitDimension.java 64 2006-07-12 22:30:50Z edavis $
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

/*
 * Provides support for the concept of the dimension of a unit.  The
 * dimension of a unit is like the dimension of a quantity except that
 * the individual quantity dimensions have been replaced with the
 * corresponding base unit.  For example, the dimension of the quantity
 * "force" is "M.L.t-2" and the dimension of the corresponding SI unit
 * (the "newton") is "kg.m.s-2".
 *
 * Instances of this class are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: UnitDimension.java 64 2006-07-12 22:30:50Z edavis $
 */
public final class
UnitDimension
    extends	Dimension
{
    /**
     * Constructs a dimensionless unit dimension.
     */
    public
    UnitDimension()
    {
	super();
    }

    /**
     * Constructs the unit dimension corresponding to a base unit.
     * @param baseUnit		A base unit.
     */
    public
    UnitDimension(BaseUnit baseUnit)
    {
	super(new Factor(baseUnit));
    }

    /**
     * Constructs a unit dimension comprised of the given factors.
     * @param factors		The factors that constitute the unit dimension.
     */
    private
    UnitDimension(Factor[] factors)
    {
	super(factors);
    }

    /**
     * Multiplies this unit dimension by another.
     * @param that		The other unit dimension.
     * @return			The product of this unit dimension multiplied
     *				by the other.
     */
    public UnitDimension
    multiplyBy(UnitDimension that)
    {
	return new UnitDimension(mult(that));
    }

    /**
     * Divides this unit dimension by another.
     * @param that		The other unit dimension.
     * @return			The quotient of this unit dimension divided
     *				by the other.
     */
    public UnitDimension
    divideBy(UnitDimension that)
    {
	return multiplyBy(that.raiseTo(-1));
    }

    /**
     * Raises this unit dimension to a power.
     * @param power		The power.
     * @return			The result of raising this unit dimension
     *				to the power.
     */
    public UnitDimension
    raiseTo(int power)
    {
	return new UnitDimension(pow(power));
    }

    /**
     * Returns the corresponding quantity dimension.
     * @return			The quantity dimension corresponding to this
     *				unit dimension.
     */
    public QuantityDimension
    getQuantityDimension()
    {
	Factor[]	factors = getFactors();
	for (int i = factors.length; --i >= 0; )
	{
	    Factor	factor = factors[i];
	    factors[i] =
		new Factor(
		    ((BaseUnit)factor.getBase()).getBaseQuantity(), 
		    factor.getExponent());
	}
	return new QuantityDimension(factors);
    }

    /**
     * Tests this class.
     */
    public static void
    main(String[] args)
	throws	Exception
    {
	System.out.println("new UnitDimension() = \"" +
	    new UnitDimension() + '"');
	UnitDimension	timeDimension =
	    new UnitDimension(
		BaseUnit.getOrCreate(
		    UnitName.newUnitName("second", null, "s"),
		    BaseQuantity.TIME));
	System.out.println("timeDimension = \"" + timeDimension + '"');
	UnitDimension	lengthDimension =
	    new UnitDimension(
		BaseUnit.getOrCreate(
		    UnitName.newUnitName("meter", null, "m"),
		    BaseQuantity.LENGTH));
	System.out.println("lengthDimension = \"" + lengthDimension + '"');
	System.out.println(
	    "lengthDimension.isReciprocalOf(timeDimension) = \"" +
	    lengthDimension.isReciprocalOf(timeDimension) + '"');
	UnitDimension	hertzDimension = timeDimension.raiseTo(-1);
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
