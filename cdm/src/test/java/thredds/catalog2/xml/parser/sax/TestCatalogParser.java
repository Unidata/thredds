package thredds.catalog2.xml.parser.sax;

import junit.framework.*;
import thredds.catalog2.xml.parser.ThreddsXmlParserFactory;
import thredds.catalog2.xml.parser.ThreddsXmlParser;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.Catalog;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestCatalogParser extends TestCase
{

//  private ThreddsXmlParser me;

  public TestCatalogParser( String name )
  {
    super( name );
  }

  /**
   * Test ...
   */
  public void testOne()
  {
    StringBuilder sb = new StringBuilder( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            .append( "<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\"")
            .append( " xmlns:xlink=\"http://www.w3.org/1999/xlink\"")
            .append( " name=\"Unidata THREDDS Data Server\" version=\"1.0.1\">\n" )
            .append( "  <service name=\"thisDODS\" serviceType=\"OPENDAP\" base=\"/thredds/dodsC/\" />\n" )
            .append( "  <dataset name=\"Realtime data from IDD\">\n" )
            .append( "    <catalogRef xlink:href=\"idd/models.xml\" xlink:title=\"NCEP Model Data\" name=\"\" />\n" )
            .append( "    <catalogRef xlink:href=\"idd/radars.xml\" xlink:title=\"NEXRAD Radar\" name=\"\" />\n" )
            .append( "    <catalogRef xlink:href=\"idd/obsData.xml\" xlink:title=\"Station Data\" name=\"\" />\n" )
            .append( "    <catalogRef xlink:href=\"idd/satellite.xml\" xlink:title=\"Satellite Data\" name=\"\" />\n" )
            .append( "  </dataset>\n" )
            .append( "  <dataset name=\"Other Unidata Data\">\n" )
            .append( "\n" )
            .append( "    <catalogRef xlink:href=\"idd/rtmodel.xml\" xlink:title=\"Unidata Real-time Regional Model\" name=\"\" />\n" )
            .append( "    <catalogRef xlink:href=\"galeon/catalog.xml\" xlink:title=\"Unidata GALEON Experimental Web Coverage Service (WCS) datasets\" name=\"\" />\n" )
            .append( "    <dataset name=\"Test Restricted Dataset\" ID=\"testRestrictedDataset\" urlPath=\"restrict/testData.nc\" restrictAccess=\"tiggeData\">\n" )
            .append( "      <serviceName>thisDODS</serviceName>\n" )
            .append( "      <dataType>Grid</dataType>\n" )
            .append( "    </dataset>\n" )
            .append( "  </dataset>\n" )
            .append( "</catalog>" );

    ThreddsXmlParser cp = ThreddsXmlParserFactory.newCatalogParser( true);
    URI baseUri;
    String baseUriString = "http://test.catalog.parser/cat.xml";
    try
    {
      baseUri = new URI( baseUriString );
    }
    catch ( URISyntaxException e )
    {
      fail( "Problem with URI [" + baseUriString + "] syntax.");
      return;
    }

    Catalog cat;
    try
    {
      cat = cp.parse( new StringReader( sb.toString() ), baseUri);
    }
    catch ( ThreddsXmlParserException e )
    {
      fail( "Failed to parse catalog: " + e.getMessage());
      return;
    }

    String catName = "Unidata THREDDS Data Server";
    assertTrue( "Catalog name [" + cat.getName() + "] not as expected [" + catName + "].",
                cat.getName().equals( catName ) );
  }
}
