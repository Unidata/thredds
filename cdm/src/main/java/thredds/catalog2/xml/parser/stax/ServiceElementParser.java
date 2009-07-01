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
  private final CatalogBuilder catalogBuilder;
  private final ServiceBuilder parentServiceBuilder;

  private ServiceBuilder selfBuilder;

  public ServiceElementParser( XMLEventReader reader,
                               ThreddsBuilderFactory builderFactory,
                               CatalogBuilder catalogBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, ServiceElementNames.ServiceElement, builderFactory);
    this.catalogBuilder = catalogBuilder;
    this.parentServiceBuilder = null;
  }

  public ServiceElementParser( XMLEventReader reader,
                               ThreddsBuilderFactory builderFactory,
                               ServiceBuilder parentServiceBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, ServiceElementNames.ServiceElement, builderFactory );
    this.catalogBuilder = null;
    this.parentServiceBuilder = parentServiceBuilder;
  }

  protected static boolean isSelfElementStatic( XMLEvent event )
  {
    return isSelfElement( event, ServiceElementNames.ServiceElement );
  }

  protected boolean isSelfElement( XMLEvent event )
  {
    return isSelfElement( event, ServiceElementNames.ServiceElement );
  }

  protected ServiceBuilder getSelfBuilder() {
    return this.selfBuilder;
  }

  protected void parseStartElement()
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
    if ( this.catalogBuilder != null )
      this.selfBuilder = this.catalogBuilder.addService( name, serviceType, baseUri );
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

  protected void handleChildStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( ServiceElementParser.isSelfElementStatic( startElement ) )
    {
      ServiceElementParser serviceElemParser = new ServiceElementParser( reader, this.builderFactory, this.selfBuilder );
      serviceElemParser.parse();
    }
    else if ( PropertyElementParser.isSelfElementStatic( startElement ))
    {
      PropertyElementParser parser = new PropertyElementParser( reader, this.builderFactory, this.selfBuilder);
      parser.parse();
    }
    else
    {
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
    }
  }

  protected void postProcessingAfterEndElement()
          throws ThreddsXmlParserException
  {
    return;
  }
}