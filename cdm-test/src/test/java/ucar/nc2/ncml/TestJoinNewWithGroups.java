package ucar.nc2.ncml;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * JoinNew has bug when groups are present because non-agg vars are not getting proxied.
 * The example unfortunately does not satisfy homogeneity = one is 44 x 60, the other 46 x 60.
 *
 * @author caron
 * @since 3/30/12
 */
@Category(NeedsCdmUnitTest.class)
public class TestJoinNewWithGroups {

  // test case from joleenf@ssec.wisc.edu 03/22/2012

  @Test
  public void testJoinNewWithGroups() throws IOException, InvalidRangeException {
    String location = TestDir.cdmUnitTestDir + "agg/groups/groupsJoinNew.ncml";
    GridDataset ncd = null;
    try {
      ncd = GridDataset.open(location);  // fails here
      
      GridDatatype v = ncd.findGridDatatype("All_Data/Lifted_Index");  // the only agg var
      assert v != null;
      assert v.getRank() == 3;
      Section s = new Section(v.getShape());
      //assert s.equals(new Section(new int[] {2, 44, 60})) : s ;
      
      v = ncd.findGridDatatype("All_Data/CAPE");  // random non-agg var
      assert v != null;
      assert v.getRank() == 2;
      Array a = v.readVolumeData(0);
      System.out.printf("array section for %s = %s%n", v, new Section(a.getShape()));

    } finally {
      if (ncd != null) ncd.close();
    }
  }
}
