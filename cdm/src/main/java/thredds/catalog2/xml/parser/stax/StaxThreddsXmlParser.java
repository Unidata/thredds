package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.xml.parser.ThreddsXmlParser;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.Catalog;
import thredds.catalog2.Dataset;
import thredds.catalog2.Metadata;
import thredds.catalog2.simpleImpl.CatalogBuilderFactoryImpl;
import thredds.catalog2.builder.*;
import thredds.util.HttpUriResolver;
import thredds.util.HttpUriResolverFactory;

import java.net.URI;
import java.io.*;


import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class StaxThreddsXmlParser implements ThreddsXmlParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final XMLInputFactory factory;
  private XMLEventReader reader;

//  private boolean isValidating = false;
//  private Schema schema = null;

  private StaxThreddsXmlParser()
  {
    factory = XMLInputFactory.newInstance();
    factory.setProperty( "javax.xml.stream.isCoalescing", Boolean.TRUE );
    factory.setProperty( "javax.xml.stream.supportDTD", Boolean.FALSE );
//    factory.setXMLReporter(  );
//    factory.setXMLResolver(  );
    reader = null;
  }

  public static StaxThreddsXmlParser newInstance()
  {
    return new StaxThreddsXmlParser();
  }

//  public boolean wantValidating( boolean wantValidating )
//  {
//  }
//
//  public boolean isValidating()
//  {
//    return this.isValidating;
//  }

  private CatalogBuilder readCatalogXML( Source source )
          throws ThreddsXmlParserException
  {
    try
    {
      reader = this.factory.createXMLEventReader( source );

      CatalogBuilderFactory catBuilderFac = new CatalogBuilderFactoryImpl();
      CatalogBuilder catBuilder = null;
      while ( reader.hasNext() )
      {
        XMLEvent event = reader.peek();
        if ( event.isEndDocument())
        {
          reader.next();
          continue;
        }
        else if ( event.isStartDocument())
        {
          reader.next();
          continue;
        }
        else if ( event.isStartElement())
        {
          if ( CatalogElementParser2.isSelfElementStatic( event.asStartElement() ))
          {
            CatalogElementParser2 catElemParser = new CatalogElementParser2( source.getSystemId(), reader, catBuilderFac);
            catBuilder = (CatalogBuilder) catElemParser.parse();
          }
          else
          {
            StaxThreddsXmlParserUtils.consumeElementAndAnyContent( this.reader );
            log.warn( "readCatalogXML(): Unrecognized start element [" + event.asStartElement().getName() + "]." );
            reader.next();
            continue;
          }
        }
        else if ( event.isEndElement())
        {
          if ( CatalogElementParser2.isSelfElementStatic( event.asEndElement() ) )
          {
            break;
          }
          else
          {
            log.error( "readCatalogXML(): Unrecognized end element [" + event.asEndElement().getName() + "]." );
            break;
          }
        }
        else
        {
          log.debug( "readCatalogXML(): Unhandled event [" + event.getLocation() + "--" + event + "].");
          reader.next();
          continue;
        }
      }

      reader.close();

      if ( catBuilder == null )
        return null;

      return catBuilder;
    }
    catch ( XMLStreamException e )
    {
      log.error( "readCatalogXML(): Failed to parse catalog document: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Failed to parse catalog document: " + e.getMessage(), e );
    }
//    catch ( BuilderException e )
//    {
//      log.error( "readCatalogXML(): Failed to parse catalog document: " + e.getMessage(), e );
//      throw new ThreddsXmlParserException( "Failed to parse catalog document: " + e.getMessage(), e );
//    }
  }

  public Catalog parse( URI documentUri )
          throws ThreddsXmlParserException
  {
    HttpUriResolver httpUriResolver = HttpUriResolverFactory.getDefaultHttpUriResolver( documentUri );
    InputStream is = null;
    try
    {
      httpUriResolver.makeRequest();
      is = httpUriResolver.getResponseBodyAsInputStream();
    }
    catch ( IOException e )
    {
      throw new ThreddsXmlParserException( "", e );
    }

    Source s = new StreamSource( is, documentUri.toString() );
    try
    {
      return readCatalogXML( s ).build();
    }
    catch ( BuilderException e )
    {
      log.error( "parse(): Failed to parse catalog document: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Failed to parse catalog document: " + e.getMessage(), e );
    }
  }

  public Catalog parse( File file, URI docBaseUri )
          throws ThreddsXmlParserException
  {
    try
    {
      return parseIntoBuilder( file, docBaseUri ).build();
    }
    catch ( BuilderException e )
    {
      log.error( "parse(): Failed to parse catalog document: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Failed to parse catalog document: " + e.getMessage(), e );
    }
  }

  public Catalog parse( Reader reader, URI docBaseUri )
          throws ThreddsXmlParserException
  {
    try
    {
      return parseIntoBuilder( reader, docBaseUri ).build();
    }
    catch ( BuilderException e )
    {
      log.error( "parse(): Failed to parse catalog document: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Failed to parse catalog document: " + e.getMessage(), e );
    }
  }

  public Catalog parse( InputStream is, URI docBaseUri )
          throws ThreddsXmlParserException
  {
    try
    {
      return parseIntoBuilder( is, docBaseUri ).build();
    }
    catch ( BuilderException e )
    {
      log.error( "parse(): Failed to parse catalog document: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Failed to parse catalog document: " + e.getMessage(), e );
    }
  }

  public CatalogBuilder parseIntoBuilder( URI documentUri )
          throws ThreddsXmlParserException
  {
    HttpUriResolver httpUriResolver = HttpUriResolverFactory.getDefaultHttpUriResolver( documentUri );
    InputStream is = null;
    try
    {
      httpUriResolver.makeRequest();
      is = httpUriResolver.getResponseBodyAsInputStream();
    }
    catch ( IOException e )
    {
      throw new ThreddsXmlParserException( "", e );
    }

    Source s = new StreamSource( is, documentUri.toString() );
    return readCatalogXML( s );
  }

  public CatalogBuilder parseIntoBuilder( File file, URI docBaseUri )
          throws ThreddsXmlParserException
  {
    if ( file == null ) throw new IllegalArgumentException( "File must not be null." );
    Source s = null;
    if ( docBaseUri == null )
      s = new StreamSource( file );
    else
    {
      InputStream is = null;
      try
      {
        is = new FileInputStream( file );
      }
      catch ( FileNotFoundException e )
      {
        log.error( "parseIntoBuilder(): Couldn't find file []: " + e.getMessage(), e );
        throw new ThreddsXmlParserException( "Couldn't find file []: " + e.getMessage(), e );
      }
      s = new StreamSource( is, docBaseUri.toString() );
    }
    return readCatalogXML( s );
  }

  public CatalogBuilder parseIntoBuilder( Reader reader, URI docBaseUri )
          throws ThreddsXmlParserException
  {
    Source source = new StreamSource( reader, docBaseUri.toString() );
    return readCatalogXML( source );
  }

  public CatalogBuilder parseIntoBuilder( InputStream is, URI docBaseUri )
          throws ThreddsXmlParserException
  {
    Source source = new StreamSource( is, docBaseUri.toString() );
    return readCatalogXML( source );
  }

  public Dataset parseDataset( URI documentUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public Dataset parseDataset( File file, URI docBaseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public Dataset parseDataset( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public Dataset parseDataset( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public DatasetBuilder parseDatasetIntoBuilder( URI documentUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public DatasetBuilder parseDatasetIntoBuilder( File file, URI docBaseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public DatasetBuilder parseDatasetIntoBuilder( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public DatasetBuilder parseDatasetIntoBuilder( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public Metadata parseMetadata( URI documentUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public Metadata parseMetadata( File file, URI docBaseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public Metadata parseMetadata( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public Metadata parseMetadata( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public MetadataBuilder parseMetadataIntoBuilder( URI documentUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public MetadataBuilder parseMetadataIntoBuilder( File file, URI docBaseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public MetadataBuilder parseMetadataIntoBuilder( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public MetadataBuilder parseMetadataIntoBuilder( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException
  {
    return null;
  }
}