package thredds.wcs.v1_0_0_Plus;

import org.jdom.Namespace;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.util.Collections;
import java.util.List;
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
  protected static final Namespace ogcNS = Namespace.getNamespace( "http://www.opengis.net/ogc" );

  private Document exceptionReport;
  public ExceptionReport( WcsException exception)
  {
    this( Collections.singletonList( exception));
  }

  public ExceptionReport( List<WcsException> exceptions )
  {
    Element rootElem = new Element( "ServiceExceptionReport", ogcNS );
    rootElem.addNamespaceDeclaration( ogcNS );
    rootElem.setAttribute( "version", "1.2.0" );

    if ( exceptions != null )
      for ( WcsException curException : exceptions )
      {
        Element exceptionElem = new Element( "ServiceException", ogcNS );
        if ( curException.getCode() != null && ! curException.getCode().equals( WcsException.Code.UNKNOWN) )
          exceptionElem.setAttribute( "code", curException.getCode().toString() );
        if ( curException.getLocator() != null && ! curException.getLocator().equals( "" ) )
          exceptionElem.setAttribute( "locator", curException.getLocator() );

        if ( curException.getTextMessages() != null )
        {
          for ( String curMessage : curException.getTextMessages() )
          {
            // ToDo - somehow seperate multiple text messages.
            exceptionElem.addContent( curMessage ); 
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
