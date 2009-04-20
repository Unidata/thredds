// $Id: SI.java 64 2006-07-12 22:30:50Z edavis $
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
 * Provides support for the International System of Units (SI). A good on-line
 * reference for the SI can be found at <http://physics.nist.gov/cuu/Units/>.
 * 
 * @author Steven R. Emmerson
 * @version $Id: SI.java 64 2006-07-12 22:30:50Z edavis $
 */
public final class SI extends UnitSystemImpl {
	private static final long		serialVersionUID	= 1L;

	/*
	 * The singleton instance.
	 * 
	 * @serial
	 */
	private static/* final */SI		si;

	/*
	 * Units corresponding to base quantities:
	 */

	/**
	 * The unit corresponding to the quantity of amount of substance.
	 */
	public static final Unit		AMOUNT_OF_SUBSTANCE_UNIT;

	/**
	 * The unit corresponding to the quantity of electric current.
	 */
	public static final Unit		ELECTRIC_CURRENT_UNIT;

	/**
	 * The unit corresponding to the quantity of length.
	 */
	public static final Unit		LENGTH_UNIT;

	/**
	 * The unit corresponding to the quantity of luminous intensity.
	 */
	public static final Unit		LUMINOUS_INTENSITY_UNIT;

	/**
	 * The unit corresponding to the quantity of mass.
	 */
	public static final Unit		MASS_UNIT;

	/**
	 * The unit corresponding to the quantity of plane angle.
	 */
	public static final Unit		PLANE_ANGLE_UNIT;

	/**
	 * The unit corresponding to the quantity of solid angle.
	 */
	public static final Unit		SOLID_ANGLE_UNIT;

	/**
	 * The unit corresponding to the quantity of thermodynamic temperature.
	 */
	public static final Unit		THERMODYNAMIC_TEMPERATURE_UNIT;

	/**
	 * The unit corresponding to the quantity of time.
	 */
	public static final Unit		TIME_UNIT;

	/*
	 * Base units:
	 */

	/**
	 * Base unit of electric current. The ampere is that constant current which,
	 * if maintained in two straight parallel conductors of infinite length, of
	 * negligible circular cross section, and placed 1 meter apart in vacuum,
	 * would produce between these conductors a force equal to 2 x 10^-7 newton
	 * per meter of length.
	 */
	public static final BaseUnit	AMPERE;

	/**
	 * Base unit of luminous intensity. The candela is the luminous intensity,
	 * in a given direction, of a source that emits monochromatic radiation of
	 * frequency 540 x 10^12 hertz and that has a radiant intensity in that
	 * direction of (1/683) watt per steradian.
	 */
	public static final BaseUnit	CANDELA;

	/**
	 * Base unit of thermodynamic temperature. The kelvin, unit of thermodynamic
	 * temperature, is the fraction 1/273.16 of the thermodynamic temperature of
	 * the triple point of water.
	 */
	public static final BaseUnit	KELVIN;

	/**
	 * Base unit of mass. The kilogram is the unit of mass; it is equal to the
	 * mass of the international prototype of the kilogram.
	 */
	public static final BaseUnit	KILOGRAM;

	/**
	 * Base unit of length. The meter is the length of the path travelled by
	 * light in vacuum during a time interval of 1/299 792 458 of a second.
	 */
	public static final BaseUnit	METER;							// NIST

	/**
	 * Synonym for <code>METER</code>.
	 */
	public static final BaseUnit	METRE;							// ISO

	/**
	 * Base unit of amount of substance. The mole is the amount of substance of
	 * a system which contains as many elementary entities as there are atoms in
	 * 0.012 kilogram of carbon 12.
	 */
	public static final BaseUnit	MOLE;

	/**
	 * Base unit of time. The second is the duration of 9 192 631 770 periods of
	 * the radiation corresponding to the trasition between the two hyperfine
	 * levels of the ground state of the cesium-133 atom.
	 */
	public static final BaseUnit	SECOND;

	/*
	 * Supplementary units: These units are more accurately referred to as
	 * "dimensionless derived units" -- for the purposes of this Java package,
	 * however, they are BaseUnit-s. Note, however, that their <code>
	 * isDimensionless()</code> method will return <code>true</code>.
	 */

	/**
	 * Base unit of angular measure. The radian is the plane angle between two
	 * radii of a circle that cut off on the circumference an arc equal in
	 * length to the radius.
	 */
	public static final BaseUnit	RADIAN;

	/**
	 * Base unit of solid angle. The steradian is the solid angle that, having
	 * its vertex in the center of a sphere, cuts off an area on the surface of
	 * the sphere equal to that of a square with sides of length equal to the
	 * radius of the sphere.
	 */
	public static final BaseUnit	STERADIAN;

	/*
	 * Derived units with special names and symbols:
	 */

	/**
	 * Special derived unit for the quantity of frequency.
	 */
	public static final Unit		HERTZ;

	/**
	 * Special derived unit for the quantity of force.
	 */
	public static final Unit		NEWTON;

	/**
	 * Special derived unit for the quantity of pressure or stress.
	 */
	public static final Unit		PASCAL;

	/**
	 * Special derived unit for the quantity of energy, work, or quantity of
	 * heat.
	 */
	public static final Unit		JOULE;

	/**
	 * Special derived unit for the quantity of power or radiant flux.
	 */
	public static final Unit		WATT;

	/**
	 * Special derived unit for the quantity of electric charge or quantity of
	 * electricity.
	 */
	public static final Unit		COULOMB;

	/**
	 * Special derived unit for the quantity of electric potential, potential
	 * difference, or electromotive force.
	 */
	public static final Unit		VOLT;

	/**
	 * Special derived unit for the quantity of electric capacitance.
	 */
	public static final Unit		FARAD;

	/**
	 * Special derived unit for the quantity of electric resistance.
	 */
	public static final Unit		OHM;

	/**
	 * Special derived unit for the quantity of electric conductance.
	 */
	public static final Unit		SIEMENS;

	/**
	 * Special derived unit for the quantity of magnetic flux.
	 */
	public static final Unit		WEBER;

	/**
	 * Special derived unit for the quantity of magnetic flux density.
	 */
	public static final Unit		TESLA;

	/**
	 * Special derived unit for the quantity of inductance.
	 */
	public static final Unit		HENRY;

	/**
	 * Special derived unit for the quantity of Celsius temperature.
	 */
	public static final Unit		DEGREE_CELSIUS;

	/**
	 * Special derived unit for the quantity of luminous flux.
	 */
	public static final Unit		LUMEN;

	/**
	 * Special derived unit for the quantity of illuminance.
	 */
	public static final Unit		LUX;

	/*
	 * Derived units with special names and symbols admitted for reasons of
	 * safegarding human health.
	 */

	/**
	 * Special derived unit for the quantity of activity of a radionuclide.
	 */
	public static final Unit		BECQUEREL;

	/**
	 * Special derived unit for the quantity of absorbed dose, specific energy
	 * (imparted), or kerms.
	 */
	public static final Unit		GRAY;

	/**
	 * Special derived unit for the quantity of dose equivalent, ambient dose
	 * equivalent, directional dose equivalent, personal dose equivalent, or
	 * equivalent dose.
	 */
	public static final Unit		SIEVERT;

	/*
	 * Units outside the SI but accepted for use with the SI according to the
	 * USA National Institute of Standards and Technology (see
	 * <http://physics.nist.gov/cuu/Units/>):
	 */

	/**
	 * Special derived unit -- accepted for use with the SI -- for the quantity
	 * of time.
	 */
	public static final Unit		MINUTE;

	/**
	 * Special derived unit -- accepted for use with the SI -- for the quantity
	 * of time.
	 */
	public static final Unit		HOUR;

	/**
	 * Special derived unit -- accepted for use with the SI -- for the quantity
	 * of time.
	 */
	public static final Unit		DAY;

	/**
	 * Special derived unit -- accepted for use with the SI -- for the quantity
	 * of plane angle.
	 */
	public static final Unit		ARC_DEGREE;

	/**
	 * Special derived unit -- accepted for use with the SI -- for the quantity
	 * of plane angle.
	 */
	public static final Unit		ARC_MINUTE;

	/**
	 * Special derived unit -- accepted for use with the SI -- for the quantity
	 * of plane angle.
	 */
	public static final Unit		ARC_SECOND;

	/**
	 * Special derived unit -- accepted for use with the SI -- for the quantity
	 * of volume.
	 */
	public static final Unit		LITER;							// NIST

	/**
	 * Synomym for <code>LITER</code>.
	 */
	public static final Unit		LITRE;							// ISO

	/**
	 * Special derived unit -- accepted for use with the SI -- for the quantity
	 * of mass.
	 */
	public static final Unit		METRIC_TON;					// NIST

	/**
	 * Synomym for <code>METRIC_TON</code>.
	 */
	public static final Unit		TONNE;							// ISO

	/*
	 * Units outside the SI but temporarily accepted for use with the SI
	 * according to the USA National Institute of Standards and Technology (see
	 * <http://physics.nist.gov/cuu/Units/>):
	 */

	/**
	 * Special derived unit -- temporarily accepted for use with the SI -- for
	 * the quantity of length.
	 */
	public static final Unit		NAUTICAL_MILE;

	/**
	 * Special derived unit -- temporarily accepted for use with the SI -- for
	 * the quantity of speed.
	 */
	public static final Unit		KNOT;

	/**
	 * Special derived unit -- temporarily accepted for use with the SI -- for
	 * the quantity of length.
	 */
	public static final Unit		ANGSTROM;

	/**
	 * Special derived unit -- temporarily accepted for use with the SI -- for
	 * the quantity of area.
	 */
	public static final Unit		ARE;

	/**
	 * Special derived unit -- temporarily accepted for use with the SI -- for
	 * the quantity of area.
	 */
	public static final Unit		HECTARE;

	/**
	 * Special derived unit -- temporarily accepted for use with the SI -- for
	 * the quantity of area.
	 */
	public static final Unit		BARN;

	/**
	 * Special derived unit -- temporarily accepted for use with the SI -- for
	 * the quantity of pressure.
	 */
	public static final Unit		BAR;

	/**
	 * Special derived unit -- temporarily accepted for use with the SI -- for
	 * the quantity of acceleration.
	 */
	public static final Unit		GAL;

	/*
	 * Units outside the SI but temporarily accepted for use with the SI until
	 * the year 2000 according to the USA National Institute of Standards and
	 * Technology (see <http://physics.nist.gov/cuu/Units/>):
	 */

	/**
	 * Special derived unit -- temporarily accepted for use with the SI until
	 * the year 2000 according to the USA National Institute of Standards and
	 * Technology -- for the quantity of activity of a radionuclide.
	 */
	public static final Unit		CURIE;

	/**
	 * Special derived unit -- temporarily accepted for use with the SI until
	 * the year 2000 according to the USA National Institute of Standards and
	 * Technology -- for the quantity of electric charge per mass.
	 */
	public static final Unit		ROENTGEN;

	/**
	 * Special derived unit -- temporarilty accepted for use with the SI until
	 * the year 2000 according to the USA National Institute of Standards and
	 * Technology -- for the quantity of absorbed dose.
	 */
	public static final Unit		RAD;

	/**
	 * Special derived unit -- temporarilty accepted for use with the SI until
	 * the year 2000 according to the USA National Institute of Standards and
	 * Technology -- for the quantity of equivalent absorbed dose.
	 */
	public static final Unit		REM;

	static {
		BaseUnit ampere = null;
		BaseUnit candela = null;
		BaseUnit kelvin = null;
		BaseUnit kilogram = null;
		BaseUnit meter = null;
		BaseUnit mole = null;
		BaseUnit second = null;
		BaseUnit radian = null;
		BaseUnit steradian = null;
		Unit hertz = null;
		Unit newton = null;
		Unit pascal = null;
		Unit joule = null;
		Unit watt = null;
		Unit coulomb = null;
		Unit volt = null;
		Unit farad = null;
		Unit ohm = null;
		Unit siemens = null;
		Unit weber = null;
		Unit tesla = null;
		Unit henry = null;
		Unit degree_celsius = null;
		Unit lumen = null;
		Unit lux = null;
		Unit becquerel = null;
		Unit gray = null;
		Unit sievert = null;
		Unit minute = null;
		Unit hour = null;
		Unit day = null;
		Unit arc_degree = null;
		Unit arc_minute = null;
		Unit arc_second = null;
		Unit liter = null;
		Unit metric_ton = null;
		Unit nautical_mile = null;
		Unit knot = null;
		Unit angstrom = null;
		Unit are = null;
		Unit hectare = null;
		Unit barn = null;
		Unit bar = null;
		Unit gal = null;
		Unit curie = null;
		Unit roentgen = null;
		Unit rad = null;
		Unit rem = null;

		try {
			ampere = bu("ampere", "A", BaseQuantity.ELECTRIC_CURRENT);
			candela = bu("candela", "cd", BaseQuantity.LUMINOUS_INTENSITY);
			kelvin = bu("kelvin", "K", BaseQuantity.THERMODYNAMIC_TEMPERATURE);
			kilogram = bu("kilogram", "kg", BaseQuantity.MASS);
			meter = bu("meter", "m", BaseQuantity.LENGTH);
			mole = bu("mole", "mol", BaseQuantity.AMOUNT_OF_SUBSTANCE);
			second = bu("second", "s", BaseQuantity.TIME);
			radian = bu("radian", "rad", BaseQuantity.PLANE_ANGLE);
			steradian = bu("steradian", "sr", BaseQuantity.SOLID_ANGLE);

			hertz = du("hertz", "Hz", second.raiseTo(-1));
			newton = du("newton", "N", kilogram.multiplyBy(meter).divideBy(
					second.raiseTo(2)));
			pascal = du("pascal", "Pa", newton.divideBy(meter.raiseTo(2)));
			joule = du("joule", "J", newton.multiplyBy(meter));
			watt = du("watt", "W", joule.divideBy(second));
			coulomb = du("coulomb", "C", ampere.multiplyBy(second));
			volt = du("volt", "V", watt.divideBy(ampere));
			farad = du("farad", "F", coulomb.divideBy(volt));
			ohm = du("ohm", "Ohm", volt.divideBy(ampere));
			siemens = du("siemens", "S", ohm.raiseTo(-1));
			weber = du("weber", "Wb", volt.multiplyBy(second));
			tesla = du("tesla", "T", weber.divideBy(meter.raiseTo(2)));
			henry = du("henry", "H", weber.divideBy(ampere));
			degree_celsius = du("degree celsius", "Cel", new OffsetUnit(kelvin,
					273.15));
			lumen = du("lumen", "lm", candela.multiplyBy(steradian));
			lux = du("lux", "lx", lumen.divideBy(meter.raiseTo(2)));

			becquerel = du("becquerel", "Bq", hertz);
			gray = du("gray", "Gy", joule.divideBy(kilogram));
			sievert = du("sievert", "Sv", joule.divideBy(kilogram));

			minute = du("minute", "min", new ScaledUnit(60, second));
			hour = du("hour", "h", new ScaledUnit(60, minute));
			day = du("day", "d", new ScaledUnit(24, hour));
			arc_degree = du("arc degree", "deg", new ScaledUnit(Math.PI / 180,
					radian));
			arc_minute = du("arc minute", "'", new ScaledUnit(1. / 60.,
					arc_degree));
			arc_second = du("arc second", "\"", new ScaledUnit(1. / 60.,
					arc_minute));
			liter = du("liter", "L", new ScaledUnit(1e-3, meter.raiseTo(3)));
			// exact. However, from 1901 to 1964, 1 liter = 1.000028 dm3
			metric_ton = du("metric ton", "t", new ScaledUnit(1e3, kilogram));

			nautical_mile = du("nautical mile", "nmi", new ScaledUnit(1852,
					meter));
			knot = du("knot", "kt", nautical_mile.divideBy(hour));
			angstrom = du("angstrom", null, new ScaledUnit(1e-10, meter));
			are = du("are", "are", new ScaledUnit(10, meter).raiseTo(2));
			hectare = du("hectare", "ha", new ScaledUnit(100, are));
			barn = du("barn", "b", new ScaledUnit(1e-28, meter.raiseTo(2)));
			bar = du("bar", "bar", new ScaledUnit(1e5, pascal));
			gal = du("gal", "Gal", new ScaledUnit(1e-2, meter).divideBy(second
					.raiseTo(2)));

			curie = du("curie", "Ci", new ScaledUnit(3.7e10, becquerel));
			roentgen = du("roentgen", "R", new ScaledUnit(2.58e-4, coulomb
					.divideBy(kilogram)));
			rad = du("rad", "rd", new ScaledUnit(1e-2, gray));
			rem = du("rem", "rem", new ScaledUnit(1e-2, sievert));
		}
		catch (final UnitException e) {
			final String reason = e.getMessage();
			System.err.println("Couldn't initialize class SI" + reason == null
					? ""
					: (": " + reason));
		}

		AMOUNT_OF_SUBSTANCE_UNIT = mole;
		ELECTRIC_CURRENT_UNIT = ampere;
		LENGTH_UNIT = meter;
		LUMINOUS_INTENSITY_UNIT = candela;
		MASS_UNIT = kilogram;
		PLANE_ANGLE_UNIT = radian;
		SOLID_ANGLE_UNIT = steradian;
		THERMODYNAMIC_TEMPERATURE_UNIT = kelvin;
		TIME_UNIT = second;

		AMPERE = ampere;
		CANDELA = candela;
		KELVIN = kelvin;
		KILOGRAM = kilogram;
		METER = meter;
		METRE = METER;
		MOLE = mole;
		SECOND = second;
		RADIAN = radian;
		STERADIAN = steradian;
		HERTZ = hertz;
		NEWTON = newton;
		PASCAL = pascal;
		JOULE = joule;
		WATT = watt;
		COULOMB = coulomb;
		VOLT = volt;
		FARAD = farad;
		OHM = ohm;
		SIEMENS = siemens;
		WEBER = weber;
		TESLA = tesla;
		HENRY = henry;
		DEGREE_CELSIUS = degree_celsius;
		LUMEN = lumen;
		LUX = lux;
		BECQUEREL = becquerel;
		GRAY = gray;
		SIEVERT = sievert;
		MINUTE = minute;
		HOUR = hour;
		DAY = day;
		ARC_DEGREE = arc_degree;
		ARC_MINUTE = arc_minute;
		ARC_SECOND = arc_second;
		LITER = liter;
		LITRE = LITER;
		METRIC_TON = metric_ton;
		TONNE = METRIC_TON;
		NAUTICAL_MILE = nautical_mile;
		KNOT = knot;
		ANGSTROM = angstrom;
		ARE = are;
		HECTARE = hectare;
		BARN = barn;
		BAR = bar;
		GAL = gal;
		CURIE = curie;
		ROENTGEN = roentgen;
		RAD = rad;
		REM = rem;
	}

	/**
	 * Factory method for constructing a base unit.
	 * 
	 * @param name
	 *            The name of the unit.
	 * @param symbol
	 *            The symbol for the unit.
	 * @param quantity
	 *            The base quantity of the unit.
	 * @return The base unit corresponding to the arguments.
	 */
	private static BaseUnit bu(final String name, final String symbol,
			final BaseQuantity quantity) throws NameException,
			UnitExistsException {
		return BaseUnit.getOrCreate(UnitName.newUnitName(name, null, symbol),
				quantity);
	}

	/**
	 * Factory method for constructing a derived unit.
	 * 
	 * @param name
	 *            The name of the unit.
	 * @param symbol
	 *            The symbol for the unit.
	 * @param quantity
	 *            The definition of the unit.
	 * @return The derived unit corresponding to the arguments.
	 */
	private static Unit du(final String name, final String symbol,
			final Unit definition) throws NameException {
		return definition.clone(UnitName.newUnitName(name, null, symbol));
	}

	/**
	 * Constructs an SI system of units from nothing.
	 */
	private SI() throws UnitExistsException, NameException, PrefixDBException,
			NoSuchUnitException {
		super(baseUnitDB(), derivedUnitDB());
	}

	/**
	 * Returns the base unit database of the SI.
	 * 
	 * @return The base unit database of the SI.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws UnitExistsException
	 *             Attempt to redefine an existing unit.
	 * @throws NoSuchUnitException
	 *             Necessary unit not found.
	 */
	private static UnitDBImpl baseUnitDB() throws NameException,
			UnitExistsException, NoSuchUnitException {
		final UnitDBImpl db = new UnitDBImpl(9, 9);
		db.addUnit(AMPERE);
		db.addUnit(CANDELA);
		db.addUnit(KELVIN);
		db.addUnit(KILOGRAM);
		db.addUnit(METER);
		db.addUnit(MOLE);
		db.addUnit(SECOND);
		db.addUnit(RADIAN);
		db.addUnit(STERADIAN);
		db.addAlias("metre", "meter");
		return db;
	}

	/**
	 * Returns the derived unit database of the SI.
	 * 
	 * @return The derived unit database of the SI.
	 * @throws NameException
	 *             Bad unit name.
	 * @throws UnitExistsException
	 *             Attempt to redefine an existing unit.
	 * @throws NoSuchUnitException
	 *             Necessary unit not found.
	 */
	private static UnitDBImpl derivedUnitDB() throws NameException,
			UnitExistsException, NoSuchUnitException {
		final UnitDBImpl db = new UnitDBImpl(42, 43);

		db.addUnit(HERTZ);
		db.addUnit(NEWTON);
		db.addUnit(PASCAL);
		db.addUnit(JOULE);
		db.addUnit(WATT);
		db.addUnit(COULOMB);
		db.addUnit(VOLT);
		db.addUnit(FARAD);
		db.addUnit(OHM);
		db.addUnit(SIEMENS);
		db.addUnit(WEBER);
		db.addUnit(TESLA);
		db.addUnit(HENRY);
		db.addUnit(DEGREE_CELSIUS);
		db.addUnit(LUMEN);
		db.addUnit(LUX);

		db.addUnit(BECQUEREL);
		db.addUnit(GRAY);
		db.addUnit(SIEVERT);

		db.addUnit(MINUTE);
		db.addUnit(HOUR);
		db.addUnit(DAY);
		db.addUnit(ARC_DEGREE);
		db.addUnit(ARC_MINUTE);
		db.addUnit(ARC_SECOND);
		db.addUnit(LITER);
		db.addUnit(METRIC_TON);

		db.addUnit(NAUTICAL_MILE);
		db.addUnit(KNOT);
		db.addUnit(ANGSTROM);
		db.addUnit(ARE);
		db.addUnit(HECTARE);
		db.addUnit(BARN);
		db.addUnit(BAR);
		db.addUnit(GAL);

		db.addUnit(CURIE);
		db.addUnit(ROENTGEN);
		db.addUnit(RAD);
		db.addUnit(REM);

		db.addAlias("litre", "liter", "l");
		db.addAlias("tonne", "metric ton");
		db.addSymbol("tne", "tonne");

		return db;
	}

	/**
	 * Returns an instance of the SI system of units.
	 * 
	 * @return An instance of the SI system of units.
	 * @throws UnitSystemException
	 *             Couldn't create an instance of the SI system of units.
	 */
	public static SI instance() throws UnitSystemException {
		synchronized (SI.class) {
			if (si == null) {
				try {
					si = new SI();
				}
				catch (final UnitException e) {
					throw new UnitSystemException(
							"Couldn't initialize class SI", e);
				}
			}
		}
		return si;
	}

	/**
	 * Tests this class.
	 */
	public static void main(final String[] args) throws Exception {
		SI.instance();
	}
}
