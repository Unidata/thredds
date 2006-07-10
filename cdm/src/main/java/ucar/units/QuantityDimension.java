// $Id: QuantityDimension.java,v 1.5 2000/08/18 04:17:30 russ Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */
package ucar.units;

/**
 * Provides support for the dimension of a quantity.  For example, the
 * dimension of the quantity "force" is "M.L.t-2".
 *
 * @author Steven R. Emmerson
 * @version $Id: QuantityDimension.java,v 1.5 2000/08/18 04:17:30 russ Exp $
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
