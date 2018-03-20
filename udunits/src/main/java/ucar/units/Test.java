/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for testing the ucar.units package.
 * 
 * @author Steven R. Emmerson
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
