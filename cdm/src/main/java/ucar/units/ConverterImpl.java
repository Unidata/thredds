// $Id: ConverterImpl.java,v 1.5 2000/08/18 04:17:26 russ Exp $
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
 * Provides support for implementations of unit converters.  A unit converter
 * class may be created by subclassing this class and implementing the methods
 * <code>convert(double)</code>, <code>convert(float[] input, float[] output)
 * </code>, and <code>convert(double[] input, double[] output)</code> of
 * interface <code>Converter</code>.
 * 
 * @author Steven R. Emmerson
 * @version $Id: ConverterImpl.java,v 1.5 2000/08/18 04:17:26 russ Exp $
 */
public abstract class
ConverterImpl
    implements	Converter
{
    /**
     * Constructs from a "from" unit and a "to" unit.
     * @param fromUnit		The unit from which to convert.
     * @param toUnit		The unit to which to convert.
     * @throws ConversionException	The units are not convertible.
     */
    protected
    ConverterImpl(Unit fromUnit, Unit toUnit)
	throws ConversionException
    {
	if (!fromUnit.isCompatible(toUnit))
	    throw new ConversionException(fromUnit, toUnit);
    }

    /**
     * Factory method for creating a unit converter.
     * @param fromUnit		The unit from which to convert.
     * @param toUnit            The unit to which to convert.
     * @throws ConversionException      The units are not convertible.
     */
    public static Converter
    create(Unit fromUnit, Unit toUnit)
	throws ConversionException
    {
	return fromUnit.getConverterTo(toUnit);
    }

    /**
     * Converts a numeric value.
     * @param amount		The numeric value to convert.
     * @return			The converted numeric value.
     */
    public final float
    convert(float amount)
    {
	return (float)convert((double)amount);
    }

    /**
     * Converts an array of numeric values.
     * @param amounts		The numeric values to convert.
     * @return			The converted numeric values in allocated
     *				space.
     */
    public final float[]
    convert(float[] amounts)
    {
	return convert(amounts, new float[amounts.length]);
    }

    /**
     * Converts an array of numeric values.
     * @param amounts		The numeric values to convert.
     * @return			The converted numeric values in allocated
     *				space.
     */
    public final double[]
    convert(double[] amounts)
    {
	return convert(amounts, new double[amounts.length]);
    }
}
