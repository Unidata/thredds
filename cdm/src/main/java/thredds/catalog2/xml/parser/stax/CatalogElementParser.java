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

import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.parser.ThreddsXmlParserIssue;
import thredds.catalog2.xml.names.CatalogElementNames;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import java.util.Date;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

import ucar.nc2.units.DateType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class CatalogElementParser extends AbstractElementParser
{
  private final String docBaseUriString;

  CatalogBuilder selfBuilder;

  CatalogElementParser( String docBaseUriString,
                               XMLEventReader reader,
                               ThreddsBuilderFactory builderFactory )
          throws ThreddsXmlParserException
  {
    super( reader, CatalogElementNames.CatalogElement, builderFactory );
    this.docBaseUriString = docBaseUriString;
  }

  static boolean isSelfElementStatic( XMLEvent event )
  {
    return isSelfElement( event, CatalogElementNames.CatalogElement );
  }

  boolean isSelfElement( XMLEvent event )
  {
    return isSelfElement( event, CatalogElementNames.CatalogElement );
  }

  CatalogBuilder getSelfBuilder() {
    return this.selfBuilder;
  }

  void parseStartElement( )
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.getNextEventIfStartElementIsMine();

    Attribute nameAtt = startElement.getAttributeByName( CatalogElementNames.CatalogElement_Name );
    String nameString = nameAtt != null ? nameAtt.getValue() : null ;

    Attribute versionAtt = startElement.getAttributeByName( CatalogElementNames.CatalogElement_Version );
    String versionString = versionAtt != null ? versionAtt.getValue() : null;
    Attribute expiresAtt = startElement.getAttributeByName( CatalogElementNames.CatalogElement_Expires );
    String expiresString = expiresAtt != null ? expiresAtt.getValue() : null;
    DateType expires = null;
    try
    {
      expires = expiresString != null ? new DateType( expiresString, null, null ) : null;
    }
    catch ( ParseException e )
    {
      String msg = "Failed to parse catalog expires date [" + expiresString + "].";
      ThreddsXmlParserIssue issue = StaxThreddsXmlParserUtils.createIssueForException( msg, this.reader, e );
      log.warn( "parseStartElement(): " + issue.getMessage(), e );
      // ToDo Gather issues rather than throw exception.
      throw new ThreddsXmlParserException( issue );
    }
    Attribute lastModifiedAtt = startElement.getAttributeByName( CatalogElementNames.CatalogElement_LastModified );
    String lastModifiedString = lastModifiedAtt != null ? lastModifiedAtt.getValue() : null;
    DateType lastModified = null;
    try
    {
      lastModified = lastModifiedString != null ? new DateType( lastModifiedString, null, null ) : null;
    }
    catch ( ParseException e )
    {
      String msg = "Failed to parse catalog lastModified date [" + lastModifiedString + "].";
      ThreddsXmlParserIssue issue = StaxThreddsXmlParserUtils.createIssueForException( msg, this.reader, e );
      log.warn( "parseStartElement(): " + issue.getMessage(), e );
      // ToDo Gather issues rather than throw exception.
      throw new ThreddsXmlParserException( issue );
    }
    URI docBaseUri = null;
    try
    {
      docBaseUri = new URI( docBaseUriString );
    }
    catch ( URISyntaxException e )
    {
      String msg = "Bad catalog base URI [" + docBaseUriString + "].";
      ThreddsXmlParserIssue issue = StaxThreddsXmlParserUtils.createIssueForException( msg, this.reader, e );
      log.warn( "parseStartElement(): " + issue.getMessage(), e );
      // ToDo Gather issues rather than throw exception.
      throw new ThreddsXmlParserException( issue );
    }
    this.selfBuilder = builderFactory.newCatalogBuilder( nameString, docBaseUri, versionString, expires, lastModified );
  }

  void handleChildStartElement()
          throws ThreddsXmlParserException
  {
    StartElement startElement = this.peekAtNextEventIfStartElement();

    if ( ServiceElementParser.isSelfElementStatic( startElement ) )
    {
      ServiceElementParser serviceElemParser = new ServiceElementParser( this.reader,
                                                                         this.builderFactory,
                                                                         this.selfBuilder );
      serviceElemParser.parse();
    }
    else if ( PropertyElementParser.isSelfElementStatic( startElement ) )
    {
      PropertyElementParser parser = new PropertyElementParser( this.reader,
                                                                this.builderFactory,
                                                                this.selfBuilder );
      parser.parse();
    }
    else if ( DatasetElementParser.isSelfElementStatic( startElement ) )
    { // ToDo Not sure about the null parameter?
      DatasetElementParser parser = new DatasetElementParser( this.reader, this.builderFactory,  this.selfBuilder, null );
      parser.parse();
    }
    else
    {
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( this.reader );
    }
  }

  void postProcessingAfterEndElement()
          throws ThreddsXmlParserException
  {
//    if ( !( builder instanceof CatalogBuilder ) )
//      throw new IllegalArgumentException( "Given ThreddsBuilder must be an instance of DatasetBuilder." );
//    CatalogBuilder catalogBuilder = (CatalogBuilder) builder;
  }
}