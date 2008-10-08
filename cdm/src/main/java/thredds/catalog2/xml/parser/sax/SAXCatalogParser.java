package thredds.catalog2.xml.parser.sax;

import thredds.catalog2.xml.parser.ThreddsXmlParser;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.Catalog;
import thredds.catalog2.Dataset;
import thredds.catalog2.Metadata;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.BuildException;
import thredds.catalog2.builder.DatasetBuilder;
import thredds.catalog2.builder.MetadataBuilder;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
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
public class SAXCatalogParser implements ThreddsXmlParser
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
      try
      {
        this.schema = CatalogNamespace.CATALOG_1_0.resolveNamespaceAsSchema();
      }
      catch ( IOException e )
      {
        log.warn( "wantValidating(): Failed to read schema.", e );
        this.schema = null;
      }
      catch ( SAXException e )
      {
        log.warn( "wantValidating(): Failed to parse schema.", e );
        this.schema = null;
      }
      if ( schema != null )
        this.isValidating = true;
      else
        this.isValidating = false;
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
          throws ThreddsXmlParserException
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

    ThreddsCatalogHandler catHandler = new ThreddsCatalogHandler( source.getSystemId() );
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
    try
    {
      return catHandler.getCatalog();
    }
    catch ( BuildException e )
    {
      throw new ThreddsXmlParserException( "Catalog builder in bad state.", e);
    }
  }

  public Catalog parse( URI uri )
          throws ThreddsXmlParserException
  {
    InputSource is = new InputSource( uri.toString() );
    return readXML( is );
  }

  public Catalog parse( File file, URI baseUri )
          throws ThreddsXmlParserException
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

  public Catalog parse( Reader reader, URI baseUri )
          throws ThreddsXmlParserException
  {
    InputSource is = new InputSource( reader );
    is.setSystemId( baseUri.toString() );
    return readXML( is);
  }

  public Catalog parse( InputStream is, URI baseUri )
          throws ThreddsXmlParserException
  {
    InputSource inSource = new InputSource( is );
    inSource.setSystemId( baseUri.toString() );
    return readXML( inSource );
  }

  public CatalogBuilder parseIntoBuilder( URI uri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public CatalogBuilder parseIntoBuilder( File file, URI baseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public CatalogBuilder parseIntoBuilder( Reader reader, URI baseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public CatalogBuilder parseIntoBuilder( InputStream is, URI baseUri ) throws ThreddsXmlParserException
  {
    return null;
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
