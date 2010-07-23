/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.dataset.transform;

import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.Variable;
import ucar.unidata.geoloc.ProjectionImpl;

/**
 * MSGnavigation projection
 *
 * @author caron
 * @since Jan 9, 2010
 */


public class MSGnavigation extends AbstractCoordTransBuilder {

    public String getTransformName() {
      return "MSGnavigation";
    }

    public TransformType getTransformType() {
      return TransformType.Projection;
    }

    public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {

      double lon0 = readAttributeDouble( ctv, "longitude_of_projection_origin", Double.NaN);
      double lat0 = readAttributeDouble( ctv, "latitude_of_projection_origin", Double.NaN);
      double minor_axis = readAttributeDouble( ctv, "semi_minor_axis", Double.NaN);
      double major_axis = readAttributeDouble( ctv, "semi_major_axis", Double.NaN);
      double height = readAttributeDouble( ctv, "height_from_earth_center", Double.NaN);
      double scale_x = readAttributeDouble( ctv, "scale_x", Double.NaN);
      double scale_y = readAttributeDouble( ctv, "scale_y", Double.NaN);

      //ProjectionImpl proj = new ucar.unidata.geoloc.projection.sat.MSGnavigation(lat0, lon0, major_axis, minor_axis, height, scale_x, scale_y);
      ProjectionImpl proj = new ucar.unidata.geoloc.projection.sat.MSGnavigation(lat0, lon0, minor_axis, major_axis, height, scale_x, scale_y);  // LOOK WTF?
      return new ProjectionCT(ctv.getShortName(), "FGDC", proj);
    }

}
