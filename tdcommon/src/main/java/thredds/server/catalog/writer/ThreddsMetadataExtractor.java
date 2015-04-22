/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

package thredds.server.catalog.writer;

import thredds.client.catalog.Dataset;
import thredds.client.catalog.ThreddsMetadata;
import thredds.client.catalog.tools.DataFactory;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dt.*;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.PointDatasetImpl;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.*;

/**
 * Extract Thredds Metadata from a feature dataset
 *
 * @author John
 * @since 1/19/2015
 */
public class ThreddsMetadataExtractor {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ThreddsMetadataExtractor.class);

  /**
   * extract info from underlying feature dataset
   * @param threddsDataset  call DataFactory().openFeatureDataset() to open it
   * @return results in ThreddsMetadata object
   * @throws IOException
   */
  public ThreddsMetadata extract(Dataset threddsDataset) throws IOException {
    ThreddsMetadata metadata = new ThreddsMetadata();
    Map<String, Object> flds = metadata.getFlds();

    try ( DataFactory.Result result = new DataFactory().openFeatureDataset(threddsDataset, null)) {
      if (result.fatalError) {
        logger.warn(" openFeatureDataset failed, errs=%s%n", result.errLog);
        return null;
      }

      if (result.featureType.isGridFeatureType()) {
        GridDataset gridDataset = (GridDataset) result.featureDataset;
        flds.put(Dataset.GeospatialCoverage, extractGeospatial(gridDataset));

        DateRange tc = extractDateRange(gridDataset);
        if (tc != null)
          flds.put(Dataset.TimeCoverage, tc);

        ThreddsMetadata.VariableGroup vars = extractVariables(threddsDataset.getDataFormatName(), gridDataset);
        if (vars != null)
          flds.put(Dataset.VariableGroups, vars);

      } else if (result.featureType.isPointFeatureType()) {
        PointDatasetImpl pobsDataset = (PointDatasetImpl) result.featureDataset;
        LatLonRect llbb = pobsDataset.getBoundingBox();
        if (null != llbb)
          flds.put(Dataset.GeospatialCoverage, new ThreddsMetadata.GeospatialCoverage(llbb, null, 0.0, 0.0));

        DateRange tc = extractDateRange(pobsDataset);
        if (tc != null)
          flds.put(Dataset.TimeCoverage, tc);

        ThreddsMetadata.VariableGroup vars = extractVariables(pobsDataset);
        if (vars != null)
          flds.put(Dataset.VariableGroups, vars);
      }

    } catch (IOException ioe) {
      logger.error("Error opening dataset " + threddsDataset.getName(), ioe);
    }

    return metadata;
  }

  public ThreddsMetadata.GeospatialCoverage extractGeospatial(GridDataset gridDataset) {
    LatLonRect llbb = null;
    CoordinateAxis1D vaxis = null;

    for (GridDataset.Gridset gridset : gridDataset.getGridsets()) {
      GridCoordSystem gsys = gridset.getGeoCoordSystem();
      if (llbb == null)
        llbb = gsys.getLatLonBoundingBox();

      CoordinateAxis1D vaxis2 = gsys.getVerticalAxis();
      if (vaxis == null)
        vaxis = vaxis2;
      else if ((vaxis2 != null) && (vaxis2.getSize() > vaxis.getSize()))
        vaxis = vaxis2;
    }

    return new ThreddsMetadata.GeospatialCoverage(llbb, vaxis, 0.0, 0.0); // LOOK can we extract dx, dy ?
  }

  public ThreddsMetadata.VariableGroup extractVariables(String fileFormat, GridDataset gridDataset) {
    List<ThreddsMetadata.Variable> vars = new ArrayList<>();
    String vocab = fileFormat;
    DataFormatType fileType = DataFormatType.getType(fileFormat);

    if ((fileType != null) && ((fileType == DataFormatType.GRIB1) || fileType == DataFormatType.GRIB2)) {
      for (GridDatatype grid : gridDataset.getGrids()) {
        String name = grid.getShortName();
        String desc = grid.getDescription();
        String units = grid.getUnitsString();
        String vname = null;
        String id = null;

        Attribute att = grid.findAttributeIgnoreCase(GribIosp.VARIABLE_ID_ATTNAME);
        if (att != null) {
          id = att.getStringValue();
          vname = att.getStringValue();
        }
        vars.add(new ThreddsMetadata.Variable(name, desc, vname, units, id));
      }

      // String vocabRef = "http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/LatestVERSION.html";

    } else { // GRID but not GRIB
      vocab = "CF-1.0";
      for (GridDatatype grid : gridDataset.getGrids()) {
        String name = grid.getShortName();
        String desc = grid.getDescription();
        String units = grid.getUnitsString();
        String vname = null;
        String id = null;

        ucar.nc2.Attribute att = grid.findAttributeIgnoreCase("standard_name");
        if (att != null)
          vname = att.getStringValue();
        vars.add(new ThreddsMetadata.Variable(name, desc, vname, units, id));
      }
    }

    Collections.sort(vars);
                                             // String vocab, String vocabHref, URI vocabUri, URI mapUri, List<Variable> variables
    return new ThreddsMetadata.VariableGroup(vocab, null, null, vars);
  }

  ///////////////////////////////////////////////////////////////////////////////


  public ThreddsMetadata.GeospatialCoverage extractGeospatial(FeatureDatasetPoint fd) {
    LatLonRect llbb = fd.getBoundingBox();
    if (llbb != null) {
      return new ThreddsMetadata.GeospatialCoverage(llbb, null, 0.0, 0.0);
    }
    return null;
  }

  public ThreddsMetadata.VariableGroup extractVariables(FeatureDatasetPoint fd) {
    List<ThreddsMetadata.Variable> vars = new ArrayList<>();

    List<VariableSimpleIF> dataVars = fd.getDataVariables();
    if (dataVars == null)
      return null;

    for (VariableSimpleIF v : dataVars) {
      String name = v.getShortName();
      String desc = v.getDescription();
      String units = v.getUnitsString();
      String vname = null;
      String id = null;

      ucar.nc2.Attribute att = v.findAttributeIgnoreCase("standard_name");
      if (att != null)
        vname = att.getStringValue();
      vars.add(new ThreddsMetadata.Variable(name, desc, vname, units, id));
    }

    Collections.sort(vars);
                                             // String vocab, String vocabHref, URI vocabUri, URI mapUri, List<Variable> variables
    return new ThreddsMetadata.VariableGroup("CF-1.0", null, null, vars);
  }

  public DateRange extractDateRange(GridDataset gridDataset) {
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
          DateUnit du = new DateUnit(time.getUnitsString());
          Date minDate = du.makeDate(time.getMinValue());
          Date maxDate = du.makeDate(time.getMaxValue());
          dateRange = new DateRange(minDate, maxDate);
        } catch (Exception e) {
          logger.warn("Illegal Date Unit " + time.getUnitsString());
          continue;
        }
      }

      if (maxDateRange == null)
        maxDateRange = dateRange;
      else
        maxDateRange.extend(dateRange);
    }

    return maxDateRange;
  }

  public DateRange extractDateRange(FeatureDatasetPoint fd) {
    return fd.getDateRange();
  }

  public CalendarDateRange extractCalendarDateRange(FeatureDatasetPoint fd) {
    return fd.getCalendarDateRange();
  }

  ////////////////////

  public CalendarDateRange extractCalendarDateRange(GridDataset gridDataset) {
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
          DateUnit du = new DateUnit(time.getUnitsString());
          Date minDate = du.makeDate(time.getMinValue());
          Date maxDate = du.makeDate(time.getMaxValue());
          dateRange = CalendarDateRange.of(minDate, maxDate);
        } catch (Exception e) {
          logger.warn("Illegal Date Unit " + time.getUnitsString());
          continue;
        }
      }

      if (maxDateRange == null)
        maxDateRange = dateRange;
      else
        maxDateRange.extend(dateRange);
    }

    return maxDateRange;
  }
}
