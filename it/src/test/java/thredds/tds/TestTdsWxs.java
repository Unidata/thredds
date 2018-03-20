/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.tds;

import junit.framework.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import ucar.nc2.util.IO;

import java.io.IOException;
import java.io.File;
import java.lang.invoke.MethodHandles;

public class TestTdsWxs extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestTdsWxs( String name) {
    super(name);
  }

  public void testWcs() throws IOException {
    showGetCapabilities(TestOnLocalServer.withHttpPath("wcs/localContent/SUPER-NATIONAL_1km_CTP_20140105_2300.gini"));
    showDescribeCoverage(TestOnLocalServer.withHttpPath("wcs/localContent/SUPER-NATIONAL_1km_CTP_20140105_2300.gini"), "CTP");
    showGetCoverage(TestOnLocalServer.withHttpPath("wcs/localContent/SUPER-NATIONAL_1km_CTP_20140105_2300.gini"), "CTP",
            "2014-01-05T23:00:00Z", null, null);
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
