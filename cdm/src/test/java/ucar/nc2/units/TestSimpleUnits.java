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

public class TestSimpleUnits extends TestCase  {

  public TestSimpleUnits( String name) {
    super(name);
  }


  ///////////////////////////////////////////////////////////
  // testing

  /* private void tryDivide() throws Exception {
    SimpleUnit t1 = SimpleUnit.factory("9 hPa");
    SimpleUnit t2 = SimpleUnit.factory("3 mbar");
    SimpleUnit t3 = t1.divideBy(t2);
    System.out.println(t1+" divideBy "+t2+" = " +t3);
    assert t3.getValue() == 3.0 : t3.getValue();
  } */

  private  void tryConvert() throws Exception {
    SimpleUnit t1 = SimpleUnit.factory("1 days");
    SimpleUnit t2 = SimpleUnit.factory("1 hour");
    double v = t1.convertTo(1.0, t2);
    System.out.println(t1+" convertTo "+t2+" = " +v);

    assert v == 24.0;
  }

  /** testing */
  public void testUnits() throws Exception {
    SimpleUnit su = SimpleUnit.factory( "11 hPa");
    assert !(su instanceof TimeUnit);
    assert su.getValue() == 1100.0 : su;
    assert su.getUnitString().equals("Pa") : su;

    su = SimpleUnit.factory( "11 km");
    assert !(su instanceof TimeUnit);
    assert su.getValue() == 11000.0 : su;
    assert su.getUnitString().equals("m") : su;

    SimpleUnit tu = SimpleUnit.factory("3 days");
    assert tu instanceof TimeUnit : tu.getClass().getName();
    assert tu.getUnitString().equals("days");
    assert tu.getValue() == 3.0 : su;

   /* String text = "3 days since 1930-07-27 12:00:00-05:00";
    SimpleUnit du = SimpleUnit.factory( text);
    System.out.println(text+" == standard format "+du);
    assert !(du instanceof TimeUnit);
    assert du instanceof DateUnit;

    text = "hours since 1930-07-29T01:00:00-08:00";
    du = SimpleUnit.factory( text);
    System.out.println(text+" == standard format "+du);
    assert !(du instanceof TimeUnit);
    assert du instanceof DateUnit;

    text = "0 hours since 1930-07-29T01:00:00-08:00";
    du = SimpleUnit.factory( text);
    System.out.println(text+" == standard format "+du);
    assert !(du instanceof TimeUnit);
    assert du instanceof DateUnit;  */

    //tryDivide();
    tryConvert();
  }

  public void testCompatible() {
    SimpleUnit su = SimpleUnit.factory( "11 hPa");
    assert su.isCompatible("mbar");
    assert !su.isCompatible("m");
    assert !su.isCompatible("sec");
    assert !su.isCompatible("3 days since 1930-07-27 12:00:00-05:00");


  }
}
