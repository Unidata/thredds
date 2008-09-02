package ucar.nc2.dataset;

import ucar.nc2.*;
import ucar.nc2.util.*;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import junit.framework.TestCase;

/**
 *  from (WUB-664639) (Didier Earith)
 *
 * @author caron
 */
public class TestSectionFillValue extends TestCase {
  private String filename = TestAll.cdmTestDataDir +"standardVar.nc";

  public TestSectionFillValue( String name) {
    super(name);
  }

  public void testNetcdfFile() throws Exception {
    NetcdfDataset ncfile = NetcdfDataset.openDataset(filename);
    VariableDS v = (VariableDS) ncfile.findVariable("t3");
    assert (v != null);
    assert (v.hasFillValue());
    assert (v.findAttribute("_FillValue") != null);

    int rank = v.getRank();
    ArrayList ranges = new ArrayList();
    ranges.add(null);
    for (int i = 1; i < rank; i++) {
      ranges.add(new Range(0, 1));
    }

    VariableDS v_section = (VariableDS) v.section(ranges);
    assert (v_section.findAttribute("_FillValue") != null);
    System.out.println(v_section.findAttribute("_FillValue"));
    assert (v_section.hasFillValue());

    ncfile.close();
  }

}
