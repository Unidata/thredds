// $Id: AlbersEqualArea.java,v 1.1 2006/05/24 00:12:57 caron Exp $
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

package ucar.nc2.dataset.transform;

import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.Variable;

/**
 * Create a AlbersEqualArea Projection from the information in the Coordinate Transform Variable.
 */
public class AlbersEqualArea extends AbstractCoordTransBuilder {

  public String getTransformName() { return "albers_conical_equal_area"; }
  public TransformType getTransformType() { return TransformType.Projection; }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
   double[] pars = readAttributeDouble2(ctv.findAttribute( "standard_parallel"));
   if (pars == null) return null;

   double lon0 = readAttributeDouble( ctv, "longitude_of_central_meridian");
   double lat0 = readAttributeDouble( ctv, "latitude_of_projection_origin");
   double false_easting = readAttributeDouble( ctv, "false_easting");
   double false_northing = readAttributeDouble( ctv, "false_northing");

   ucar.unidata.geoloc.projection.AlbersEqualArea proj = new ucar.unidata.geoloc.projection.AlbersEqualArea(lat0, lon0, pars[0], pars[1]);
   return new ProjectionCT(ctv.getShortName(), "FGDC", proj);
  }
}
