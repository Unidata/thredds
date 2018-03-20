/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import ucar.ma2.DataType;
import ucar.nc2.ft.FeatureDataset;

import java.util.List;

/**
 * A lightweight abstractions of a Variable.
 *
 * @author caron
 * @see FeatureDataset
 */
public interface VariableSimpleIF extends Comparable<VariableSimpleIF> {

  /**
   * Name of the data Variable.
   * Not that this is technically ambiguous v-a-v short or full name;
   * however, since this is a Variable interface, one must assume
   * that it is intended to be getFullName().
   *
   * @return name of the data Variable
   * @deprecated use getFullName or getShortName
   */

    @Deprecated
    String getName();

  /**
   * full, backslash escaped name of the data Variable
   * @return full, backslash escaped name of the data Variable
   */
  String getFullName();

  /**
   * short name of the data Variable
   * @return short name of the data Variable
   */
  String getShortName();

  /**
   * description of the Variable
   * @return description of the Variable, or null if none.
   */
  String getDescription();

  /**
   * Units of the Variable. These should be udunits compatible if possible
   * @return Units of the Variable, or null if none.
   */
  String getUnitsString();

  /**
   * Variable rank
   * @return Variable rank
   */
  int getRank();

  /**
   * Variable shape
   * @return Variable shape
   */
  int[] getShape();

  /**
   * Dimension List. empty for a scalar variable.
   * @return List of ucar.nc2.Dimension
   */
  List<Dimension> getDimensions();

  /**
   * Variable's data type
   * @return Variable's data type
   */
  DataType getDataType();

  /**
   * Attributes for the variable.
   * @return List of type ucar.nc2.Attribute
   */
  List<Attribute> getAttributes();

  /**
   * find the attribute for the variable with the given name, ignoring case.
   * @param name attribute name
   * @return the attribute for the variable with the given name, or null if not found.
   */
  ucar.nc2.Attribute findAttributeIgnoreCase(String name);

}
