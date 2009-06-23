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

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class AccessElementParser extends AbstractElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final DatasetBuilder datasetBuilder;

  public AccessElementParser( XMLEventReader reader, DatasetBuilder datasetBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, AccessElementNames.AccessElement );
    this.datasetBuilder = datasetBuilder;
  }

  protected static boolean isSelfElementStatic( XMLEvent event )
  {
    return isSelfElement( event, AccessElementNames.AccessElement );
  }

  protected boolean isSelfElement( XMLEvent event )
  {
    return isSelfElement( event, AccessElementNames.AccessElement );
  }

  protected AccessBuilder parseStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();

    AccessBuilder builder = null;
    if ( this.datasetBuilder != null )
      builder = this.datasetBuilder.addAccessBuilder();
    else
      throw new ThreddsXmlParserException( "" );

    Attribute serviceNameAtt = startElement.getAttributeByName( AccessElementNames.AccessElement_ServiceName );
    String serviceName = serviceNameAtt.getValue();
    // ToDo This only gets top level services, need findServiceBuilderByName() to crawl services
    ServiceBuilder serviceBuilder = this.datasetBuilder.getParentCatalogBuilder().findServiceBuilderByNameGlobally( serviceName );

    Attribute urlPathAtt = startElement.getAttributeByName( AccessElementNames.AccessElement_UrlPath );
    String urlPath = urlPathAtt.getValue();

    builder.setServiceBuilder( serviceBuilder );
    builder.setUrlPath( urlPath );

    Attribute dataFormatAtt = startElement.getAttributeByName( AccessElementNames.AccessElement_DataFormat );
    if ( dataFormatAtt != null )
    {
      builder.setDataFormat( DataFormatType.getType( dataFormatAtt.getValue() ) );
    }

    return builder;
  }

  protected void handleChildStartElement( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
    StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
  }

  protected void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    return;
  }
}