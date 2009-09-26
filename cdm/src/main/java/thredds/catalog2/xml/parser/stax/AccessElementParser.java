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

import thredds.catalog2.builder.*;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.names.AccessElementNames;
import thredds.catalog.DataFormatType;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.namespace.QName;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class AccessElementParser extends AbstractElementParser
{
  private final DatasetBuilder parentDatasetBuilder;

  private AccessBuilder selfBuilder;

  private AccessElementParser( QName elementName,
                               XMLEventReader reader,
                               ThreddsBuilderFactory builderFactory,
                               DatasetBuilder parentDatasetBuilder )
  {
    super( elementName, reader, builderFactory );
    this.parentDatasetBuilder = parentDatasetBuilder;
  }

  AccessBuilder getSelfBuilder() {
    return this.selfBuilder;
  }

  void parseStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();
       
    if ( this.parentDatasetBuilder != null )
      this.selfBuilder = this.parentDatasetBuilder.addAccessBuilder();
    else
      throw new ThreddsXmlParserException( "" );

    Attribute serviceNameAtt = startElement.getAttributeByName( AccessElementNames.AccessElement_ServiceName );
    if ( serviceNameAtt != null )
    {
      String serviceName = serviceNameAtt.getValue();
      ServiceBuilder serviceBuilder = this.parentDatasetBuilder.getParentCatalogBuilder().findServiceBuilderByNameGlobally( serviceName );
      this.selfBuilder.setServiceBuilder( serviceBuilder );
    }

    Attribute urlPathAtt = startElement.getAttributeByName( AccessElementNames.AccessElement_UrlPath );
    String urlPath = urlPathAtt.getValue();

    this.selfBuilder.setUrlPath( urlPath );

    Attribute dataFormatAtt = startElement.getAttributeByName( AccessElementNames.AccessElement_DataFormat );
    if ( dataFormatAtt != null )
    {
      this.selfBuilder.setDataFormat( DataFormatType.getType( dataFormatAtt.getValue() ) );
    }

    return;
  }

  void handleChildStartElement()
          throws ThreddsXmlParserException
  {
    // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
    StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
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
      this.elementName = AccessElementNames.AccessElement;
    }

    boolean isEventMyStartElement( XMLEvent event ) {
      return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
    }

    AccessElementParser getNewParser( XMLEventReader reader,
                                      ThreddsBuilderFactory builderFactory,
                                      DatasetBuilder parentDatasetBuilder )
    {
      return new AccessElementParser( this.elementName, reader, builderFactory, parentDatasetBuilder );
    }
  }
}