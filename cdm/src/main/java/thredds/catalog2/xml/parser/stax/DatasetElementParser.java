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

import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.names.DatasetElementNames;
import thredds.catalog2.builder.*;

import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.XMLEventReader;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DatasetElementParser extends AbstractElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );
  
  /** Document base URI for documents with dataset as the root element. */
  private final String docBaseUriString;

  private final CatalogBuilder catBuilder;
  private final DatasetNodeBuilder datasetNodeBuilder;
  private final ThreddsBuilderFactory catBuilderFactory;

  private final DatasetNodeElementParserHelper datasetNodeElementParserHelper;

  public DatasetElementParser( XMLEventReader reader,
                               CatalogBuilder catBuilder,
                               DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
          throws ThreddsXmlParserException
  {
    super( reader, DatasetElementNames.DatasetElement );
    this.docBaseUriString = null;
    this.catBuilder = catBuilder;
    this.datasetNodeBuilder = null;
    this.catBuilderFactory = null;

    this.datasetNodeElementParserHelper = new DatasetNodeElementParserHelper( parentDatasetNodeElementParserHelper );
  }

  public DatasetElementParser( XMLEventReader reader,
                               DatasetNodeBuilder datasetNodeBuilder,
                               DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
          throws ThreddsXmlParserException
  {
    super( reader, DatasetElementNames.DatasetElement );
    this.docBaseUriString = null;
    this.catBuilder = null;
    this.datasetNodeBuilder = datasetNodeBuilder;
    this.catBuilderFactory = null;

    this.datasetNodeElementParserHelper = new DatasetNodeElementParserHelper( parentDatasetNodeElementParserHelper );
  }

  public DatasetElementParser( XMLEventReader reader,
                               ThreddsBuilderFactory catBuilderFactory,
                               DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
          throws ThreddsXmlParserException
  {
    super( reader, DatasetElementNames.DatasetElement );
    this.docBaseUriString = null;
    this.catBuilder = null;
    this.datasetNodeBuilder = null;
    this.catBuilderFactory = catBuilderFactory;

    this.datasetNodeElementParserHelper = new DatasetNodeElementParserHelper( parentDatasetNodeElementParserHelper );
  }

  public DatasetElementParser( String docBaseUriString,
                               XMLEventReader reader,
                               ThreddsBuilderFactory catBuilderFactory )
          throws ThreddsXmlParserException
  {
    super( reader, DatasetElementNames.DatasetElement );
    this.docBaseUriString = docBaseUriString;
    this.catBuilder = null;
    this.datasetNodeBuilder = null;
    this.catBuilderFactory = catBuilderFactory;

    this.datasetNodeElementParserHelper = new DatasetNodeElementParserHelper( null);
  }

  protected void setDefaultServiceName( String defaultServiceName )
  {
    this.datasetNodeElementParserHelper.setDefaultServiceName( defaultServiceName );
  }

  protected String getDefaultServiceName()
  {
    return this.datasetNodeElementParserHelper.getDefaultServiceName();
  }

  protected static boolean isSelfElementStatic( XMLEvent event )
  {
    return isSelfElement( event, DatasetElementNames.DatasetElement );
  }

  protected boolean isSelfElement( XMLEvent event )
  {
    return isSelfElement( event, DatasetElementNames.DatasetElement );
  }

  protected DatasetBuilder parseStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();

    Attribute nameAtt = startElement.getAttributeByName( DatasetElementNames.DatasetElement_Name );
    String name = nameAtt.getValue();

    DatasetBuilder datasetBuilder = null;
    if ( this.catBuilder != null )
      datasetBuilder = this.catBuilder.addDataset( name );
    else if ( this.datasetNodeBuilder != null )
      datasetBuilder = this.datasetNodeBuilder.addDataset( name );
    else if ( catBuilderFactory != null )
      datasetBuilder = catBuilderFactory.newDatasetBuilder( name );
    else
      throw new ThreddsXmlParserException( "" );

    this.datasetNodeElementParserHelper.parseStartElementIdAttribute( startElement, datasetBuilder );
    this.datasetNodeElementParserHelper.parseStartElementIdAuthorityAttribute( startElement, datasetBuilder );

    Attribute serviceNameAtt = startElement.getAttributeByName( DatasetElementNames.DatasetElement_ServiceName );
    if ( serviceNameAtt != null )
      this.setDefaultServiceName( serviceNameAtt.getValue() );

    Attribute urlPathAtt = startElement.getAttributeByName( DatasetElementNames.DatasetElement_UrlPath );
    if ( urlPathAtt != null )
    {
      // Add AccessBuilder and set urlPath, set ServiceBuilder in postProcessing().
      AccessBuilder accessBuilder = datasetBuilder.addAccessBuilder();
      accessBuilder.setUrlPath( urlPathAtt.getValue() );
    }

    return datasetBuilder;
  }

  protected void handleChildStartElement( ThreddsBuilder builder ) throws ThreddsXmlParserException
  {
    if ( !( builder instanceof DatasetBuilder ) )
      throw new IllegalArgumentException( "Given ThreddsBuilder must be an instance of DatasetBuilder." );
    DatasetBuilder datasetBuilder = (DatasetBuilder) builder;

    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( this.datasetNodeElementParserHelper.handleBasicChildStartElement( startElement, this.reader, datasetBuilder ))
      return;
    else if ( this.datasetNodeElementParserHelper.handleCollectionChildStartElement( startElement, this.reader, datasetBuilder ))
      return;
    else if ( AccessElementParser.isSelfElementStatic( startElement ) )
    {
      AccessElementParser parser = new AccessElementParser( this.reader, datasetBuilder );
      parser.parse();
      return;
    }
    else
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
  }

  protected void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( ! ( builder instanceof DatasetBuilder) )
      throw new IllegalArgumentException( "Given ThreddsBuilder must be an instance of DatasetBuilder.");
    DatasetBuilder datasetBuilder = (DatasetBuilder) builder;

    this.datasetNodeElementParserHelper.postProcessing( builder );

    // In any AccessBuilders that don't have a ServiceBuilder, set it with the default service.
    if ( this.getDefaultServiceName() != null
         && ! datasetBuilder.getAccessBuilders().isEmpty() )
    {
      ServiceBuilder defaultServiceBuilder = datasetBuilder.getParentCatalogBuilder().findServiceBuilderByNameGlobally( this.getDefaultServiceName() );

      for ( AccessBuilder curAB : datasetBuilder.getAccessBuilders() )
      {
        if ( curAB.getServiceBuilder() == null )
          curAB.setServiceBuilder( defaultServiceBuilder );
      }
    }

    // ToDo Deal with inherited metadata
  }
}
