// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.nc2.dataset;

import ucar.nc2.Variable;
import ucar.nc2.Dimension;

/**
 * Implement this interface to add a Coordinate Transform to a NetcdfDataset.
 * Must be able to know how to build one from the info in a Coordinate Transform Variable.
 *
 * @author john caron
 * @version $Revision$ $Date$
 */
public interface CoordTransBuilderIF {

  /**
   * Make a CoordinateTransform from a Coordinate Transform Variable.
   * @param ds
   * @param ctv the coordinate transform variable.
   * @return CoordinateTransform
   */
  public CoordinateTransform makeCoordinateTransform (NetcdfDataset ds, Variable ctv);

  /**
   * Make a VerticalTransform. Only implement if you are a TransformType.Vertical.
   * We need to defer making the transform until we've identified the time coordinate dimension.
   * @param ds the dataset
   * @param timeDim the time dimension
   * @param vCT the vertical coordinate transform
   * @return ucar.unidata.geoloc.vertical.VerticalTransform math transform
   */
  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT);


  public String getTransformName();
  public TransformType getTransformType();

  public void setErrorBuffer( StringBuffer parseInfo);

}
