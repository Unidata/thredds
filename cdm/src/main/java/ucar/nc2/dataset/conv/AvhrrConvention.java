/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset.conv;

import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.ma2.DataType;
import ucar.ma2.ArrayLong;

import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Avhrr Hdf5 files - geostationary satellite images.
 * Image datatype.
 *
 * @author caron
 * @since May 27, 2009
 */


public class AvhrrConvention extends ucar.nc2.dataset.CoordSysBuilder {

  public static boolean isMine(NetcdfFile ncfile) {
    if (!ncfile.getFileTypeId().equals("HDF5")) return false;

    Group loc = ncfile.findGroup("VHRR/Geo-Location");
    if (null == loc) return false;
    if (null == loc.findVariable("Latitude")) return false;
    if (null == loc.findVariable("Longitude")) return false;

    return null != ncfile.findGroup("VHRR/Image Data");
  }

  public AvhrrConvention() {
    this.conventionName = "AvhrrSatellite";
  }

  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    ds.addAttribute(null, new Attribute("FeatureType", FeatureType.SWATH.toString()));

    Group vhrr = ds.findGroup("VHRR");
    if (vhrr == null) throw new IllegalStateException();
    Group loc = vhrr.findGroup("Geo-Location");
    if (loc == null) throw new IllegalStateException();
    Variable lat = loc.findVariable("Latitude");
    lat.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
    lat.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    Variable lon = loc.findVariable("Longitude");
    lon.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
    lon.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

    int[] shape = lat.getShape();
    assert shape.length == 2;
    Dimension scan = new Dimension("scan", shape[0]);
    Dimension xscan = new Dimension("xscan", shape[1]);
    vhrr.addDimension(scan);
    vhrr.addDimension(xscan);

    lat.setDimensions("scan xscan");
    lon.setDimensions("scan xscan");

    Group data = vhrr.findGroup("Image Data");
    if (data == null) throw new IllegalStateException();
    for (Variable v : data.getVariables()) {
      int[] vs = v.getShape();
      if ((vs.length == 2) && (vs[0] == shape[0]) && (vs[1] == shape[1])) {
        v.setDimensions("scan xscan");
        v.addAttribute(new Attribute(_Coordinate.Axes, "lat lon time"));
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
   } */

    Group info = ds.findGroup("PRODUCT_METADATA/PRODUCT_DETAILS");
    if (info == null) throw new IllegalStateException("AvhrrConvention must have PRODUCT_METADATA/PRODUCT_DETAILS group");
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
      CoordinateAxis1D timeAxis = new CoordinateAxis1D(ds, time);
      ds.addVariable(vhrr, timeAxis);
      ArrayLong.D0 timeData = new ArrayLong.D0(false);
      timeData.set(d.getTime() / 1000);
      time.setCachedData(timeData, true);

    } catch (ParseException e) {
      e.printStackTrace();
      throw new IOException(e.getMessage());
    }

    ds.finish();
  }

}
