package ucar.nc2.iosp.bufr;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.ArrayStructure;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;

import static org.junit.Assert.fail;

/**
 * 
 * Initial dummy test class for the test_framework structure.
 * It only provides a mock sample of what it should be  
 * 
 * @author mhermida
 *
 */
public class SomeTest {
  ucar.ma2.TestStructureArray test;

	@Test
  public void testBufr() throws IOException, InvalidRangeException {
    //String fileIn = "C:/data/bufr/edition3/ecmwf/synop.bufr";
    String fileIn = TestAll.cdmUnitTestDir +"bufr/edition3/ecmwf/synop.bufr";
    NetcdfFile ncf = NetcdfFile.open(fileIn);
    System.out.println(ncf.toString());

    Structure s = (Structure) ncf.findVariable(BufrIosp.obsRecord);
    Array data = s.read();
    test.testArrayStructure( (ArrayStructure) data);

    Array data2 = s.read(new Section().appendRange(1,3));
    assert data2.getSize() == 3;
    test.testArrayStructure( (ArrayStructure) data2);
    System.out.println( NCdumpW.printArray(data2, "testArrayStructure", null));

    // test nested
  }

  @Test
  public void utestBufrEnhanced() throws IOException, InvalidRangeException {
    //String fileIn = "C:/data/bufr/edition3/ecmwf/synop.bufr";
    String fileIn = TestAll.cdmUnitTestDir +"bufr/edition3/ecmwf/synop.bufr";
    NetcdfDataset ncf = NetcdfDataset.openDataset(fileIn);
    System.out.println(ncf.toString());

    Structure s = (Structure) ncf.findVariable(BufrIosp.obsRecord);
    Array data = s.read();
    test.testArrayStructure( (ArrayStructure) data);

    Array data2 = s.read(new Section().appendRange(1,3));
    assert data2.getSize() == 3;
    test.testArrayStructure( (ArrayStructure) data2);
    System.out.println( NCdumpW.printArray(data2, "testArrayStructure", null));

    // test nested
  }

}
