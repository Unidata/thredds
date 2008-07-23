package thredds.catalog2.xml.parser.sax;

import thredds.catalog2.xml.parser.CatalogParser;
import thredds.catalog2.xml.parser.CatalogParserException;
import thredds.catalog2.Catalog;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
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
  private boolean isValidating = false;
  private SAXCatalogParser()
  {}

  public static SAXCatalogParser getInstance()
  {
    return new SAXCatalogParser();
  }

  public void setValidating( boolean isValidating )
  {
    this.isValidating = isValidating;
  }

  public boolean isValidating()
  {
    return this.isValidating;
  }

  private Catalog readXML( InputSource source )
          throws CatalogParserException
  {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating( isValidating );
    factory.setNamespaceAware( true );
    //factory.setSchema(  );
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
