package ucar.nc2.iosp.grib;

import junit.framework.TestCase;
import ucar.grib.GribGridRecord;
import ucar.grib.GribPds;
import ucar.grib.grib1.Grib1Pds;
import ucar.grib.grib1.Grib1Tables;
import ucar.grib.grib2.Grib2Pds;
import ucar.grib.grib2.Grib2Tables;
import ucar.grid.GridIndex;
import ucar.grid.GridRecord;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.TestAll;
import ucar.nc2.Variable;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.iosp.grid.GridServiceProvider;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionImpl;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

/**
 * misc test against the TDS grib files
 *
 * @author caron
 * @since Jun 3, 2010
 */
public class TestGribFromTds extends TestCase {
  private boolean show = false;

  public TestGribFromTds(String name) {
    super(name);
  }

  public int nfiles = 0;
  public int nvars = 0;
  public int nintVars = 0;

  public void testGribFromTds() throws Exception {
    //doDir(TestAll.testdataDir + "cdmUnitTest/formats/grib1", false);
    //doDir(TestAll.testdataDir + "cdmUnitTest/formats/grib2", false);
    doDir(TestAll.testdataDir + "cdmUnitTest/tds/normal", false);
    //doDir(TestAll.testdataDir + "cdmUnitTest/tds/fnmoc", true);
  }

  public void doDir(String dir, boolean recurse) throws Exception {
    //String dir = "E:/work/foster";
    TestAll.actOnAll(dir, new GribFilter(), new TestAll.Act() {
      @Override
      public int doAct(String filename) throws IOException {
        System.out.printf("%n%s%n", filename);
        checkOpen(filename);
        //showGenType(filename, false);
        //showGrids(filename);
        //showNames(filename);
        //showProjectionType(filename);
        //showStatType(filename, true);
        //showTableVersion(filename, true);
        //showTemplates(filename);
        //showTimeIntervalType(filename);
        //showTimeInterval(filename);
        return 0;
      }
    }, recurse);
    System.out.printf("%nnfiles = %d %n", nfiles);
    System.out.printf("totvars = %d %n", nvars);
    System.out.printf("intVars = %d %n", nintVars);
  }


  public class GribFilter implements FileFilter {
    public boolean accept(File file) {
      return (!file.getPath().endsWith(".gbx8"));
    }
  }


  public void testOne() throws IOException {
    //checkTimeInterval("Q:/cdmUnitTest/tds/grib/ndfd/NDFD_CONUS_5km_conduit_20100913_0000.grib2");
    showTimeInterval2("Q:/cdmUnitTest/tds/normal/NDFD_CONUS_5km_20100912_1800.grib2");
  }

  ///////////////////////////////////////////////////////////
  // just open files - see if there are error messages
  public void checkOpen(String filename) throws IOException {
    NetcdfFile ncd = null;
    try {
      ncd = NetcdfFile.open(filename);
    } catch (Exception t) {
      System.out.printf("Failed on %s = %s%n", filename, t.getMessage());
      return;
    } catch (Throwable t) {
      System.out.printf("Failed on %s = %s%n", filename, t.getMessage());
      return;
    } finally {
      if (ncd != null) ncd.close();
    }
  }

  ///////////////////////////////////////////////////////////
  // show grid names and param ids
  public void showGrids(String filename) throws IOException {
    Formatter f = new Formatter(System.out);
    GridDataset ncd = null;

    try {
      ncd = GridDataset.open(filename);
      List<GridDatatype> grids = ncd.getGrids();
      Collections.sort(grids);

      for (GridDatatype g : grids) {
        f.format(" %s (", g.getName());
        Attribute att = g.findAttributeIgnoreCase("GRIB_param_id");
        if (att != null)
          f.format("%s/%s,param=%s", att.getNumericValue(1), att.getNumericValue(2), att.getNumericValue(3));
        f.format(")%n");
      }
    } catch (Throwable t) {
      System.out.printf("Failed on %s = %s%n", filename, t.getMessage());
      return;
    } finally {
      if (ncd != null) ncd.close();
    }
  }

  ////////////////////////////////////////////////////////////////////////
  // show getGenProcessType()
  public void showGenType(String filename, boolean showVars) throws IOException {
    GridServiceProvider.debugOpen = true;
    NetcdfFile ncd = null;
    try {
      ncd = NetcdfFile.open(filename);
    } catch (Throwable t) {
      System.out.printf("Failed on %s = %s%n", filename, t.getMessage());
      return;
    }

    GribGridServiceProvider iosp = (GribGridServiceProvider) ncd.getIosp();
    GridIndex index = (GridIndex) iosp.sendIospMessage("GridIndex");
    boolean isGrib1 = iosp.getFileTypeId().equals("GRIB1");

    boolean first = true;
    Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();

    List<GridRecord> grList = index.getGridRecords();
    for (GridRecord gr : grList) {
      GribGridRecord ggr = (GribGridRecord) gr;
      int genType = ggr.getPds().getGenProcessId();
      if (!isGrib1) {
        Grib2Pds pds =  (Grib2Pds) ggr.getPds();
        genType = pds.getGenProcessType();
      }
      List<String> uses = map.get(genType);
      if (uses == null) {
        uses = new ArrayList<String>();
        map.put(genType, uses);
      }
      String name = ggr.getParameterName();
      if (!uses.contains(name))
        uses.add(name);

      if (first) {
        System.out.printf("Center=  %d / %d%n", ggr.getCenter(), ggr.getSubCenter());
        first = false;
      }
    }

    List<Integer> sortList = new ArrayList<Integer>();
    sortList.addAll(map.keySet());
    Collections.sort(sortList);
    for (int val : sortList) {
      String desc = isGrib1 ? "" : Grib2Tables.codeTable4_3(val);
      System.out.printf(" %d (%s)%n", val, desc);
      if (showVars) {
        List<String> uses = map.get(val);
        for (String use : uses)
          System.out.printf("   %s%n", use);
      }
    }

    GridServiceProvider.debugOpen = false;
  }

  ////////////////////////////////////////////////////////////////////////
  // show param names. how they relate to level, stat, ens, prob
  private void showNames(String filename) throws IOException {
    GridDataset ncd = GridDataset.open(filename);
    nfiles++;

    String format = "%-50s %-20s %-12s %-12s %-12s %n";
    System.out.printf(format, " ", "level", "stat", "ens", "prob" );

    List<GridDatatype> grids =  ncd.getGrids();
    Collections.sort(grids);
    for (GridDatatype g : grids) {
      GridCoordSystem gsys = g.getCoordinateSystem();
      CoordinateAxis t = gsys.getTimeAxis();
      Variable v = g.getVariable();
      String level = v.findAttribute("GRIB_level_type_name").getStringValue();
      Attribute att = v.findAttribute("GRIB_interval_stat_type");
      String stat = (att == null) ? "" : att.getStringValue();
      att = v.findAttribute("GRIB_ensemble");
      String ens = "";
      if (att != null) ens = att.getStringValue();
      else {
        att = v.findAttribute("GRIB_ensemble_derived_type");
        if (att != null) ens = att.getStringValue();
      }
      att = v.findAttribute("GRIB_probability_type");
      String prob = (att == null) ? "" : att.getStringValue();
      System.out.printf(format, v.getName(), level, stat, ens, prob);
    }
    System.out.printf("%n");
    ncd.close();
  }

  //////////////////////////////////////////////////////////////////////////
  // look for ProjectionImpl name
  public void showProjectionType(String filename) throws IOException {
    NetcdfDataset ncd = NetcdfDataset.openDataset(filename);
    GridDataset gds = new GridDataset(ncd);
    nfiles++;

    Map<String, HoldEm> map = new HashMap<String, HoldEm>();

    for (ucar.nc2.dt.GridDataset.Gridset g : gds.getGridsets()) {
      GridCoordSystem gsys = g.getGeoCoordSystem();
      for (CoordinateTransform t : gsys.getCoordinateTransforms()) {
        if (t instanceof ProjectionCT) {
          ncd.findVariable(t.getName());
          ProjectionImpl p = ((ProjectionCT)t).getProjection();
          map.put(t.getName()+" "+p.paramsToString(), new HoldEm(gsys, p, ncd.findVariable(t.getName())));
        }
      }
    }

    for (String key : map.keySet()) {
      System.out.printf("  %s: %n", key);
      checkProjection( map.get(key));
    }
    System.out.printf("%n");
    ncd.close();
  }

  private class HoldEm {
    GridCoordSystem gcs;
    Variable projVar;
    ProjectionImpl p;

    private HoldEm(GridCoordSystem gcs, ProjectionImpl p, Variable projVar) {
      this.gcs = gcs;
      this.p = p;
      this.projVar = projVar;
    }
  }

  private void checkProjection(HoldEm h) {
    System.out.printf( "    llbb=%s%n", h.gcs.getLatLonBoundingBox());
    System.out.printf( "%s%n", h.projVar);

    CoordinateAxis1D xaxis = (CoordinateAxis1D) h.gcs.getXHorizAxis();
    CoordinateAxis1D yaxis =  (CoordinateAxis1D) h.gcs.getYHorizAxis();
    h.p.projToLatLon(xaxis.getCoordValue(0), yaxis.getCoordValue(0)  );
    LatLonPointImpl start1 =  h.p.projToLatLon(xaxis.getCoordValue(0), yaxis.getCoordValue(0));
    LatLonPointImpl start2 =  h.p.projToLatLon(xaxis.getCoordValue((int)xaxis.getSize()-1), yaxis.getCoordValue((int)yaxis.getSize()-1));
    System.out.printf( "start = %s%n", start1);
    System.out.printf( "end   = %s%n", start2);
  }

  ////////////////////////////////////////////////////////////////////////
  // show getStatisticalProcessType()
  public void showStatType(String filename, boolean showVars) throws IOException {
    GridServiceProvider.debugOpen = true;
    NetcdfFile ncd = null;
    try {
      ncd = NetcdfFile.open(filename);
    } catch (Throwable t) {
      System.out.printf("Failed on %s = %s%n", filename, t.getMessage());
      return;
    }

    GribGridServiceProvider iosp = (GribGridServiceProvider) ncd.getIosp();
    GridIndex index = (GridIndex) iosp.sendIospMessage("GridIndex");
    boolean isGrib1 = iosp.getFileTypeId().equals("GRIB1");

    boolean first = true;
    Map<String, List<Integer>> map = new HashMap<String, List<Integer>>();

    List<GridRecord> grList = index.getGridRecords();
    for (GridRecord gr : grList) {
      GribGridRecord ggr = (GribGridRecord) gr;
      int genType = ggr.getPds().getStatisticalProcessType();
      if (genType < 0) continue;

      String vname = ggr.getParameterName();
      List<Integer> uses = map.get(vname);
      if (uses == null) {
        uses = new ArrayList<Integer>();
        map.put(vname, uses);
      }
      if (!uses.contains(genType))
        uses.add(genType);

      if (first) {
        System.out.printf("Center=  %d / %d%n", ggr.getCenter(), ggr.getSubCenter());
        first = false;
      }
    }

    List<String> sortList = new ArrayList<String>();
    sortList.addAll(map.keySet());
    Collections.sort(sortList);
    for (String vname : sortList) {
      List<Integer> uses = map.get(vname);
      if (showVars || uses.size() > 1) {
        for (int val : map.get(vname)) {
          if (uses.size() > 1)
            System.out.printf("***** ");
          System.out.printf(" %s%n", vname);
          String desc = Grib2Tables.codeTable4_10short( val);
          System.out.printf("  %d (%s)%n", val, desc);
        }
      }
    }

    GridServiceProvider.debugOpen = false;
  }

  ////////////////////////////////////////////////////////////////////////
  // show getTableVersion()
  public void showTableVersion(String filename, boolean showVars) throws IOException {
    GridServiceProvider.debugOpen = true;
    NetcdfFile ncd = null;
    try {
      ncd = NetcdfFile.open(filename);
    } catch (Throwable t) {
      System.out.printf("Failed on %s = %s%n", filename, t.getMessage());
      return;
    }

    GribGridServiceProvider iosp = (GribGridServiceProvider) ncd.getIosp();
    GridIndex index = (GridIndex) iosp.sendIospMessage("GridIndex");
    boolean isGrib1 = iosp.getFileTypeId().equals("GRIB1");
    if (!isGrib1) return;

    boolean first = true;
    Set<Integer> versionSet = new HashSet<Integer>();

    List<GridRecord> grList = index.getGridRecords();
    for (GridRecord gr : grList) {
      GribGridRecord ggr = (GribGridRecord) gr;
      int ver = ggr.getTableVersion();
      versionSet.add(ver);
      if (first) {
        System.out.printf(    "Originating Center : (%d) %s%n", ggr.getCenter(), Grib1Tables.getCenter_idName( ggr.getCenter() ));
        System.out.printf("Originating Sub-Center : (%d) %s%n",  ggr.getSubCenter(),  Grib1Tables.getSubCenter_idName( ggr.getCenter(), ggr.getSubCenter()) );
        first = false;
      }
    }

    System.out.printf("Version(s) = ");
    Iterator<Integer> iter = versionSet.iterator();
    while (iter.hasNext()) {
      System.out.printf(" %d", iter.next());
    }
    System.out.printf("%n");

    GridServiceProvider.debugOpen = false;
  }

  ////////////////////////////////////////////////////////////////////////
  // show timeIncrementType
  // Grib2 only
  public void showTimeIntervalType2(String filename) throws IOException {
    GridServiceProvider.debugOpen = true;
    NetcdfFile ncd = NetcdfFile.open(filename);

    GribGridServiceProvider iosp = (GribGridServiceProvider) ncd.getIosp();
    GridIndex index = (GridIndex) iosp.sendIospMessage("GridIndex");

    Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();

    List<GridRecord> grList = index.getGridRecords();
    for (GridRecord gr : grList) {
      GribGridRecord ggr = (GribGridRecord) gr;
      if (!ggr.isInterval()) continue;

      Grib2Pds pds = (Grib2Pds) ggr.getPds();
      Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) pds;
      for (Grib2Pds.TimeInterval intv : pdsIntv.getTimeIntervals()) {
        int val = intv.timeIncrementType;
        List<String> uses = map.get(val);
        if (uses == null) {
          uses = new ArrayList<String>();
          map.put(val, uses);
        }
        String name = ggr.getParameterName();
        if (!uses.contains(name))
          uses.add(name);
      }
    }

    List<Integer> sortList = new ArrayList<Integer>();
    sortList.addAll(map.keySet());
    Collections.sort(sortList);
    for (int val : sortList) {
      System.out.printf(" %d (%s)%n", val, Grib2Tables.codeTable4_11(val));
      List<String> uses = map.get(val);
      for (String use : uses)
        System.out.printf("   %s%n", use);
    }

    GridServiceProvider.debugOpen = false;
  }

  ////////////////////////////////////////////////////////////////////////
  // show params that are time intervals, how they relate to Grib2Pds.makeDate().
  // Grib2 only
  public void showTimeInterval2(String filename) throws IOException {
    GridServiceProvider.debugOpen = true;
    NetcdfFile ncd = NetcdfFile.open(filename);
    DateFormatter df = new DateFormatter();
    Calendar cal = Calendar.getInstance();

    GribGridServiceProvider iosp = (GribGridServiceProvider) ncd.getIosp();
    GridIndex index = (GridIndex) iosp.sendIospMessage("GridIndex");

    List<GridRecord> grList = index.getGridRecords();
    for (GridRecord gr : grList) {
      GribGridRecord ggr = (GribGridRecord) gr;
      if (!ggr.isInterval()) continue;

      Grib2Pds pds = (Grib2Pds) ggr.getPds();
      System.out.printf(" ref=%s fore=%s%n", df.toDateTimeStringISO(gr.getReferenceTime()), df.toDateTimeStringISO(pds.getForecastDate()));

      Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) pds;
      long timeEnd = pdsIntv.getIntervalTimeEnd();
      int[] intv = pds.getForecastTimeInterval();
      long startDate = Grib2Pds.makeDate(gr.getReferenceTime().getTime(), pds.getTimeUnit(), intv[0], cal);
      long endDate = Grib2Pds.makeDate(gr.getReferenceTime().getTime(), pds.getTimeUnit(), intv[1], cal);

      System.out.printf("  intv=[%s, %s] = [%d,%d]%n",  df.toDateTimeStringISO(new Date(startDate)),
              df.toDateTimeStringISO(new Date(endDate)), intv[0], intv[1]);
      System.out.printf("  timeEnd=%s", df.toDateTimeStringISO(new Date(timeEnd)));

      if (timeEnd == startDate)
        System.out.printf(" agrees with intv start");
      if (timeEnd == endDate)
        System.out.printf(" agrees with intv end");
      System.out.printf("%n");

      for (Grib2Pds.TimeInterval ti : pdsIntv.getTimeIntervals()) {
        System.out.printf("    TimeInterval type=%d range=%d incr=%d%n", ti.timeIncrementType, ti.timeRangeLength, ti.timeIncrement);
      }
      break;
    }

    GridServiceProvider.debugOpen = false;
  }

  ////////////////////////////////////////////////////////////////////////
 // show GRIB_product_definition_template attribute value
  // Grib2 only
  private int showTemplateType2(String filename) throws IOException {
    GridDataset ncd = GridDataset.open(filename);
    nfiles++;

    HashMap<Integer, List<String>> map = new  HashMap<Integer, List<String>>();

    for (GridDatatype g : ncd.getGrids()) {
      GridCoordSystem gsys = g.getCoordinateSystem();
      CoordinateAxis t = gsys.getTimeAxis();
      Variable v = g.getVariable();
      Attribute param = v.findAttribute("GRIB_product_definition_template");
      Integer template = param.getNumericValue().intValue();
      List<String> list = map.get(template);
      if (list == null) {
        list = new ArrayList<String>();
        map.put(template, list);
      }
      list.add(v.getShortName());
    }
    ncd.close();

    for (Integer key : map.keySet()) {
      System.out.printf("template=%d:", key);
      List<String> list = map.get(key);
      for (String vname : list)
        System.out.printf("%s, ", vname);
      System.out.printf("%n");
    }
    System.out.printf("%n");

    return 1;
  }

  /* see if analy code differs
  private int checkAnal(String filename) throws IOException {
    NetcdfFile ncd = NetcdfFile.open(filename);
    nfiles++;
    //System.out.printf("==============================================================================%n");

    GribGridServiceProvider iosp = (GribGridServiceProvider) ncd.getIosp();
    GridIndex index = (GridIndex) iosp.sendIospMessage("GridIndex");

    Set<Integer> codeSet = null;
    List<GridRecord> grList = index.getGridRecords();
    for (GridRecord gr : grList) {
      GribGridRecord ggr = (GribGridRecord) gr;
      if (codeSet == null) {
        codeSet = new HashSet<Integer>();
        codeSet.add(ggr.analGenProcess);
        System.out.printf("analGen=%d ", ggr.analGenProcess);
      } else {
        if (codeSet.contains(ggr.analGenProcess)) continue;
        codeSet.add(ggr.analGenProcess);
        System.out.printf("%d (%s) ", ggr.analGenProcess, ggr.getParameterName());
      }
    }
    System.out.printf("%n");

    ncd.close();
    return 1;
  }

  // look for probability vars
  private int checkProb(String filename) throws IOException {
    NetcdfFile ncd = NetcdfFile.open(filename);
    nfiles++;
    //System.out.printf("==============================================================================%n");

    Set<String> vars = new HashSet<String>();

    GribGridServiceProvider iosp = (GribGridServiceProvider) ncd.getIosp();
    GridIndex index = (GridIndex) iosp.sendIospMessage("GridIndex");

    List<GridRecord> grList = index.getGridRecords();
    for (GridRecord gr : grList) {
      GribGridRecord ggr = (GribGridRecord) gr;
      if ((ggr.productTemplate == 5) || (ggr.productTemplate == 9)) {
        vars.add( ggr.getParameterName());
      }
    }

    if (vars.size() > 0) {
    System.out.printf("Vars with templates 5,9: ");
    for (String vname : vars)
      System.out.printf("%s, ",vname);
    System.out.printf("%n");
    }

    ncd.close();
    return 1;
  } */


  private int analalyseIntervals(String filename) throws IOException {
    NetcdfFile ncd = NetcdfFile.open(filename);
    nfiles++;
    //System.out.printf("==============================================================================%n");

    GribGridServiceProvider iosp = (GribGridServiceProvider) ncd.getIosp();
    GridIndex index = (GridIndex) iosp.sendIospMessage("GridIndex");

    Map<Integer, Product> pdsSet = new HashMap<Integer, Product>();

    List<GridRecord> grList = index.getGridRecords();
    for (GridRecord gr : grList) {
      GribGridRecord ggr = (GribGridRecord) gr;
      GribPds pds =  ggr.getPds();
      if (!pds.isInterval()) continue;

      //int startInterval = ggr.startOfInterval;
      //if ((startInterval == GribNumbers.UNDEFINED) || (startInterval == GribNumbers.MISSING)) continue;

      /* check valid time == base time + forecast
      int forecast = ggr.forecastTime;
      Date validTime = ggr.getValidTime();
      Date refTime = ggr.getReferenceTime();
      if (forecast != startInterval) {
        String name = ParameterTable.getParameterName(ggr.discipline, ggr.category, ggr.paramNumber) +"/" + Grib2Tables.codeTable4_5(ggr.levelType1);
        System.out.printf(" **time %s %d != %d%n", name, forecast, startInterval);
      } */

      Product bean = pdsSet.get(ggr.cdmVariableHash());
      if (bean == null) {
        bean = new Product(ggr);
        pdsSet.put(ggr.cdmVariableHash(), bean);
        nintVars++;
      }
      bean.list.add(ggr);
    }

    List<Product> sortList = new ArrayList<Product>();
    sortList.addAll(pdsSet.values());
    Collections.sort(sortList);
    for (Product p : sortList) {
      p.sort();
      System.out.printf("  %s (%d)%n", p.name, p.ggr.getPds().getParameterNumber());
      System.out.printf("%s%n", p.doAccumAlgo(false));
    }

    ncd.close();
    return 1;
  }

  private class Product implements Comparable<Product> {
    GribGridRecord ggr;
    List<GribGridRecord> list = new ArrayList<GribGridRecord>();
    String name;

    Product(GribGridRecord ggr) {
      this.ggr = ggr;
      name = ggr.getParameterName() + "/" + ggr.getLevelType1();
    }

    void sort() {
      Collections.sort(list, new Comparator<GribGridRecord>() {

        @Override
        public int compare(GribGridRecord o1, GribGridRecord o2) {
          return (int) (o1.getValidTime().getTime() - o2.getValidTime().getTime());
        }
      });
    }

    private String doAccumAlgo(boolean detail) {
      List<GribGridRecord> all = new ArrayList<GribGridRecord>(list);
      List<GribGridRecord> hourAccum = new ArrayList<GribGridRecord>(all.size());
      List<GribGridRecord> runAccum = new ArrayList<GribGridRecord>(all.size());

      Set<Integer> ftimes = new HashSet<Integer>();

      for (GribGridRecord rb : all) {
        GribPds pds =  ggr.getPds();
        int ftime = pds.getForecastTime();
        ftimes.add(ftime);

        int[] intv = pds.getForecastTimeInterval();

        int start = intv[0];
        int end = intv[1];
        if (end - start == 1) hourAccum.add(rb);
        if (start == 0) runAccum.add(rb);
      }

      int n = ftimes.size();

      Formatter f = new Formatter();
      f.format("      all: ");
      check(detail, all, f);

      if (hourAccum.size() > n - 2) {
        for (GribGridRecord rb : hourAccum) all.remove(rb);
        f.format("hourAccum: ");
        check(detail, hourAccum, f);
      }

      if (runAccum.size() > n - 2) {
        for (GribGridRecord rb : runAccum) all.remove(rb);
        f.format(" runAccum: ");
        check(detail, runAccum, f);
      }

      if ((all.size() > 0) && (all.size() != list.size())) {
        f.format("remaining: ");
        check(detail, all, f);
      }

      return f.toString();
    }

    void check(boolean detail, List<GribGridRecord> list, Formatter f) {
      if (detail) showList(list, f);
      else {
        boolean unique = testUniqueEndpoint(list, f);
        if (!unique) showList(list, f);
        else testConstantInterval(list, f);
      }
    }


    private void showList(List<GribGridRecord> list, Formatter f) {
      f.format("(%d) ", list.size());
      for (GribGridRecord rb : list) {
        GribPds pds =  rb.getPds();
        int[] intv = pds.getForecastTimeInterval();
        f.format(" %d-%d", intv[0], intv[1]);
      }
      testConstantInterval(list, f);
    }

    private void testConstantInterval(List<GribGridRecord> list, Formatter f) {
      boolean same = true;
      int intv = -1;
      for (GribGridRecord rb : list) {
        GribPds pds =  rb.getPds();
        int[] interv = pds.getForecastTimeInterval();
        int start = interv[0];
        int end = interv[1];
        int intv2 = end - start;
        if (intv2 == 0) continue; // skip those weird zero-intervals
        else if (intv < 0) intv = intv2;
        else same = (intv == intv2);
        if (!same) break;
      }
      if (same)
        f.format(" Interval=%d%n", intv);
      else
        f.format(" Mixed%n");
    }

    private boolean testUniqueEndpoint(List<GribGridRecord> list, Formatter f) {
      boolean unique = true;
      HashSet<Integer> set = new HashSet<Integer>();
      for (GribGridRecord rb : list) {
        GribPds pds =  rb.getPds();
        int end = pds.getForecastTime();
        if (set.contains(end)) {
          unique = false;
          break;
        }
        set.add(end);
      }
      f.format(" Unique=%s", unique);
      return unique;
    }

    @Override
    public int compareTo(Product o) {
      return name.compareTo(o.name);
    }
  }


}
