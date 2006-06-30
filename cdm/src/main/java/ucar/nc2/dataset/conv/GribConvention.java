// $Id: GribConvention.java,v 1.7 2004/12/07 02:43:21 caron Exp $
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
package ucar.nc2.dataset.conv;

import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

/**
 *  Conventions.
 */


public class GribConvention extends GDVConvention {

  public void augmentDataset( NetcdfDataset ncDataset, CancelTask cancelTask) {
    this.conventionName = "Grib";
  }

  /* protected void constructCoordAxes(NetcdfDataset ds) {
    super.constructCoordAxes(ds);
    addCoordAxisFromProjection(ds);
  }

  /**
   * Search for "Grib" style projection info
   *
  protected void addCoordAxisFromProjection(NetcdfDataset ds) {

    double latin = getAttributeValueDouble(ds, "Latin");
    //double latin1 = getAttributeValueDouble(ds, "Latin1");
    //double latin2 = getAttributeValueDouble(ds, "Latin2");
    double lov = getAttributeValueDouble(ds, "Lov");
    double la1 = getAttributeValueDouble(ds, "La1");
    double lo1 = getAttributeValueDouble(ds, "Lo1");

    double dx = getAttributeValueDouble(ds, "DxKm") * .0001;
    double dy = getAttributeValueDouble(ds, "DyKm") * .0001;

    //     public LambertConformal(double lat0, double lon0, double par1, double par2) {
    LambertConformal lc = new LambertConformal(latin, lov, latin, latin);

    // we have to project in order to find the origin
    ProjectionPointImpl start = (ProjectionPointImpl) lc.latLonToProj( new LatLonPointImpl( la1, lo1));
    if (debug) System.out.println("start at proj coord "+start);

    double startx = start.getX();
    double starty = start.getY();

    int nx = ds.findDimension("x").getLength();
    int ny = ds.findDimension("y").getLength();

    CoordinateAxis xaxis = new CoordinateAxis1D( ds, null, "x", DataType.DOUBLE, "x",
      "km", "X coordinate on projection plane");
    ncDataset.setValues( xaxis, nx, startx, dx);
    ds.addCoordinateAxis( xaxis);

    CoordinateAxis yaxis = new CoordinateAxis1D( ds, null, "y", DataType.DOUBLE, "y",
      "km", "Y coordinate on projection plane");
    ncDataset.setValues( yaxis, ny, starty, dy);
    ds.addCoordinateAxis( yaxis);

    projectCT = new ProjectionCT("LambertConformal", "FGDC", lc);
  }

  private double getAttributeValueDouble(NetcdfDataset ds, String name) {
    Attribute att = ds.findGlobalAttributeIgnoreCase(name);
    if (att == null) throw new IllegalStateException("GribConvention cant find attrinute= "+name);
    return att.getNumericValue().doubleValue();
  } */

}

/* Change History:
   $Log: GribConvention.java,v $
   Revision 1.7  2004/12/07 02:43:21  caron
   *** empty log message ***

   Revision 1.6  2004/12/03 04:46:25  caron
   no message

   Revision 1.5  2004/12/01 05:53:40  caron
   ncml pass 2, new convention parsing

   Revision 1.4  2004/11/07 03:00:50  caron
   *** empty log message ***

   Revision 1.3  2004/11/04 00:38:18  caron
   no message

   Revision 1.2  2004/10/29 00:14:09  caron
   no message

   Revision 1.1  2004/10/19 19:45:02  caron
   misc

*/