package ucar.nc2.grib.grib2.table;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.nc2.grib.grib2.table.WmoTemplateTables.Field;
import ucar.nc2.grib.grib2.table.WmoTemplateTables.TemplateTable;

@RunWith(JUnit4.class)
  public class TestWmoTemplateTables {

  @Test
  public void testWmoTableConsistency() throws IOException {
    WmoTemplateTables tables = WmoTemplateTables.getInstance();
    for (TemplateTable table : tables.getTemplateTables()) {
      for (Field fld : table.getFlds()) {
        assertThat(fld.getContent() != null);
      }
    }
  }
}
