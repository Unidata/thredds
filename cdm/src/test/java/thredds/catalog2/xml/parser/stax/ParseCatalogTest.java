package thredds.catalog2.xml.parser.stax;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.StringReader;

import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.parser.ThreddsXmlParser;
import thredds.catalog2.xml.parser.CatalogXmlUtils;
import thredds.catalog2.builder.*;

import javax.xml.stream.XMLStreamException;

import ucar.nc2.units.DateType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ParseCatalogTest
{

    // Do we have all these:
    // 1) dataset with urlPath and serviceName attributes
    // 2) dataset with urlPath att and serviceName child element
    // 3) dataset with urlPath att and child metadata element with child serviceName element
    // 4) same as 3 but metadata element has inherited="true" attribute
    // 5) same as 3 but metadata element is in dataset that is parent to dataset with urlPath
    // 6) 1-5 where serviceName points to single top-level service
    // 7) 1-5 where serviceName points to compound service
    // 8) 1-5 where serviceName points to a single service contained in a compound service


    @Test
  public void parseCatalog()
          throws URISyntaxException,
                 XMLStreamException,
                 ThreddsXmlParserException
  {
    String baseUriString = "http://cat2.stax.ParseCatalogTest/simpleCatalog.xml";
    URI docBaseUri = new URI( baseUriString );
    DateType expires = new DateType( false, new Date( System.currentTimeMillis() + 60*60*1000));
    String xml = CatalogXmlUtils.getCatalog( expires);

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    CatalogBuilder catBuilder = cp.parseIntoBuilder( new StringReader( xml ),
                                                     docBaseUri );
    assertNotNull( catBuilder );

    CatalogXmlUtils.assertCatalogAsExpected( catBuilder, docBaseUri, expires);
  }

  @Test
  public void parseCatalogWithService()
          throws URISyntaxException,
                 XMLStreamException,
                 ThreddsXmlParserException
  {
    String baseUriString = "http://cat2.stax.ParseCatalogTest/CatalogWithService.xml";
    URI docBaseUri = new URI( baseUriString );
    DateType expires = new DateType( false, new Date( System.currentTimeMillis() + 60*60*1000));
    String xml = CatalogXmlUtils.getCatalogWithService( expires);

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    CatalogBuilder catBuilder = cp.parseIntoBuilder( new StringReader( xml ),
                                                     docBaseUri );
    assertNotNull( catBuilder );

    CatalogXmlUtils.assertCatalogWithServiceAsExpected( catBuilder, docBaseUri, expires);
  }

  @Test
  public void parseCatalogWithCompoundService()
          throws URISyntaxException,
                 XMLStreamException,
                 ThreddsXmlParserException
  {
    String baseUriString = "http://cat2.stax.ParseCatalogTest/CatalogWithCompoundService.xml";
    URI docBaseUri = new URI( baseUriString );
    DateType expires = new DateType( false, new Date( System.currentTimeMillis() + 60*60*1000));
    String xml = CatalogXmlUtils.getCatalogWithCompoundService( expires);

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    CatalogBuilder catBuilder = cp.parseIntoBuilder( new StringReader( xml ),
                                                     docBaseUri );
    assertNotNull( catBuilder );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, expires);
  }

    @Test
    public void parseContainerDatasetWithMetadataServicename()
            throws URISyntaxException,
                   ThreddsXmlParserException
    {
        String docBaseUriString = "http://cat2.stax.ParseContainerDatasetTest/MetadataServiceName.xml";
        URI docBaseUri = new URI( docBaseUriString);
        String catXml = CatalogXmlUtils.wrapThreddsXmlInCatalogDatasetMetadata( "<serviceName>odap</serviceName>\n" );

        CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catXml );
        assertNotNull( catBuilder );
    }

    @Test
  public void parseAccessibleDatasetWithRawServiceName()
          throws URISyntaxException,
                 XMLStreamException,
                 ThreddsXmlParserException
  {
    String xml = CatalogXmlUtils.getCatalogWithSingleAccessDatasetWithRawServiceName();
    String baseUriString = "http://cat2.stax.ParseAccessibleDatasetTest/RawServiceName.xml";

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    URI docBaseUri = new URI( baseUriString );
    CatalogBuilder catBuilder = cp.parseIntoBuilder( new StringReader( xml ),
                                                     docBaseUri );
    assertNotNull( catBuilder );

    CatalogXmlUtils.assertCatalogHasSingleAccessDataset( catBuilder, docBaseUri );
  }

  @Test
  public void parseAccessibleDatasetWithMetadataServiceName()
          throws URISyntaxException,
                 XMLStreamException,
                 ThreddsXmlParserException
  {
    String xml = CatalogXmlUtils.getCatalogWithSingleAccessDatasetWithMetadataServiceName();
    String baseUriString = "http://cat2.stax.ParseAccessibleDatasetTest/MetadataServiceName.xml";

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    URI docBaseUri = new URI( baseUriString );
    CatalogBuilder catBuilder = cp.parseIntoBuilder( new StringReader( xml ),
                                                     docBaseUri );
    assertNotNull( catBuilder );

    CatalogXmlUtils.assertCatalogHasSingleAccessDataset( catBuilder, docBaseUri );
  }

  @Test
  public void parseAccessibleDatasetWithInheritedMetadataServiceName()
          throws URISyntaxException,
                 XMLStreamException,
                 ThreddsXmlParserException
  {
    String xml = CatalogXmlUtils.getCatalogWithSingleAccessDatasetWithInheritedMetadataServiceName();
    String baseUriString = "http://cat2.stax.ParseAccessibleDatasetTest/InheritedMetadataServiceName.xml";

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    URI docBaseUri = new URI( baseUriString );
    CatalogBuilder catBuilder = cp.parseIntoBuilder( new StringReader( xml ),
                                                     docBaseUri );
    assertNotNull( catBuilder );

    CatalogXmlUtils.assertCatalogHasSingleAccessDataset( catBuilder, docBaseUri );
  }

  @Test
  public void parseAccessibleDatasetOldStyle()
          throws URISyntaxException,
                 XMLStreamException,
                 ThreddsXmlParserException
  {
    String xml = CatalogXmlUtils.getCatalogWithSingleAccessDatasetOldStyle();
    String baseUriString = "http://cat2.stax.ParseAccessibleDatasetTest/OldStyleAccess.xml";

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    URI docBaseUri = new URI( baseUriString );
    CatalogBuilder catBuilder = cp.parseIntoBuilder( new StringReader( xml ),
                                                     docBaseUri );
    assertNotNull( catBuilder );

    CatalogXmlUtils.assertCatalogHasSingleAccessDataset( catBuilder, docBaseUri );
  }

  @Test
  public void parseNestedDatasetWithRawServiceName()
          throws URISyntaxException,
                 XMLStreamException,
                 ThreddsXmlParserException
  {
    String xml = CatalogXmlUtils.getNestedDatasetWithRawServiceName();
    String baseUriString = "http://cat2.stax.ParseNestedDatasetTest/RawServiceNameNotAccessible.xml";
      System.out.println( "Catalog ["+baseUriString+"]:\n" + xml );


      ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    URI docBaseUri = new URI( baseUriString );
    CatalogBuilder catBuilder = cp.parseIntoBuilder( new StringReader( xml ),
                                                     docBaseUri );
    assertNotNull( catBuilder );

    CatalogXmlUtils.assertNestedDatasetIsNotAccessible( catBuilder, docBaseUri );
  }

  @Test
  public void parseNestedDatasetWithMetadataServiceName()
          throws URISyntaxException,
                 XMLStreamException,
                 ThreddsXmlParserException
  {
    String xml = CatalogXmlUtils.getNestedDatasetWithMetadataServiceName();
    String baseUriString = "http://cat2.stax.ParseNestedDatasetTest/MetadataServiceNameNotAccessible.xml";

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    URI docBaseUri = new URI( baseUriString );
    CatalogBuilder catBuilder = cp.parseIntoBuilder( new StringReader( xml ),
                                                     docBaseUri );
    assertNotNull( catBuilder );

    CatalogXmlUtils.assertNestedDatasetIsNotAccessible( catBuilder, docBaseUri );
  }

  @Test
  public void parseNestedDatasetWithUninheritedMetadataServiceName()
          throws URISyntaxException,
                 XMLStreamException,
                 ThreddsXmlParserException
  {
    String xml = CatalogXmlUtils.getNestedDatasetWithUninheritedMetadataServiceName();
    String baseUriString = "http://cat2.stax.ParseNestedDatasetTest/UninheritedMetadataServiceNameNotAccessible.xml";

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    URI docBaseUri = new URI( baseUriString );
    CatalogBuilder catBuilder = cp.parseIntoBuilder( new StringReader( xml ),
                                                     docBaseUri );
    assertNotNull( catBuilder );

    CatalogXmlUtils.assertNestedDatasetIsNotAccessible( catBuilder, docBaseUri );
  }

  @Test
  public void parseNestedDatasetWithInheritedMetadataServiceName()
          throws URISyntaxException,
                 XMLStreamException,
                 ThreddsXmlParserException
  {
    String xml = CatalogXmlUtils.getNestedDatasetWithInheritedMetadataServiceName();
    String baseUriString = "http://cat2.stax.ParseNestedDatasetTest/InheritedMetadataServiceNameNotAccessible.xml";

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    URI docBaseUri = new URI( baseUriString );
    CatalogBuilder catBuilder = cp.parseIntoBuilder( new StringReader( xml ),
                                                     docBaseUri );
    assertNotNull( catBuilder );

    CatalogXmlUtils.assertNestedDatasetIsAccessible( catBuilder, docBaseUri );
  }
}