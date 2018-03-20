/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util;

/**
 * Interface for global debug flags.
 * Allows decoupling of packages.
 *
 * @author John Caron
 */
public interface DebugFlags {

  /** Test if named debug flag is set.
   * @param flagName name of flag
   * @return true if named flag is set true
   */
  boolean isSet(String flagName);

  /** Set named debug flag.
   * @param flagName set this flag
   * @param value set to this value
   */
  void set(String flagName, boolean value);

  /**
   *  Return the string representing the current debug flag(s) set. Flags can be either
   *  or false - they just need to be set.
   */
  String getSetFlags();

}