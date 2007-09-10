package thredds.wcs.v1_1_0;

import org.jdom.Namespace;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ExceptionReport
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( ExceptionReport.class );

  protected static final Namespace owsNS = Namespace.getNamespace( "http://www.opengis.net/ows" );

  public enum Code
  {
    OperationNotSupported,
    MissingParameterValue,
    InvalidParameterValue,
    VersionNegotiationFailed, // GetCapabilities only
    InvalidUpdateSequence,
    NoApplicableCode,
    UnsupportedCombination,   // GetCoverage only
    NotEnoughStorage          // GetCoverage only
  }

  private Document exceptionReport;
  public ExceptionReport( Code code, String locator, String message)
  {
    this( Collections.singletonList( new Exception(code, locator, Collections.singletonList( message))));
  }

  public ExceptionReport( List<Exception> exceptions )
  {
    Element rootElem = new Element( "ExceptionReport", owsNS );
    rootElem.addNamespaceDeclaration( owsNS );
    rootElem.setAttribute( "version", "1.0.0" );
    // rootElem.setAttribute( "language", "en" );

    if ( exceptions != null )
      for ( Exception curException : exceptions )
      {
        Element exceptionElem = new Element( "Exception", owsNS );
        exceptionElem.setAttribute( "code", curException.getCode().toString() );
        if ( curException.getLocator() != null && ! curException.getLocator().equals( "" ) )
          exceptionElem.setAttribute( "locator", curException.getLocator() );

        if ( curException.getMessages() != null )
        {
          for ( String curMessage : curException.getMessages() )
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

  public static class Exception
  {
    private Code code;
    private String locator;
    private List<String> messages;

    public Exception( Code code, String locator, List<String> messages )
    {
      this.code = code;
      this.locator = locator;
      this.messages = new ArrayList<String>(messages.size());
      Collections.copy( this.messages, messages);
    }

    public Code getCode() { return code; }
    public String getLocator() { return locator; }
    public List<String> getMessages() { return Collections.unmodifiableList( messages); }
  }
}
