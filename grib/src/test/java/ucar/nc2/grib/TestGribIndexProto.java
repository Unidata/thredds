/* Copyright Unidata */
package ucar.nc2.grib;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.grib.grib1.Grib1Index;
import ucar.nc2.grib.grib2.Grib2Index;

import java.util.ArrayList;
import java.util.List;

/**
 * Test can read both proto2 and proto3 gbx9.
 * Also test failure mode for invalid proto.
 *
 * @author caron
 * @since 11/28/2015.
 */
@RunWith(Parameterized.class)
public class TestGribIndexProto {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    result.add(new Object[]{"grib1.proto2.gbx9", true, false});
    result.add(new Object[]{"grib1.proto3.gbx9", true, true});  // fails
    result.add(new Object[]{"grib1.proto3.syntax2.gbx9", true, false});
    result.add(new Object[]{"grib2.proto2.gbx9", false, false});
    result.add(new Object[]{"grib2.proto3.gbx9", false, true});  // fails
    result.add(new Object[]{"grib2.proto3.syntax2.gbx9", false, false});
    return result;
  }

  String filename;
  boolean isGrib1;
  boolean fail;

  public TestGribIndexProto(String ds, boolean isGrib1, boolean fail) {
    this.filename = "../grib/src/test/data/index/" + ds;
    this.isGrib1 = isGrib1;
    this.fail = fail;
  }

  @Test
  public void testOpen() {
    try {
      if (isGrib1) {
        Grib1Index reader = new Grib1Index();
        boolean ok = reader.readIndex(filename, -1, CollectionUpdateType.never);
        assertTrue(ok || fail);
      } else {
        Grib2Index reader = new Grib2Index();
        boolean ok = reader.readIndex(filename, -1, CollectionUpdateType.never);
        assertTrue(ok || fail);
      }
    } catch (Exception e) {
      fail("Exception should not be thrown");
    }
  }
}

