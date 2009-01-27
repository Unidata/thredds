// $Id: DerivableUnit.java 64 2006-07-12 22:30:50Z edavis $
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
 * Interface for units that can convert numeric values to and from an
 * underlying derived unit.
 *
 * @author Steven R. Emmerson
 * @version $Id: DerivableUnit.java 64 2006-07-12 22:30:50Z edavis $
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
