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
package thredds.catalog2.xml.writer.stax;

import thredds.catalog2.xml.writer.ThreddsXmlWriterException;
import thredds.catalog2.xml.names.CatalogNamespace;
import thredds.catalog2.xml.names.ServiceElementNames;
import thredds.catalog2.Service;
import thredds.catalog2.Property;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ServiceElementWriter implements AbstractElementWriter
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  public ServiceElementWriter() {}

  public void writeElement( Service service, XMLStreamWriter writer, int nestLevel )
          throws ThreddsXmlWriterException
  {
    String indentString = StaxWriter.getIndentString( nestLevel );
    try
    {
      if ( nestLevel == 0 )
      {
        writer.writeStartDocument();
        writer.writeCharacters( "\n" );
      }
      else
        writer.writeCharacters( indentString );

      boolean isEmptyElement = service.getProperties().isEmpty() && service.getServices().isEmpty();
      if ( isEmptyElement )
        writer.writeEmptyElement( ServiceElementNames.ServiceElement.getLocalPart() );
      else
        writer.writeStartElement( ServiceElementNames.ServiceElement.getLocalPart() );
      if ( nestLevel == 0 )
      {
        writer.writeNamespace( CatalogNamespace.CATALOG_1_0.getStandardPrefix(),
                               CatalogNamespace.CATALOG_1_0.getNamespaceUri() );
        writer.writeNamespace( CatalogNamespace.XLINK.getStandardPrefix(),
                               CatalogNamespace.XLINK.getNamespaceUri() );
      }
      writer.writeAttribute( ServiceElementNames.ServiceElement_Name.getLocalPart(),
                             service.getName() );
      writer.writeAttribute( ServiceElementNames.ServiceElement_ServiceType.getLocalPart(),
                             service.getType().toString() );
      writer.writeAttribute( ServiceElementNames.ServiceElement_Base.getLocalPart(),
                             service.getBaseUri().toString() );

      if ( service.getSuffix() != null && ! service.getSuffix().equals( ""))
      {
        writer.writeAttribute( ServiceElementNames.ServiceElement_Suffix.getLocalPart(),
                               service.getSuffix() );
      }
      if ( service.getDescription() != null && ! service.getDescription().equals( "" ) )
      {
        writer.writeAttribute( ServiceElementNames.ServiceElement_Description.getLocalPart(),
                               service.getDescription() );
      }

      writer.writeCharacters( "\n" );
      for ( Property curProperty : service.getProperties() )
        new PropertyElementWriter().writeElement( curProperty, writer, nestLevel + 1 );
      for ( Service curService : service.getServices() )
        new ServiceElementWriter().writeElement( curService, writer, nestLevel + 1 );

      if ( ! isEmptyElement )
      {
        writer.writeCharacters( indentString );
        writer.writeEndElement();
        writer.writeCharacters( "\n" );
      }
      if ( nestLevel == 0 )
        writer.writeEndDocument();
      writer.flush();
      if ( nestLevel == 0 )
        writer.close();
    }
    catch ( XMLStreamException e )
    {
      log.error( "writeElement(): Failed while writing to XMLStreamWriter: " + e.getMessage() );
      throw new ThreddsXmlWriterException( "Failed while writing to XMLStreamWriter: " + e.getMessage(), e );
    }
  }

}
