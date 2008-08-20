package thredds.wcs.v1_1_0;

import org.jdom.Namespace;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.dt.GridDataset;

import java.util.*;

/**
 * Generates WCS 1.1.0 XML responses.
 *
 * @author edavis
 * @since 4.0
 */
public class XMLwriter
{
  protected static final Namespace wcsNS = Namespace.getNamespace( "http://www.opengis.net/wcs/1.1" );
  protected static final Namespace owcsNS = Namespace.getNamespace( "http://www.opengis.net/wcs/1.1/ows" );
  protected static final Namespace owsNS = Namespace.getNamespace( "http://www.opengis.net/ows");

  private XMLOutputter xmlOutputter;

  public XMLwriter()
  {
    xmlOutputter = new XMLOutputter( Format.getPrettyFormat() );
  }

  public static String seqDate;
  static {
    // will get incremented each time we start up
    DateFormatter formatter = new DateFormatter();
    seqDate = formatter.toDateTimeStringISO(new Date());
  }
  //////////////////////////////////////////////////////////////////////////////////
  public enum ExceptionCodes
  {
    MissingParameterValue, InvalidParameterValue, VersionNegotiationFailed,
    InvalidUpdateSequence, NoApplicableCode
  }

  public Document generateExceptionReport( ExceptionCodes code, String locator, String message)
  {
    Element rootElem = new Element( "ExceptionReport", owsNS);

    rootElem.addNamespaceDeclaration( owsNS );
    rootElem.setAttribute( "version", "1.0.0" );
    // rootElem.setAttribute( "language", "en" );

    Element exceptionElem = new Element( "Exception", owsNS);
    exceptionElem.setAttribute( "code", code.toString());
    if ( locator != null && ! locator.equals( ""))
      exceptionElem.setAttribute( "locator", locator);

    if ( message != null && ! message.equals( ""))
    {
      Element excTextElem = new Element( "ExceptionText", owsNS);
      excTextElem.addContent( message);

      exceptionElem.addContent( excTextElem);
    }
    rootElem.addContent( exceptionElem);

    return new Document( rootElem );
  }

  public Document generateCapabilities( String serverURL, GridDataset dataset, List<GetCapabilities.Section> sections )
  {
    Element capabilitiesElem = new Element( "Capabilities", owsNS );
    capabilitiesElem.addNamespaceDeclaration( owsNS );


    return new Document( capabilitiesElem );
  }

//  public static void main( String[] args )
//  {
//    List<String> keywds = new ArrayList<String>();
//    keywds.add( "keywd1");
//    keywds.add( "keywd2");
//    List<String> servTypVers = new ArrayList<String>();
//    servTypVers.add( "1.1.0" );
//    servTypVers.add( "1.0.0" );
//
//    List<String> accessConsts = new ArrayList<String>();
//    accessConsts.add("NONE");
//
//    XMLwriter xmlWriter = new XMLwriter();
//
//    Element si = xmlWriter.generateServiceIdentification( "title", "abs", keywds,"WCS", servTypVers, "NONE", accessConsts);
//
//    XMLOutputter xmlOutputter = new XMLOutputter( Format.getPrettyFormat() );
//
//    System.out.println( xmlOutputter.outputString( si) );
//  }
}
