package thredds.catalog2.xml.parser.stax;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.StringReader;
import java.io.Reader;
import java.io.PrintWriter;
import java.io.StringWriter;

import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.parser.ThreddsXmlParser;
import thredds.catalog2.xml.parser.CatalogXmlUtils;
import thredds.catalog2.xml.parser.stax.ThreddsMetadataElementParser;
import thredds.catalog2.xml.parser.stax.StaxThreddsXmlParserUtils;
import thredds.catalog2.xml.parser.stax.StaxThreddsXmlParser;
import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.DatasetNodeBuilder;
import thredds.catalog2.builder.ThreddsMetadataBuilder;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
//@RunWith(Parameterized.class)
public class ParseMetadataTest
{
  //private final String metadataXml;
  private XMLInputFactory factory;

//  public ParseMetadataTest( String metadataXml )
//  {
//    this.metadataXml = metadataXml;
//  }

  public ParseMetadataTest() { }

  @Before
  public void init()
  {
    //this.tdsUrl = TdsTestUtils.getTargetTdsUrl();
    this.factory = XMLInputFactory.newInstance();
    this.factory.setProperty( "javax.xml.stream.isCoalescing", Boolean.TRUE );
    this.factory.setProperty( "javax.xml.stream.supportDTD", Boolean.FALSE );
//    this.factory.setXMLReporter(  );
//    this.factory.setXMLResolver(  );

  }

//  @Parameterized.Parameters
  public static Collection<Object[]> getCatalogUrls()
  {
    String[][] individualthreddsMetadataElements =
            {
                    { "<metadata xlink:title='good metadata' xlink:href='http://good.metadata/'/>"},
                    { "<metadata><dataType>Radial</dataType></metadata>"},
                    { "<metadata><serviceName>OPENDAP</serviceName></metadata>"},
                    { "<metadata><dataType>Radial</dataType></metadata>"},
                    { "<metadata><dataFormat>NEXRAD2</dataFormat></metadata>"}
            };

    List<Object[]> catUrls = new ArrayList<Object[]>(
            Arrays.asList( individualthreddsMetadataElements ) );
    return catUrls;
  }

  @Test
  public void parseServiceNameXml()
          throws URISyntaxException,
                 XMLStreamException,
                 ThreddsXmlParserException
  {
    String xml = CatalogXmlUtils.wrapThreddsXmlInCatalogDataset( "<serviceName>OPeNDAP</serviceName>");
    String baseUriString = "http://test.metadata.parser/tmd.xml";

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    CatalogBuilder catBuilder = cp.parseIntoBuilder( new StringReader( xml ),
                                                     new URI( baseUriString ) );
    assertNotNull( catBuilder );

    List<DatasetNodeBuilder> dsBuilders = catBuilder.getDatasetNodeBuilders();
    assertTrue( dsBuilders.size() == 1 );
    DatasetNodeBuilder dsnBuilder = dsBuilders.get( 0 );
    ThreddsMetadataBuilder tmdb = dsnBuilder.getThreddsMetadataBuilder();

//    String catName = "Unidata THREDDS Data Server";
//    assertTrue( "Catalog name [" + metadata.getName() + "] not as expected [" + catName + "].",
//                metadata.getName().equals( catName ) );

  }

  @Test
  public void parseDataFormatXml()
          throws URISyntaxException,
                 ThreddsXmlParserException
  {
    String xml = CatalogXmlUtils.wrapThreddsXmlInCatalogDataset( "<dataFormat>NEXRAD2</dataFormat>");
    String baseUriString = "http://test.metadata.parser/tmd.xml";

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    CatalogBuilder catBuilder = cp.parseIntoBuilder( new StringReader( xml ),
                                                     new URI( baseUriString ) );

    assertNotNull( catBuilder );

//    String catName = "Unidata THREDDS Data Server";
//    assertTrue( "Catalog name [" + metadata.getName() + "] not as expected [" + catName + "].",
//                metadata.getName().equals( catName ) );

  }
}
