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

import ucar.nc2.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;

/**
 * NPP/NPOESS Conventions
 * note this is almost same as Avhrr ??
 * @author caron
 * @since Sep 11, 2009
 */
public class NppConvention extends ucar.nc2.dataset.CoordSysBuilder {

  public static boolean isMine(NetcdfFile ncfile) {
    if (!ncfile.getFileTypeId().equals("HDF5")) return false;

    Group loc = ncfile.findGroup("All_Data/VIIRS-MOD-GEO-TC_All");
    if (loc == null)
       loc = ncfile.findGroup("All_Data/VIIRS-CLD-AGG-GEO_All");

    if (null == loc) return false;
    if (null == loc.findVariable("Latitude")) return false;
    if (null == loc.findVariable("Longitude")) return false;
    return true;
  }

  public NppConvention() {
    this.conventionName = "NPP/NPOESS";
  }

  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    ds.addAttribute(null, new Attribute("FeatureType", FeatureType.IMAGE.toString())); // LOOK

    Group loc = ds.findGroup("All_Data/VIIRS-MOD-GEO-TC_All");
    if (loc == null)
       loc = ds.findGroup("All_Data/VIIRS-CLD-AGG-GEO_All");

    Variable lat = loc.findVariable("Latitude");
    lat.addAttribute(new Attribute("units", "degrees_north"));
    lat.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    Variable lon = loc.findVariable("Longitude");
    lon.addAttribute(new Attribute("units", "degrees_east"));
    lon.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

    int[] shape = lat.getShape();
    assert shape.length == 2;
    Dimension scan = new Dimension("scan", shape[0]);
    Dimension xscan = new Dimension("xscan", shape[1]);
    loc.addDimension(scan);
    loc.addDimension(xscan);

    lat.setDimensions("scan xscan");
    lon.setDimensions("scan xscan");

    for (Variable v : loc.getVariables()) {
      int[] vs = v.getShape();
      if ((vs.length == 2) && (vs[0] == shape[0]) && (vs[1] == shape[1])) {
        v.setDimensions("scan xscan");
        v.addAttribute(new Attribute(_Coordinate.Axes, "Latitude Longitude"));
      }
    }

    /* Group PRODUCT_METADATA {

   Group PRODUCT_DETAILS {
     PRODUCT_DETAILS:UNIQUE_ID = "3AVHR_22NOV2007_0902";
     PRODUCT_DETAILS:PRODUCT_TYPE = "STANDARD(FULL DISK) ";
     PRODUCT_DETAILS:PROCESSING_SOFTWARE = "InPGS_XXXXXXXXXXXXXX";
     PRODUCT_DETAILS:SPACECRAFT_ID = "INSAT-3A  ";
     PRODUCT_DETAILS:SENSOR_ID = "VHR";
     PRODUCT_DETAILS:ACQUISITION_TYPE = "FULL FRAME     ";
     PRODUCT_DETAILS:ACQUISITION_DATE = "22NOV2007";
     PRODUCT_DETAILS:ACQUISITION_TIME_IN_GMT = "0902";
     PRODUCT_DETAILS:PRODUCT_NAME = "      FULL DISK";
     PRODUCT_DETAILS:PROCESSING_LEVEL = "L1B";
     PRODUCT_DETAILS:BAND_COMBINATION = "Visible(VIS),Thermal Infrared(TIR),Water Vapour(WV)";
     PRODUCT_DETAILS:PRODUCT_CODE = "ST00001HD";
   }

    Group info = ds.findGroup("PRODUCT_METADATA/PRODUCT_DETAILS");
    String dateS = info.findAttribute("ACQUISITION_DATE").getStringValue();
    String timeS = info.findAttribute("ACQUISITION_TIME_IN_GMT").getStringValue();

    SimpleDateFormat format = new SimpleDateFormat("ddMMMyyyyHHmm");
    format.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    try {
      Date d = format.parse(dateS+timeS);
      VariableDS time = new VariableDS(ds, vhrr, null, "time", DataType.LONG, "",
          "seconds since 1970-01-01 00:00", "time generated from PRODUCT_METADATA/PRODUCT_DETAILS");

      time.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString())); // // LOOK : cant handle scalar coordinates yet ??
      time.addAttribute( new Attribute("IsoDate", new DateFormatter().toDateTimeStringISO(d)));
      ds.addVariable(vhrr, time);
      ArrayLong.D0 timeData = new ArrayLong.D0();
      timeData.set(d.getTime() / 1000);
      time.setCachedData(timeData, true);

    } catch (ParseException e) {
      e.printStackTrace();
      throw new IOException(e.getMessage());
    }  */

    ds.finish();
  }
}
