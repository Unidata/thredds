// $Id: Test.java 64 2006-07-12 22:30:50Z edavis $
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
 * Provides support for testing the ucar.units package.
 * 
 * @author Steven R. Emmerson
 * @version $Id: Test.java 64 2006-07-12 22:30:50Z edavis $
 */
public class Test {
    private static void myAssert(final boolean value) {
        if (!value) {
            throw new AssertionError();
        }
    }

    public static void main(final String[] args) throws Exception {
        final UnitFormat format = UnitFormatManager.instance();
        Unit t1 = format.parse("secs since 1970-01-01 00:00:00");
        final Unit t2 = format.parse("secs since 1999-02-03 00:00:00");
        System.out.println("t1 = " + t1);
        System.out.println("t2 = " + t2);
        System.out.println("t1.isCompatible(t2) = " + t1.isCompatible(t2));
        System.out.println("t2.convertTo(0.0, t1) = " + t2.convertTo(0.0, t1));
        t1 = format.parse("years since 1930-07-27");
        t1 = format.parse("years since 1930");
        t1 = format.parse("12 secs since 1970-01-02T00:00:00Z");
        System.out.println("format.parse(\"\") = \"" + format.parse("") + '"');
        System.out
                .println("format.parse(\"s\") = \"" + format.parse("s") + '"');
        System.out.println("format.parse(\"SeCoNd\") = \""
                + format.parse("SeCoNd") + '"');
        System.out.println("format.parse(\"min\") = \"" + format.parse("min")
                + '"');
        System.out.println("format.parse(\"min\").toString() = \""
                + format.parse("min").toString() + '"');
        System.out.println("format.parse(\"60 s\") = \"" + format.parse("60 s")
                + '"');
        System.out.println("format.parse(\"Cel\") = \"" + format.parse("Cel")
                + '"');
        System.out.println("format.parse(\"Cel\").toString() = \""
                + format.parse("Cel").toString() + '"');
        System.out
                .println("format.parse(\"min @ 1970-01-01 00:00:00 UTC\") = \""
                        + format.parse("min @ 1970-01-01 00:00:00 UTC") + '"');
        System.out
                .println("format.parse(\"min @ 2000-01-01 00 UTC\").toString() = \""
                        + format.parse("min @ 2000-01-01 00 UTC").toString()
                        + '"');
        System.out.println("format.parse(\"g/kg\") = \"" + format.parse("g/kg")
                + '"');
        System.out.println("format.longFormat(format.parse(\"g/kg\")) = \""
                + format.longFormat(format.parse("g/kg")) + '"');
        final Unit celsius = format.parse("Cel");
        Unit kelvin = format.parse("K");
        System.out.println("celsius.getConverterTo(kelvin).convert(0) = \""
                + celsius.getConverterTo(kelvin).convert(0) + '"');
        System.out
                .println("kelvin.getConverterTo(celsius).convert(273.15) = \""
                        + kelvin.getConverterTo(celsius).convert(273.15) + '"');
        myAssert(kelvin.isCompatible(celsius));
        myAssert(!kelvin.equals(celsius));
        myAssert(kelvin.hashCode() != celsius.hashCode());
        Unit fahrenheit = format.parse("fahrenheit");
        fahrenheit = fahrenheit.clone(UnitName.newUnitName("fahrenheit"));
        kelvin = kelvin.clone(UnitName.newUnitName("kelvin"));
        myAssert(kelvin.isCompatible(fahrenheit));
        myAssert(fahrenheit.isCompatible(kelvin));
        final Unit kelvinUnit = format.parse("kelvin").clone(
                UnitName.newUnitName("kelvin"));
        final Unit fahrenUnit = format.parse("fahrenheit").clone(
                UnitName.newUnitName("fahrenheit"));
        myAssert(kelvinUnit.isCompatible(fahrenUnit));
        final Unit second = format.parse("s");
        final Unit minute = format.parse("min");
        System.out.println("second.getConverterTo(minute).convert(60) = \""
                + second.getConverterTo(minute).convert(60) + '"');
        System.out.println("minute.getConverterTo(second).convert(1) = \""
                + minute.getConverterTo(second).convert(1) + '"');
        final Unit mixingRatio = format.parse("g/kg").clone(
                UnitName.newUnitName("mixing ratio"));
        System.out.println("mixingRatio.toString() = \""
                + mixingRatio.toString() + '"');
        System.out.println("format.longFormat(mixingRatio) = \""
                + format.longFormat(mixingRatio) + '"');
        System.out
                .println("format.longFormat(format.parse(\"(g/kg)/(lb/ton)\")) = \""
                        + format.longFormat(format.parse("(g/kg)/(lb/ton)"))
                        + '"');
        System.out
                .println("format.parse(\"(g/mol)/(lb/kmol)\").toString() = \""
                        + format.parse("(g/mol)/(lb/kmol)").toString() + '"');
        System.out
                .println("format.longFormat(format.parse(\"(g/mol)/(lb/kmol)\")) = \""
                        + format.longFormat(format.parse("(g/mol)/(lb/kmol)"))
                        + '"');
    }
}
