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

import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.util.DatasetElementUtils;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.builder.*;

import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.XMLEventReader;
import javax.xml.XMLConstants;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DatasetElementParser extends AbstractElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );
  
  protected final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                        DatasetElementUtils.ELEMENT_NAME );
  private final static QName nameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      DatasetElementUtils.NAME_ATTRIBUTE_NAME );
  private final static QName idAttName = new QName( XMLConstants.NULL_NS_URI,
                                                    DatasetElementUtils.ID_ATTRIBUTE_NAME );
  private final static QName urlPathAttName = new QName( XMLConstants.NULL_NS_URI,
                                                         DatasetElementUtils.URL_PATH_ATTRIBUTE_NAME );
  private final static QName serviceNameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                             DatasetElementUtils.SERVICE_NAME_ATTRIBUTE_NAME );

  private final static QName collectionTypeAttName = new QName( XMLConstants.NULL_NS_URI,
                                                                DatasetElementUtils.COLLECTION_TYPE_ATTRIBUTE_NAME );
  private final static QName harvestAttName = new QName( XMLConstants.NULL_NS_URI,
                                                         DatasetElementUtils.HARVEST_ATTRIBUTE_NAME );
  private final static QName restrictedAccessAttName = new QName( XMLConstants.NULL_NS_URI,
                                                                  DatasetElementUtils.RESOURCE_CONTROL_ATTRIBUTE_NAME );
  /** Document base URI for documents with dataset as the root element. */
  private final String docBaseUriString;

  private final CatalogBuilder catBuilder;
  private final DatasetNodeBuilder datasetNodeBuilder;
  private final ThreddsBuilderFactory catBuilderFactory;

  private final DatasetNodeElementParserUtils datasetNodeElementParserUtils;

  public DatasetElementParser( XMLEventReader reader,
                               CatalogBuilder catBuilder,
                               DatasetNodeElementParserUtils parentDatasetNodeElementParserUtils )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.docBaseUriString = null;
    this.catBuilder = catBuilder;
    this.datasetNodeBuilder = null;
    this.catBuilderFactory = null;

    this.datasetNodeElementParserUtils = new DatasetNodeElementParserUtils( parentDatasetNodeElementParserUtils);
  }

  public DatasetElementParser( XMLEventReader reader,
                               DatasetNodeBuilder datasetNodeBuilder,
                               DatasetNodeElementParserUtils parentDatasetNodeElementParserUtils )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.docBaseUriString = null;
    this.catBuilder = null;
    this.datasetNodeBuilder = datasetNodeBuilder;
    this.catBuilderFactory = null;

    this.datasetNodeElementParserUtils = new DatasetNodeElementParserUtils( parentDatasetNodeElementParserUtils);
  }

  public DatasetElementParser( XMLEventReader reader,
                               ThreddsBuilderFactory catBuilderFactory,
                               DatasetNodeElementParserUtils parentDatasetNodeElementParserUtils )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.docBaseUriString = null;
    this.catBuilder = null;
    this.datasetNodeBuilder = null;
    this.catBuilderFactory = catBuilderFactory;

    this.datasetNodeElementParserUtils = new DatasetNodeElementParserUtils( parentDatasetNodeElementParserUtils);
  }

  public DatasetElementParser( String docBaseUriString,
                               XMLEventReader reader,
                               ThreddsBuilderFactory catBuilderFactory )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.docBaseUriString = docBaseUriString;
    this.catBuilder = null;
    this.datasetNodeBuilder = null;
    this.catBuilderFactory = catBuilderFactory;

    this.datasetNodeElementParserUtils = new DatasetNodeElementParserUtils( null);
  }

  protected void setDefaultServiceName( String defaultServiceName )
  {
    this.datasetNodeElementParserUtils.setDefaultServiceName( defaultServiceName );
  }

  protected String getDefaultServiceName()
  {
    return this.datasetNodeElementParserUtils.getDefaultServiceName();
  }

  protected static boolean isSelfElementStatic( XMLEvent event )
  {
    return isSelfElement( event, elementName );
  }

  protected boolean isSelfElement( XMLEvent event )
  {
    return isSelfElement( event, elementName );
  }

  protected DatasetBuilder parseStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();

    Attribute nameAtt = startElement.getAttributeByName( nameAttName );
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

    this.datasetNodeElementParserUtils.parseStartElementIdAttribute( startElement, datasetBuilder );
    this.datasetNodeElementParserUtils.parseStartElementIdAuthorityAttribute( startElement, datasetBuilder );

    Attribute serviceNameAtt = startElement.getAttributeByName( serviceNameAttName );
    if ( serviceNameAtt != null )
      this.setDefaultServiceName( serviceNameAtt.getValue() );

    Attribute urlPathAtt = startElement.getAttributeByName( urlPathAttName );
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

    if ( this.datasetNodeElementParserUtils.handleBasicChildStartElement( startElement, this.reader, datasetBuilder ))
      return;
    else if ( this.datasetNodeElementParserUtils.handleCollectionChildStartElement( startElement, this.reader, datasetBuilder ))
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

    this.datasetNodeElementParserUtils.postProcessing( builder );

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
