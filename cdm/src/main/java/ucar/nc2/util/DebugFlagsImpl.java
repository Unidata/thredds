/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util;

import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * An implementation of DebugFlags
 *
 * @author caron
 */
public class DebugFlagsImpl implements DebugFlags {

  private Map<String,Boolean> map = new HashMap<>();

  /**
   * All flags are off
   */
  public DebugFlagsImpl() {
  }

  /**
   * Constructor.
   * @param flagsOn space-separated list of flags to turn on.
   */
  public DebugFlagsImpl(String flagsOn) {
    StringTokenizer stoke = new StringTokenizer(flagsOn);
    while (stoke.hasMoreTokens())
      set(stoke.nextToken(), true);
  }

  public boolean isSet(String flagName) {
    Boolean b  = map.get(flagName);
    return (b != null) && b;
  }

  public void set(String flagName, boolean value) {
    map.put(flagName, value);
  }

  public String getSetFlags() {
    return map.keySet().toString();
  }

}
