/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset.conv;

import ucar.nc2.*;
import ucar.nc2.constants.CDM;
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
    timeVar.addAttribute( new Attribute( CDM.UNITS, time_units));

    Variable levelVar = ds.findVariable("level");
    String level_units = ds.findAttValueIgnoreCase(null, "level_units", null);
    String level_name = ds.findAttValueIgnoreCase(null, "level_name", null);
    levelVar.addAttribute( new Attribute( CDM.UNITS, level_units));
    levelVar.addAttribute( new Attribute( CDM.LONG_NAME, level_name));

    // may be 1 or 2 data variables
    String unit_name = ds.findAttValueIgnoreCase(null, "unit_name", null);
    String parameter_name = ds.findAttValueIgnoreCase(null, "parameter_name", null);
    List<Variable> vlist = ds.getVariables();
    for (Variable v : vlist) {
      if (v.getRank() > 1) {
        v.addAttribute(new Attribute(CDM.UNITS, unit_name));
        v.addAttribute(new Attribute(CDM.LONG_NAME, v.getShortName() + " " + parameter_name));
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
    latVar.setValues(latDim.getLength(), startLat, incrLat);

    // add lon
    double startLon = translation.getNumericValue(0).doubleValue();
    double incrLon = affine.getNumericValue(3).doubleValue();
    Dimension lonDim = ds.findDimension( "column");
    Variable lonVar = ds.findVariable("longitude");
    lonVar.setValues( lonDim.getLength(), startLon, incrLon);
  }

}