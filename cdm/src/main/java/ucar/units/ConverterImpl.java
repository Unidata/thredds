// $Id: ConverterImpl.java 64 2006-07-12 22:30:50Z edavis $
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
 * Provides support for implementations of unit converters.  A unit converter
 * class may be created by subclassing this class and implementing the methods
 * <code>convert(double)</code>, <code>convert(float[] input, float[] output)
 * </code>, and <code>convert(double[] input, double[] output)</code> of
 * interface <code>Converter</code>.
 * 
 * @author Steven R. Emmerson
 * @version $Id: ConverterImpl.java 64 2006-07-12 22:30:50Z edavis $
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
