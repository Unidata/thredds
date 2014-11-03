/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.units;

import org.junit.Test;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Misc tests on iosp, mostly just sanity (opens ok)
 *
 * @author caron
 * @since 7/29/2014
 */
public class TestUdunits {

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
