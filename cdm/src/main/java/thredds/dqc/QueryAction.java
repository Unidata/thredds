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
