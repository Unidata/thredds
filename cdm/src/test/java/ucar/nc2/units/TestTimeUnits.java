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

import ucar.nc2.util.Misc;
import ucar.units.Unit;
import ucar.units.ScaledUnit;
import ucar.units.DerivedUnit;

public class TestTimeUnits extends TestCase  {

  public TestTimeUnits( String name) {
    super(name);
  }

  private void showUnitInfo( Unit uu) {
    System.out.println(" ucar.units.Unit.class=              "+uu.getClass().getName());
    System.out.println(" ucar.units.Unit.toString=           "+uu.toString());
    System.out.println(" ucar.units.Unit.getCanonicalString= "+uu.getCanonicalString());
    System.out.println(" ucar.units.Unit.getName=            "+uu.getName());
    System.out.println(" ucar.units.Unit.getSymbol=          "+uu.getSymbol());
    System.out.println(" ucar.units.Unit.getUnitName=        "+uu.getUnitName());
    System.out.println(" ucar.units.Unit.getDerivedUnit=     "+uu.getDerivedUnit());

    if (uu instanceof ScaledUnit) {
      ScaledUnit su = (ScaledUnit) uu;
      DerivedUnit du = su.getDerivedUnit();
      showUnitInfo( du);
    }
  }

  public void testTimes() throws Exception {
    TimeUnit tu = new TimeUnit(3.0, "hours");
    System.out.println(" TimeUnit.toString=      "+tu.toString());
    System.out.println(" TimeUnit.getValue=      "+tu.getValue());
    System.out.println(" TimeUnit.getUnitString= "+tu.getUnitString());

    Unit uu = tu.getUnit();
    showUnitInfo( uu);
    System.out.println();

    uu = SimpleUnit.makeUnit("3.0 hours");
    showUnitInfo( uu);
    System.out.println();

    String unitBefore = tu.getUnitString();
    double secsBefore = tu.getValueInSeconds();

    tu.setValue( 33.0);
    System.out.println(" NewTimeUnit.toString=      "+tu.toString());

    assert tu.getValue() == 33.0;
    assert 3600.0 * tu.getValue() == tu.getValueInSeconds() : tu.getValue() +" "+tu.getValueInSeconds();
    assert tu.getUnitString().equals( unitBefore);
    assert Misc.closeEnough(tu.getValueInSeconds(), 11.0 * secsBefore) : (tu.getValueInSeconds())+" "+ secsBefore;

    System.out.println();
    tu.setValueInSeconds( 3600.0);
    System.out.println(" NewTimeUnitSecs.toString=      "+tu.toString());

    assert tu.getValue() == 1.0;
    assert tu.getValueInSeconds() == 3600.0 : tu.getValueInSeconds();
    assert tu.getUnitString().equals( unitBefore);
    assert Misc.closeEnough( 3.0 * tu.getValueInSeconds(), secsBefore) : tu.getValueInSeconds()+" "+secsBefore;

    TimeUnit day = new TimeUnit(1.0, "day");
    double hoursInDay = day.convertTo(1.0, tu);
    assert hoursInDay == 24.0;

    // note the value is ignored, only the base unit is used
    day = new TimeUnit(10.0, "day");
    hoursInDay = day.convertTo(1.0, tu);
    assert hoursInDay == 24.0;

    hoursInDay = day.convertTo(10.0, tu);
    assert hoursInDay == 240.0 : hoursInDay;

  }

}
