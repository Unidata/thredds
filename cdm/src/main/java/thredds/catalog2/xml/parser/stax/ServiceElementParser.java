/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.util.ServiceElementUtils;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog.ServiceType;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.namespace.QName;
import javax.xml.XMLConstants;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ServiceElementParser extends AbstractElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                        ServiceElementUtils.ELEMENT_NAME );
  private final static QName nameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      ServiceElementUtils.NAME_ATTRIBUTE_NAME );
  private final static QName baseAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      ServiceElementUtils.BASE_ATTRIBUTE_NAME );
  private final static QName serviceTypeAttName = new QName( XMLConstants.NULL_NS_URI,
                                                             ServiceElementUtils.SERVICE_TYPE_ATTRIBUTE_NAME );
  private final static QName descriptionAttName = new QName( XMLConstants.NULL_NS_URI,
                                                             ServiceElementUtils.DESCRIPTION_ATTRIBUTE_NAME );
  private final static QName suffixAttName = new QName( XMLConstants.NULL_NS_URI,
                                                             ServiceElementUtils.SUFFIX_ATTRIBUTE_NAME );

  private final CatalogBuilder catBuilder;
  private final ServiceBuilder serviceBuilder;
  private final ThreddsBuilderFactory catBuilderFactory;

  public ServiceElementParser( XMLEventReader reader,  CatalogBuilder catBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, elementName);
    this.catBuilder = catBuilder;
    this.serviceBuilder = null;
    this.catBuilderFactory = null;
  }

  public ServiceElementParser( XMLEventReader reader,  ServiceBuilder serviceBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = null;
    this.serviceBuilder = serviceBuilder;
    this.catBuilderFactory = null;
  }

  public ServiceElementParser( XMLEventReader reader, ThreddsBuilderFactory catBuilderFactory )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = null;
    this.serviceBuilder = null;
    this.catBuilderFactory = catBuilderFactory;
  }

  protected static boolean isSelfElementStatic( XMLEvent event )
  {
    return isSelfElement( event, elementName );
  }

  protected boolean isSelfElement( XMLEvent event )
  {
    return isSelfElement( event, elementName );
  }

  protected ServiceBuilder parseStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();

    Attribute nameAtt = startElement.getAttributeByName( nameAttName );
    String name = nameAtt.getValue();
    Attribute serviceTypeAtt = startElement.getAttributeByName( serviceTypeAttName );
    ServiceType serviceType = ServiceType.getType( serviceTypeAtt.getValue() );
    Attribute baseUriAtt = startElement.getAttributeByName( baseAttName );
    String baseUriString = baseUriAtt.getValue();
    URI baseUri = null;
    try
    {
      baseUri = new URI( baseUriString );
    }
    catch ( URISyntaxException e )
    {
      log.error( "parseElement(): Bad service base URI [" + baseUriString + "]: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Bad service base URI [" + baseUriString + "]", e );
    }
    ServiceBuilder serviceBuilder = null;
    if ( this.catBuilder != null )
      serviceBuilder = this.catBuilder.addService( name, serviceType, baseUri );
    else if ( this.serviceBuilder != null )
      serviceBuilder = this.serviceBuilder.addService( name, serviceType, baseUri );
    else if ( catBuilderFactory != null )
      serviceBuilder = catBuilderFactory.newServiceBuilder( name, serviceType, baseUri );
    else
      throw new ThreddsXmlParserException( "" );

    Attribute suffixAtt = startElement.getAttributeByName( suffixAttName );
    if ( suffixAtt != null )
    {
      serviceBuilder.setSuffix( suffixAtt.getValue() );
    }

    Attribute descriptionAtt = startElement.getAttributeByName( descriptionAttName );
    if ( descriptionAtt != null )
    {
      serviceBuilder.setSuffix( descriptionAtt.getValue() );
    }

    return serviceBuilder;
  }

  protected void handleChildStartElement( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( !( builder instanceof ServiceBuilder ) )
      throw new IllegalArgumentException( "Given ThreddsBuilder must be an instance of DatasetBuilder." );
    ServiceBuilder serviceBuilder = (ServiceBuilder) builder;

    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( ServiceElementParser.isSelfElementStatic( startElement ) )
    {
      ServiceElementParser serviceElemParser = new ServiceElementParser( reader, serviceBuilder );
      serviceElemParser.parse();
    }
    else if ( PropertyElementParser.isSelfElementStatic( startElement ))
    {
      PropertyElementParser parser = new PropertyElementParser( reader, serviceBuilder);
      parser.parse();
    }
    else
    {
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
    }
  }

  protected void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    return;
  }
}