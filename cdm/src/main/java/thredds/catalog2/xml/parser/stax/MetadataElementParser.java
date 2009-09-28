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
import thredds.catalog2.xml.parser.ThreddsXmlParserIssue;
import thredds.catalog2.xml.names.MetadataElementNames;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class MetadataElementParser extends AbstractElementParser
{
  private final DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper;
  private final DatasetNodeBuilder parentDatasetNodeBuilder;

  private final MetadataBuilder selfBuilder;

  private boolean isInheritedByDescendants = false;
  private boolean containsThreddsMetadata = false;
  private String title;
  private URI externalRefUri;
  private boolean isContainedContent = false;
  private StringBuilder content;

  private ThreddsMetadataElementParser.Factory threddsMetadataElementParserFactory;
  private ThreddsMetadataElementParser threddsMetadataElementParser;

  private MetadataElementParser( QName elementName,
                                 XMLEventReader reader,
                                 ThreddsBuilderFactory builderFactory,
                                 DatasetNodeBuilder parentDatasetNodeBuilder,
                                 DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
  {
    super( elementName, reader, builderFactory );
    this.parentDatasetNodeBuilder = parentDatasetNodeBuilder;
    this.parentDatasetNodeElementParserHelper = parentDatasetNodeElementParserHelper;

    this.threddsMetadataElementParserFactory = new ThreddsMetadataElementParser.Factory();

    this.selfBuilder = builderFactory.newMetadataBuilder();
  }

  MetadataBuilder getSelfBuilder() {
    if ( this.containsThreddsMetadata)
      return null;
    return this.selfBuilder;
  }

  boolean doesMetadataElementGetInherited() {
    return this.isInheritedByDescendants;
  }

  boolean isContainsThreddsMetadata() {
    return containsThreddsMetadata;
  }

  ThreddsMetadataBuilder getThreddsMetadataBuilder()
  {
    if ( ! this.containsThreddsMetadata )
      return this.builderFactory.newThreddsMetadataBuilder();
    return this.threddsMetadataElementParser.getSelfBuilder();
  }

  boolean addThreddsMetadataBuilderToList( List<ThreddsMetadataBuilder> tmBuilders ) {
    if ( this.getSelfBuilder() != null )
      return tmBuilders.add(this.threddsMetadataElementParser.getSelfBuilder());
    return false;
  }

  void parseStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();

    // Determine if this metadata element gets inherited.
    Attribute inheritedAtt = startElement.getAttributeByName( MetadataElementNames.MetadataElement_Inherited );
    if ( inheritedAtt != null && inheritedAtt.getValue().equalsIgnoreCase( "true" ) )
      this.isInheritedByDescendants = true;

    // If contains "threddsMetadataGroup" elements, drop metadata wrapper
    StartElement nextElement = this.peekAtNextEventIfStartElement();

    if ( this.threddsMetadataElementParserFactory.isEventMyStartElement( nextElement ) )
    {
      this.containsThreddsMetadata = true;
      return;
    }

    Attribute titleAtt = startElement.getAttributeByName( MetadataElementNames.MetadataElement_XlinkTitle );
    Attribute externalRefAtt = startElement.getAttributeByName( MetadataElementNames.MetadataElement_XlinkHref );
    if ( titleAtt == null && externalRefAtt == null )
    {
      this.selfBuilder.setContainedContent( true );
      return;
    }
    if ( titleAtt == null || externalRefAtt == null )
    {
      String msg = "External reference metadata element has null title or URI.";
      ThreddsXmlParserIssue issue = StaxThreddsXmlParserUtils.createIssueForUnexpectedElement( msg, this.reader );
      log.warn( "parseStartElement(): " + issue.getMessage());
      // ToDo Gather issues rather than throw exception.
      throw new ThreddsXmlParserException( issue);
    }

    this.selfBuilder.setTitle( titleAtt.getValue() );

    String uriString = externalRefAtt.getValue();
    try
    {
      this.selfBuilder.setExternalReference( new URI( uriString ));
    }
    catch ( URISyntaxException e )
    {
      String msg = "External reference metadata element with bad URI syntax [" + uriString + "].";
      ThreddsXmlParserIssue issue = StaxThreddsXmlParserUtils.createIssueForException( msg, this.reader, e );
      log.warn( "parseStartElement(): " + issue.getMessage(), e );
      // ToDo Gather issues rather than throw exception.
      throw new ThreddsXmlParserException( issue );
    }
  }

  void handleChildStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( this.containsThreddsMetadata )
    {
      if ( this.threddsMetadataElementParserFactory.isEventMyStartElement( startElement ) )
      {
        if ( this.threddsMetadataElementParser == null )
        {
          this.threddsMetadataElementParser
                  = this.threddsMetadataElementParserFactory.getNewParser( this.reader,
                                                                           this.builderFactory,
                                                                           this.parentDatasetNodeBuilder,
                                                                           this.parentDatasetNodeElementParserHelper,
                                                                           this.isInheritedByDescendants );
        }
        this.threddsMetadataElementParser.parse();
        
      }
      else
      {
        String msg = "Expecting THREDDS Metadata, got non-THREDDS Metadata";
        ThreddsXmlParserIssue issue = StaxThreddsXmlParserUtils.createIssueForUnexpectedElement( msg, this.reader );
        // ToDo Instead of throwing exception, gather issues and continue.
        throw new ThreddsXmlParserException( issue );
      }

    }
    else
    {
      if ( this.threddsMetadataElementParserFactory.isEventMyStartElement( startElement ) )
      {
        String msg = "Unexpected THREDDS Metadata";
        ThreddsXmlParserIssue issue = StaxThreddsXmlParserUtils.createIssueForUnexpectedElement( msg, this.reader );
        // ToDo Instead of throwing exception, gather issues and continue.
        throw new ThreddsXmlParserException( issue);
      }
      else
      {
        if ( this.isContainedContent )
        {
          if ( this.content == null )
            this.content = new StringBuilder();
          this.content.append( StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader ) );
        }
        else
        {
          String msg = "Unexpected content";
          ThreddsXmlParserIssue issue = StaxThreddsXmlParserUtils.createIssueForUnexpectedElement( msg, this.reader );
          // ToDo Instead of throwing exception, gather issues and continue.
          throw new ThreddsXmlParserException( issue );
        }
      }
    }
  }

  void postProcessingAfterEndElement()
          throws ThreddsXmlParserException
  {
    if ( ! this.containsThreddsMetadata )
    {
      if ( this.content != null )
        this.selfBuilder.setContent( this.content.toString() );
    }
  }

  static class Factory
  {
    private QName elementName;

    Factory() {
      this.elementName = MetadataElementNames.MetadataElement;
    }

    boolean isEventMyStartElement( XMLEvent event ) {
      return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
    }

    MetadataElementParser getNewParser( XMLEventReader reader,
                                        ThreddsBuilderFactory builderFactory,
                                        DatasetNodeBuilder parentDatasetNodeBuilder,
                                        DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper )
    {
      return new MetadataElementParser( this.elementName, reader, builderFactory, parentDatasetNodeBuilder, parentDatasetNodeElementParserHelper );
    }
  }
}