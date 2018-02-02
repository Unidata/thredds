/*
 * Copyright 1998-2017 University Corporation for Atmospheric Research/Unidata
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

/**
 * Provides support for a database of standard units.
 * 
 * @author Steven R. Emmerson
 */
public final class StandardUnitDB extends UnitDBImpl {
	private static final long				serialVersionUID	= 1L;

	/**
	 * The singleton instance of this class.
	 * 
	 * @serial
	 */
	private static/* final */StandardUnitDB	instance			= null;

	/**
	 * The unit formatter for parsing unit string specifications.
	 * 
	 * @serial
	 */
	private static UnitFormat				format;

	/**
	 * Constructs from nothing.
	 * 
	 * @throws UnitParseException
	 *             A string specification couldn't be parsed.
	 * @throws SpecificationException
	 *             Bad string specification.
	 * @throws UnitDBException
	 *             Problem with the unit database.
	 * @throws PrefixDBException
	 *             Problem with the prefix database.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws OperationException
	 *             Improper unit operation.
	 * @throws UnitSystemException
	 *             Problem with UnitSystem.
	 */
	private StandardUnitDB() throws UnitParseException, SpecificationException,
			UnitDBException, PrefixDBException, NameException,
			OperationException, UnitSystemException {
		super(50, 50);

		/*
		 * SI units:
		 */
		add((UnitDBImpl) SI.instance().getUnitDB());

		/*
		 * Unit specification strings in the following rely on standard unit
		 * syntax. Additionally, unit specification strings may not reference
		 * units that have not yet been defined (i.e. forward referencing is not
		 * allowed).
		 */
		format = StandardUnitFormat.instance();

		/*
		 * Constants:
		 */
		au("PI", Double.toString(Math.PI));
		au("percent", "0.01", "%");
		au("bakersdozen", "13");
		au("pair", "2");
		au("dozen", "12");
		au("ten", "10");
		au("score", "20");
		au("hundred", "100");
		au("thousand", "1000");
		au("million", "1e6");

		/*
		 * Dimensionless derived unit:
		 */
		addUnit(DerivedUnitImpl.DIMENSIONLESS);

		/*
		 * Dimensionless derived units (avoid! Use "1e-6 kg/kg" instead):
		 */
		au("ppt", "1e-3");
		au("pptv", "1e-3");
		au("pptm", "1e-3");
		au("pptn", "1e-3");
		au("ppm", "1e-6");
		au("ppmv", "1e-6");
		au("ppmm", "1e-6");
		au("ppmn", "1e-6");
		au("ppb", "1e-9");
		au("ppbv", "1e-9");
		au("ppbm", "1e-9");
		au("ppbn", "1e-9");

		/*
		 * UNITS OF ELECTRIC CURRENT
		 */
		au("abampere", "10 ampere"); // exact
		au("gilbert", "7.957747e-1 ampere");
		au("statampere", "3.335640e-10 ampere");

		aa("amp", "ampere");
		aa("biot", "abampere");

		/*
		 * UNITS OF LUMINOUS INTENSITY
		 */
		aa("candle", "candela");

		/*
		 * UNITS OF THERMODYNAMIC TEMPERATURE
		 */
		au("rankine", "K/1.8");
		au("fahrenheit", "rankine @ 459.67");

		aa("degree Kelvin", "kelvin", "degrees Kelvin");
		aa("degreeK", "kelvin", "degreesK");
		aa("degree K", "kelvin", "degrees K");

		aa("Celsius", "degree_Celsius");
		aa("degreeC", "degree_Celsius", "degreesC");
		aa("degree C", "degree_Celsius", "degrees C");
		aa("degree centigrade", "degree_Celsius", "degrees centigrade");
		aa("centigrade", "degree_Celsius");

		aa("degree Rankine", "rankine", "degrees Rankine");
		aa("degreeR", "rankine", "degreesR");
		aa("degree R", "rankine", "degrees R");

		aa("degree Fahrenheit", "fahrenheit", "degrees Fahrenheit");
		aa("degreeF", "fahrenheit", "degreesF");
		aa("degree F", "fahrenheit", "degrees F");

		as("degK", "kelvin");
		as("deg K", "kelvin");
		as("degC", "degree_Celsius");
		as("deg C", "degree_Celsius");
		as("degR", "rankine");
		as("deg R", "rankine");
		as("degF", "fahrenheit");
		as("deg F", "fahrenheit");

		/*
		 * UNITS OF MASS
		 */
		au("assay ton", "2.916667e-2 kg");
		au("avoirdupois ounce", "2.834952e-2 kg"); // "oz" ambiguous
		au("avoirdupois pound", "4.5359237e-1 kg", "lb"); // exact
		au("carat", "2e-4 kg");
		au("grain", "6.479891e-5 kg", "gr"); // exact
		au("gram", "1e-3 kg", "g"); // exact
		au("long hundredweight", "5.080235e1 kg");
		au("pennyweight", "1.555174e-3 kg");
		au("short hundredweight", "4.535924e1 kg");
		au("slug", "14.59390 kg");
		au("troy ounce", "3.110348e-2 kg"); // "oz" ambiguous
		au("troy pound", "3.732417e-1 kg");
		au("unified atomic mass unit", "1.66054e-27 kg");
		au("scruple", "20 gr");
		au("apdram", "60 gr");
		au("apounce", "480 gr");
		au("appound", "5760 gr");

		aa("atomic mass unit", "unified_atomic_mass_unit");
		aa("amu", "unified_atomic_mass_unit");
		aa("metricton", "metric_ton");
		aa("apothecary ounce", "troy_ounce");
		aa("apothecary pound", "troy_pound");
		aa("pound", "avoirdupois_pound");
		aa("atomicmassunit", "unified_atomic_mass_unit");

		au("bag", "94 lb");
		au("short ton", "2000 lb");
		au("long ton", "2240 lb");

		aa("ton", "short_ton");
		aa("shortton", "short_ton");
		aa("longton", "long_ton");

		/*
		 * UNITS OF LENGTH
		 */
		au("astronomical unit", "1.495979e11 m", "AU");
		au("fermi", "femtometer"); // definition
		au("light year", "9.46073e15 m");
		au("micron", "micrometer"); // definition
		au("mil", "2.54e-5 m"); // exact
		au("parsec", "3.085678e16 m", "prs");
		au("printers point", "3.514598e-4 m");

		/*
		 * God help us! There's an international foot and a US survey foot and
		 * they're not the same!
		 */

		// US Survey foot stuff:
		au("US survey foot", "1200/3937 m", null, "US survey feet");
		au("US survey yard", "3 US_survey_feet"); // exact
		au("US survey mile", "5280 US_survey_feet"); // exact
		aa("US statute mile", "US_survey_mile");
		au("rod", "16.5 US_survey_feet"); // exact
		au("perch", "5.02921005842012 m");
		au("furlong", "660 US_survey_feet"); // exact
		au("fathom", "6 US_survey_feet"); // exact

		aa("pole", "rod");

		// International foot stuff:
		au("international inch", ".0254 m", null, "international inches"); // exact
		au("international foot", "12 international_inches", null,
				"international feet"); // exact
		au("international yard", "3 international_feet"); // exact
		au("international mile", "5280 international_feet"); // exact

		// Alias unspecified units to the international units:
		aa("inch", "international_inch", null, "in");
		aa("foot", "international_foot", "feet", "ft");
		aa("yard", "international_yard", null, "yd");
		aa("mile", "international_mile");

		au("chain", "2.011684e1 m");

		au("printers pica", "12 printers_point"); // exact

		aa("astronomicalunit", "astronomical_unit");
		aa("asu", "astronomical_unit");
		aa("nmile", "nautical_mile");

		aa("pica", "printers_pica");

		au("big point", "in/72"); // exact
		au("barleycorn", "in/3");

		au("arpentlin", "191.835 ft");

        /*
         * UNITS OF SUBSTANCE
         */
        au("avogadro_constant", "6.02214179e23/mol");
        au("molecule", "1/avogadro_constant");
        au("molec", "molecule");
        au("nucleon", "molecule");
        au("nuc", "molecule");

		/*
		 * UNITS OF TIME
		 */
		aa("sec", "second"); // avoid
		au("shake", "1e-8 s"); // exact
		au("sidereal day", "8.616409e4 s");
		au("sidereal hour", "3.590170e3 s");
		au("sidereal minute", "5.983617e1 s");
		au("sidereal second", "0.9972696 s");
		au("sidereal year", "3.155815e7 s");
		/*
		 * Interval between 2 successive passages of sun through vernal equinox
		 * (365.242198781 days -- see
		 * http://www.ast.cam.ac.uk/pubinfo/leaflets/,
		 * http://aa.usno.navy.mil/AA/ and
		 * http://adswww.colorado.edu/adswww/astro_coord.html):
		 */
		au("tropical year", "3.15569259747e7 s");
		au("lunar month", "29.530589 d");

		au("common year", "365 d");// exact: 153600e7 seconds
		au("leap year", "366 d");
		au("Julian year", "365.25 d");
		au("Gregorian year", "365.2425 d");
		au("sidereal month", "27.321661 d");
		au("tropical month", "27.321582 d");
		as("hr", "hour");
		au("fortnight", "14 d");
		au("week", "7 d");
		au("jiffy", "cs");
		aa("year", "tropical_year", null, "yr");
		au("eon", "Gyr"); // fuzzy
		au("month", "yr/12"); // on average
		aa("anno", "year", null, "ann");

		/*
		 * UNITS OF PLANE ANGLE
		 */
		au("circle", "360 deg");
		au("grade", "0.9 deg"); // exact

		aa("turn", "circle");
		aa("revolution", "circle", null, "r");
		aa("degree", "arc_degree");
		aa("degree north", "arc_degree", "degrees north");
		aa("degree east", "arc_degree", "degrees east");
		aa("degree true", "arc_degree", "degrees true");
		aa("angular degree", "arc_degree");
		aa("angular minute", "arc_minute");
		aa("angular second", "arc_second");
		aa("gon", "grade");

		aa("arcdegree", "arc_degree");
		aa("arcminute", "arc_minute");
		aa("arcsecond", "arc_second");

		aa("arcdeg", "arc_degree");
		aa("arcmin", "arc_minute");
		as("mnt", "arc_minute");
		aa("arcsec", "arc_second");

		aa("degree N", "degree_north", "degrees N");

		aa("degreeE", "degree_east", "degreesE");
		aa("degree E", "degree_east", "degrees E");

		au("degree west", "-1 degree_east", null, "degrees west");

		aa("degreeW", "degree_west", "degreesW");
		aa("degree W", "degree_west", "degrees W");
		aa("degreeT", "degree_true", "degreesT");
		aa("degree T", "degree_true", "degrees T");

		/*
		 * The following are derived units with special names. They are useful
		 * for defining other derived units.
		 */
		au("sphere", "4 PI sr");
		au("standard free fall", "9.806650 m/s2");

		aa("force", "standard_free_fall");
		aa("gravity", "standard_free_fall");
		aa("free fall", "standard_free_fall");

		au("mercury 0C", "13595.1 gravity kg/m3");
		au("mercury 60F", "13556.8 gravity kg/m3");
		au("conventional water", "1000 gravity kg/m3"); // exact
		au("water 4C", "999.972 gravity kg/m3");
		au("water 60F", "999.001 gravity kg/m3");

		aa("conventional mercury", "mercury_0C");

		aa("mercury0C", "mercury_0C");
		aa("mercury 32F", "mercury_0C");
		aa("water4C", "water_4C");
		aa("water 39F", "water_4C"); // actually 39.2 degF
		aa("mercury", "conventional_mercury");
		aa("water", "conventional_water");

		as("Hg", "mercury");
		as("H2O", "water");

		/*
		 * AREA
		 */
		au("circular mil", "5.067075e-10 m2");
		au("darcy", "9.869233e-13 m2");
		// porous solid permeability
		aa("ha", "hectare");
		au("acre", "160 rod2"); // exact

		/*
		 * ELECTRICITY AND MAGNETISM
		 */
		au("abfarad", "GF");
		au("abhenry", "nH"); // exact
		au("abmho", "GS"); // exact
		au("abohm", "nOhm"); // exact
		au("megohm", "MOhm"); // exact
		au("kilohm", "kOhm"); // exact
		au("abvolt", "1e-8 V"); // exact
		au("e", "1.60217733e-19 C");
		au("chemical faraday", "9.64957e4 C");
		au("physical faraday", "9.65219e4 C");
		au("C12 faraday", "9.648531e4 C");
		au("gamma", "nT"); // exact
		au("gauss", "1e-4 T"); // exact
		au("maxwell", "1e-8 Wb"); // exact
		au("oersted", "7.957747e1 A/m", "Oe");
		au("statcoulomb", "3.335640e-10 C");
		au("statfarad", "1.112650e-12 F");
		au("stathenry", "8.987554e11 H");
		au("statmho", "1.112650e-12 S");
		au("statohm", "8.987554e11 Ohm");
		au("statvolt", "2.997925e2 V");
		au("unit pole", "1.256637e-7 Wb");
		au("mho", "S");
		aa("faraday", "C12 faraday");
		// charge of 1 mole of electrons

		/*
		 * ENERGY (INCLUDES WORK)
		 */
		au("electronvolt", "1.602177e-19 J");
		au("erg", "1e-7 J"); // exact
		au("IT Btu", "1.05505585262e3 J");
		au("EC therm", "1.05506e8 J"); // exact
		au("thermochemical calorie", "4.184000 J"); // exact
		au("IT calorie", "4.1868 J"); // exact
		au("ton TNT", "4.184e9 J");
		au("US therm", "1.054804e8 J"); // exact
		au("watthour", "W.h");

		aa("therm", "US_therm");
		as("Wh", "watthour");
		aa("Btu", "IT_Btu");
		aa("calorie", "IT_calorie");
		aa("electron volt", "electronvolt");

		au("thm", "therm");
		au("cal", "calorie");
		as("eV", "electronvolt");
		au("bev", "gigaelectronvolt");

		/*
		 * FORCE
		 */
		au("dyne", "1e-5 N"); // exact
		au("pond", "9.806650e-3 N"); // exact
		au("force kilogram", "9.806650 N", "kgf"); // exact
		au("force ounce", "2.780139e-1 N", "ozf");
		au("force pound", "4.4482216152605 N", "lbf"); // exact
		au("poundal", "1.382550e-1 N");
		au("gf", "gram force");

		au("force gram", "milliforce_kilogram");
		au("force ton", "2000 force_pound"); // exact
		aa("ounce force", "force_ounce");
		aa("kilogram force", "force_kilogram");
		aa("pound force", "force_pound");

		au("kip", "kilolbf");
		aa("ton force", "force_ton");
		au("gram force", "force_gram");

		/*
		 * HEAT
		 */
		au("clo", "1.55e-1 K.m2.W-1");

		/*
		 * LIGHT
		 */
		au("footcandle", "1.076391e-1 lx");
		au("footlambert", "3.426259 cd/m2");
		au("lambert", "1e4/PI cd/m2"); // exact
		au("stilb", "1e4 cd/m2", "sb");
		au("phot", "1e4 lm/m2", "ph"); // exact
		au("nit", "cd.m2", "nt"); // exact
		au("langley", "4.184000e4 J/m2"); // exact
		au("blondel", "1/PI cd/m2");

		aa("apostilb", "blondel");

		/*
		 * MASS PER UNIT LENGTH
		 */
		au("denier", "1.111111e-7 kg/m");
		au("tex", "1e-6 kg/m");

		/*
		 * MASS PER UNIT TIME (INCLUDES FLOW)
		 */
		au("perm 0C", "5.72135e-11 kg/(Pa.s.m2)");
		au("perm 23C", "5.74525e-11 kg/(Pa.s.m2)");

		/*
		 * POWER
		 */
		au("voltampere", "V.A", "VA");
		au("boiler horsepower", "9.80950e3 W");
		au("shaft horsepower", "7.456999e2 W");
		au("metric horsepower", "7.35499 W");
		au("electric horsepower", "7.460000e2 W"); // exact
		au("water horsepower", "7.46043e2 W");
		au("UK horsepower", "7.4570e2 W");
		au("refrigeration ton", "12000 Btu/h");

		aa("horsepower", "shaft_horsepower", null, "hp");
		aa("ton of refrigeration", "refrigeration_ton");

		/*
		 * PRESSURE OR STRESS
		 */
		au("standard atmosphere", "1.01325e5 Pa", "atm"); // exact
		au("technical atmosphere", "kg.(0.01 gravity/m)2", "at");
		au("inch H2O 39F", "inch.water_39F");
		au("inch H2O 60F", "inch.water_60F");
		au("inch Hg 32F", "inch.mercury_32F");
		au("inch Hg 60F", "inch.mercury_60F");
		au("millimeter Hg 0C", "mm.mercury_0C");
		au("footH2O", "foot.water");
		au("cmHg", "cm.Hg");
		au("cmH2O", "cm.water");
		as("pal", "pascal");
		au("inch Hg", "inch.Hg");
		aa("inch hg", "inch_Hg");
		aa("inHg", "inch_Hg");
		aa("in Hg", "inch_Hg");
		au("millimeter Hg", "mm.Hg");
		aa("mmHg", "millimeter_Hg");
		aa("mm Hg", "millimeter_Hg");
		aa("torr", "millimeter_Hg");
		au("foot H2O", "foot.water");
		aa("ftH2O", "foot_H2O");
		au("psi", "pound.gravity/inch2");
		au("ksi", "kip/inch2");
		au("barie", "0.1 N/m2");

		aa("atmosphere", "standard_atmosphere");
		aa("barye", "barie");

		/*
		 * RADIATION UNITS
		 */
		aa("sie", "sievert");

		/*
		 * VELOCITY (INCLUDES SPEED)
		 */
		aa("knot international", "knot");
		aa("international knot", "knot");

		/*
		 * VISCOSITY
		 */
		au("poise", "1e-1 Pa.s", "P"); // exact
		au("stokes", "1e-4 m2/s", "St"); // exact
		au("rhe", "10 (Pa.s)-1");

		/*
		 * VOLUME (INCLUDES CAPACITY)
		 */
		au("acre foot", "1.233489e3 m3");
		// but `acre foot' is 1.2334867714897e3 meters^3. Odd.
		au("board foot", "2.359737e-3 m3");
		au("bushel", "3.523907e-2 m3", "bu");
		au("UK liquid gallon", "4.546090e-3 m3"); // exact
		au("Canadian liquid gallon", "4.546090e-3 m3"); // exact
		au("US dry gallon", "4.404884e-3 m3");
		au("US liquid gallon", "3.785412e-3 m3");
		au("cc", "1e-6 m3");
		au("stere", "m3"); // exact
		au("register ton", "2.831685 m3");

		au("US dry quart", "1/4 US_dry_gallon");
		au("US dry pint", "1/8 US_dry_gallon");

		au("US liquid quart", "1/4 US_liquid_gallon");
		au("US liquid pint", "1/8 US_liquid_gallon");
		au("US liquid cup", "1/16 US_liquid_gallon");
		au("US liquid gill", "1/32 US_liquid_gallon");
		au("US fluid ounce", "1/128 US_liquid_gallon");
		aa("US liquid ounce", "US_fluid_ounce");

		au("UK liquid quart", "1/4 UK_liquid_gallon");
		au("UK liquid pint", "1/8 UK_liquid_gallon");
		au("UK liquid cup", "1/16 UK_liquid_gallon");
		au("UK liquid gill", "1/32 UK_liquid_gallon");
		au("UK fluid ounce", "1/160 UK_liquid_gallon");
		aa("UK liquid ounce", "UK_fluid_ounce");

		aa("liquid gallon", "US_liquid_gallon");
		aa("fluid ounce", "US_fluid_ounce", null, "fl_oz");

		aa("dry quart", "US_dry_quart");
		aa("dry pint", "US_dry_pint");

		au("liquid quart", "1/4 liquid_gallon");
		au("liquid pint", "1/8 liquid_gallon");

		aa("gallon", "liquid_gallon");
		au("barrel", "42 US_liquid_gallon", "bbl");
		// petroleum industry definition
		aa("quart", "liquid_quart");
		aa("pint", "liquid_pint", null, "pt");
		au("cup", "1/16 liquid_gallon");
		au("gill", "1/32 liquid_gallon");
		au("tablespoon", "0.5 US_fluid_ounce", "Tbl");
		au("teaspoon", "1/3 tablespoon", "tsp");
		au("peck", "1/4 bushel", "pk");

		as("floz", "fluid_ounce");
		aa("acre_feet", "acre_foot");
		aa("board_feet", "board_foot");
		aa("tbsp", "tablespoon");
		aa("tblsp", "tablespoon");

		au("fldr", "1/8 floz");
		au("dram", "1/16 floz", "dr");

		au("firkin", "1/4 barrel");
		// exact but "barrel" is vague

		/*
		 * VOLUME PER UNIT TIME (includes FLOW)
		 */
		au("sverdrup", "1e6 m3/s"); // oceanographic flow

		/*
		 * COMPUTERS AND COMMUNICATION
		 */
		au("bit", "1"); // unit of information
		au("baud", "s-1", "Bd");
		au("bps", "s-1");
		au("cps", "s-1");

		/*
		 * MISC
		 */
		au("count", "1");
		au("kayser", "100 m-1"); // exact
		au("rps", "r/s");
		au("rpm", "1/60 rps");
		au("work_year", "2056 h");
		au("work_month", "1/12 work_year");
		aa("geopotential", "gravity", null, "gp");
		aa("dynamic", "geopotential");

		format = null; // allow garbage collection
	}

	/**
	 * Gets an instance of this database.
	 * 
	 * @return An instance of this database.
	 * @throws UnitDBException
	 *             The instance couldn't be created.
	 */
	public static StandardUnitDB instance() throws UnitDBException {
		synchronized (StandardUnitDB.class) {
			if (instance == null) {
				try {
					instance = new StandardUnitDB();
				}
				catch (final Exception e) {
					throw new UnitDBException(
							"Couldn't create standard unit-database", e);
				}
			}
		}
		return instance;
	}

	/**
	 * Adds a derived unit to the database.
	 * 
	 * @param name
	 *            The name of the unit.
	 * @param definition
	 *            The definition for the unit.
	 * @throws UnitExistsException
	 *             Attempt to redefine an existing unit.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws UnitParseException
	 *             A string specification couldn't be parsed.
	 * @throws SpecificationException
	 *             Bad string specification.
	 * @throws UnitDBException
	 *             Problem with the unit database.
	 * @throws PrefixDBException
	 *             Problem with the prefix database.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws OperationException
	 *             Improper unit operation.
	 * @throws UnitSystemException
	 *             Problem with UnitSystem.
	 */
	private void au(final String name, final String definition)
			throws UnitExistsException, NoSuchUnitException,
			UnitParseException, SpecificationException, UnitDBException,
			PrefixDBException, OperationException, NameException,
			UnitSystemException {
		au(name, definition, null);
	}

	/**
	 * Adds a derived unit to the database.
	 * 
	 * @param name
	 *            The name of the unit.
	 * @param definition
	 *            The definition for the unit.
	 * @param symbol
	 *            The symbol for the unit.
	 * @throws UnitExistsException
	 *             Attempt to redefine an existing unit.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws UnitParseException
	 *             A string specification couldn't be parsed.
	 * @throws SpecificationException
	 *             Bad string specification.
	 * @throws UnitDBException
	 *             Problem with the unit database.
	 * @throws PrefixDBException
	 *             Problem with the prefix database.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws OperationException
	 *             Improper unit operation.
	 * @throws UnitSystemException
	 *             Problem with UnitSystem.
	 */
	private void au(final String name, final String definition,
			final String symbol) throws UnitExistsException,
			NoSuchUnitException, UnitParseException, SpecificationException,
			UnitDBException, PrefixDBException, OperationException,
			NameException, UnitSystemException {
		au(name, definition, symbol, (String) null);
	}

	/**
	 * Adds a derived unit to the database.
	 * 
	 * @param name
	 *            The name of the unit.
	 * @param definition
	 *            The definition for the unit.
	 * @param symbol
	 *            The symbol for the unit.
	 * @param plural
	 *            The plural form of the name of the unit.
	 * @throws UnitExistsException
	 *             Attempt to redefine an existing unit.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws UnitParseException
	 *             A string specification couldn't be parsed.
	 * @throws SpecificationException
	 *             Bad string specification.
	 * @throws UnitDBException
	 *             Problem with the unit database.
	 * @throws PrefixDBException
	 *             Problem with the prefix database.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws OperationException
	 *             Improper unit operation.
	 * @throws UnitSystemException
	 *             Problem with UnitSystem.
	 */
	private void au(final String name, final String definition,
			final String symbol, final String plural)
			throws UnitExistsException, NoSuchUnitException,
			UnitParseException, SpecificationException, UnitDBException,
			PrefixDBException, OperationException, NameException,
			UnitSystemException {
		final Unit unit = format.parse(definition, this);
		if (unit == null) {
			throw new NoSuchUnitException(definition);
		}
		addUnit(unit.clone(UnitName.newUnitName(name, plural, symbol)));
	}

	/**
	 * Adds an alias for a unit to the database.
	 * 
	 * @param alias
	 *            The alias for the unit.
	 * @param name
	 *            The name of the unit.
	 * @throws UnitExistsException
	 *             Attempt to redefine an existing unit.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws UnitParseException
	 *             A string specification couldn't be parsed.
	 * @throws SpecificationException
	 *             Bad string specification.
	 * @throws UnitDBException
	 *             Problem with the unit database.
	 * @throws PrefixDBException
	 *             Problem with the prefix database.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws OperationException
	 *             Improper unit operation.
	 * @throws UnitSystemException
	 *             Problem with UnitSystem.
	 */
	private void aa(final String alias, final String name)
			throws UnitExistsException, NoSuchUnitException,
			UnitParseException, SpecificationException, UnitDBException,
			PrefixDBException, OperationException, NameException,
			UnitSystemException {
		aa(alias, name, null);
	}

	/**
	 * Adds an alias for a unit to the database.
	 * 
	 * @param alias
	 *            The alias for the unit.
	 * @param name
	 *            The name of the unit.
	 * @param plural
	 *            The plural form of the alias for the unit.
	 * @throws UnitExistsException
	 *             Attempt to redefine an existing unit.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws UnitParseException
	 *             A string specification couldn't be parsed.
	 * @throws SpecificationException
	 *             Bad string specification.
	 * @throws UnitDBException
	 *             Problem with the unit database.
	 * @throws PrefixDBException
	 *             Problem with the prefix database.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws OperationException
	 *             Improper unit operation.
	 * @throws UnitSystemException
	 *             Problem with UnitSystem.
	 */
	private void aa(final String alias, final String name, final String plural)
			throws UnitExistsException, NoSuchUnitException,
			UnitParseException, SpecificationException, UnitDBException,
			PrefixDBException, OperationException, NameException,
			UnitSystemException {
		aa(alias, name, plural, null);
	}

	/**
	 * Adds an alias for a unit to the database.
	 * 
	 * @param alias
	 *            The alias for the unit.
	 * @param name
	 *            The name of the unit.
	 * @param plural
	 *            The plural form of the alias for the unit.
	 * @param symbol
	 *            The symbol for the alias.
	 * @throws UnitExistsException
	 *             Attempt to redefine an existing unit.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws UnitParseException
	 *             A string specification couldn't be parsed.
	 * @throws SpecificationException
	 *             Bad string specification.
	 * @throws UnitDBException
	 *             Problem with the unit database.
	 * @throws PrefixDBException
	 *             Problem with the prefix database.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws OperationException
	 *             Improper unit operation.
	 * @throws UnitSystemException
	 *             Problem with UnitSystem.
	 */
	private void aa(final String alias, final String name, final String plural,
			final String symbol) throws UnitExistsException,
			NoSuchUnitException, UnitParseException, SpecificationException,
			UnitDBException, PrefixDBException, OperationException,
			NameException, UnitSystemException {
		addAlias(alias, name, symbol, plural);
	}

	/**
	 * Adds a symbol for a unit to the database.
	 * 
	 * @param symbol
	 *            The symbol for the unit.
	 * @param name
	 *            The name of the unit.
	 * @throws UnitExistsException
	 *             Attempt to redefine an existing unit.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws UnitParseException
	 *             A string specification couldn't be parsed.
	 * @throws SpecificationException
	 *             Bad string specification.
	 * @throws UnitDBException
	 *             Problem with the unit database.
	 * @throws PrefixDBException
	 *             Problem with the prefix database.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws OperationException
	 *             Improper unit operation.
	 * @throws UnitSystemException
	 *             Problem with UnitSystem.
	 */
	private void as(final String symbol, final String name)
			throws UnitExistsException, NoSuchUnitException,
			UnitParseException, SpecificationException, UnitDBException,
			PrefixDBException, OperationException, NameException,
			UnitSystemException {
		addSymbol(symbol, name);
	}

	/**
	 * Tests this class.
	 */
	public static void main(final String[] args) throws Exception {
		final UnitDB db = StandardUnitDB.instance();
		System.out.println("db.get(\"meter\")=" + db.get("meter"));
		System.out.println("db.get(\"meters\")=" + db.get("meters"));
		System.out.println("db.get(\"metre\")=" + db.get("metre"));
		System.out.println("db.get(\"metres\")=" + db.get("metres"));
		System.out.println("db.get(\"m\")=" + db.get("m"));
		System.out.println("db.get(\"newton\")=" + db.get("newton"));
		System.out.println("db.get(\"Cel\")=" + db.get("Cel"));
		System.out.println("db.get(\"Roentgen\")=" + db.get("Roentgen"));
		System.out.println("db.get(\"rad\")=" + db.get("rad"));
		System.out.println("db.get(\"rd\")=" + db.get("rd"));
		System.out.println("db.get(\"perches\")=" + db.get("perches"));
		System.out.println("db.get(\"jiffies\")=" + db.get("jiffies"));
		System.out.println("db.get(\"foo\")=" + db.get("foo"));
	}
}
