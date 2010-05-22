/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.fmrc;

import thredds.inventory.FeatureCollectionConfig;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.Misc;

import java.io.FileNotFoundException;
import java.util.*;

/**
 * A lightweight, serializable version of FmrcInv
 *
 * @author caron
 * @since Apr 14, 2010
 */
public class FmrcInvLite implements java.io.Serializable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FmrcInvLite.class);

  // public for debugging
  public String collectionName;
  public Date base;
  public int nruns; // runOffset[nruns]
  public double[] runOffset; // run time in offset hours since base
  public double[] forecastOffset; // all forecast times in offset hours since base, for "constant forecast" datasets
  public double[] offsets; // all the offset values, for "constant offset" datasets

  public List<String> locationList = new ArrayList<String>();
  public List<Gridset> gridSets = new ArrayList<Gridset>();
  public List<GridInventory> invList = new ArrayList<GridInventory>(); // share these, they are expensive!

  private DateFormatter df = new DateFormatter();


  public FmrcInvLite(FmrcInv fmrcInv) {
    this.collectionName = fmrcInv.getName();
    this.base = fmrcInv.getBaseDate();

    // store forecasts as offsets instead of Dates
    List<Date> forecasts = fmrcInv.getForecastTimes();
    this.forecastOffset = new double[forecasts.size()];
    for (int i = 0; i < forecasts.size(); i++) {
      Date f = forecasts.get(i);
      this.forecastOffset[i] = FmrcInv.getOffsetInHours(base, f);
    }

    // for each run
    List<FmrInv> fmrList = fmrcInv.getFmrInv();
    nruns = fmrList.size();
    runOffset = new double[nruns];

    for (int run = 0; run < nruns; run++) {
      FmrInv fmr = fmrList.get(run);
      runOffset[run] = FmrcInv.getOffsetInHours(base, fmr.getRunDate());

      for (GridDatasetInv inv : fmr.getInventoryList()) {
        locationList.add(inv.getLocation());
      }
    }

    // for each RunSeq
    for (FmrcInv.RunSeq runseq : fmrcInv.getRunSeqs()) {
      gridSets.add(new Gridset(runseq));
    }

  }

  public List<Date> getRunDates() {
    List<Date> result = new ArrayList<Date>(runOffset.length);
    for (double off : runOffset)
      result.add(FmrcInv.makeOffsetDate(base, off));
    return result;
  }

  public List<Date> getForecastDates() {
    List<Date> result = new ArrayList<Date>(forecastOffset.length);
    for (double f : forecastOffset)
      result.add(FmrcInv.makeOffsetDate(base, f));
    return result;
  }

  public double[] getForecastOffsets() {
    if (offsets == null) {
      TreeSet<Double> tree = new TreeSet<Double>();
      for (Gridset gridset : gridSets) {
        for (int run = 0; run < nruns; run++) {
          double baseOffset = gridset.timeOffset[run * gridset.noffsets];
          for (int time = 0; time < gridset.noffsets; time++) {
            double offset = gridset.timeOffset[run * gridset.noffsets + time];
            if (!Double.isNaN(offset))
              tree.add(offset - baseOffset);
          }
        }
      }
      offsets = new double[tree.size()];
      Iterator<Double> iter = tree.iterator();
      for (int i = 0; i < tree.size(); i++) {
        offsets[i] = iter.next();
      }
    }
    return offsets;
  }

  public Gridset.Grid findGrid(String gridName) {
    for (Gridset gridset : gridSets) {
      for (Gridset.Grid grid : gridset.grids) {
        if (gridName.equals(grid.name)) return grid;
      }
    }
    return null;
  }

  public void showGridInfo(String gridName, Formatter out) {
    Gridset.Grid grid = findGrid(gridName);
    if (grid == null ) {
      out.format("Cant find grid = %s%n", gridName);
      return;
    }
    BestDatasetInventory best = new BestDatasetInventory( null);

    Gridset gridset = grid.getGridset();
    out.format("%n=======================================%nFmrcLite%n");

    // show the 2D    
    out.format("run   %n");
    for (int i=0; i< gridset.noffsets; i++)
      out.format(" %6d", i);
    out.format("%n");
    for (int run = 0; run < nruns; run++) {
      out.format("%6d", run);
      for (int time = 0; time < gridset.noffsets; time++) {
        out.format(" %6.0f", gridset.getTimeCoord(run, time));
      }
      out.format("%n");
    }
    out.format("%n");


    List<TimeInv> bestInv = gridset.timeCoordMap.get("Best");
    if (bestInv == null) bestInv = gridset.makeBest(null);
    double[] coords = best.getTimeCoords( gridset);

    // show the best
    out.format("            ");
    for (int i=0; i< coords.length; i++)
      out.format(" %6d", i);
    out.format("%n");

    out.format("best coords=");
    for (TimeInv inv : bestInv)
      out.format(" %6.0f", inv.offset);
    out.format("%n");

    out.format("best run   =");
    for (TimeInv inv : bestInv)
      out.format(" %6d", inv.runIdx);
    out.format("%n");
  }

  // group of Grids with the same time coordinate
  public class Gridset implements java.io.Serializable {
    String gridsetName;
    List<Grid> grids = new ArrayList<Grid>();
    int noffsets;
    double[] timeOffset;  // timeOffset(nruns,noffsets) in offset hours since base. this is the twoD time coordinate for this Gridset

    Map<String, List<TimeInv>> timeCoordMap = new HashMap<String, List<TimeInv>>();

    Gridset(FmrcInv.RunSeq runseq) {
      this.gridsetName = runseq.getName();
      List<TimeCoord> timeList = runseq.getTimes();
      boolean hasMissingTimes = (nruns != timeList.size()); // missing one or more variables in one or more runs
      double[] unionOffsetHours = runseq.getUnionOffsetHours();
      noffsets = unionOffsetHours.length;

      // this is the twoD time coordinate for this Gridset
      timeOffset = new double[nruns * noffsets];
      for (int i = 0; i < timeOffset.length; i++) timeOffset[i] = Double.NaN;

      // fill twoD time coordinate from the sequence of time coordinates
      int runIdx = 0;
      for (int seqIdx = 0; seqIdx < timeList.size(); seqIdx++) {
        TimeCoord tc = null;
        if (hasMissingTimes) {
          tc = timeList.get(seqIdx);
          double tc_offset = FmrcInv.getOffsetInHours(base, tc.getRunDate());

          while (true) { // incr run till we find it
            double run_offset = runOffset[runIdx];
            if (Misc.closeEnough(run_offset, tc_offset))
              break;
            runIdx++;
            if (log.isDebugEnabled()) {
              String missingDate = df.toDateTimeStringISO(FmrcInv.makeOffsetDate(base, run_offset));
              String wantDate = df.toDateTimeStringISO(tc.getRunDate());
              log.debug(collectionName +": runseq missing time "+missingDate+" looking for "+ wantDate+" for var = "+ runseq.getUberGrids().get(0).getName());
            }
          }

        } else {  // common case
          tc = timeList.get(runIdx);
        }

        double run_offset = FmrcInv.getOffsetInHours(base, tc.getRunDate());
        double[] offsets = tc.getOffsetHours();
        int ntimes = offsets.length;
        for (int time = 0; time < ntimes; time++)
          timeOffset[runIdx * noffsets + time] = run_offset + offsets[time];

        runIdx++;
      }

      for (FmrcInv.UberGrid ugrid : runseq.getUberGrids()) {
        grids.add(new Grid(ugrid.getName(), getInventory(ugrid)));
      }
    }

    // create GridInventory, see if it matches other Grids
    private GridInventory getInventory(FmrcInv.UberGrid ugrid) {
      GridInventory result = null;
      GridInventory need = new GridInventory(ugrid, timeOffset, noffsets);

      // see if we already have it
      for (GridInventory got : invList) {
        if (got.equalData(need)) {
          result = got;
          break;
        }
      }
      if (result == null) {
        invList.add(need);
        result = need;
      }
      return result;
    }

    double getTimeCoord(int run, int time) {
      return timeOffset[run * noffsets + time];
    }

    // time coord in hours since base
    private List<TimeInv> makeBest(FeatureCollectionConfig.BestDataset bd) {
      Map<Double, TimeInv> map = new HashMap<Double, TimeInv>();
      for (int run = 0; run < nruns; run++) {
        for (int time = 0; time < noffsets; time++) {
          double baseOffset = timeOffset[run * noffsets + time];
          if (Double.isNaN(baseOffset)) continue;
          if (bd != null && baseOffset < bd.greaterThan) continue; // skip it
          map.put(baseOffset, new TimeInv(run, time, baseOffset)); // later ones override
        }
      }

      Collection<TimeInv> values = map.values();
      int n = values.size();
      List<TimeInv> best = Arrays.asList((TimeInv[]) values.toArray(new TimeInv[n]));
      Collections.sort(best);
      timeCoordMap.put("best", best);
      return best;
    }

    private List<TimeInv> makeRun(int runIdx) {
      List<TimeInv> result = new ArrayList<TimeInv>(noffsets);
      for (int time = 0; time < noffsets; time++) {
        double offset = timeOffset[runIdx * noffsets + time];
        if (!Double.isNaN(offset))
          result.add(new TimeInv(runIdx, time, offset));
      }
      timeCoordMap.put("run" + runIdx, result);
      return result;
    }

    private List<TimeInv> makeConstantForecast(double offset) {
      List<TimeInv> result = new ArrayList<TimeInv>(noffsets);
      for (int run = 0; run < nruns; run++) {
        for (int time = 0; time < noffsets; time++) { // search for all offsets that match - presumably 0 or 1 per run
          double baseOffset = timeOffset[run * noffsets + time];
          if (Double.isNaN(baseOffset)) continue;
          if (Misc.closeEnough(baseOffset, offset))
            result.add(new TimeInv(run, time, offset - timeOffset[run * noffsets])); // use offset from start of run
        }
      }
      timeCoordMap.put("forecast" + offset, result);
      return result;
    }

    private List<TimeInv> makeConstantOffset(double offset) {
      List<TimeInv> result = new ArrayList<TimeInv>(nruns);
      for (int run = 0; run < nruns; run++) {
        for (int time = 0; time < noffsets; time++) { // search for all offsets that match - presumably 0 or 1 per run
          double baseOffset = getTimeCoord(run, time);
          if (Double.isNaN(baseOffset)) continue;
          double runOffset = baseOffset - getTimeCoord(run, 0);
          if (!Double.isNaN(baseOffset) && Misc.closeEnough(runOffset, offset))
            result.add(new TimeInv(run, time, baseOffset));
        }
      }
      timeCoordMap.put("offset" + offset, result);
      return result;
    }

    class Grid implements java.io.Serializable {
      String name;
      GridInventory inv;

      Grid(String name, GridInventory inv) {
        this.name = name;
        this.inv = inv;
      }

      Gridset getGridset() {
        return Gridset.this;
      }

      TimeInventory.Instance getInstance(int runIdx, int timeIdx) {
        int locIdx = inv.getLocation(runIdx, timeIdx);
        if (locIdx == 0) return null;

        int invIndex = inv.getInvIndex(runIdx, timeIdx);
        return new TimeInstance(locationList.get(locIdx - 1), invIndex);
      }
    }
  }

  // represents 1 element in a 2d time matrix
  private class TimeInv implements Comparable<TimeInv> {
    int runIdx, timeIdx;
    double offset; // hours since base or hours since run time

    TimeInv(int runIdx, int timeIdx, double offset) {
      this.runIdx = runIdx;
      this.timeIdx = timeIdx;
      this.offset = offset;
    }

    @Override
    public int compareTo(TimeInv o) {
      return (int) (offset - o.offset);
    }
  }

  // track inventory, shared amongst grids
  public class GridInventory implements java.io.Serializable {
    int noffsets;
    int[] location;  // (run,time) file location (index+1 into locationList, 0 = missing)
    int[] invIndex;  // (run,time) time index in file

    GridInventory(FmrcInv.UberGrid ugrid, double[] timeOffset, int noffsets) {
      this.noffsets = noffsets;
      this.location = new int[nruns * noffsets];
      this.invIndex = new int[nruns * noffsets];

      // loop over runDates
      int gridIdx = 0;
      List<FmrInv.GridVariable> grids = ugrid.getRuns(); // must be sorted by rundate. extract needed info, do not keep reference

      for (int runIdx = 0; runIdx < nruns; runIdx++) {
        Date runDate = FmrcInv.makeOffsetDate(base, runOffset[runIdx]);

        // do we have a grid for this runDate?
        if (gridIdx >= grids.size()) {
          log.debug(collectionName+": cant find "+ugrid.getName()+" for "+df.toDateTimeStringISO(runDate)); // could be normal condition
          break;
        }
        FmrInv.GridVariable grid = grids.get(gridIdx);
        if (!grid.getRunDate().equals(runDate))
          continue;
        gridIdx++; // for next loop

        // loop over actual inventory
        for (GridDatasetInv.Grid inv : grid.getInventory()) {
          double invOffset = FmrcInv.getOffsetInHours(base, inv.tc.getRunDate());
          double[] offsets = inv.tc.getOffsetHours();
          for (int i = 0; i < offsets.length; i++) {
            int timeIdx = findIndex(timeOffset, runIdx, invOffset + offsets[i]);
            if (timeIdx >= 0) {
              location[runIdx * noffsets + timeIdx] = findLocation(inv.getLocation()) + 1;
              invIndex[runIdx * noffsets + timeIdx] = i;
            }
          }
        }
      }
    }

    private boolean equalData(Object oo) {
      GridInventory o = (GridInventory) oo;
      if (o.location.length != location.length) return false;
      if (o.invIndex.length != invIndex.length) return false;
      for (int i = 0; i < location.length; i++)
        if (location[i] != o.location[i]) return false;
      for (int i = 0; i < invIndex.length; i++)
        if (invIndex[i] != o.invIndex[i]) return false;
      return true;
    }

    // LOOK linear search!
    private int findIndex(double[] coords, int runIdx, double want) {
      for (int j = 0; j < noffsets; j++)
        if (Misc.closeEnough(coords[runIdx * noffsets + j], want)) return j;
      return -1;
    }

    // LOOK linear search!
    private int findLocation(String location) {
      return locationList.indexOf(location);
    }

    int getLocation(int run, int time) {
      return location[run * noffsets + time];
    }

    int getInvIndex(int run, int time) {
      return invIndex[run * noffsets + time];
    }

  }

  static class TimeInstance implements TimeInventory.Instance {
    String location;
    int index; // time index in the file named by inv

    TimeInstance(String location, int index) {
      this.location = location;
      this.index = index;
    }

    @Override
    public String getDatasetLocation() {
      return location;
    }

    @Override
    public int getDatasetIndex() {
      return index;
    }
  }

  TimeInventory makeBestDatasetInventory() {
    return new BestDatasetInventory(null);
  }

  TimeInventory makeBestDatasetInventory(FeatureCollectionConfig.BestDataset bd) {
    return new BestDatasetInventory(bd);
  }

  TimeInventory makeRunTimeDatasetInventory(Date run) throws FileNotFoundException {
    return new RunTimeDatasetInventory(run);
  }

  TimeInventory getConstantForecastDataset(Date time) throws FileNotFoundException {
    return new ConstantForecastDataset(time);
  }

  TimeInventory getConstantOffsetDataset(double hour) throws FileNotFoundException {
    return new ConstantOffsetDataset(hour);
  }

  class BestDatasetInventory implements TimeInventory {
    FeatureCollectionConfig.BestDataset bd; // parameterized for offsets >= p. null means want all offsets

    BestDatasetInventory( FeatureCollectionConfig.BestDataset bd) {
      this.bd = bd;
    }
 
    @Override
    public String getName() {
      return (bd == null) ? "Best" : bd.name;
    }

    @Override
    public int getTimeLength(Gridset gridset) {
      List<TimeInv> best = gridset.timeCoordMap.get(getName());
      if (best == null) best = gridset.makeBest(bd);
      return best.size();
    }

    @Override
    public double[] getTimeCoords(Gridset gridset) {
      List<TimeInv> best = gridset.timeCoordMap.get(getName());
      if (best == null) best = gridset.makeBest(bd);

      double[] result = new double[best.size()];
      for (int i = 0; i < best.size(); i++) {
        TimeInv b = best.get(i);
        result[i] = b.offset;
      }
      return result;
    }

    @Override
    public double[] getRunTimeCoords(Gridset gridset) {
      List<TimeInv> best = gridset.timeCoordMap.get(getName());
      if (best == null)
        best = gridset.makeBest(bd);
      double[] result = new double[best.size()];
      for (int i = 0; i < best.size(); i++) {
        TimeInv b = best.get(i);
        result[i] = gridset.getTimeCoord(b.runIdx, 0);  // the first one for the run given by runIdx
      }
      return result;
    }

    @Override
    public double[] getOffsetCoords(Gridset gridset) {
      List<TimeInv> best = gridset.timeCoordMap.get(getName());
      if (best == null)
        best = gridset.makeBest(bd);

      double[] result = new double[best.size()];
      for (int i = 0; i < best.size(); i++) {
        TimeInv b = best.get(i);
        result[i] = b.offset - gridset.getTimeCoord(b.runIdx, 0);  // offset from run start
      }
      return result;
    }

    @Override
    public Instance getInstance(Gridset.Grid grid, int timeIdx) {
      Gridset gridset = grid.getGridset();
      List<TimeInv> best = gridset.timeCoordMap.get(getName());
      if (best == null)
        best = gridset.makeBest(bd);

      TimeInv b = best.get(timeIdx);
      int locIdx = grid.inv.getLocation(b.runIdx, b.timeIdx);
      if (locIdx == 0) return null;

      int invIndex = grid.inv.getInvIndex(b.runIdx, b.timeIdx);
      return new TimeInstance(locationList.get(locIdx - 1), invIndex);
    }
  }

  class RunTimeDatasetInventory implements TimeInventory {
    int runIdx = -1;

    RunTimeDatasetInventory(Date run) throws FileNotFoundException {
      double offset = FmrcInv.getOffsetInHours(base, run);
      for (int i = 0; i < runOffset.length; i++) {
        if (Misc.closeEnough(runOffset[i], offset)) {
          runIdx = i;
          break;
        }
      }
      if (runIdx < 0)
        throw new FileNotFoundException("No run date of " + run);
    }

    @Override
    public String getName() {
      DateFormatter df = new DateFormatter();
      return "Run " + df.toDateTimeStringISO(FmrcInv.makeOffsetDate(base, runOffset[runIdx]));
    }

    @Override
    public int getTimeLength(Gridset gridset) {
      List<TimeInv> coords = gridset.timeCoordMap.get("run" + runIdx);
      if (coords == null)
        coords = gridset.makeRun(runIdx);
      return coords.size();
    }

    @Override
    public double[] getTimeCoords(Gridset gridset) {
      List<TimeInv> coords = gridset.timeCoordMap.get("run" + runIdx);
      if (coords == null)
        coords = gridset.makeRun(runIdx);

      double[] result = new double[coords.size()];
      for (int i = 0; i < coords.size(); i++) {
        TimeInv b = coords.get(i);
        result[i] = b.offset;
      }
      return result;
    }

    @Override
    public double[] getRunTimeCoords(Gridset gridset) {
      return null;
    }

    @Override
    public double[] getOffsetCoords(Gridset gridset) {
      List<TimeInv> coords = gridset.timeCoordMap.get("run" + runIdx);
      if (coords == null)
        coords = gridset.makeRun(runIdx);

      double startRun = gridset.getTimeCoord(runIdx, 0);
      double[] result = new double[coords.size()];
      for (int i = 0; i < coords.size(); i++) {
        TimeInv b = coords.get(i);
        result[i] = b.offset - startRun;
      }
      return result;
    }

    @Override
    public Instance getInstance(Gridset.Grid grid, int timeIdx) {
      Gridset gridset = grid.getGridset();
      List<TimeInv> coords = gridset.timeCoordMap.get("run" + runIdx);
      if (coords == null)
        coords = gridset.makeRun(runIdx);

      TimeInv b = coords.get(timeIdx);
      return grid.getInstance(b.runIdx, b.timeIdx);
    }
  }

  class ConstantForecastDataset implements TimeInventory {
    double offset;

    ConstantForecastDataset(Date time) throws FileNotFoundException {
      this.offset = FmrcInv.getOffsetInHours(base, time);
      for (Date d : getForecastDates())
        if (d.equals(time))
          return; // ok

      throw new FileNotFoundException("No forecast date of " + time);  // we dont got it
    }

    @Override
    public String getName() {
      DateFormatter df = new DateFormatter();
      return "Constant Forecast " + df.toDateTimeStringISO(FmrcInv.makeOffsetDate(base, offset));
    }

    @Override
    public int getTimeLength(Gridset gridset) {
      List<TimeInv> coords = gridset.timeCoordMap.get("forecast" + offset);
      if (coords == null)
        coords = gridset.makeConstantForecast(offset);
      return coords.size();
    }

    @Override
    public double[] getTimeCoords(Gridset gridset) {
      return null;
    }

    @Override
    public double[] getRunTimeCoords(Gridset gridset) {
      List<TimeInv> coords = gridset.timeCoordMap.get("forecast" + offset);
      if (coords == null)
        coords = gridset.makeConstantForecast(offset);

      double[] result = new double[coords.size()];
      for (int i = 0; i < coords.size(); i++) {
        TimeInv b = coords.get(i);
        result[i] = gridset.getTimeCoord(b.runIdx, 0);
      }
      return result;
    }

    @Override
    public double[] getOffsetCoords(Gridset gridset) {
      List<TimeInv> coords = gridset.timeCoordMap.get("forecast" + offset);
      if (coords == null)
        coords = gridset.makeConstantForecast(offset);

      double[] result = new double[coords.size()];
      for (int i = 0; i < coords.size(); i++) {
        TimeInv b = coords.get(i);
        result[i] = b.offset;
      }
      return result;
    }

    @Override
    public Instance getInstance(Gridset.Grid grid, int timeIdx) {
      Gridset gridset = grid.getGridset();
      List<TimeInv> coords = gridset.timeCoordMap.get("forecast" + offset);
      if (coords == null)
        coords = gridset.makeConstantForecast(offset);

      TimeInv b = coords.get(timeIdx);
      return grid.getInstance(b.runIdx, b.timeIdx);
    }
  }

  class ConstantOffsetDataset implements TimeInventory {
    double offset;

    ConstantOffsetDataset(double offset) throws FileNotFoundException {
      this.offset = offset;
      boolean ok = false;
      double[] offsets = getForecastOffsets();
      for (int i=0; i<offsets.length; i++)
        if (Misc.closeEnough(offsets[i], offset)) ok = true;

      if (!ok)
        throw new FileNotFoundException("No constant offset dataset for = " + offset);
    }

    @Override
    public String getName() {
      return "Constant Offset " + offset + " hours";
    }

    @Override
    public int getTimeLength(Gridset gridset) {
      List<TimeInv> coords = gridset.timeCoordMap.get("offset" + offset);
      if (coords == null)
        coords = gridset.makeConstantOffset(offset);
      return coords.size();
    }

    @Override
    public double[] getTimeCoords(Gridset gridset) {
      List<TimeInv> coords = gridset.timeCoordMap.get("offset" + offset);
      if (coords == null)
        coords = gridset.makeConstantOffset(offset);

      double[] result = new double[coords.size()];
      for (int i = 0; i < coords.size(); i++) {
        TimeInv b = coords.get(i);
        result[i] = b.offset;
      }
      return result;
    }

    @Override
    public double[] getRunTimeCoords(Gridset gridset) {
      List<TimeInv> coords = gridset.timeCoordMap.get("offset" + offset);
      if (coords == null)
        coords = gridset.makeConstantOffset(offset);

      double[] result = new double[coords.size()];
      for (int i = 0; i < coords.size(); i++) {
        TimeInv b = coords.get(i);
        result[i] = gridset.getTimeCoord(b.runIdx, 0);
      }
      return result;
    }

    @Override
    public double[] getOffsetCoords(Gridset gridset) {
      return null;
    }

    @Override
    public Instance getInstance(Gridset.Grid grid, int timeIdx) {
      Gridset gridset = grid.getGridset();
      List<TimeInv> coords = gridset.timeCoordMap.get("offset" + offset);
      if (coords == null)
        coords = gridset.makeConstantOffset(offset);

      TimeInv b = coords.get(timeIdx);
      return grid.getInstance(b.runIdx, b.timeIdx);
    }
  }
}
