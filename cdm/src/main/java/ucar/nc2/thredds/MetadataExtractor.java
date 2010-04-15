/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

package ucar.nc2.thredds;

import ucar.nc2.constants.FeatureType;
import thredds.catalog.ThreddsMetadata;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.DataFormatType;
import ucar.nc2.units.DateRange;

import java.io.IOException;
import java.util.Date;

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

          v.setName( vs.getName());
          v.setDescription( vs.getDescription());
          v.setUnits( vs.getUnitsString());

          ucar.nc2.Attribute att = vs.findAttributeIgnoreCase("standard_name");
          v.setVocabularyName( (att != null) ? att.getStringValue() : "N/A");
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

    thredds.catalog.DataFormatType fileFormat = threddsDataset.getDataFormatType();
    if ((fileFormat != null) &&
        (fileFormat.equals(DataFormatType.GRIB1) || fileFormat.equals(DataFormatType.GRIB2))) {
      boolean isGrib1 = fileFormat.equals(DataFormatType.GRIB1);
      ThreddsMetadata.Variables vars = new ThreddsMetadata.Variables(fileFormat.toString());
      for (GridDatatype grid : gridDataset.getGrids()) {
        ThreddsMetadata.Variable v = new ThreddsMetadata.Variable();
        v.setName(grid.getName());
        v.setDescription(grid.getDescription());
        v.setUnits(grid.getUnitsString());

        //ucar.nc2.Attribute att = grid.findAttributeIgnoreCase("GRIB_param_number");
        //String paramNumber = (att != null) ? att.getNumericValue().toString() : null;
        if (isGrib1) {
          v.setVocabularyName(grid.findAttValueIgnoreCase("GRIB_param_name", "ERROR"));
          v.setVocabularyId(grid.findAttributeIgnoreCase("GRIB_param_id"));
        } else {
          String paramDisc = grid.findAttValueIgnoreCase("GRIB_param_discipline", "");
          String paramCategory = grid.findAttValueIgnoreCase("GRIB_param_category", "");
          String paramName = grid.findAttValueIgnoreCase("GRIB_param_name", "");
          v.setVocabularyName(paramDisc + " / " + paramCategory + " / " + paramName);
          v.setVocabularyId(grid.findAttributeIgnoreCase("GRIB_param_id"));
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

        v.setName(grid.getName());
        v.setDescription(grid.getDescription());
        v.setUnits(grid.getUnitsString());

        ucar.nc2.Attribute att = grid.findAttributeIgnoreCase("standard_name");
        v.setVocabularyName((att != null) ? att.getStringValue() : "N/A");
      }
      vars.sort();
      return vars;
    }

  }

  static public DateRange extractDateRange(GridDataset gridDataset) {
    DateRange maxDateRange = null;

    for (GridDataset.Gridset gridset : gridDataset.getGridsets()) {
      GridCoordSystem gsys = gridset.getGeoCoordSystem();
      DateRange dateRange;

      CoordinateAxis1DTime time1D = gsys.getTimeAxis1D();
      if (time1D != null) {
        dateRange = time1D.getDateRange();
      } else {
        CoordinateAxis time = gsys.getTimeAxis();
        if (time == null)
          continue;
        
        try {
          DateUnit du = new DateUnit( time.getUnitsString());
          Date minDate = du.makeDate(time.getMinValue());
          Date maxDate = du.makeDate(time.getMaxValue());
          dateRange = new DateRange( minDate, maxDate);
        } catch (Exception e) {
          logger.warn("Illegal Date Unit "+time.getUnitsString());
          continue;
        }
      }

      if (maxDateRange == null)
        maxDateRange = dateRange;
      else
        maxDateRange.extend( dateRange);
    }

    return maxDateRange;
  }


}
