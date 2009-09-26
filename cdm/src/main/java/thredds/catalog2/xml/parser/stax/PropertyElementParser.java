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
import thredds.catalog2.xml.names.PropertyElementNames;

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
class PropertyElementParser extends AbstractElementParser
{
  
  private final CatalogBuilder catBuilder;
  private final DatasetNodeBuilder datasetNodeBuilder;
  private final ServiceBuilder serviceBuilder;

  private PropertyElementParser( QName elementName,
                         XMLEventReader reader,
                         ThreddsBuilderFactory builderFactory,
                         CatalogBuilder catBuilder )
  {
    super( elementName, reader, builderFactory);
    this.catBuilder = catBuilder;
    this.datasetNodeBuilder = null;
    this.serviceBuilder = null;
  }

  private PropertyElementParser( QName elementName,
                         XMLEventReader reader,
                         ThreddsBuilderFactory builderFactory,
                         DatasetNodeBuilder datasetNodeBuilder )
  {
    super( elementName, reader, builderFactory );
    this.catBuilder = null;
    this.datasetNodeBuilder = datasetNodeBuilder;
    this.serviceBuilder = null;
  }

  private PropertyElementParser( QName elementName,
                         XMLEventReader reader,
                         ThreddsBuilderFactory builderFactory,
                         ServiceBuilder serviceBuilder )
  {
    super( elementName, reader, builderFactory );
    this.catBuilder = null;
    this.datasetNodeBuilder = null;
    this.serviceBuilder = serviceBuilder;
  }

  ThreddsBuilder getSelfBuilder() {
    return null;
  }

  void parseStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();

    Attribute nameAtt = startElement.getAttributeByName( PropertyElementNames.PropertyElement_Name );
    String name = nameAtt.getValue();
    Attribute valueAtt = startElement.getAttributeByName( PropertyElementNames.PropertyElement_Value );
    String value = valueAtt.getValue();

    if ( this.catBuilder != null )
      this.catBuilder.addProperty( name, value );
    else if ( this.datasetNodeBuilder != null )
      this.datasetNodeBuilder.addProperty( name, value );
    else if ( this.serviceBuilder != null )
      this.serviceBuilder.addProperty( name, value );
    else
      throw new ThreddsXmlParserException( "Unknown builder - for addProperty()." );
  }

  void handleChildStartElement()
          throws ThreddsXmlParserException
  {
    String unexpectedChildElementAsString =
            StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );

    ThreddsBuilder parentBuilder;
    if ( this.catBuilder != null )
      parentBuilder = this.catBuilder;
    else if ( this.datasetNodeBuilder != null )
      parentBuilder = this.datasetNodeBuilder;
    else if ( this.serviceBuilder != null )
      parentBuilder = this.serviceBuilder;
    else
      throw new ThreddsXmlParserException( "Unknown parent builder." );

    BuilderIssue issue = new BuilderIssue( BuilderIssue.Severity.WARNING, "Unexpected child element: " + unexpectedChildElementAsString, parentBuilder, null );
    // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
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
      this.elementName = PropertyElementNames.PropertyElement;
    }

    boolean isEventMyStartElement( XMLEvent event ) {
      return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
    }

    PropertyElementParser getNewParser( XMLEventReader reader,
                                       ThreddsBuilderFactory builderFactory,
                                       CatalogBuilder parentCatalogBuilder )
    {
      return new PropertyElementParser( this.elementName, reader, builderFactory, parentCatalogBuilder );
    }

    PropertyElementParser getNewParser( XMLEventReader reader,
                                       ThreddsBuilderFactory builderFactory,
                                       DatasetNodeBuilder parentDatasetNodeBuilder )
    {
      return new PropertyElementParser( this.elementName, reader, builderFactory, parentDatasetNodeBuilder );
    }

    PropertyElementParser getNewParser( XMLEventReader reader,
                                       ThreddsBuilderFactory builderFactory,
                                       ServiceBuilder parentServiceBuilder )
    {
      return new PropertyElementParser( this.elementName, reader, builderFactory, parentServiceBuilder );
    }
  }
}