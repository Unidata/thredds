// $Id:VExplicitField.java 51 2006-07-12 17:13:13Z caron $
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

import ucar.nc2.dataset.*;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.unidata.geoloc.vertical.VTfromExistingData;
import ucar.unidata.util.Parameter;

/**
 * Create a Vertical Transform from an "explicit_field", where the vertical coordinate is explciitly specified as a variable.
 *  *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
public class VExplicitField extends AbstractCoordTransBuilder {
  public String getTransformName() {
    return "explicit_field";
  }

  public TransformType getTransformType() {
    return TransformType.Vertical;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    VerticalCT ct = new VerticalCT (ctv.getName(), getTransformName(), VerticalCT.Type.Existing3DField, this);
    ct.addParameter(new Parameter(VTfromExistingData.existingDataField, ctv.getName()));
    return ct;
  }

  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return new VTfromExistingData(ds, timeDim, vCT);
  }

}
