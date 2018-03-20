/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.conv;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.constants.CDM;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dataset.CoordSysBuilder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * Suomi coord sys builder.
 *
 * @author caron
 * @since Nov 3, 2009
 */
public class Suomi extends CoordSysBuilder {

  /**
   * @param ncfile the NetcdfFile to test
   * @return true if we think this is a Zebra file.
   */
  public static boolean isMine(NetcdfFile ncfile) {

    Variable v = ncfile.findVariable("time_offset");
    if (v == null || !v.isCoordinateVariable()) return false;
    String desc = v.getDescription();
    if (desc == null || (!desc.equals("Time delta from start_time") && !desc.equals("PWV window midpoint time delta from start_time"))) return false;

    if (null == ncfile.findGlobalAttribute( "start_date")) return false;
    if (null == ncfile.findGlobalAttribute( "start_time")) return false;

    return true;
  }

  public Suomi() {
    this.conventionName = "Suomi";
  }

  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    String start_date = ds.findAttValueIgnoreCase(null, "start_date", null);
    if (start_date == null) return;

    SimpleDateFormat df = new SimpleDateFormat("yyyy.DDD.HH.mm.ss");   // "2006.105.00.00.00"
    DateFormatter dfo = new DateFormatter();

    Date start;
    try {
      start = df.parse(start_date);
    } catch (ParseException e) {
      throw new RuntimeException("Cant read start_date="+start_date);
    }

    Variable v = ds.findVariable("time_offset");
    v.addAttribute(new Attribute( CDM.UNITS, "seconds since "+dfo.toDateTimeString(start)));

    Group root = ds.getRootGroup();
    root.addAttribute(new Attribute( CDM.CONVENTIONS, "Suomi-Station-CDM"));
    ds.finish();
  }

  protected AxisType getAxisType(NetcdfDataset ncDataset, VariableEnhanced v) {
    String name =  v.getShortName();
    if (name.equals("time_offset")) return AxisType.Time;
    if (name.equals("lat")) return AxisType.Lat;
    if (name.equals("lon")) return AxisType.Lon;
    if (name.equals("height")) return AxisType.Height;
    return null;
  }
}
