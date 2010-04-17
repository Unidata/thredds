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
import ucar.nc2.units.DateFormatter;

import java.util.*;

/**
 * Inventory for a Forecast Model Run Collection - a series of Forecast Model Runs.
 * Create rectangular representation of var(runtime, time) of data(ens, vert, x, y).
 * For each Grid, the vert, time and ens coordinates are created as the union of the components.
 * Make sure to share coordinates across grids where they are equivilent.
 * <p/>
 * We are thus making a rectangular array var(runtime, time, ens, level).
 * So obviously we have to tolerate missing data.
 * <p/>
 * seems to be immutable ??
 *
 * @author caron
 * @since Jan 11, 2010
 */
@Immutable
public class FmrcInv {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FmrcInv.class);
  static private boolean debug = false;

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

  // all offsets
  private final List<Double> offsetsAll;  // all offset hours reletive to single baseTime
  private final Date baseDate;            // first runtime : offsetsAll calculated from here
  private final TimeCoord tcAll;          // timeCoord using offsetsAll

  // all forecast times
  private final List<Date> forecastTimeList;          // sorted list of Date : all forecast times

  private final Calendar cal = new GregorianCalendar(); // for date computations
  private final DateFormatter dateFormatter = new DateFormatter();

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

      // track overall list of times and offsets
      for (TimeCoord tc : fmrInv.getTimeCoords()) {
        for (double offset : tc.getOffsetHours()) {
          Date fcDate = addHour(fmrInv.getRunDate(), offset);
          forecastTimeHash.add(fcDate); // track all forecast times
          double d = getOffsetInHours(firstDate, fcDate);
          offsetHash.add(d); // track all offset hours, calculated from baseDate
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

    // create the overall list of offsets
    offsetsAll = Arrays.asList((Double[]) offsetHash.toArray(new Double[offsetHash.size()]));
    Collections.sort(offsetsAll);

    int counto = 0;
    double[] offs = new double[offsetHash.size()];
    for (double off : offsetsAll) offs[counto++] = off;
    tcAll = new TimeCoord(baseDate);
    tcAll.setOffsetHours(offs);
  }


  // public for debugging
  public List<FmrInv> getFmrList() {
    return fmrList;
  }

  private int getHour(Date d) {
    cal.setTime(d);
    return cal.get(Calendar.HOUR);
  }

  private UberGrid findVar(String varName) {
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

  public List<Date> getRunTimes() {
    return runTimeList;
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

  public TimeCoord getTimeCoordsAll() {
    return tcAll;
  }

  /**
   * Find the difference between two dates in hours
   * @param base date1
   * @param forecast date2
   * @return (forecast - base) difference in hours
   */
  public static double getOffsetInHours(Date base, Date forecast) {
    double diff = forecast.getTime() - base.getTime();
    return diff / 1000.0 / 60.0 / 60.0;
  }

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

    // the union of all offset hours, ignoring rundate
    public double[] getUnionOffsetHours() {
      return runSeq.getUnionOffsetHours();
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
        if (exp == null)
          System.out.println("HEY");
        ntimes += exp.getOffsetHours().length;
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
          log.warn(name+" Grid "+ gridName +" has different vert coords in run " + grid.getRunDate());
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
          // we are just using this routine to get the union of offset hours.
          // we discard the resulting TimeCoord and just use the offset array of doubles.
          hg.expected = TimeCoord.makeUnion(timeListExp, baseDate); // add the other coords
          for (FmrInv.GridVariable grid : hg.runs)
            grid.timeExpected = new TimeCoord(grid.getRunDate(), hg.expected.getOffsetHours());
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
    private final HashMap<Date, TimeCoord> coordMap;
    private final List<UberGrid> vars = new ArrayList<UberGrid>(); // list of UberGrid that use this
    private int id;
    private List<TimeCoord> timeList = null; // timeList has differing runDates
    private TimeCoord timeCoordUnion = null; // union of all offset hours

    RunSeq(List<FmrInv.GridVariable> runs) {
      this.coordMap = new HashMap<Date, TimeCoord>(2 * runs.size());

      // make sure every date has a slot
      for (Date d : runTimeList)
        this.coordMap.put(d, TimeCoord.EMPTY);

      // overwrite with actual coords
      for (FmrInv.GridVariable grid : runs)
        this.coordMap.put(grid.getRunDate(), grid.getTimeExpected()); // match on timeExpected
    }

    public List<TimeCoord> getTimes() {
      if (timeList == null)
        getUnionOffsetHours();
      return timeList;
    }

    public String getName() {
      return id == 0 ? "time" : "time" + id;
    }

    public int getNTimeOffsets() {
      int n = 0;
      for (TimeCoord tc : coordMap.values())
        n = Math.max(n,tc.getOffsetHours().length);
      return n;
    }

    // appears to be the union of all offset hours, ignoring rundate, so its the rectangularization of the
    // offsets for each run
    // has the side effect of constructing timeList, the list of expected TimeCoords, one for each run
    public double[] getUnionOffsetHours() {
      if (timeCoordUnion == null) { // defer creation
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
      return timeCoordUnion.getOffsetHours();
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

  ////////////////////////////////////////////////
  // debugging

  public void showRuntimeOffsetMatrix(Formatter out) {
    out.format("                                  Forecast Time Offset %n");
    out.format("     RunTime                 ");
    for (double offsetHour : offsetsAll) {
      out.format("%10.0f", offsetHour);
    }
    out.format("%n");

    int count = 0;
    for (int i = 0; i < fmrList.size(); i++) {
      FmrInv fmr = fmrList.get(i);
      Date runDate = fmr.getRunDate();
      out.format("%2d %s      ", i, dateFormatter.toDateTimeStringISO(runDate));
      for (double offsetHour : offsetsAll) {
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

  //////////////////////////////////////
  // 1D subsets

  public void showBest(Formatter out) {
    out.format("%nRun used in best dataset%n");
    out.format("                              Forecast Time Offset %n");
    out.format("Grid                         ");
    for (double offsetHour : tcAll.getOffsetHours())
      out.format("%3.0f ", offsetHour);
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
    for (double offsetHour : tcAll.getOffsetHours())
      out.format("%3.0f ", offsetHour);
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
    TimeInventory inv = new TimeInventory(tcAll, ugrid);
    for (FmrInv.GridVariable grid : ugrid.getRuns()) {
      double forecastOffset = getOffsetInHours(baseDate, grid.getRunDate());
      int runIndex = findFmrIndex(grid.getRunDate());
      TimeCoord tc = grid.getTimeCoord(); // forecast times for this run
      for (double offset : tc.getOffsetHours()) {
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
    for (double offsetHour : tcAll.getOffsetHours()) {
      out.format("%3.0f ", offsetHour);
    }
    out.format("%n");

    TimeInventory inv = new TimeInventory(tcAll, ugrid);
    for (FmrInv.GridVariable grid : ugrid.getRuns()) {
      double forecastOffset = getOffsetInHours(baseDate, grid.getRunDate());
      int runIndex = findFmrIndex(grid.getRunDate());
      TimeCoord tc = grid.getTimeCoord(); // forecast times for this run
      TimeInventory invRun = new TimeInventory(tcAll, ugrid);
      for (double offset : tc.getOffsetHours()) {
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
        out.format("%3d ", tinv.runIndex[i]);
    }
  }

  private boolean testInv(TimeInventory tinv1, TimeInventory tinv2) {
    if (tinv1.useGrid.length != tinv2.useGrid.length) return false;

    for (int i = 0; i < tinv1.useGrid.length; i++) {
      if (tinv1.runIndex[i] != tinv2.runIndex[i])
        return false;
    }
    return true;
  }

  // immutable after UberGrid.finish() is called.
  private class TimeInventory {
    final TimeCoord tc; // all the TimeCoords possible
    final FmrcInv.UberGrid ugrid; // for this grid
    final FmrInv.GridVariable[] useGrid; // use this run and grid for this offset
    final int[] runIndex; // the index of the run in the fmrList. ie findFmrIndex()

    TimeInventory(TimeCoord tc, FmrcInv.UberGrid ugrid) {
      this.tc = tc;
      this.ugrid = ugrid;
      this.useGrid = new FmrInv.GridVariable[tc.getOffsetHours().length];
      this.runIndex = new int[tc.getOffsetHours().length];
    }

    public void setOffset(double offsetHour, FmrInv.GridVariable grid, int runIndex) {
      int offsetIndex = tc.findIndex(offsetHour);
      if (offsetIndex < 0)
        throw new IllegalStateException("FmrSnapshot cant find hour " + offsetHour + " in " + tc);
      this.useGrid[offsetIndex] = grid;
      this.runIndex[offsetIndex] = runIndex;
    }
  }

  //////////////////////////////////////

  public static Date addHour(Date d, double hour) {
    long msecs = d.getTime();
    msecs += hour * 3600 * 1000;
    return new Date(msecs);
  }


  /////////////////////////////////////////////////
  // stuff below here is not used

  /*
  private class InventoryOld {
    Date forecastTime;
    Date runTime;
    double hourOffset;

    InventoryOld(Date runTime, Date forecastTime, double hourOffset) {
      this.runTime = runTime;
      this.hourOffset = hourOffset;
      this.forecastTime = forecastTime;
    }
  }

  // another abstraction of ForecastModelRun
  static class Run implements Comparable {
    TimeCoord tc;
    List<InventoryOld> invList; // list of Inventory
    Date runTime;

    Run(Date runTime, TimeCoord tc) {
      this.runTime = runTime;
      this.tc = tc;
      invList = new ArrayList<InventoryOld>();
    }

    public int compareTo(Object o) {
      Run other = (Run) o;
      return runTime.compareTo(other.runTime);
    }

    // Instances that have the same offsetHours are equal
    public boolean equalsData(Run orun) {
      if (invList.size() != orun.invList.size())
        return false;
      for (int i = 0; i < invList.size(); i++) {
        InventoryOld useGrid = invList.get(i);
        InventoryOld oinv = orun.invList.get(i);
        if (useGrid.hourOffset != oinv.hourOffset)
          return false;
      }
      return true;
    }

    double[] getOffsetHours() {
      if (tc != null)
        return tc.getOffsetHours();

      double[] result = new double[invList.size()];
      for (int i = 0; i < invList.size(); i++) {
        InventoryOld useGrid = invList.get(i);
        result[i] = useGrid.hourOffset;
      }
      return result;
    }

  }


  // list of all unique RunSeq objects
  private List<RunSeq> runSequences = new ArrayList<RunSeq>();

  private List<RunSeq> getRunSequences() {
    return runSequences;
  }




  private RunSeq findRunSequence(List<RunExpected> runs) {
    for (RunSeq seq : runSequences) {
      if (seq.equalsData(runs)) return seq;
    }
    RunSeq seq = new RunSeq(runs);
    runSequences.add(seq);
    return seq;
  }

  private TimeCoord findTime(TimeCoord want) {
    for (TimeCoord tc : timeCoords) {
      if (want.equalsData(tc))
        return tc;
    }
    return null;
  }

  private EnsCoord findEnsCoord(EnsCoord want) {
    for (EnsCoord ec : ensCoords) {
      if (want.equalsData(ec))
        return ec;
    }
    return null;
  }

  private VertCoord findVertCoord(VertCoord want) {
    for (VertCoord vc : vertCoords) {
      if (want.equalsData(vc))
        return vc;
    }
    return null;
  }


  private class RunExpected implements Comparable {
    Run run;                    // this has actual time coord
    GridDatasetInv.Grid grid;           //  grid containing actual vert coord
    //FmrInv.TimeCoord expected;  // expected time coord
    // FmrcDefinition.Grid expectedGrid;     // expected grid

    RunExpected(Run run, TimeCoord expected, GridDatasetInv.Grid grid) {
      this.run = run;
      //this.expected = expected;
      this.grid = grid;
      //this.expectedGrid = expectedGrid;
    }

    public int compareTo(Object o) {
      RunExpected other = (RunExpected) o;
      return run.runTime.compareTo(other.run.runTime);
    }

    int countInventory(double hourOffset) {
      //boolean hasExpected = (expected != null) && (expected.findIndex(hourOffset) >= 0);
      return grid.countInventory(hourOffset);
    }

    int countExpected(double hourOffset) {
      return grid.countTotal();

/*       if (expected != null) {
        boolean hasExpected = expected.findIndex(hourOffset) >= 0;
        return hasExpected ? expectedGrid.countVertCoords(hourOffset) : 0;
      } else {
        return grid.countTotal();
      }  *
    }

  }

  /////////////////////////

  private class TimeMatrixDataset {
    private int ntimes, nruns, noffsets;
    private short[][] countInv; // count[ntimes][nruns] = actual # Inventory missing at that time, run
    private short[][] expected; // expected[ntimes][nruns] = expected # Inventory at that time, run
    private short[][] countOffsetInv; // countOffset[nruns][noffsets] = # Inventory missing at that run, offset, summed over Variables
    private short[][] expectedOffset; // expectedOffset[nruns][noffsets] = expected # Inventory at that run, offset

    private int[] countTotalRunInv; // countTotalRun[nruns] = actual # Inventory missing at that run
    private int[] expectedTotalRun; // expectedTotalRun[nruns] = expected # Inventory at that run

    TimeMatrixDataset() {
      nruns = runTimeList.size();
      ntimes = forecastTimeList.size();
      noffsets = offsets.size();

      countInv = new short[ntimes][nruns];
      expected = new short[ntimes][nruns];
      countOffsetInv = new short[nruns][noffsets];
      expectedOffset = new short[nruns][noffsets];

      for (UberGrid uv : uberGridList) {
        addInventory(uv);
      }

      // sum each run
      countTotalRunInv = new int[nruns];
      expectedTotalRun = new int[nruns];
      for (int i = 0; i < nruns; i++) {
        for (int j = 0; j < noffsets; j++) {
          countTotalRunInv[i] += countOffsetInv[i][j];
          expectedTotalRun[i] += expectedOffset[i][j];
        }
      }
    }

    // after makeArrays, this gets called for each uv
    void addInventory(UberGrid uv) {
      uv.countInv = 0;
      uv.countExpected = 0;

      for (int runIndex = 0; runIndex < runTimeList.size(); runIndex++) {
        Date runTime = runTimeList.get(runIndex);
        RunExpected rune = uv.findRun(runTime);
        if (rune == null)
          continue;

        for (int offsetIndex = 0; offsetIndex < offsets.size(); offsetIndex++) {
          double hourOffset = offsets.get(offsetIndex);
          int invCount = rune.countInventory(hourOffset);
          int expectedCount = rune.countExpected(hourOffset);

          Date forecastTime = addHour(runTime, hourOffset);
          int forecastIndex = findForecastIndex(forecastTime);
          if (forecastIndex < 0) {
            log.debug("No Forecast for runTime=" + dateFormatter.toDateTimeString(runTime) + " OffsetHour=" + hourOffset +
                " dataset=" + name);
          } else {
            countInv[forecastIndex][runIndex] += invCount;
            expected[forecastIndex][runIndex] += expectedCount;
          }

          countOffsetInv[runIndex][offsetIndex] += invCount;
          expectedOffset[runIndex][offsetIndex] += expectedCount;

          uv.countInv += invCount;
          uv.countExpected += expectedCount;
        }
      }

      /* for (int i = 0; i < uv.runs.size(); i++) {
        RunExpected rune = (RunExpected) uv.runs.get(i);
        int runIndex = findRunIndex(rune.run.runTime);


        // missing inventory
        for (int j = 0; j < rune.run.invList.size(); j++) {
          Inventory useGrid = (Inventory) rune.run.invList.get(j);
          int missing = rune.grid.countMissing(useGrid.hourOffset);
          int forecastIndex = findForecastIndex(useGrid.forecastTime);
          int offsetIndex = findOffsetIndex(useGrid.hourOffset);

          countMissing[forecastIndex][runIndex] += missing;
          countOffsetMissing[runIndex][offsetIndex] += missing;
        }

        // the expected inventory
        if (rune.expected != null) {
          double[] offsets = rune.expected.getOffsetHours();
          for (int j = 0; j < offsets.length; j++) {
            Date fcDate = addHour(rune.run.runTime, offsets[j]);
            int forecastIndex = findForecastIndex(fcDate);
            int offsetIndex = findOffsetIndex(offsets[j]);

            expected[forecastIndex][runIndex]++;
            expectedOffset[runIndex][offsetIndex]++;
            //uv.totalExpectedGrids++;
          }
        }

      } *
    }

    int findRunIndex(Date runTime) {
      for (int i = 0; i < runTimeList.size(); i++) {
        Date d = runTimeList.get(i);
        if (d.equals(runTime))
          return i;
      }
      return -1;
    }

    int findForecastIndex(Date forecastTime) {
      for (int i = 0; i < forecastTimeList.size(); i++) {
        Date d = forecastTimeList.get(i);
        if (d.equals(forecastTime))
          return i;
      }
      return -1;
    }

    int findOffsetIndex(double offsetHour) {
      for (int i = 0; i < offsets.size(); i++) {
        if (offsetHour == offsets.get(i))
          return i;
      }
      return -1;
    }

  }

  private Date addHour(Date d, double hour) {
    cal.setTime(d);

    int ihour = (int) hour;
    int imin = (int) (hour - ihour) * 60;
    cal.add(Calendar.HOUR_OF_DAY, ihour);
    cal.add(Calendar.MINUTE, imin);
    return cal.getTime();
  }

  private double getOffsetHour(Date run, Date forecast) {
    double diff = forecast.getTime() - run.getTime();
    return diff / 1000.0 / 60.0 / 60.0;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////

  public String writeMatrixXML(String varName) {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    if (varName == null) {
      return fmt.outputString(makeMatrixDocument());
    } else {
      return fmt.outputString(makeMatrixDocument(varName));
    }
  }

  public void writeMatrixXML(String varName, OutputStream os) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    if (varName == null) {
      fmt.output(makeMatrixDocument(), os);
    } else {
      fmt.output(makeMatrixDocument(varName), os);
    }
  }

  private TimeMatrixDataset tmAll;


  /**
   * Create an XML document for the entire collection
   *
  public Document makeMatrixDocument() {
    if (tmAll == null)
      tmAll = new TimeMatrixDataset();

    Element rootElem = new Element("forecastModelRunCollectionInventory");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("dataset", name);

    // list all the offset hours
    for (Double offset : offsets) {
      Element offsetElem = new Element("offsetTime");
      rootElem.addContent(offsetElem);
      offsetElem.setAttribute("hours", offset.toString());
    }

    // list all the variables
    for (UberGrid uv : uberGridList) {
      Element varElem = new Element("variable");
      rootElem.addContent(varElem);
      varElem.setAttribute("name", uv.name);

      addCountPercent(uv.countInv, uv.countExpected, varElem, false);
    }

    // list all the runs
    for (int i = runTimeList.size() - 1; i >= 0; i--) {
      Element runElem = new Element("run");
      rootElem.addContent(runElem);
      Date runTime = runTimeList.get(i);
      runElem.setAttribute("date", dateFormatter.toDateTimeStringISO(runTime));

      addCountPercent(tmAll.countTotalRunInv[i], tmAll.expectedTotalRun[i], runElem, true);

      for (int k = 0; k < offsets.size(); k++) {
        Element offsetElem = new Element("offset");
        runElem.addContent(offsetElem);
        Double offset = offsets.get(k);
        offsetElem.setAttribute("hours", offset.toString());

        addCountPercent(tmAll.countOffsetInv[i][k], tmAll.expectedOffset[i][k], offsetElem, false);
      }
    }

    // list all the forecasts
    for (int k = forecastTimeList.size() - 1; k >= 0; k--) {
      Element fcElem = new Element("forecastTime");
      rootElem.addContent(fcElem);

      Date ftime = forecastTimeList.get(k);
      fcElem.setAttribute("date", dateFormatter.toDateTimeStringISO(ftime));

      // list all the forecasts
      for (int j = runTimeList.size() - 1; j >= 0; j--) {
        Element rtElem = new Element("runTime");
        fcElem.addContent(rtElem);

        addCountPercent(tmAll.countInv[k][j], tmAll.expected[k][j], rtElem, false);
      }
    }

    return doc;
  }

  private void addCountPercent(int have, int want, Element elem, boolean always) {
    if (((have == want) || (want == 0)) && (have != 0)) {
      elem.setAttribute("count", Integer.toString(have));
      if (always) elem.setAttribute("percent", "100");
    } else if (want != 0) {
      int percent = (int) (100.0 * have / want);
      elem.setAttribute("count", have + "/" + want);
      elem.setAttribute("percent", Integer.toString(percent));
    }
  }

  /**
   * Create an XML document for a variable
   *
  public Document makeMatrixDocument(String varName) {
    if (tmAll == null)
      tmAll = new TimeMatrixDataset();

    UberGrid uv = findVar(varName);
    if (uv == null)
      throw new IllegalArgumentException("No variable named = " + varName);

    Element rootElem = new Element("forecastModelRunCollectionInventory");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("dataset", name);
    rootElem.setAttribute("variable", uv.name);

    // list all the offset hours
    for (int k = 0; k < offsets.size(); k++) {
      Element offsetElem = new Element("offsetTime");
      rootElem.addContent(offsetElem);
      Double offset = offsets.get(k);
      offsetElem.setAttribute("hour", offset.toString());
    }

    // list all the runs
    for (int i = runTimeList.size() - 1; i >= 0; i--) {
      Element runElem = new Element("run");
      rootElem.addContent(runElem);
      Date runTime = runTimeList.get(i);
      runElem.setAttribute("date", dateFormatter.toDateTimeStringISO(runTime));

      RunExpected rune = uv.findRun(runTime);

      for (Double offset : offsets) {
        Element offsetElem = new Element("offset");
        runElem.addContent(offsetElem);
        double hourOffset = offset.doubleValue();
        offsetElem.setAttribute("hour", offset.toString());

        int missing = rune.countInventory(hourOffset);
        int expected = rune.countExpected(hourOffset);
        addCountPercent(missing, expected, offsetElem, false);
      }
    }

    // list all the forecasts
    for (int k = forecastTimeList.size() - 1; k >= 0; k--) {
      Element fcElem = new Element("forecastTime");
      rootElem.addContent(fcElem);

      Date forecastTime = forecastTimeList.get(k);
      fcElem.setAttribute("date", dateFormatter.toDateTimeStringISO(forecastTime));

      // list all the forecasts
      for (int j = runTimeList.size() - 1; j >= 0; j--) {
        Element rtElem = new Element("runTime");
        fcElem.addContent(rtElem);

        Date runTime = runTimeList.get(j);

        RunExpected rune = uv.findRun(runTime);
        double hourOffset = getOffsetHour(runTime, forecastTime);
        int missing = rune.countInventory(hourOffset);
        int expected = rune.countExpected(hourOffset);
        addCountPercent(missing, expected, rtElem, false);
      }
    }

    return doc;
  }

  public String showOffsetHour(String varName, String offsetHour) {
    UberGrid uv = findVar(varName);
    if (uv == null)
      return "No variable named = " + varName;

    double hour = Double.parseDouble(offsetHour);

    StringBuilder sbuff = new StringBuilder();
    sbuff.append("Inventory for ").append(varName).append(" for offset hour= ").append(offsetHour).append("\n");

    for (FmrInv.GridVariable run : uv.runs) {
      double[] vcoords = run.vertCoordUnion.getValues1(); // acutllay was based on hour
      sbuff.append(" Run ");
      sbuff.append(dateFormatter.toDateTimeString(run.getRunDate()));
      sbuff.append(": ");
      for (int j = 0; j < vcoords.length; j++) {
        if (j > 0) sbuff.append(",");
        sbuff.append(vcoords[j]);
      }
      sbuff.append("\n");
    }

    /* sbuff.append("\nExpected for ").append(varName).append(" for offset hour= ").append(offsetHour).append("\n");

    for (RunExpected rune : uv.runs) {
      double[] vcoords = rune.expectedGrid.getVertCoords(hour);
      sbuff.append(" Run ");
      sbuff.append(formatter.toDateTimeString(rune.run.runTime));
      sbuff.append(": ");
      for (int j = 0; j < vcoords.length; j++) {
        if (j > 0) sbuff.append(",");
        sbuff.append(vcoords[j]);
      }
      sbuff.append("\n");
    } *
    return sbuff.toString();
  } */
}


/* private Inventory findByOffset(List invList, double offset) {
 for (int i = 0; i < invList.size(); i++) {
   Inventory useGrid = (Inventory) invList.get(i);
   if (useGrid.hourOffset == offset) return useGrid;
 }
 return null;
}

private Inventory findByForecast(List invList, Date forecast) {
 for (int i = 0; i < invList.size(); i++) {
   Inventory useGrid = (Inventory) invList.get(i);
   if (useGrid.forecastTime.equals(forecast)) return useGrid;
 }
 return null;
} */

