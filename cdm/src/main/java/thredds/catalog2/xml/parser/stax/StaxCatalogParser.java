package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.xml.parser.CatalogParser;
import thredds.catalog2.xml.parser.CatalogParserException;
import thredds.catalog2.xml.parser.CatalogNamespace;
import thredds.catalog2.Catalog;
import thredds.catalog2.simpleImpl.CatalogBuilderFactoryImpl;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.CatalogBuilderFactory;
import thredds.util.HttpUriResolver;
import thredds.util.HttpUriResolverFactory;

import java.net.URI;
import java.io.*;


import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.EndElement;
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

  private XMLInputFactory factory;
  private XMLEventReader reader;

//  private boolean isValidating = false;
//  private Schema schema = null;

  private StaxCatalogParser()
  {
    factory = XMLInputFactory.newInstance();
    factory.setProperty( "javax.xml.stream.isCoalescing", Boolean.TRUE );
    factory.setProperty( "javax.xml.stream.supportDTD", Boolean.FALSE );
//    factory.setXMLReporter(  );
//    factory.setXMLResolver(  );
    reader = null;
  }

  public static StaxCatalogParser newInstance()
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
       reader = this.factory.createXMLEventReader( source );
    }
    catch ( XMLStreamException e )
    {
      throw new CatalogParserException( "", e );
    }

    CatalogBuilderFactory catBuilderFac = new CatalogBuilderFactoryImpl();
    try
    {
      while ( reader.hasNext() )
      {
        XMLEvent event = reader.peek();
        if ( event.isEndDocument())
        {
          reader.close();
        }
        else if ( event.isStartDocument())
        {
          reader.next();
          continue;
        }
        else if ( event.isStartElement())
        {
          StartElement se = event.asStartElement();
          if ( se.getName().getNamespaceURI().equals( CatalogNamespace.CATALOG_1_0.getNamespaceUri())
               && se.getName().getLocalPart().equals( "catalog" ) )
          {
            CatalogElementParser catElemParser = new CatalogElementParser( source.getSystemId(), reader, catBuilderFac);
            catElemParser.parse();
          }
          else
            System.out.println( "start : " + se.getName().getLocalPart());
        }
        else if ( event.isEndElement())
        {
          EndElement se = event.asEndElement();
          System.out.println( "end   : " + se.getName() );
        }
        else
        {
          reader.next();
          continue;
        }
      }
    }
    catch ( XMLStreamException e )
    {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
    if ( file == null ) throw new IllegalArgumentException( "File must not be null.");
    Source s = null;
    if ( baseUri == null)
      s = new StreamSource( file);
    else
    {
      InputStream is = null;
      try
      {
        is = new FileInputStream( file);
      }
      catch ( FileNotFoundException e )
      {
        throw new CatalogParserException( "Couldn't find file []: " + e.getMessage());
      }
      s = new StreamSource( is, baseUri.toString() );
    }
    return readXML( s);
  }

  public Catalog parse( Reader reader, URI baseUri )
          throws CatalogParserException
  {
    Source source = new StreamSource( reader, baseUri.toString() );
    return readXML( source);
  }

  public Catalog parse( InputStream is, URI baseUri )
          throws CatalogParserException
  {
    Source source = new StreamSource( is, baseUri.toString() );
    return readXML( source );
  }

  public CatalogBuilder parseIntoBuilder( URI uri )
          throws CatalogParserException
  {
    return null;
  }

  public CatalogBuilder parseIntoBuilder( File file, URI baseUri )
          throws CatalogParserException
  {
    return null;
  }

  public CatalogBuilder parseIntoBuilder( Reader reader, URI baseUri )
          throws CatalogParserException
  {
    return null;
  }

  public CatalogBuilder parseIntoBuilder( InputStream is, URI baseUri )
          throws CatalogParserException
  {
    return null;
  }
}