/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

/**
 * Container of Attributes
 *
 * @author caron
 * @since 3/20/14
 */
public interface AttributeContainer {

  /**
   * Returns the list of attributes for this variable.
   * @return list of attributes, immutable
   */
  java.util.List<Attribute> getAttributes();

  /**
   * Add all; replace old if has same name
   */
  void addAll(Iterable<Attribute> atts);

  /**
   * Add new or replace old if has same name
   * @param att add this Attribute
   * @return the added attribute
   */
  Attribute addAttribute(Attribute att);


  /**
   * Find a String-valued Attribute by Attribute name (ignore case), return the (string) value of the Attribute.
   * @return the attribute value, or defaultValue if not found
   */
  String findAttValueIgnoreCase(String attName, String defaultValue);

  Attribute findAttribute(String attName);

  Attribute findAttributeIgnoreCase(String attName);

  String getName();


  /**
   * Remove an Attribute : uses the attribute hashCode to find it.
   *
   * @param a remove this attribute
   * @return true if was found and removed
   */
  boolean remove(Attribute a);

  /**
   * Remove an Attribute by name.
   *
   * @param attName if exists, remove this attribute
   * @return true if was found and removed
   */
  boolean removeAttribute(String attName);

  /**
   * Remove an Attribute by name, ignoring case
   *
   * @param attName if exists, remove this attribute
   * @return true if was found and removed
   */
  boolean removeAttributeIgnoreCase(String attName);

}
