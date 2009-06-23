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
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.util.CatalogRefElementUtils;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;

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
public class CatalogRefElementParser extends AbstractElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  protected final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                        CatalogRefElementUtils.ELEMENT_NAME );
  protected final static QName xlinkTitleAttName = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                                                              CatalogRefElementUtils.XLINK_TITLE_ATTRIBUTE_NAME );
  protected final static QName xlinkHrefAttName = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                                                             CatalogRefElementUtils.XLINK_HREF_ATTRIBUTE_NAME );
  protected final static QName xlinkTypeAttName = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                                                             CatalogRefElementUtils.XLINK_TYPE_ATTRIBUTE_NAME );


  private final CatalogBuilder catBuilder;
  private final DatasetNodeBuilder datasetNodeBuilder;
  private final ThreddsBuilderFactory catBuilderFactory;

  private final DatasetNodeElementParserUtils datasetNodeElementParserUtils;


  public CatalogRefElementParser( XMLEventReader reader,
                                  CatalogBuilder catBuilder,
                                  DatasetNodeElementParserUtils parentDatasetNodeElementParserUtils )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = catBuilder;
    this.datasetNodeBuilder = null;
    this.catBuilderFactory = null;
    this.datasetNodeElementParserUtils = new DatasetNodeElementParserUtils( parentDatasetNodeElementParserUtils );
  }

  public CatalogRefElementParser( XMLEventReader reader,
                                  DatasetNodeBuilder datasetNodeBuilder,
                                  DatasetNodeElementParserUtils parentDatasetNodeElementParserUtils )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = null;
    this.datasetNodeBuilder = datasetNodeBuilder;
    this.catBuilderFactory = null;
    this.datasetNodeElementParserUtils = new DatasetNodeElementParserUtils( parentDatasetNodeElementParserUtils );
  }

  public CatalogRefElementParser( XMLEventReader reader,
                                  ThreddsBuilderFactory catBuilderFactory,
                                  DatasetNodeElementParserUtils parentDatasetNodeElementParserUtils )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = null;
    this.datasetNodeBuilder = null;
    this.catBuilderFactory = catBuilderFactory;
    this.datasetNodeElementParserUtils = new DatasetNodeElementParserUtils( parentDatasetNodeElementParserUtils );
  }

  protected static boolean isSelfElementStatic( XMLEvent event )
  {
    return isSelfElement( event, elementName );
  }

  protected boolean isSelfElement( XMLEvent event )
  {
    return isSelfElement( event, elementName );
  }

  protected DatasetNodeBuilder parseStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();

    // Get required attributes.
    Attribute titleAtt = startElement.getAttributeByName( xlinkTitleAttName );
    String title = titleAtt.getValue();
    Attribute hrefAtt = startElement.getAttributeByName( xlinkHrefAttName );
    String href = hrefAtt.getValue();
    URI hrefUri = null;
    try
    {
      hrefUri = new URI( href );
    }
    catch ( URISyntaxException e )
    {
      log.error( "parseElement(): Bad catalog base URI [" + href + "]: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Bad catalog base URI [" + href + "]: " + e.getMessage(), e );
    }

    // Construct builder.
    CatalogRefBuilder catalogRefBuilder = null;
    if ( this.catBuilder != null )
      catalogRefBuilder = this.catBuilder.addCatalogRef( title, hrefUri );
    else if ( this.datasetNodeBuilder != null )
      catalogRefBuilder = this.datasetNodeBuilder.addCatalogRef( title, hrefUri );
    else if ( catBuilderFactory != null )
      catalogRefBuilder = catBuilderFactory.newCatalogRefBuilder( title, hrefUri );
    else
      throw new ThreddsXmlParserException( "" );

    // Set optional attributes
    this.datasetNodeElementParserUtils.parseStartElementIdAttribute( startElement, catalogRefBuilder );
    this.datasetNodeElementParserUtils.parseStartElementIdAuthorityAttribute( startElement, catalogRefBuilder );

    return catalogRefBuilder;
  }
  protected void handleChildStartElement( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( !( builder instanceof CatalogRefBuilder ) )
      throw new IllegalArgumentException( "Given ThreddsBuilder must be an instance of DatasetBuilder." );

    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( this.datasetNodeElementParserUtils.handleBasicChildStartElement( startElement, this.reader, (CatalogRefBuilder) builder ))
      return;
    else
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
  }

  protected void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    // ToDo Deal with inherited metadata
    return;
  }
}