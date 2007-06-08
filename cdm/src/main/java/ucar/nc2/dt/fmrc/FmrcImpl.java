// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.nc2.dt.fmrc;

import ucar.nc2.dataset.*;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;
import ucar.nc2.*;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;

/**
 * ForecastModelRunCollection implementation.
 * Uses a GridDataset that has two time dimensions.
 * Assume all grids have the same runTime dimension.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class FmrcImpl implements ForecastModelRunCollection {
  static private final String BEST = "best";
  static private final String RUN = "run";
  static private final String FORECAST = "forecast";
  static private final String OFFSET = "offset";

  ///////////////////////////

  private NetcdfDataset ncd_2dtime; // netcdfDataset with run time and forecast time
  private ucar.nc2.dt.GridDataset gds; // the corresponding GridDataset
  private Date baseDate;            // first runtime : offsets calculated from here
  private String runtimeDimName;    // the runtime dimension name

  private List<Gridset> gridsets; // divide grids into sets based on their (Forecast) time coordinate
  private Map<String, Gridset> gridHash; // key = grid name, value = Gridset : associate a Gridset with each grid.
  private Set<String> coordSet;  // time coord names, including runtime

  /* apparently not used, except for debugging ??
  private Map<Date, List<Inventory>> runMapAll;    // key = run Date, value = List<Inventory>
  private Map<Date, List<Inventory>> timeMapAll;   // key = forecast Date, value = List<Inventory>
  private Map<Double, List<Inventory>> offsetMapAll; // key = offset Double, value = List<Inventory>
  private List<Inventory> bestListAll;                   // best List<Inventory>  */

  private List<Date> runtimes;  // List of all possible runtime Date
  private List<Date> forecasts;  // List of all possible forecast Date
  private List<Double> offsets;  // List of all possible offset Double

  private boolean debugSync = false;

  public FmrcImpl(String filename) throws IOException {
    this(ucar.nc2.dataset.NetcdfDatasetCache.acquire(filename, null));
  }

  public FmrcImpl(NetcdfDataset ncd) throws IOException {
    init(ncd);
  }

  /**
   * Check if file has changed, and reread metadata if needed.
   * All previous object references (variables, dimensions, etc) may become invalid - you must re-obtain.
   *
   * @return true if file was changed.
   * @throws IOException
   */
  public boolean sync() throws IOException {
    boolean changed = ncd_2dtime.syncExtend();
    if (changed) {
      if (debugSync) System.out.println("sync Fmrc "+ncd_2dtime.getLocation());
      init(ncd_2dtime);
    }
    return changed;
  }

  public ucar.nc2.dt.GridDataset getGridDataset() {
    return gds;
  }

  // close and release all resources
  public void close() throws IOException {
    gds.close();
  }

  private void init(NetcdfDataset ncd) throws IOException {
    this.ncd_2dtime = ncd;
    if (!ncd.isEnhanced())
      ncd.enhance();
    // ncd.setCached(3); // dont allow a normal close LOOK why ?? who is managing ??

    gridHash = new HashMap<String, Gridset>();  // key = grid name, value = Gridset
    coordSet = new HashSet<String>();  // time coord names
    runtimes = null;

    gds = new ucar.nc2.dt.grid.GridDataset(ncd);
    List<GridDatatype> grids = gds.getGrids();
    if (grids.size() == 0)
      throw new IllegalArgumentException("no grids");

    // collect the grids into Gridsets, based on what time axis they use
    HashMap<CoordinateAxis, Gridset> timeAxisHash = new HashMap<CoordinateAxis, Gridset>(); // key = timeAxis, value = Gridset
    for (GridDatatype grid : grids) {
      GridCoordSystem gcs = grid.getCoordinateSystem();
      CoordinateAxis timeAxis = gcs.getTimeAxis();
      if (timeAxis != null) {
        Gridset gset = timeAxisHash.get(timeAxis); // group by timeAxis
        if (gset == null) {
          gset = new Gridset(timeAxis, gcs);
          timeAxisHash.put(timeAxis, gset);
          coordSet.add(timeAxis.getName());
        }
        gset.gridList.add(grid);
        gridHash.put(grid.getName(), gset);
      }

      // assume runtimes are always the same
      if ((runtimes == null) && (gcs.getRunTimeAxis() != null)) {
        CoordinateAxis1DTime runtimeCoord = gcs.getRunTimeAxis();
        Date[] runDates = runtimeCoord.getTimeDates();
        baseDate = runDates[0];
        runtimes = Arrays.asList(runDates);
        runtimeDimName = runtimeCoord.getDimension(0).getName();
        coordSet.add(runtimeCoord.getName());
      }
    }

    if (runtimes == null)
      throw new IllegalArgumentException("no runtime dimension");

    // generate the lists of possible forecasts, offsets, using all gridsets
    HashSet<Date> forecastSet = new HashSet<Date>();
    HashSet<Double> offsetSet = new HashSet<Double>();
    gridsets = new ArrayList<Gridset>(timeAxisHash.values());
    for (Gridset gridset : gridsets) {
      for (int run = 0; run < runtimes.size(); run++) {
        Date runDate = runtimes.get(run);

        // we assume that with the same taxis, we get the same results here
        CoordinateAxis1DTime timeCoordRun = gridset.gcs.getTimeAxisForRun(run);
        Date[] forecastDates = timeCoordRun.getTimeDates();

        for (int time = 0; time < forecastDates.length; time++) {
          Date forecastDate = forecastDates[time];
          forecastSet.add(forecastDate);

          double hourOffset = getOffsetHour(runDate, forecastDate);
          offsetSet.add(hourOffset);
        }
      }
    }
    // turn those into lists, with unique values
    forecasts = Arrays.asList(forecastSet.toArray(new Date[forecastSet.size()]));
    Collections.sort(forecasts);
    offsets = Arrays.asList(offsetSet.toArray(new Double[offsetSet.size()]));
    Collections.sort(offsets);

    // now each Gridset generates its own inventory
    for (Gridset gridset : gridsets) {
      gridset.generateInventory();
    }

    /* are these really used?
    runMapAll = new HashMap<Date, List<Inventory>>(); // for each runDate, List<Inventory> unique
    timeMapAll = new HashMap<Date, List<Inventory>>(); // for each forecast Date, List<Inventory> unique
    offsetMapAll = new HashMap<Double, List<Inventory>>(); // for each offset hour, List<Inventory> unique
    bestListAll = new ArrayList<Inventory>();  // best Inventory

    // for each runDate, List<Inventory> unique
    for (int run = 0; run < runtimes.size(); run++) {
      Date rundate = runtimes.get(run);
      HashSet<Inventory> all = new HashSet<Inventory>();

      for (Gridset gridset : gridsets) {
        List<Inventory> invList = gridset.runMap.get(rundate);
        if (invList != null) all.addAll(invList);
      }
      List<Inventory> invList = Arrays.asList(all.toArray(new Inventory[all.size()]));
      Collections.sort(invList);
      runMapAll.put(rundate, invList);
    }

    // for each forecast Date, List<Inventory> unique
    for (int time = 0; time < forecasts.size(); time++) {
      Date timedate = forecasts.get(time);
      HashSet<Inventory> all = new HashSet<Inventory>();

      for (Gridset gridset : gridsets) {
        List<Inventory> invList = gridset.timeMap.get(timedate);
        if (invList != null) all.addAll(invList);
      }
      List<Inventory> invList = Arrays.asList(all.toArray(new Inventory[all.size()]));
      Collections.sort(invList, new InvRuntimeComparator());
      timeMapAll.put(timedate, invList);
    }

    // for each offset hour, List<Inventory> unique
    for (int offset = 0; offset < offsets.size(); offset++) {
      Double offsetHour = offsets.get(offset);
      HashSet<Inventory> all = new HashSet<Inventory>();

      for (Gridset gridset : gridsets) {
        List<Inventory> invList = gridset.offsetMap.get(offsetHour);
        if (invList != null) all.addAll(invList);
      }
      List<Inventory> invList = Arrays.asList(all.toArray(new Inventory[all.size()]));
      Collections.sort(invList);
      offsetMapAll.put(offsetHour, invList);
    }

    // this seems fishy
    HashSet<Inventory> all = new HashSet<Inventory>();
    for (int i = 0; i < gridsets.size(); i++) {
      Gridset gridset = gridsets.get(i);
      all.addAll(gridset.bestList);
    }
    bestListAll = Arrays.asList(all.toArray(new Inventory[all.size()]));
    Collections.sort(bestListAll); */
  }

  private double getOffsetHour(Date run, Date forecast) {
    double diff = forecast.getTime() - run.getTime();
    return diff / 1000.0 / 60.0 / 60.0;
  }

  ////////////////////////////////////////////////////
  // all grids in a gridset have same time coordinate
  private class Gridset {
    List<GridDatatype> gridList = new ArrayList<GridDatatype>();  // ucar.nc2.dt.GridDatatype
    ucar.nc2.dt.GridCoordSystem gcs; // keep this so we can call gcs.getTimeAxisForRun( run)
    CoordinateAxis timeAxis;
    String timeDimName;

    HashMap<Date, List<Inventory>> runMap = new HashMap<Date, List<Inventory>>(); // key = run Date, value = List<Invenory>
    HashMap<Date, List<Inventory>> timeMap = new HashMap<Date, List<Inventory>>(); // key = forecast Date, value = List<Invenory>
    HashMap<Double, List<Inventory>> offsetMap = new HashMap<Double, List<Inventory>>(); // key = offset Double, value = List<Invenory>
    List<Inventory> bestList = new ArrayList<Inventory>();  // best List<Inventory>  */

    Gridset(CoordinateAxis timeAxis, ucar.nc2.dt.GridCoordSystem gcs) {
      this.gcs = gcs;
      this.timeAxis = timeAxis;
      timeDimName = timeAxis.getDimension(1).getName();
    }

    String makeDimensions(List dims) {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append(timeDimName);
      for (int i = 0; i < dims.size(); i++) {
        Dimension d = (Dimension) dims.get(i);
        if (d.getName().equals(runtimeDimName) || d.getName().equals(timeDimName)) continue;
        sbuff.append(" " + d.getName());
      }
      return sbuff.toString();
    }

    void generateInventory() {
      HashMap<Date, Inventory> bestMap = new HashMap<Date, Inventory>();

      int nruns = runtimes.size();
      for (int run = 0; run < nruns; run++) {
        Date runDate = runtimes.get(run);
        List<Inventory> runList = new ArrayList<Inventory>();
        runMap.put(runDate, runList);

        // we assume that with the same taxis, we get the same CoordinateAxis1DTime
        CoordinateAxis1DTime timeCoordRun = gcs.getTimeAxisForRun(run);
        Date[] forecastDates = timeCoordRun.getTimeDates();
        for (int time = 0; time < forecastDates.length; time++) {
          Date forecastDate = forecastDates[time];
          double hourOffset = getOffsetHour(runDate, forecastDate);

          Inventory inv = new Inventory(runDate, forecastDate, hourOffset, run, time);
          runList.add(inv);
          bestMap.put(forecastDate, inv); // later ones will be used

          List<Inventory> offsetList = offsetMap.get(hourOffset);
          if (offsetList == null) {
            offsetList = new ArrayList<Inventory>();
            offsetMap.put(hourOffset, offsetList);
          }
          offsetList.add(inv);

          List<Inventory> timeList = timeMap.get(forecastDate);
          if (timeList == null) {
            timeList = new ArrayList<Inventory>();
            timeMap.put(forecastDate, timeList);
          }
          timeList.add(inv);
        }
      }

      bestList = new ArrayList<Inventory>(bestMap.values());
      Collections.sort(bestList);
    }

    void dump() throws IOException {
      DateFormatter df = new DateFormatter();
      System.out.println("Gridset timeDimName= " + timeDimName+"\n grids= ");
      for (GridDatatype grid : gridList) {
        System.out.println("  "+grid.getName());
      }

      System.out.println("\nRun Dates= " + runtimes.size());
      for (Date date : runtimes) {
        System.out.print(" " + df.toDateTimeString(date) + " (");
        List<Inventory> list = runMap.get(date);
        if (list == null)
          System.out.print(" none");
        else {
          for (Inventory inv : list) {
            System.out.print(" " + inv.hourOffset);
          }
        }
        System.out.println(")");
      }

      System.out.println("\nForecast Dates= " + forecasts.size());
      for (Date date : forecasts) {
        System.out.print(" " + df.toDateTimeString(date) + " (");
        List<Inventory> list = timeMap.get(date);
        if (list == null)
          System.out.print(" none");
        else {
          for (Inventory inv : list) {
            System.out.print(" " + inv.run + "/" + inv.hourOffset);
          }
        }
        System.out.println(")");
      }

      System.out.println("\nForecast Hours= " + offsets.size());
      for (Double hour : offsets) {
        List<Inventory> offsetList = offsetMap.get(hour);
        System.out.print(" " + hour + ": (");
        if (offsetList == null)
          System.out.print(" none");
        else {
          for (int j = 0; j < offsetList.size(); j++) {
            Inventory inv = offsetList.get(j);
            if (j > 0) System.out.print(", ");
            System.out.print(inv.run + "/" + df.toDateTimeStringISO(inv.runTime));
          }
        }
        System.out.println(")");
      }

      System.out.println("\nBest Forecast = " + bestList.size());
      for (Inventory inv : bestList) {
        System.out.println(" " + df.toDateTimeStringISO(inv.forecastTime) + " (run=" + inv.run + "/" + df.toDateTimeStringISO(inv.runTime) + ") offset=" + inv.hourOffset);
      }

    }

  }

  /////////////////////////////////////////////////
  private class Inventory implements Comparable {
    Date forecastTime;
    Date runTime;
    double hourOffset;
    int run, time;

    Inventory(Date runTime, Date forecastTime, double hourOffset, int run, int time) {
      this.runTime = runTime;
      this.hourOffset = hourOffset;
      this.forecastTime = forecastTime;
      this.run = run;
      this.time = time;
    }

    public int compareTo(Object o) {
      Inventory other = (Inventory) o;
      return forecastTime.compareTo(other.forecastTime);
    }
  }

  private class InvRuntimeComparator implements Comparator<Inventory> {
    public int compare(Inventory inv1, Inventory inv2) {
      return inv1.runTime.compareTo(inv2.runTime);
    }
  }

  private interface InventoryGetter {
    public List<Inventory> get(Gridset gridset);
  }

  private class RuntimeInvGetter implements InventoryGetter {
    Date wantRuntime;

    RuntimeInvGetter(Date wantRuntime) {
      this.wantRuntime = wantRuntime;
    }

    public List<Inventory> get(Gridset gridset) {
//      if (gridset == null)
  //      return runMapAll.get(wantRuntime);
    //  else
        return gridset.runMap.get(wantRuntime);
    }
  }

  private class ForecastInvGetter implements InventoryGetter {
    Date forecastTime;

    ForecastInvGetter(Date forecastTime) {
      this.forecastTime = forecastTime;
    }

    public List<Inventory> get(Gridset gridset) {
 //     if (gridset == null)
   //     return timeMapAll.get(forecastTime);
     // else
        return gridset.timeMap.get(forecastTime);
    }
  }

  private class OffsetInvGetter implements InventoryGetter {
    Double hours;

    OffsetInvGetter(double hours) {
      this.hours = hours;
    }

    public List<Inventory> get(Gridset gridset) {
//      if (gridset == null)
  //      return offsetMapAll.get(hours);
    //  else
        return gridset.offsetMap.get(hours);
    }
  }

  /////////////////////////////////////////////////////////////
  public List<Date> getRunDates() {
    return runtimes;
  }

  public NetcdfDataset getRunTimeDataset(Date wantRuntime) {
    if (!runtimes.contains(wantRuntime)) return null;
    NetcdfDataset ncd = createDataset(new RuntimeInvGetter(wantRuntime), RUN);

    DateFormatter df = new DateFormatter();
    ncd.addAttribute(null, new Attribute(_Coordinate.ModelRunDate, df.toDateTimeStringISO(wantRuntime)));
    ncd.finish();

    return ncd;
  }

  public List<Date> getForecastDates() {
    return forecasts;
  }

  public NetcdfDataset getForecastTimeDataset(Date forecastTime) {
    if (!forecasts.contains(forecastTime)) return null;
    return createDataset(new ForecastInvGetter(forecastTime), FORECAST);
  }

  public List<Double> getForecastOffsets() {
    return offsets;
  }

  public NetcdfDataset getForecastOffsetDataset(double hours) {
    if (!offsets.contains(new Double(hours))) return null;
    return createDataset(new OffsetInvGetter(hours), OFFSET);
  }

  public NetcdfDataset getBestTimeSeries() {
    return createDataset(new InventoryGetter() {
      public List get(Gridset gridset) {
        //return (gridset == null) ? bestListAll : gridset.bestList;
        return gridset.bestList;
      }
    }, BEST);
  }

  public NetcdfDataset getFmrcDataset() {
    return ncd_2dtime;
  }

  /////////////////////////
  private NetcdfDataset createDataset(InventoryGetter invGetter, String type) {
    NetcdfDataset newds = new NetcdfDataset();
    //addRunTimeCoordinate( newds, invGetter.get( null));

    Group src = ncd_2dtime.getRootGroup();
    Group target = newds.getRootGroup();

    // global attributes
    Iterator iterAtt = src.getAttributes().iterator();
    while (iterAtt.hasNext()) {
      ucar.nc2.Attribute a = (ucar.nc2.Attribute) iterAtt.next();
      target.addAttribute(a);
    }
    String oldHistory = ncd_2dtime.findAttValueIgnoreCase(null, "history", null);
    String newHistory = "Synthetic dataset from TDS fmrc (" + type + ") aggregation, original data from " + ncd_2dtime.getLocation();
    String history = (oldHistory != null) ? oldHistory + "; " + newHistory : newHistory;
    target.addAttribute(new Attribute("history", history));

    // need this attribute for fmrInventory
    DateFormatter df = new DateFormatter();
    target.addAttribute(new Attribute(_Coordinate.ModelBaseDate, df.toDateTimeStringISO(baseDate)));

    // dimensions
    for (Dimension d : src.getDimensions()) {
      target.addDimension(new Dimension(d.getName(), d));
    }

    // take each gridset seperately
    for (Gridset gridset : gridsets) {
      List<Inventory> invList = invGetter.get(gridset);
      if (invList == null) continue;

      addTime3Coordinates(newds, gridset, invList, type);
      Subsetter subs = new Subsetter(invList);

      List grids = gridset.gridList;
      for (int j = 0; j < grids.size(); j++) {
        GridDatatype grid = (GridDatatype) grids.get(j);
        Variable orgVar = ncd_2dtime.findVariable(grid.getName());

        VariableDS v = new VariableDS(target, orgVar, false);
        v.setDimensions(gridset.makeDimensions(v.getDimensions()));
        v.setProxyReader(subs);
        v.remove(v.findAttribute(_Coordinate.Axes));
        v.remove(v.findAttribute("coordinates"));
        //v.addAttribute(new Attribute("coordinates", grid.getGridCoordSystem().getName()));
        target.addVariable(v); // reparent
      }
    }

    // any non-grid variables
    Iterator iterVar = src.getVariables().iterator();
    while (iterVar.hasNext()) {
      VariableDS v = (VariableDS) iterVar.next();
      if ((null == gridHash.get(v.getName()) && !coordSet.contains(v.getName())))
        target.addVariable(new VariableDS(newds.getRootGroup(), v, false)); // reparent LOOK fishy !!!!
    }

    newds.finish();
    // newds.setCached(3); // dont allow a normal close
    return newds;
  }

  private void addTime3Coordinates(NetcdfDataset newds, Gridset gridset, List<Inventory> invList, String type) {
    DateFormatter formatter = new DateFormatter();
    boolean useRun = type.equals(FORECAST);

    // add the time dimensions
    int n = invList.size();
    String dimName = gridset.timeDimName;

    Group g = newds.getRootGroup();
    g.remove(g.findDimension(dimName));
    g.addDimension(new Dimension(dimName, n, true));

    // make the time coordinate variable data
    ArrayDouble.D1 offsetData = new ArrayDouble.D1(n);
    for (int i = 0; i < n; i++) {
      Inventory inv = (Inventory) invList.get(i);
      double offsetHour = getOffsetHour(baseDate, useRun ? inv.runTime : inv.forecastTime);
      offsetData.set(i, offsetHour);
    }

    // add the time coordinate variable
    String typeName = useRun ? "run" : "forecast";
    String desc = typeName + " time coordinate";
    VariableDS timeCoordinate = new VariableDS(newds, g, null, dimName, DataType.DOUBLE, dimName,
            "hours since " + formatter.toDateTimeStringISO(baseDate), desc);
    timeCoordinate.setCachedData(offsetData, true);
    timeCoordinate.addAttribute(new Attribute("long_name", desc));
    timeCoordinate.addAttribute(new Attribute("standard_name", useRun ? "forecast_reference_time" : "time"));
    timeCoordinate.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    newds.addVariable(g, timeCoordinate);

    // add the runtime coordinate
    ArrayObject.D1 runData = new ArrayObject.D1(String.class, n);
    for (int i = 0; i < n; i++) {
      Inventory inv = (Inventory) invList.get(i);
      runData.set(i, formatter.toDateTimeStringISO(inv.runTime));
    }
    desc = "model run dates for coordinate = " + dimName;

    VariableDS runtimeCoordinate = new VariableDS(newds, newds.getRootGroup(), null, dimName + "_run",
            DataType.STRING, dimName, null, desc);
    runtimeCoordinate.setCachedData(runData, true);
    runtimeCoordinate.addAttribute(new Attribute("long_name", desc));

    runtimeCoordinate.addAttribute(new Attribute("standard_name", "forecast_reference_time"));
    runtimeCoordinate.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.RunTime.toString()));
    newds.addVariable(newds.getRootGroup(), runtimeCoordinate);

    // add the offset coordinate
    offsetData = new ArrayDouble.D1(n);
    for (int i = 0; i < n; i++) {
      Inventory inv = invList.get(i);
      offsetData.set(i, inv.hourOffset);
    }
    desc = "hour offset from start of run for coordinate = " + dimName;

    VariableDS offsetCoordinate = new VariableDS(newds, newds.getRootGroup(), null, dimName + "_offset",
            DataType.DOUBLE, dimName, null, desc);

    offsetCoordinate.setCachedData(offsetData, true);
    offsetCoordinate.addAttribute(new Attribute("long_name", desc));
    offsetCoordinate.addAttribute(new Attribute("units", "hour"));
    offsetCoordinate.addAttribute(new Attribute("standard_name", "forecast_period"));
    newds.addVariable(newds.getRootGroup(), offsetCoordinate);
  }

  private void addTimeCoordinates(NetcdfDataset newds, Gridset gridset, List invList, String type) {
    DateFormatter formatter = new DateFormatter();
    boolean useRun = type.equals(FORECAST);

    // add the time dimensions
    int n = invList.size();
    String dimName = gridset.timeDimName;

    Group g = newds.getRootGroup();
    g.remove(g.findDimension(dimName));
    g.addDimension(new Dimension(dimName, n, true));

    // make the time coordinate variable data
    ArrayDouble.D1 offsetData = new ArrayDouble.D1(n);
    for (int i = 0; i < n; i++) {
      Inventory inv = (Inventory) invList.get(i);
      double offsetHour = getOffsetHour(baseDate, useRun ? inv.runTime : inv.forecastTime);
      offsetData.set(i, offsetHour);
    }

    // add the time coordinate variable
    String typeName = useRun ? "run" : "forecast";
    String desc = typeName + " time coordinate";
    VariableDS timeCoordinate = new VariableDS(newds, g, null, dimName, DataType.DOUBLE, dimName,
            "hours since " + formatter.toDateTimeStringISO(baseDate), desc);
    timeCoordinate.setCachedData(offsetData, true);
    timeCoordinate.addAttribute(new Attribute("long_name", desc));
    timeCoordinate.addAttribute(new Attribute("standard_name", useRun ? "forecast_reference_time" : "time"));
    timeCoordinate.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    newds.addVariable(g, timeCoordinate);

    // make the other coordinate variable data
    Array data;
    boolean useOffset = type.equals(RUN) || type.equals(FORECAST);
    typeName = useOffset ? "offset" : "run";
    DataType dataType = useOffset ? DataType.DOUBLE : DataType.STRING;

    if (useOffset) {
      ArrayDouble.D1 runData = new ArrayDouble.D1(n);
      for (int i = 0; i < n; i++) {
        Inventory inv = (Inventory) invList.get(i);
        runData.set(i, inv.hourOffset);
      }
      data = runData;
      desc = "hour offset from start of run for coordinate = " + dimName;

    } else {
      ArrayObject.D1 runData = new ArrayObject.D1(String.class, n);
      for (int i = 0; i < n; i++) {
        Inventory inv = (Inventory) invList.get(i);
        runData.set(i, formatter.toDateTimeStringISO(useRun ? inv.forecastTime : inv.runTime));
      }
      data = runData;
      desc = "model run dates for coordinate = " + dimName;
    }

    VariableDS otherCoordinate = new VariableDS(newds, newds.getRootGroup(), null, typeName + "_" + dimName,
            dataType, dimName, null, desc);
    otherCoordinate.setCachedData(data, true);
    otherCoordinate.addAttribute(new Attribute("long_name", desc));

    if (!useOffset) {
      otherCoordinate.addAttribute(new Attribute("standard_name", "forecast_reference_time"));
      otherCoordinate.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.RunTime.toString()));
    } else {
      otherCoordinate.addAttribute(new Attribute("units", "hour"));
      otherCoordinate.addAttribute(new Attribute("standard_name", "forecast_period"));
    }
    newds.addVariable(newds.getRootGroup(), otherCoordinate);
  }

  /////////////////////////////
  // assumes any Variable coming here has one time dimension, and orgVar has 2

  private class Subsetter implements ucar.nc2.dataset.ProxyReader {
    List<Inventory> invList;

    Subsetter(List<Inventory> invList) {
      this.invList = invList;
    }

    public Array read(Variable mainv, CancelTask cancelTask) throws IOException {
      Variable orgVar = ncd_2dtime.findVariable(mainv.getName());
      int[] orgVarShape = orgVar.getShape();

      // calculate shape - wants it "all"  (we dont seem to have access to the derived variable, so must construct)
      int rank = orgVar.getRank()-1;
      int[] varShape = new int[rank];
      varShape[0] = invList.size();
      for (int i=1; i<rank;i++)
        varShape[i] = orgVarShape[i+1];

      Array allData = Array.factory(mainv.getDataType(), varShape);
      int destPos = 0;
      List<Range> section = orgVar.getRanges();

      // loop over inventory
      for (int i = 0; i < invList.size(); i++) {
        Inventory inv = invList.get(i);

        Array varData;
        try {
          section.set(0, new Range(inv.run, inv.run));
          section.set(1, new Range(inv.time, inv.time));

          varData = orgVar.read(section);

        } catch (InvalidRangeException e) {
          throw new IllegalStateException(e.getMessage());
        }

        Array.arraycopy(varData, 0, allData, destPos, (int) varData.getSize());
        destPos += varData.getSize();

        if ((cancelTask != null) && cancelTask.isCancel())
          return null;
      }

      return allData;
    }

    public Array read(Variable mainv, CancelTask cancelTask, List section) throws IOException, InvalidRangeException {
      // If its full sized, then use full read, so that data gets cached. LOOK probably doesnt work, since mainv == orgVar
      long size = Range.computeSize(section);
      if (size == mainv.getSize())
        return read(mainv, cancelTask);

      Variable orgVar = ncd_2dtime.findVariable(mainv.getName());

      Array sectionData = Array.factory(mainv.getDataType(), Range.getShape(section));
      int destPos = 0;

      Range timeRange = (Range) section.get(0);
      List<Range> allSection = new ArrayList<Range>(section); // copy
      allSection.add(0, null); // need 1 more.

      Range.Iterator iter = timeRange.getIterator();
      while (iter.hasNext()) {
        int index = iter.next();
        Inventory inv = invList.get(index);

        Array varData;
        try {
          allSection.set(0, new Range(inv.run, inv.run));
          allSection.set(1, new Range(inv.time, inv.time));

          varData = orgVar.read(allSection);

        } catch (InvalidRangeException e) {
          throw new IllegalStateException(e.getMessage());
        }

        Array.arraycopy(varData, 0, sectionData, destPos, (int) varData.getSize());
        destPos += varData.getSize();

        if ((cancelTask != null) && cancelTask.isCancel())
          return null;
      }

      return sectionData;
    }
  } // subsetter

  /////////////////////////////

  static void test(String location, String timeVarName) throws IOException {
    FmrcImpl fmrc = new FmrcImpl(location);

    System.out.println("Fmrc for dataset= " + location);

    NetcdfDataset fmrcd = fmrc.getFmrcDataset();
    Variable time = fmrcd.findVariable(timeVarName);
    Array data = time.read();
    NCdump.printArray(data, "2D time", System.out, null);

    for (Gridset gridset : fmrc.gridsets) {
      System.out.println("===========================");
      gridset.dump();
    }
  }

  static void testSync(String location, String timeVarName) throws IOException, InterruptedException {
    FmrcImpl fmrc = new FmrcImpl(location);
    System.out.println("Fmrc for dataset= " + location);

    NetcdfDataset fmrcd = fmrc.getFmrcDataset();
    Variable time = fmrcd.findVariable(timeVarName);
    Array data = time.read();
    NCdump.printArray(data, "2D time", System.out, null);

    for (Gridset gridset : fmrc.gridsets) {
      System.out.println("===========================");
      gridset.dump();
    }

    boolean changed = fmrc.sync();

    if (changed) {
      System.out.println("========== Sync =================");
      data = time.read();
      NCdump.printArray(data, "2D time", System.out, null);
      for (Gridset gridset : fmrc.gridsets) {
        System.out.println("===========================");
        gridset.dump();
      }
    }
  }

  public static void main(String args[]) throws Exception {
    //test("R:/testdata/fmrc/NAMfmrc.nc", "valtime");

    //test("C:/dev/thredds/cdm/src/test/data/ncml/AggFmrcGrib.xml", "time");
    //test("C:/dev/thredds/cdm/src/test/data/ncml/aggFmrcGribRunseq.xml", "time");
    //test("C:/dev/thredds/cdm/src/test/data/ncml/aggFmrcNomads.xml", "time");
    testSync("C:/data/ral/fmrc.ncml", "time");
    //test("C:/data/grib/gfs/puerto/fmrc.ncml", "time");
  }

}
