package ucar.nc2.units;

import junit.framework.*;

import ucar.units.Unit;
import ucar.units.ScaledUnit;
import ucar.units.DerivedUnit;
import ucar.units.ConversionException;
import ucar.nc2.TestAll;

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
    assert TestAll.closeEnough(tu.getValueInSeconds(), 11.0 * secsBefore) : (tu.getValueInSeconds())+" "+ secsBefore;

    System.out.println();
    tu.setValueInSeconds( 3600.0);
    System.out.println(" NewTimeUnitSecs.toString=      "+tu.toString());

    assert tu.getValue() == 1.0;
    assert tu.getValueInSeconds() == 3600.0 : tu.getValueInSeconds();
    assert tu.getUnitString().equals( unitBefore);
    assert TestAll.closeEnough( 3.0 * tu.getValueInSeconds(), secsBefore) : tu.getValueInSeconds()+" "+secsBefore;

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
