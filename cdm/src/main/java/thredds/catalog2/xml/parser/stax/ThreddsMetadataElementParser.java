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
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.util.ThreddsMetadataElementUtils;
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
public class ThreddsMetadataElementParser extends AbstractElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                      ThreddsMetadataElementUtils.PROXY_ELEMENT_NAME );

  private final static QName serviceNameElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                                 ThreddsMetadataElementUtils.SERVICE_NAME_ELEMENT_NAME );
  private final static QName authorityElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                               ThreddsMetadataElementUtils.AUTHORITY_ELEMENT_NAME );

  private final static QName documentationElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                                   ThreddsMetadataElementUtils.DOCUMENTATION_ELEMENT_NAME );
  private final static QName keyphraseElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                               ThreddsMetadataElementUtils.KEYPHRASE_ELEMENT_NAME );
  private final static QName creatorElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                             ThreddsMetadataElementUtils.CREATOR_ELEMENT_NAME );
  private final static QName contributorElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                                 ThreddsMetadataElementUtils.CONTRIBUTOR_ELEMENT_NAME );
  private final static QName publisherElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                               ThreddsMetadataElementUtils.PUBLISHER_ELEMENT_NAME );
  private final static QName projectElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                             ThreddsMetadataElementUtils.PROJECT_ELEMENT_NAME );
  private final static QName dateElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                          ThreddsMetadataElementUtils.DATE_ELEMENT_NAME );

  private final static QName geospatialCoverageElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                                        ThreddsMetadataElementUtils.GEOSPATIAL_COVERAGE_ELEMENT_NAME );
  private final static QName timeCoverageElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                                  ThreddsMetadataElementUtils.TEMPORAL_COVERAGE_ELEMENT_NAME );

  private final static QName variablesElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                               ThreddsMetadataElementUtils.VARIABLES_ELEMENT_NAME );

  private final static QName dataSizeElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                              ThreddsMetadataElementUtils.DATA_SIZE_ELEMENT_NAME );
  private final static QName dataFormatElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                                ThreddsMetadataElementUtils.DATA_FORMAT_ELEMENT_NAME );
  private final static QName dataTypeElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                              ThreddsMetadataElementUtils.DATA_TYPE_ELEMENT_NAME );

  private final DatasetNodeBuilder datasetNodeBuilder;

  private final DatasetNodeElementParserUtils parentDatasetNodeElementParserUtils;

  private ThreddsMetadataBuilder resultThreddsMetadataBuilder;

  public ThreddsMetadataElementParser( XMLEventReader reader,
                                       DatasetNodeBuilder datasetNodeBuilder,
                                       DatasetNodeElementParserUtils parentDatasetNodeElementParserUtils )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.datasetNodeBuilder = datasetNodeBuilder;
    this.parentDatasetNodeElementParserUtils = parentDatasetNodeElementParserUtils;
  }

  protected static boolean isSelfElementStatic( XMLEvent event )
  {
    if ( ServiceNameElementParser.isSelfElementStatic( event )) return true;
    return false;
  }

  protected boolean isSelfElement( XMLEvent event )
  {
    return isSelfElementStatic( event );
  }

  protected ThreddsMetadataBuilder parseStartElement( StartElement startElement )
          throws ThreddsXmlParserException
  {
    if ( ! this.isSelfElement( startElement ) )
      throw new IllegalArgumentException( "Start element ["+startElement.getName().getLocalPart()+"] must be one of the THREDDS metadata element." );

    if ( this.resultThreddsMetadataBuilder == null )
    {
      if ( this.datasetNodeBuilder != null )
        this.resultThreddsMetadataBuilder = this.datasetNodeBuilder.setNewThreddsMetadataBuilder();
      else
        throw new ThreddsXmlParserException( "" );
    }

    if ( ServiceNameElementParser.isSelfElementStatic( startElement ) )
    {
      ServiceNameElementParser parser = new ServiceNameElementParser( this.reader, this.resultThreddsMetadataBuilder, this.parentDatasetNodeElementParserUtils );
      parser.parseCharacterContent( startElement );
    }
    else
      throw new ThreddsXmlParserException( "");

    return this.resultThreddsMetadataBuilder;
  }

  protected void handleChildStartElement( StartElement startElement, ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( ServiceNameElementParser.isSelfElementStatic( startElement ) )
      return;
    else
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.readElementAndAnyContent( this.reader );
  }

  protected void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    return;
  }

  public static class ServiceNameElementParser extends AbstractElementParser
  {
    private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

    private final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                        ThreddsMetadataElementUtils.SERVICE_NAME_ELEMENT_NAME );

    private final ThreddsMetadataBuilder threddsMetadataBuilder;
    private DatasetNodeBuilder datasetNodeBuilder;

    private final DatasetNodeElementParserUtils parentDatasetNodeElementParserUtils;


    public ServiceNameElementParser( XMLEventReader reader,
                                     ThreddsMetadataBuilder threddsMetadataBuilder,
                                     DatasetNodeElementParserUtils parentDatasetNodeElementParserUtils )
            throws ThreddsXmlParserException
    {
      super( reader, elementName );
      this.threddsMetadataBuilder = threddsMetadataBuilder;
      this.parentDatasetNodeElementParserUtils = parentDatasetNodeElementParserUtils;
    }

    protected static boolean isSelfElementStatic( XMLEvent event )
    {
      return isSelfElement( event, elementName );
    }

    protected boolean isSelfElement( XMLEvent event )
    {
      return isSelfElementStatic( event );
    }

    protected void parseCharacterContent( StartElement startElement )
            throws ThreddsXmlParserException
    {
      if ( !startElement.getName().equals( elementName ) )
        throw new IllegalArgumentException( "Start element must be an 'serviceName' element." );

      this.serviceName = StaxThreddsXmlParserUtils.readCharacterContent( startElement, this.reader );

      // Set default service name on parent dataset.
      this.parentDatasetNodeElementParserUtils.setDefaultServiceName( this.serviceName );
    }

    private String serviceName;
    protected ThreddsMetadataBuilder parseStartElement( StartElement startElement )
            throws ThreddsXmlParserException
    {
      if ( !startElement.getName().equals( elementName ) )
        throw new IllegalArgumentException( "Start element must be an 'serviceName' element." );

      this.serviceName = StaxThreddsXmlParserUtils.readCharacterContent( startElement, this.reader );

      return null;
    }

    protected void handleChildStartElement( StartElement startElement, ThreddsBuilder builder ) throws ThreddsXmlParserException
    {
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.readElementAndAnyContent( this.reader );
    }

    protected void postProcessing( ThreddsBuilder builder ) throws ThreddsXmlParserException
    {
      this.parentDatasetNodeElementParserUtils.setDefaultServiceName( this.serviceName );
    }
  }
}