package ucar.nc2.util;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 4/19/12
 */
public class TestTableParser {

    ////////////////////////////////////////////////////////////////////////////////////
  static String testName3 = "/resources/nj22/tables/nexrad.tbl";

  @Test
  public void testRead() throws IOException {
    Class c = TableParser.class;
    InputStream is = c.getResourceAsStream(testName3);
    List recs = TableParser.readTable(is, "3,15,54,60d,67d,73d", 50000);
    for (int i = 0; i < recs.size(); i++) {
      TableParser.Record record = (TableParser.Record) recs.get(i);
      for (int j = 0; j < record.values.size(); j++) {
        Object s = record.values.get(j);
        System.out.print(" " + s.toString());
      }
      System.out.println();
    }
  }

    static String testRepeat = "C:\\data\\ghcnm\\ghcnm.v3.0.0-beta1.20101207.qae.dat";

      static public void main(String[] args) throws IOException {
        List recs = TableParser.readTable(testRepeat, "11L,15i,19,(24i,25,26,27)*10", 5);
        //List recs = TableParser.readTable(testRepeat, "11L,15i,19,24i,25,26,27", 5);
        for (int i = 0; i < recs.size(); i++) {
          TableParser.Record record = (TableParser.Record) recs.get(i);
          for (int j = 0; j < record.values.size(); j++) {
            Object s = record.values.get(j);
            System.out.print("; " + s.toString());
          }
          System.out.println();
        }
      }


}
