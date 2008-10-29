package thredds.catalog2.xml.parser;

import junit.framework.*;
import thredds.catalog2.xml.parser.ThreddsXmlParser;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.parser.stax.StaxThreddsXmlParser;
import thredds.catalog2.xml.writer.ThreddsXmlWriterFactory;
import thredds.catalog2.xml.writer.ThreddsXmlWriter;
import thredds.catalog2.xml.writer.ThreddsXmlWriterException;
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

  public void testCatalog()
  {
    DocumentSource docSource = getMainTestCatalog();

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    Catalog cat;
    try
    {
      cat = cp.parse( new StringReader( docSource.getDocAsString() ), docSource.getDocBaseUri());
    }
    catch ( ThreddsXmlParserException e )
    {
      fail( "Failed to parse catalog: " + e.getMessage());
      return;
    }

    if ( cat != null)
    {
      String catName = "Unidata THREDDS Data Server";
      assertTrue( "Catalog name [" + cat.getName() + "] not as expected [" + catName + "].",
                  cat.getName().equals( catName ) );

      ThreddsXmlWriter txw = ThreddsXmlWriterFactory.newInstance().createThreddsXmlWriter();
      try
      {
        txw.writeCatalog( cat, System.out );
      }
      catch ( ThreddsXmlWriterException e )
      {
        e.printStackTrace();
        fail( "Failed writing catalog to sout: " + e.getMessage());
      }
    }
  }

  public void testThreddsMetadata()
  {
    DocumentSource docSource = getTestThreddsMetadata();

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    Catalog cat;
    try
    {
      cat = cp.parse( new StringReader( docSource.getDocAsString() ), docSource.getDocBaseUri());
    }
    catch ( ThreddsXmlParserException e )
    {
      fail( "Failed to parse catalog: " + e.getMessage());
      return;
    }

    if ( cat != null)
    {
      String catName = "Unidata THREDDS Data Server";
      assertTrue( "Catalog name [" + cat.getName() + "] not as expected [" + catName + "].",
                  cat.getName().equals( catName ) );

      ThreddsXmlWriter txw = ThreddsXmlWriterFactory.newInstance().createThreddsXmlWriter();
      try
      {
        txw.writeCatalog( cat, System.out );
      }
      catch ( ThreddsXmlWriterException e )
      {
        e.printStackTrace();
        fail( "Failed writing catalog to sout: " + e.getMessage());
      }
    }
  }

  private DocumentSource getMainTestCatalog()
  {
    StringBuilder sb = new StringBuilder( "<?xml version='1.0' encoding='UTF-8'?>\n" )
            .append( "<catalog xmlns='http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0'" )
            .append( " xmlns:xlink='http://www.w3.org/1999/xlink'" )
            .append( " name='Unidata THREDDS Data Server' version='1.0.1'>\n" )
            .append( "  <service name='all' serviceType='Compound' base=''>\n" )
            .append( "    <service name='odap' serviceType='OPENDAP' base='/thredds/dodsC/' />\n" )
            .append( "    <service name='wcs' serviceType='WCS' base='/thredds/wcs/'>\n" )
            .append( "      <property name='someInfo' value='good stuff' />\n" )
            .append( "    </service>" )
            .append( "  </service>" )
            .append( "  <property name='moreInfo' value='more good stuff' />\n" )
            .append( "  <dataset name='fred'>" )
            .append( "    <access urlPath='fred.nc' serviceName='odap' />" )
            .append( "  </dataset>" )
            .append( "  <dataset name='fred2' serviceName='odap' urlPath='fred2.nc' />" )
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

    URI docBaseUri;
    String docBaseUriString = "http://test.catalog.parser/cat.xml";
    try
    {
      docBaseUri = new URI( docBaseUriString );
    }
    catch ( URISyntaxException e )
    {
      fail( "Problem with URI [" + docBaseUriString + "] syntax." );
      return null;
    }

    return new DocumentSource( docBaseUri, sb.toString());
  }

  private DocumentSource getTestThreddsMetadata()
  {
    StringBuilder sb = new StringBuilder( "<?xml version='1.0' encoding='UTF-8'?>\n" )
            .append( "<metadata xmlns='http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0'" )
            .append( "          xmlns:xlink='http://www.w3.org/1999/xlink'>\n" )
            .append( "  <serviceName>odap</serviceName>\n")
            .append( "</metadata>" );

    URI docBaseUri;
    String docBaseUriString = "http://test.catalog.parser/threddsMetadata.xml";
    try
    {
      docBaseUri = new URI( docBaseUriString );
    }
    catch ( URISyntaxException e )
    {
      fail( "Problem with URI [" + docBaseUriString + "] syntax." );
      return null;
    }

    return new DocumentSource( docBaseUri, sb.toString() );
  }

  private class DocumentSource
  {
    private final URI docBaseUri;
    private final String docAsString;

    DocumentSource( URI docBaseUri, String docAsString )
    {
      this.docBaseUri = docBaseUri;
      this.docAsString = docAsString;
    }
    URI getDocBaseUri() { return this.docBaseUri; }
    String getDocAsString() { return this.docAsString; }
  }
}