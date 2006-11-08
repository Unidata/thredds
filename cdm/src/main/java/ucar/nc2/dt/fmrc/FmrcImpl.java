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

  private NetcdfDataset org_ncd;
  private ucar.nc2.dt.GridDataset gds;
  private Date baseDate;
  private String runtimeDimName;

  private ArrayList gridsets;
  private HashMap gridHash; // key = grid name, value = Gridset
  private HashSet coordSet;  // time coord names

  private HashMap runMapAll;    // key = run Date, value = List<Invenory>
  private HashMap timeMapAll;   // key = forecasst Date, value = List<Invenory>
  private HashMap offsetMapAll; // key = offset Double, value = List<Invenory>
  private List bestListAll;     // best List<Invenory>  */

  private List runtimes;  // List of all possible runtime Date
  private List forecasts;  // List of all possible forecast Date
  private List offsets;  // List of all possible offset Double

  public FmrcImpl(String filename) throws IOException {
    this( ucar.nc2.dataset.NetcdfDatasetCache.acquire( filename, null));
  }

  public FmrcImpl(NetcdfDataset ncd) throws IOException {
    init( ncd);
  }

  /** Check if file has changed, and reread metadata if needed.
   *  All previous object references (variables, dimensions, etc) may become invalid - you must re-obtain.
   * @return true if file was changed.
   * @throws IOException
   */
  public boolean sync() throws IOException {
    boolean changed = org_ncd.syncExtend();
    if (changed)
      init( org_ncd);
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
    this.org_ncd = ncd;
    if (!ncd.isEnhanced())
      ncd.enhance();
    // ncd.setCached(3); // dont allow a normal close LOOK why ?? who is managing ??

    gridHash = new HashMap();  // key = grid name, value = Gridset
    coordSet = new HashSet();  // time coord names
    runtimes = null;

    HashMap timeAxisHash = new HashMap(); // key = timeAxis, value = Gridset
    gds = new ucar.nc2.dt.grid.GridDataset( ncd);
    List grids = gds.getGrids();
    if (grids.size() == 0)
      throw new IllegalArgumentException("no grids");

    // collect the grids into Gridsets, based on what time axis they use
    for (int i = 0; i < grids.size(); i++) {
      ucar.nc2.dt.GridDatatype grid = (ucar.nc2.dt.GridDatatype) grids.get(i);
      ucar.nc2.dt.GridCoordSystem gcs = grid.getCoordinateSystem();
      CoordinateAxis timeAxis = gcs.getTimeAxis();
      if (timeAxis != null) {
        Gridset gset = (Gridset) timeAxisHash.get( timeAxis); // group by timeAxis
        if (gset == null) {
          gset = new Gridset(timeAxis, gcs);
          timeAxisHash.put( timeAxis, gset);
          coordSet.add( timeAxis.getName());
        }
        gset.gridList.add( grid);
        gridHash.put( grid.getName(), gset);
      }

      // assume runtimes are always the same
      if ((runtimes == null) && (gcs.getRunTimeAxis() != null)) {
        CoordinateAxis1DTime runtimeCoord = gcs.getRunTimeAxis();
        Date[] runDates = runtimeCoord.getTimeDates();
        baseDate = runDates[0];
        runtimes = Arrays.asList(runDates);
        runtimeDimName = runtimeCoord.getDimension(0).getName();
        coordSet.add( runtimeCoord.getName());
      }
    }

    if (runtimes == null)
      throw new IllegalArgumentException("no runtime dimension");

    // generate the lists of possible forecasts, offsets
    HashSet forecastSet = new HashSet();
    HashSet offsetSet = new HashSet();
    gridsets = new ArrayList( timeAxisHash.values());
    for (int i = 0; i < gridsets.size(); i++) {
      Gridset gridset = (Gridset) gridsets.get(i);

      for (int run = 0; run < runtimes.size(); run++) {
        Date runDate = (Date) runtimes.get(run);

        // we assume that with the same taxis, we get the same results here
        CoordinateAxis1DTime timeCoordRun = gridset.gcs.getTimeAxisForRun( run);
        Date[] forecastDates = timeCoordRun.getTimeDates();

        for (int time = 0; time < forecastDates.length; time++) {
          Date forecastDate = forecastDates[time];
          forecastSet.add(forecastDate);

          double hourOffset = getOffsetHour(runDate, forecastDate);
          offsetSet.add( new Double(hourOffset));
        }
      }
    }
    forecasts = Arrays.asList( forecastSet.toArray());
    Collections.sort(forecasts);
    offsets = Arrays.asList( offsetSet.toArray());
    Collections.sort(offsets);

    // now work with each Gridset in turn
    for (int i = 0; i < gridsets.size(); i++) {
      Gridset gridset = (Gridset) gridsets.get(i);
      gridset.generateInventory();
    }

    // are these really used?
    runMapAll = new HashMap(); // key = run Date, value = List<Invenory>
    timeMapAll = new HashMap(); // key = forecasst Date, value = List<Invenory>
    offsetMapAll = new HashMap(); // key = offset Double, value = List<Invenory>
    bestListAll = new ArrayList();  // best List<Invenory>  */

    // now we want the union of all gridsets
    for (int run = 0; run < runtimes.size(); run++) {
      Date rundate = (Date) runtimes.get(run);
      HashSet all = new HashSet();

      for (int i = 0; i < gridsets.size(); i++) {
        Gridset gridset = (Gridset) gridsets.get(i);
        List invList = (List) gridset.runMap.get(rundate);
        if (invList != null) all.addAll( invList);
      }
      List invList = Arrays.asList( all.toArray());
      Collections.sort(invList);
      runMapAll.put( rundate, invList);
    }

    for (int time = 0; time < forecasts.size(); time++) {
      Date timedate = (Date) forecasts.get(time);
      HashSet all = new HashSet();

      for (int i = 0; i < gridsets.size(); i++) {
        Gridset gridset = (Gridset) gridsets.get(i);
        List invList = (List) gridset.timeMap.get(timedate);
        if (invList != null) all.addAll( invList);
      }
      List invList = Arrays.asList( all.toArray());
      Collections.sort(invList, new InvRuntimeComparator());
      timeMapAll.put( timedate, invList);
    }

    for (int offset = 0; offset < offsets.size(); offset++) {
      Double offsetHour = (Double) offsets.get(offset);
      HashSet all = new HashSet();

      for (int i = 0; i < gridsets.size(); i++) {
        Gridset gridset = (Gridset) gridsets.get(i);
        List invList = (List) gridset.offsetMap.get(offsetHour);
        if (invList != null) all.addAll( invList);
      }
      List invList = Arrays.asList( all.toArray());
      Collections.sort(invList);
      offsetMapAll.put( offsetHour, invList);
    }

    HashSet all = new HashSet();
    for (int i = 0; i < gridsets.size(); i++) {
      Gridset gridset = (Gridset) gridsets.get(i);
      all.addAll( gridset.bestList);
    }
    bestListAll = Arrays.asList( all.toArray());
    Collections.sort(bestListAll);
  }

  private double getOffsetHour( Date run, Date forecast) {
    double diff = forecast.getTime() - run.getTime();
    return diff / 1000.0 / 60.0 / 60.0;
  }

  ////////////////////////////////////////////////////
  // all grids in a gridset have same time coordinate
  private class Gridset {
    ArrayList gridList = new ArrayList();  // ucar.nc2.dt.GridDatatype
    ucar.nc2.dt.GridCoordSystem gcs;
    CoordinateAxis timeAxis;
    String timeDimName;

    HashMap runMap = new HashMap(); // key = run Date, value = List<Invenory>
    HashMap timeMap = new HashMap(); // key = forecast Date, value = List<Invenory>
    HashMap offsetMap = new HashMap(); // key = offset Double, value = List<Invenory>
    ArrayList bestList = new ArrayList();  // best List<Invenory>  */

    Gridset(CoordinateAxis timeAxis, ucar.nc2.dt.GridCoordSystem gcs ) {
      this.gcs = gcs;
      this.timeAxis = timeAxis;
      timeDimName = timeAxis.getDimension(1).getName();
    }

    String makeDimensions( List dims) {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append( timeDimName);
      for (int i = 0; i < dims.size(); i++) {
        Dimension d = (Dimension) dims.get(i);
        if (d.getName().equals(runtimeDimName) || d.getName().equals(timeDimName)) continue;
        sbuff.append(" "+d.getName());
      }
      return sbuff.toString();
    }

    void generateInventory() {
      HashMap bestMap = new HashMap();

      int nruns = runtimes.size();
      for (int run = 0; run < nruns; run++) {
        Date runDate = (Date) runtimes.get(run);

        // we assume that with the same taxis, we get the same CoordinateAxis1DTime
        CoordinateAxis1DTime timeCoordRun = gcs.getTimeAxisForRun( run);
        Date[] forecastDates = timeCoordRun.getTimeDates();

        ArrayList runList = new ArrayList();
        runMap.put( runDate, runList);

        for (int time = 0; time < forecastDates.length; time++) {
          Date forecastDate = forecastDates[time];
          double hourOffset = getOffsetHour(runDate, forecastDate);

          Inventory inv = new Inventory(runDate, forecastDate, hourOffset, run, time);
          runList.add(inv);
          bestMap.put( forecastDate, inv); // later ones will be used

          Double dd = new Double(hourOffset);
          ArrayList offsetList = (ArrayList) offsetMap.get(dd);
          if (offsetList == null) {
            offsetList = new ArrayList();
            offsetMap.put(dd, offsetList);
          }
          offsetList.add(inv);

          ArrayList timeList = (ArrayList) timeMap.get(forecastDate);
          if (timeList == null) {
            timeList = new ArrayList();
            timeMap.put(forecastDate, timeList);
          }
          timeList.add(inv);
        }
      }

      bestList = new ArrayList(bestMap.values());
      Collections.sort( bestList);
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
      return forecastTime.compareTo( other.forecastTime);
    }
  }

  private class InvRuntimeComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      Inventory inv1 = (Inventory) o1;
      Inventory inv2 = (Inventory) o2;
      return inv1.runTime.compareTo(inv2.runTime);
    }
  }

  private interface InventoryGetter {
    public List get(Gridset gridset);
  }

  private class RuntimeInvGetter implements InventoryGetter {
    Date wantRuntime;
    RuntimeInvGetter(Date wantRuntime) { this.wantRuntime= wantRuntime; }

    public List get(Gridset gridset) {
      if (gridset == null)
        return (List) runMapAll.get(wantRuntime);
      else
        return (List) gridset.runMap.get( wantRuntime);
    }
  }

  private class ForecastInvGetter implements InventoryGetter {
    Date forecastTime;
    ForecastInvGetter(Date forecastTime) { this.forecastTime= forecastTime; }

    public List get(Gridset gridset) {
      if (gridset == null)
        return (List) timeMapAll.get( forecastTime);
      else
        return (List) gridset.timeMap.get( forecastTime);
    }
  }

  private class OffsetInvGetter implements InventoryGetter {
    Double hours;
    OffsetInvGetter(double hours) { this.hours = new Double(hours); }

    public List get(Gridset gridset) {
      if (gridset == null)
        return (List) offsetMapAll.get( hours);
      else
        return (List) gridset.offsetMap.get( hours);
    }
  }

  /////////////////////////////////////////////////////////////
  public List getRunDates() {
    return runtimes;
  }

  public NetcdfDataset getRunTimeDataset(Date wantRuntime) {
    if (!runtimes.contains(wantRuntime)) return null;
    NetcdfDataset ncd = createDataset( new RuntimeInvGetter(wantRuntime),RUN);

    DateFormatter df = new DateFormatter();
    ncd.addAttribute(null, new Attribute(_Coordinate.ModelRunDate, df.toDateTimeStringISO(wantRuntime)));
    ncd.finish();

    return ncd;
  }

  public List getForecastDates() {
    return forecasts;
  }

  public NetcdfDataset getForecastTimeDataset(Date forecastTime) {
    if (!forecasts.contains(forecastTime)) return null;
    return createDataset(new ForecastInvGetter(forecastTime), FORECAST);
  }

  public List getForecastOffsets() {
    return offsets;
  }

  public NetcdfDataset getForecastOffsetDataset(double hours) {
    if (!offsets.contains(new Double(hours))) return null;
    return createDataset(new OffsetInvGetter(hours), OFFSET);
  }

  public NetcdfDataset getBestTimeSeries() {
    return createDataset(new InventoryGetter() {
      public List get(Gridset gridset) {
        return (gridset == null) ? bestListAll : gridset.bestList;
      }
    }, BEST);
  }

  public NetcdfDataset getFmrcDataset() {
    return org_ncd;
  }

  /////////////////////////
  private NetcdfDataset createDataset(InventoryGetter invGetter, String type) {
    NetcdfDataset newds = new NetcdfDataset();
    //addRunTimeCoordinate( newds, invGetter.get( null));

    Group src = org_ncd.getRootGroup();
    Group target = newds.getRootGroup();

    // global attributes
    Iterator iterAtt = src.getAttributes().iterator();
    while (iterAtt.hasNext()) {
      ucar.nc2.Attribute a = (ucar.nc2.Attribute) iterAtt.next();
       target.addAttribute(a);
    }
    String oldHistory = org_ncd.findAttValueIgnoreCase(null, "history", null);
    String newHistory = "Synthetic dataset from TDS fmrc ("+type+") aggregation, original data from "+org_ncd.getLocation();
    String history = (oldHistory != null) ? oldHistory + "; "+newHistory : newHistory;
    target.addAttribute( new Attribute("history", history));

    // need this attribute for fmrInventory
    DateFormatter df = new DateFormatter();
    target.addAttribute( new Attribute(_Coordinate.ModelBaseDate, df.toDateTimeStringISO(baseDate)));

        // dimensions
    Iterator iterDim = src.getDimensions().iterator();
    while (iterDim.hasNext()) {
      Dimension d = (Dimension) iterDim.next();
      target.addDimension( d);
    }

    // take each gridset seperately
    for (int i = 0; i < gridsets.size(); i++) {
      Gridset gridset = (Gridset) gridsets.get(i);
      List invList = invGetter.get( gridset);
      if (invList == null) continue;

      addTime3Coordinates( newds, gridset, invList, type);
      Subsetter subs = new Subsetter(invList);

      List grids = gridset.gridList;
      for (int j = 0; j < grids.size(); j++) {
        ucar.nc2.dt.GridDatatype grid =  (ucar.nc2.dt.GridDatatype) grids.get(j);
        Variable orgVar = org_ncd.findVariable(grid.getName());

        VariableDS v = new VariableDS( target, orgVar, false);
        v.setDimensions( gridset.makeDimensions( v.getDimensions()));
        v.setProxyReader(subs);
        v.remove( v.findAttribute(_Coordinate.Axes));
        v.remove( v.findAttribute("coordinates"));
        //v.addAttribute(new Attribute("coordinates", grid.getGridCoordSystem().getName()));
        target.addVariable(v); // reparent
      }
    }

    // any non-grid variables
    Iterator iterVar = src.getVariables().iterator();
    while (iterVar.hasNext()) {
      VariableDS v = (VariableDS) iterVar.next();
      if ((null == gridHash.get(v.getName()) && !coordSet.contains(v.getName())))
        target.addVariable(v); // reparent
    }

    newds.finish();
    // newds.setCached(3); // dont allow a normal close
    return newds;
  }

  private void addTime3Coordinates(NetcdfDataset newds, Gridset gridset, List invList, String type) {
    DateFormatter formatter = new DateFormatter();
    boolean useRun = type.equals(FORECAST);

    // add the time dimensions
    int n = invList.size();
    String dimName = gridset.timeDimName;

    Group g = newds.getRootGroup();
    g.remove( g.findDimension( dimName));
    g.addDimension( new Dimension( dimName, n, true));

    // make the time coordinate variable data
    ArrayDouble.D1 offsetData = new ArrayDouble.D1( n);
    for (int i = 0; i < n; i++) {
      Inventory inv = (Inventory) invList.get(i);
      double offsetHour = getOffsetHour( baseDate, useRun ? inv.runTime : inv.forecastTime);
      offsetData.set( i, offsetHour);
    }

    // add the time coordinate variable
    String typeName = useRun ? "run" : "forecast";
    String desc = typeName + " time coordinate";
    VariableDS timeCoordinate = new VariableDS(newds, g, null, dimName, DataType.DOUBLE, dimName,
            "hours since "+formatter.toDateTimeStringISO(baseDate), desc);
    timeCoordinate.setCachedData(offsetData, true);
    timeCoordinate.addAttribute( new Attribute("long_name", desc));
    timeCoordinate.addAttribute( new Attribute("standard_name", useRun ? "forecast_reference_time" : "time"));
    timeCoordinate.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    newds.addVariable(g, timeCoordinate);

    // add the runtime coordinate
    ArrayObject.D1 runData = new ArrayObject.D1( String.class, n);
    for (int i = 0; i < n; i++) {
      Inventory inv = (Inventory) invList.get(i);
      runData.set(i, formatter.toDateTimeStringISO(inv.runTime));
    }
    desc = "model run dates for coordinate = "+dimName;

    VariableDS runtimeCoordinate = new VariableDS(newds, newds.getRootGroup(), null, dimName+"_run",
      DataType.STRING, dimName, null, desc);
    runtimeCoordinate.setCachedData(runData, true);
    runtimeCoordinate.addAttribute( new Attribute("long_name", desc));

    runtimeCoordinate.addAttribute( new Attribute("standard_name", "forecast_reference_time"));
    runtimeCoordinate.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.RunTime.toString()));
    newds.addVariable(newds.getRootGroup(), runtimeCoordinate);

    // add the offset coordinate
    offsetData = new ArrayDouble.D1( n);
    for (int i = 0; i < n; i++) {
      Inventory inv = (Inventory) invList.get(i);
      offsetData.set(i, inv.hourOffset);
    }
    desc = "hour offset from start of run for coordinate = "+dimName;

    VariableDS offsetCoordinate = new VariableDS(newds, newds.getRootGroup(), null, dimName+"_offset",
      DataType.DOUBLE, dimName, null, desc);

    offsetCoordinate.setCachedData(offsetData, true);
    offsetCoordinate.addAttribute( new Attribute("long_name", desc));
    offsetCoordinate.addAttribute( new Attribute("units", "hour"));
    offsetCoordinate.addAttribute( new Attribute("standard_name", "forecast_period"));
    newds.addVariable(newds.getRootGroup(), offsetCoordinate);
  }

  private void addTimeCoordinates(NetcdfDataset newds, Gridset gridset, List invList, String type) {
    DateFormatter formatter = new DateFormatter();
    boolean useRun = type.equals(FORECAST);

    // add the time dimensions
    int n = invList.size();
    String dimName = gridset.timeDimName;

    Group g = newds.getRootGroup();
    g.remove( g.findDimension( dimName));
    g.addDimension( new Dimension( dimName, n, true));

    // make the time coordinate variable data
    ArrayDouble.D1 offsetData = new ArrayDouble.D1( n);
    for (int i = 0; i < n; i++) {
      Inventory inv = (Inventory) invList.get(i);
      double offsetHour = getOffsetHour( baseDate, useRun ? inv.runTime : inv.forecastTime);
      offsetData.set( i, offsetHour);
    }

    // add the time coordinate variable
    String typeName = useRun ? "run" : "forecast";
    String desc = typeName + " time coordinate";
    VariableDS timeCoordinate = new VariableDS(newds, g, null, dimName, DataType.DOUBLE, dimName,
            "hours since "+formatter.toDateTimeStringISO(baseDate), desc);
    timeCoordinate.setCachedData(offsetData, true);
    timeCoordinate.addAttribute( new Attribute("long_name", desc));
    timeCoordinate.addAttribute( new Attribute("standard_name", useRun ? "forecast_reference_time" : "time"));
    timeCoordinate.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    newds.addVariable(g, timeCoordinate);

    // make the other coordinate variable data
    Array data;
    boolean useOffset = type.equals(RUN) || type.equals(FORECAST);
    typeName = useOffset ? "offset" : "run";
    DataType dataType = useOffset ? DataType.DOUBLE : DataType.STRING;

    if (useOffset) {
      ArrayDouble.D1 runData = new ArrayDouble.D1( n);
      for (int i = 0; i < n; i++) {
        Inventory inv = (Inventory) invList.get(i);
        runData.set(i, inv.hourOffset);
      }
      data = runData;
      desc = "hour offset from start of run for coordinate = "+dimName;

    } else {
      ArrayObject.D1 runData = new ArrayObject.D1( String.class, n);
      for (int i = 0; i < n; i++) {
        Inventory inv = (Inventory) invList.get(i);
        runData.set(i, formatter.toDateTimeStringISO(useRun ? inv.forecastTime : inv.runTime));
      }
      data = runData;
      desc = "model run dates for coordinate = "+dimName;
    }

    VariableDS otherCoordinate = new VariableDS(newds, newds.getRootGroup(), null, typeName+"_"+dimName,
      dataType, dimName, null, desc);
    otherCoordinate.setCachedData(data, true);
    otherCoordinate.addAttribute( new Attribute("long_name", desc));

    if (!useOffset) {
      otherCoordinate.addAttribute( new Attribute("standard_name", "forecast_reference_time"));
      otherCoordinate.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.RunTime.toString()));
    } else {
      otherCoordinate.addAttribute( new Attribute("units", "hour"));
      otherCoordinate.addAttribute( new Attribute("standard_name", "forecast_period"));
    }
    newds.addVariable(newds.getRootGroup(), otherCoordinate);
  }

  /////////////////////////////
  private class Subsetter implements ucar.nc2.dataset.ProxyReader {
    List invList;
    Subsetter( List invList) {
      this.invList = invList;
    }

    public Array read(Variable mainv, CancelTask cancelTask) throws IOException {
      Variable orgVar = org_ncd.findVariable(mainv.getName());
      List section = orgVar.getRanges();
      boolean hasTime = true; // orgVar.findDimensionIndex(org_timeDimName) >= 0;

      Array allData = Array.factory( mainv.getDataType(), mainv.getShape()); // LOOK why getOriginalDataType() ?
      int destPos = 0;

      // wants it "all"
      for (int i = 0; i < invList.size(); i++) {
        Inventory inv = (Inventory) invList.get(i);

        Array varData;
        try {
          section.set(0, new Range(inv.run, inv.run));
          if (hasTime)
            section.set(1, new Range(inv.time, inv.time));

          varData = orgVar.read( section);

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
      // If its full sized, then use full read, so that data gets cached.
      long size = Range.computeSize(section);
      if (size == mainv.getSize())
        return read(mainv, cancelTask);

      Variable orgVar = org_ncd.findVariable(mainv.getName());
      boolean hasTime = true; // orgVar.findDimensionIndex(org_timeDimName) >= 0;

      Array sectionData = Array.factory(mainv.getDataType(), Range.getShape(section));
      int destPos = 0;

      Range timeRange = (Range) section.get(0);
      ArrayList allSection = new ArrayList( section);
      allSection.add(0, null); // need 1 more.

      Range.Iterator iter = timeRange.getIterator();
      while (iter.hasNext()) {
        int index = iter.next();
        Inventory inv = (Inventory) invList.get(index);

        Array varData;
        try {
          allSection.set(0, new Range(inv.run, inv.run));
          if (hasTime)
            allSection.set(1, new Range(inv.time, inv.time));

          varData = orgVar.read( allSection);

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
     DateFormatter df = new DateFormatter();

     System.out.println("Fmrc for dataset= "+location);
     List dates = fmrc.getRunDates();
     System.out.println("\nRun Dates= "+dates.size());
     for (int i = 0; i < dates.size(); i++) {
       Date date = (Date) dates.get(i);
       System.out.print(" "+df.toDateTimeString(date)+" (");
       List list = (List) fmrc.runMapAll.get(date);
       for (int j = 0; j < list.size(); j++) {
         Inventory inv = (Inventory) list.get(j);
         System.out.print(" "+inv.hourOffset);
       }
       System.out.println(")");
     }

     dates = fmrc.getForecastDates();
     System.out.println("\nForecast Dates= "+dates.size());
     for (int i = 0; i < dates.size(); i++) {
       Date date = (Date) dates.get(i);
       System.out.print(" "+df.toDateTimeString(date)+" (");
       List list = (List) fmrc.timeMapAll.get(date);
       for (int j = 0; j < list.size(); j++) {
         Inventory inv = (Inventory) list.get(j);
         System.out.print(" "+inv.hourOffset);
       }
       System.out.println(")");
     }

     List hours = fmrc.getForecastOffsets();
     System.out.println("\nForecast Hours= "+hours.size());
     for (int i = 0; i < hours.size(); i++) {
       Double hour = (Double) hours.get(i);
       List offsetList = (List) fmrc.offsetMapAll.get(hour);
       System.out.print(" "+hour+": (");
       for (int j = 0; j < offsetList.size(); j++) {
         Inventory inv = (Inventory) offsetList.get(j);
         if (j>0) System.out.print(", ");
         System.out.print(df.toDateTimeStringISO(inv.runTime));
       }
       System.out.println(")");
     }

     List best = fmrc.bestListAll;
     System.out.println("\nBest Forecast = "+best.size());
     for (int i = 0; i < best.size(); i++) {
       Inventory inv = (Inventory) best.get(i);
       System.out.println(" "+df.toDateTimeStringISO(inv.forecastTime)+" (run="+df.toDateTimeStringISO(inv.runTime)+") offset="+inv.hourOffset);
     }

     NetcdfDataset fmrcd = fmrc.getFmrcDataset();
     Variable time = fmrcd.findVariable(timeVarName);
     Array data = time.read();
     NCdump.printArray( data, "2D time", System.out, null);

   }

  public static void main(String args[]) throws IOException {
    //test("R:/testdata/fmrc/NAMfmrc.nc", "valtime");

    //test("C:/dev/thredds/cdm/src/test/data/ncml/AggFmrcGrib.xml", "time");
    test("C:/dev/thredds/cdm/src/test/data/ncml/aggFmrcGribRunseq.xml", "time");
  }

}
