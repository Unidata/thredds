package ucar.nc2.iosp.grib;

import junit.framework.TestCase;
import ucar.nc2.Attribute;
import ucar.nc2.TestAll;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;

import java.io.IOException;

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
    TestAll.actOnAll(TestAll.testdataDir + "cdmUnitTest/tds/new", new TestAll.FileFilterImpl("grib2"), new TestAll.Act() {
      @Override
      public int doAct(String filename) throws IOException {
        return doOne(filename);
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
}
