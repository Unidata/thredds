/* Copyright Unidata */
package ucar.nc2.grib;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.grib.grib1.Grib1Index;
import ucar.nc2.grib.grib2.Grib2Index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test can read proto2 and proto3 gbx9
 *
 * @author caron
 * @since 11/28/2015.
 */
@RunWith(Parameterized.class)
public class TestGribIndexProtoVersions {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    result.add(new Object[]{"grib1.proto2.gbx9", true});
    //result.add(new Object[]{"grib1.proto3.gbx9", true});
    result.add(new Object[]{"grib1.proto3.syntax2.gbx9", true});
    result.add(new Object[]{"grib2.proto2.gbx9", false});
    //result.add(new Object[]{"grib2.proto3.gbx9", false});
    result.add(new Object[]{"grib2.proto3.syntax2.gbx9", false});
    return result;
  }

  String filename;
  boolean isGrib1;

  public TestGribIndexProtoVersions(String ds, boolean isGrib1) {
    this.filename = "../grib/src/test/data/index/" + ds;
    this.isGrib1 = isGrib1;
  }

  @Test
  public void testOpen() throws IOException {
    if (isGrib1) {
      Grib1Index reader = new Grib1Index();
      Assert.assertTrue(reader.readIndex(filename, -1, CollectionUpdateType.never));
    } else {
      Grib2Index reader = new Grib2Index();
      Assert.assertTrue(reader.readIndex(filename, -1, CollectionUpdateType.never));
    }
  }
}

