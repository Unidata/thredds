/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import ucar.units.*;

/**
 * Convenience routines on top of ucar.units package.
 * <p/>
 * The ucar.units package handles <ol>
 * <li> scientific units, which are factors of the fundamental
 * dimensions such as length, time, mass, etc
 * <li> dates, represented as "n units of time since reference date" eg
 * "1203 days since 1970-01-01 00:00:00"
 * </ol>
 *
 * @author caron
 */

public class SimpleUnit {
  public static final SimpleUnit kmUnit;
  public static final SimpleUnit meterUnit;
  public static final SimpleUnit pressureUnit;

  //static protected UnitFormat format;
  static protected Unit secsUnit, dateReferenceUnit;
  static protected boolean debugParse = false;

  static {
    try {
      UnitFormat udunit = UnitFormatManager.instance();
      secsUnit = udunit.parse("sec");
      dateReferenceUnit = udunit.parse("ms since 1970-01-01");

      // aliasing
      UnitDB unitDB = UnitDBManager.instance();
      Unit u = udunit.parse("millibar");
      Unit alias = u.clone(UnitName.newUnitName("mb"));
      unitDB.addUnit(alias);

      kmUnit = SimpleUnit.factoryWithExceptions("km");
      meterUnit = SimpleUnit.factoryWithExceptions("m");
      pressureUnit = SimpleUnit.factoryWithExceptions("Pa");

    } catch (Exception e) {
      System.out.println("SimpleUnit initialization failed " + e);
      throw new RuntimeException("SimpleUnit initialization failed " + e);
    }
  }

  /**
   * Create a SimpleUnit from the given name, catch Exceptions.
   *
   * @param name parse this name to create a unit.
   * @return SimpleUnit, DateUnit, TimeUnit, or null if failed
   * @see ucar.units.UnitFormat#parse
   */
  static public SimpleUnit factory(String name) {
    try {
      return factoryWithExceptions(name);
    } catch (Exception e) {
      if (debugParse) System.out.println("Parse " + name + " got Exception " + e);
      return null;
    }
  }

  /**
   * Create a SimpleUnit from the given name, allow Exceptions.
   *
   * @param name parse this name to create a unit.
   * @return SimpleUnit, DateUnit, or TimeUnit
   * @throws Exception when date parser fails
   * @see ucar.units.UnitFormat#parse
   */
  static public SimpleUnit factoryWithExceptions(String name) throws UnitException {
    UnitFormat format = UnitFormatManager.instance();
    Unit uu = format.parse(name);
    //if (isDateUnit(uu)) return new DateUnit(name);
    if (isTimeUnit(uu)) return new TimeUnit(name);
    return new SimpleUnit(uu);
  }

  // need subclass access
  static protected Unit makeUnit(String name) throws UnitException {
    UnitFormat format = UnitFormatManager.instance();
    return format.parse(name);
  }

  /**
   * Return true if unitString1 is compatible to unitString2, meaning one can be converted to the other.
   * If either unit string is illegal, return false.
   *
   * @param unitString1 compare this unit
   * @param unitString2 compare this unit
   * @return true if the 2 units are compatible
   */
  static public boolean isCompatible(String unitString1, String unitString2) {
    Unit uu1, uu2;
    try {
      UnitFormat format = UnitFormatManager.instance();
      uu1 = format.parse(unitString1);
    } catch (Exception e) {
      if (debugParse) System.out.println("Parse " + unitString1 + " got Exception1 " + e);
      return false;
    }

    try {
      UnitFormat format = UnitFormatManager.instance();
      uu2 = format.parse(unitString2);
    } catch (Exception e) {
      if (debugParse) System.out.println("Parse " + unitString2 + " got Exception2 " + e);
      return false;
    }

    //System.out.println("udunits isCompatible "+ uu1+ " "+ uu2);
    return uu1.isCompatible(uu2);
  }

  /**
   * Return true if  unitString1 is convertible to unitString2
   *
   * @param unitString1 compare this unit
   * @param unitString2 compare this unit
   * @return true if the 2 units are compatible
   * @throws UnitException if units parsing fails
   */
  static public boolean isCompatibleWithExceptions(String unitString1, String unitString2) throws UnitException {
    UnitFormat format = UnitFormatManager.instance();
    Unit uu1 = format.parse(unitString1);
    Unit uu2 = format.parse(unitString2);
    return uu1.isCompatible(uu2);
  }

  /**
   * Return true if this ucar.units.Unit is a Date.
   *
   * @param uu check this Unit
   * @return true if its a Date
   */
  static public boolean isDateUnit(ucar.units.Unit uu) {
    boolean ok = uu.isCompatible(dateReferenceUnit);
    if (!ok) return false;
    try {
      uu.getConverterTo(dateReferenceUnit);
      return true;
    } catch (ConversionException e) {
      return false;
    }
  }

  /**
   * Return true if this ucar.units.Unit is convertible to secs.
   *
   * @param uu check this Unit
   * @return true if its a Time
   */
  static public boolean isTimeUnit(ucar.units.Unit uu) {
    return uu.isCompatible(secsUnit);
  }

  /**
   * Return true if the given unit is convertible to a date Unit.
   * allowed format is something like:
   * <pre>[-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]</pre>
   *
   * @param unitString check this unit string
   * @return true if its a Date
   */
  static public boolean isDateUnit(String unitString) {
    SimpleUnit su = factory(unitString);
    return su != null && isDateUnit(su.getUnit());
  }

  /**
   * Return true if the given unit is a time Unit, eg "seconds".
   *
   * @param unitString check this unit string
   * @return true if its a Time
   */
  static public boolean isTimeUnit(String unitString) {
    SimpleUnit su = factory(unitString);
    return su != null && isTimeUnit(su.getUnit());
  }

  /**
   * Get the conversion factor to convert inputUnit to outputUnit.
   *
   * @param inputUnitString  inputUnit in string form
   * @param outputUnitString outputUnit in string form
   * @return conversion factor
   * @throws IllegalArgumentException if not convertible
   */
  static public double getConversionFactor(String inputUnitString, String outputUnitString) throws IllegalArgumentException {
    SimpleUnit inputUnit = SimpleUnit.factory(inputUnitString);
    SimpleUnit outputUnit = SimpleUnit.factory(outputUnitString);
    return inputUnit.convertTo(1.0, outputUnit);
  }

  ////////////////////////////////////////////////
  protected ucar.units.Unit uu = null;

  /**
   * for subclasses.
   */
  protected SimpleUnit() {
  }

  /**
   * Wrap a ucar.units.Unit in a SimpleUnit. Use factory().
   *
   * @param uu wrap this Unit
   */
  SimpleUnit(ucar.units.Unit uu) {
    this.uu = uu;
  }

  /**
   * Unit string representation.
   */
  public String toString() {
    return uu.toString();
  }

  /**
   * Get underlying ucar.units.Unit.
   *
   * @return underlying ucar.units.Unit.
   */
  public Unit getUnit() {
    return uu;
  }

  /**
   * Convert given value of this unit to the new unit.
   *
   * @param value  value in this unit
   * @param outputUnit convert to this unit
   * @return  value in outputUnit
   * @throws IllegalArgumentException if outputUnit not convertible from this unit
   */
  public double convertTo(double value, SimpleUnit outputUnit) throws IllegalArgumentException {
    try {
      return uu.convertTo(value, outputUnit.getUnit());
    } catch (ConversionException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  /** Divide this unit by given unit, creat a new unit to hold result.
   public SimpleUnit divideBy(SimpleUnit denom) throws OperationException {
   return new SimpleUnit( uu.divideBy( denom.uu));
   } */

  /**
   * Return true if unitString1 is compatible to unitString2,
   * meaning one can be converted to the other.
   * If either unit string is illegal, return false.
   * @param unitString check if this is compatible with unitString
   * @return true if compatible
   */
  public boolean isCompatible(String unitString) {
    Unit uuWant;
    try {
      UnitFormat format = UnitFormatManager.instance();
      uuWant = format.parse(unitString);
    } catch (Exception e) {
      if (debugParse) System.out.println("Parse " + unitString + " got Exception1 " + e);
      return false;
    }

    return uu.isCompatible(uuWant);
  }


  /**
   * Is this an instance of an UnknownUnit?
   *
   * @return true if an instance of an UnknownUnit
   */
  public boolean isUnknownUnit() {
    ucar.units.Unit uu = getUnit();
    if (uu instanceof ucar.units.UnknownUnit)
      return true;
    if (uu instanceof ucar.units.DerivedUnit)
      return isUnknownUnit((ucar.units.DerivedUnit) uu);
    if (uu instanceof ucar.units.ScaledUnit) {
      ucar.units.ScaledUnit scu = (ucar.units.ScaledUnit) uu;
      Unit u = scu.getUnit();
      if (u instanceof ucar.units.UnknownUnit)
        return true;

      if (u instanceof ucar.units.DerivedUnit)
        return isUnknownUnit((ucar.units.DerivedUnit) u);
    }
    return false;
  }

  private boolean isUnknownUnit(ucar.units.DerivedUnit du) {
    UnitDimension dim = du.getDimension();
    for (Factor f : dim.getFactors()) {
      if (f.getBase() instanceof ucar.units.UnknownUnit)
        return true;
    }
    return false;
  }

  /**
   * Extract the value, can only be called for ScaledUnit.
   * @return value of this unit if ScaledUnit, else NaN
   */
  public double getValue() {
    if (!(uu instanceof ScaledUnit)) return Double.NaN;
    ScaledUnit offset = (ScaledUnit) uu;
    return offset.getScale();
  }

  /**
   * Extract the simple unit string (no number), eg "s" or "m".
   * @return unit string with no value
   */
  public String getUnitString() {
    return uu.getDerivedUnit().toString();
  }

  public String getCanonicalString() {
    return uu.getCanonicalString();
  }

  public String getImplementingClass() {
    return uu.getClass().getName();
  }

}
