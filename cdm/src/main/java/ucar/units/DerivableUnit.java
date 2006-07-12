// $Id$
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
 * Interface for units that can convert numeric values to and from an
 * underlying derived unit.
 *
 * @author Steven R. Emmerson
 * @version $Id$
 */
public interface
DerivableUnit
{
    /**
     * Returns the derived unit that underlies this unit.
     * @return			The derived unit that underlies this unit.
     */
    public DerivedUnit
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
    public float
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
    public double
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
    public float[]
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
    public double[]
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
    public float
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
    public double
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
    public float[]
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
    public double[]
    fromDerivedUnit(double[] input, double[] output)
        throws ConversionException;
}
