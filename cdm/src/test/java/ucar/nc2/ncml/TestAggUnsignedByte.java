/*
 * (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
 */
package ucar.nc2.ncml;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/** Test AggExisting with Unsigned Byte */

public class TestAggUnsignedByte {
  private final String AGG_FILENAME = "ubyte_agg.ncml";
  private final String UBYTE_VAR_NAME = "ir_anvil_detection";

  private NetcdfFile ncfile;
  private Variable v;

  /**
   * Read aggregation of unsigned variable for test
   */
  @Before
  public void prepAggDataset() {
    String filename = "file:./"+TestNcML.topDir + AGG_FILENAME;
    try {
      ncfile = NcMLReader.readNcML(filename, null);
      v = ncfile.findVariable(UBYTE_VAR_NAME);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Test reading entire unsigned variable
   *
   * @throws IOException
   */
  @Test
  public void testIsUnsignedRead() throws IOException {
    // this worked as of 4.6.7, so no bug here...
    assert v.isUnsigned();

    Array data = v.read();
    // this is the failure for https://github.com/Unidata/thredds/issues/695
    assert data.isUnsigned();
  }

  /**
   * Test reading a section of data from an unsigned variable
   * @throws IOException
   * @throws InvalidRangeException
   */
  @Test
  public void testIsUnsignedReadSection() throws IOException, InvalidRangeException {
    // this worked as of 4.6.7, so no bug here...
    assert v.isUnsigned();

    int[] shape = new int[] {1, 10, 20};
    Section section = new Section(shape);
    Array data = v.read(section);
    // this is the failure for https://github.com/Unidata/thredds/issues/695
    assert data.isUnsigned();
  }

  /**
   * close out agg dataset when tests are finished
   */
  @After
  public void closeAggDataset() {
    try {
      ncfile.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
