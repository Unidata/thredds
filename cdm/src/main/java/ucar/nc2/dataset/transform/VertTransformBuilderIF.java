/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
