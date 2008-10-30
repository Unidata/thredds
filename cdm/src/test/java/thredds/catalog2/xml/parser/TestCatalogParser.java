package thredds.catalog2.xml.parser;

import junit.framework.*;
import thredds.catalog2.xml.parser.ThreddsXmlParser;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.parser.stax.StaxThreddsXmlParser;
import thredds.catalog2.xml.writer.ThreddsXmlWriterFactory;
import thredds.catalog2.xml.writer.ThreddsXmlWriter;
import thredds.catalog2.xml.writer.ThreddsXmlWriterException;
import thredds.catalog2.Catalog;
import thredds.catalog2.Metadata;

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

  // 1) dataset with urlPath and serviceName attributes
  // 2) dataset with urlPath att and serviceName child element
  // 3) dataset with urlPath att and child metadata element with child serviceName element
  // 4) same as 3 but metadata element has inherited="true" attribute
  // 5) same as 3 but metadata element is in dataset that is parent to dataset with urlPath
  // 6) 1-5 where serviceName points to single top-level service
  // 7) 1-5 where serviceName points to compound service
  // 8) 1-5 where serviceName points to a single service contained in a compound service

  public void testCatalogSingleDatasetAccessAttributes()
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/TestCatalogParser/testCatalogSingleDatasetAccessAttributes.xml";

    StringBuilder doc = new StringBuilder( "<?xml version='1.0' encoding='UTF-8'?>\n" )
            .append( "<catalog xmlns='http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0'" )
            .append( " xmlns:xlink='http://www.w3.org/1999/xlink'" )
            .append( " version='1.0.1'>\n" )
            .append( "  <service name='odap' serviceType='OPENDAP' base='/thredds/dodsC/' />\n" )
            .append( "  <dataset name='Test1' urlPath='test/test1.nc' serviceName='odap' />" )
            .append( "</catalog>" );

    Catalog cat = this.parseCatalog( doc.toString(), docBaseUriString );

    // ToDo some tests

    writeCatalogXml( cat );
  }

  public void testCatalog()
  {
    String docBaseUriString = "http://test/thredds/catalog2/xml/parser/TestCatalogParser/testCatalog.xml";

    StringBuilder doc = new StringBuilder( "<?xml version='1.0' encoding='UTF-8'?>\n" )
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
            .append( "      <serviceName>odap</serviceName>\n" )
            .append( "      <dataType>Grid</dataType>\n" )
            .append( "    </dataset>\n" )
            .append( "  </dataset>\n" )
            .append( "</catalog>" );

    Catalog cat = this.parseCatalog( doc.toString(), docBaseUriString );

    String catName = "Unidata THREDDS Data Server";
    assertTrue( "Catalog name [" + cat.getName() + "] not as expected [" + catName + "].",
                cat.getName().equals( catName ) );
    // ToDo More testing.

    writeCatalogXml( cat );
  }

  public void testThreddsMetadata()
  {
    String docBaseUriString = "http://test.catalog.parser/threddsMetadata.xml";

    StringBuilder doc = new StringBuilder( "<?xml version='1.0' encoding='UTF-8'?>\n" )
            .append( "<metadata xmlns='http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0'" )
            .append( "          xmlns:xlink='http://www.w3.org/1999/xlink'>\n" )
            .append( "  <serviceName>odap</serviceName>\n" )
            .append( "</metadata>" );

    Metadata md = this.parseMetadata( doc.toString(), docBaseUriString );

    assertTrue( md.isContainedContent());

    this.writeMetadataXml( md );
  }

  private Catalog parseCatalog( String docAsString, String docBaseUriString )
  {
    URI docBaseUri;
    try
    { docBaseUri = new URI( docBaseUriString ); }
    catch ( URISyntaxException e )
    { fail( "Syntax problem with URI [" + docBaseUriString + "]." ); return null; }

    Catalog cat;
    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    try
    { cat = cp.parse( new StringReader( docAsString ), docBaseUri ); }
    catch ( ThreddsXmlParserException e )
    { fail( "Failed to parse catalog: " + e.getMessage() ); return null; }

    assertNotNull( "Result of parse was null catalog [" + docBaseUriString + "].",
                   cat );
    return cat;
  }

  private Metadata parseMetadata( String docAsString, String docBaseUriString )
  {
    URI docBaseUri;
    try
    { docBaseUri = new URI( docBaseUriString ); }
    catch ( URISyntaxException e )
    { fail( "Syntax problem with URI [" + docBaseUriString + "]." ); return null; }

    Metadata md;
    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    try
    { md = cp.parseMetadata( new StringReader( docAsString ), docBaseUri ); }
    catch ( ThreddsXmlParserException e )
    { fail( "Failed to parse catalog: " + e.getMessage() ); return null; }

    assertNotNull( "Result of parse was null metadata [" + docBaseUriString + "].",
                   md );
    return md;
  }

  private void writeCatalogXml( Catalog cat )
  {
    ThreddsXmlWriter txw = ThreddsXmlWriterFactory.newInstance().createThreddsXmlWriter();
    try
    {
      txw.writeCatalog( cat, System.out );
    }
    catch ( ThreddsXmlWriterException e )
    {
      e.printStackTrace();
      fail( "Failed writing catalog to sout: " + e.getMessage() );
    }
  }

  private void writeMetadataXml( Metadata md )
  {
    ThreddsXmlWriter txw = ThreddsXmlWriterFactory.newInstance().createThreddsXmlWriter();
    try
    {
      txw.writeMetadata( md, System.out );
    }
    catch ( ThreddsXmlWriterException e )
    {
      e.printStackTrace();
      fail( "Failed writing catalog to sout: " + e.getMessage() );
    }
  }
}