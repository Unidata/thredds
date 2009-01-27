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
package ucar.nc2.dataset.conv;

import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;

import java.util.List;
import java.io.IOException;

/**
 * GEIF Convention.
 * https://www.metnet.navy.mil/~hofschnr/GIEF-F/1.2/
 *
 * @author caron
 */

public class GIEFConvention extends CoordSysBuilder {

  public GIEFConvention() {
    this.conventionName = "GIEF";
  }

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    NcMLReader.wrapNcMLresource( ds, CoordSysBuilder.resourcesDir+"GIEF.ncml", cancelTask);

    Variable timeVar = ds.findVariable("time");
    String time_units = ds.findAttValueIgnoreCase(null, "time_units", null);
    timeVar.addAttribute( new Attribute( "units", time_units));

    Variable levelVar = ds.findVariable("level");
    String level_units = ds.findAttValueIgnoreCase(null, "level_units", null);
    String level_name = ds.findAttValueIgnoreCase(null, "level_name", null);
    levelVar.addAttribute( new Attribute( "units", level_units));
    levelVar.addAttribute( new Attribute( "long_name", level_name));

    // may be 1 or 2 data variables
    String unit_name = ds.findAttValueIgnoreCase(null, "unit_name", null);
    String parameter_name = ds.findAttValueIgnoreCase(null, "parameter_name", null);
    List<Variable> vlist = ds.getVariables();
    for (Variable v : vlist) {
      if (v.getRank() > 1) {
        v.addAttribute(new Attribute("units", unit_name));
        v.addAttribute(new Attribute("long_name", v.getName() + " " + parameter_name));
        v.addAttribute(new Attribute(_Coordinate.Axes, "time level latitude longitude"));
      }
    }

    Attribute translation = ds.findGlobalAttributeIgnoreCase("translation");
    Attribute affine = ds.findGlobalAttributeIgnoreCase("affine_transformation");

    // LOOK only handling the 1D case
    // add lat
    double startLat = translation.getNumericValue(1).doubleValue();
    double incrLat = affine.getNumericValue(6).doubleValue();
    Dimension latDim = ds.findDimension( "row");
    Variable latVar = ds.findVariable("latitude");
    ds.setValues( latVar, latDim.getLength(), startLat, incrLat);

    // add lon
    double startLon = translation.getNumericValue(0).doubleValue();
    double incrLon = affine.getNumericValue(3).doubleValue();
    Dimension lonDim = ds.findDimension( "column");
    Variable lonVar = ds.findVariable("longitude");
    ds.setValues( lonVar, lonDim.getLength(), startLon, incrLon);
  }

}