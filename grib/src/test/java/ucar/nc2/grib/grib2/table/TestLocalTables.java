package ucar.nc2.grib.grib2.table;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.nc2.grib.GribTables;

@RunWith(JUnit4.class)
public class TestLocalTables {

  @Test
  public void testLocalTables() throws IOException {
    ImmutableList<Grib2Tables> tables = Grib2Tables.getAllRegisteredTables();
    for (Grib2Tables t : tables) {
      assertThat(t.getParameters()).isNotEmpty();
      for (GribTables.Parameter p : t.getParameters()) {
        assertThat(p.getName()).isNotEmpty();
        assertThat(p.getId()).isNotEmpty();
      }
    }
  }

  @Test
  public void testKmaTable() {
    Grib2Tables kma = Grib2Tables.factory(40,-1,-1,-1,-1);
    assertThat(kma).isNotNull();
    assertThat(kma.getType()).isEqualTo(Grib2TablesId.Type.kma);
    assertThat(kma.getParameters()).isNotEmpty();
    for (GribTables.Parameter p : kma.getParameters())
      System.out.printf("%s%n", p);
  }
}