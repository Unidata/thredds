/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.thredds;

import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import thredds.catalog.ThreddsMetadata;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.DataFormatType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dt.*;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Extract THREDDS metadata from the underlying CDM dataset.
 *
 * @author caron
 */
public class MetadataExtractor {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MetadataExtractor.class);

  /**
   * Extract the lat/lon/alt bounding boxes from the dataset.
   * @param threddsDataset open this dataset
   * @return ThreddsMetadata.GeospatialCoverage, or null if unable.
   * @throws IOException on read error
   */
  static public ThreddsMetadata.GeospatialCoverage extractGeospatial(InvDatasetImpl threddsDataset) throws IOException {
    ThreddsDataFactory.Result result = null;

    try {
      result = new ThreddsDataFactory().openFeatureDataset(threddsDataset, null);
      if (result.fatalError) {
        System.out.println(" openDatatype errs=" + result.errLog);
        return null;
      }

      if (result.featureType == FeatureType.GRID) {
        System.out.println(" GRID=" + result.location);
        GridDataset gridDataset = (GridDataset) result.featureDataset;
        return extractGeospatial( gridDataset);

      } else if (result.featureType == FeatureType.POINT) {
        PointObsDataset pobsDataset = (PointObsDataset) result.featureDataset;
        LatLonRect llbb = pobsDataset.getBoundingBox();
        if (null != llbb) {
          ThreddsMetadata.GeospatialCoverage gc = new ThreddsMetadata.GeospatialCoverage();
          gc.setBoundingBox(llbb);
          return gc;
        }
      } else if (result.featureType == FeatureType.STATION) {
        StationObsDataset sobsDataset = (StationObsDataset) result.featureDataset;
        LatLonRect llbb = sobsDataset.getBoundingBox();
        if (null != llbb) {
          ThreddsMetadata.GeospatialCoverage gc = new ThreddsMetadata.GeospatialCoverage();
          gc.setBoundingBox(llbb);
          return gc;
        }
      }

    } finally {
      try {
        if ((result != null) && (result.featureDataset != null))
          result.featureDataset.close();
      } catch (IOException ioe) {
        logger.error("Closing dataset "+result.featureDataset, ioe);
      }
    }

    return null;
  }

  static public ThreddsMetadata.GeospatialCoverage extractGeospatial(GridDataset gridDataset) {
    ThreddsMetadata.GeospatialCoverage gc = new ThreddsMetadata.GeospatialCoverage();
    LatLonRect llbb = null;
    CoordinateAxis1D vaxis = null;

    for(GridDataset.Gridset gridset : gridDataset.getGridsets()) {
      GridCoordSystem gsys = gridset.getGeoCoordSystem();
      if (llbb == null)
        llbb = gsys.getLatLonBoundingBox();

      CoordinateAxis1D vaxis2 = gsys.getVerticalAxis();
      if (vaxis == null)
        vaxis = vaxis2;
      else if ((vaxis2 != null) && (vaxis2.getSize() > vaxis.getSize()))
        vaxis = vaxis2;
    }

    if (llbb != null)
      gc.setBoundingBox(llbb);
    if (vaxis != null)
      gc.setVertical(vaxis);
    return gc;
  }

  /**
   * Extract a list of data variables (and their canonical names if possible) from the dataset.
   * @param threddsDataset open this dataset
   * @return ThreddsMetadata.Variables, or null if unable.
   * @throws IOException on read error
   */
  static public ThreddsMetadata.Variables extractVariables(InvDatasetImpl threddsDataset) throws IOException {
    ThreddsDataFactory.Result result = null;

    try {
      result = new ThreddsDataFactory().openFeatureDataset(threddsDataset, null);
      if (result.fatalError) {
        System.out.println(" openDatatype errs=" + result.errLog);
        return null;
      }

      if (result.featureType == FeatureType.GRID) {
        // System.out.println(" extractVariables GRID=" + result.location);
        GridDataset gridDataset = (GridDataset) result.featureDataset;
        return extractVariables(threddsDataset, gridDataset);

      } else if ((result.featureType == FeatureType.STATION) || (result.featureType == FeatureType.POINT)) {
        PointObsDataset pobsDataset = (PointObsDataset) result.featureDataset;
        ThreddsMetadata.Variables vars = new ThreddsMetadata.Variables("CF-1.0");
        for (VariableSimpleIF vs  : pobsDataset.getDataVariables()) {
          ThreddsMetadata.Variable v = new ThreddsMetadata.Variable();
          vars.addVariable( v);

          v.setName( vs.getShortName());
          v.setDescription( vs.getDescription());
          v.setUnits( vs.getUnitsString());

          ucar.nc2.Attribute att = vs.findAttributeIgnoreCase("standard_name");
          if (att != null)
            v.setVocabularyName(att.getStringValue());
        }
        vars.sort();
        return vars;
      }

    } finally {
      try {
        if ((result != null) && (result.featureDataset != null))
          result.featureDataset.close();
      } catch (IOException ioe) {
        logger.error("Closing dataset "+result.featureDataset, ioe);
      }
    }

    return null;
  }

  static public ThreddsMetadata.Variables extractVariables(InvDatasetImpl threddsDataset, GridDataset gridDataset) {
    return extractVariables( threddsDataset.getDataFormatType(), gridDataset);
  }

  static public ThreddsMetadata.Variables extractVariables(thredds.catalog.DataFormatType fileFormat, GridDataset gridDataset) {
    if ((fileFormat != null) && (fileFormat.equals(DataFormatType.GRIB1) || fileFormat.equals(DataFormatType.GRIB2))) {
      ThreddsMetadata.Variables vars = new ThreddsMetadata.Variables(fileFormat.toString());
      for (GridDatatype grid : gridDataset.getGrids()) {
        ThreddsMetadata.Variable v = new ThreddsMetadata.Variable();
        v.setName(grid.getFullName());
        v.setDescription(grid.getDescription());
        v.setUnits(grid.getUnitsString());

        Attribute att = grid.findAttributeIgnoreCase("Grib_Variable_Id");
        if (att != null) {
          v.setVocabularyId(att.getStringValue());
          v.setVocabularyName(att.getStringValue());
        } else {
          att = grid.findAttributeIgnoreCase("Grib_Parameter");
          v.setVocabularyId(att);
        }
        vars.addVariable(v);
      }
      vars.sort();
      return vars;

    } else { // GRID but not GRIB
      ThreddsMetadata.Variables vars = new ThreddsMetadata.Variables("CF-1.0");
      for (GridDatatype grid : gridDataset.getGrids()) {
        ThreddsMetadata.Variable v = new ThreddsMetadata.Variable();
        vars.addVariable(v);

        v.setName(grid.getFullName());
        v.setDescription(grid.getDescription());
        v.setUnits(grid.getUnitsString());

        ucar.nc2.Attribute att = grid.findAttributeIgnoreCase("standard_name");
        if (att != null)
          v.setVocabularyName(att.getStringValue());
      }
      vars.sort();
      return vars;
    }

  }

  static public CalendarDateRange extractCalendarDateRange(GridDataset gridDataset) {
    CalendarDateRange maxDateRange = null;

    for (GridDataset.Gridset gridset : gridDataset.getGridsets()) {
      GridCoordSystem gsys = gridset.getGeoCoordSystem();
      CalendarDateRange dateRange;

      CoordinateAxis1DTime time1D = gsys.getTimeAxis1D();
      if (time1D != null) {
        dateRange = time1D.getCalendarDateRange();
      } else {
        CoordinateAxis time = gsys.getTimeAxis();
        if (time == null)
          continue;
        
        try {
          DateUnit du = new DateUnit( time.getUnitsString());
          Date minDate = du.makeDate(time.getMinValue());
          Date maxDate = du.makeDate(time.getMaxValue());
          dateRange = CalendarDateRange.of( minDate, maxDate);
        } catch (Exception e) {
          logger.warn("Illegal Date Unit "+time.getUnitsString());
          continue;
        }
      }

      if (maxDateRange == null)
        maxDateRange = dateRange;
      else
        maxDateRange = maxDateRange.extend( dateRange);
    }

    return maxDateRange;
  }

  ///////////////////////////////////////////////////////////////////////////////
  static public ThreddsMetadata.Variables extractVariables(FeatureDatasetPoint fd) {
    ThreddsMetadata.Variables vars = new ThreddsMetadata.Variables("CF-1.5");
    List<VariableSimpleIF> dataVars =  fd.getDataVariables();
    if (dataVars == null)
      return vars;

    for (VariableSimpleIF v : dataVars) {
      ThreddsMetadata.Variable tv = new ThreddsMetadata.Variable();
      vars.addVariable(tv);

      tv.setName(v.getShortName());
      tv.setDescription(v.getDescription());
      tv.setUnits(v.getUnitsString());

      ucar.nc2.Attribute att = v.findAttributeIgnoreCase("standard_name");
      if (att != null)
         tv.setVocabularyName(att.getStringValue());
    }
    vars.sort();
    return vars;
  }

  static public ThreddsMetadata.GeospatialCoverage extractGeospatial(FeatureDatasetPoint fd) {
    LatLonRect llbb = fd.getBoundingBox();
    if (llbb != null) {
      ThreddsMetadata.GeospatialCoverage gc = new ThreddsMetadata.GeospatialCoverage();
      gc.setBoundingBox(llbb);
      return gc;
    }
    return null;
  }

  static public CalendarDateRange extractCalendarDateRange(FeatureDatasetPoint fd) {
    return fd.getCalendarDateRange();
  }

}
