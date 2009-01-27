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
import thredds.catalog2.xml.util.DatasetElementUtils;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.XMLEventReader;
import javax.xml.namespace.QName;
import javax.xml.XMLConstants;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DatasetNodeElementParserUtils
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  protected final static QName nameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                        DatasetElementUtils.NAME_ATTRIBUTE_NAME );
  protected final static QName idAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      DatasetElementUtils.ID_ATTRIBUTE_NAME );
  protected final static QName authorityAttName = new QName( XMLConstants.NULL_NS_URI,
                                                             DatasetElementUtils.AUTHORITY_ATTRIBUTE_NAME );

  private final DatasetNodeElementParserUtils parentDatasetNodeElementParserUtils;

  private ThreddsMetadataElementParser threddsMetadataElementParser;

  DatasetNodeElementParserUtils( DatasetNodeElementParserUtils parentDatasetNodeElementParserUtils )
  {
    this.parentDatasetNodeElementParserUtils = parentDatasetNodeElementParserUtils;
  }

  private String idAuthorityThatGetsInherited;
  public void setIdAuthorityThatGetsInherited( String idAuthorityThatGetsInherited)
  {
    this.idAuthorityThatGetsInherited = idAuthorityThatGetsInherited;
  }

  public String getIdAuthorityThatGetsInherited()
  {
    return this.idAuthorityThatGetsInherited;
  }

  public void parseStartElementNameAttribute( StartElement startElement,
                                                     DatasetNodeBuilder dsNodeBuilder )
  {
    Attribute att = startElement.getAttributeByName( nameAttName );
    if ( att != null )
      dsNodeBuilder.setName( att.getValue() );
  }

  public void parseStartElementIdAttribute( StartElement startElement,
                                                   DatasetNodeBuilder dsNodeBuilder )
  {
    Attribute att = startElement.getAttributeByName( idAttName );
    if ( att != null )
      dsNodeBuilder.setId( att.getValue() );
  }

  public void parseStartElementIdAuthorityAttribute( StartElement startElement,
                                                            DatasetNodeBuilder dsNodeBuilder )
  {
    Attribute att = startElement.getAttributeByName( authorityAttName );
    if ( att != null )
      dsNodeBuilder.setId( att.getValue() );
  }

  public boolean handleBasicChildStartElement( StartElement startElement,
                                                      XMLEventReader reader,
                                                      DatasetNodeBuilder dsNodeBuilder )
          throws ThreddsXmlParserException
  {
    if ( PropertyElementParser.isSelfElementStatic( startElement ))
    {
      PropertyElementParser parser = new PropertyElementParser( reader, dsNodeBuilder);
      parser.parse();
      return true;
    }
    else if ( MetadataElementParser.isSelfElementStatic( startElement ))
    {
      MetadataElementParser parser = new MetadataElementParser( reader, dsNodeBuilder, this );
      parser.parse();
      return true;
    }
    else if ( ThreddsMetadataElementParser.isSelfElementStatic( startElement ))
    {
      if ( this.threddsMetadataElementParser == null )
        this.threddsMetadataElementParser = new ThreddsMetadataElementParser( reader, dsNodeBuilder, this);
      this.threddsMetadataElementParser.parse();
      return true;
    }
    else
      return false;
  }
  public boolean handleCollectionChildStartElement( StartElement startElement,
                                                           XMLEventReader reader,
                                                           DatasetNodeBuilder dsNodeBuilder )
          throws ThreddsXmlParserException
  {
    if ( DatasetElementParser.isSelfElementStatic( startElement ))
    {
      DatasetElementParser parser = new DatasetElementParser( reader, dsNodeBuilder, this);
      parser.parse();
      return true;
    }
    else if ( CatalogRefElementParser.isSelfElementStatic( startElement ))
    {
      CatalogRefElementParser parser = new CatalogRefElementParser( reader, dsNodeBuilder, this);
      parser.parse();
      return true;
    }
    else
      return false;
  }

  public void postProcessing( ThreddsBuilder builder )
  {
    if ( !( builder instanceof DatasetNodeBuilder ) )
      throw new IllegalArgumentException( "Given ThreddsBuilder must be an instance of DatasetNodeBuilder." );
    DatasetNodeBuilder datasetNodeBuilder = (DatasetNodeBuilder) builder;

    // ToDo Deal with inherited metadata. Crawl up DatasetNodeBuilder heirarchy and gather inherited metadata.
    if ( this.defaultServiceName == null )
      this.defaultServiceName = this.getInheritedDefaultServiceName( this );
  }

  /**
   * The name of the service used by any access of this datasetNode
   * that does not explicitly specify a service.
   */
  private String defaultServiceName;
  String getDefaultServiceName()
  { return this.defaultServiceName; }

  void setDefaultServiceName( String defaultServiceName )
  { this.defaultServiceName = defaultServiceName; }

  /**
   * The default serviceName
   */
  private String defaultServiceNameThatGetsInherited;

  protected void setDefaultServiceNameThatGetsInherited( String defaultServiceNameThatGetsInherited )
  {
    this.defaultServiceNameThatGetsInherited = defaultServiceNameThatGetsInherited;
  }

  protected String getDefaultServiceNameThatGetsInherited()
  {
    return this.defaultServiceNameThatGetsInherited;
  }

  private String getInheritedDefaultServiceName( DatasetNodeElementParserUtils selfOrAncestor )
  {
    if ( selfOrAncestor == null )
      return null;
    String curDefServiceName = selfOrAncestor.getDefaultServiceNameThatGetsInherited();
    if ( curDefServiceName == null )
      curDefServiceName = this.getInheritedDefaultServiceName( selfOrAncestor.parentDatasetNodeElementParserUtils );
    return curDefServiceName;
  }
}