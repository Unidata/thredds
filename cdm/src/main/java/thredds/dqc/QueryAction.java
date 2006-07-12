// $Id$
package thredds.dqc;

/**
 * A description
 *
 * User: edavis
 * Date: Jan 23, 2004
 * Time: 9:09:03 PM
 */
public class QueryAction
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( QueryAction.class );

  protected String base = null;
  // @todo construct should be a type safe enumeration: "paramValue" or "template"
  protected String construct = null;
  // @todo returns should be a type safe enumeration: "catalog" or "other"
  protected String returns = "catalog";

  private QueryAction() {}
  protected QueryAction( String base, String construct)
  {
    this.base = base;
    this.construct = construct;
  }
  protected QueryAction( String base, String construct, String returns)
  {
    this.base = base;
    this.construct = construct;
    this.returns = returns;
  }
}

/*
 * $Log: QueryAction.java,v $
 * Revision 1.3  2006/01/20 20:42:04  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.2  2005/04/05 22:37:03  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.1  2004/03/05 06:33:40  edavis
 * Classes to handle DQC and user query information.
 *
 */