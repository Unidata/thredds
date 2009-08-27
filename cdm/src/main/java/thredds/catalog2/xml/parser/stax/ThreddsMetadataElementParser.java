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
import thredds.catalog2.xml.names.ThreddsMetadataElementNames;
import thredds.catalog2.ThreddsMetadata;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.XMLEventReader;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class ThreddsMetadataElementParser extends AbstractElementParser
{
  private final DatasetNodeBuilder parentDatasetNodeBuilder;

  private final DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper;

  private final boolean inheritedByDescendants;

  private final ThreddsMetadataBuilder selfBuilder;

  private AbstractElementParser delegate = null;

  ThreddsMetadataElementParser( XMLEventReader reader,
                                ThreddsBuilderFactory builderFactory,
                                DatasetNodeBuilder parentDatasetNodeBuilder,
                                DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper,
                                boolean inheritedByDescendants )
          throws ThreddsXmlParserException
  {
    super( reader, ThreddsMetadataElementNames.ThreddsMetadataElement, builderFactory );
    this.parentDatasetNodeBuilder = parentDatasetNodeBuilder;
    this.parentDatasetNodeElementParserHelper = parentDatasetNodeElementParserHelper;
    this.inheritedByDescendants = inheritedByDescendants;

    this.selfBuilder = builderFactory.newThreddsMetadataBuilder();
  }

    static boolean isSelfElementStatic( XMLEvent event ) {
        if ( ServiceNameElementParser.isSelfElementStatic( event ) )
            return true;
        if ( DataFormatElementParser.isSelfElementStatic( event ) )
            return true;
        if ( DataTypeElementParser.isSelfElementStatic( event ) )
            return true;
        if ( DateElementParser.isSelfElementStatic( event ) )
            return true;
        return false;
    }

    boolean isSelfElement( XMLEvent event ) {
        if ( delegate != null )
            return delegate.isSelfElement( event );
        return isSelfElementStatic( event );
    }

    ThreddsMetadataBuilder getSelfBuilder() {
        return this.selfBuilder;
    }

    void parseStartElement()
            throws ThreddsXmlParserException
    {
        // ThreddsMetadata container object only, no self element exists!
        // So peek at next event to see how to route it.
        StartElement startElement = this.peekAtNextEventIfStartElement();

        if ( ! isSelfElementStatic( startElement ) )
            throw new IllegalArgumentException( "Start element [" + startElement.getName().getLocalPart()
                                                + "] must be one of the THREDDS metadata element." );

        if ( ServiceNameElementParser.isSelfElementStatic( startElement ) )
        {
            this.delegate = new ServiceNameElementParser( this.reader, this.builderFactory, this.selfBuilder,
                                                          this.parentDatasetNodeElementParserHelper,
                                                          this.inheritedByDescendants );
        }
        else if ( DataFormatElementParser.isSelfElementStatic( startElement ) )
        {
            this.delegate = new DataFormatElementParser( this.reader, this.builderFactory, this.selfBuilder );
        }
        else if ( DataTypeElementParser.isSelfElementStatic( startElement ) )
        {
            this.delegate = new DataTypeElementParser( this.reader, this.builderFactory, this.selfBuilder );
        }
        else if ( DateElementParser.isSelfElementStatic( startElement ) )
        {
            this.delegate = new DateElementParser( this.reader, this.builderFactory, this.selfBuilder );
        }
        else
            throw new ThreddsXmlParserException( "" );

        this.delegate.parseStartElement();
    }

    void handleChildStartElement()
            throws ThreddsXmlParserException
    {
        if ( this.delegate == null )
            throw new IllegalStateException( "Proxy delegate is null: "
                                             + StaxThreddsXmlParserUtils.getLocationInfo( this.reader ) );

        this.delegate.handleChildStartElement();
    }

    void postProcessingAfterEndElement()
            throws ThreddsXmlParserException
    {
        if ( this.delegate == null )
            throw new IllegalStateException( "Proxy delegate is null: "
                                             + StaxThreddsXmlParserUtils.getLocationInfo( this.reader ) );

        this.delegate.postProcessingAfterEndElement();
    }

  static class ServiceNameElementParser extends AbstractElementParser
  {
    private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

    private final ThreddsMetadataBuilder threddsMetadataBuilder;
    //private DatasetNodeBuilder datasetNodeBuilder;

    private final DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper;

    private final boolean inheritedByDescendants;

    private String serviceName;


    ServiceNameElementParser( XMLEventReader reader,
                                     ThreddsBuilderFactory builderFactory,
                                     ThreddsMetadataBuilder threddsMetadataBuilder,
                                     DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper,
                                     boolean inheritedByDescendants )
            throws ThreddsXmlParserException
    {
      super( reader, ThreddsMetadataElementNames.ServiceNameElement, builderFactory );
      this.threddsMetadataBuilder = threddsMetadataBuilder;
      this.parentDatasetNodeElementParserHelper = parentDatasetNodeElementParserHelper;
      this.inheritedByDescendants = inheritedByDescendants;
    }

    static boolean isSelfElementStatic( XMLEvent event )
    {
      return isSelfElement( event, ThreddsMetadataElementNames.ServiceNameElement );
    }

    boolean isSelfElement( XMLEvent event )
    {
      return isSelfElementStatic( event );
    }

    ThreddsBuilder getSelfBuilder() {
      return null;
    }

    void parseStartElement()
            throws ThreddsXmlParserException
    {
      this.getNextEventIfStartElementIsMine();
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      this.serviceName = StaxThreddsXmlParserUtils.getCharacterContent( this.reader, ThreddsMetadataElementNames.ServiceNameElement );
    }

    void handleChildStartElement()
            throws ThreddsXmlParserException {
      // Should not have child elements.
    }

    void postProcessingAfterEndElement()
            throws ThreddsXmlParserException
    {
      this.parentDatasetNodeElementParserHelper.setDefaultServiceName( this.serviceName );
      if ( this.inheritedByDescendants )
        this.parentDatasetNodeElementParserHelper.setDefaultServiceNameInheritedByDescendants( this.serviceName );
    }
  }

  /**
   * Parser for THREDDS metadata DataFormat elements.
   */
  static class DataFormatElementParser extends AbstractElementParser
  {
    private final ThreddsMetadataBuilder threddsMetadataBuilder;

    DataFormatElementParser( XMLEventReader reader,
                                    ThreddsBuilderFactory builderFactory,
                                    ThreddsMetadataBuilder threddsMetadataBuilder )
            throws ThreddsXmlParserException
    {
      super( reader, ThreddsMetadataElementNames.DataFormatElement, builderFactory );
      this.threddsMetadataBuilder = threddsMetadataBuilder;
    }

    static boolean isSelfElementStatic( XMLEvent event )
    {
      return isSelfElement( event, ThreddsMetadataElementNames.DataFormatElement );
    }

    boolean isSelfElement( XMLEvent event )
    {
      return isSelfElementStatic( event );
    }

    ThreddsBuilder getSelfBuilder() {
      return null;
    }

    void parseStartElement()
            throws ThreddsXmlParserException
    {
      StartElement startElement = this.getNextEventIfStartElementIsMine();

      String dataFormat = StaxThreddsXmlParserUtils.getCharacterContent( this.reader, ThreddsMetadataElementNames.DataFormatElement );
      this.threddsMetadataBuilder.setDataFormat( dataFormat );
    }

    void handleChildStartElement()
            throws ThreddsXmlParserException
    { }

    void postProcessingAfterEndElement()
            throws ThreddsXmlParserException
    { }
  }

  /**
   * Parser for THREDDS metadata DataType elements.
   */
  static class DataTypeElementParser extends AbstractElementParser
  {
    private final ThreddsMetadataBuilder threddsMetadataBuilder;

    DataTypeElementParser( XMLEventReader reader,
                           ThreddsBuilderFactory builderFactory,
                           ThreddsMetadataBuilder threddsMetadataBuilder )
            throws ThreddsXmlParserException
    {
      super( reader, ThreddsMetadataElementNames.DataTypeElement, builderFactory );
      this.threddsMetadataBuilder = threddsMetadataBuilder;
    }

    static boolean isSelfElementStatic( XMLEvent event ) {
        return isSelfElement( event, ThreddsMetadataElementNames.DataTypeElement );
    }

    boolean isSelfElement( XMLEvent event ) {
        return isSelfElementStatic( event );
    }

    ThreddsBuilder getSelfBuilder() {
      return null;
    }

    void parseStartElement() throws ThreddsXmlParserException {
        StartElement startElement = this.getNextEventIfStartElementIsMine();

        String dataType = StaxThreddsXmlParserUtils.getCharacterContent( this.reader,
                                                                         ThreddsMetadataElementNames.DataTypeElement );
        this.threddsMetadataBuilder.setDataType( dataType );
    }

    void handleChildStartElement() throws ThreddsXmlParserException {
        return;
    }

    void postProcessingAfterEndElement() throws ThreddsXmlParserException {
        return;
    }
  }

  /**
   * Parser for THREDDS metadata DataType elements.
   */
  static class DateElementParser extends AbstractElementParser
  {
    private final ThreddsMetadataBuilder threddsMetadataBuilder;

    DateElementParser( XMLEventReader reader,
                       ThreddsBuilderFactory builderFactory,
                       ThreddsMetadataBuilder threddsMetadataBuilder )
            throws ThreddsXmlParserException
    {
      super( reader, ThreddsMetadataElementNames.DateElement, builderFactory );
      this.threddsMetadataBuilder = threddsMetadataBuilder;
    }

    static boolean isSelfElementStatic( XMLEvent event ) {
        return isSelfElement( event, ThreddsMetadataElementNames.DateElement );
    }

    boolean isSelfElement( XMLEvent event ) {
        return isSelfElementStatic( event );
    }

    ThreddsBuilder getSelfBuilder() {
      return null;
    }

    void parseStartElement() throws ThreddsXmlParserException {
        StartElement startElement = this.getNextEventIfStartElementIsMine();

        Attribute typeAtt = startElement.getAttributeByName( ThreddsMetadataElementNames.DateElement_Type );
        String typeString = typeAtt != null ? typeAtt.getValue() : null;

        Attribute formatAtt = startElement.getAttributeByName( ThreddsMetadataElementNames.DateElement_Format );
        String formatString = formatAtt != null ? formatAtt.getValue() : null;


        String date = StaxThreddsXmlParserUtils.getCharacterContent( this.reader,
                                                                     ThreddsMetadataElementNames.DateElement );
        ThreddsMetadata.DatePointType type = ThreddsMetadata.DatePointType.getTypeForLabel( typeString );
        if ( type.equals( ThreddsMetadata.DatePointType.Untyped ) || type.equals( ThreddsMetadata.DatePointType.Other) )
            this.threddsMetadataBuilder.addOtherDatePointBuilder( date, formatString, typeString );
        else if ( type.equals( ThreddsMetadata.DatePointType.Created) )
            this.threddsMetadataBuilder.setCreatedDatePointBuilder( date, formatString );
        else if ( type.equals( ThreddsMetadata.DatePointType.Modified ) )
            this.threddsMetadataBuilder.setModifiedDatePointBuilder( date, formatString );
        else if ( type.equals( ThreddsMetadata.DatePointType.Valid ) )
            this.threddsMetadataBuilder.setValidDatePointBuilder( date, formatString );
        else if ( type.equals( ThreddsMetadata.DatePointType.Issued ) )
            this.threddsMetadataBuilder.setIssuedDatePointBuilder( date, formatString );
        else if ( type.equals( ThreddsMetadata.DatePointType.Available ) )
            this.threddsMetadataBuilder.setAvailableDatePointBuilder( date, formatString );
        else if ( type.equals( ThreddsMetadata.DatePointType.MetadataCreated ) )
            this.threddsMetadataBuilder.setMetadataCreatedDatePointBuilder( date, formatString );
        else if ( type.equals( ThreddsMetadata.DatePointType.MetadataModified ) )
            this.threddsMetadataBuilder.setMetadataModifiedDatePointBuilder( date, formatString );
        else
        {
            String msg = "Unsupported DatePointType [" + typeString + "].";
            ThreddsXmlParserIssue parserIssue = StaxThreddsXmlParserUtils
                    .createIssueForUnexpectedEvent( msg, ThreddsXmlParserIssue.Severity.WARNING, this.reader, startElement );
            log.error( "parseStartElement(): " + parserIssue.getMessage() );
            throw new ThreddsXmlParserException( parserIssue );
        }

    }

    void handleChildStartElement() throws ThreddsXmlParserException {
        return;
    }

    void postProcessingAfterEndElement() throws ThreddsXmlParserException {
        return;
    }
  }
}