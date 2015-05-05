/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.ProjectionCT;

/**
 * Create a Mercator Projection from the information in the Coordinate Transform Variable.
 *
 * @author caron
 */
public class Mercator extends AbstractTransformBuilder implements HorizTransformBuilderIF {

  @Override
  public String getTransformName() {
    return CF.MERCATOR;
  }

  @Override
  public ProjectionCT makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double par = readAttributeDouble( ctv, CF.STANDARD_PARALLEL, Double.NaN);
    if (Double.isNaN(par)) {
      double scale = readAttributeDouble( ctv, CF.SCALE_FACTOR_AT_PROJECTION_ORIGIN, Double.NaN);
      if (Double.isNaN(scale))
        throw new IllegalArgumentException("Mercator projection must have attribute " +
                CF.STANDARD_PARALLEL + " or " + CF.SCALE_FACTOR_AT_PROJECTION_ORIGIN);
      par = ucar.unidata.geoloc.projection.Mercator.convertScaleToStandardParallel(scale);
    }

    readStandardParams(ctv, geoCoordinateUnits);

    ucar.unidata.geoloc.projection.Mercator proj =
            new ucar.unidata.geoloc.projection.Mercator( lon0, par, false_easting, false_northing, earth_radius);
    return new ProjectionCT(ctv.getName(), "FGDC", proj);
  }
}
