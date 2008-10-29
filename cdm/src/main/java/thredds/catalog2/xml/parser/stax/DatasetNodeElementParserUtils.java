package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.*;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.util.DatasetElementUtils;
import thredds.catalog2.xml.util.CatalogRefElementUtils;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.namespace.QName;
import javax.xml.XMLConstants;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DatasetNodeElementParserUtils
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  protected final static QName nameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                        DatasetElementUtils.NAME_ATTRIBUTE_NAME );
  protected final static QName idAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      DatasetElementUtils.ID_ATTRIBUTE_NAME );
  protected final static QName authorityAttName = new QName( XMLConstants.NULL_NS_URI,
                                                             DatasetElementUtils.AUTHORITY_ATTRIBUTE_NAME );

  private final DatasetNodeElementParserUtils datasetNodeElementParserUtils;

  private ThreddsMetadataElementParser threddsMetadataElementParser;

  DatasetNodeElementParserUtils( DatasetNodeElementParserUtils datasetNodeElementParserUtils )
  {
    this.datasetNodeElementParserUtils = datasetNodeElementParserUtils;
  }

  private String defaultServiceNameThatGetsInherited;

  protected void setDefaultServiceNameThatGetsInherited( String defaultServiceNameThatGetsInherited )
  {
    this.defaultServiceNameThatGetsInherited = defaultServiceNameThatGetsInherited;
  }

  protected String getDefaultServiceNameThatGetsInherited()
  {
    return this.defaultServiceNameThatGetsInherited;
  }

  private String idAuthorityThatGetsInherited;
  public void setIdAuthorityThatGetsInherited( String idAuthorityThatGetsInherited)
  {
    this.idAuthorityThatGetsInherited = idAuthorityThatGetsInherited;
  }

  public String getIdAuthorityThatGetsInherited()
  {
    return this.idAuthorityThatGetsInherited;
  }

  public void parseStartElementNameAttribute( StartElement startElement,
                                                     DatasetNodeBuilder dsNodeBuilder )
  {
    Attribute att = startElement.getAttributeByName( nameAttName );
    if ( att != null )
      dsNodeBuilder.setName( att.getValue() );
  }

  public void parseStartElementIdAttribute( StartElement startElement,
                                                   DatasetNodeBuilder dsNodeBuilder )
  {
    Attribute att = startElement.getAttributeByName( idAttName );
    if ( att != null )
      dsNodeBuilder.setId( att.getValue() );
  }

  public void parseStartElementIdAuthorityAttribute( StartElement startElement,
                                                            DatasetNodeBuilder dsNodeBuilder )
  {
    Attribute att = startElement.getAttributeByName( authorityAttName );
    if ( att != null )
      dsNodeBuilder.setId( att.getValue() );
  }

  public boolean handleBasicChildStartElement( StartElement startElement,
                                                      XMLEventReader reader,
                                                      DatasetNodeBuilder dsNodeBuilder )
          throws ThreddsXmlParserException
  {
    if ( PropertyElementParser.isSelfElementStatic( startElement ))
    {
      PropertyElementParser parser = new PropertyElementParser( reader, dsNodeBuilder);
      parser.parse();
      return true;
    }
    else if ( MetadataElementParser.isSelfElementStatic( startElement ))
    {
      MetadataElementParser parser = new MetadataElementParser( reader, dsNodeBuilder, this );
      parser.parse();
      return true;
    }
    else if ( ThreddsMetadataElementParser.isSelfElementStatic( startElement ))
    {
      if ( this.threddsMetadataElementParser == null )
        this.threddsMetadataElementParser = new ThreddsMetadataElementParser( reader, dsNodeBuilder, this);
      this.threddsMetadataElementParser.parse();
      return true;
    }
    else
      return false;
  }
  public boolean handleCollectionChildStartElement( StartElement startElement,
                                                           XMLEventReader reader,
                                                           DatasetNodeBuilder dsNodeBuilder )
          throws ThreddsXmlParserException
  {
    if ( DatasetElementParser.isSelfElementStatic( startElement ))
    {
      DatasetElementParser parser = new DatasetElementParser( reader, dsNodeBuilder, this);
      parser.parse();
      return true;
    }
    else if ( CatalogRefElementParser.isSelfElementStatic( startElement ))
    {
      CatalogRefElementParser parser = new CatalogRefElementParser( reader, dsNodeBuilder, this);
      parser.parse();
      return true;
    }
    else
      return false;
  }

  public void postProcessing( ThreddsBuilder builder )
  {
    // ToDo Deal with inherited metadata. Crawl up DatasetNodeBuilder heirarchy and gather inherited metadata. 
  }

}