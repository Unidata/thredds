/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.dt.grid;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

/** Check opening dods datasets - latest dataset from thredds */

@RunWith(Parameterized.class)
@Category(NeedsExternalResource.class)
public class TestReadAndCountDods {
  static String base = "thredds:resolve:http://"+ TestDir.threddsTestServer+"/thredds/";

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    // Grib files, one from each model
    result.add(new Object[]{"catalog/grib/NCEP/DGEX/CONUS_12km/files/latest.xml", 23, 11, 13, 8});
    // Geopotential_height_surface put out 6 and 18Z, ngrids osc between 22 and 23
    result.add(new Object[]{"catalog/grib/NCEP/DGEX/Alaska_12km/files/latest.xml", -1, 11, 13, 8});

    result.add(new Object[]{"catalog/grib/NCEP/GEFS/Global_1p0deg_Ensemble/members/latest.xml", 35, 13, 13, 7});
    result.add(new Object[]{"grib/NCEP/GEFS/Global_1p0deg_Ensemble/derived/latest.xml", 70, 13, 12, 7}); // 63, 15, 14, 6});

    // 133, 26, 27, 21 vs 133, 31, 29, 21
    result.add(new Object[]{"catalog/grib/NCEP/GFS/Global_0p5deg/files/latest.xml", 135, -1, -1, 19});
    result.add(new Object[]{"catalog/grib/NCEP/GFS/Global_onedeg/files/latest.xml", 135, 26, 27, 19});
    result.add(new Object[]{"catalog/grib/NCEP/GFS/Global_2p5deg/files/latest.xml", 135, 24, 25, 19});
    // flipping between 23, 9, 11, 6 and 23, 12, 14, 6
    result.add(new Object[]{"catalog/grib/NCEP/GFS/N_Hemisphere_381km/files/latest.xml", 23, -1, -1, 6});
    // flipping between 50, 14, 15, 8 and  50, 11, 13, 8
    result.add(new Object[]{"catalog/grib/NCEP/GFS/Pacific_40km/files/latest.xml", 50, -1, -1, 8});
    result.add(new Object[]{"catalog/grib/NCEP/GFS/Puerto_Rico_0p5deg/files/latest.xml", 50, 11, 13, 8});
    result.add(new Object[]{"catalog/grib/NCEP/GFS/CONUS_80km/files/latest.xml", 31, 12, 15, 8});

    // flipping 59, 15, 17, 13,  and 59, 15, 18, 13,
    result.add(new Object[]{"catalog/grib/NCEP/NAM/Alaska_11km/files/latest.xml", 59, 15, 18, 13});
    // flipping between 154, 34, 36, 31 and 154, 33, 35, 31, and 156, 34, 37, 31
    result.add(new Object[]{"catalog/grib/NCEP/NAM/Alaska_45km/conduit/files/latest.xml", -1, -1, -1, 31});
    result.add(new Object[]{"catalog/grib/NCEP/NAM/CONUS_12km/files/latest.xml", 59, 15, 17, 13});

    // flipping between  176, 29, 31, 25 and 178, 29, 32, 25
    result.add(new Object[]{"catalog/grib/NCEP/NAM/CONUS_40km/conduit/files/latest.xml", -1, 29, -1, 25});
    result.add(new Object[]{"catalog/grib/NCEP/NAM/Polar_90km/files/latest.xml", 133, 28, 30, 25});
    result.add(new Object[]{"catalog/grib/NCEP/NAM/Polar_90km/files/latest.xml", 133, 28, 30, 25});

    result.add(new Object[]{"catalog/grib/NCEP/NAM/Alaska_22km/files/latest.xml", 25, 8, 10, 6});
    result.add(new Object[]{"catalog/grib/NCEP/NAM/Alaska_45km/noaaport/files/latest.xml", 21, 6, 8, 4});
    // flipping between 29, 12, 14, 9, and 29, 15, 15, 9
    result.add(new Object[]{"catalog/grib/NCEP/NAM/Alaska_95km/files/latest.xml", 29, -1, -1, 9});
    result.add(new Object[]{"catalog/grib/NCEP/NAM/CONUS_20km/noaaport/files/latest.xml", 33, 9, 11, 7});   // ngrids keeps bouning between 33 and 40
    result.add(new Object[]{"catalog/grib/NCEP/NAM/CONUS_80km/files/latest.xml", 41, 11, 13, 8});

    result.add(new Object[]{"catalog/grib/NCEP/RAP/CONUS_13km/files/latest.xml", 53, 12, 14, 9});
    result.add(new Object[]{"catalog/grib/NCEP/RAP/CONUS_20km/files/latest.xml", 89, 18, 20, 14});
    result.add(new Object[]{"catalog/grib/NCEP/RAP/CONUS_40km/files/latest.xml", 89, 18, 20, 14});
    return result;
  }

  @Parameterized.Parameter(value = 0)
  public String name;

  @Parameterized.Parameter(value = 1)
  public int ngrids;

  @Parameterized.Parameter(value = 2)
  public int ncoordSys;

  @Parameterized.Parameter(value = 3)
  public int ncoordAxes;

  @Parameterized.Parameter(value = 4)
  public int nVertCooordAxes;

  @Test
  public void readAndCount() throws Exception {
    TestReadandCount.doOne(base, name, ngrids, ncoordSys, ncoordAxes, nVertCooordAxes);
  }
}
