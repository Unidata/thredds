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
