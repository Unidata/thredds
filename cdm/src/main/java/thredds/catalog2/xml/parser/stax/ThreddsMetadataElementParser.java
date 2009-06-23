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
import javax.xml.namespace.QName;
import java.util.Map;
import java.util.HashMap;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsMetadataElementParser extends AbstractElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final static Map<QName, AbstractElementParser> selfElements = new HashMap<QName, AbstractElementParser>();
  private final DatasetNodeBuilder datasetNodeBuilder;

  private final DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper;

  private final boolean inheritedByDescendants;

  private ThreddsMetadataBuilder resultThreddsMetadataBuilder;

  public ThreddsMetadataElementParser( XMLEventReader reader,
                                       DatasetNodeBuilder datasetNodeBuilder,
                                       DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper,
                                       boolean inheritedByDescendants )
          throws ThreddsXmlParserException
  {
    super( reader, ThreddsMetadataElementNames.ThreddsMetadataElement );
    this.datasetNodeBuilder = datasetNodeBuilder;
    this.parentDatasetNodeElementParserHelper = parentDatasetNodeElementParserHelper;
    this.inheritedByDescendants = inheritedByDescendants;
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

  protected ThreddsMetadataBuilder parseStartElement()
          throws ThreddsXmlParserException
  {
    // ThreddsMetadata container object only, no self element exists!
    // So peek at next event to see how to route it.
    StartElement startElement = this.peekAtNextEventIfStartElement();

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
      ServiceNameElementParser parser = new ServiceNameElementParser( this.reader, this.resultThreddsMetadataBuilder, this.parentDatasetNodeElementParserHelper, this.inheritedByDescendants );
      parser.parse();
    }
    else if ( DataFormatElementParser.isSelfElementStatic(  startElement ))
    {
      DataFormatElementParser parser = new DataFormatElementParser( this.reader, this.resultThreddsMetadataBuilder );
      parser.parse();
    }
    else
      throw new ThreddsXmlParserException( "");

    return this.resultThreddsMetadataBuilder;
  }

  protected void handleChildStartElement( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( ServiceNameElementParser.isSelfElementStatic( startElement ) )
      return;
    else
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
  }

  protected void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    return;
  }

  public static class ServiceNameElementParser extends AbstractElementParser
  {
    private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

    private final ThreddsMetadataBuilder threddsMetadataBuilder;
    private DatasetNodeBuilder datasetNodeBuilder;

    private final DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper;

    private final boolean inheritedByDescendants;

    public ServiceNameElementParser( XMLEventReader reader,
                                     ThreddsMetadataBuilder threddsMetadataBuilder,
                                     DatasetNodeElementParserHelper parentDatasetNodeElementParserHelper,
                                     boolean inheritedByDescendants )
            throws ThreddsXmlParserException
    {
      super( reader, ThreddsMetadataElementNames.ServiceNameElement );
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

    private String serviceName;
    protected ThreddsMetadataBuilder parseStartElement()
            throws ThreddsXmlParserException
    {
      this.getNextEventIfStartElementIsMine();
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      this.serviceName = StaxThreddsXmlParserUtils.getCharacterContent( this.reader, ThreddsMetadataElementNames.ServiceNameElement );

      // Set default service name on parent dataset.
      this.parentDatasetNodeElementParserHelper.setDefaultServiceName( this.serviceName );

      return null;
    }

    protected void handleChildStartElement( ThreddsBuilder builder ) throws ThreddsXmlParserException
    {
      // Should not have child elements.
    }

    protected void postProcessing( ThreddsBuilder builder ) throws ThreddsXmlParserException
    {
      if ( this.inheritedByDescendants )
        this.parentDatasetNodeElementParserHelper.setDefaultServiceNameThatGetsInherited( this.serviceName );
      else
        this.parentDatasetNodeElementParserHelper.setDefaultServiceName( this.serviceName );
    }
  }

  /**
   * Parser for THREDDS metadata DataFormat elements.
   */
  public static class DataFormatElementParser extends AbstractElementParser
  {
    private final ThreddsMetadataBuilder threddsMetadataBuilder;

    public DataFormatElementParser( XMLEventReader reader,
                                    ThreddsMetadataBuilder threddsMetadataBuilder )
            throws ThreddsXmlParserException
    {
      super( reader, ThreddsMetadataElementNames.DataFormatElement );
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

    protected ThreddsMetadataBuilder parseStartElement()
            throws ThreddsXmlParserException
    {
      StartElement startElement = this.getNextEventIfStartElementIsMine();

      String dataFormat = StaxThreddsXmlParserUtils.getCharacterContent( this.reader, ThreddsMetadataElementNames.DataFormatElement );
      this.threddsMetadataBuilder.setDataFormat( dataFormat );

      return null;
    }

    protected void handleChildStartElement( ThreddsBuilder builder ) throws ThreddsXmlParserException
    {
    }

    protected void postProcessing( ThreddsBuilder builder ) throws ThreddsXmlParserException
    {
    }
  }
}