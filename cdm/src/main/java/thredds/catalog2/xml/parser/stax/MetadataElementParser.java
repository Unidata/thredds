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
import thredds.catalog2.xml.names.MetadataElementNames;

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
public class MetadataElementParser extends AbstractElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final DatasetNodeBuilder datasetBuilder;
  private final ThreddsBuilderFactory catBuilderFactory;

  private final DatasetNodeElementParserHelper datasetNodeElementParserHelper;

  private boolean isMetadataElementInherited;

  public MetadataElementParser( XMLEventReader reader,
                                DatasetNodeBuilder datasetNodeBuilder,
                                DatasetNodeElementParserHelper datasetNodeElementParserHelper )
          throws ThreddsXmlParserException
  {
    super( reader, MetadataElementNames.MetadataElement );
    this.datasetBuilder = datasetNodeBuilder;
    this.catBuilderFactory = null;
    this.datasetNodeElementParserHelper = datasetNodeElementParserHelper;
    this.isMetadataElementInherited = false;
  }

  public MetadataElementParser( XMLEventReader reader,
                                ThreddsBuilderFactory catBuilderFactory,
                                DatasetNodeElementParserHelper datasetNodeElementParserHelper )
          throws ThreddsXmlParserException
  {
    super( reader, MetadataElementNames.MetadataElement );
    this.datasetBuilder = null;
    this.catBuilderFactory = catBuilderFactory;
    this.datasetNodeElementParserHelper = datasetNodeElementParserHelper;
    this.isMetadataElementInherited = false;
  }

  protected static boolean isSelfElementStatic( XMLEvent event )
  {
    return isSelfElement( event, MetadataElementNames.MetadataElement );
  }

  protected boolean isSelfElement( XMLEvent event )
  {
    return isSelfElement( event, MetadataElementNames.MetadataElement );
  }

  public boolean doesMetadataElementGetInherited()
  {
    return this.isMetadataElementInherited;
  }

  protected MetadataBuilder parseStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();

    MetadataBuilder builder = null;
    if ( this.datasetBuilder != null )
      builder = this.datasetBuilder.addMetadata();
    else if ( catBuilderFactory != null )
      builder = catBuilderFactory.newMetadataBuilder();
    else
      throw new ThreddsXmlParserException( "" );

    // Determine if this metadata element gets inherited.
    Attribute inheritedAtt = startElement.getAttributeByName( MetadataElementNames.MetadataElement_Inherited );
    if ( inheritedAtt != null && inheritedAtt.getValue().equalsIgnoreCase( "true" ))
      this.isMetadataElementInherited = true;

    Attribute titleAtt = startElement.getAttributeByName( MetadataElementNames.MetadataElement_XlinkTitle );
    Attribute externalRefAtt = startElement.getAttributeByName( MetadataElementNames.MetadataElement_XlinkHref );
    if ( titleAtt == null && externalRefAtt == null )
    {
      builder.setContainedContent( true );
      return builder;
    }
    if ( titleAtt == null || externalRefAtt == null )
      throw new ThreddsXmlParserException( "MetadataBuilder with link has a null title or link URL ");
    String title = titleAtt.getValue();
    String uriString = externalRefAtt.getValue();
    URI uri = null;
    try
    {
      uri = new URI( uriString );
    }
    catch ( URISyntaxException e )
    {
      throw new ThreddsXmlParserException( "MetadataBuilder with link has link with bad URI syntax.", e);
    }

    builder.setContainedContent( false );
    builder.setTitle( title );
    builder.setExternalReference( uri );

    return builder;
  }
  private StringBuilder content = null;
  protected void handleChildStartElement( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( !( builder instanceof DatasetNodeBuilder ) )
      throw new IllegalArgumentException( "Given ThreddsBuilder must be an instance of DatasetNodeBuilder." );
    DatasetNodeBuilder dsNodeBuilder = (DatasetNodeBuilder) builder;

    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( ThreddsMetadataElementParser.isSelfElementStatic( startElement ) )
    {
      ThreddsMetadataElementParser parser = new ThreddsMetadataElementParser( this.reader, this.datasetBuilder, this.datasetNodeElementParserHelper, this.isMetadataElementInherited );
      parser.parse();
    }
    else
    {
      if ( this.content == null )
        this.content = new StringBuilder();
      //if ( !isChildElement( startElement ) )
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      this.content.append( StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader ));
    }
  }

  protected void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( ! ( builder instanceof MetadataBuilder ) )
      throw new IllegalArgumentException( "Builder must be a MetadataBuilder.");
    MetadataBuilder mdBldr = (MetadataBuilder) builder;
    if ( this.content != null )
      mdBldr.setContent( this.content.toString() );
    
    return;
  }
}