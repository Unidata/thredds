/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;

/**
 * Client catalog name/value pair
 *
 * @author caron
 * @since 1/7/2015
 */
@Immutable
public class Property {
  private final String name;
  private final String value;

  public Property(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Property property = (Property) o;

    if (!name.equals(property.name)) return false;
    return !(value != null ? !value.equals(property.value) : property.value != null);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  // first one override
  public static List<Property> removeDups(List<Property> org) {
    List<Property> result = new ArrayList<>(org.size());
    for (Property p : org)
      if (!result.contains(p))  // O(n**2)
        result.add(p);
    return result;
  }

  public static List<Property> convertToProperties(AttributeContainer from) {
    List<Property> result = new ArrayList<>();
    for (Attribute p : from.getAttributes())
      result.add(new Property(p.getShortName(), p.getStringValue()));
    return result;
  }

  @Override
  public String toString() {
    return "Property{" +
            "name='" + name + '\'' +
            ", value='" + value + '\'' +
            '}';
  }
}
