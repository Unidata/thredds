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
import thredds.catalog2.xml.parser.stax.StaxThreddsXmlParser;
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

  public ParseCatalogTest() { }

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
    String xml = CatalogXmlUtils.wrapThreddsXmlInCatalogWithService( "", expires);

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
    String xml = CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( "", expires);

    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
    CatalogBuilder catBuilder = cp.parseIntoBuilder( new StringReader( xml ),
                                                     docBaseUri );
    assertNotNull( catBuilder );

    CatalogXmlUtils.assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, expires);
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
}