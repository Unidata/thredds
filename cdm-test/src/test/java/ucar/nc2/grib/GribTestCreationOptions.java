/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.grib;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

/**
 * Describe
 *
 * @author caron
 * @since 3/11/2015
 */
@Category(NeedsCdmUnitTest.class)
public class GribTestCreationOptions {
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
