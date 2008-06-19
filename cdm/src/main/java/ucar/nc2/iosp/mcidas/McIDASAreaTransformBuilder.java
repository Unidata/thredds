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

package ucar.nc2.iosp.mcidas;

import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.transform.AbstractCoordTransBuilder;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.ma2.Array;

/**
 * Projection based on Mcidas Area files.
 *
 * @author caron
 */
public class McIDASAreaTransformBuilder extends AbstractCoordTransBuilder {

  public String getTransformName() {
    return "mcidas_area";
  }

  public TransformType getTransformType() {
    return TransformType.Projection;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {

    int[] area = getIntArray(ctv, "AreaHeader");
    int[] nav = getIntArray(ctv, "NavHeader");

    McIDASAreaProjection proj = new McIDASAreaProjection( area, nav) ;
    return new ProjectionCT(ctv.getShortName(), "FGDC", proj);
  }

  private int[] getIntArray(Variable ctv, String attName) {
    Attribute att = ctv.findAttribute(attName);
    if (att == null)
      throw new IllegalArgumentException("McIDASArea coordTransformVariable "+ctv.getName()+" must have "+attName+ " attribute");

    Array arr = att.getValues();
    return (int[]) arr.get1DJavaArray(int.class);
  }
}

