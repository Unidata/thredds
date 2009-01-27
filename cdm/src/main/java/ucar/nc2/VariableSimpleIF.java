/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2;

import ucar.ma2.DataType;

import java.util.List;

/**
 * A lightweight abstractions of a Variable.
 *
 * @author caron
 * @see ucar.nc2.ft.FeatureDataset
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