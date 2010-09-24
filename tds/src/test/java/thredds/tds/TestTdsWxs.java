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
package thredds.tds;

import junit.framework.*;

import thredds.catalog.*;
import ucar.nc2.Attribute;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.dataset.*;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NCdump;
import ucar.nc2.util.IO;
import ucar.nc2.constants.FeatureType;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.io.File;
import java.util.Formatter;

public class TestTdsWxs extends TestCase {

  public TestTdsWxs( String name) {
    super(name);
  }

  public void testWcs() throws IOException {
    showGetCapabilities("http://localhost:8080/thredds/wcs/aggNewTest/SUPER-NATIONAL_8km_WV.gini");
    showDescribeCoverage("http://localhost:8080/thredds/wcs/aggNewTest/SUPER-NATIONAL_8km_WV.gini", "IR_WV");
    showGetCoverage("http://localhost:8080/thredds/wcs/aggNewTest/SUPER-NATIONAL_8km_WV.gini", "IR_WV",
            "2000-06-16T07:00:00Z", null, null);
  }

  private void showGetCapabilities(String url) throws IOException {
    showRead(url+"?request=GetCapabilities&version=1.0.0&service=WCS");
  }

  private void showDescribeCoverage(String url, String grid) throws IOException {
    showRead(url+"?request=DescribeCoverage&version=1.0.0&service=WCS&coverage="+grid);
  }

  private void showGetCoverage(String url, String grid, String time, String vert, String bb) throws IOException {
    String getURL = url+"?request=GetCoverage&version=1.0.0&service=WCS&format=NetCDF3&coverage="+grid;
    if (time != null)
      getURL = getURL + "&time="+time;
    if (vert != null)
      getURL = getURL + "&vertical="+vert;
    if (bb != null)
      getURL = getURL + "&bbox="+bb;

    File file = new File("C:/TEMP/"+grid+"3.nc");
    IO.readURLtoFile(getURL, file);
    System.out.println(" copied contents to "+file.getPath());
  }

  private void showRead(String url) throws IOException {
    System.out.println("****************\n");
    System.out.println(url+"\n");
    String contents = IO.readURLcontentsWithException( url);
    System.out.println(contents);
  }



}