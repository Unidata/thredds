// $Id$
package thredds.dqc;

/**
 * Represents a single selection in a DQC query. The validity of a selection
 * is determined by the corresponding DQC selector.
 *
 * User: edavis
 * Date: Jan 22, 2004
 * Time: 11:15:26 PM
 */
public abstract class Selection
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( Selection.class );

  protected Selector selector = null;

  protected Selection() {}

  public Selection( Selector selector) { this.selector = selector ; }
}
