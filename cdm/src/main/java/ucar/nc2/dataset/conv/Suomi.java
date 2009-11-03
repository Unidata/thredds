/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
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
 * Describe
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
    if (desc == null || !desc.equals("Time delta from start_time")) return false;

    if (null == ncfile.findGlobalAttribute( "start_date")) return false;
    if (null == ncfile.findGlobalAttribute( "start_time")) return false;

    return true;
  }

  public Suomi() {
    this.conventionName = "Suomi";
  }

  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    String start_date = ds.findAttValueIgnoreCase(null, "start_date", null);
    SimpleDateFormat df = new SimpleDateFormat("yyyy.DDD.HH.mm.ss");   // "2006.105.00.00.00"
    DateFormatter dfo = new DateFormatter();

    Date start = null;
    try {
      start = df.parse(start_date);
    } catch (ParseException e) {
      throw new RuntimeException("Cant read start_date="+start_date);
    }

    Variable v = ds.findVariable("time_offset");
    v.addAttribute(new Attribute( "units", "seconds since "+dfo.toDateTimeString(start)));

    Group root = ds.getRootGroup();
    root.addAttribute(new Attribute( "Convention", "Suomi-Station-CDM"));    
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
