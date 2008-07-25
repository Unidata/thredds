package thredds.catalog2.xml.parser.sax;

import thredds.catalog2.xml.parser.CatalogParser;
import thredds.catalog2.xml.parser.CatalogParserException;
import thredds.catalog2.xml.parser.CatalogNamespace;
import thredds.catalog2.Catalog;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Schema;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import java.net.URI;
import java.io.*;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class SAXCatalogParser implements CatalogParser
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( SAXCatalogParser.class );

  private boolean isValidating = false;
  private Schema schema = null;

  private SAXCatalogParser()
  {
  }

  public static SAXCatalogParser getInstance()
  {
    return new SAXCatalogParser();
  }

  public boolean wantValidating( boolean wantValidating )
  {
    if ( wantValidating && schema == null )
    {
      SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
      InputStream is = null;
      try
      {
        is = CatalogNamespace.CATALOG_1_0.resolveNamespace();
      }
      catch ( IOException e )
      {
        log.warn( "wantValidating(): Failed to read schema.", e );
        is = null;
      }
      if ( is != null )
      {
        StreamSource source = new StreamSource( is );
        try
        {
          schema = schemaFactory.newSchema( source );
        }
        catch ( SAXException e )
        {
          log.warn( "wantValidating(): Failed to parse schema.", e );
          schema = null;
        }
        if ( schema != null )
          this.isValidating = true;
        else
          this.isValidating = false;
      }
    }
    else if ( wantValidating && schema != null )
    {
      this.isValidating = true;
    }
    else if ( ! wantValidating )
      this.isValidating = false;

    return this.isValidating;
  }

  public boolean isValidating()
  {
    return this.isValidating;
  }

  private Catalog readXML( InputSource source )
          throws CatalogParserException
  {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware( true );
    if ( this.isValidating )
      factory.setSchema( schema );
    SAXParser parser = null;
    try
    {
      parser = factory.newSAXParser();
    }
    catch ( ParserConfigurationException e )
    {
      e.printStackTrace();
    }
    catch ( SAXException e )
    {
      e.printStackTrace();
    }

//    XMLReader reader = parser.getXMLReader();
//    reader.setErrorHandler( new DefaultErrorHandler() );
//    reader.setContentHandler( new CatalogDefaultHandler() );

    CatalogDefaultHandler catHandler = new CatalogDefaultHandler();
    try
    {
      parser.parse( source, catHandler );
    }
    catch ( SAXException e )
    {
      e.printStackTrace();
    }
    catch ( IOException e )
    {
      e.printStackTrace();
    }
//    return catHandler.getCatalog();
    return null;
  }

  public Catalog readXML( URI uri )
          throws CatalogParserException
  {
    InputSource is = new InputSource( uri.toString() );
    return readXML( is );
  }

  public Catalog readXML( File file, URI baseUri )
          throws CatalogParserException
  {
    InputSource is = null;
    try
    {
      is = new InputSource( new FileReader( file ));
    }
    catch ( FileNotFoundException e )
    {
      e.printStackTrace();
    }
    is.setSystemId( baseUri.toString() );
    return readXML( is);
  }

  public Catalog readXML( Reader reader, URI baseUri )
          throws CatalogParserException
  {
    InputSource is = new InputSource( reader );
    is.setSystemId( baseUri.toString() );
    return readXML( is);
  }

  public Catalog readXML( InputStream is, URI baseUri )
          throws CatalogParserException
  {
    InputSource inSource = new InputSource( is );
    inSource.setSystemId( baseUri.toString() );
    return readXML( inSource );
  }
}
