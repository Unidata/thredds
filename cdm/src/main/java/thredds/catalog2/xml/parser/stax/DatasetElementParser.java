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
import javax.xml.namespace.QName;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class DatasetElementParser extends AbstractElementParser
{
  private final CatalogBuilder parentCatalogBuilder;
  private final DatasetNodeBuilder parentDatasetNodeBuilder;
  private final DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper;

  private final AccessElementParser.Factory accessElementParserFactory;

  private DatasetNodeElementParserHelper datasetNodeElementParserHelper;

  private DatasetBuilder selfBuilder;

  private DatasetElementParser( QName elementName,
                        XMLEventReader reader,
                        ThreddsBuilderFactory builderFactory,
                        CatalogBuilder parentCatalogBuilder,
                        DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
  {
    super( elementName, reader, builderFactory );
    this.parentCatalogBuilder = parentCatalogBuilder;
    this.parentDatasetNodeBuilder = null;

    this.parentDatasetNodeElementParserHelper = parentDatasetNodeElementParserHelper;

    this.accessElementParserFactory = new AccessElementParser.Factory();
  }

  private DatasetElementParser( QName elementName,
                        XMLEventReader reader,
                        ThreddsBuilderFactory builderFactory,
                        DatasetNodeBuilder parentDatasetNodeBuilder,
                        DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
  {
    super( elementName, reader, builderFactory );
    this.parentCatalogBuilder = null;
    this.parentDatasetNodeBuilder = parentDatasetNodeBuilder;

    this.parentDatasetNodeElementParserHelper = parentDatasetNodeElementParserHelper;

    this.accessElementParserFactory = new AccessElementParser.Factory();
  }

  void setDefaultServiceName( String defaultServiceName )
  {
    this.datasetNodeElementParserHelper.setDefaultServiceNameSpecifiedInSelf( defaultServiceName );
  }

  String getDefaultServiceName()
  {
    return this.datasetNodeElementParserHelper.getDefaultServiceNameSpecifiedInSelf() != null
            ? this.datasetNodeElementParserHelper.getDefaultServiceNameSpecifiedInSelf()
            : this.datasetNodeElementParserHelper.getDefaultServiceNameInheritedFromAncestors();
  }

  DatasetBuilder getSelfBuilder() {
    return this.selfBuilder;
  }

  void parseStartElement()
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

    this.datasetNodeElementParserHelper = new DatasetNodeElementParserHelper( this.parentDatasetNodeElementParserHelper,
                                                                              this.selfBuilder,
                                                                              this.builderFactory );

    this.datasetNodeElementParserHelper.parseStartElementIdAttribute( startElement );
    this.datasetNodeElementParserHelper.parseStartElementIdAuthorityAttribute( startElement );

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

  void handleChildStartElement() throws ThreddsXmlParserException
  {
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( this.datasetNodeElementParserHelper.handleBasicChildStartElement( startElement, this.reader, this.selfBuilder ))
      return;
    else if ( this.datasetNodeElementParserHelper.handleCollectionChildStartElement( startElement, this.reader, this.selfBuilder ))
      return;
    else if ( this.accessElementParserFactory.isEventMyStartElement( startElement ) )
    {
      AccessElementParser parser = this.accessElementParserFactory.getNewParser( this.reader, this.builderFactory,
                                                                                 this.selfBuilder );
      parser.parse();
      return;
    }
    else
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
  }

    void postProcessingAfterEndElement()
            throws ThreddsXmlParserException
    {
        this.datasetNodeElementParserHelper.postProcessingAfterEndElement();

        // For each AccessBuilders that doesn't have a ServiceBuilder, either set its
        // ServiceBuilder to the default service or, if there is no default service,
        // remove it from this dataset builder.
        if ( ! this.selfBuilder.getAccessBuilders().isEmpty() )
        {
            ServiceBuilder defaultServiceBuilder = null;
            if ( this.getDefaultServiceName() != null )
                defaultServiceBuilder = this.selfBuilder.getParentCatalogBuilder()
                        .findServiceBuilderByNameGlobally( this.getDefaultServiceName() );

            for ( AccessBuilder curAB : this.selfBuilder.getAccessBuilders() )
            {
                if ( curAB.getServiceBuilder() == null )
                {
                    if ( defaultServiceBuilder != null )
                        curAB.setServiceBuilder( defaultServiceBuilder );
                    else
                        this.selfBuilder.removeAccessBuilder( curAB );
                }
            }
        }

        this.datasetNodeElementParserHelper.addFinalThreddsMetadataToDatasetNodeBuilder( this.selfBuilder );
        this.datasetNodeElementParserHelper.addFinalMetadataToDatasetNodeBuilder( this.selfBuilder );
    }

  static class Factory
  {
    private QName elementName;

    Factory() {
      this.elementName = DatasetElementNames.DatasetElement;
    }

    boolean isEventMyStartElement( XMLEvent event ) {
      return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
    }

    DatasetElementParser getNewParser( XMLEventReader reader,
                                       ThreddsBuilderFactory builderFactory,
                                       CatalogBuilder parentCatalogBuilder,
                                       DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
    {
      return new DatasetElementParser( this.elementName, reader, builderFactory, parentCatalogBuilder,
                                       parentDatasetNodeElementParserHelper );
    }

    DatasetElementParser getNewParser( XMLEventReader reader,
                                       ThreddsBuilderFactory builderFactory,
                                       DatasetNodeBuilder parentDatasetNodeBuilder,
                                       DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
    {
      return new DatasetElementParser( this.elementName, reader, builderFactory, parentDatasetNodeBuilder,
                                       parentDatasetNodeElementParserHelper );
    }
  }
}
