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
public class DatasetNodeElementParser
{
  private DatasetNodeElementParser() {}

  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  protected final static QName nameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                        DatasetElementUtils.NAME_ATTRIBUTE_NAME );
  protected final static QName idAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      DatasetElementUtils.ID_ATTRIBUTE_NAME );
  protected final static QName authorityAttName = new QName( XMLConstants.NULL_NS_URI,
                                                             DatasetElementUtils.AUTHORITY_ATTRIBUTE_NAME );

  public void parseStartElement( XMLEvent event, DatasetNodeBuilder dsNodeBuilder )
          throws ThreddsXmlParserException
  {
    if ( !event.isStartElement() )
      throw new IllegalArgumentException( "Event must be start element." );
    StartElement startElement = event.asStartElement();

    // Get required attributes.
    Attribute nameAtt = startElement.getAttributeByName( nameAttName );
    String name = nameAtt.getValue();

    // Set optional attributes
    Attribute idAtt = startElement.getAttributeByName( idAttName );
    if ( idAtt != null )
    {
      dsNodeBuilder.setId( idAtt.getValue() );
    }
    Attribute authAtt = startElement.getAttributeByName( authorityAttName );
    if ( authAtt != null )
    {
      dsNodeBuilder.setIdAuthority( authAtt.getValue() );
    }

    return;
  }

  public void handleChildStartElementBasic( StartElement startElement,
                                            XMLEventReader reader,
                                            DatasetNodeBuilder dsNodeBuilder )
          throws ThreddsXmlParserException
  {
    if ( PropertyElementParser.isSelfElementStatic( startElement ))
    {
      PropertyElementParser parser = new PropertyElementParser( reader, dsNodeBuilder);
      parser.parse();
    }
    if ( MetadataElementParser.isSelfElementStatic( startElement ))
    {
      MetadataElementParser parser = new MetadataElementParser( reader, dsNodeBuilder);
      parser.parse();
    }
//    if ( ThreddsMetadataElementParser.isSelfElementStatic( startElement ))
//    {
//      ThreddsMetadataElementParser parser = new ThreddsMetadataElementParser( reader, dsNodeBuilder);
//      parser.parse();
//    }
    else
    {
      StaxThreddsXmlParserUtils.readElementAndAnyContent( reader );
    }
  }

  protected void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    return;
  }
}