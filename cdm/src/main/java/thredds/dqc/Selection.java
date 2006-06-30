// $Id: Selection.java,v 1.3 2006/01/20 20:42:05 caron Exp $
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

/*
 * $Log: Selection.java,v $
 * Revision 1.3  2006/01/20 20:42:05  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.2  2005/04/05 22:37:04  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.1  2004/03/05 06:33:40  edavis
 * Classes to handle DQC and user query information.
 *
 */