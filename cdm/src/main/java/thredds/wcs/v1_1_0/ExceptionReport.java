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
package thredds.wcs.v1_1_0;

import org.jdom.Namespace;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.util.List;
import java.util.Collections;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * Generates a WCS 1.1.0 Exception Report.
 *
 * @author edavis
 * @since 4.0
 */
public class ExceptionReport
{
  protected static final Namespace owsNS = Namespace.getNamespace( "http://www.opengis.net/ows" );

  private Document exceptionReport;
  public ExceptionReport( WcsException exception)
  {
    this( Collections.singletonList( exception));
  }

  public ExceptionReport( List<WcsException> exceptions )
  {
    Element rootElem = new Element( "ExceptionReport", owsNS );
    rootElem.addNamespaceDeclaration( owsNS );
    rootElem.setAttribute( "version", "1.0.0" );
    // rootElem.setAttribute( "language", "en" );

    if ( exceptions != null )
      for ( WcsException curException : exceptions )
      {
        Element exceptionElem = new Element( "Exception", owsNS );
        exceptionElem.setAttribute( "code", curException.getCode().toString() );
        if ( curException.getLocator() != null && ! curException.getLocator().equals( "" ) )
          exceptionElem.setAttribute( "locator", curException.getLocator() );

        if ( curException.getTextMessages() != null )
        {
          for ( String curMessage : curException.getTextMessages() )
          {
            Element excTextElem = new Element( "ExceptionText", owsNS );
            excTextElem.addContent( curMessage );

            exceptionElem.addContent( excTextElem );
          }
        }
        rootElem.addContent( exceptionElem );
      }

    exceptionReport = new Document( rootElem );
  }

  public Document getExceptionReport() { return exceptionReport; }
  public void writeExceptionReport( PrintWriter pw )
          throws IOException
  {
    XMLOutputter xmlOutputter = new XMLOutputter( Format.getPrettyFormat() );
    xmlOutputter.output( exceptionReport, pw);
  }
}
