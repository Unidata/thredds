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
import javax.xml.namespace.QName;

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

  private final ServiceNameElementParser.Factory serviceNameParserFactory;
  private final DataFormatElementParser.Factory dataFormatParserFactory;
  private final DataTypeElementParser.Factory dataTypeParserFactory;
  private final DateElementParser.Factory dateParserFactory;
  private final AuthorityElementParser.Factory authorityParserFactory;
  private final DocumentationElementParser.Factory documentationParserFactory;
  private final KeyphraseElementParser.Factory keyphraseParserFactory;
  private final ProjectElementParser.Factory projectNameParserFactory;
  private final CreatorElementParser.Factory creatorParserFactory;
  private final PublisherElementParser.Factory publisherParserFactory;
  private final ContributorElementParser.Factory contribParserFactory;
  private final TimeCoverageElementParser.Factory timeCovParserFactory;
  private final VariableGroupElementParser.Factory variableGroupParserFactory;

  private ThreddsMetadataElementParser( QName elementName,
                                        ServiceNameElementParser.Factory serviceNameParserFactory,
                                        DataFormatElementParser.Factory dataFormatParserFactory,
                                        DataTypeElementParser.Factory dataTypeParserFactory,
                                        DateElementParser.Factory dateParserFactory,
                                        AuthorityElementParser.Factory authorityParserFactory,
                                        DocumentationElementParser.Factory documentationParserFactory,
                                        KeyphraseElementParser.Factory keyphraseParserFactory,
                                        ProjectElementParser.Factory projectNameParserFactory,
                                        CreatorElementParser.Factory creatorParserFactory,
                                        PublisherElementParser.Factory publisherParserFactory,
                                        ContributorElementParser.Factory contribParserFactory,
                                        TimeCoverageElementParser.Factory timeCovParserFactory,
                                        VariableGroupElementParser.Factory variableGroupParserFactory,
                                        XMLEventReader reader,
                                        ThreddsBuilderFactory builderFactory,
                                        DatasetNodeBuilder parentDatasetNodeBuilder,
                                        DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper,
                                        boolean inheritedByDescendants )
  {
    super( elementName, reader, builderFactory );
    this.parentDatasetNodeBuilder = parentDatasetNodeBuilder;
    this.parentDatasetNodeElementParserHelper = parentDatasetNodeElementParserHelper;
    this.inheritedByDescendants = inheritedByDescendants;

    this.selfBuilder = builderFactory.newThreddsMetadataBuilder();
    
    this.serviceNameParserFactory = serviceNameParserFactory;
    this.dataFormatParserFactory = dataFormatParserFactory;
    this.dataTypeParserFactory = dataTypeParserFactory;
    this.dateParserFactory = dateParserFactory;
    this.authorityParserFactory = authorityParserFactory;
    this.documentationParserFactory = documentationParserFactory;
    this.keyphraseParserFactory = keyphraseParserFactory;
    this.projectNameParserFactory = projectNameParserFactory;
    this.creatorParserFactory = creatorParserFactory;
    this.publisherParserFactory = publisherParserFactory;
    this.contribParserFactory = contribParserFactory;
    this.variableGroupParserFactory = variableGroupParserFactory;
    this.timeCovParserFactory = timeCovParserFactory;
  }

    boolean isSelfElement( XMLEvent event ) {
      return delegate.isSelfElement( event );
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

    if ( this.serviceNameParserFactory.isEventMyStartElement( startElement ) )
      this.delegate = this.serviceNameParserFactory.getNewParser( this.reader, this.builderFactory,
                                                                         this.selfBuilder,
                                                                         this.parentDatasetNodeElementParserHelper,
                                                                         this.inheritedByDescendants );
    else if ( this.dataFormatParserFactory.isEventMyStartElement( startElement ) )
      this.delegate = this.dataFormatParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder );
    else if ( this.dataTypeParserFactory.isEventMyStartElement( startElement ) )
      this.delegate = this.dataTypeParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder );
    else if ( this.dateParserFactory.isEventMyStartElement( startElement ) )
      this.delegate = this.dateParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder );
    else if ( this.authorityParserFactory.isEventMyStartElement( startElement ) )
    {
      this.delegate = this.authorityParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder,
                                                                this.parentDatasetNodeElementParserHelper,
                                                                this.inheritedByDescendants );
    }
    else if ( this.documentationParserFactory.isEventMyStartElement( startElement ) )
      this.delegate = this.documentationParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder );
    else if ( this.keyphraseParserFactory.isEventMyStartElement( startElement ) )
      this.delegate = this.keyphraseParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder );
    else if ( this.projectNameParserFactory.isEventMyStartElement( startElement ) )
      this.delegate = this.projectNameParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder );
    else if ( this.creatorParserFactory.isEventMyStartElement( startElement ) )
      this.delegate = this.creatorParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder );
    else if ( this.publisherParserFactory.isEventMyStartElement( startElement ) )
      this.delegate = this.publisherParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder );
    else if ( this.contribParserFactory.isEventMyStartElement( startElement ) )
      this.delegate = this.contribParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder );
    else if ( this.timeCovParserFactory.isEventMyStartElement( startElement ) )
      this.delegate = this.timeCovParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder );
    else if ( this.variableGroupParserFactory.isEventMyStartElement( startElement ) )
      this.delegate = this.variableGroupParserFactory.getNewParser( this.reader, this.builderFactory, this.selfBuilder );
    else
      throw new ThreddsXmlParserException( "Not a recognized ThreddsMetadata child element [" + startElement.getName().getLocalPart() + "]." );

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

  static class Factory
  {
    private final QName elementName;
    private final ServiceNameElementParser.Factory serviceNameParserFactory;
    private final DataFormatElementParser.Factory dataFormatParserFactory;
    private final DataTypeElementParser.Factory dataTypeParserFactory;
    private final DateElementParser.Factory dateParserFactory;
    private final AuthorityElementParser.Factory authorityParserFactory;
    private final DocumentationElementParser.Factory documentationParserFactory;
    private final KeyphraseElementParser.Factory keyphraseParserFactory;
    private final ProjectElementParser.Factory projectNameParserFactory;
    private final CreatorElementParser.Factory creatorParserFactory;
    private final PublisherElementParser.Factory publisherParserFactory;
    private final ContributorElementParser.Factory contribParserFactory;
    private final TimeCoverageElementParser.Factory timeCovParserFactory;
    private final VariableGroupElementParser.Factory varGroupParserFactory;


    Factory() {
      this.elementName = ThreddsMetadataElementNames.ThreddsMetadataElement;
      this.serviceNameParserFactory = new ServiceNameElementParser.Factory();
      this.dataFormatParserFactory = new DataFormatElementParser.Factory();
      this.dataTypeParserFactory = new DataTypeElementParser.Factory();
      this.dateParserFactory = new DateElementParser.Factory();
      this.authorityParserFactory = new AuthorityElementParser.Factory();
      this.documentationParserFactory = new DocumentationElementParser.Factory();
      this.keyphraseParserFactory = new KeyphraseElementParser.Factory();
      this.projectNameParserFactory = new ProjectElementParser.Factory();
      this.creatorParserFactory = new CreatorElementParser.Factory();
      this.publisherParserFactory = new PublisherElementParser.Factory();
      this.contribParserFactory = new ContributorElementParser.Factory();
      this.timeCovParserFactory = new TimeCoverageElementParser.Factory();
      this.varGroupParserFactory = new VariableGroupElementParser.Factory();
    }

    boolean isEventMyStartElement( XMLEvent event )
    {
      if ( this.serviceNameParserFactory.isEventMyStartElement( event ) )
        return true;
      if ( this.dataFormatParserFactory.isEventMyStartElement( event ) )
        return true;
      if ( this.dataTypeParserFactory.isEventMyStartElement( event ) )
        return true;
      if ( this.dateParserFactory.isEventMyStartElement( event ) )
        return true;
      if ( this.authorityParserFactory.isEventMyStartElement( event ) )
        return true;
      if ( this.documentationParserFactory.isEventMyStartElement( event ) )
        return true;
      if ( this.keyphraseParserFactory.isEventMyStartElement( event ) )
        return true;
      if ( this.projectNameParserFactory.isEventMyStartElement( event ) )
        return true;
      if ( this.creatorParserFactory.isEventMyStartElement( event ) )
        return true;
      if ( this.publisherParserFactory.isEventMyStartElement( event ) )
        return true;
      if ( this.contribParserFactory.isEventMyStartElement( event ) )
        return true;
      if ( this.timeCovParserFactory.isEventMyStartElement( event ) )
        return true;
      if ( this.varGroupParserFactory.isEventMyStartElement( event ) )
        return true;
      return false;
    }

    ThreddsMetadataElementParser getNewParser( XMLEventReader reader,
                                               ThreddsBuilderFactory builderFactory,
                                               DatasetNodeBuilder parentDatasetNodeBuilder,
                                               DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper,
                                               boolean inheritedByDescendants )
    {
      return new ThreddsMetadataElementParser( this.elementName, this.serviceNameParserFactory,
                                               this.dataFormatParserFactory, this.dataTypeParserFactory,
                                               this.dateParserFactory, this.authorityParserFactory,
                                               this.documentationParserFactory, this.keyphraseParserFactory,
                                               this.projectNameParserFactory, this.creatorParserFactory,
                                               this.publisherParserFactory, this.contribParserFactory,
                                               this.timeCovParserFactory, this.varGroupParserFactory,
                                               reader, builderFactory, parentDatasetNodeBuilder,
                                               parentDatasetNodeElementParserHelper, inheritedByDescendants );
    }
  }

  static class ServiceNameElementParser extends AbstractElementParser
  {
    private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

    private final ThreddsMetadataBuilder threddsMetadataBuilder;
    //private DatasetNodeBuilder datasetNodeBuilder;

    private final DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper;

    private final boolean inheritedByDescendants;

    private String serviceName;


    private ServiceNameElementParser( QName elementName,
                                      XMLEventReader reader,
                                      ThreddsBuilderFactory builderFactory,
                                      ThreddsMetadataBuilder threddsMetadataBuilder,
                                      DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper,
                                      boolean inheritedByDescendants )
    {
      super( elementName, reader, builderFactory );
      this.threddsMetadataBuilder = threddsMetadataBuilder;
      this.parentDatasetNodeElementParserHelper = parentDatasetNodeElementParserHelper;
      this.inheritedByDescendants = inheritedByDescendants;
    }

    ThreddsBuilder getSelfBuilder() {
      return null;
    }

    void parseStartElement()
            throws ThreddsXmlParserException
    {
      this.getNextEventIfStartElementIsMine();
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      this.serviceName = StaxThreddsXmlParserUtils.getCharacterContent( this.reader, this.elementName );
    }

    void handleChildStartElement()
            throws ThreddsXmlParserException {
      // Should not have child elements.
    }

    void postProcessingAfterEndElement()
            throws ThreddsXmlParserException
    {
      this.parentDatasetNodeElementParserHelper.setDefaultServiceNameSpecifiedInSelf( this.serviceName );
      if ( this.inheritedByDescendants )
        this.parentDatasetNodeElementParserHelper.setDefaultServiceNameToBeInheritedByDescendants( this.serviceName );
    }

    static class Factory
    {
      private QName elementName;

      Factory() {
        this.elementName = ThreddsMetadataElementNames.ServiceNameElement;
      }

      boolean isEventMyStartElement( XMLEvent event ) {
        return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
      }

      ServiceNameElementParser getNewParser( XMLEventReader reader,
                                             ThreddsBuilderFactory builderFactory,
                                             ThreddsMetadataBuilder threddsMetadataBuilder,
                                             DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper,
                                             boolean inheritedByDescendants )
      {
        return new ServiceNameElementParser( this.elementName, reader, builderFactory, threddsMetadataBuilder,
                                             parentDatasetNodeElementParserHelper, inheritedByDescendants );
      }
    }
  }

  /**
   * Parser for THREDDS metadata DataFormat elements.
   */
  static class DataFormatElementParser extends AbstractElementParser
  {
    private final ThreddsMetadataBuilder threddsMetadataBuilder;

    private DataFormatElementParser( QName elementName,
                                     XMLEventReader reader,
                                     ThreddsBuilderFactory builderFactory,
                                     ThreddsMetadataBuilder threddsMetadataBuilder )
    {
      super( elementName, reader, builderFactory );
      this.threddsMetadataBuilder = threddsMetadataBuilder;
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

    static class Factory
    {
      private QName elementName;

      Factory() {
        this.elementName = ThreddsMetadataElementNames.DataFormatElement;
      }

      boolean isEventMyStartElement( XMLEvent event ) {
        return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
      }

      DataFormatElementParser getNewParser( XMLEventReader reader,
                                            ThreddsBuilderFactory builderFactory,
                                            ThreddsMetadataBuilder parentBuilder )
      {
        return new DataFormatElementParser( this.elementName, reader, builderFactory, parentBuilder );
      }
    }
  }

  /**
   * Parser for THREDDS metadata DataType elements.
   */
  static class DataTypeElementParser extends AbstractElementParser
  {
    private final ThreddsMetadataBuilder threddsMetadataBuilder;

    private DataTypeElementParser( QName elementName,
                                   XMLEventReader reader,
                                   ThreddsBuilderFactory builderFactory,
                                   ThreddsMetadataBuilder threddsMetadataBuilder )
    {
      super( elementName, reader, builderFactory );
      this.threddsMetadataBuilder = threddsMetadataBuilder;
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

    static class Factory
    {
      private QName elementName;

      Factory() {
        this.elementName = ThreddsMetadataElementNames.DataTypeElement;
      }

      boolean isEventMyStartElement( XMLEvent event ) {
        return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
      }

      DataTypeElementParser getNewParser( XMLEventReader reader,
                                          ThreddsBuilderFactory builderFactory,
                                          ThreddsMetadataBuilder parentBuilder )
      {
        return new DataTypeElementParser( this.elementName, reader, builderFactory, parentBuilder );
      }
    }
  }

  /**
   * Parser for THREDDS metadata DataType elements.
   */
  static class DateElementParser extends AbstractElementParser
  {
    private final ThreddsMetadataBuilder threddsMetadataBuilder;

    private DateElementParser( QName elementName,
                               XMLEventReader reader,
                               ThreddsBuilderFactory builderFactory,
                               ThreddsMetadataBuilder threddsMetadataBuilder )
    {
      super( elementName, reader, builderFactory );
      this.threddsMetadataBuilder = threddsMetadataBuilder;
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


        String date = StaxThreddsXmlParserUtils.getCharacterContent( this.reader, this.elementName );
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

    static class Factory
    {
      private QName elementName;

      Factory() {
        this.elementName = ThreddsMetadataElementNames.DateElement;
      }

      boolean isEventMyStartElement( XMLEvent event ) {
        return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
      }

      DateElementParser getNewParser( XMLEventReader reader,
                                         ThreddsBuilderFactory builderFactory,
                                         ThreddsMetadataBuilder parentBuilder )
      {
        return new DateElementParser( this.elementName, reader, builderFactory, parentBuilder );
      }
    }
  }

  static class AuthorityElementParser extends AbstractElementParser
  {
    private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

    private final ThreddsMetadataBuilder threddsMetadataBuilder;

    private final DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper;

    private final boolean inheritedByDescendants;

    private String idAuthority;

    private AuthorityElementParser( QName elementName,
                                    XMLEventReader reader,
                                    ThreddsBuilderFactory builderFactory,
                                    ThreddsMetadataBuilder threddsMetadataBuilder,
                                    DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper,
                                    boolean inheritedByDescendants )
    {
      super( elementName, reader, builderFactory );
      this.threddsMetadataBuilder = threddsMetadataBuilder;
      this.parentDatasetNodeElementParserHelper = parentDatasetNodeElementParserHelper;
      this.inheritedByDescendants = inheritedByDescendants;
    }

    ThreddsBuilder getSelfBuilder() {
      return null;
    }

    void parseStartElement()
            throws ThreddsXmlParserException
    {
      this.getNextEventIfStartElementIsMine();
      this.idAuthority = StaxThreddsXmlParserUtils.getCharacterContent( this.reader, this.elementName );
    }

    void handleChildStartElement()
            throws ThreddsXmlParserException
    {
      // Should not have child elements.
    }

    void postProcessingAfterEndElement()
            throws ThreddsXmlParserException
    {
      this.parentDatasetNodeElementParserHelper.setIdAuthoritySpecifiedInSelf( this.idAuthority );
      if ( this.inheritedByDescendants )
        this.parentDatasetNodeElementParserHelper.setIdAuthorityToBeInheritedByDescendants( this.idAuthority );
    }

    static class Factory
    {
      private QName elementName;

      Factory() {
        this.elementName = ThreddsMetadataElementNames.AuthorityElement;
      }

      boolean isEventMyStartElement( XMLEvent event ) {
        return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
      }

      AuthorityElementParser getNewParser( XMLEventReader reader,
                                           ThreddsBuilderFactory builderFactory,
                                           ThreddsMetadataBuilder parentBuilder,
                                           DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper,
                                           boolean inheritedByDescendants )
      {
        return new AuthorityElementParser( this.elementName, reader, builderFactory, parentBuilder,
                                           parentDatasetNodeElementParserHelper, inheritedByDescendants);
      }
    }
  }

  static class DocumentationElementParser extends AbstractElementParser
  {
    private final ThreddsMetadataBuilder threddsMetadataBuilder;

    private DocumentationElementParser( QName elementName,
                                        XMLEventReader reader,
                                        ThreddsBuilderFactory builderFactory,
                                        ThreddsMetadataBuilder threddsMetadataBuilder )
    {
      super( elementName, reader, builderFactory );
      this.threddsMetadataBuilder = threddsMetadataBuilder;
    }

    ThreddsBuilder getSelfBuilder() {
      return null;
    }

    void parseStartElement()
            throws ThreddsXmlParserException
    {
      StartElement startElement = this.getNextEventIfStartElementIsMine();

      Attribute typeAtt = startElement.getAttributeByName( ThreddsMetadataElementNames.DocumentationElement_Type );
      Attribute xlinkTitleAtt = startElement.getAttributeByName( ThreddsMetadataElementNames.DocumentationElement_XlinkTitle );
      Attribute xlinkExternalRefAtt = startElement.getAttributeByName( ThreddsMetadataElementNames.DocumentationElement_XlinkHref );

      String type = typeAtt != null ? typeAtt.getValue() : null;
      String xlinkTitle = xlinkTitleAtt != null ? xlinkTitleAtt.getValue() : null;
      String xlinkExternalRef = xlinkExternalRefAtt != null ? xlinkExternalRefAtt.getValue() : null;

      String content = StaxThreddsXmlParserUtils.getCharacterContent( this.reader, this.elementName );

      if ( xlinkTitle == null && xlinkExternalRef == null )
      {
        this.threddsMetadataBuilder.addDocumentation( type, content );
        return;
      }
      else
        this.threddsMetadataBuilder.addDocumentation( type, xlinkTitle, xlinkExternalRef );
    }

    void handleChildStartElement()
            throws ThreddsXmlParserException
    {
    }

    void postProcessingAfterEndElement()
            throws ThreddsXmlParserException
    {
    }

    static class Factory
    {
      private QName elementName;

      Factory() {
        this.elementName = ThreddsMetadataElementNames.DocumentationElement;
      }

      boolean isEventMyStartElement( XMLEvent event ) {
        return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
      }

      DocumentationElementParser getNewParser( XMLEventReader reader,
                                               ThreddsBuilderFactory builderFactory,
                                               ThreddsMetadataBuilder parentBuilder )
      {
        return new DocumentationElementParser( this.elementName, reader, builderFactory, parentBuilder );
      }
    }
  }

}