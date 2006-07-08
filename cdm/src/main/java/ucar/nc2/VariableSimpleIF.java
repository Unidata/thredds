// $Id: VariableSimpleIF.java,v 1.4 2006/02/13 19:51:26 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
 * @see ucar.nc2.dt.TypedDataset
 * @author caron
 * @version $Revision: 1.21 $ $Date: 2006/05/08 02:47:36 $
 */
public interface VariableSimpleIF {
  /** The full name of the data Variable */
  public String getName();
  /** The short name of the data Variable */
  public String getShortName();
  /** Text description of the Variable */
  public String getDescription();
  /* Units of the Variable. These should be udunits compatible if possible */
  public String getUnitsString();

  /** Variable rank */
  public int getRank();
  /** Variable shape */
  public int[] getShape();
  /** Variable data type */
  public DataType getDataType();

  /** Attributes for the variable.
   * @return List of type ucar.nc2.Attribute */
  public List getAttributes();

  /** Return the attribute for the variable with the given name, ignoring case. */
  public ucar.nc2.Attribute findAttributeIgnoreCase( String name );

  public double convertScaleOffsetMissing(byte value);
  public double convertScaleOffsetMissing(short value);
  public double convertScaleOffsetMissing(int value);
  public double convertScaleOffsetMissing(long value);
  public double convertScaleOffsetMissing(double value);
}