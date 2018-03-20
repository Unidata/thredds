/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.lang.invoke.MethodHandles;

/**
 * Describe
 *
 * @author caron
 * @since 3/11/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCreationOptions {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testTimeUnitOption() throws Exception {
    String config = TestDir.cdmTestDataDir + "ucar/nc2/grib/collection/hrrrConus3surface.xml";
    GribCdmIndex.main(new String[] {"--featureCollection", config} );

    /*
<featureCollection xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
                   name="GSD HRRR CONUS 3km surface" featureType="GRIB2" harvest="true" path="grib/HRRR/CONUS_3km/surface">

  <collection name="GSD_HRRR_CONUS_3km_surface"
              spec="${cdmUnitTest}/gribCollections/hrrr/HRRR_CONUS_3km_20141010_0000.grib2"
              timePartition="file"
              dateFormatMark="#HRRR_CONUS_3km_surface_#yyyyMMddHHmm"
              olderThan="5 min"/>

  <tdm rewrite="test" rescan="0 0/15 * * * ? *"/>
  <gribConfig>
    <option name="timeUnit" value="1 minute" />
  </gribConfig>
</featureCollection>
     */

    String dataset = TestDir.cdmUnitTestDir + "gribCollections/hrrr/DewpointTempFromGsdHrrrrConus3surface.grib2";
    try (NetcdfDataset ds = NetcdfDataset.openDataset(dataset)) {
      Variable v = ds.findVariable("Dewpoint_temperature_height_above_ground");
      Assert.assertNotNull("Dewpoint_temperature_height_above_ground", v);
      Dimension d = v.getDimension(0);
      Assert.assertEquals(57, d.getLength());
    }

  }

}
