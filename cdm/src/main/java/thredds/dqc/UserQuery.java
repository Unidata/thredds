// $Id$
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
