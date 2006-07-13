// $Id: Test.java 64 2006-07-12 22:30:50Z edavis $
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
 * Provides support for testing the ucar.units package.
 *
 * @author Steven R. Emmerson
 * @version $Id: Test.java 64 2006-07-12 22:30:50Z edavis $
 */
public class
Test
{
    public static void
    main(String[] args)
	throws	Exception
    {
	UnitFormat	format = UnitFormatManager.instance();
	Unit	t1 = format.parse("secs since 1970-01-01 00:00:00");
	Unit	t2 = format.parse("secs since 1999-02-03 00:00:00");
	System.out.println("t1 = " + t1);
	System.out.println("t2 = " + t2);
	System.out.println("t1.isCompatible(t2) = " + t1.isCompatible(t2));
	System.out.println("t2.convertTo(0.0, t1) = " + t2.convertTo(0.0, t1));
	t1 = format.parse("years since 1930-07-27");
	t1 = format.parse("years since 1930");
	t1 = format.parse("12 secs since 1970-01-02T00:00:00Z");
	System.out.println("format.parse(\"\") = \"" +
	    format.parse("") + '"');
	System.out.println("format.parse(\"s\") = \"" + 
	    format.parse("s") + '"');
	System.out.println("format.parse(\"SeCoNd\") = \"" + 
	    format.parse("SeCoNd") + '"');
	System.out.println("format.parse(\"min\") = \"" + 
	    format.parse("min") + '"');
	System.out.println("format.parse(\"min\").toString() = \"" + 
	    format.parse("min").toString() + '"');
	System.out.println("format.parse(\"60 s\") = \"" + 
	    format.parse("60 s") + '"');
	System.out.println("format.parse(\"Cel\") = \"" + 
	    format.parse("Cel") + '"');
	System.out.println("format.parse(\"Cel\").toString() = \"" + 
	    format.parse("Cel").toString() + '"');
	System.out.println(
	    "format.parse(\"min @ 1970-01-01 00:00:00 UTC\") = \"" + 
	    format.parse("min @ 1970-01-01 00:00:00 UTC") + '"');
	System.out.println(
	"format.parse(\"min @ 2000-01-01 00 UTC\").toString() = \"" +
	    format.parse("min @ 2000-01-01 00 UTC").toString() + '"');
	System.out.println( "format.parse(\"g/kg\") = \"" +
	    format.parse("g/kg") + '"');
	System.out.println(
	    "format.longFormat(format.parse(\"g/kg\")) = \"" +
	    format.longFormat(format.parse("g/kg")) + '"');
	Unit	celsius = format.parse("Cel");
	Unit	kelvin = format.parse("K");
	System.out.println(
	    "celsius.getConverterTo(kelvin).convert(0) = \"" +
	    celsius.getConverterTo(kelvin).convert(0) + '"');
	System.out.println(
	    "kelvin.getConverterTo(celsius).convert(273.15) = \"" +
	    kelvin.getConverterTo(celsius).convert(273.15) + '"');
	Unit	second = format.parse("s");
	Unit	minute = format.parse("min");
	System.out.println(
	    "second.getConverterTo(minute).convert(60) = \"" +
	    second.getConverterTo(minute).convert(60) + '"');
	System.out.println(
	    "minute.getConverterTo(second).convert(1) = \"" +
	    minute.getConverterTo(second).convert(1) + '"');
	Unit	mixingRatio =
	    format.parse("g/kg").clone(UnitName.newUnitName("mixing ratio"));
	System.out.println("mixingRatio.toString() = \"" +
	    mixingRatio.toString() + '"');
	System.out.println("format.longFormat(mixingRatio) = \"" +
	    format.longFormat(mixingRatio) + '"');
	System.out.println(
	    "format.longFormat(format.parse(\"(g/kg)/(lb/ton)\")) = \"" +
	    format.longFormat(format.parse("(g/kg)/(lb/ton)")) + '"');
	System.out.println(
	    "format.parse(\"(g/mol)/(lb/kmol)\").toString() = \"" +
	    format.parse("(g/mol)/(lb/kmol)").toString() + '"');
	System.out.println(
	    "format.longFormat(format.parse(\"(g/mol)/(lb/kmol)\")) = \"" +
	    format.longFormat(format.parse("(g/mol)/(lb/kmol)")) + '"');
    }
}
