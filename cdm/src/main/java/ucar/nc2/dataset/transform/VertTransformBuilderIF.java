/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VerticalCT;

import java.util.Formatter;

/**
 * Implement this interface to add a Coordinate Transform to a NetcdfDataset.
 * Must be able to know how to build one from the info in a Coordinate Transform Variable.
 *
 */
public interface VertTransformBuilderIF {

  /**
   * Make a vertical CoordinateTransform from a Coordinate Transform Variable.
   * A VerticalCT is just a container for the metadata, the real work is in the VerticalTransform
   *
   * @param ds the containing dataset
   * @param ctv the coordinate transform variable.
   * @return CoordinateTransform
   */
  VerticalCT makeCoordinateTransform (NetcdfDataset ds, AttributeContainer ctv);

  /**
   * Make a VerticalTransform.
   * We need to defer making the transform until we've identified the time coordinate dimension.
   * @param ds the dataset
   * @param timeDim the time dimension
   * @param vCT the vertical coordinate transform
   * @return ucar.unidata.geoloc.vertical.VerticalTransform math transform
   */
  ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT);

  /**
   * Get the Transform name. Typically this is matched on by an attribute in the dataset.
   * @return name of the transform.
   */
  String getTransformName();

  /***
   * Pass in a Formatter where error messages can be appended.
   * @param sb use this Formatter to record parse and error info
   */
  void setErrorBuffer( Formatter sb);
}
