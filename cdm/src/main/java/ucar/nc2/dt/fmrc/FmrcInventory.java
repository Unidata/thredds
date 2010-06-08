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
package ucar.nc2.dt.fmrc;

import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.util.StringUtil;

import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;
import thredds.catalog.crawl.CatalogCrawler;
import thredds.catalog.InvDataset;
import thredds.catalog.InvAccess;
import thredds.catalog.ServiceType;
import thredds.catalog.InvCatalogRef;

/**
 * A collection of ForecastModelRun (aka "run").
 * The Set if {Run, TimeCoord, Grid} are grouped into "run Sequences" {{Run, TimeCoord} X {Grid}}
 *
 * The FmrcDefinition object defines what is to be expected.
 * The TimeMatrixDataset object keeps an inventory for all variables for the ForecastModelRunCollection.
 *
 * The set of possible valid times vs run times is thought of as a 2D time matrix.
 *
 * All this rigamorole is because NCEP grid files are so irregular.
 *
 * <pre>
 * Data Structures
 *
 *  List RunTime Date
 *  List ForecastTime Date
 *  List Offsets Double
 *
 *  List VertTimeCoord
 *    double[] values
 *
 *  List TimeCoord
 *    double[] offsetHour
 *
 *  List RunSeq    // sequence of runs; ie sequence of TimeCoords; ie actual time coord
 *    List Run run;
 *      Date runDate
 *      TimeCoord
 *
 *    List UberGrid
 *     String name
 *     List RunExpected  // corresponds to the runs in the RunSeq, matches to expected inventory
 *       Run run;                              //  actual time coord
 *       ForecastModelRun.Grid grid;           //  contains actual vert coord for this Run
 *       ForecastModelRun.TimeCoord expected;  // expected time coord
 *       FmrcDefinition.Grid expectedGrid;     // expected grid, vertCoord
 *
 * </pre>
 *
 * @author caron
 */
public class FmrcInventory {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FmrcInventory.class);

  private static SimpleDateFormat dateFormatShort = new java.text.SimpleDateFormat("MM-dd HH.mm");
  private static SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH.mm'Z'");
  private static boolean debug = false;

  static {
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT")); // same as UTC
    dateFormatShort.setTimeZone(java.util.TimeZone.getTimeZone("GMT")); // same as UTC
  }

  private String name; // name of ForecastModelRunCollection

  // list of unique ForecastModelRunInventory.TimeCoord
  private int tc_seqno = 0;
  private List<ForecastModelRunInventory.TimeCoord> timeCoords = new ArrayList<ForecastModelRunInventory.TimeCoord>();

  // list of unique ForecastModelRunInventory.EnsCoord
  private int ec_seqno = 0;
  private List<ForecastModelRunInventory.EnsCoord> ensCoords = new ArrayList<ForecastModelRunInventory.EnsCoord>();

  // list of unique ForecastModelRunInventory.VertCoord
  private int vc_seqno = 0;
  private List<ForecastModelRunInventory.VertCoord> vertCoords = new ArrayList<ForecastModelRunInventory.VertCoord>();

  // list of all unique RunSeq objects
  private int run_seqno = 0;
  private List<RunSeq> runSequences = new ArrayList<RunSeq>();

  // the variables
  private Map<String,UberGrid> uvHash = new HashMap<String,UberGrid>(); // hash of UberGrid
  private List<UberGrid> varList;              // sorted list of UberGrid

  // all run times
  private Set<Date> runTimeHash = new HashSet<Date>();
  private List<Date> runTimeList;                // sorted list of Date : all run times

  // all offsets
  private Set<Double> offsetHash = new HashSet<Double>();
  private List<Double> offsetList;                // sorted list of Double : all offset hours

  // all forecast times
  private Set<Date> forecastTimeHash = new HashSet<Date>();
  private List<Date> forecastTimeList;          // sorted list of Date : all forecast times

  // optional definition, describes what is expected
  private String fmrcDefinitionDir;
  private FmrcDefinition definition = null;

  private TimeMatrixDataset tmAll;
  private Calendar cal = new GregorianCalendar(); // for date computations
  private DateFormatter formatter = new  DateFormatter();


  /**
   * Open a collection of ForecastModelRun. An optional definition file may exist.
   * Call addRun() to add the ForecastModelRuns.
   *
   * @param fmrcDefinitionDir put optional definition file in this directory
   * @param name name for the collection, the definition file = name + ".fmrcDefinition.xml";
   * @throws IOException on io error
   */
  FmrcInventory(String fmrcDefinitionDir, String name) throws IOException {
    this.fmrcDefinitionDir = fmrcDefinitionDir;
    int pos = name.indexOf(".fmrcDefinition.xml");
    if (pos > 0)
      this.name = name.substring(0,pos);
    else
      this.name = name;

    // see if theres a definition file
    FmrcDefinition fmrc = new FmrcDefinition();
    if (fmrc.readDefinitionXML( getDefinitionPath()))
      definition = fmrc;
    else
      log.warn("FmrcCollection has no Definition "+getDefinitionPath());

    cal.setTimeZone( TimeZone.getTimeZone("UTC"));
  }

  public String getName() { return name; }
  public String getDefinitionPath() {
    if (fmrcDefinitionDir != null) {
      return fmrcDefinitionDir + name + ".fmrcDefinition.xml";
    }
    return "./" + name + ".fmrcDefinition.xml";
  }
  public List<ForecastModelRunInventory.TimeCoord> getTimeCoords() { return timeCoords; }
  public List<RunSeq> getRunSequences() { return runSequences; }
  public List<ForecastModelRunInventory.EnsCoord> getEnsCoords() { return ensCoords; }
  public List<ForecastModelRunInventory.VertCoord> getVertCoords() { return vertCoords; }

  public String getSuffixFilter() {
    return (definition == null) ? null  : definition.getSuffixFilter();
  }

  /* public void report( PrintStream out, boolean showMising ) {
    if (definition != null)
      definition.report( this, out, showMising);
    else
      out.println("No definition found");
  } */

  public FmrcDefinition getDefinition() { return definition; }

  /**
   * Add a ForecastModelRun to the collection.
   * @param fmr add this ForecastModelRun to the collection
   */
  void addRun(ForecastModelRunInventory fmr) {
    if (debug) System.out.println(" Adding ForecastModelRun "+fmr.getRunDateString());

    // track all run times
    runTimeHash.add( fmr.getRunDate());

    // add each time coord, variable
    for (ForecastModelRunInventory.TimeCoord tc : fmr.getTimeCoords()) {
      // Construct list of unique TimeCoords. Reset their id so they are unique.
      // Note that after this, we ignore the nested variables.
      ForecastModelRunInventory.TimeCoord tcUse = findTime(tc);
      if (tcUse == null) {
        this.timeCoords.add(tc);
        tcUse = tc;
        tc.setId(Integer.toString(tc_seqno));
        tc_seqno++;
      }

      // create a Run object, encapsolating this ForecastModelRun
      Run run = new Run(fmr.getRunDate(), tcUse);
      for (double offset : tcUse.getOffsetHours()) {
        Date fcDate = addHour(fmr.getRunDate(), offset);
        Inventory inv = new Inventory(fmr.getRunDate(), fcDate, offset);
        run.invList.add(inv);
        forecastTimeHash.add(fcDate); // track all forecast times
        offsetHash.add(offset); // track all offset hours
      }

      // Construct list of Variables across all runs in the collection.
      for (ForecastModelRunInventory.Grid grid :  tc.getGrids()) {
        UberGrid uv = uvHash.get(grid.name);
        if (uv == null) {
          // we may not have this variable in the definition
          if ((definition != null) && (null == definition.findSeqForVariable(grid.name))) {
            log.warn("FmrcCollection Definition " + name + " does not contain variable " + grid.name);
            continue; // skip it
          } else {
            uv = new UberGrid(grid.name);
            uvHash.put(grid.name, uv);
          }
        }
        uv.addRun(run, grid);
      }
    }
  }

  // call after adding all runs
  void finish() {
    // create the overall list of variables
    varList = new ArrayList<UberGrid>(uvHash.values());
    Collections.sort(varList);

    // create the overall list of run times
    runTimeList = Arrays.asList((Date[]) runTimeHash.toArray( new Date[ runTimeHash.size()]));
    Collections.sort(runTimeList);

    // create the overall list of forecast times
    forecastTimeList = Arrays.asList((Date[]) forecastTimeHash.toArray( new Date[ forecastTimeHash.size()]));
    Collections.sort(forecastTimeList);

    // create the overall list of offsets
    offsetList = Arrays.asList((Double[]) offsetHash.toArray( new Double[ offsetHash.size()]));
    Collections.sort(offsetList);

    // finish the variables, assign to a RunSeq
    for (UberGrid uv : varList) {
      uv.finish();
      uv.seq = findRunSequence(uv.runs); // assign to a unique RunSeq
      uv.seq.addVariable(uv); // add to list of vars for that RunSeq
    }
  }

  private UberGrid findVar(String varName) {
    for (UberGrid uv : varList) {
      if (uv.name.equals(varName))
        return uv;
    }
    return null;
  }

  /* public void report(PrintStream out) {

    // show all the forecast time coordinates
    for (int i = 0; i < times.size(); i++) {
      ForecastModelRun.TimeCoord tc = (ForecastModelRun.TimeCoord) times.get(i);
      out.print("TC"+tc.getId() +" =[");
      double[] offsets = tc.getOffsetHours();
      for (int j = 0; j < offsets.length; j++) {
        if (j > 0) out.print(",");
        out.print(offsets[j]);
      }
      out.println("]");
    }

    TimeSeq tseq = null;
    for (int i = 0; i < varList.size(); i++) {
      UberGrid uv = (UberGrid) varList.get(i);
      uv.tm.finish();

      if (tseq != uv.seq) {
        tseq = uv.seq;
        out.print("\n" + tseq.name + "= [");
        for (int j = 0; j < tseq.seq.size(); j++) {
          ForecastModelRun.TimeCoord tc = (ForecastModelRun.TimeCoord) tseq.seq.get(j);
          if (j > 0) out.print(",");
          if (tc == null)
            out.print("null");
          else
            out.print(tc.getId());
        }
        out.println("]");
      }
      out.println("  "+uv.name);

      if (false) {
        out.println("matrix for " + uv.name + ":");

        List rtimes = uv.tm.getRunTimes();
        List ftimes = uv.tm.getForecastTimes();
        for (int k = ftimes.size() - 1; k >= 0; k--) {
          Date ftime = (Date) ftimes.get(k);
          out.print(" " + DateUnit.getStandardDateString2(ftime) + ": ");

          for (int j = rtimes.size() - 1; j >= 0; j--) {
            Date rtime = (Date) rtimes.get(j);
            if (uv.tm.isPresent(rtime, ftime))
              out.print("X");
            else
              out.print(" ");
          }
          out.println();
        }
        out.println();

        out.println();
      }
    }
  } */


  /////////////////////////////////////////////////

  private class Inventory {
    Date forecastTime;
    Date runTime;
    double hourOffset;

    Inventory(Date runTime, Date forecastTime, double hourOffset) {
      this.runTime = runTime;
      this.hourOffset = hourOffset;
      this.forecastTime = forecastTime;
    }
  }

  // another abstraction of ForecastModelRun
  static class Run implements Comparable {
    ForecastModelRunInventory.TimeCoord tc;
    List<Inventory> invList; // list of Inventory
    Date runTime;

    Run(Date runTime, ForecastModelRunInventory.TimeCoord tc) {
      this.runTime = runTime;
      this.tc = tc;
      invList = new ArrayList<Inventory>();
    }

    public int compareTo(Object o) {
      Run other = (Run) o;
      return runTime.compareTo(other.runTime);
    }

        // Instances that have the same offsetHours are equal
    public boolean equalsData(Run orun) {
      if (invList.size() != orun.invList.size())
        return false;
      for (int i = 0; i < invList.size() ; i++) {
        Inventory inv = invList.get(i);
        Inventory oinv = orun.invList.get(i);
        if (inv.hourOffset != oinv.hourOffset)
          return false;
      }
      return true;
    }

    double[] getOffsetHours() {
      if (tc != null)
        return tc.getOffsetHours();

      double[] result = new double[ invList.size()];
      for (int i = 0; i < invList.size(); i++) {
        Inventory inv = invList.get(i);
        result[i] = inv.hourOffset;
      }
      return result;
    }

  }

  /** Represents a sequence of Run, each run has a particular TimeCoord.
   * keep track of all the Variables with the same RunSeq */
  class RunSeq {
    List<Run> runs; // list of Run
    List<UberGrid> vars = new ArrayList<UberGrid>(); // list of UberGrid
    String name;

    RunSeq(List<RunExpected> runs) {
      this.runs = new ArrayList<Run>();
      for (RunExpected rune : runs) {
        this.runs.add(rune.run);
      }
      name = "RunSeq" + run_seqno;
      run_seqno++;
    }

    /**
     * Decide if this RunSeq is equivilent to the list of Runs.
     * @param oruns list of RunExpected
     * @return true if it has an equivilent set of runs.
     */
    boolean equalsData(List<RunExpected> oruns) {
      if (runs.size() != oruns.size()) return false;
      for (int i = 0; i < runs.size(); i++) {
        Run run = runs.get(i);
        RunExpected orune = oruns.get(i);
        if (!run.runTime.equals(orune.run.runTime)) return false;
        if (!run.equalsData(orune.run)) return false;
      }
      return true;
    }

    // keep track of all the Variables with the this RunSeq
    void addVariable( UberGrid uv) { vars.add( uv); }

    List<UberGrid> getVariables() {
      return vars;
    }
  }

  private RunSeq findRunSequence(List<RunExpected> runs) {
    for (RunSeq seq : runSequences) {
      if (seq.equalsData(runs)) return seq;
    }
    RunSeq seq = new RunSeq(runs);
    runSequences.add(seq);
    return seq;
  }

  private ForecastModelRunInventory.TimeCoord findTime(ForecastModelRunInventory.TimeCoord want) {
    for (ForecastModelRunInventory.TimeCoord tc : timeCoords) {
      if (want.equalsData(tc))
        return tc;
    }
    return null;
  }

  private ForecastModelRunInventory.EnsCoord findEnsCoord(ForecastModelRunInventory.EnsCoord want) {
    for (ForecastModelRunInventory.EnsCoord ec : ensCoords) {
      if (want.equalsData(ec))
        return ec;
    }
    return null;
  }

  private ForecastModelRunInventory.VertCoord findVertCoord(ForecastModelRunInventory.VertCoord want) {
    for (ForecastModelRunInventory.VertCoord vc : vertCoords) {
      if (want.equalsData(vc))
        return vc;
    }
    return null;
  }

  ////////////////////////////////////////////////////////////////////

  // The collection across runs of one variable
  class UberGrid implements Comparable {
    String name;
    List<RunExpected> runs = new ArrayList<RunExpected>();  // List of RunExpected
    ForecastModelRunInventory.VertCoord vertCoordUnion = null;
    ForecastModelRunInventory.EnsCoord ensCoordUnion = null;
    int countInv, countExpected;

    RunSeq seq;                        // which seq this belongs to
    FmrcDefinition.RunSeq expectedSeq; // expected sequence
    FmrcDefinition.Grid expectedGrid; // expected vert coordinate (optional)

    UberGrid(String name) {
      this.name = name;
      if (definition != null) {
        expectedSeq = definition.findSeqForVariable( name);
        expectedGrid = expectedSeq.findGrid( name);
      }
    }

    String getName() { return name; }

    void addRun(Run run, ForecastModelRunInventory.Grid grid) {
      ForecastModelRunInventory.TimeCoord xtc = (expectedSeq == null) ? null : expectedSeq.findTimeCoordByRuntime( run.runTime);
      RunExpected rune = new RunExpected( run, xtc, grid, expectedGrid);
      runs.add( rune);

      // now we can generate the list of possible forecast hours
      if (rune.expected != null) {
        for (double offset : rune.expected.getOffsetHours()) {
          Date fcDate = addHour(run.runTime, offset);
          forecastTimeHash.add(fcDate); // track all forecast times
          offsetHash.add(offset); // track all offset hours
        }
      }
    }

    void finish() {
      Collections.sort( runs);

      // run over all ensCoords and construct the union
      List<ForecastModelRunInventory.EnsCoord> eextendList = new ArrayList<ForecastModelRunInventory.EnsCoord>();
      ForecastModelRunInventory.EnsCoord ec_union = null;
      for (RunExpected rune : runs) {
        ForecastModelRunInventory.EnsCoord ec = rune.grid.ec;
        if (ec == null) continue;
        if (ec_union == null)
          ec_union = new ForecastModelRunInventory.EnsCoord( ec );
        else if (!ec_union.equalsData(ec))
          eextendList.add(ec);
      }

      if (ec_union != null) {
        normalize( ec_union, eextendList);  

        // now find unique within collection
        ensCoordUnion = findEnsCoord( ec_union);
        if (ensCoordUnion == null) {
          ensCoords.add(ec_union);
          ensCoordUnion = ec_union;
          ec_union.setId(Integer.toString(ec_seqno));
          ec_seqno++;
        }
      }

      // run over all vertCoords and construct the union
      List<ForecastModelRunInventory.VertCoord> extendList = new ArrayList<ForecastModelRunInventory.VertCoord>();
      ForecastModelRunInventory.VertCoord vc_union = null;
      for (RunExpected rune : runs) {
        ForecastModelRunInventory.VertCoord vc = rune.grid.vc;
        if (vc == null) continue;
        if (vc_union == null)
          vc_union = new ForecastModelRunInventory.VertCoord(vc);
        else if (!vc_union.equalsData(vc))
          extendList.add(vc);
      }

      if (vc_union != null) {
        normalize( vc_union, extendList);

        // now find unique within collection
        vertCoordUnion = findVertCoord( vc_union);
        if (vertCoordUnion == null) {
          vertCoords.add(vc_union);
          vertCoordUnion = vc_union;
          vc_union.setId(Integer.toString(vc_seqno));
          vc_seqno++;
        }
      }

    }

    /**
     * Extend with all the values in the list of EnsCoord
     *  .
     * @param ecList list of EnsCoord, may be empty
     */
    public void normalize(ForecastModelRunInventory.EnsCoord result,
                          List<ForecastModelRunInventory.EnsCoord> ecList) {
      List<ForecastModelRunInventory.EnsCoord> extra = new ArrayList<ForecastModelRunInventory.EnsCoord>();
      for (ForecastModelRunInventory.EnsCoord ec : ecList) {
        if ( ! result.equalsData( ec ) ) {
          // differences can only be greater
          extra.add( ec );
        }
      }
      if( extra.size() == 0 )
        return;
       for (ForecastModelRunInventory.EnsCoord ec : extra ) {
         if ( ec.getNEnsembles() < result.getNEnsembles() )
           continue;
         result = ec;
       }

    }

    /**
     * Extend with all the values in the list of VertCoord
     * Sort the values and recreate the double[] values array.
     * @param vcList list of VertCoord, may be empty
     */
    public void normalize(ForecastModelRunInventory.VertCoord result, List<ForecastModelRunInventory.VertCoord> vcList) {
      // get all values into a HashSet of LevelCoord
      Set<LevelCoord> valueSet = new HashSet<LevelCoord>();
      addValues( valueSet, result.getValues1(), result.getValues2());
      for (ForecastModelRunInventory.VertCoord vc : vcList) {
        addValues(valueSet, vc.getValues1(), vc.getValues2());
      }

      // now create a sorted list, transfer to values array
      List<LevelCoord> valueList = Arrays.asList( (LevelCoord[]) valueSet.toArray( new LevelCoord[ valueSet.size()]));
      Collections.sort( valueList);
      double[] values1 = new double[valueList.size()];
      double[] values2 = new double[valueList.size()];
      boolean has_values2 = false;
      for (int i = 0; i < valueList.size(); i++) {
        LevelCoord lc = valueList.get(i);
        values1[i] = lc.value1;
        values2[i] = lc.value2;
        if (lc.value2 != 0.0)
          has_values2 = true;
      }
      result.setValues1(values1);
      if (has_values2)
        result.setValues2(values2);
    }

    private void addValues(Set<LevelCoord> valueSet, double[] values1, double[] values2) {
      for (int i = 0; i < values1.length; i++) {
        double val2 = (values2 == null) ? 0.0 : values2[i];
        valueSet.add( new LevelCoord(values1[i], val2));
      }
    }

    public int compareTo(Object o) {
      UberGrid uv = (UberGrid) o;
      return name.compareTo( uv.name);
    }

    RunExpected findRun(Date runTime) {
      for (RunExpected rune : runs) {
        if (runTime.equals(rune.run.runTime))
          return rune;
      }
      return null;
    }

    /* ForecastModelRun.TimeCoord findExpectedTimeCoord( Date runTime) {
      if (expectedSeq == null)
        return null;
      return expectedSeq.findTimeCoordByRuntime( runTime);
    }

    /* String showInventory(Date runTime, Date forecastTime) {

      if ((rune != null) && (null != findByForecast(rune.run.invList, forecastTime))) {
        return "X";

      } else if ((rune != null) && (rune.expected != null)) {
        double offset = getOffsetHour(runTime, forecastTime);
        if (rune.expected.findIndex(offset) >= 0) {
           return "O";
        }
      }

      return null;
    }  */

  } // end UberGrid

  class  LevelCoord implements Comparable {
    double mid;
    double value1, value2;
    LevelCoord( double value1, double value2) {
      this.value1 = value1;
      this.value2 = value2;
      mid = (value2 == 0) ? value1 : (value1 + value2)/2;
    }

    public int compareTo(Object o) {
      LevelCoord other = (LevelCoord) o;
      //if (closeEnough(value1, other.value1) && closeEnough(value2, other.value2)) return 0;
      if (mid < other.mid) return -1;
      if (mid > other.mid) return 1;
      return 0;
    }

    public boolean equals(Object oo) {
      if (this == oo) return true;
      if ( !(oo instanceof LevelCoord)) return false;
      LevelCoord other = (LevelCoord) oo;
      return (ucar.nc2.util.Misc.closeEnough(value1, other.value1) && ucar.nc2.util.Misc.closeEnough(value2, other.value2));
    }

    public int hashCode() {
      return (int) (value1 * 100000 + value2 * 100);
    }
  }

  class RunExpected implements Comparable {
    Run run;                              // this has actual time coord
    ForecastModelRunInventory.Grid grid;           //  grid containing actual vert coord
    ForecastModelRunInventory.TimeCoord expected;  // expected time coord
    FmrcDefinition.Grid expectedGrid;     // expected grid

     RunExpected(Run run, ForecastModelRunInventory.TimeCoord expected, ForecastModelRunInventory.Grid grid, FmrcDefinition.Grid expectedGrid) {
       this.run = run;
       this.expected = expected;
       this.grid = grid;
       this.expectedGrid = expectedGrid;
     }

    public int compareTo(Object o) {
      RunExpected other = (RunExpected) o;
      return run.runTime.compareTo(other.run.runTime);
    }

    int countInventory( double hourOffset) {
      boolean hasExpected = (expected != null) && (expected.findIndex(hourOffset) >= 0);
      return hasExpected ? grid.countInventory(hourOffset) : 0;
    }

    int countExpected( double hourOffset) {
      if (expected != null) {
        boolean hasExpected = expected.findIndex(hourOffset) >= 0;
        return hasExpected ? expectedGrid.countVertCoords(hourOffset) : 0;
      } else {
        return grid.countTotal();
      }
    }

  }


  /////////////////////////

  /* inventory for one variable
  private class TimeMatrix implements ForecastModelRunCollectionSave.TimeMatrix {
    private String varName;

    private ArrayList forecastTimes = new ArrayList(); // list Forecast
    private ArrayList runTimes = new ArrayList(); // list of Run
    // private int ntimes, nruns;
    private byte[][] present; // present[ntimes][nruns] = 1 if Inventory present
    private byte[][] expect; // expect[ntimes][nruns] = 1 if Inventory expected
    private HashSet offsetSet = new HashSet(); // hash Double
    private List offsetHours = new ArrayList(); // list Double
    private int total = 0; // total # Inventory

    TimeMatrix (String varName) {
      this.varName = varName;
    }

    public List getRunTimes() {
      ArrayList result = new ArrayList(runTimes.size());
      for (int i = 0; i < runTimes.size(); i++) {
        Run run = (Run) runTimes.get(i);
        result.add(run.runTime);
      }
      return result;
    }

    public List getForecastTimes() {
      ArrayList result = new ArrayList(forecastTimes.size());
      for (int i = 0; i < forecastTimes.size(); i++) {
        Forecast fc = (Forecast) forecastTimes.get(i);
        result.add(fc.forecastTime);
      }
      return result;
    }

    public boolean isPresent(Date runTime, Date forecastTime) {
      int time = findForecastIndex(forecastTime);
      int run = findRunIndex(runTime);
      if ((time < 0) || (run < 0))
        return false;
      return present[time][run] != 0;
    }

    public String showInventory(Date runTime, Date forecastTime) {
      int time = findForecastIndex(forecastTime);
      int run = findRunIndex(runTime);
      if ((time < 0) || (run < 0))
        return null;
      if (present[time][run] != 0)
        return "X";
      if (expect[time][run] != 0)
        return "O";
      return null;
    }

    public int getTotalNumberGrids() {
      return total;
    }

    void addRun(Date runDate, double[] offset) {
      Run run = new Run(runDate, null);
      runTimes.add(run);

      DateUnit du;
      try {
        du = new DateUnit("hours since "+DateUnit.getStandardDateString(runDate));
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }

      for (int i = 0; i < offset.length; i++) {
        Date forecastDate = du.makeDate( offset[i]);
        Inventory inv = new Inventory(runDate, forecastDate, offset[i]);
        offsetSet.add(new Double(inv.hourOffset));

        run.invList.add(inv);
        int idx = findForecastIndex(forecastDate);
        Forecast fc;
        if (idx < 0) {
          fc = new Forecast(forecastDate);
          forecastTimes.add(fc);
        } else
          fc = (Forecast) forecastTimes.get(idx);

        fc.invList.add(inv);
      }
    }

    void finish() {
      present = new byte[ntimes][nruns];

      // make sure everything is sorted
      Collections.sort(forecastTimes);
      Collections.sort(runTimes);

      InventorySortByRunTime runTimeSorter = new InventorySortByRunTime();
      InventorySortByForecastTime forecastTimeSorter = new InventorySortByForecastTime();
      for (int i = 0; i < runTimes.size(); i++) {
        Run run = (Run) runTimes.get(i);
        Collections.sort(run.invList, forecastTimeSorter);
      }

      for (int i=0; i<ntimes; i++) {
        Forecast tc = (Forecast) forecastTimes.get(i);
        Collections.sort(tc.invList, runTimeSorter);

        // construct the isPresent array
        int fcIndex = findForecastIndex(tc.forecastTime);
        for (int j = 0; j < tc.invList.size(); j++) {
          Inventory inv = (Inventory) tc.invList.get(j);
          int runIndex = findRunIndex(inv.runTime);
          present[fcIndex][runIndex] = 1;
          total++;
        }
      }

      offsetHours = Arrays.asList(offsetSet.toArray());
      Collections.sort(offsetHours);

      // the expected inventory
      if (definition == null)
        return;

      expect = new byte[ntimes][nruns];

      FmrcDefinition.RunSeq seq = definition.findSeqForVariable( varName);

      for (int i = 0; i < runTimes.size(); i++) {
        Run run = (Run) runTimes.get(i);
        FmrcDefinition.OffsetHours oh = seq.findOffsetHoursByRuntime( run.runTime);
        int runIndex = findRunIndex(run.runTime);

        for (int j = 0; j < oh.offset.length; j++) {
          double offsetHour = oh.offset[j];

          Date forecastTime = addHour( run.runTime, offsetHour);
          int fcIndex = findForecastIndex(forecastTime);
          if (fcIndex >= 0) {
            expect[fcIndex][runIndex] = 1;
          } else
            System.out.println(" cat find Forecast= "+forecastTime);
        }
      }
    }

    int findRunIndex(Date runTime) {
      for (int i = 0; i < runTimes.size(); i++) {
        Run rt = (Run) runTimes.get(i);
        if (rt.runTime.equals(runTime))
          return i;
      }
      return -1;
    }

    int findForecastIndex(Date forecastTime) {
      for (int i = 0; i < forecastTimes.size(); i++) {
        Forecast rt = (Forecast) forecastTimes.get(i);
        if (rt.forecastTime.equals(forecastTime))
          return i;
      }
      return -1;
    }

  } */

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
      noffsets = offsetList.size();

      countInv = new short[ntimes][nruns];
      expected = new short[ntimes][nruns];
      countOffsetInv = new short[nruns][noffsets];
      expectedOffset = new short[nruns][noffsets];

      for (UberGrid uv : varList) {
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
        RunExpected rune = uv.findRun( runTime);
        if (rune == null)
          continue;

        for (int offsetIndex = 0; offsetIndex < offsetList.size(); offsetIndex++) {
          double hourOffset = offsetList.get(offsetIndex);
          int invCount = rune.countInventory(hourOffset);
          int expectedCount = rune.countExpected(hourOffset);

          Date forecastTime = addHour( runTime, hourOffset);
          int forecastIndex = findForecastIndex(forecastTime);
          if (forecastIndex < 0) {
            log.debug("No Forecast for runTime="+formatter.toDateTimeString(runTime)+" OffsetHour="+hourOffset+
              " dataset="+name);
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
          Inventory inv = (Inventory) rune.run.invList.get(j);
          int missing = rune.grid.countMissing(inv.hourOffset);
          int forecastIndex = findForecastIndex(inv.forecastTime);
          int offsetIndex = findOffsetIndex(inv.hourOffset);

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

      } */
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
      for (int i = 0; i < offsetList.size(); i++) {
        if (offsetHour ==  offsetList.get(i))
          return i;
      }
      return -1;
    }

  }

  /* private class Forecast implements Comparable {
    ArrayList invList; // list of inventory
    Date forecastTime;

    Forecast(Date forecastTime) {
      this.forecastTime = forecastTime;
      invList = new ArrayList();
    }

    public int compareTo(Object o) {
      Forecast other = (Forecast) o;
      return forecastTime.compareTo(other.forecastTime);
    }
  }

  private class InventorySortByRunTime implements Comparator {
    public int compare(Object o1, Object o2) {
      Inventory inv1 = (Inventory) o1;
      Inventory inv2 = (Inventory) o2;
      return inv1.runTime.compareTo(inv2.runTime);
    }
  }

  private class InventorySortByForecastTime implements Comparator {
    public int compare(Object o1, Object o2) {
      Inventory inv1 = (Inventory) o1;
      Inventory inv2 = (Inventory) o2;
      return inv1.forecastTime.compareTo(inv2.forecastTime);
    }
  }  */

  private Date addHour( Date d, double hour) {
    cal.setTime( d);

    int ihour = (int) hour;
    int imin = (int) (hour - ihour) * 60;
    cal.add(Calendar.HOUR_OF_DAY, ihour);
    cal.add(Calendar.MINUTE, imin);
    return cal.getTime();
  }

  private double getOffsetHour( Date run, Date forecast) {
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

  /**
   * Create an XML document for the entire collection
   */
  public Document makeMatrixDocument() {
    if (tmAll == null)
      tmAll = new TimeMatrixDataset();

    Element rootElem = new Element("forecastModelRunCollectionInventory");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("dataset", name);

        // list all the offset hours
    for (Double offset : offsetList) {
      Element offsetElem = new Element("offsetTime");
      rootElem.addContent(offsetElem);
      offsetElem.setAttribute("hours", offset.toString());
    }

    // list all the variables
    for (UberGrid uv : varList) {
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
      runElem.setAttribute("date", dateFormat.format(runTime));

      addCountPercent(tmAll.countTotalRunInv[i], tmAll.expectedTotalRun[i], runElem, true);

      for (int k = 0; k < offsetList.size(); k++) {
        Element offsetElem = new Element("offset");
        runElem.addContent(offsetElem);
        Double offset = offsetList.get(k);
        offsetElem.setAttribute("hours", offset.toString());

        addCountPercent(tmAll.countOffsetInv[i][k], tmAll.expectedOffset[i][k], offsetElem, false);
      }
    }

    // list all the forecasts
    for (int k = forecastTimeList.size() - 1; k >= 0; k--) {
      Element fcElem = new Element("forecastTime");
      rootElem.addContent(fcElem);

      Date ftime = forecastTimeList.get(k);
      fcElem.setAttribute("date", dateFormat.format(ftime));

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
   */
  public Document makeMatrixDocument(String varName) {
    UberGrid uv = findVar(varName);
    if (uv == null)
      throw new IllegalArgumentException("No variable named = " + varName);

    Element rootElem = new Element("forecastModelRunCollectionInventory");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("dataset", name);
    rootElem.setAttribute("variable", uv.name);

    // list all the offset hours
    for (int k = 0; k < offsetList.size(); k++) {
      Element offsetElem = new Element("offsetTime");
      rootElem.addContent(offsetElem);
      Double offset = offsetList.get(k);
      offsetElem.setAttribute("hour", offset.toString());
    }

    // list all the runs
    for (int i = runTimeList.size() - 1; i >= 0; i--) {
      Element runElem = new Element("run");
      rootElem.addContent(runElem);
      Date runTime = runTimeList.get(i);
      runElem.setAttribute("date", dateFormat.format(runTime));

      RunExpected rune = uv.findRun( runTime);

      for (Double offset : offsetList) {
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
      fcElem.setAttribute("date", dateFormat.format(forecastTime));

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

    double hour = Double.parseDouble( offsetHour);

    StringBuilder sbuff = new StringBuilder();
    sbuff.append("Inventory for ").append(varName).append(" for offset hour= ").append(offsetHour).append("\n");

    for (RunExpected rune : uv.runs) {
      double[] vcoords = rune.grid.getVertCoords(hour);
      sbuff.append(" Run ");
      sbuff.append(formatter.toDateTimeString(rune.run.runTime));
      sbuff.append(": ");
      for (int j = 0; j < vcoords.length; j++) {
        if (j > 0) sbuff.append(",");
        sbuff.append(vcoords[j]);
      }
      sbuff.append("\n");
    }

    sbuff.append("\nExpected for ").append(varName).append(" for offset hour= ").append(offsetHour).append("\n");

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
    }
    return sbuff.toString();
  }


  /* private Inventory findByOffset(List invList, double offset) {
    for (int i = 0; i < invList.size(); i++) {
      Inventory inv = (Inventory) invList.get(i);
      if (inv.hourOffset == offset) return inv;
    }
    return null;
  }

  private Inventory findByForecast(List invList, Date forecast) {
    for (int i = 0; i < invList.size(); i++) {
      Inventory inv = (Inventory) invList.get(i);
      if (inv.forecastTime.equals(forecast)) return inv;
    }
    return null;
  } */


  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Create a ForecastModelRun Collection from the files in a directory.
   * @param fmrcDefinitionPath put/look for fmrc definition files in this directory, may be null
   * @param collectionName the definition file = "name.fmrcDefinition.xml";
   * @param fmr_cache cache fmr inventory files here, may be null
   * @param dirName  scan this directory
   * @param suffix filter on this suffix
   * @param mode one of the ForecastModelRun.OPEN_ modes
   * @return ForecastModelRunCollection or null if no files exist
   * @throws Exception on bad
   */
  public static FmrcInventory makeFromDirectory(String fmrcDefinitionPath, String collectionName,
          ucar.nc2.util.DiskCache2 fmr_cache, String dirName, String suffix, int mode) throws Exception {

    long startTime = System.currentTimeMillis();
    FmrcInventory fmrCollection = new FmrcInventory(fmrcDefinitionPath, collectionName);

    // override the suffix by the definition attribute suffixFilter, if it exists
    if (fmrCollection.getSuffixFilter() != null)
      suffix = fmrCollection.getSuffixFilter();

    File dir = new File(dirName);
    File[] files = dir.listFiles();
    if (null == files)
      return null;

    for (File file : files) {
      if (!file.getPath().endsWith(suffix))
        continue;

      ForecastModelRunInventory fmr = ForecastModelRunInventory.open(fmr_cache, file.getPath(), mode, true);
      if (null != fmr)
        fmrCollection.addRun(fmr);
    }

    fmrCollection.finish();
    if (debugTiming) {
      long took = System.currentTimeMillis() - startTime;
      System.out.println("that took = "+took+" msecs");
    }

    return fmrCollection;
  }

  private static boolean debugTiming = false;
  public static void main2(String args[]) throws Exception {
    String dir = "nam/c20s";
    FmrcInventory fmrc = makeFromDirectory("R:/testdata/motherlode/grid/inv/new/", "NCEP-NAM-CONUS_20km-surface", null, "C:/data/grib/"+dir, "grib1",
            ForecastModelRunInventory.OPEN_FORCE_NEW);

    FmrcDefinition def = fmrc.getDefinition();
    if (null != def) {
      System.out.println("current definition = "+fmrc.getDefinitionPath());
      //def.addVertCoordsFromCollectionInventory( fmrc);
      System.out.println( def.writeDefinitionXML());
    } else {
      System.out.println("write definition to "+fmrc.getDefinitionPath());
      def = new FmrcDefinition();
      def.makeFromCollectionInventory( fmrc);
      FileOutputStream fos = new FileOutputStream( fmrc.getDefinitionPath());
      System.out.println( def.writeDefinitionXML());
      def.writeDefinitionXML( fos);
    }

    String varName = "Temperature";
    System.out.println( fmrc.writeMatrixXML( varName));
    FileOutputStream fos = new FileOutputStream("C:/data/grib/"+dir+"/fmrcMatrix.xml");
    fmrc.writeMatrixXML( varName, fos);

    System.out.println( fmrc.writeMatrixXML( null));
    FileOutputStream fos2 = new FileOutputStream("C:/data/grib/"+dir+"/fmrcMatrixAll.xml");
    fmrc.writeMatrixXML( null, fos2);

    System.out.println( fmrc.showOffsetHour(varName,"7.0"));
  }

  ///////////////////////////////////////////////////////////////////////////

  /* private static String[] catalogs  = {
          "NCEP/DGEX/Alaska_12km",
          "NCEP/DGEX/CONUS_12km",

          "NCEP/GFS/Alaska_191km",
          "NCEP/GFS/CONUS_191km",
          "NCEP/GFS/CONUS_80km",
          "NCEP/GFS/CONUS_95km",
          "NCEP/GFS/Global_0p5deg",
          "NCEP/GFS/Global_2p5deg",
          "NCEP/GFS/Global_onedeg",
          "NCEP/GFS/Hawaii_160km",
          "NCEP/GFS/N_Hemisphere_381km",
          "NCEP/GFS/Puerto_Rico_191km",

          "NCEP/NAM/Alaska_11km",
          "NCEP/NAM/Alaska_22km",
          "NCEP/NAM/Alaska_45km/conduit",
          "NCEP/NAM/Alaska_45km/noaaport",
          "NCEP/NAM/Alaska_95km",

          "NCEP/NAM/CONUS_12km",
          "NCEP/NAM/CONUS_12km/conduit",
          "NCEP/NAM/CONUS_20km/noaaport",
          "NCEP/NAM/CONUS_20km/selectsurface",
          "NCEP/NAM/CONUS_20km/surface",
          "NCEP/NAM/CONUS_40km/conduit",
          "NCEP/NAM/CONUS_80km",
          "NCEP/NAM/Polar_90km",

          "NCEP/NDFD/CONUS_5km", };

  private static String[] catalog24hours  = {
          // 24 hours
          "NCEP/RUC/CONUS_80km",
          "NCEP/RUC2/CONUS_20km/hybrid",
          "NCEP/RUC2/CONUS_20km/pressure",
          "NCEP/RUC2/CONUS_20km/surface",
          "NCEP/RUC2/CONUS_40km",
  }; */


  public static void main(String args[]) throws Exception {  
    for (String cat : FmrcDefinition.fmrcDatasets) {
      if (cat.contains("/RUC"))
        doOne( cat, 72);
      else
        doOne( cat, 12);
    }
  }

  public static void doOne(String cat, int n) throws Exception {
    String server = "http://motherlode.ucar.edu:8080/thredds/catalog/fmrc/";
    String writeDir = "D:/temp/modelDef/";
    new File(writeDir).mkdirs();

    String catName = server + cat + "/files/catalog.xml";
    FmrcInventory fmrCollection = makeFromCatalog(null, catName, catName, n, ForecastModelRunInventory.OPEN_FORCE_NEW);

    String writeFile = writeDir + StringUtil.replace(cat, "/","-") + ".fmrcDefinition.xml";
    System.out.println("write definition to " + writeFile);
    FmrcDefinition def = new FmrcDefinition();
    def.makeFromCollectionInventory(fmrCollection);
    FileOutputStream fos = new FileOutputStream(writeFile);
    def.writeDefinitionXML(fos);
    fos.close();
  }

  /**
    * Create a ForecastModelRun Collection from the datasets in a catalog.
    * @param catURL  scan this catalog
    * @throws Exception on bad
    */
   public static void writeDefinitionFromCatalog(String catURL, String collectionName, int maxDatasets) throws Exception {

     FmrcInventory fmrCollection = makeFromCatalog( catURL, collectionName, maxDatasets, ForecastModelRunInventory.OPEN_NORMAL);
     System.out.println("write definition to "+fmrCollection.getDefinitionPath());
     FmrcDefinition def = new FmrcDefinition();
     def.makeFromCollectionInventory( fmrCollection);
     FileOutputStream fos = new FileOutputStream( fmrCollection.getDefinitionPath());
     def.writeDefinitionXML( fos);
  }

  /**
    * Create a ForecastModelRun Collection from the datasets in a catalog.
    * @param catURL  scan this catalog
    * @throws Exception on bad
    */
  public static FmrcInventory makeFromCatalog(String catURL, String collectionName, int maxDatasets, int mode) throws Exception {
    DiskCache2 cache =  new DiskCache2("fmrcInventory/", true, 0, -1); // dont scour - messes up the TDS!

    return makeFromCatalog(cache, catURL, collectionName, maxDatasets,  mode);
  }

  public static FmrcInventory makeFromCatalog(DiskCache2 cache, String catURL, String collectionName, int maxDatasets, int mode) throws Exception {
     String fmrcDefinitionPath = (cache == null)? null : cache.getRootDirectory()+"/defs/";

     System.out.println("***makeFromCatalog "+catURL);
     long startTime = System.currentTimeMillis();
     FmrcInventory fmrCollection = new FmrcInventory(fmrcDefinitionPath, collectionName);

     CatalogCrawler crawler = new CatalogCrawler(CatalogCrawler.USE_ALL_DIRECT, false,
             new MyListener(fmrCollection, maxDatasets, mode, cache));

     crawler.crawl(catURL, null, System.out, null);
     fmrCollection.finish();

     if (debugTiming) {
       long took = System.currentTimeMillis() - startTime;
       System.out.println("that took = "+took+" msecs");
     }
     return fmrCollection;
  }

  private static class MyListener implements CatalogCrawler.Listener {
    FmrcInventory fmrCollection;
    DiskCache2 cache;
    int maxDatasets;
    int mode, count;
    boolean first = true;

    MyListener(FmrcInventory fmrCollection, int maxDatasets, int mode, DiskCache2 cache) {
      this.fmrCollection = fmrCollection;
      this.maxDatasets = maxDatasets;
      this.mode = mode;
      this.count = 0;
      this.cache = cache;
    }

    public void getDataset(InvDataset dd, Object context) {
      if ((count > maxDatasets) && (maxDatasets > 0)) return;

      InvAccess access = dd.getAccess(ServiceType.OPENDAP);
      if (access == null) {
        System.out.println(" no opendap access");
        return;
      }

      if (first) { // skip the first one
        System.out.println(" skip "+access.getStandardUrlName());
        first = false;
        return;
      }

      count++;      
      System.out.println(" access " + access.getStandardUrlName());
      ForecastModelRunInventory fmr;
      try {
        fmr = ForecastModelRunInventory.open(cache, access.getStandardUrlName(), mode, false);
        if (null != fmr) {
          fmrCollection.addRun(fmr);
          fmr.releaseDataset();
        }
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }

    }

    public boolean getCatalogRef(InvCatalogRef dd, Object context) { return true; }

  }

  public static void main4(String args[]) throws Exception {
    /* String work = "R:/testdata2/motherlode/grid2/";
    fmrcDefinitionPath = work+"def/";
    cache = new DiskCache2(work+"inv/", false, -1, -1);

    File file = new File(work+"def/");
    file.mkdirs();

    for (int i = 0; i < catalogs.length; i++) {
      String urlFragment = catalogs[i];
      String catURL = "http://motherlode.ucar.edu:8080/thredds/catalog/model/"+urlFragment+"/catalog.xml";
      makeDefinitionFromCatalog(catURL);
    } */

    String dir = "nam/conus80";
    FmrcInventory fmrc = makeFromDirectory("C:/temp", "NCEP-NAM-CONUS_80km", null, "C:/data/grib/"+dir, "grib1",
            ForecastModelRunInventory.OPEN_FORCE_NEW);

  }

}
