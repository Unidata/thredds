package ucar.nc2.iosp.grib;

import junit.framework.TestCase;
import ucar.grib.GribGridRecord;
import ucar.grib.GribNumbers;
import ucar.grib.grib2.Grib2Tables;
import ucar.grib.grib2.ParameterTable;
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

  public TestIntervalVars( String name) {
    super(name);
  }

  public int nfiles = 0;
  public int nvars = 0;
  public int nintVars = 0;
  public void testCountIntervalVars() throws Exception {
    String dir = TestAll.testdataDir + "cdmUnitTest/tds/new";
    //String dir = "E:/formats/grib";
    TestAll.actOnAll(dir, new TestAll.FileFilterImpl("grib2"), new TestAll.Act() {
      @Override
      public int doAct(String filename) throws IOException {
        return doTwo(filename);
      }
    });
    System.out.printf("%nnfiles = %d %n", nfiles);
    System.out.printf("totvars = %d %n", nvars);
    System.out.printf("intVars = %d %n", nintVars);
  }

  private int doOne(String filename) throws IOException {
    GridDataset ncd = GridDataset.open(filename);
    System.out.printf("Open %s%n", filename);
    nfiles++;
    for (GridDatatype g : ncd.getGrids()) {
      GridCoordSystem gsys = g.getCoordinateSystem();
      CoordinateAxis t = gsys.getTimeAxis();
      Variable v = g.getVariable();
      nvars++;
      if (null != v.findAttribute("cell_methods")) {
        Attribute param = v.findAttribute("GRIB_product_definition_template");
        System.out.printf(" %s (ntimes=%d) template=%s %n", v.getName(), t.getSize(), param.getNumericValue());
        nintVars++;
      }
    }
    ncd.close();
    return 1;
  }

  private int doTwo(String filename) throws IOException {
    NetcdfFile ncd = NetcdfFile.open(filename);
    //System.out.printf("==============================================================================%n");
    System.out.printf("%n%s%n", filename);

    GribGridServiceProvider iosp = (GribGridServiceProvider) ncd.getIosp();
    GridIndex index = (GridIndex) iosp.sendIospMessage("GridIndex");

    Map<Integer, Product> pdsSet = new HashMap<Integer, Product>();

    List<GridRecord> grList = index.getGridRecords();
    for (GridRecord gr : grList) {
      GribGridRecord ggr = (GribGridRecord) gr;
      int startInterval = ggr.startOfInterval;
      if ((startInterval == GribNumbers.UNDEFINED) || (startInterval == GribNumbers.MISSING)) continue;

      /* check valid time == base time + forecast
      int forecast = ggr.forecastTime;
      Date validTime = ggr.getValidTime();
      Date refTime = ggr.getReferenceTime();
      if (forecast != startInterval) {
        String name = ParameterTable.getParameterName(ggr.discipline, ggr.category, ggr.paramNumber) +"/" + Grib2Tables.codeTable4_5(ggr.levelType1);
        System.out.printf(" **time %s %d != %d%n", name, forecast, startInterval);
      } */

      Product bean = pdsSet.get(makeUniqueId(ggr));
      if (bean == null) {
        bean = new Product(ggr);
        pdsSet.put( makeUniqueId(ggr), bean);
      }
      bean.list.add(ggr);
    }

    List<Product> sortList = new ArrayList<Product>();
    sortList.addAll(pdsSet.values());
    Collections.sort(sortList);
    for (Product p : sortList) {
      p.sort();
      System.out.printf("  %s (%d)%n", p.name, p.ggr.productTemplate);
      //System.out.printf("%s%n", p.doAccumAlgo());
    }

    ncd.close();
    return 1;
  }

  private int makeUniqueId(GribGridRecord ggr) {
    int result = 17;
    result += result*37 + ggr.productTemplate;       // productType, discipline, category, paramNumber
    result += result*37 + ggr.discipline;
    result += result*37 + ggr.category;
    result += result*37 + ggr.paramNumber;
    result *= result*37 + ggr.levelType1;
    return result;
  }

  private class Product implements Comparable<Product> {
    GribGridRecord ggr;
    List<GribGridRecord> list = new ArrayList<GribGridRecord>();
    String name;

    Product(GribGridRecord ggr) {
      this.ggr= ggr;
      name = ParameterTable.getParameterName(ggr.discipline, ggr.category, ggr.paramNumber) +"/" + Grib2Tables.codeTable4_5(ggr.levelType1);
    }

    void sort() {
      Collections.sort(list, new Comparator<GribGridRecord>() {

        @Override
        public int compare(GribGridRecord o1, GribGridRecord o2) {
          return o1.forecastTime - o2.forecastTime;
        }
      });
    }

    private String doAccumAlgo() {
      List<GribGridRecord> all = new ArrayList<GribGridRecord>( list);
      List<GribGridRecord> hourAccum = new ArrayList<GribGridRecord>(all.size());
      List<GribGridRecord> runAccum = new ArrayList<GribGridRecord>(all.size());

      Set<Integer> ftimes = new HashSet<Integer>();

      for (GribGridRecord rb : all) {
        int ftime = rb.forecastTime;
        ftimes.add( ftime);

        int start = rb.startOfInterval;
        int end = rb.forecastTime;
        if (end-start == 1) hourAccum.add(rb);
        if (start == 0) runAccum.add(rb);
      }

      int n = ftimes.size();

      Formatter f = new Formatter();
      f.format("      all: ");
      showList(all, f);

      if (hourAccum.size() > n -2) {
        for (GribGridRecord rb :hourAccum) all.remove(rb);
        f.format("hourAccum: ");
        showList(hourAccum, f);
      }

      if (runAccum.size() > n -2) {
        for (GribGridRecord rb :runAccum) all.remove(rb);
        f.format(" runAccum: ");
        showList(runAccum, f);
      }

      if ((all.size() > 0) && (all.size() != list.size())) {
        f.format("remaining: ");
        showList(all, f);
      }

      return f.toString();
    }

    private String testConstantInterval(List<GribGridRecord> list) {
        boolean same = true;
        int intv = -1;
        for (GribGridRecord rb :list) {
          int start = rb.startOfInterval;
          int end = rb.forecastTime;
          int intv2 = end - start;
          if (intv2 == 0) continue; // skip those weird zero-intervals
          else if (intv < 0) intv = intv2;
          else same = (intv == intv2);
          if (!same) break;
        }
     return same ? " Interval="+intv : " Mixed";
    }

    private void showList(List<GribGridRecord> list, Formatter f) {
      f.format("(%d) ", list.size());
      for (GribGridRecord rb : list)
        f.format(" %d-%d", rb.startOfInterval, rb.forecastTime);
      f.format(" %s %n", testConstantInterval(list));
    }


    @Override
    public int compareTo(Product o) {
      return name.compareTo(o.name);
    }
  }


}
