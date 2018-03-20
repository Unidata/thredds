/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.*;
import ucar.nc2.Dimension;
import ucar.unidata.geoloc.vertical.VTfromExistingData;
import ucar.unidata.util.Parameter;

/**
 * Create a Vertical Transform from an "explicit_field", where the vertical coordinate is explcitly specified as a variable.
 *
 * @author caron
 */
public class VExplicitField extends AbstractTransformBuilder implements VertTransformBuilderIF {
  public String getTransformName() {
    return VerticalCT.Type.Existing3DField.name();
  }

  public VerticalCT makeCoordinateTransform(NetcdfDataset ds, AttributeContainer ctv) {
    VerticalCT ct = new VerticalCT (ctv.getName(), getTransformName(), VerticalCT.Type.Existing3DField, this);
    String fieldName = ctv.findAttValueIgnoreCase(VTfromExistingData.existingDataField, null);
    if (null == fieldName)
      throw new IllegalArgumentException("ExplicitField Vertical Transform must have attribute "+VTfromExistingData.existingDataField);
    ct.addParameter(new Parameter("standard_name", getTransformName()));
    ct.addParameter(new Parameter(VTfromExistingData.existingDataField, fieldName));
    return ct;
  }

  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return new VTfromExistingData(ds, timeDim, vCT.getParameters());
  }

}
