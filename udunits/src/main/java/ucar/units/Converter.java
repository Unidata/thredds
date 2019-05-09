/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Interface for converting numeric values from one unit to another.
 * @author Steven R. Emmerson
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
    float
    convert(float amount);

    /**
     * Converts a numeric value.
     * @param amount		The numeric value to convert.
     * @return			The converted numeric value.
     */
    double
    convert(double amount);

    /**
     * Converts an array of numeric values.
     * @param amounts		The numeric values to convert.
     * @return			The converted numeric values in allocated
     *				space.
     */
    float[]
    convert(float[] amounts);

    /**
     * Converts an array of numeric values.
     * @param amounts		The numeric values to convert.
     * @return			The converted numeric values in allocated
     *				space.
     */
    double[]
    convert(double[] amounts);

    /**
     * Converts an array of numeric values.
     * @param input		The numeric values to convert.
     * @param output		The converted numeric values.  May be
     *				same array as <code>input</code>.
     * @return			<code>output</code>.
     */
    float[]
    convert(float[] input, float[] output);

    /**
     * Converts an array of numeric values.
     * @param input		The numeric values to convert.
     * @param output		The converted numeric values.  May be
     *				same array as <code>input</code>.
     * @return			<code>output</code>.
     */
    double[]
    convert(double[] input, double[] output);
}
