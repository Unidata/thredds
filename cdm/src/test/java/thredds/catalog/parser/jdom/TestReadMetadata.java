package thredds.catalog.parser.jdom;

import junit.framework.*;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDatasetImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;

import ucar.nc2.util.Misc;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestReadMetadata extends TestCase
{

  public TestReadMetadata( String name )
  {
    super( name );
  }

  /**
   * Test ...
   */
  public void testReadDatasetWithDataSize() throws IOException {
    double sizeKb = 439.78;
    double sizeBytes = 439780;
    StringBuilder catAsString = new StringBuilder()
            .append( "<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\"\n" )
            .append( "         xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" )
            .append( "         name=\"Catalog for TestReadMetadata.testReadDatasetWithDataSize()\"\n" )
            .append( "         version=\"1.0.1\">\n" )
            .append( "  <service name=\"gridded\" serviceType=\"OPENDAP\" base=\"/thredds/dodsC/\" />\n" )
            .append( "  <dataset name=\"Atmosphere\" ID=\"cgcm3.1_t47_atmos\" serviceName=\"gridded\">\n" )
            .append( "     <metadata inherited=\"false\">\n" )
            .append( "       <dataSize units=\"Kbytes\">").append( sizeKb ).append( "</dataSize>\n" )
            .append( "     </metadata>\n" )
            .append( "  </dataset>\n" )
            .append( "</catalog>" );
    String catUri = "Cat.TestReadMetadata.testReadDatasetWithDataSize";

    URI catURI = null;
    try
    {
      catURI = new URI( catUri );
    }
    catch ( URISyntaxException e )
    {
      fail( "URISyntaxException: " + e.getMessage() );
      return;
    }

    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
    InvCatalogImpl cat = fac.readXML( catAsString.toString(), catURI );

    InvDatasetImpl ds = (InvDatasetImpl) cat.getDatasets().get( 0 );
    double d = ds.getDataSize();

    fac.writeXML(cat, System.out);
    assertTrue( "Size of data <" + d + "> not as expected <" + sizeBytes + ">.", Misc.closeEnough(d, sizeBytes));
  }
}
