/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.util;

/**
 * An object that has a name and a description.
 * @author caron
 */

public interface NamedObject {

  /** Get the object's name
   * @return object's name
   */
  String getName();

  /** Get the object's description. Use as a tooltip, for example
   * @return object's description
   */
  String getDescription();

  // the object itself
  Object getValue();

}