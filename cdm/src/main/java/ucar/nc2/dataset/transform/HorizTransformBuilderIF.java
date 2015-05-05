/* Copyright */
package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.ProjectionCT;

import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 5/5/2015
 */
public interface HorizTransformBuilderIF {

  /**
   * Make a ProjectionCT from a Coordinate Transform Variable.
   * A ProjectionCT is just a container for the metadata, the real work is in the ProjectionImpl
   *
   * @param ctv the coordinate transform variable.
   * @param geoCoordinateUnits the geo X/Y coordinate units, or null.
   * @return CoordinateTransform
   */
  ProjectionCT makeCoordinateTransform (AttributeContainer ctv, String geoCoordinateUnits);

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
