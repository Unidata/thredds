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
  private final CatalogBuilder parentCatalogBuilder;
  private final DatasetNodeBuilder parentDatasetNodeBuilder;

  private final DatasetNodeElementParserHelper datasetNodeElementParserHelper;

  private DatasetBuilder selfBuilder;

  public DatasetElementParser( XMLEventReader reader,
                               ThreddsBuilderFactory builderFactory,
                               CatalogBuilder parentCatalogBuilder,
                               DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
          throws ThreddsXmlParserException
  {
    super( reader, DatasetElementNames.DatasetElement, builderFactory );
    this.parentCatalogBuilder = parentCatalogBuilder;
    this.parentDatasetNodeBuilder = null;

    this.datasetNodeElementParserHelper = new DatasetNodeElementParserHelper( parentDatasetNodeElementParserHelper,
                                                                              this.builderFactory );
  }

  public DatasetElementParser( XMLEventReader reader,
                               ThreddsBuilderFactory builderFactory,
                               DatasetNodeBuilder parentDatasetNodeBuilder,
                               DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
          throws ThreddsXmlParserException
  {
    super( reader, DatasetElementNames.DatasetElement, builderFactory );
    this.parentCatalogBuilder = null;
    this.parentDatasetNodeBuilder = parentDatasetNodeBuilder;

    this.datasetNodeElementParserHelper = new DatasetNodeElementParserHelper( parentDatasetNodeElementParserHelper,
                                                                              this.builderFactory);
  }

  public DatasetElementParser( XMLEventReader reader,
                               ThreddsBuilderFactory builderFactory,
                               DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
          throws ThreddsXmlParserException
  {
    super( reader, DatasetElementNames.DatasetElement, builderFactory );
    this.parentCatalogBuilder = null;
    this.parentDatasetNodeBuilder = null;

    this.datasetNodeElementParserHelper = new DatasetNodeElementParserHelper( parentDatasetNodeElementParserHelper,
                                                                              this.builderFactory );
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

  protected DatasetBuilder getSelfBuilder() {
    return this.selfBuilder;
  }

  protected void parseStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();

    Attribute nameAtt = startElement.getAttributeByName( DatasetElementNames.DatasetElement_Name );
    String name = nameAtt.getValue();

    if ( this.parentCatalogBuilder != null )
      this.selfBuilder = this.parentCatalogBuilder.addDataset( name );
    else if ( this.parentDatasetNodeBuilder != null )
      this.selfBuilder = this.parentDatasetNodeBuilder.addDataset( name );
    else if ( builderFactory != null )
      this.selfBuilder = builderFactory.newDatasetBuilder( name );
    else
      throw new ThreddsXmlParserException( "" );

    this.datasetNodeElementParserHelper.parseStartElementIdAttribute( startElement, this.selfBuilder );
    this.datasetNodeElementParserHelper.parseStartElementIdAuthorityAttribute( startElement, this.selfBuilder );

    Attribute serviceNameAtt = startElement.getAttributeByName( DatasetElementNames.DatasetElement_ServiceName );
    if ( serviceNameAtt != null )
      this.setDefaultServiceName( serviceNameAtt.getValue() );

    Attribute urlPathAtt = startElement.getAttributeByName( DatasetElementNames.DatasetElement_UrlPath );
    if ( urlPathAtt != null )
    {
      // Add AccessBuilder and set urlPath, set ServiceBuilder in postProcessingAfterEndElement().
      AccessBuilder accessBuilder = this.selfBuilder.addAccessBuilder();
      accessBuilder.setUrlPath( urlPathAtt.getValue() );
    }
  }

  protected void handleChildStartElement() throws ThreddsXmlParserException
  {
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( this.datasetNodeElementParserHelper.handleBasicChildStartElement( startElement, this.reader, this.selfBuilder ))
      return;
    else if ( this.datasetNodeElementParserHelper.handleCollectionChildStartElement( startElement, this.reader, this.selfBuilder ))
      return;
    else if ( AccessElementParser.isSelfElementStatic( startElement ) )
    {
      AccessElementParser parser = new AccessElementParser( this.reader,
                                                            this.builderFactory,
                                                            this.selfBuilder );
      parser.parse();
      return;
    }
    else
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
  }

  protected void postProcessingAfterEndElement()
          throws ThreddsXmlParserException
  {
    this.datasetNodeElementParserHelper.postProcessingAfterEndElement();

    // In any AccessBuilders that don't have a ServiceBuilder, set it with the default service.
    if ( this.getDefaultServiceName() != null
         && ! this.selfBuilder.getAccessBuilders().isEmpty() )
    {
      ServiceBuilder defaultServiceBuilder = this.selfBuilder.getParentCatalogBuilder().findServiceBuilderByNameGlobally( this.getDefaultServiceName() );

      for ( AccessBuilder curAB : this.selfBuilder.getAccessBuilders() )
      {
        if ( curAB.getServiceBuilder() == null )
          curAB.setServiceBuilder( defaultServiceBuilder );
      }
    }

    this.datasetNodeElementParserHelper.addFinalThreddsMetadataToDatasetNodeBuilder( this.selfBuilder );
    this.datasetNodeElementParserHelper.addFinalMetadataToDatasetNodeBuilder( this.selfBuilder );
  }
}
