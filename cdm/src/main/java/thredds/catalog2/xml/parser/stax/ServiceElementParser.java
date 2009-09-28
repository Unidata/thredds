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

import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.names.ServiceElementNames;
import thredds.catalog.ServiceType;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class ServiceElementParser extends AbstractElementParser
{
  private final CatalogBuilder parentCatalogBuilder;
  private final ServiceBuilder parentServiceBuilder;

  private final ServiceElementParser.Factory serviceElemParserFactory;
  private final PropertyElementParser.Factory propertyElemParserFactory;

  private ServiceBuilder selfBuilder;


  private ServiceElementParser( QName elementName,
                                XMLEventReader reader,
                                ThreddsBuilderFactory builderFactory,
                                CatalogBuilder parentCatalogBuilder )
  {
    super( elementName, reader, builderFactory);
    this.parentCatalogBuilder = parentCatalogBuilder;
    this.parentServiceBuilder = null;

    this.serviceElemParserFactory = new Factory();
    this.propertyElemParserFactory = new PropertyElementParser.Factory();
  }

  private ServiceElementParser( QName elementName,
                        XMLEventReader reader,
                        ThreddsBuilderFactory builderFactory,
                        ServiceBuilder parentServiceBuilder )
  {
    super( elementName, reader, builderFactory );
    this.parentCatalogBuilder = null;
    this.parentServiceBuilder = parentServiceBuilder;

    this.serviceElemParserFactory = new Factory();
    this.propertyElemParserFactory = new PropertyElementParser.Factory();
  }

  ServiceBuilder getSelfBuilder() {
    return this.selfBuilder;
  }

  void parseStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();

    Attribute nameAtt = startElement.getAttributeByName( ServiceElementNames.ServiceElement_Name );
    String name = nameAtt.getValue();
    Attribute serviceTypeAtt = startElement.getAttributeByName( ServiceElementNames.ServiceElement_ServiceType );
    ServiceType serviceType = ServiceType.getType( serviceTypeAtt.getValue() );
    Attribute baseUriAtt = startElement.getAttributeByName( ServiceElementNames.ServiceElement_Base );
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
    if ( this.parentCatalogBuilder != null )
      this.selfBuilder = this.parentCatalogBuilder.addService( name, serviceType, baseUri );
    else if ( this.parentServiceBuilder != null )
      this.selfBuilder = this.parentServiceBuilder.addService( name, serviceType, baseUri );
    else
      throw new ThreddsXmlParserException( "" );

    Attribute suffixAtt = startElement.getAttributeByName( ServiceElementNames.ServiceElement_Suffix );
    if ( suffixAtt != null )
    {
      this.selfBuilder.setSuffix( suffixAtt.getValue() );
    }

    Attribute descriptionAtt = startElement.getAttributeByName( ServiceElementNames.ServiceElement_Description );
    if ( descriptionAtt != null )
    {
      this.selfBuilder.setSuffix( descriptionAtt.getValue() );
    }
  }

  void handleChildStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( this.serviceElemParserFactory.isEventMyStartElement( startElement ) )
    {
      ServiceElementParser serviceElemParser = this.serviceElemParserFactory.getNewParser( reader,
                                                                                           this.builderFactory,
                                                                                           this.selfBuilder );
      serviceElemParser.parse();
    }
    else if ( this.propertyElemParserFactory.isEventMyStartElement( startElement ))
    {
      PropertyElementParser parser = this.propertyElemParserFactory.getNewParser( reader,
                                                                                  this.builderFactory,
                                                                                  this.selfBuilder);
      parser.parse();
    }
    else
    {
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
    }
  }

  void postProcessingAfterEndElement()
          throws ThreddsXmlParserException
  {
    return;
  }

  static class Factory
  {
    private QName elementName;

    Factory() {
      this.elementName = ServiceElementNames.ServiceElement;
    }

    boolean isEventMyStartElement( XMLEvent event ) {
      return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
    }

    ServiceElementParser getNewParser( XMLEventReader reader,
                                       ThreddsBuilderFactory builderFactory,
                                       CatalogBuilder parentCatalogBuilder )
    {
      return new ServiceElementParser( this.elementName, reader, builderFactory, parentCatalogBuilder );
    }

    ServiceElementParser getNewParser( XMLEventReader reader,
                                       ThreddsBuilderFactory builderFactory,
                                       ServiceBuilder parentServiceBuilder )
    {
      return new ServiceElementParser( this.elementName, reader, builderFactory, parentServiceBuilder );
    }
  }
}