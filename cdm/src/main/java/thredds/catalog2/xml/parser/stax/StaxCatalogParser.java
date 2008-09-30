package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.xml.parser.CatalogParser;
import thredds.catalog2.xml.parser.CatalogParserException;
import thredds.catalog2.xml.parser.CatalogNamespace;
import thredds.catalog2.Catalog;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.util.HttpUriResolver;
import thredds.util.HttpUriResolverFactory;

import java.net.URI;
import java.io.*;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class StaxCatalogParser implements CatalogParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private XMLInputFactory myFactory;
  private XMLEventReader parser;

//  private boolean isValidating = false;
//  private Schema schema = null;

  private StaxCatalogParser()
  {
    myFactory = XMLInputFactory.newInstance();
    myFactory.setProperty( "javax.xml.stream.isCoalescing", Boolean.TRUE );
    parser = null;
  }

  public static StaxCatalogParser getInstance()
  {
    return new StaxCatalogParser();
  }

//  public boolean wantValidating( boolean wantValidating )
//  {
//  }
//
//  public boolean isValidating()
//  {
//    return this.isValidating;
//  }

  private Catalog readXML( Source source )
          throws CatalogParserException
  {
    try
    {
       parser = this.myFactory.createXMLEventReader( source );
    }
    catch ( XMLStreamException e )
    {
      throw new CatalogParserException( "", e );
    }

    return null;
  }

  public Catalog parse( URI uri )
          throws CatalogParserException
  {
    HttpUriResolver httpUriResolver = HttpUriResolverFactory.getDefaultHttpUriResolver( uri );
    InputStream is = null;
    try
    {
      httpUriResolver.makeRequest();
      is = httpUriResolver.getResponseBodyAsInputStream();
    }
    catch ( IOException e )
    {
      throw new CatalogParserException( "", e );
    }

    Source s = new StreamSource( is, uri.toString() );
    return readXML( s );
  }

  public Catalog parse( File file, URI baseUri )
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
//    return readXML( is);
    return null;
  }

  public Catalog parse( Reader reader, URI baseUri )
          throws CatalogParserException
  {
    InputSource is = new InputSource( reader );
    is.setSystemId( baseUri.toString() );
//    return readXML( is);
    return null;
  }

  public Catalog parse( InputStream is, URI baseUri )
          throws CatalogParserException
  {
    InputSource inSource = new InputSource( is );
    inSource.setSystemId( baseUri.toString() );
    return null; //readXML( inSource );
  }

  public CatalogBuilder parseIntoBuilder( URI uri ) throws CatalogParserException
  {
    return null;
  }

  public CatalogBuilder parseIntoBuilder( File file, URI baseUri ) throws CatalogParserException
  {
    return null;
  }

  public CatalogBuilder parseIntoBuilder( Reader reader, URI baseUri ) throws CatalogParserException
  {
    return null;
  }

  public CatalogBuilder parseIntoBuilder( InputStream is, URI baseUri ) throws CatalogParserException
  {
    return null;
  }
}