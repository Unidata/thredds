package ucar.nc2.iosp.grib;

import junit.framework.TestCase;
import ucar.grib.GribGridRecord;
import ucar.grib.GribPds;
import ucar.grib.grib2.Grib2Pds;
import ucar.grib.grib2.Grib2Tables;
import ucar.grid.GridIndex;
import ucar.grid.GridRecord;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.TestAll;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.iosp.grid.GridServiceProvider;

import java.io.IOException;
import java.util.*;

/**
 * Describe
 *
 * @author caron
 * @since Jun 3, 2010
 */
public class TestIntervalVars extends TestCase {
  private boolean show = false;

  public TestIntervalVars(String name) {
    super(name);
  }

  public int nfiles = 0;
  public int nvars = 0;
  public int nintVars = 0;

  // grib 2 only
  public void testCountIntervalVars() throws Exception {
    String dir = TestAll.testdataDir + "cdmUnitTest/tds/prob";
    //String dir = "E:/formats/grib";
    TestAll.actOnAll(dir, new TestAll.FileFilterImpl("grib2"), new TestAll.Act() {
      @Override
      public int doAct(String filename) throws IOException {
        System.out.printf("%n%s%n", filename);
        //showNames(filename);
        //checkTemplates(filename);
        checkIntervalType(filename);
        return 0;
      }
    });
    System.out.printf("%nnfiles = %d %n", nfiles);
    System.out.printf("totvars = %d %n", nvars);
    System.out.printf("intVars = %d %n", nintVars);
  }

  public void testOne() throws IOException {
    checkIntervalType("Q:/cdmUnitTest/tds/grib/ndfd/NDFD_CONUS_5km_conduit_20100913_0000.grib2");
  }

  public void checkIntervalType(String filename) throws IOException {
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

  private int checkIntervalTypqe(String filename) throws IOException {
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


  private int checkTemplates(String filename) throws IOException {
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
