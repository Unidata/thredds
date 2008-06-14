/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.ncml;

import ucar.nc2.dataset.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateFromString;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.fmrc.ForecastModelRunInventory;
import ucar.unidata.util.StringUtil;
import ucar.ma2.*;

import java.io.IOException;
import java.util.*;

/**
 * Implement NcML Forecast Model Run Collection Aggregation
 * with files that contain a single forecast time.
 *
 * @author caron
 */
public class AggregationFmrcSingle extends AggregationFmrc {
  private Calendar cal = new GregorianCalendar(); // for date computations

  private Map<Date, List<DatasetOuterDimension>> runHash = new HashMap<Date, List<DatasetOuterDimension>>();
  private List<Date> runs; // list of run dates

  private CoordinateAxis1D timeAxis = null;
  private int max_times = 0;
  private Dataset typicalDataset = null;
  private NetcdfFile typicalFile;
  private GridDataset typicalGridDataset = null;
  private boolean debug = false;

  private String runMatcher, forecastMatcher, offsetMatcher; // scanFmrc

  public AggregationFmrcSingle(NetcdfDataset ncd, String dimName, String recheckS) {
    super(ncd, dimName, Type.FORECAST_MODEL_SINGLE, recheckS);
  }

  public void addDirectoryScanFmrc(String dirName, String suffix, String regexpPatternString, String subdirs, String olderThan,
                                   String runMatcher, String forecastMatcher, String offsetMatcher) throws IOException {

    this.runMatcher = runMatcher;
    this.forecastMatcher = forecastMatcher;
    this.offsetMatcher = offsetMatcher;

    this.enhance = NetcdfDataset.EnhanceMode.All;
    isDate = true;

    CrawlableScanner d = new CrawlableScanner(null, dirName, suffix, regexpPatternString, subdirs, olderThan);
    datasetManager.addDirectoryScan(d);
  }

  @Override
  protected void closeDatasets() throws IOException {
    if (typicalGridDataset != null) {
      typicalGridDataset.close();
    }

    for (Dataset ds : datasets) {
      OpenDataset ods = (OpenDataset) ds;
      if (ods.openFile != null)
        ods.openFile.close();
    }
  }

  @Override
  protected void buildDataset(CancelTask cancelTask) throws IOException {
    buildDataset(typicalDataset, typicalFile, typicalGridDataset, cancelTask);
  }

  @Override
  protected void makeDatasets(CancelTask cancelTask) throws IOException {

    // find the runtime, forecast time coordinates, put in list
    runHash = new HashMap<Date, List<DatasetOuterDimension>>();

    Collection<MyCrawlableDataset> fileList = datasetManager.getFiles();
    for (MyCrawlableDataset myf : fileList) {
      String location = StringUtil.replace(myf.file.getPath(), '\\', "/");

      // parse for rundate
      if (runMatcher != null) {
        myf.runDate = DateFromString.getDateUsingDemarkatedMatch(location, runMatcher, '#');
        if (null == myf.runDate) {
          logger.error("Cant extract rundate from =" + location + " using format " + runMatcher);
          continue;
        }
      }

      // parse for forecast date
      if (forecastMatcher != null) {
        myf.dateCoord = DateFromString.getDateUsingDemarkatedMatch(location, forecastMatcher, '#');
        if (null == myf.dateCoord) {
          logger.error("Cant extract forecast date from =" + location + " using format " + forecastMatcher);
          continue;
        }
        myf.dateCoordS = formatter.toDateTimeStringISO(myf.dateCoord);
      }

      // parse for forecast offset
      if (offsetMatcher != null) {
        myf.offset = DateFromString.getHourUsingDemarkatedMatch(location, offsetMatcher, '#');
        if (null == myf.offset) {
          logger.error("Cant extract forecast offset from =" + location + " using format " + offsetMatcher);
          continue;
        }
        myf.dateCoord = addHour(myf.runDate, myf.offset);
        myf.dateCoordS = formatter.toDateTimeStringISO(myf.dateCoord);
      }

      // LOOK - should cache the GridDataset directly
      // create the dataset wrapping this file, each is 1 forecast time coordinate of the nested aggregation
      DatasetOuterDimension ds = (DatasetOuterDimension)
              makeDataset(location, location, null, myf.dateCoordS, null, NetcdfDataset.EnhanceMode.All, null);
      ds.coordValueDate = myf.dateCoord;
      ds.ncoord = 1;

      // add to list for given run date
      List<DatasetOuterDimension> runDatasets = runHash.get(myf.runDate);
      if (runDatasets == null) {
        runDatasets = new ArrayList<DatasetOuterDimension>();
        runHash.put(myf.runDate, runDatasets);
      }
      if (debug)
        System.out.println("  adding " + myf.file.getPath() + " forecast date= " + myf.dateCoordS + "(" + myf.dateCoord + ")"
            + " run date= " + formatter.toDateTimeStringISO(myf.runDate));
      runDatasets.add(ds);

      if (typicalDataset == null)
        typicalDataset = ds;
    }

    // LOOK - should cache the GridDataset directly    
    // open a "typical" dataset and make a GridDataset
    typicalFile = typicalDataset.acquireFile(cancelTask);
    NetcdfDataset typicalDS = (typicalFile instanceof NetcdfDataset) ? (NetcdfDataset) typicalFile : new NetcdfDataset(typicalFile);
    if (typicalDS.getEnhanceMode() == NetcdfDataset.EnhanceMode.None)
      typicalDS.enhance();
    GridDataset gds = new ucar.nc2.dt.grid.GridDataset(typicalDS);

    // find the one time axis
    for (GridDatatype grid : gds.getGrids()) {
      GridCoordSystem gcc = grid.getCoordinateSystem();
      timeAxis = gcc.getTimeAxis1D();
      if (null != timeAxis)
        break;
    }

    if (timeAxis == null)
      throw new IllegalStateException("No time variable");

    // create new list of Datasets
    datasets = new ArrayList<Dataset>();
    for (Aggregation.Dataset dataset : explicitDatasets) {
      datasets.add(dataset);
    }

    // loop over the runs; each becomes a nested dataset
    max_times = 0;
    runs = new ArrayList<Date>(runHash.keySet());
    Collections.sort(runs);
    for (Date runDate : runs) {
      String runDateS = formatter.toDateTimeStringISO(runDate);

      List<DatasetOuterDimension> runDatasets = runHash.get(runDate);
      max_times = Math.max(max_times, runDatasets.size());

      // within each list, sort the datasets by time coordinate
      Collections.sort(runDatasets, new Comparator<DatasetOuterDimension>() {
        public int compare(DatasetOuterDimension ds1, DatasetOuterDimension ds2) {
          return ds1.coordValueDate.compareTo(ds2.coordValueDate);
        }
      });

      // create the dataset wrapping this run, each is 1 runtime coordinate of the outer aggregation
      NetcdfDataset ncd = new NetcdfDataset();
      ncd.setLocation("Run" + runDateS);
      DateFormatter format = new DateFormatter();
      if (debug) System.out.println("Run" + format.toDateTimeString(runDate));

      AggregationExisting agg = new AggregationExisting(ncd, timeAxis.getName(), null); // LOOK: dim name, existing vs new ??
      for (Dataset dataset : runDatasets) {
        agg.addDataset(dataset);
        if (debug)
          System.out.println("  adding Forecast " + format.toDateTimeString( ((DatasetOuterDimension)dataset).coordValueDate) + " " + dataset.getLocation());
      }
      ncd.setAggregation(agg);
      agg.finish(cancelTask);

      datasets.add(new OpenDataset(ncd, runDate, runDateS));
    }

    typicalGridDataset = gds;
  }

  private Date addHour(Date d, double hour) {
    cal.setTime(d);

    int ihour = (int) hour;
    int imin = (int) (hour - ihour) * 60;
    cal.add(Calendar.HOUR_OF_DAY, ihour);
    cal.add(Calendar.MINUTE, imin);
    return cal.getTime();
  }

  // used in buildDataset
  @Override
  protected Dataset getTypicalDataset() throws IOException {
    return typicalDataset;
  }

  // for the case that we dont have a fmrcDefinition.
  @Override
  protected void makeTimeCoordinate(GridDataset gds, CancelTask cancelTask) throws IOException {
    String innerDimName = timeAxis.getName();
    Dimension innerDim = new Dimension(innerDimName, max_times);
    ncDataset.removeDimension(null, innerDimName); // remove previous declaration, if any
    ncDataset.addDimension(null, innerDim);

    int[] shape = new int[]{runs.size(), max_times};
    Array timeCoordVals = Array.factory(DataType.DOUBLE, shape);
    MAMath.setDouble(timeCoordVals, Double.NaN); // anything not set is missing
    Index ima = timeCoordVals.getIndex();

    // loop over the runs, calculate the offset for each dataset
    Date baseDate = null;
    for (int i = 0; i < runs.size(); i++) {
      Date runDate = runs.get(i);
      if (baseDate == null) baseDate = runDate;

      List<DatasetOuterDimension> runDatasets = runHash.get(runDate);
      for (int j = 0; j < runDatasets.size(); j++) {
        DatasetOuterDimension dataset = runDatasets.get(j);
        double offset = ForecastModelRunInventory.getOffsetInHours(baseDate, dataset.coordValueDate);
        timeCoordVals.setDouble(ima.set(i, j), offset);
      }
    }

    // construct new variable, replace old one, set values
    String dims = dimName + " " + innerDimName;
    String units = "hours since " + formatter.toDateTimeStringISO(baseDate);
    String desc = "calculated forecast date from AggregationFmrcSingle processing";
    VariableDS vagg = new VariableDS(ncDataset, null, null, innerDimName, DataType.DOUBLE, dims, units, desc);
    vagg.setCachedData(timeCoordVals, false);
    DatasetConstructor.transferVariableAttributes(timeAxis, vagg);
    vagg.addAttribute(new Attribute("units", units));
    vagg.addAttribute(new Attribute("long_name", desc));
    vagg.addAttribute(new ucar.nc2.Attribute("missing_value", Double.NaN));

    //ncDataset.removeVariable(null, vagg.getName());
    ncDataset.addCoordinateAxis(vagg);

    if (debug) System.out.println("FmrcAggregation: promoted timeCoord " + innerDimName);
  }

  // the timeAxis will be 2D, and there's only one
  @Override
  protected void readTimeCoordinates(VariableDS timeAxis, CancelTask cancelTask) throws IOException {

    // redo the time dimension, makes things easier if you dont replace Dimension, just modify the length
    String dimName = timeAxis.getName();
    Dimension timeDim = ncDataset.findDimension(dimName); // LOOK use group
    timeDim.setLength(max_times);

    // reset all variables using this dimension
    List<Variable> vars = ncDataset.getVariables();
    for (Variable v : vars) {
      if (v.findDimensionIndex(dimName) >= 0) {
        v.resetDimensions();   
        v.setCachedData(null, false); // get rid of any cached data, since its now wrong
      }
    }

    // create the data array for the time coordinate
    int[] shape = new int[]{runs.size(), max_times};
    Array timeCoordVals = Array.factory(DataType.DOUBLE, shape);
    MAMath.setDouble(timeCoordVals, Double.NaN); // anything not set is missing
    Index ima = timeCoordVals.getIndex();

    // loop over the runs, calculate the offset for each dataset
    Date baseDate = null;
    for (int i = 0; i < runs.size(); i++) {
      Date runDate = runs.get(i);
      if (baseDate == null) baseDate = runDate;

      List<DatasetOuterDimension> runDatasets = runHash.get(runDate);
      for (int j = 0; j < runDatasets.size(); j++) {
        DatasetOuterDimension dataset = runDatasets.get(j);
        double offset = ForecastModelRunInventory.getOffsetInHours(baseDate, dataset.coordValueDate);
        timeCoordVals.setDouble(ima.set(i, j), offset);
      }
    }
    timeAxis.setCachedData(timeCoordVals, true);

    String units = "hours since " + formatter.toDateTimeStringISO(baseDate);
    timeAxis.addAttribute(new Attribute("units", units));
  }

  /**
   * Encapsolates a NetcdfFile that is a component of the aggregation.
   * public for NcMLWriter
   */
  public class OpenDataset extends DatasetOuterDimension {
    private NetcdfFile openFile;

    /**
     * Dataset constructor with an opened NetcdfFile.
     * Used in nested aggregations like scanFmrc.
     *
     * @param openFile       already opened file
     * @param coordValueDate has this coordinate as a date
     * @param coordValue     has this coordinate as a String
     */
    protected OpenDataset(NetcdfFile openFile, Date coordValueDate, String coordValue) {
      super(openFile.getLocation());
      this.openFile = openFile;
      this.ncoord = 1;
      this.coordValueDate = coordValueDate;
      this.coordValue = coordValue;
    }

    @Override
    protected NetcdfFile acquireFile(CancelTask cancelTask) throws IOException {
      return openFile;
    }

    @Override
    protected void close(NetcdfFile ncfile) throws IOException {
      if (ncfile == null) return;
      cacheVariables(ncfile);
    }

  }

}
