/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Interface for units that can convert numeric values to and from an
 * underlying derived unit.
 *
 * @author Steven R. Emmerson
 */
public interface
DerivableUnit
{
    /**
     * Returns the derived unit that underlies this unit.
     * @return			The derived unit that underlies this unit.
     */
    DerivedUnit
    getDerivedUnit();

    /**
     * Converts a numeric value in this unit to the underlying derived unit.
     * This method might fail even though <code>convertTo()</code> succeeds.
     * @param amount		The numeric values in this unit.
     * @return			The numeric value in the underlying derived
     *				unit.
     * @throws ConversionException	Can't convert values to underlying 
     *					derived unit.
     */
    float
    toDerivedUnit(float amount)
	throws ConversionException;

    /**
     * Converts a numeric value in this unit to the underlying derived unit.
     * This method might fail even though <code>convertTo()</code> succeeds.
     * @param amount            The numeric value in this unit.
     * @return                  The equivalent numeric value in the 
     *                          underlying derived unit.
     * @throws ConversionException      Can't convert to derived unit.
     */
    double
    toDerivedUnit(double amount)
        throws ConversionException;

    /**
     * Converts numeric values in this unit to the underlying derived unit.
     * This method might fail even though <code>convertTo()</code> succeeds.
     * @param input             The numeric values in this unit.
     * @param output            The equivalent numeric values in the
     *                          underlying derived unit.  May be the same
     *                          array as <code>input</code>.
     * @return                  <code>output</code>.
     * @throws ConversionException      Can't convert to derived unit.
     */
    float[]
    toDerivedUnit(float[] input, float[] output)
        throws ConversionException;

    /**
     * Converts numeric values in this unit to the underlying derived unit.
     * This method might fail even though <code>convertTo()</code> succeeds.
     * @param input             The numeric values in this unit.
     * @param output            The equivalent numeric values in the
     *                          underlying derived unit.  May be the same
     *                          array as <code>input</code>.
     * @return                  <code>output</code>.
     * @throws ConversionException      Can't convert to derived unit.
     */
    double[]
    toDerivedUnit(double[] input, double[] output)
        throws ConversionException;

    /**
     * Converts numeric values from the underlying derived unit to this unit.
     * @param amount		The numeric values in the underlying derived
     *				unit.
     * @return			The numeric values in this unit.
     * @throws ConversionException	Can't convert values from underlying
     *					derived unit.
     */
    float
    fromDerivedUnit(float amount)
	throws ConversionException;

    /**
     * Converts a numeric value from the underlying derived unit to this unit.
     * This method might fail even though <code>convertTo()</code> succeeds.
     * @param amount            The numeric value in the underlying derived
     *                          unit.
     * @return                  The equivalent numeric value in this unit.
     * @throws ConversionException      Can't convert from underlying derived
     *                                  unit.
     */
    double
    fromDerivedUnit(double amount)
        throws ConversionException;

    /**
     * Converts numeric values from the underlying derived unit to this unit.
     * This method might fail even though <code>convertTo()</code> succeeds.
     * @param input             The numeric values in the underlying derived
     *                          unit.
     * @param output            The equivalent numeric values in this unit.
     *                          May be same array as <code>input</code>.
     * @return                  <code>output</code>.
     * @throws ConversionException      Can't convert from underlying derived
     *                                  unit.
     */
    float[]
    fromDerivedUnit(float[] input, float[] output)
        throws ConversionException;

    /**
     * Converts numeric values from the underlying derived unit to this unit.
     * This method might fail even though <code>convertTo()</code> succeeds.
     * @param input             The numeric values in the underlying derived
     *                          unit.
     * @param output            The equivalent numeric values in this unit.
     *                          May be same array as <code>input</code>.
     * @return                  <code>output</code>.
     * @throws ConversionException      Can't convert from underlying derived
     *                                  unit.
     */
    double[]
    fromDerivedUnit(double[] input, double[] output)
        throws ConversionException;
}
