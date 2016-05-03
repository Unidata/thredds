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

package thredds.motherlode;

import java.io.IOException;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

/**
 * Run sanity check on motherlode
 *
 * @author caron
 */
@Category(NeedsExternalResource.class)
public class TestMotherlodePing {

  public static String server = "http://"+TestDir.threddsTestServer+"/thredds";

  static void ping(String url) {
    try {
      IO.readURLcontentsWithException(server + url);
      System.out.printf("ping OK %s%n", server+url);
    } catch (IOException e) {
      System.out.printf("FAIL ON %s error=%n  %s%n", server+url, e.getMessage());
    }
  }

  @Test
  public void ping() throws Exception {
    ping("/ncss/nws/metar/ncdecoded/Metar_Station_Data_fc.cdmr/dataset.html");
    ping("/ncss/nws/metar/ncdecoded/Metar_Station_Data_fc.cdmr/dataset.xml");
    ping("/ncss/grib/NCEP/NAM/CONUS_80km/best/dataset.html");
    ping("/ncss/grib/NCEP/NAM/CONUS_80km/best/dataset.xml");

    ping("/wms/grib/NCEP/GFS/Global_2p5deg/best?REQUEST=GetCapabilities&VERSION=1.3.0&SERVICE=WMS");
    ping("/radarServer/nexrad/level3/IDD?north=50.00&south=20.00&west=-127&east=-66&time=present&var=NST");
  }
}
