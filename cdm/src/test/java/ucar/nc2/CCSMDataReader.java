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

package ucar.nc2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import ncar.scd.vets.time.DateTime;
import ncar.scd.vets.time.TimeHandler;
import ncar.scd.vets.time.TimeHandlerFactory;

import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import edu.ucar.gis.ipcc.DatasetParameters;

public class CCSMDataReader extends QueuedProducer implements Callable<ExtractionResults> {
  /**
   * Logger for this class
   */
  private static final Log LOG = LogFactory.getLog(CCSMDataReader.class);

  // Need to make this configurable or move it elsewhere....
  int zIndex = 10;


  // FIXME - Get the time constraint class fixed to handle this!!!
  float tMin, tMax;

  /*
  * New state variables...
  */

  private DatasetParameters datasetParameters;
  ;

  private GeoGrid grid = null;
  private GridCoordSystem coordSys = null;
  private TimeHandler timeHandler = null;

  private DateFormatPolicy dateFormatPolicy;
  private List<TimeSlice> timeStep = new ArrayList<TimeSlice>();
  private List<NetCDFOutputCell> coordinateIndexes = new ArrayList<NetCDFOutputCell>();

  /**
   * @param outputCellQueue
   */
  public CCSMDataReader(OutputCellQueue outputCellQueue, DateFormatPolicy dateFormatPolicy, DatasetParameters datasetParameters) {
    super(outputCellQueue);
    this.dateFormatPolicy = dateFormatPolicy;
    this.datasetParameters = datasetParameters;
  }


  public ExtractionResults call() throws Exception {
    ExtractionResults results = new ExtractionResults();

    //File inputFile = new F datasetParameters.getRunMember().getSelectedDataFiles(datasetParameters.getVariableType(), datasetParameters.getStartYear(), datasetParameters.getEndYear()).get(0); // new File("C:/projects/datazone/downscale/new/parents.nc");// datasetParameters.getDataFiles().get(0);

    // create NetCDF Dataset
    NetcdfFile ncFile = NetcdfFile.open(datasetParameters.getDataFile().toString());
    NetcdfDataset dataset = null;

    try {
      dataset = new NetcdfDataset(ncFile);

      GridDataset gridDataset = new GridDataset(dataset);

      grid = gridDataset.findGridByName(datasetParameters.getVariable().getName());

      if (grid == null) {
        throw new Exception("Variable " + datasetParameters.getVariable().getName() + " not found in file " + datasetParameters.getRunMember().getPath());
      }

      Dimension dimX = grid.getXDimension();
      Dimension dimY = grid.getYDimension();

//             read coordinate system
      coordSys = grid.getCoordinateSystem();

      // Setup the time handler...
//             obtain TimeHandler to convert (and validate) dates and times
      // according to appropriate calendar


      String units = coordSys.getTimeAxis().getUnitString();
      String calendar = coordSys.getTimeAxis().findAttribute("calendar").getStringValue();
      timeHandler = TimeHandlerFactory.getTimeHandler(units, calendar);

      AxisConstraint2<String> timeCons = datasetParameters.getTimeConstraint();

//             process time limits according to calendar defined in data files
      DateTime _tMin = timeHandler.getDateTime(timeCons.getMin());
      tMin = DateTime.toNumber(_tMin);
      DateTime _tMax = timeHandler.getDateTime(timeCons.getMax());
      tMax = DateTime.toNumber(_tMax);


      //results = extractData(grid);

      // Load the time slices that fall into range....
      loadTimeSlices();

      // Load the coordinate points that fall into range....
      loadCoordinateIndexes();


      extractCellValues(results);


      outputCellQueue.setComplete();

      // Set the dates...
      for (TimeSlice timeSlice : timeStep) {
        results.getDates().add(timeSlice.getDateTimeString());
      }


    } catch (Exception e) {
      LOG.error("call()", e); //$NON-NLS-1$
      throw e;
    } finally {
      if (null != dataset) {
        dataset.close();
      }
    }

    return results;
  }


  protected void loadCoordinateIndexes() throws Exception {

    Array latValues = coordSys.getYHorizAxis().read();
    Array lonValues = coordSys.getXHorizAxis().read();

    Index latIndex = latValues.getIndex();
    Index lonIndex = lonValues.getIndex();

    for (int latLoopIndex = 0; latLoopIndex < latIndex.getSize(); latLoopIndex++) {
      latIndex.set(latLoopIndex);
      Latitude lat = new Latitude(latValues.getFloat(latIndex));
      if (datasetParameters.getLatitudeConstraint().containsValue(lat)) {

        for (int lonLoopIndex = 0; lonLoopIndex < lonIndex.getSize(); lonLoopIndex++) {
          lonIndex.set(lonLoopIndex);
          Longitude lon = new Longitude(lonValues.getFloat(lonIndex));
          if (datasetParameters.getLongitudeConstraint().containsValue(lon)) {

            NetCDFOutputCell netCDFOutputCell = new NetCDFOutputCell();
            netCDFOutputCell.setLon(lon);
            netCDFOutputCell.setLat(lat);
            netCDFOutputCell.setGridIndexX(lonLoopIndex);
            netCDFOutputCell.setGridIndexY(latLoopIndex);

            coordinateIndexes.add(netCDFOutputCell);

          }

        }

      }

    }
    if (coordinateIndexes.isEmpty()) {
      throw new RuntimeException("Couldn't find any coordinates that matched selection.");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Loaded " + coordinateIndexes.size() + " coordinate index(es)");
    }
  }

  protected void loadTimeSlices() throws Exception {

//        CoordinateAxis1DTime timeAxis = coordSys.getTimeAxis1D();
//
//
//        String timeUnit = timeAxis.getTimeResolution().getUnitString();
//        if (logger.isDebugEnabled()) {
//            logger.debug("loadTimeSlices() - String timeUnit=" + timeUnit);
//        }
//
//        DateRange range = timeAxis.getDateRange();
////        Downscaled...
////        DEBUG-loadTimeSlices() - String timeUnit=days
////        DEBUG-Start: 1998-09-16 12:00:00Z End: 2008-08-13 12:00:00Z
//
////        CCSM - TAS
////        DEBUG-loadTimeSlices() - String timeUnit=days
////        DEBUG-Start: 1868-10-17 12:00:00Z End: 1998-08-16 12:00:00Z
//
//        logger.debug("Start: " + range.getStart().getText() + " End: " + range.getEnd().getText());
//
//        Date[] dates = timeAxis.getTimeDates();
//
//        for (int i = 0; i < dates.length; i++) {
//            Date date = dates[i];
//            if (logger.isDebugEnabled()) {
//                //logger.debug("loadTimeSlices() - Date date=" + date);
//            }
//
//        }
//
//        // TODO - Review the findTimeIndexFromDate function to see if that works better...


    int tCounter = 0; // works with time step
    // loop over all times in file, read XY data slice
    Array timeValues = coordSys.getTimeAxis().read(); // read times from
    // current file
    Index timeIndex = timeValues.getIndex();
    int tStep = datasetParameters.getTimeConstraint().getStep();
    for (int i = 0; i < timeIndex.getSize(); i++) {
      timeIndex.set(i);
      float time = timeValues.getFloat(timeIndex); // time from file,
      // i.e. usually
      // relative time
      DateTime dateTime = timeHandler.getDateTime(time);
      if (LOG.isDebugEnabled()) {
        //logger.debug("loadTimeSlices() - DateTime dateTime= " + dateTime.get(DateTime.YEAR) + "-" + dateTime.get(DateTime.MONTH) + "-" + dateTime.get(DateTime.DAY));
      }

      float t = DateTime.toNumber(dateTime);

      if (tMin <= t && t <= tMax) { // time within limits

        if (tCounter++ % tStep == 0) { // select one every "tStep"

          Array xySlice = grid.readYXData(i, zIndex);
          timeStep.add(new TimeSlice(dateTime, dateFormatPolicy.getFormatedDateTime(timeHandler, dateTime), xySlice));

        }

      } // time within limits

    } // loop over all times in file

    if (LOG.isDebugEnabled()) {
      LOG.debug("Loaded " + timeStep.size() + " time step(s)");
    }
  }

  /**
   * Extracts all the cell values into OutputCells and stores them in the queue for reading.
   *
   * @throws InterruptedException
   */
  protected void extractCellValues(ExtractionResults results) throws InterruptedException {
    // Loop over all the coordinate pairs
    for (NetCDFOutputCell netCDFOutputCell : coordinateIndexes) {

      OutputCell outputCell = new OutputCell();
      outputCell.setLon(netCDFOutputCell.getLon());
      outputCell.setLat(netCDFOutputCell.getLat());

      for (TimeSlice timeSlice : timeStep) {

        CellValue cellValue = new CellValue();
        cellValue.setDateTime(timeSlice.getDateTime());
        cellValue.setDataTimeString(timeSlice.getDateTimeString());
        cellValue.setValue(timeSlice.getValue(netCDFOutputCell.getGridIndexY(), netCDFOutputCell.getGridIndexX()));

        outputCell.getCellValues().add(cellValue);

      }

      if (LOG.isDebugEnabled()) {
        Set uniqueValues = new HashSet();
        for (CellValue cellValue : outputCell.getCellValues()) {

          Float value = new Float(cellValue.getValue());

          if (-9999 != value) {
            uniqueValues.add(value);
            // Track the min/max values
            results.setExtractionValue(value);
          }
        }
        if (uniqueValues.size() == 1) {
          LOG.warn("Cell only contained 1 unique value for all timesteps");
        }
      }

      outputCellQueue.putOutputCell(outputCell);

    }
  }

} // Netcdf2GisConverter
