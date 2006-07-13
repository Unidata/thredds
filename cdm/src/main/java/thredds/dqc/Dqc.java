// $Id: Dqc.java 63 2006-07-12 21:50:51Z edavis $
package thredds.dqc;

import org.jdom.*;
import org.jdom.input.SAXBuilder;

import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.IOException;

/**
 * A description
 *
 * User: edavis
 * Date: Jan 23, 2004
 * Time: 8:59:23 PM
 */
public class Dqc
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( Dqc.class );

  protected String name = null;
  protected Namespace ns = null;
  protected String version = null;

  protected QueryAction query;
  protected List selectors;

  private static String rootElementName = "queryCapability";
  private static String version_0_3 = "0.3";
  private static String rootElementNamespaceURI_0_3 = "http://www.unidata.ucar.edu/schemas/thredds/queryCapability.0.3.xsd";

  /** Constructor */
  protected Dqc() { selectors = new ArrayList(); }

  /** Factory method specific for JPL QuikSCAT catalog.  */
  public static Dqc createDqcForJplQuikSCAT()
          throws Exception
  {
    // @todo This shouldn't exist. Should instead use createDqc() to read DQC doc.

    // Create the new Dqc instance.
    Dqc dqc = new Dqc();
    dqc.name = "NASA JPL QuikSCAT Global Level-2B Data";
    dqc.version = "0.3";
    dqc.ns = Namespace.getNamespace( rootElementNamespaceURI_0_3);

    dqc.query = new QueryAction( "http://dods.jpl.nasa.gov/dqcServlet/quikscat", "template", "catalog");

    SelectFromRange sfr = new SelectFromRange();
    sfr.setTitle( "Select Date Range");
    sfr.setMultiple( false);
    sfr.setRequired( false);

    sfr.setAllowedRange( 0.0, 5.0); // @todo Set correct values.
    sfr.setUnits( "seconds since 1999-01-01");
    sfr.setTemplate( "min=(minDate)&max=(maxDate)" );
    sfr.setModulo( false);

    dqc.selectors.add( sfr);

    sfr = new SelectFromRange();

    sfr.setTitle( "Northerly Equatorial Crossing (longitude)");
    sfr.description = new Description( "The longitude at which the satellite crosses the equator from the\n" +
                                       "southern into the northern hemisphere." );
    sfr.setMultiple( true);
    sfr.setRequired( false);

    sfr.setAllowedRange( 0.0, 360.0);
    sfr.setUnits( "degree_east");
    sfr.setTemplate( "minCross=(minCross)&maxCross=(maxCross)" );
    sfr.setModulo( true);

    dqc.selectors.add( sfr);

    return( dqc);
  }

  /**
   * Factory method to read a DQC document and create a Dqc instance.
   *
   * @param xmlIs - java.io.InputStream for a DQC document.
   * @return a Dqc instance that represents the given DQC document.
   * @throws Exception
   */
  public static Dqc createInstance( InputStream xmlIs)
          throws Exception
  {
    // Open DQC document.
    Document doc;
    try
    {
      SAXBuilder builder = new SAXBuilder( true);
      doc = builder.build( xmlIs);
    }
    catch ( JDOMException e)
    {
      String tmp = "createInstance(): exception thrown while parsing XML document: " + e.getMessage();
      log.debug( tmp);
      throw new Exception( tmp);
    }

    // Create the new Dqc instance.
    Dqc dqc = new Dqc();

    // Check that a DQC document has been opened.
    Element root = doc.getRootElement();
    if ( ! root.getName().equals( rootElementName) )
    {
      String tmp = "createInstance(): name of root element <" + root.getName() +
              "> is not \"" + rootElementName + "\"";
      log.debug( tmp);
      throw new Exception( tmp);
    }

    // Handle the document according to the version.
    dqc.version = root.getAttributeValue( "version");
    dqc.ns = root.getNamespace();
    if ( dqc.version.equals( version_0_3) &&
            dqc.ns.getURI().equals( rootElementNamespaceURI_0_3))
    {
      return( createInstance_0_3( dqc, root));
    }
    else
    {
      String tmp = "createInstance(): version of DQC document <" +
              dqc.version + "><" + dqc.ns.getURI() + "> is not supported.";
      log.debug( tmp);
      throw new Exception( tmp);
    }

  }

  /**
   * Helper method: creates a Dqc instance for version 0.3 DQC documents.
   *
   * @param dqc
   * @param root
   * @return
   * @throws Exception
   */
  private static Dqc createInstance_0_3( Dqc dqc, Element root )
          throws Exception
  {
    Attribute tmpAtt = null;
    dqc.name = root.getAttributeValue( "name");

    // Add the query element.
    root.getChild( "query", dqc.ns);
    dqc.query = new QueryAction( root.getAttributeValue( "base"),
                           root.getAttributeValue( "construct"),
                           root.getAttributeValue( "returns"));

    // Add any selectFromRange elements.
    java.util.List list = root.getChildren( "selectFromRange", dqc.ns);
    for (int i=0; i< list.size(); i++)
    {
      Element selectFromRangeElement = (Element) list.get(i);
      SelectFromRange curSfr = new SelectFromRange();

      curSfr.setId( selectFromRangeElement.getAttributeValue( "id"));
      curSfr.setTitle( selectFromRangeElement.getAttributeValue( "title"));
      tmpAtt = selectFromRangeElement.getAttribute( "required");
      if ( tmpAtt != null)
      {
        curSfr.setRequired( tmpAtt.getBooleanValue());
      }
      tmpAtt = selectFromRangeElement.getAttribute( "multiple");
      if ( tmpAtt != null)
      {
        curSfr.setMultiple( tmpAtt.getBooleanValue());
      }

      curSfr.setAllowedRange( selectFromRangeElement.getAttribute( "min").getDoubleValue(),
                              selectFromRangeElement.getAttribute( "max").getDoubleValue() );
      curSfr.setUnits( selectFromRangeElement.getAttributeValue( "units"));
      tmpAtt = selectFromRangeElement.getAttribute( "modulo");
      if ( tmpAtt != null)
      {
        curSfr.setModulo( tmpAtt.getBooleanValue());
      }
      curSfr.setTemplate( selectFromRangeElement.getAttributeValue( "template"));
      dqc.selectors.add( curSfr);
    }

    // @todo Finish this so it actually works.

    return( dqc);
  }

//  public Selection buildQueryResponse( String urlQueryString)
//  {
//
//  }
}
