package ucar.nc2.dataset.conv;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.dataset.*;
import ucar.nc2.iosp.hdf4.HdfEos;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.projection.Sinusoidal;

import java.io.IOException;

/**
 * HDF4-EOS TERRA MODIS
 *
 * @author caron
 * @since 2/23/13
 * @see "https://lpdaac.usgs.gov/products/modis_overview"
 */
public class HdfEosModisConvention extends ucar.nc2.dataset.CoordSysBuilder {
  private static final String CRS = "Projection";
  private static final String DATA_GROUP = "Data_Fields";
  private static final String DIMX_NAME = "XDim";
  private static final String DIMY_NAME = "YDim";
  private static final String TIME_NAME = "time";

  public static boolean isMine(NetcdfFile ncfile) {
    if (!ncfile.getFileTypeId().equals("HDF4-EOS")) return false;

    String typeName = ncfile.findAttValueIgnoreCase(null, CF.FEATURE_TYPE, null);
    if (typeName == null) return false;
    if (!typeName.equals(FeatureType.GRID.toString()) &&
            !typeName.equals(FeatureType.SWATH.toString())) return false;

    return checkGroup(ncfile.getRootGroup());
  }

  private static boolean checkGroup(Group g) {
    Variable crs = g.findVariable(HdfEos.HDFEOS_CRS);
    Group dataG = g.findGroup(DATA_GROUP);
    if (crs != null && dataG != null) {
      Attribute att = crs.findAttribute(HdfEos.HDFEOS_CRS_Projection);
      if (att == null) return false;
      if (!att.getStringValue().equals("GCTP_SNSOID") && !att.getStringValue().equals("GCTP_GEO")) return false;
      if (dataG.findDimensionLocal(DIMX_NAME) == null || dataG.findDimensionLocal(DIMY_NAME) == null) return false;
      return true;
    }

    for (Group ng : g.getGroups()) {
      if (checkGroup(ng)) return true;
    }
    return false;
  }

  public HdfEosModisConvention() {
    this.conventionName = "HDF4-EOS-MODIS";
  }

  /*
    group: MODIS_Grid_16DAY_250m_500m_VI {
    variables:
      short _HDFEOS_CRS;
        :Projection = "GCTP_SNSOID";
        :UpperLeftPointMtrs = -2.0015109354E7, 1111950.519667; // double
        :LowerRightMtrs = -1.8903158834333E7, 0.0; // double
        :ProjParams = 6371007.181, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0; // double
        :SphereCode = "-1";


   group: Data_Fields {
      dimensions:
        YDim = 4800;
        XDim = 4800;
      variables:
        short 250m_16_days_NDVI(YDim=4800, XDim=4800);
        ...

   */
  private boolean addTimeCoord;
  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    addTimeCoord = addTimeCoordinate(ds);
    augmentGroup(ds, ds.getRootGroup());
    ds.addAttribute(ds.getRootGroup(), new Attribute(CDM.CONVENTIONS, "CF-1.0"));

    ds.finish();
  }

  private boolean addTimeCoordinate(NetcdfDataset ds) {
    // add time coordinate
    CalendarDate cd = parseFilenameForDate(ds.getLocation());
    if (cd == null) return false;

    ds.addAttribute(ds.getRootGroup(), new Attribute("_MODIS_Date", cd.toString()));

    // add the time dimension
    int nTimesDim = 1;
    Dimension newDim = new Dimension(TIME_NAME, nTimesDim);
    ds.addDimension( null, newDim);

    // add the coordinate variable
    String units = "seconds since "+cd.toString();
    String desc = "time coordinate";

    Array data = Array.makeArray(DataType.DOUBLE, 1, 0.0, 0.0) ;

    CoordinateAxis1D timeCoord = new CoordinateAxis1D( ds, null, TIME_NAME, DataType.DOUBLE, "", units, desc);
    timeCoord.setCachedData(data, true);
    timeCoord.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    ds.addCoordinateAxis(timeCoord);

    return true;
  }

  private CalendarDate parseFilenameForDate(String filename) {
    // filename MOD13Q1.A2000065.h11v04.005.2008238031620.hdf
    String[] tokes = filename.split("\\.");
    if (tokes.length < 2) return null;
    if (tokes[1].length() < 8) return null;
    String want = tokes[1];
    String yearS = want.substring(1,5);
    String jdayS = want.substring(5,8);
    try {
      int year = Integer.parseInt(yearS);
      int jday = Integer.parseInt(jdayS);
      return CalendarDate.withDoy(null, year, jday, 0,0,0);
    }  catch (Exception e) {
      return null;
    }
  }

  private void augmentGroup(NetcdfDataset ds, Group g) {
    Variable crs = g.findVariable(HdfEos.HDFEOS_CRS);
    if (crs != null) augmentGroupWithProjectionInfo(ds, g);

    for (Group ng : g.getGroups())
      augmentGroup(ds, ng);
  }

  private void augmentGroupWithProjectionInfo(NetcdfDataset ds, Group g) {
    Dimension dimX = null, dimY = null;
    Group dataG = g.findGroup(DATA_GROUP);
    if (dataG != null) {
      dimX = dataG.findDimensionLocal(DIMX_NAME);
      dimY = dataG.findDimensionLocal(DIMY_NAME);
    }
    if (dimX == null || dimY == null) return;

    Variable crs = g.findVariable(HdfEos.HDFEOS_CRS);
    Attribute projAtt = crs.findAttribute(HdfEos.HDFEOS_CRS_Projection);
    if (projAtt != null) {
      Attribute upperLeft = crs.findAttribute(HdfEos.HDFEOS_CRS_UpperLeft);
      Attribute lowerRight = crs.findAttribute(HdfEos.HDFEOS_CRS_LowerRight);
      Attribute projParams = crs.findAttribute(HdfEos.HDFEOS_CRS_ProjParams);

      double minX = upperLeft.getNumericValue(0).doubleValue();
      double minY = upperLeft.getNumericValue(1).doubleValue();

      double maxX = lowerRight.getNumericValue(0).doubleValue();
      double maxY = lowerRight.getNumericValue(1).doubleValue();

      boolean hasProjection = false;
      String coordinates = null;
      ProjectionCT ct;
      if (projAtt.getStringValue().equals("GCTP_SNSOID")) {
        hasProjection = true;
        ct = makeSinusoidalProjection(CRS, projParams);
        VariableDS crss = makeCoordinateTransformVariable(ds, ct);
        crss.addAttribute(new Attribute(_Coordinate.AxisTypes, "GeoX GeoY"));
        ds.addVariable(dataG, crss);

        ds.addCoordinateAxis(makeCoordAxis(ds, dataG, DIMX_NAME, dimX.getLength(), minX,  maxX, true));
        ds.addCoordinateAxis(makeCoordAxis(ds, dataG, DIMY_NAME, dimY.getLength(), minY,  maxY, false));
        coordinates = addTimeCoord ? TIME_NAME+" "+DIMX_NAME + " " + DIMY_NAME : DIMX_NAME + " " + DIMY_NAME;

      } else if (projAtt.getStringValue().equals("GCTP_GEO")) {

        ds.addCoordinateAxis(makeLatLonCoordAxis(ds, dataG, dimX.getLength(), minX * 1e-6, maxX * 1e-6, true));
        ds.addCoordinateAxis(makeLatLonCoordAxis(ds, dataG, dimY.getLength(), minY * 1e-6,  maxY * 1e-6, false));
        coordinates = addTimeCoord ? TIME_NAME+" Lat Lon" : "Lat Lon";
      }

      for (Variable v : dataG.getVariables()) {
        if (v.getRank() != 2)  continue;
        if (!v.getDimension(0).equals(dimY))  continue;
        if (!v.getDimension(1).equals(dimX))  continue;

        if (coordinates != null)
          v.addAttribute(new Attribute(CF.COORDINATES, coordinates));

        if (hasProjection)
          v.addAttribute(new Attribute(CF.GRID_MAPPING, CRS));
      }
    }

  }

  /*
  The UpperLeftPointMtrs is in projection coordinates, and identifies the very upper left corner of the upper left pixel of the image data
  • The LowerRightMtrs identifies the very lower right corner of the lower right pixel of the image data. These projection coordinates are the only metadata that accurately reflect the extreme corners of the gridded image
   */

  private CoordinateAxis makeCoordAxis(NetcdfDataset ds, Group g, String name, int n, double start, double end, boolean isX) {
    CoordinateAxis v = new CoordinateAxis1D(ds, g, name, DataType.DOUBLE, name, "km", isX ? "x coordinate" : "y coordinate");
    double incr = (end - start) / n;
    v.setValues(n, start * .001, incr * .001); // km
    v.addAttribute(new Attribute(_Coordinate.AxisType, isX ? AxisType.GeoX.toString() : AxisType.GeoY.toString()));
    return v;
  }

  /*
    group: MOD_Grid_MOD17A3 {
    variables:
      short _HDFEOS_CRS;
        :Projection = "GCTP_GEO";
        :UpperLeftPointMtrs = -1.8E8, 9.0E7; // double
        :LowerRightMtrs = 1.8E8, -9.0E7; // double
   */
  private CoordinateAxis makeLatLonCoordAxis(NetcdfDataset ds, Group g, int n, double start, double end, boolean isLon) {
    String name = isLon ? AxisType.Lon.toString() : AxisType.Lat.toString();
    String dimName = isLon ? DIMX_NAME : DIMY_NAME;
    CoordinateAxis v = new CoordinateAxis1D(ds, g, name, DataType.DOUBLE, dimName, isLon ? "degrees_east" : "degrees_north", null);
    double incr = (end - start) / n;
    v.setValues(n, start, incr);
    v.addAttribute(new Attribute(_Coordinate.AxisType, name));
    return v;
  }

  /*
                     1       5        7   8
		16 PGSd_SNSOID  Sphere  CentMer  FE  FN

   */
  private ProjectionCT makeSinusoidalProjection(String name, Attribute projParams) {
    double radius = projParams.getNumericValue(0).doubleValue();
    double centMer = projParams.getNumericValue(4).doubleValue();
    double falseEast = projParams.getNumericValue(6).doubleValue();
    double falseNorth = projParams.getNumericValue(7).doubleValue();

    Sinusoidal proj = new Sinusoidal(centMer, falseEast * .001, falseNorth * .001, radius * .001);
    return new ProjectionCT(name, "FGDC", proj);
  }


}


/*

Can we extract projection info ??

// external xml
        <Platform>
            <PlatformShortName>Terra</PlatformShortName>
            <Instrument>
                <InstrumentShortName>MODIS</InstrumentShortName>
                <Sensor>
                    <SensorShortName>MODIS</SensorShortName>
                </Sensor>
            </Instrument>
        </Platform>

 // ODL
	  UpperLeftPointMtrs=(-20015109.354000,1111950.519667)
		LowerRightMtrs=(-18903158.834333,-0.000000)
		Projection=GCTP_SNSOID
		ProjParams=(6371007.181000,0,0,0,0,0,0,0,0,0,0,0,0)
		SphereCode=-1

		// from  http://datamirror.csdb.cn/modis/resource/doc/HDFEOS5ref.pdf

                    1       5        7   8
		16 PGSd_SNSOID  Sphere  CentMer  FE  FN

		CentMer Longitude of the central meridian
		OriginLat Latitude of the projection origin
		FE False easting in the same units as the semi-major axis
		FN False northing in the same units as the semi-major axis

		according to snyder (p 243), sinusoidal projection parameterized only by CentMer and R
		its a very simple form for sphere.

		question as usual is what are the x,y coordinates?  Assuming even spacing from the cornerss is the only thing you can do:

		UpperLeftPointMtrs=(-20015109.354000, 1111950.519667)
		LowerRightMtrs=(-18903158.834333,-0.000000)


 */