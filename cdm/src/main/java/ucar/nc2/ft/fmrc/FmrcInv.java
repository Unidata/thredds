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

import net.jcip.annotations.Immutable;

import java.util.*;

/**
 * Inventory for a Forecast Model Run Collection = a series of Forecast Model Runs.
 *
 * Create rectangular representation of var(runtime, time) of data(ens, vert, x, y).
 * For each Grid, the vert, time and ens coordinates are created as the union of the components.
 * Make sure to share coordinates across grids where they are equivilent.
 * <p/>
 * We are thus making a rectangular array var(runtime, time, ens, level).
 * So obviously we have to tolerate missing data.
 * Keeps track of what inventory exists, and where it is.
 * <p/>
 *
 * @author caron
 * @since Jan 11, 2010
 */
@Immutable
public class FmrcInv {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FmrcInv.class);

  public static Date addHour(Date d, double hour) {
    long msecs = d.getTime();
    msecs += hour * 3600 * 1000;
    return new Date(msecs);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////
  private final String name; // name of ForecastModelRunCollection

  // this is where the time coordinates are normalized and missing data may occur
  private final List<RunSeq> runSeqs = new ArrayList<RunSeq>();

  // list of unique EnsCoord
  private final List<EnsCoord> ensCoords = new ArrayList<EnsCoord>();

  // list of unique VertCoord
  private final List<VertCoord> vertCoords = new ArrayList<VertCoord>();

  // the list of runs
  private final List<FmrInv> fmrList;

  // the list of grids
  private final List<UberGrid> uberGridList;              // sorted list of UberGrid

  // all run times
  private final List<Date> runTimeList;                // sorted list of Date: all run times

  // track offsets and bounds
  private final Date baseDate;            // first runtime : offsetsAll calculated from here

  // all forecast times
  private final List<Date> forecastTimeList;          // sorted list of Date : all forecast times

  private Calendar cal;

  // use on motherlode to regularize the missing inventory
  private final boolean regularize;

  /////////////////////////////////////////////////////

  /**
   * Construct the inventory from a set of FmrInv, one for each model run.
   *
   * @param name name of collection
   * @param fmrList the component runs FmrInv
   * @param regularize regularize time coords based on offset from 0Z
   */
  FmrcInv(String name, List<FmrInv> fmrList, boolean regularize) {
    this.name = name;
    this.regularize = regularize;

    this.fmrList = new ArrayList<FmrInv>(fmrList);
    runTimeList = new ArrayList<Date>();

    Date firstDate = null;
    Map<String, UberGrid> uvHash = new HashMap<String, UberGrid>();
    Set<Double> offsetHash = new HashSet<Double>();
    Set<TimeCoord.Tinv> intervalHash = new HashSet<TimeCoord.Tinv>();
    Set<Date> forecastTimeHash = new HashSet<Date>();
    for (FmrInv fmrInv : fmrList) {
      runTimeList.add(fmrInv.getRunDate());
      if (firstDate == null) firstDate = fmrInv.getRunDate();

      // hour of this runDate
      int hour = getHour(fmrInv.getRunDate());

      // for each GridVariable, add to the UberGrid
      for (FmrInv.GridVariable fmrGrid : fmrInv.getGrids()) {
        UberGrid uv = uvHash.get(fmrGrid.getName());
        if (uv == null) {
          uv = new UberGrid(fmrGrid.getName());
          uvHash.put(fmrGrid.getName(), uv);
        }
        uv.addGridVariable(fmrGrid, hour);
      }

      // track overall list of times, offsets, and intervals
      for (TimeCoord tc : fmrInv.getTimeCoords()) {

        if (tc.isInterval()) {
          double[] bounds1 =  tc.getBound1();
          double[] bounds2 =  tc.getBound2();
          for (int i=0; i<bounds1.length; i++) {
            Date date1 = addHour(fmrInv.getRunDate(),  bounds1[i]);
            Date date2 = addHour(fmrInv.getRunDate(),  bounds2[i]);
            forecastTimeHash.add(date2); // second is used as the forecast date
            double b1 = getOffsetInHours(firstDate, date1);
            double b2 = getOffsetInHours(firstDate, date2);
            intervalHash.add(new TimeCoord.Tinv(b1, b2));
          }

        } else {
          // regular single time offset - add to offsetHash
          for (double offset : tc.getOffsetTimes()) {
            Date fcDate = addHour(fmrInv.getRunDate(), offset);
            forecastTimeHash.add(fcDate); // track all forecast times
            double d = getOffsetInHours(firstDate, fcDate);
            offsetHash.add(d); // track all offset hours, calculated from baseDate
          }
        }
      }
    }
    baseDate = firstDate;

    // create the overall list of variables and coordinates
    uberGridList = new ArrayList<UberGrid>(uvHash.values());
    Collections.sort(uberGridList);
    for (UberGrid uv : uberGridList) {
      uv.finish();
    }

    // assign sequence ids
    int seqno = 0;
    for (RunSeq seq : runSeqs) {
      seq.id = seqno++;
    }

    // create the overall list of forecast times
    forecastTimeList = Arrays.asList((Date[]) forecastTimeHash.toArray(new Date[forecastTimeHash.size()]));
    Collections.sort(forecastTimeList);

    // create the overall list of offsets - may be zero
    List<Double> offsetsAll = Arrays.asList((Double[]) offsetHash.toArray(new Double[offsetHash.size()]));
    Collections.sort(offsetsAll);
    int counto = 0;
    double[] offs = new double[offsetsAll.size()];
    for (double off : offsetsAll) offs[counto++] = off;
    tcOffAll = new TimeCoord(baseDate);
    tcOffAll.setOffsetTimes(offs);

    // create the overall list of offsets - may be zero
    List<TimeCoord.Tinv> intervalAll = Arrays.asList((TimeCoord.Tinv[]) intervalHash.toArray(new TimeCoord.Tinv[intervalHash.size()]));
    Collections.sort(intervalAll);
    tcIntAll = new TimeCoord(baseDate);
    tcIntAll.setBounds(intervalAll);
  }

  // not really needed
  private final TimeCoord tcOffAll;       // all offsets in this collection, reletive to baseDate
  private final TimeCoord tcIntAll;       // all intervals in this collection, reletive to baseDate

  // public for debugging
  public List<FmrInv> getFmrList() {
    return fmrList;
  }

  private int getHour(Date d) {
    if (cal == null) {
      cal = Calendar.getInstance();
      cal.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    cal.setTime(d);
    return cal.get(Calendar.HOUR_OF_DAY);
  }

  /* private UberGrid findVar(String varName) {
    for (UberGrid uv : uberGridList) {
      if (uv.gridName.equals(varName))
        return uv;
    }
    return null;
  }

  public int findFmrIndex(Date runDate) {
    for (int i = 0; i < fmrList.size(); i++) {
      FmrInv fmr = fmrList.get(i);
      if (fmr.getRunDate().equals(runDate)) return i;
    }
    return -1;
  }

  public List<Date> getRunTimes() {
    return runTimeList;
  }

  */

  public String getName() {
    return name;
  }

  public List<RunSeq> getRunSeqs() {
    return runSeqs;
  }

  public List<EnsCoord> getEnsCoords() {
    return ensCoords;
  }

  public List<VertCoord> getVertCoords() {
    return vertCoords;
  }

  public List<UberGrid> getUberGrids() {
    return uberGridList;
  }

  public UberGrid findUberGrid(String name) {
    for (UberGrid grid : uberGridList)
      if (grid.getName().equals(name)) return grid;
    return null;
  }


  public List<Date> getForecastTimes() {
    return forecastTimeList;
  }

  public List<FmrInv> getFmrInv() {
    return fmrList;
  }

  public Date getBaseDate() {
    return baseDate;
  }

  /* public TimeCoord getTimeCoordsAll() {
    return tcAll;
  } */

  /**
   * Find the difference between two dates in hours
   * @param base date1
   * @param forecast date2
   * @return (forecast minus base) difference in hours
   */
  public static double getOffsetInHours(Date base, Date forecast) {
    double diff = forecast.getTime() - base.getTime();
    return diff / 1000.0 / 60.0 / 60.0;
  }

  /**
   * Create a date from base and hour offset
   * @param base base date
   * @param offset hourss
   * @return base + offset as a Date
   */
  public static Date makeOffsetDate(Date base, double offset) {
    long time = base.getTime() + (long) (offset * 60 * 60 * 1000);
    return new Date(time);
  }

  ////////////////////////////////////////////////////////////////////

  // The collection of GridVariable for one variable, across all runs in the collection
  // immutable after finish() is called.
  public class UberGrid implements Comparable<UberGrid> {
    private final String gridName;
    private final List<FmrInv.GridVariable> runs = new ArrayList<FmrInv.GridVariable>();

    private VertCoord vertCoordUnion = null;
    private EnsCoord ensCoordUnion = null;
    private RunSeq runSeq = null;

    UberGrid(String name) {
      this.gridName = name;
    }

    void addGridVariable(FmrInv.GridVariable grid, int hour) {
      runs.add(grid);
    }

    public String getName() {
      return gridName;
    }

    public String toString() {
      return gridName;
    }

     // the union of all offset hours, ignoring rundate
    public TimeCoord getUnionTimeCoord() {
      return runSeq.getUnionTimeCoord();
    }

    public boolean isInterval() {
      return getUnionTimeCoord().isInterval();
    }

    public String getTimeCoordName() {
      return runSeq.getName();
    }

    public String getVertCoordName() {
      return (vertCoordUnion == null) ? "" : vertCoordUnion.getName();
    }

    public List<FmrInv.GridVariable> getRuns() {
      return runs;
    }

    public int compareTo(UberGrid o) {
      return gridName.compareTo(o.gridName);
    }

    public int countTotal() {
      int total = 0;
      for (FmrInv.GridVariable grid : runs)
        total += grid.countTotal();
      return total;
    }

    public int countExpected() {
      int nvert = (vertCoordUnion == null) ? 1 : vertCoordUnion.getSize();

      int ntimes = 0;
      for (FmrInv.GridVariable grid : runs) {  // maybe should use the runseq ???
        TimeCoord exp = grid.getTimeExpected();
        ntimes += exp.getNCoords();
      }
      return ntimes * nvert;
    }

    void finish() {
      if (runs.size() == 1) {
        FmrInv.GridVariable grid = runs.get(0);
        ensCoordUnion = EnsCoord.findEnsCoord(getEnsCoords(), grid.ensCoordUnion);
        vertCoordUnion = VertCoord.findVertCoord(getVertCoords(), grid.vertCoordUnion);
        //timeCoordUnion = TimeCoord.findTimeCoord(getTimeCoords(), grid.timeCoordUnion);
        //timeCoordUnion.addGridVariable(this);
        grid.timeExpected = grid.timeCoordUnion; // if only one, not much else to do
        this.runSeq = findRunSeq(runs);
        this.runSeq.addVariable(this);
        return;
      }

      // run over all ensCoords and construct the union
      List<EnsCoord> ensList = new ArrayList<EnsCoord>();
      EnsCoord ec_union = null;
      for (FmrInv.GridVariable grid : runs) {
        EnsCoord ec = grid.ensCoordUnion;
        if (ec == null) continue;
        if (ec_union == null)
          ec_union = new EnsCoord(ec);
        else if (!ec_union.equalsData(ec))
          ensList.add(ec);
      }
      if (ec_union != null) {
        if (ensList.size() > 0) EnsCoord.normalize(ec_union, ensList); // add the other coords
        ensCoordUnion = EnsCoord.findEnsCoord(getEnsCoords(), ec_union);  // find unique within collection
      }

      // run over all vertCoords and construct the union
      List<VertCoord> vertList = new ArrayList<VertCoord>();
      VertCoord vc_union = null;
      for (FmrInv.GridVariable grid : runs) {
        VertCoord vc = grid.vertCoordUnion;
        if (vc == null) continue;
        if (vc_union == null)
          vc_union = new VertCoord(vc);
        else if (!vc_union.equalsData(vc)) {
//          log.warn(name+" Grid "+ gridName +" has different vert coords in run " + grid.getRunDate());
          vertList.add(vc);
        }
      }
      if (vc_union != null) {
        if (vertList.size() > 0) VertCoord.normalize(vc_union, vertList); // add the other coords
        vertCoordUnion = VertCoord.findVertCoord(getVertCoords(), vc_union); // now find unique within collection
      }

     // optionally calculate expected inventory based on matching run hours
      if (regularize) {
        // create groups of runs with the same runtime hour (integer offset from 0Z)
        Map<Integer, HourGroup> hourMap = new HashMap<Integer, HourGroup>();
        for (FmrInv.GridVariable grid : runs) {
          Date runDate = grid.getRunDate();
          int hour = getHour(runDate);
          HourGroup hg = hourMap.get(hour);
          if (hg == null) {
            hg = new HourGroup(hour);
            hourMap.put(hour, hg);
          }
          hg.runs.add(grid);
        }

        // assume each hour group should have the same set of forecast time coords, as represented by their offset
        for (HourGroup hg : hourMap.values()) {
          // run over all timeCoords in this group and construct the union
          List<TimeCoord> timeListExp = new ArrayList<TimeCoord>();
          for (FmrInv.GridVariable run : hg.runs)
            timeListExp.add(run.timeCoordUnion);
          // note that in this case, the baseDates of the TimeCoords in timeListExp are not the same
          // we are just using this routine to get the union of offset hours or intervals
          hg.expected = TimeCoord.makeUnion(timeListExp, baseDate); // add the other coords
          if (hg.expected.isInterval()) {
            // we discard the resulting TimeCoord and just use the union of intervals.
            for (FmrInv.GridVariable grid : hg.runs) {
              grid.timeExpected = new TimeCoord(grid.getRunDate());
              grid.timeExpected.setBounds(hg.expected.getBound1(), hg.expected.getBound2());
            }
            
          } else {
            // we discard the resulting TimeCoord and just use the union of offsets
            for (FmrInv.GridVariable grid : hg.runs)
              grid.timeExpected = new TimeCoord(grid.getRunDate(), hg.expected.getOffsetTimes());
          }
        }

        // now find the RunSeq, based on the timeExpected
        this.runSeq = findRunSeq(runs);
        this.runSeq.addVariable(this);

      } else {
        // otherwise expected == actual
        for (FmrInv.GridVariable grid : runs)
          grid.timeExpected = grid.timeCoordUnion;

        // now find the RunSeq, based on the timeExpected
        this.runSeq = findRunSeq(runs);
        this.runSeq.addVariable(this);

        /* run over all timeCoords and construct the union
        List<TimeCoord> timeList = new ArrayList<TimeCoord>();
        for (FmrInv.GridVariable grid : runs)
          timeList.add(grid.timeCoordUnion);
        TimeCoord tc_union = TimeCoord.makeUnion(timeList, baseDate); // create the union of all time coords used by this grid
        this.timeCoordUnion = TimeCoord.findTimeCoord(getRunSeqs(), tc_union); // track unique ones in the FmrcInv */
      }
    }

  } // end UberGrid

  // immutable after UberGrid.finish() is called.
  private class HourGroup {
    final int hour;
    final List<FmrInv.GridVariable> runs = new ArrayList<FmrInv.GridVariable>();
    private TimeCoord expected;

    HourGroup(int hour) {
      this.hour = hour;
    }
  }

  private RunSeq findRunSeq(List<FmrInv.GridVariable> runs) {
    for (RunSeq seq : runSeqs) {
      if (seq.equalsData(runs))
        return seq;
    }

    // make a new one
    RunSeq result = new RunSeq(runs);
    runSeqs.add(result);
    return result;
  }

  /**
   * Represents a sequence of Runs, each run has a particular TimeCoord.
   * this is where the time coordinates may be regularized
   * keep track of all the Variables with the same RunSeq
   */
  // immutable after UberGrid.finish() is called.
  public class RunSeq {
    private final HashMap<Date, TimeCoord> coordMap; // runDate, timeExpected
    private final List<UberGrid> vars = new ArrayList<UberGrid>(); // list of UberGrid that use this
    private int id;
    private List<TimeCoord> timeList = null; // timeList has differing runDates
    private TimeCoord timeCoordUnion = null; // union of all offset hours
    private boolean isInterval;

    RunSeq(List<FmrInv.GridVariable> runs) {
      this.coordMap = new HashMap<Date, TimeCoord>(2 * runs.size());

      // make sure every date has a slot
      for (Date d : runTimeList)
        this.coordMap.put(d, TimeCoord.EMPTY);

      // overwrite with actual coords
      boolean first = true;
      for (FmrInv.GridVariable grid : runs) {
        this.coordMap.put(grid.getRunDate(), grid.getTimeExpected()); // match on timeExpected

        // check intervals are consistent
        if (first)
          isInterval = grid.getTimeCoord().isInterval();
        else if (isInterval  != grid.getTimeCoord().isInterval()) {
          log.error("mixed intervals for grid "+grid. getName());
          throw new IllegalArgumentException("mixed intervals for grid "+grid. getName());
        }
        first = false;
      }
    }

    public boolean isInterval() {
      return isInterval;
    }

    public List<TimeCoord> getTimes() {
      if (timeList == null)
        getUnionTimeCoord();
      return timeList;
    }

    public String getName() {
      return id == 0 ? "time" : "time" + id;
    }

    public int getNTimeOffsets() {
      int n = 0;
      for (TimeCoord tc : coordMap.values())
        n = Math.max(n, tc.getNCoords());
      return n;
    }

    // the union of all offset hours, ignoring rundate, so its the rectangularization of the
    // offsets for each run
    // has the side effect of constructing timeList, the list of expected TimeCoords, one for each run
    public TimeCoord getUnionTimeCoord() {
      if (timeCoordUnion == null) { // deferred creation
        // eliminate the empties
        timeList = new ArrayList<TimeCoord>();
        for (TimeCoord tc : coordMap.values()) {
          if ((tc != null) && (tc != TimeCoord.EMPTY))
            timeList.add(tc);
        }
        // sort by run Date
        Collections.sort(timeList, new Comparator<TimeCoord>() {
          public int compare(TimeCoord o1, TimeCoord o2) {
            if (o1 == null || o1.getRunDate() == null) return -1;
            if (o2 == null || o2.getRunDate() == null) return 1;
            return o1.getRunDate().compareTo(o2.getRunDate());
          }
        });
        // here again the timeList has differing runDates
        timeCoordUnion = TimeCoord.makeUnion(timeList, baseDate); // create the union of all offsets used by this grid
      }
      return timeCoordUnion;
    }

    /**
     * Decide if this RunSeq is equivalent to the list of Runs.
     *
     * @param oruns list of FmrInv.GridVariable, use their expected times to match
     * @return true if it has an equivalent set of runs.
     */
    boolean equalsData(List<FmrInv.GridVariable> oruns) {
      List<FmrInv.GridVariable> okAdd = null; // may need to fill in the gaps
      for (FmrInv.GridVariable grid : oruns) {
        TimeCoord run = coordMap.get(grid.getRunDate());
        if (run == null) {
          if (okAdd == null) okAdd = new ArrayList<FmrInv.GridVariable>();
          okAdd.add(grid);
        } else {
          TimeCoord orune = grid.getTimeExpected();
          if (!run.equalsData(orune)) return false;
        }
      }

      // everything else matches, ok to expand this runSeq with missing runs
      if (okAdd != null)
        for (FmrInv.GridVariable grid : okAdd)
          this.coordMap.put(grid.getRunDate(), grid.getTimeExpected());

      return true;
    }

    // keep track of all the Variables that have this RunSeq
    void addVariable(UberGrid uv) {
      vars.add(uv);
    }

    public List<UberGrid> getUberGrids() {
      return vars;
    }
  }


  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /* debugging  - Fmrc2 Panel


  public void showRuntimeOffsetMatrix(Formatter out) {
    out.format("                                  Forecast Time Offset %n");
    out.format("     RunTime                 ");
    for (double offsetHour : tcOffAll.getOffsetTimes()) {
      out.format("%10.0f", offsetHour);
    }
    out.format("%n");

    int count = 0;
    for (int i = 0; i < fmrList.size(); i++) {
      FmrInv fmr = fmrList.get(i);
      Date runDate = fmr.getRunDate();
      out.format("%2d %s      ", i, dateFormatter.toDateTimeStringISO(runDate));
      for (double offsetHour : tcOffAll.getOffsetTimes()) {
        Inventory inv = getInventory(fmr, offsetHour);
        count += inv.showInventory( out);
      }
      out.format("%n");
    }
    out.format("%ntotal inventory count = %d%n", count);
  }

  // for a given offset hour and Fmr, find the expected and actual inventory over all grids
  private Inventory getInventory(FmrInv fmr, double hour) {
    int actual = 0, expected = 0;
    for (FmrInv.GridVariable grid : fmr.getGrids()) {
      TimeCoord tExpect = grid.getTimeExpected();
      if (tExpect.findIndex(hour) >= 0)
        expected += grid.getNVerts();

      for (GridDatasetInv.Grid inv : grid.getInventory()) {
        TimeCoord tc = inv.getTimeCoord();
        if (tc.findIndex(hour) >= 0)
          actual += inv.getVertCoordLength();
      }

    }
    return new Inventory(actual, expected);
  }

  @Immutable
  public static class Inventory {
    public final int actual;
    public final int expected;

    public Inventory(int actual, int expected) {
      this.actual = actual;
      this.expected = expected;
    }

    public int showInventory(Formatter out) {
      if (actual != expected) {
        out.format("%10s", actual + "/" + expected);
      } else if (actual == 0)
        out.format("          "); // blank
      else
        out.format("%10d", actual);

      return actual;
    }

  }

  private class TimeInventory {
    final TimeCoord tc; // all the TimeCoords possible
    final FmrcInv.UberGrid ugrid; // for this grid
    final FmrInv.GridVariable[] useGrid; // use this run and grid for ith offset
    final int[] runIndex; // the index of the run in the fmrList. ie findFmrIndex()

    TimeInventory(TimeCoord tc, FmrcInv.UberGrid ugrid) {
      this.tc = tc;
      this.ugrid = ugrid;
      this.useGrid = new FmrInv.GridVariable[tc.getNCoords()];
      this.runIndex = new int[tc.getNCoords()];
    }

    private void setOffset(double offsetHour, FmrInv.GridVariable grid, int runIndex) {
      int offsetIndex = tc.findIndex(offsetHour);
      if (offsetIndex < 0)
        throw new IllegalStateException("FmrSnapshot cant find hour " + offsetHour + " in " + tc);
      this.useGrid[offsetIndex] = grid;
      this.runIndex[offsetIndex] = runIndex;
    }
  }


  //////////////////////////////////////
  // 1D subsets

  public void showBest(Formatter out) {
    out.format("%nRun used in best dataset%n");
    out.format("                              Forecast Time Offset %n");
    out.format("Grid                         ");
    for (double offsetHour : tcOffAll.getOffsetTimes())
      out.format("%4.0f ", offsetHour);
    out.format("%n");

    for (FmrcInv.UberGrid ugrid : getUberGrids()) {
      TimeInventory inv = makeBestInventory(ugrid);
      String name = ugrid.getName();
      if (name.length() > 27) name = name.substring(0, 27);
      out.format(" %-27s ", name);
      showInv(inv, out);
      out.format("%n");
    }
  }

  public void showBest2(Formatter out) {
    out.format("%nRun used in best dataset by RunSeq%n");
    out.format("Seq  Forecast Time Offset %n    ");
    for (double offsetHour : tcOffAll.getOffsetTimes())
      out.format("%4.0f ", offsetHour);
    out.format("%n");

    int count = 0;
    for (RunSeq seq : getRunSeqs()) {
      out.format("%3d ", count++);
      List<FmrcInv.UberGrid> ugrids = seq.getUberGrids();
      TimeInventory inv = makeBestInventory(ugrids.get(0));
      showInv(inv, out);
      for (FmrcInv.UberGrid ugrid : seq.getUberGrids()) {
        TimeInventory inv2 = makeBestInventory(ugrid);
        out.format(", %s ", ugrid.getName());
        if (!testInv(inv, inv2))  out.format("BAD ");
      }
      out.format("%n");
    }
  }

  // select best inventory for each forecast time
  private TimeInventory makeBestInventory(FmrcInv.UberGrid ugrid) {
    return ugrid.isInterval() ? makeBestInventoryFromIntervals(ugrid) : makeBestInventoryFromOffsets( ugrid);
  }

  private TimeInventory makeBestInventoryFromIntervals(FmrcInv.UberGrid ugrid) {
    TimeInventory inv = new TimeInventory(tcIntAll, ugrid);
    for (FmrInv.GridVariable grid : ugrid.getRuns()) {
      double forecastOffset = getOffsetInHours(baseDate, grid.getRunDate());
      int runIndex = findFmrIndex(grid.getRunDate());
      TimeCoord tc = grid.getTimeCoord(); // forecast times for this run
      for (double offset : tc.getOffsetTimes()) {
        inv.setOffset(forecastOffset + offset, grid, runIndex); // later ones override
      }
    }
    return inv;
  }

  // select best inventory for each forecast time
  private TimeInventory makeBestInventoryFromOffsets(FmrcInv.UberGrid ugrid) {
    TimeInventory inv = new TimeInventory(tcOffAll, ugrid);
    for (FmrInv.GridVariable grid : ugrid.getRuns()) {
      double forecastOffset = getOffsetInHours(baseDate, grid.getRunDate());
      int runIndex = findFmrIndex(grid.getRunDate());
      TimeCoord tc = grid.getTimeCoord(); // forecast times for this run
      for (double offset : tc.getOffsetTimes()) {
        inv.setOffset(forecastOffset + offset, grid, runIndex); // later ones override
      }
    }
    return inv;
  }

  // select best inventory for each forecast time
  public void showBest(FmrcInv.UberGrid ugrid, Formatter out) {
    out.format("%nRun used in best dataset for grid %s %n", ugrid.getName());
    out.format("                              Forecast Time Offset %n");
    out.format("Run                          ");
    for (double offsetHour : tcOffAll.getOffsetTimes()) {
      out.format("%3.0f ", offsetHour);
    }
    out.format("%n");

    TimeInventory inv = new TimeInventory(tcOffAll, ugrid);
    for (FmrInv.GridVariable grid : ugrid.getRuns()) {
      double forecastOffset = getOffsetInHours(baseDate, grid.getRunDate());
      int runIndex = findFmrIndex(grid.getRunDate());
      TimeCoord tc = grid.getTimeCoord(); // forecast times for this run
      TimeInventory invRun = new TimeInventory(tcOffAll, ugrid);
      for (double offset : tc.getOffsetTimes()) {
        inv.setOffset(forecastOffset + offset, grid, runIndex); // later ones override
        invRun.setOffset(forecastOffset + offset, grid, runIndex); // later ones override
      }
      out.format(" %-27s ", dateFormatter.toDateTimeString(grid.getRunDate()));
      showInv(invRun, out);
      out.format("%n");
    }

    String name = ugrid.getName();
    if (name.length() > 27) name = name.substring(0, 27);
    out.format(" %-27s ", "result");
    showInv(inv, out);
    out.format("%n");
  }

  private void showInv(TimeInventory tinv, Formatter out) {
    for (int i = 0; i < tinv.useGrid.length; i++) {
      FmrInv.GridVariable grid = tinv.useGrid[i];
      if (grid == null)
        out.format("    ");
      else
        out.format("%4d ", tinv.runIndex[i]);
    }
  }

  private boolean testInv(TimeInventory tinv1, TimeInventory tinv2) {
    if (tinv1.useGrid.length != tinv2.useGrid.length) return false;

    for (int i = 0; i < tinv1.useGrid.length; i++) {
      if (tinv1.runIndex[i] != tinv2.runIndex[i])
        return false;
    }
    return true;
  } */

}