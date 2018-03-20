/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import javax.annotation.concurrent.Immutable;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Provides support for unknown base units. This can be used, for example, to
 * accomodate an unknown unit (e.g. "foo"). Values in such a unit will only be
 * convertible with units derived from "foo" (e.g. "20 foo").
 * 
 * @author Steven R. Emmerson
 */
@Immutable
public final class UnknownUnit extends BaseUnit {
  private static final long serialVersionUID = 1L;
  /**
   * The name-to-unit map.
   *
   * @serial
   */
  private static final SortedMap<String, UnknownUnit> map = new TreeMap<>();

  /**
   * Constructs from a name.
   *
   * @param name The name of the unit.
   */
  private UnknownUnit(final String name) throws NameException {
    super(UnitName.newUnitName(name, null, name), BaseQuantity.UNKNOWN);
  }

  /**
   * Factory method for constructing an unknown unit from a name.
   *
   * @param name The name of the unit.
   * @return The unknown unit.
   * @throws NameException <code>name == null</code>.
   */
  public static UnknownUnit create(String name) throws NameException {
    UnknownUnit unit;
    name = name.toLowerCase();
    synchronized (map) {
      unit = map.get(name);
      if (unit == null) {
        unit = new UnknownUnit(name);
        map.put(unit.getName(), unit);
        map.put(unit.getPlural(), unit);
      }
    }
    return unit;
  }

    /*
     * From Unit:
     */

  /**
   * Indicates if this unit is semantically identical to an object.
   *
   * @param object The object.
   * @return <code>true</code> if and only if this instance is semantically
   * identical to the object.
   */
  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof UnknownUnit)) {
      return false;
    }
    final UnknownUnit that = (UnknownUnit) object;
    return getName().equalsIgnoreCase(that.getName());
  }

  /**
   * Returns the hash code of this instance.
   *
   * @return The hash code of this instance.
   */
  @Override
  public int hashCode() {
    return getName().toLowerCase().hashCode();
  }

  /**
   * Indicates if this unit is dimensionless. An unknown unit is never
   * dimensionless.
   *
   * @return <code>false</code> always.
   */
  @Override
  public boolean isDimensionless() {
    return false;
  }


}
