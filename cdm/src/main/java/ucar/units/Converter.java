// $Id: Converter.java 64 2006-07-12 22:30:50Z edavis $
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
 * Interface for converting numeric values from one unit to another.
 * @author Steven R. Emmerson
 * @version $Id: Converter.java 64 2006-07-12 22:30:50Z edavis $
 * @see Unit#getConverterTo(Unit)
 */
public interface
Converter
{
    /**
     * Converts a numeric value.
     * @param amount		The numeric value to convert.
     * @return			The converted numeric value.
     */
    public float
    convert(float amount);

    /**
     * Converts a numeric value.
     * @param amount		The numeric value to convert.
     * @return			The converted numeric value.
     */
    public double
    convert(double amount);

    /**
     * Converts an array of numeric values.
     * @param amounts		The numeric values to convert.
     * @return			The converted numeric values in allocated
     *				space.
     */
    public float[]
    convert(float[] amounts);

    /**
     * Converts an array of numeric values.
     * @param amounts		The numeric values to convert.
     * @return			The converted numeric values in allocated
     *				space.
     */
    public double[]
    convert(double[] amounts);

    /**
     * Converts an array of numeric values.
     * @param input		The numeric values to convert.
     * @param output		The converted numeric values.  May be
     *				same array as <code>input</code>.
     * @return			<code>output</code>.
     */
    public float[]
    convert(float[] input, float[] output);

    /**
     * Converts an array of numeric values.
     * @param input		The numeric values to convert.
     * @param output		The converted numeric values.  May be
     *				same array as <code>input</code>.
     * @return			<code>output</code>.
     */
    public double[]
    convert(double[] input, double[] output);
}
