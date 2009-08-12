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
import thredds.catalog2.builder.util.ThreddsMetadataBuilderUtils;
import thredds.catalog2.builder.util.MetadataBuilderUtils;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.names.DatasetNodeElementNames;
import thredds.catalog2.xml.names.DatasetElementNames;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.XMLEventReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Helper class for DatasetNodeElementParser that deals with metadata and other
 * information inherited from ancestor DatasetNodes.
 *
 * @author edavis
 * @since 4.0
 */
class DatasetNodeElementParserHelper
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final ThreddsBuilderFactory builderFactory;

  private String defaultServiceName;
  private String defaultServiceNameInheritedByDescendants;
  private String idAuthorityInheritedByDescendants;

  // All metadata applicable to this dataset.
  private List<MetadataElementParser> metadataForThisDataset;
  // All metadata inherited by descendant datasets.
  private List<MetadataElementParser> metadataInheritedByDescendants;

  private ThreddsMetadataElementParser threddsMetadataElementParser;

  private SplitMetadata finalSplitMetadata;

  DatasetNodeElementParserHelper( DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper,
                                  ThreddsBuilderFactory builderFactory )
  {
    if ( parentDatasetNodeElementParserHelper != null)
    {
      List<MetadataElementParser> metadataInheritedFromAncestors
              = parentDatasetNodeElementParserHelper.getMetadataInheritedByDescendants();

      if ( metadataInheritedFromAncestors != null
           && ! metadataInheritedFromAncestors.isEmpty() )
      {
        // Add to list of metadata inherited by descendant datasets.
        this.metadataInheritedByDescendants = new ArrayList<MetadataElementParser>();
        this.metadataInheritedByDescendants.addAll( metadataInheritedFromAncestors);

        // Add to list of metadata applicable to this dataset.
        this.metadataForThisDataset = new ArrayList<MetadataElementParser>();
        this.metadataForThisDataset.addAll( metadataInheritedFromAncestors);
      }

      this.defaultServiceNameInheritedByDescendants = parentDatasetNodeElementParserHelper.getDefaultServiceNameInheritedByDescendants();
      this.idAuthorityInheritedByDescendants = parentDatasetNodeElementParserHelper.getIdAuthorityInheritedByDescendants();
    }

    this.builderFactory = builderFactory;
  }

  void setIdAuthorityInheritedByDescendants( String idAuthorityInheritedByDescendants ) {
    this.idAuthorityInheritedByDescendants = idAuthorityInheritedByDescendants;
  }

  String getIdAuthorityInheritedByDescendants() {
    return this.idAuthorityInheritedByDescendants;
  }

  /** Return the name of the service used by any access of this datasetNode
   * that does not explicitly specify a service. */
  String getDefaultServiceName() {
    return this.defaultServiceName != null
           ? this.defaultServiceName
           : this.defaultServiceNameInheritedByDescendants;
  }

  /** Set the default service name. */
  void setDefaultServiceName( String defaultServiceName ) {
    this.defaultServiceName = defaultServiceName;
  }

  /** Return the name of the service used by any access of this datasetNode
   * that does not explicitly specify a service. */
  String getDefaultServiceNameInheritedByDescendants() {
    return this.defaultServiceNameInheritedByDescendants;
  }

  void setDefaultServiceNameInheritedByDescendants( String defaultServiceNameInheritedByDescendants ) {
    this.defaultServiceNameInheritedByDescendants = defaultServiceNameInheritedByDescendants;
  }

  public List<MetadataElementParser> getMetadataForThisDataset() {
    if ( this.metadataForThisDataset == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( this.metadataForThisDataset);
  }

  List<MetadataElementParser> getMetadataInheritedByDescendants() {
    if ( this.metadataInheritedByDescendants == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( this.metadataInheritedByDescendants);
  }

  void parseStartElementNameAttribute( StartElement startElement,
                                              DatasetNodeBuilder dsNodeBuilder )
  {
    Attribute att = startElement.getAttributeByName( DatasetElementNames.DatasetElement_Name );
    if ( att != null )
      dsNodeBuilder.setName( att.getValue() );
  }

  void parseStartElementIdAttribute( StartElement startElement,
                                            DatasetNodeBuilder dsNodeBuilder )
  {
    Attribute att = startElement.getAttributeByName( DatasetNodeElementNames.DatasetNodeElement_Id );
    if ( att != null )
      dsNodeBuilder.setId( att.getValue() );
  }

  void parseStartElementIdAuthorityAttribute( StartElement startElement,
                                                     DatasetNodeBuilder dsNodeBuilder )
  {
    Attribute att = startElement.getAttributeByName( DatasetNodeElementNames.DatasetNodeElement_Authority );
    if ( att != null )
      dsNodeBuilder.setId( att.getValue() );
  }

  boolean handleBasicChildStartElement( StartElement startElement,
                                               XMLEventReader reader,
                                               DatasetNodeBuilder dsNodeBuilder )
          throws ThreddsXmlParserException
  {
    if ( PropertyElementParser.isSelfElementStatic( startElement ))
    {
      PropertyElementParser parser = new PropertyElementParser( reader, this.builderFactory, dsNodeBuilder );
      parser.parse();
      return true;
    }
    else if ( MetadataElementParser.isSelfElementStatic( startElement ))
    {
      MetadataElementParser parser = new MetadataElementParser( reader, this.builderFactory,
                                                                dsNodeBuilder, this );
      parser.parse();

      if ( this.metadataForThisDataset == null )
        this.metadataForThisDataset = new ArrayList<MetadataElementParser>();
      this.metadataForThisDataset.add( parser);

      if ( parser.doesMetadataElementGetInherited())
      {
        if ( this.metadataInheritedByDescendants == null )
          this.metadataInheritedByDescendants = new ArrayList<MetadataElementParser>();
        this.metadataInheritedByDescendants.add( parser );
      }

      return true;
    }
    else if ( ThreddsMetadataElementParser.isSelfElementStatic( startElement ))
    {
      if ( this.threddsMetadataElementParser == null )
        this.threddsMetadataElementParser = new ThreddsMetadataElementParser( reader,
                                                                              this.builderFactory,
                                                                              dsNodeBuilder,
                                                                              this, false );
      this.threddsMetadataElementParser.parse();
      return true;
    }
    else
      return false;
  }

  boolean handleCollectionChildStartElement( StartElement startElement,
                                                    XMLEventReader reader,
                                                    DatasetNodeBuilder dsNodeBuilder )
          throws ThreddsXmlParserException
  {
    if ( DatasetElementParser.isSelfElementStatic( startElement ))
    {
      DatasetElementParser parser = new DatasetElementParser( reader, this.builderFactory,
                                                              dsNodeBuilder, this);
      parser.parse();
      return true;
    }
    else if ( CatalogRefElementParser.isSelfElementStatic( startElement ))
    {
      CatalogRefElementParser parser = new CatalogRefElementParser( reader, this.builderFactory,
                                                                    dsNodeBuilder, this);
      parser.parse();
      return true;
    }
    else
      return false;
  }

  void postProcessingAfterEndElement()
          throws ThreddsXmlParserException
  {
    if ( this.threddsMetadataElementParser != null )
      this.threddsMetadataElementParser.postProcessingAfterEndElement();

    this.finalSplitMetadata = new SplitMetadata( this.metadataForThisDataset);
  }

  /**
   * Add to the target DatasetNodeBuilder a ThreddsMetadataBuilder that contains
   * all the ThreddsMetadata for the target. The ThreddsMetadataBuilder merges
   * together:
   * 1) the local ThreddsMetadata that was not wrapped in a metadata element,
   * 2) all local ThreddsMetadata that was wrapped in a metadata element, and
   * 3) all inherited ThreddsMetadata.
   *
   * @param dsNodeBuilder the target DatasetNodeBuilder.
   */
  void addFinalThreddsMetadataToDatasetNodeBuilder( DatasetNodeBuilder dsNodeBuilder)
  {
    ThreddsMetadataBuilder unwrappedThreddsMetadataBuilder = null;
    boolean isUnwrappedEmpty = true;
    if ( this.threddsMetadataElementParser != null )
    {
      unwrappedThreddsMetadataBuilder = this.threddsMetadataElementParser.getSelfBuilder();
      isUnwrappedEmpty = unwrappedThreddsMetadataBuilder.isEmpty();
    }

    if ( ! isUnwrappedEmpty || ! this.finalSplitMetadata.threddsMetadata.isEmpty())
    {
      ThreddsMetadataBuilder result = dsNodeBuilder.setNewThreddsMetadataBuilder();
      if ( ! isUnwrappedEmpty )
        ThreddsMetadataBuilderUtils.copyThreddsMetadataBuilder( unwrappedThreddsMetadataBuilder, result );
      for ( MetadataElementParser mdElemParser : this.finalSplitMetadata.threddsMetadata )
        ThreddsMetadataBuilderUtils.copyThreddsMetadataBuilder( mdElemParser.getThreddsMetadataBuilder(), result );
    }
  }

  /**
   * Add to the target DatasetNodeBuilder the list of MetadataBuilders for the target
   * including all inherited and local metadata.
   *
   * @param dsNodeBuilder the target DatasetNodeBuilder.
   */
  void addFinalMetadataToDatasetNodeBuilder( DatasetNodeBuilder dsNodeBuilder )
  {
    for ( MetadataElementParser currentMetadataElemParser : this.finalSplitMetadata.nonThreddsMetadata )
    {
      MetadataBuilder newMetadataBuilder = dsNodeBuilder.addMetadata();
      MetadataBuilderUtils.copyMetadataBuilder( currentMetadataElemParser.getSelfBuilder(), newMetadataBuilder);
    }
  }

  private class SplitMetadata
  {
    final List<MetadataElementParser> threddsMetadata;
    final List<MetadataElementParser> nonThreddsMetadata;

    SplitMetadata( List<MetadataElementParser> metadata )
    {
      if ( metadata == null || metadata.isEmpty())
      {
        this.threddsMetadata = Collections.emptyList();
        this.nonThreddsMetadata = Collections.emptyList();
        return;
      }

      this.threddsMetadata = new ArrayList<MetadataElementParser>();
      this.nonThreddsMetadata = new ArrayList<MetadataElementParser>();

      for ( MetadataElementParser current : metadata )
      {
        if ( current.isContainsThreddsMetadata())
          this.threddsMetadata.add( current);
        else
          this.nonThreddsMetadata.add( current);
      }
    }
  }

}