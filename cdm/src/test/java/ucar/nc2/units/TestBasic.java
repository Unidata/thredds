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
package ucar.nc2.units;

import junit.framework.*;
import ucar.units.*;

public class TestBasic extends TestCase {
  private boolean debug = false;

  public TestBasic( String name) {
    super(name);
  }

  public void testBasic() {
    UnitFormat format = UnitFormatManager.instance();

    try {
      Unit meter = format.parse("meter");
      Unit second = format.parse("second");
      Unit meterPerSecondUnit = meter.divideBy(second);
      Unit knot = format.parse("knot");
      assert (meterPerSecondUnit.isCompatible(knot));

      if (debug) System.out.println("5 knots is " +
        knot.convertTo(5, meterPerSecondUnit) +
        ' ' + format.format(meterPerSecondUnit));
      assert(closeEnough(2.5722222, knot.convertTo(5, meterPerSecondUnit)));

    } catch (Exception e) {
      System.out.println("Exception " + e);
    }
  }

  private boolean closeEnough( double d1, double d2) {
    return Math.abs(d1-d2) < 1.0e-5;
  }

  public void testTimeConversion()   {
    UnitFormat format = UnitFormatManager.instance();
    Unit t1, t2;

    try {
      t1 = format.parse("secs since 1999-01-01 00:00:00");
      t2 = format.parse("secs since 1999-01-02 00:00:00");
      assert(t1.isCompatible( t2));
    } catch (Exception e) {
      System.out.println("testTimeConversion failed " +e);
      return;
    }

    try {
      if (debug) System.out.println("t2.convertTo(0.0, t1) " +t2.convertTo(0.0, t1));
      assert(closeEnough(86400.0, t2.convertTo(0.0, t1)));
    } catch (Exception e) {
      System.out.println("testTimeConversion failed 2 =" +e);
    }
  }

  public void testTimeConversion2()   {
    UnitFormat format = UnitFormatManager.instance();
    Unit t1, t2;

    try {
      t1 = format.parse("secs since 1999-01-01 00:00:00");
      t2 = format.parse("10 hours since 1999-01-01 00:00:00");
      assert(t1.isCompatible( t2));
    } catch (Exception e) {
      System.out.println("testTimeConversion failed " +e);
      return;
    }

    try {
      System.out.println("t2.convertTo(0.0, t1) " +t2.convertTo(0.0, t1));
      // assert(closeEnough(86400.0, t2.convertTo(0.0, t1)));
    } catch (Exception e) {
      System.out.println("testTimeConversion failed 2 =" +e);
    }
  }

  public void testException() {
    UnitFormat format = UnitFormatManager.instance();
    try {
      Unit uu = format.parse("barf");
      System.out.println("Parse ok= " +uu);
    } catch (Exception e) {
      System.out.println("Parse got Exception " +e);
    }
  }


}
