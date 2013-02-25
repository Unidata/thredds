package ucar.nc2.dataset.conv;

import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;

import java.io.IOException;

/**
 * HDF5-EOS AURA OMI
 *
 * @author John
 * @since 12/26/12
 * @see "http://aura.gsfc.nasa.gov/instruments/omi.html"
 */
public class HdfEosOmiConvention extends ucar.nc2.dataset.CoordSysBuilder {

  public static boolean isMine(NetcdfFile ncfile) {
    if (!ncfile.getFileTypeId().equals("HDF5-EOS")) return false;

    String typeName = ncfile.findAttValueIgnoreCase(null, CF.FEATURE_TYPE, null);
    if (typeName == null) return false;
    if (!typeName.equals(FeatureType.GRID.toString()) &&
            !typeName.equals(FeatureType.SWATH.toString())) return false;

    /*
 group: HDFEOS {

    group: ADDITIONAL {

      group: FILE_ATTRIBUTES {
        :InstrumentName = "OMI";
        :ProcessLevel = "3e";
        :GranuleMonth = 12; // int
        :GranuleDay = 14; // int
        :GranuleYear = 2005; // int
        :GranuleDayOfYear = 348; // int
        :TAI93At0zOfGranule = 4.08672005E8; // double
        :PGEVersion = "0.9.26";
        :StartUTC = "2005-12-13T18:00:00.000000Z";
        :EndUTC = "2005-12-15T06:00:00.000000Z";
        :Period = "Daily";
      }
    }
     */
    Attribute instName = ncfile.findAttribute("/HDFEOS/ADDITIONAL/FILE_ATTRIBUTES/@InstrumentName");
    if (instName == null || !instName.getStringValue().equals("OMI"))  return false;

    Attribute level = ncfile.findAttribute("/HDFEOS/ADDITIONAL/FILE_ATTRIBUTES/@ProcessLevel");
    if (level == null)  return false;
    if (!level.getStringValue().startsWith("2") && !level.getStringValue().startsWith("3"))  return false;

    return true;
  }

  public HdfEosOmiConvention() {
    this.conventionName = "HDF5-EOS-OMI";
  }

  /*

  // level3
   group: HDFEOS {

    group: GRIDS {

      group: OMI_Column_Amount_O3 {
        dimensions:
          XDim = 1440;
          YDim = 720;

        group: Data_Fields {
          variables:
            float ColumnAmountO3(YDim=720, XDim=1440);
              :_FillValue = -1.2676506E30f; // float
              :Units = "DU";
              :Title = "Best Total Ozone Solution";
              :UniqueFieldDefinition = "TOMS-OMI-Shared";
              :ScaleFactor = 1.0; // double
              :Offset = 0.0; // double
              :ValidRange = 50.0f, 700.0f; // float
              :MissingValue = -1.2676506E30f; // float
              :_ChunkSize = 180, 180; // int

            float Reflectivity331(YDim=720, XDim=1440);
              :_FillValue = -1.2676506E30f; // float
              :Units = "%";
              :Title = "Effective Surface Reflectivity at 331 nm";
              :UniqueFieldDefinition = "TOMS-OMI-Shared";
              :ScaleFactor = 1.0; // double
              :Offset = 0.0; // double
              :ValidRange = -15.0f, 115.0f; // float
              :MissingValue = -1.2676506E30f; // float
              :_ChunkSize = 180, 180; // int

            float UVAerosolIndex(YDim=720, XDim=1440);
              :_FillValue = -1.2676506E30f; // float
              :Units = "NoUnits";
              :Title = "UV Aerosol Index";
              :UniqueFieldDefinition = "TOMS-OMI-Shared";
              :ScaleFactor = 1.0; // double
              :Offset = 0.0; // double
              :ValidRange = -30.0f, 30.0f; // float
              :MissingValue = -1.2676506E30f; // float
              :_ChunkSize = 180, 180; // int

        }
        // group attributes:
        :GCTPProjectionCode = 0; // int
        :Projection = "Geographic";
        :GridOrigin = "Center";
        :NumberOfLongitudesInGrid = 1440; // int
        :NumberOfLatitudesInGrid = 720; // int
      }
    }
  }
   */
  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    Attribute levelAtt = ds.findAttribute("/HDFEOS/ADDITIONAL/FILE_ATTRIBUTES/@ProcessLevel");
    if (levelAtt == null)  return;
    int level = levelAtt.getStringValue().startsWith("2") ? 2 : 3;

    Attribute time = ds.findAttribute("/HDFEOS/ADDITIONAL/FILE_ATTRIBUTES/@TAI93At0zOfGranule");

    if (level == 3) augmentDataset3(ds);

  }

  private void augmentDataset3(NetcdfDataset ds) throws IOException {

    Group grids = ds.findGroup("/HDFEOS/GRIDS");
    if (grids == null) return;
    for (Group g2 : grids.getGroups()) {
       Attribute gctp = g2.findAttribute("GCTPProjectionCode");
       if (gctp == null || !gctp.getNumericValue().equals(0)) continue;

       Attribute nlon = g2.findAttribute("NumberOfLongitudesInGrid");
       Attribute nlat = g2.findAttribute("NumberOfLatitudesInGrid");
       if (nlon == null || nlon.isString()) continue;
       if (nlat == null || nlat.isString()) continue;

       ds.addCoordinateAxis(makeLonCoordAxis(ds, g2, nlon.getNumericValue().intValue(), "XDim"));
       ds.addCoordinateAxis(makeLatCoordAxis(ds, g2, nlat.getNumericValue().intValue(), "YDim"));

      for (Group g3 : g2.getGroups()) {
        for (Variable v : g3.getVariables()) {
          v.addAttribute(new Attribute(_Coordinate.Axes, "lat lon"));
        }
      }
    }

    ds.finish();
  }

  private CoordinateAxis makeLatCoordAxis(NetcdfDataset ds, Group g, int n, String dimName) {
    double incr = 180.0 / n;
    CoordinateAxis v = new CoordinateAxis1D(ds, g, "lat", DataType.FLOAT, dimName, CDM.LAT_UNITS, "latitude");
    v.setValues(n, -90.0, incr);
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    return v;
  }

  private CoordinateAxis makeLonCoordAxis(NetcdfDataset ds, Group g, int n, String dimName) {
    double incr = 360.0 / n;
    CoordinateAxis v = new CoordinateAxis1D(ds, g, "lon", DataType.FLOAT, dimName, CDM.LON_UNITS, "longitude");
    v.setValues(n, -180.0, incr);
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    return v;
  }

}
