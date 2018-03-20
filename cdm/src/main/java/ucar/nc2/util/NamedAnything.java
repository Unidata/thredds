/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util;

/**
 * NamedObject implementation
 *
 * @author caron
 * @since Apr 1, 2010
 */

public class NamedAnything implements NamedObject {
  private Object value;
  private String desc;

  public NamedAnything(Object value, String desc) {
    this.value = value;
    this.desc = desc;
  }

  public String getName() {
    return value.toString();
  }

  public String getDescription() {
    return desc;
  }

  public String toString() {
    return value.toString();
  }

  public Object getValue() {
    return value;
  }

}
