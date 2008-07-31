package thredds.catalog2.xml.parser.sax;

import junit.framework.*;
import thredds.catalog2.xml.parser.CatalogParserFactory;
import thredds.catalog2.xml.parser.CatalogParser;
import thredds.catalog2.xml.parser.CatalogParserException;
import thredds.catalog2.xml.parser.CatalogNamespace;
import thredds.util.UriResolver;

import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Schema;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Source;
import java.io.StringReader;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.xml.sax.SAXException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestCatalogParser extends TestCase
{

//  private CatalogParser me;

  public TestCatalogParser( String name )
  {
    super( name );
  }

  public void test1()
  {
    Schema schema = null;
    try
    {
      schema = getSchema();
    }
    catch ( SAXException e )
    {
      fail( "Failed to read catalog schema: " + e.getMessage());
    }
    catch ( IOException e )
    {
      fail( "Failed to read catalog schema: " + e.getMessage() );
    }

    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware( true );
    if ( schema != null )
      factory.setSchema( schema );
    SAXParser parser = null;
    try
    {
      parser = factory.newSAXParser();
    }
    catch ( ParserConfigurationException e )
    {
      fail( "Failed to get SAXParser: " + e.getMessage() );
    }
    catch ( SAXException e )
    {
      fail( "Failed to get SAXParser: " + e.getMessage() );
    }

    CatalogDefaultHandler catHandler = new CatalogDefaultHandler();


    String catUriString = "http://newmotherlode.ucar.edu:8080/thredds/catalog/nexrad/composite/gini/ntp/4km/20080731/catalog.xml";
    //String catUriString = "http://newmotherlode.ucar.edu:8080/thredds/catalog.xml";
    URI catUri = null;
    try
    {
      catUri = new URI( catUriString );
    }
    catch ( URISyntaxException e )
    {
      fail( "Bad syntax in catalog URI [" + catUriString + "]: " + e.getMessage() );
    }
    try
    {
      parser.parse( catUri.toString(), catHandler );
      //parser.parse( getInputStream( catUri), catHandler );
    }
    catch ( SAXException e )
    {
      fail( "Failed to parse catalog [" + catUriString + "]: " + e.getMessage() );
    }
    catch ( IOException e )
    {
      fail( "Failed to read catalog [" + catUriString + "]: " + e.getMessage() );
    }


  }

  private InputStream getInputStream( URI uri )
          throws IOException
  {
    UriResolver uriResolver = UriResolver.newDefaultUriResolver();
    return uriResolver.getInputStream( uri );
  }

  private Schema getSchema()
          throws IOException, SAXException
  {
    SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
    URI catSchemaUri = CatalogNamespace.CATALOG_1_0.getResourceUri();
    StreamSource source = new StreamSource( getInputStream( catSchemaUri ));
    source.setSystemId( catSchemaUri.toString() );
    return schemaFactory.newSchema( source );
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

    CatalogParserFactory cpf = CatalogParserFactory.getInstance();
    cpf.setValidating( true );
    CatalogParser cp = cpf.getCatalogParser();
    URI baseUri = null;
    try
    {
      baseUri = new URI( "http://test.catalog.parser/cat.xml" );
    }
    catch ( URISyntaxException e )
    {
      fail();
    }
    try
    {
      cp.parse( new StringReader( sb.toString() ), baseUri);
    }
    catch ( CatalogParserException e )
    {
      e.printStackTrace();
      fail();
    }
//    me = new CatalogParser( );
//    assertTrue( me != null );
  }
}
