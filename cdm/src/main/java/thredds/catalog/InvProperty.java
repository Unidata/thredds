/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package thredds.catalog;


/**
 * A Property is a name/value pair.
 *
 * @author john caron
 */

public class InvProperty {

  private String name, value;

  public InvProperty() {
  }

  /**
   * Constructor
   *
   * @param name  name
   * @param value value
   */
  public InvProperty(String name, String value) {
    this.name = name;
    this.value = value;
  }

  /**
   * Get the name of the property.
   *
   * @return the name of the property.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the value of the property.
   *
   * @return the value of the property.
   */
  public String getValue() {
    return value;
  }

  /**
   * Set the value of the property.
   * @param value set to this value
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * string representation
   */
  public String toString() {
    return "<" + name + "> <" + value + ">";
  }

  /**
   * InvProperty elements with same name are equal.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InvProperty)) return false;
    return o.hashCode() == this.hashCode();
  }

  /**
   * Override Object.hashCode() to be consistent with equals.
   */
  public int hashCode() {
    return getName().hashCode();
  }

}