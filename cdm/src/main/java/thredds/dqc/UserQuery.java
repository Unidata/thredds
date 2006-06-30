// $Id: UserQuery.java,v 1.3 2006/01/20 20:42:05 caron Exp $
package thredds.dqc;

/**
 * A user query for a given DQC.
 *
 * User: edavis
 * Date: Feb 11, 2004
 * Time: 4:30:30 PM
 */
public interface UserQuery
{
  /**
   * Return true if the UserQuery is set, false otherwise.
   *
   * @return true if the UserQuery is set, false otherwise.
   */
  public boolean isSet();
}

/*
 * $Log: UserQuery.java,v $
 * Revision 1.3  2006/01/20 20:42:05  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.2  2005/04/05 22:37:04  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.1  2004/03/05 06:33:41  edavis
 * Classes to handle DQC and user query information.
 *
 */