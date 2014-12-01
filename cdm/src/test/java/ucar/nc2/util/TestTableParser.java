package ucar.nc2.util;

import org.junit.Test;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureMembers;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.Sequence;
import ucar.nc2.iosp.noaa.Ghcnm;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test  TableParser.readTable
 *
 * @author caron
 * @since 4/19/12
 */
public class TestTableParser {

    ////////////////////////////////////////////////////////////////////////////////////
  static final String testName3 = "/resources/nj22/tables/nexrad.tbl";

  @Test
  public void testReadNexradTable() throws IOException {
    Class c = TableParser.class;
    InputStream is = c.getResourceAsStream(testName3);
    List<TableParser.Record> recs = TableParser.readTable(is, "3,15,54,60d,67d,73d", 50000);
    for (TableParser.Record record : recs) {
      for (int j = 0; j < record.values.size(); j++) {
        Object s = record.values.get(j);
        System.out.print(" " + s.toString());
      }
      System.out.println();
    }
  }

}
