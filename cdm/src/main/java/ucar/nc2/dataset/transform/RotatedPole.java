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

package ucar.nc2.dataset.transform;

import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Variable;

/**
 * Create a RotatedPole Projection from the information in the Coordinate Transform Variable.
 *
 * @author caron
 */
public class RotatedPole extends AbstractCoordTransBuilder {

  public String getTransformName() {
    return "rotated_latitude_longitude";
  }

  public TransformType getTransformType() {
    return TransformType.Projection;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    double lon = readAttributeDouble( ctv, "grid_north_pole_longitude");
    double lat = readAttributeDouble( ctv, "grid_north_pole_latitude");

    ucar.unidata.geoloc.projection.RotatedPole proj = new ucar.unidata.geoloc.projection.RotatedPole( lat, lon);
    return new ProjectionCT(ctv.getShortName(), "FGDC", proj);
  }

    /**

   //	Rotated pole info should be specified by grid mapping attribute.
   //	If not, then check if variable has pole attributes.
   //	Failing that, check if its dimensions have "rotated" in their
   //	definitions.
   Attribute gm = varDS.findAttributeIgnoreCase("grid_mapping");

   if (gm != null) {
   examineRPVariable(gm.getStringValue());
   } else {
   boolean hasPA = examinePoleAttributes();

   if (!hasPA) {
   boolean inDims = examineDimensions();

   if (!inDims) {
   throw new NcException("Could not find pole definition");
   }
   }
   }



   * Examines named variable for attributes defining lon and lat of
   * north pole location in rotated pole grid.
   *


   private void examineRPVariable(
   String vname) {
   VariableDS rpvar = ncvar.getDataset().findVariable(vname);

   if (rpvar == null) {
   throw new NcException("Grid mapping variable is missing");
   }

   Attribute gatt = rpvar.findAttribute("grid_mapping_name");

   String gname = gatt.getStringValue();

   //	Make sure mapping is rotated pole. CF conventions specified
   //	it should have name "rotated_pole", but we have also seen
   //	"rotated_latitude_longitude" used in not-quite-CF files.
   if (!gname.equalsIgnoreCase("rotated_latitude_longitude")
   && !gname.equalsIgnoreCase("rotated_pole")) {
   throw new NcException("Grid mapping variable is not rotated pole");
   }

   //	Look for pole description, which will define the rotation.
   Attribute lon = rpvar.findAttribute("grid_north_pole_longitude");
   Attribute lat = rpvar.findAttribute("grid_north_pole_latitude");

   //	Both north lat and north lon specified.
   if (lat != null && lon != null) {
   Number lonNum = lon.getNumericValue();
   Number latNum = lat.getNumericValue();

   if (latNum == null || lonNum == null) {
   throw new NcException("Rotated pole not defined with numerics (N)");
   }

   northPole = new Point2D.Double(lonNum.doubleValue(),
   latNum.doubleValue());
   }

   //	North lat or north lon specified, but not both
   else if (lat != null || lon != null) {
   throw new NcException("Rotated pole is ill defined (N)");
   }

   //	Neither north lon or nort lat specified. Maybe south lat and south lon?
   else {
   lon = rpvar.findAttribute("grid_south_pole_longitude");
   lat = rpvar.findAttribute("grid_south_pole_latitude");

   if (lat != null && lon != null) {
   Number lonNum = lon.getNumericValue();
   Number latNum = lat.getNumericValue();

   if (latNum == null || lonNum == null) {
   throw new NcException("Rotated pole not defined with numerics (S)");
   }

   double sPoleLon = lonNum.doubleValue();
   double sPoleLat = latNum.doubleValue();

   northPole = new Point2D.Double(sPoleLon + 180., -sPoleLat);
   } else if (lat != null || lon != null) {
   throw new NcException("Rotated pole is ill defined (S)");
   }
   }
   }

   /**
   * Examines variable's axes to see if they separately define the lon and lat
   * of north pole location in rotated pole grid. If so, the lon axis will tell
   * what the lon value is, and the lat axis the lat value.
   *
   private boolean examinePoleAttributes() {
   Attribute lon = xAxis.getAxis().findAttribute("grid_north_pole_longitude");
   Attribute lat = yAxis.getAxis().findAttribute("grid_north_pole_latitude");

   if (lon == null) {
   lon = xAxis.getAxis().findAttribute("north_pole");
   }

   if (lat == null) {
   lat = yAxis.getAxis().findAttribute("north_pole");
   }

   if (lat == null || lon == null) {
   return false;
   }

   Number lonNum = lon.getNumericValue();
   Number latNum = lat.getNumericValue();

   if (latNum == null || lonNum == null) {
   throw new NcException("Rotated pole not defined with numerics (PA)");
   }

   northPole = new Point2D.Double(lonNum.doubleValue(),
   latNum.doubleValue());

   return true;
   }

   /**
   * Examine dimensions of variable to see if they include two tagged as rotated
   * lon and lat. If so, then check dataset for a generic variable called
   * rotated_pole which defines the pole location.
   *
   private boolean examineDimensions() {
   int rank = varDS.getRank();

   String lonName = null;
   String latName = null;

   for (int i = 0; i < rank; ++i) {
   Dimension d = varDS.getDimension(i);

   String dname = d.getName();

   if (dname == null) {
   continue;
   }

   String lcname = dname.toLowerCase();

   if (lcname.indexOf("lon") >= 0) {
   lonName = dname;
   } else if (lcname.indexOf("lat") >= 0) {
   latName = dname;
   }
   }

   if (lonName == null || latName == null) {
   return false;
   }

   Attribute lonA = dataset.findVariable(lonName)
   .findAttribute("long_name");
   Attribute latA = dataset.findVariable(latName)
   .findAttribute("long_name");

   if (lonA == null || latA == null) {
   return false;
   }

   if (lonA.getStringValue().indexOf("rotated") < 0
   || latA.getStringValue().indexOf("rotated") < 0) {
   return false;
   }

   examineRPVariable("rotated_pole");

   return true;
   } */
}
