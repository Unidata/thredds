/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

import ucar.nc2.dataset.*;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.unidata.geoloc.vertical.VTfromExistingData;
import ucar.unidata.util.Parameter;

/**
 * Create a Vertical Transform from an "explicit_field", where the vertical coordinate is explciitly specified as a variable.
 *
 * @author caron
 */
public class VExplicitField extends AbstractCoordTransBuilder {
  public String getTransformName() {
    return "explicit_field";
  }

  public TransformType getTransformType() {
    return TransformType.Vertical;
  }

  //   public VerticalCT (String name, String authority, VerticalCT.Type type, CoordTransBuilderIF builder) {


  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    VerticalCT ct = new VerticalCT (ctv.getShortName(), getTransformName(), VerticalCT.Type.Existing3DField, this);
    String fieldName = ds.findAttValueIgnoreCase(ctv, VTfromExistingData.existingDataField, null);
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
