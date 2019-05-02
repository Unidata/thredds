package ucar.nc2.grib.grib2.table;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables.TableType;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables.WmoTable;

@RunWith(JUnit4.class)
public class TestWmoCodeFlagTables {

  @Test
  public void testWmoTableConsistency() throws IOException {
    WmoCodeFlagTables tables = WmoCodeFlagTables.getInstance();
    for (WmoTable table : tables.getWmoTables()) {
      if (table.getType() == TableType.param) {
        WmoParamTable paramTable = new WmoParamTable(table);
        for (GribTables.Parameter entry : paramTable.getParameters()) {
          assertThat(paramTable.getParameter(entry.getNumber()) != null);
        }
      } else if (table.getType() == TableType.code) {
        WmoCodeTable codeTable = new WmoCodeTable(table);
        for (Grib2CodeTableInterface.Entry entry : codeTable.getEntries()) {
          assertThat(codeTable.getEntry(entry.getCode()) != null);
        }
      } else if (table.getType() == TableType.flag) {
        WmoFlagTable flagTable = new WmoFlagTable(table);
        for (Grib2FlagTableInterface.Entry entry : flagTable.getEntries()) {
          assertThat(flagTable.getEntry(entry.getCode()) != null);
        }
      } else {
        assertThat(table.getType() == TableType.cat);
        assertThat(table.getId().startsWith("4.1."));
        WmoCodeTable codeTable = new WmoCodeTable(table);
        for (Grib2CodeTableInterface.Entry entry : codeTable.getEntries()) {
          assertThat(codeTable.getEntry(entry.getCode()) != null);
        }
      }
    }
  }

}
