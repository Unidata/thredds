/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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