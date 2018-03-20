/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.units;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Misc tests on iosp, mostly just sanity (opens ok)
 *
 * @author caron
 * @since 7/29/2014
 */
public class TestUdunits {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testOffsetUnit() throws IOException, NameException, UnitExistsException, OperationException, ConversionException {
    final BaseUnit kelvin = BaseUnit.getOrCreate(UnitName.newUnitName("kelvin", null, "K"), BaseQuantity.THERMODYNAMIC_TEMPERATURE);
    final OffsetUnit celsius = new OffsetUnit(kelvin, 273.15);
    System.out.println("celsius.equals(kelvin)=" + celsius.equals(kelvin));
    System.out.println("celsius.getUnit().equals(kelvin)=" + celsius.getUnit().equals(kelvin));
    assert !celsius.equals(kelvin);
    assert celsius.getUnit().equals(kelvin);

    final Unit celsiusKelvin = celsius.multiplyBy(kelvin);
    System.out.println("celsiusKelvin.divideBy(celsius)=" + celsiusKelvin.divideBy(celsius));
    System.out.println("celsius.divideBy(kelvin)=" + celsius.divideBy(kelvin));
    System.out.println("kelvin.divideBy(celsius)=" + kelvin.divideBy(celsius));
    System.out.println("celsius.raiseTo(2)=" + celsius.raiseTo(2));
    System.out.println("celsius.toDerivedUnit(1.)=" + celsius.toDerivedUnit(1.));
    System.out.println("celsius.toDerivedUnit(new float[]{1,2,3}, new float[3])[1]=" + celsius.toDerivedUnit(new float[]{1, 2, 3}, new float[3])[1]);
    System.out.println("celsius.fromDerivedUnit(274.15)=" + celsius.fromDerivedUnit(274.15));
    System.out.println("celsius.fromDerivedUnit(new float[]{274.15f},new float[1])[0]=" + celsius.fromDerivedUnit(new float[]{274.15f}, new float[1])[0]);
    System.out.println("celsius.equals(celsius)=" + celsius.equals(celsius));
    assert celsius.equals(celsius);

    final OffsetUnit celsius100 = new OffsetUnit(celsius, 100.);
    System.out.println("celsius.equals(celsius100)=" + celsius.equals(celsius100));
    System.out.println("celsius.isDimensionless()=" + celsius.isDimensionless());
    assert !celsius.equals(celsius100);
    assert !celsius.isDimensionless();

    final BaseUnit radian = BaseUnit.getOrCreate(UnitName.newUnitName("radian", null, "rad"), BaseQuantity.PLANE_ANGLE);
    final OffsetUnit offRadian = new OffsetUnit(radian, 3.14159 / 2);
    System.out.println("offRadian.isDimensionless()=" + offRadian.isDimensionless());
    assert offRadian.isDimensionless();
  }

  @Test
  public void testLogarithmicUnit() throws IOException, NameException, UnitExistsException, OperationException, ConversionException {
    final BaseUnit meter = BaseUnit.getOrCreate(UnitName.newUnitName("meter", null, "m"), BaseQuantity.LENGTH);
    final ScaledUnit micron = new ScaledUnit(1e-6, meter);
    final Unit cubicMicron = micron.raiseTo(3);
    final LogarithmicUnit Bz = new LogarithmicUnit(cubicMicron, 10.0);
    assert Bz.isDimensionless();
    assert Bz.equals(Bz);
    assert Bz.getReference().equals(cubicMicron);
    assert Bz.getBase() == 10.0;
    assert !Bz.equals(cubicMicron);
    assert !Bz.equals(micron);
    assert !Bz.equals(meter);
    try {
      Bz.multiplyBy(meter);
      assert false;
    } catch (final MultiplyException e) {
    }
    try {
      Bz.divideBy(meter);
      assert false;
    } catch (final DivideException e) {
    }
    try {
      Bz.raiseTo(2);
      assert false;
    } catch (final RaiseException e) {
    }
    double value = Bz.toDerivedUnit(0);
    assert 0.9e-18 < value && value < 1.1e-18 : value;
    value = Bz.toDerivedUnit(1);
    assert 0.9e-17 < value && value < 1.1e-17 : value;
    value = Bz.fromDerivedUnit(1e-18);
    assert -0.1 < value && value < 0.1 : value;
    value = Bz.fromDerivedUnit(1e-17);
    assert 0.9 < value && value < 1.1 : value;
    final String string = Bz.toString();
    assert string.equals("lg(re 9.999999999999999E-19 m3)") : string;
  }

  @Test
  public void testTimeScaleUnit() throws Exception {
      final TimeZone tz = TimeZone.getTimeZone("UTC");
      final Calendar calendar = Calendar.getInstance(tz);
      calendar.clear();
      calendar.set(1970, 0, 1);
    TimeScaleUnit tunit = new TimeScaleUnit(TimeScaleUnit.SECOND, calendar.getTime());
    System.out.printf("%s%n",tunit );
  }

  @Test
  public void testUnknownUnit() throws Exception  {
    final UnknownUnit unit1 = UnknownUnit.create("a");
    assert unit1.equals(unit1) : "unit1.equals(unit1)=" + unit1.equals(unit1);
    assert !unit1.isDimensionless() : "UnknownUnit.isDimensionless()=" + unit1.isDimensionless();
    UnknownUnit unit2 = UnknownUnit.create("b");
    assert !unit1.equals(unit2): "unit1.equals(unit2)="+unit1.equals(unit2);
    unit2 = UnknownUnit.create("A");
    assert unit1.equals(unit2): "unit_a.equals(unit_A))="+unit1.equals(unit2);
  }
}
