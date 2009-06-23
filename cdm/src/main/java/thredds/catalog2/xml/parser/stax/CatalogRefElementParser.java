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
import thredds.catalog2.xml.names.CatalogRefElementNames;

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
public class CatalogRefElementParser extends AbstractElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final CatalogBuilder catBuilder;
  private final DatasetNodeBuilder datasetNodeBuilder;
  private final ThreddsBuilderFactory catBuilderFactory;

  private final DatasetNodeElementParserHelper datasetNodeElementParserHelper;


  public CatalogRefElementParser( XMLEventReader reader,
                                  CatalogBuilder catBuilder,
                                  DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
          throws ThreddsXmlParserException
  {
    super( reader, CatalogRefElementNames.CatalogRefElement );
    this.catBuilder = catBuilder;
    this.datasetNodeBuilder = null;
    this.catBuilderFactory = null;
    this.datasetNodeElementParserHelper = new DatasetNodeElementParserHelper( parentDatasetNodeElementParserHelper );
  }

  public CatalogRefElementParser( XMLEventReader reader,
                                  DatasetNodeBuilder datasetNodeBuilder,
                                  DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
          throws ThreddsXmlParserException
  {
    super( reader, CatalogRefElementNames.CatalogRefElement );
    this.catBuilder = null;
    this.datasetNodeBuilder = datasetNodeBuilder;
    this.catBuilderFactory = null;
    this.datasetNodeElementParserHelper = new DatasetNodeElementParserHelper( parentDatasetNodeElementParserHelper );
  }

  public CatalogRefElementParser( XMLEventReader reader,
                                  ThreddsBuilderFactory catBuilderFactory,
                                  DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
          throws ThreddsXmlParserException
  {
    super( reader, CatalogRefElementNames.CatalogRefElement );
    this.catBuilder = null;
    this.datasetNodeBuilder = null;
    this.catBuilderFactory = catBuilderFactory;
    this.datasetNodeElementParserHelper = new DatasetNodeElementParserHelper( parentDatasetNodeElementParserHelper );
  }

  protected static boolean isSelfElementStatic( XMLEvent event )
  {
    return isSelfElement( event, CatalogRefElementNames.CatalogRefElement );
  }

  protected boolean isSelfElement( XMLEvent event )
  {
    return isSelfElement( event, CatalogRefElementNames.CatalogRefElement );
  }

  protected DatasetNodeBuilder parseStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();

    // Get required attributes.
    Attribute titleAtt = startElement.getAttributeByName( CatalogRefElementNames.CatalogRefElement_XlinkTitle );
    String title = titleAtt.getValue();
    Attribute hrefAtt = startElement.getAttributeByName( CatalogRefElementNames.CatalogRefElement_XlinkHref );
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
    this.datasetNodeElementParserHelper.parseStartElementIdAttribute( startElement, catalogRefBuilder );
    this.datasetNodeElementParserHelper.parseStartElementIdAuthorityAttribute( startElement, catalogRefBuilder );

    return catalogRefBuilder;
  }
  protected void handleChildStartElement( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( !( builder instanceof CatalogRefBuilder ) )
      throw new IllegalArgumentException( "Given ThreddsBuilder must be an instance of DatasetBuilder." );

    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( this.datasetNodeElementParserHelper.handleBasicChildStartElement( startElement, this.reader, (CatalogRefBuilder) builder ))
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