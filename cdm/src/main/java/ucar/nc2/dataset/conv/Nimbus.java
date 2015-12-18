/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
