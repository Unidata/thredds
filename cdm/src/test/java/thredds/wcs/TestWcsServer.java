package thredds.wcs;

import junit.framework.*;
import java.io.IOException;
import java.io.File;

import ucar.nc2.NetcdfFile;

/** Test WCS server */

public class TestWcsServer extends TestCase {
  private String server = "http://localhost:8080/thredds/wcs/";
  private String server2 = "http://motherlode.ucar.edu:8080/thredds/wcs/";

  private String ncdcWcsServer = "http://eclipse.ncdc.noaa.gov:9090/thredds/wcs/";
  private String ncdcWcsDataset = "http://eclipse.ncdc.noaa.gov:9090/thredds/wcs/gfsmon/largedomain.nc";
  private String ncdcOpendapDataset = "http://eclipse.ncdc.noaa.gov:9090/thredds/dodsC/gfsmon/largedomain.nc";

  public TestWcsServer( String name) {
    super(name);
  }

  public void testPfeg() throws IOException {
    String dataset = "http://localhost:8080/thredds/wcs/satellite/AT/ssta/1day?";
    //String url = "http://oceanwatch.pfeg.noaa.gov:8081/thredds/wcs/satellite/AG/ssta/14day?request=GetCoverage&version=1.0.0&service=WCS&format=GeoTIFF&coverage=AGssta&Vertical=.0&time=2006-01-09T23:59:59Z&bbox=220,20,250,50";
    //String dataset = "http://oceanwatch.pfeg.noaa.gov:8081/thredds/wcs/satellite/AG/ssta/14day?";
    //String contents = thredds.util.IO.readURLcontentsWithException( dataset+"?request=GetCapabilities&version=1.0.0&service=WCS");
    //System.out.println(dataset+" response length= "+ contents.length()+"\n"+contents);

    showGetCapabilities(dataset);
    showDescribeCoverage(dataset, "ATssta");
    showGetCoverage(dataset, "ATssta", "2006-01-04T23:59:59Z",null,"220,20,250,50");
  }


  public void eTestForEthan() throws IOException
  {
    showGetCapabilities( ncdcWcsDataset + "?" );
    showGetCapabilities( server2 + "?dataset=" + ncdcOpendapDataset + "&" );
    //showDescribeCoverage( ncdsWcsDataset , "ssta" );
    //showGetCoverage( ncdsWcsDataset , "ssta",
    //                 "2005-06-24T00:00:00Z", null, null );
    //showGetCoverage(ncdsWcsDataset, "u_wind", "2002-12-02T22:00:00Z", "100.0", "-134,11,-47,57.555");
  }

  public void utestGC() throws IOException {
    testGC("testdata/ocean.nc");
    testGC("testdata/eta.nc");
    testGC("testdata/RUC.nc");
    testGC("testdata/sst.nc");
    testGC("testdata/striped.nc");
  }

  private void testGC(String dataset) throws IOException {
    String url = server+dataset+"?request=GetCapabilities&version=1.0.0&service=WCS";
    String contents = thredds.util.IO.readURLcontentsWithException( url);
    System.out.println(url+" is OK, len= "+ contents.length());
  }

  public void testShow1() throws IOException {

    // "http://localhost:8080/thredds/wcs/galeon/ocean.nc?request=GetCapabilities&version=1.0.0&service=WCS?request=GetCapabilities&version=1.0.0&service=WCS
    showGetCapabilities(server+"galeon/ocean.nc?");
    showDescribeCoverage(server+"galeon/ocean.nc?", "u_sfc");
    showGetCoverage(server+"galeon/ocean.nc?", "u_sfc", "2005-03-17T12:00:00Z", null, "-100,20,-50,44.40");
  }

  public void testDatasetParam() throws IOException {
     showGetCapabilities(server+"?dataset=http://localhost:8080/thredds/dodsC/testContent/testData.nc&");
     showDescribeCoverage(server+"?dataset=http://localhost:8080/thredds/dodsC/testContent/testData.nc&", "Z_sfc");
     showGetCoverage(server+"?dataset=http://localhost:8080/thredds/dodsC/testContent/testData.nc&", "Z_sfc",
         "2003-09-25T00:00:00Z", null, "-100,20,-50,44.40");
  }

  private void showGetCapabilities(String url) throws IOException {
    showRead(url+"request=GetCapabilities&version=1.0.0&service=WCS");
  }

  private void showDescribeCoverage(String url, String grid) throws IOException {
    showRead(url+"request=DescribeCoverage&version=1.0.0&service=WCS&coverage="+grid);
  }

  private void showGetCoverage(String url, String grid, String time, String vert, String bb) throws IOException {
    String getURL = url+"request=GetCoverage&version=1.0.0&service=WCS&format=NetCDF3&coverage="+grid;
    if (time != null)
      getURL = getURL + "&time="+time;
    if (vert != null)
      getURL = getURL + "&vertical="+vert;
    if (bb != null)
      getURL = getURL + "&bbox="+bb;

    System.out.println("req= "+getURL);
    File file = new File("C:/TEMP/"+grid+".nc");
    thredds.util.IO.readURLtoFile(getURL, file);
    System.out.println(" copied contents to "+file.getPath());

    NetcdfFile ncfile = NetcdfFile.open(file.getPath());
    assert ncfile != null;
    //showRead( getURL);
  }

  public void testReq() {
    String server = "http://motherlode.ucar.edu:8080/";
    String req = server+"thredds/wcs/testdata/sst.nc?SERVICE=WCS&" +
       "VERSION=1.0.0&REQUEST=GetCoverage&COVERAGE=tos&CRS=EPSG%3a4326&BBOX=1,-79.5,359,89.5"+
       "&TIME=2002-12-07T00:00:00Z&FORMAT=GeoTIFF&EXCEPTIONS=application/vnd.ogc.se_xml";
    System.out.println("req= "+req);

    File file = new File("C:/TEMP/martin.tiff");
    thredds.util.IO.readURLtoFile(req, file);
    System.out.println(" copied contents to "+file.getPath());
  }


  private void showRead(String url) throws IOException {
    System.out.println("****************\n");
    System.out.println(url+"\n");
    String contents = thredds.util.IO.readURLcontentsWithException( url);
    System.out.println(contents);
  }

}
