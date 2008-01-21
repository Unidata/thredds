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
package ucar.nc2;

import ucar.ma2.DataType;

import java.util.List;

/**
 * A "Simple" Variable, that allows non-netcdf implementations of typed datasets.
 *
 * @author caron
 * @see ucar.nc2.dt.TypedDataset
 */
public interface VariableSimpleIF extends Comparable<VariableSimpleIF> {
  /**
   * full name of the data Variable
   * @return full name of the data Variable
   */
  public String getName();

  /**
   * short name of the data Variable
   * @return short name of the data Variable
   */
  public String getShortName();

  /**
   * description of the Variable
   * @return description of the Variable, or null if none.
   */
  public String getDescription();

  /**
   * Units of the Variable. These should be udunits compatible if possible
   * @return Units of the Variable, or null if none.
   */
  public String getUnitsString();

  /**
   * Variable rank
   * @return Variable rank
   */
  public int getRank();

  /**
   * Variable shape
   * @return Variable shape
   */
  public int[] getShape();

  /**
   * Dimension List. empty for a scalar variable.
   * @return List of ucar.nc2.Dimension
   */
  public List<Dimension> getDimensions();

  /**
   * Variable's data type
   * @return Variable's data type
   */
  public DataType getDataType();

  /**
   * Attributes for the variable.
   * @return List of type ucar.nc2.Attribute
   */
  public List<Attribute> getAttributes();

  /**
   * find the attribute for the variable with the given name, ignoring case.
   * @param name attribute name
   * @return the attribute for the variable with the given name, or null if not found.
   */
  public ucar.nc2.Attribute findAttributeIgnoreCase(String name);

}