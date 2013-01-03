/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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
import ucar.nc2.constants.CDM;
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
 * @see "http://jointmission.gsfc.nasa.gov/science/documents.html"
 */
/*
 * NPP Common Data Format Control Book – External (CDFCB-X)
 – Volume I – Overview
 – Volume II – RDR Formats
 – Volume III – SDR/TDR Formats
 – Volume IV – EDR/IP/ARP and Geolocation Formats
 – Volume V – Metadata
 – Volume VI – Ancillary Data, Auxiliary Data, Messages, and Reports
 – Volume VII – Downlink Formats (Application Packets)
 – Volume VIII – Look Up Table Formats
 */
public class NppConvention extends ucar.nc2.dataset.CoordSysBuilder {
  private static final String SPECTRAL_COORD_NAME = "Surface_Emissivity_Wavelengths";
  private static final String PRESSURE_COORD_NAME = "Pressure_Levels";

  public static boolean isMine(NetcdfFile ncfile) {
    if (!ncfile.getFileTypeId().equals("HDF5")) return false;

    // Visible Infrared Imaging Radiometer Suite (VIIRS)
    Group loc = ncfile.findGroup("All_Data/VIIRS-MOD-GEO-TC_All");
    if (loc == null)
       loc = ncfile.findGroup("All_Data/VIIRS-CLD-AGG-GEO_All");

    // Cross-track Infrared Sounder (CrIS)
    // http://npp.gsfc.nasa.gov/cris.html
    if (loc == null) {
      loc = ncfile.findGroup("All_Data");
      Attribute att = ncfile.findGlobalAttribute("Instrument_Name");
      if ((att == null) || !att.getStringValue().equals("CrIS")) return false;
      if (null == loc.findVariable(PRESSURE_COORD_NAME)) return false;
    }

    if (null == loc.findVariable("Latitude")) return false;
    if (null == loc.findVariable("Longitude")) return false;

    return true;
  }

  public NppConvention() {
    this.conventionName = "NPP/NPOESS";
  }

  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    ds.addAttribute(null, new Attribute("FeatureType", FeatureType.SWATH.toString()));

    boolean hasPressureLevels = false;
    Variable spectralCoord = null;

    Group group = ds.findGroup("All_Data/VIIRS-MOD-GEO-TC_All");
    if (group == null)
       group = ds.findGroup("All_Data/VIIRS-CLD-AGG-GEO_All");
    if (group == null) {
      group = ds.findGroup("All_Data");
      hasPressureLevels = true;
      spectralCoord = group.findVariable(SPECTRAL_COORD_NAME);
    }

    Variable lat = group.findVariable("Latitude");
    lat.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
    lat.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    Variable lon = group.findVariable("Longitude");
    lon.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
    lon.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

    int[] shape = lat.getShape();
    assert shape.length == 2;
    Dimension scan = new Dimension("scan", shape[0]);
    Dimension xscan = new Dimension("xscan", shape[1]);
    group.addDimension(scan);
    group.addDimension(xscan);

    lat.setDimensions("scan xscan");
    lon.setDimensions("scan xscan");

    Dimension altd = null;
    if (hasPressureLevels) {
      Variable alt = group.findVariable(PRESSURE_COORD_NAME);
      alt.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.toString()));
      shape = alt.getShape();
      assert shape.length == 1;
      altd = new Dimension(PRESSURE_COORD_NAME, shape[0]);
      group.addDimension(altd);
      alt.setDimensions(PRESSURE_COORD_NAME);
    }

    Dimension spectrald = null;
    if (null != spectralCoord)  {
      spectralCoord.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Spectral.toString()));
      shape = spectralCoord.getShape();
      assert shape.length == 1;
      spectrald = new Dimension(SPECTRAL_COORD_NAME, shape[0]);
      group.addDimension(spectrald);
      spectralCoord.setDimensions(SPECTRAL_COORD_NAME);
    }

    for (Variable v : group.getVariables()) {
      int[] vs = v.getShape();
      if ((vs.length == 2) && (vs[0] == scan.getLength()) && vs[1] == xscan.getLength()) {
        v.setDimensions("scan xscan");
        v.addAttribute(new Attribute(_Coordinate.Axes, "Latitude Longitude"));

      } else if ((vs.length == 3) && altd != null &&
              (vs[0] == altd.getLength()) && (vs[1] == scan.getLength()) && vs[2] == xscan.getLength()) {
        v.setDimensions(PRESSURE_COORD_NAME+" scan xscan");
        v.addAttribute(new Attribute(_Coordinate.Axes, PRESSURE_COORD_NAME+" Latitude Longitude"));

      }  else if ((vs.length == 3) && spectrald != null &&
                    (vs[0] == spectrald.getLength()) && (vs[1] == scan.getLength()) && vs[2] == xscan.getLength()) {
        v.setDimensions(SPECTRAL_COORD_NAME+" scan xscan");
        v.addAttribute(new Attribute(_Coordinate.Axes, SPECTRAL_COORD_NAME+" Latitude Longitude"));
      }

    }

    /*
     F:\data\cdmUnitTest\ft\image\npp\GMTCO_npp_d20030125_t084705_e084830_b00015_c20071212222807_den_OPS_SEG.h5

          :AggregateBeginningTime = "084705.090560Z";
          :AggregateEndingDate = "20030125";
          :AggregateEndingTime = "084830.543424Z";

          probably

          YYYYMMDDHHMMSSssssss
          20030125084705090560
          2003-01-25T08:47:05

          mysterious is

          long ScanMidTime(=48);
            :_FillValue = -993L; // long
            :_ChunkSize = 48; // int

           data:
            {1422175657912747, 1422175659699117, 1422175661485487, 1422175663271857, 1422175665058227, ...

           what is 48 ?
           data is byte QF2_VIIRSMODGEOTC(scan=768, xscan=3200);

     */

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
