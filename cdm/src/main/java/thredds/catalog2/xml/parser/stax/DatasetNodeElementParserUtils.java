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
  private DatasetNodeElementParserUtils() {}

  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  protected final static QName nameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                        DatasetElementUtils.NAME_ATTRIBUTE_NAME );
  protected final static QName idAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      DatasetElementUtils.ID_ATTRIBUTE_NAME );
  protected final static QName authorityAttName = new QName( XMLConstants.NULL_NS_URI,
                                                             DatasetElementUtils.AUTHORITY_ATTRIBUTE_NAME );

  public static void parseStartElementNameAttribute( StartElement startElement,
                                                     DatasetNodeBuilder dsNodeBuilder )
  {
    Attribute att = startElement.getAttributeByName( nameAttName );
    if ( att != null )
      dsNodeBuilder.setName( att.getValue() );
  }

  public static void parseStartElementIdAttribute( StartElement startElement,
                                                   DatasetNodeBuilder dsNodeBuilder )
  {
    Attribute att = startElement.getAttributeByName( idAttName );
    if ( att != null )
      dsNodeBuilder.setId( att.getValue() );
  }

  public static void parseStartElementIdAuthorityAttribute( StartElement startElement,
                                                            DatasetNodeBuilder dsNodeBuilder )
  {
    Attribute att = startElement.getAttributeByName( authorityAttName );
    if ( att != null )
      dsNodeBuilder.setId( att.getValue() );
  }

  public static boolean handleBasicChildStartElement( StartElement startElement,
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
      MetadataElementParser parser = new MetadataElementParser( reader, dsNodeBuilder);
      parser.parse();
      return true;
    }
//    else if ( ThreddsMetadataElementParser.isSelfElementStatic( startElement ))
//    {
//      ThreddsMetadataElementParser parser = new ThreddsMetadataElementParser( reader, dsNodeBuilder);
//      parser.parse();
//      return true;    
//    }
    else
      return false;
  }
  public static boolean handleCollectionChildStartElement( StartElement startElement,
                                                           XMLEventReader reader,
                                                           DatasetNodeBuilder dsNodeBuilder )
          throws ThreddsXmlParserException
  {
    if ( DatasetElementParser.isSelfElementStatic( startElement ))
    {
      DatasetElementParser parser = new DatasetElementParser( reader, dsNodeBuilder);
      parser.parse();
      return true;
    }
    else if ( CatalogRefElementParser.isSelfElementStatic( startElement ))
    {
      CatalogRefElementParser parser = new CatalogRefElementParser( reader, dsNodeBuilder);
      parser.parse();
      return true;
    }
    else
      return false;
  }
}