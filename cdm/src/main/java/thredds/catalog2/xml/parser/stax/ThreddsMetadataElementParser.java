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
import thredds.catalog2.xml.names.ThreddsMetadataElementNames;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsMetadataElementParser extends AbstractElementParser
{
  private final DatasetNodeBuilder parentDatasetNodeBuilder;

  private final DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper;

  private final boolean inheritedByDescendants;

  private final ThreddsMetadataBuilder selfBuilder;

  public ThreddsMetadataElementParser( XMLEventReader reader,
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

  protected static boolean isSelfElementStatic( XMLEvent event )
  {
    if ( ServiceNameElementParser.isSelfElementStatic( event ))
      return true;
    if ( DataFormatElementParser.isSelfElementStatic( event ))
      return true;
    return false;
  }

  protected boolean isSelfElement( XMLEvent event )
  {
    return isSelfElementStatic( event );
  }

  protected ThreddsMetadataBuilder getSelfBuilder() {
    return this.selfBuilder;
  }

  protected void parseStartElement()
          throws ThreddsXmlParserException
  {
    // ThreddsMetadata container object only, no self element exists!
    // So peek at next event to see how to route it.
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( ! this.isSelfElement( startElement ) )
      throw new IllegalArgumentException( "Start element [" + startElement.getName().getLocalPart()
                                          + "] must be one of the THREDDS metadata element." );

    if ( ServiceNameElementParser.isSelfElementStatic( startElement ) )
    {
      ServiceNameElementParser parser
              = new ServiceNameElementParser( this.reader,
                                              this.builderFactory,
                                              this.selfBuilder,
                                              this.parentDatasetNodeElementParserHelper,
                                              this.inheritedByDescendants );
      parser.parse();
    }
    else if ( DataFormatElementParser.isSelfElementStatic(  startElement ))
    {
      DataFormatElementParser parser = new DataFormatElementParser( this.reader, this.builderFactory, this.selfBuilder );
      parser.parse();
    }
    else
      throw new ThreddsXmlParserException( "");
  }

  protected void handleChildStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( ServiceNameElementParser.isSelfElementStatic( startElement ) )
      return;
    else
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
  }

  protected void postProcessingAfterEndElement()
          throws ThreddsXmlParserException
  {
    return;
  }

  public static class ServiceNameElementParser extends AbstractElementParser
  {
    private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

    private final ThreddsMetadataBuilder threddsMetadataBuilder;
    //private DatasetNodeBuilder datasetNodeBuilder;

    private final DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper;

    private final boolean inheritedByDescendants;

    private String serviceName;


    public ServiceNameElementParser( XMLEventReader reader,
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

    protected static boolean isSelfElementStatic( XMLEvent event )
    {
      return isSelfElement( event, ThreddsMetadataElementNames.ServiceNameElement );
    }

    protected boolean isSelfElement( XMLEvent event )
    {
      return isSelfElementStatic( event );
    }

    protected ThreddsBuilder getSelfBuilder() {
      return null;
    }

    protected void parseStartElement()
            throws ThreddsXmlParserException
    {
      this.getNextEventIfStartElementIsMine();
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      this.serviceName = StaxThreddsXmlParserUtils.getCharacterContent( this.reader, ThreddsMetadataElementNames.ServiceNameElement );
    }

    protected void handleChildStartElement()
            throws ThreddsXmlParserException {
      // Should not have child elements.
    }

      protected void postProcessingAfterEndElement()
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
  public static class DataFormatElementParser extends AbstractElementParser
  {
    private final ThreddsMetadataBuilder threddsMetadataBuilder;

    public DataFormatElementParser( XMLEventReader reader,
                                    ThreddsBuilderFactory builderFactory,
                                    ThreddsMetadataBuilder threddsMetadataBuilder )
            throws ThreddsXmlParserException
    {
      super( reader, ThreddsMetadataElementNames.DataFormatElement, builderFactory );
      this.threddsMetadataBuilder = threddsMetadataBuilder;
    }

    protected static boolean isSelfElementStatic( XMLEvent event )
    {
      return isSelfElement( event, ThreddsMetadataElementNames.DataFormatElement );
    }

    protected boolean isSelfElement( XMLEvent event )
    {
      return isSelfElementStatic( event );
    }

    protected ThreddsBuilder getSelfBuilder() {
      return null;
    }

    protected void parseStartElement()
            throws ThreddsXmlParserException
    {
      StartElement startElement = this.getNextEventIfStartElementIsMine();

      String dataFormat = StaxThreddsXmlParserUtils.getCharacterContent( this.reader, ThreddsMetadataElementNames.DataFormatElement );
      this.threddsMetadataBuilder.setDataFormat( dataFormat );
    }

    protected void handleChildStartElement()
            throws ThreddsXmlParserException
    { }

    protected void postProcessingAfterEndElement()
            throws ThreddsXmlParserException
    { }
  }
}