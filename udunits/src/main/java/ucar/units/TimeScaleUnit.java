/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.annotation.concurrent.Immutable;
import java.util.Date;

/**
 * Provides support for a reference time unit whose origin is at a certain time.
 * <p/>
 * Instances of this class are immutable.
 *
 * @author Steven R. Emmerson
 */
@Immutable
public final class TimeScaleUnit extends UnitImpl {
  private static final long serialVersionUID = 1L;

  /**
   * The date formatter.
   *
   * @serial
   */
  static final DateTimeFormatter df_units = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS 'UTC'").withZoneUTC(); // joda-time

  /**
   * The second unit.
   */
  static final BaseUnit SECOND;

  static {
    try {
      SECOND = BaseUnit.getOrCreate(UnitName.newUnitName("second", null, "s"), BaseQuantity.TIME);
    } catch (final Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  ///////////////////////////////////////////////////////


  /**
   * The reference time unit.
   *
   * @serial
   */
  private final Unit _unit;

  /**
   * The time origin for this instance.
   *
   * @serial
   */
  private final Date _origin;


  /**
   * Constructs from a reference unit and a time origin.
   *
   * @param unit   The reference time unit.
   * @param origin The time origin.
   * @throws BadUnitException <code>unit</code> is not a unit of time.
   */
  public TimeScaleUnit(final Unit unit, final Date origin)
          throws BadUnitException, UnitSystemException {
    this(unit, origin, null);
  }

  /**
   * Constructs from a reference unit, a time origin, and an identifier.
   *
   * @param unit   The reference time unit.
   * @param origin The time origin.
   * @param id     The identifier.
   * @throws BadUnitException <code>unit</code> is not a unit of time.
   */
  public TimeScaleUnit(final Unit unit, final Date origin, final UnitName id)
          throws BadUnitException, UnitSystemException {
    super(id);
    if (!unit.isCompatible(UnitSystemManager.instance().getBaseUnit(
            BaseQuantity.TIME))) {
      throw new BadUnitException("\"" + unit + "\" is not a unit of time");
    }
    _unit = unit;
    _origin = origin;
  }

  static Unit getInstance(final Unit unit, final Date origin)
          throws ShiftException {
    try {
      return unit instanceof TimeScaleUnit
              ? new TimeScaleUnit(((TimeScaleUnit) unit)._unit, origin)
              : new TimeScaleUnit(unit, origin);
    } catch (final Exception e) {
      throw (ShiftException) new ShiftException(unit, origin)
              .initCause(e);
    }
  }

  /**
   * Returns the reference unit.
   *
   * @return The reference unit.
   */
  public Unit getUnit() {
    return _unit;
  }

  /**
   * Returns the time origin.
   *
   * @return The time origin.
   */
  public Date getOrigin() {
    return _origin;
  }

    /*
     * From UnitImpl:
     */

  /**
   * Clones this unit, changing the identifier.
   *
   * @param id The new identifier.
   * @return This unit with the new identifier.
   */
  public Unit clone(final UnitName id) {
    Unit clone;
    try {
      clone = new TimeScaleUnit(getUnit(), getOrigin(), id);
    } catch (final UnitException e) {
      clone = null; // can't happen
    }
    return clone;
  }

  @Override
  public Unit shiftTo(final double origin) throws ShiftException {
    Date newOrigin;
    try {
      newOrigin = new Date(_origin.getTime()
              + (long) (_unit.convertTo(origin, SECOND) * 1000));
    } catch (final ConversionException e) {
      throw (ShiftException) new ShiftException(this, origin)
              .initCause(e);
    }
    try {
      return new TimeScaleUnit(_unit, newOrigin);
    } catch (final BadUnitException e) {
      throw new AssertionError();
    } catch (final UnitSystemException e) {
      throw (ShiftException) new ShiftException(this, origin)
              .initCause(e);
    }
  }

  @Override
  public Unit shiftTo(final Date origin) throws ShiftException {
    return getInstance(_unit, origin);
  }

  /**
   * Multiplies this unit by another unit. This operation is invalid.
   *
   * @param that The other unit.
   * @return The product of multiplying this unit by the other unit.
   * @throws MultiplyException Illegal operation. Always thrown.
   */
  @Override
  protected Unit myMultiplyBy(final Unit that) throws MultiplyException {
    throw new MultiplyException(this);
  }

  /**
   * Divides this unit by another unit. This operation is invalid.
   *
   * @param that The other unit.
   * @return The quotient of dividing this unit by the other unit.
   * @throws DivideException Illegal operation. Always thrown.
   */
  @Override
  protected Unit myDivideBy(final Unit that) throws DivideException {
    throw new DivideException(this);
  }

  /**
   * Divides this unit into another unit. This operation is invalid.
   *
   * @param that The other unit.
   * @return The quotient of dividing this unit into the other unit.
   * @throws DivideException Illegal operation. Always thrown.
   */
  @Override
  protected Unit myDivideInto(final Unit that) throws DivideException {
    throw new DivideException(that, this);
  }

  /**
   * Raises this unit to a power. This operation is invalid.
   *
   * @param power The power.
   * @return The result of raising this unit to the power.
   * @throws RaiseException Illegal operation. Always thrown.
   */
  @Override
  protected Unit myRaiseTo(final int power) throws RaiseException {
    throw new RaiseException(this);
  }

  /**
   * Returns the derived unit underlying the reference time unit.
   *
   * @return The derived unit underlying the reference time unit.
   */
  public DerivedUnit getDerivedUnit() {
    return getUnit().getDerivedUnit();
  }

  /**
   * Provides support for Converter-s.
   */
  protected static final class MyConverter extends ConverterImpl {
    private final double offset;
    private final Converter converter;

    protected MyConverter(final TimeScaleUnit fromUnit, final Unit toUnit)
            throws ConversionException {
      super(fromUnit, toUnit);
      converter = fromUnit.getUnit().getConverterTo(
              ((TimeScaleUnit) toUnit).getUnit());
      offset = SI.SECOND.convertTo(
              (fromUnit.getOrigin().getTime() - ((TimeScaleUnit) toUnit)
                      .getOrigin().getTime()) / 1000.0,
              ((TimeScaleUnit) toUnit).getUnit());
    }

    public double convert(final double amount) {
      return converter.convert(amount) + offset;
    }

    public float[] convert(final float[] input, float[] output) {
      output = converter.convert(input, output);
      for (int i = input.length; --i >= 0; ) {
        output[i] += offset;
      }
      return output;
    }

    public double[] convert(final double[] input, double[] output) {
      output = converter.convert(input, output);
      for (int i = input.length; --i >= 0; ) {
        output[i] += offset;
      }
      return output;
    }
  }

  /**
   * Returns a Converter for converting numeric values from this unit to
   * another unit.
   *
   * @param outputUnit The other unit. Shall be a TimeScaleUnit.
   * @return A Converter.
   * @throws ConversionException <code>outputUnit</code> is not a TimeScaleUnit.
   */
  @Override
  public Converter getConverterTo(final Unit outputUnit)
          throws ConversionException {
    return new MyConverter(this, outputUnit);
  }

  /**
   * Indicates if numeric values in this unit are convertible to another unit.
   *
   * @param that The other unit.
   * @return <code>true</code> if and only if numeric values in this unit are
   * convertible to <code>
   * that</code>.
   */
  @Override
  public final boolean isCompatible(final Unit that) {
    return that instanceof TimeScaleUnit;
  }

  /**
   * Indicates if this unit is semantically identical to an object.
   *
   * @param object The object.
   * @return <code>true</code> if and only if this unit is semantically
   * identical to <code>object
   * </code>.
   */
  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof TimeScaleUnit)) {
      return false;
    }
    final TimeScaleUnit that = (TimeScaleUnit) object;
    return _origin.equals(that._origin) && _unit.equals(that._unit);
  }

  /**
   * Returns the hash code of this instance.
   *
   * @return The hash code of this instance.
   */
  @Override
  public int hashCode() {
    return getUnit().hashCode() ^ getOrigin().hashCode();
  }

  /**
   * Indicates if this unit is dimensionless. TimeScaleUnit-s are never
   * dimensionless.
   *
   * @return <code>false</code>.
   */
  public boolean isDimensionless() {
    return false; // a TimeScaleUnit is never dimensionless by definition
  }

  /**
   * Returns the string representation of this unit.
   *
   * @return The string representation of this unit.
   */
  @Override
  public String toString() {
    final String string = super.toString(); // get symbol or name
    return string != null
            ? string
            : getCanonicalString();
  }

  /**
   * Returns the canonical string representation of the unit.
   *
   * @return The canonical string representation.
   */
  public String getCanonicalString() {
      /* change this, not thread-safe; require dependency of joda-time (!)
             dateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
             dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
             dateFormat.applyPattern(" 'since' yyyy-MM-dd HH:mm:ss.SSS 'UTC'");
       */
    return getUnit().toString() + " since " + df_units.print(getOrigin().getTime());
  }

}
