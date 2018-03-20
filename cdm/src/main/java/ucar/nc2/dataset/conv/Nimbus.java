/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset.conv;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.units.DateUnit;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;

import java.io.IOException;

/**
 * NCAR RAF / NIMBUS
 * @see "http://www.eol.ucar.edu/raf/Software/netCDF.html"
 * @author caron
 * @since Dec 31, 2008
 */
public class Nimbus extends COARDSConvention {

  public static boolean isMine(NetcdfFile ncfile) {
    String s = ncfile.findAttValueIgnoreCase(null, "Convention", "none");
    return s.equalsIgnoreCase("NCAR-RAF/nimbus");
  }

  public Nimbus() {
    this.conventionName = "NCAR-RAF/nimbus";
  }

  @Override
  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    ds.addAttribute(null, new Attribute("cdm_data_type", ucar.nc2.constants.FeatureType.TRAJECTORY.name()));  // deprecated
    ds.addAttribute(null, new Attribute(CF.FEATURE_TYPE, ucar.nc2.constants.FeatureType.TRAJECTORY.name()));

    if (!setAxisType(ds, "LATC", AxisType.Lat))
      if (!setAxisType(ds, "LAT", AxisType.Lat))
        setAxisType(ds, "GGLAT", AxisType.Lat);

    if (!setAxisType(ds, "LONC", AxisType.Lon))
      if (!setAxisType(ds, "LON", AxisType.Lon))
        setAxisType(ds, "GGLON", AxisType.Lon);

    if (!setAxisType(ds, "PALT", AxisType.Height))
      setAxisType(ds, "GGALT", AxisType.Height);

    boolean hasTime = setAxisType(ds, "Time", AxisType.Time);
    if (!hasTime)
      hasTime = setAxisType(ds, "time", AxisType.Time);

    // do we need to version this ?
    // String version =  ds.findAttValueIgnoreCase(null, "version", null);

    if (!hasTime) {
      Variable time = ds.findVariable("time_offset");
      if (time != null) {
        Variable base = ds.findVariable("base_time");
        int base_time = base.readScalarInt();
        try {
          DateUnit dunit = new DateUnit("seconds since 1970-01-01 00:00");
          String time_units = "seconds since " + dunit.makeStandardDateString(base_time);
          time.addAttribute(new Attribute(CDM.UNITS, time_units));
          time.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.name()));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    // look for coordinates
    String coordinates = ds.findAttValueIgnoreCase(null, "coordinates", null);
    if (coordinates != null) {
      String[] vars = coordinates.split(" ");
      for (String vname : vars) {
        Variable v = ds.findVariable(vname);
        if (v != null) {
          AxisType atype = getAxisType(ds, (VariableEnhanced) v);
          if (atype != null)
            v.addAttribute(new Attribute(_Coordinate.AxisType, atype.name()));
        }
      }
    }

  }

  private boolean setAxisType(NetcdfDataset ds, String varName, AxisType atype) {
    Variable v = ds.findVariable(varName);
    if (v == null) return false;

    v.addAttribute(new Attribute(_Coordinate.AxisType, atype.toString()));
    return true;
  }
}
