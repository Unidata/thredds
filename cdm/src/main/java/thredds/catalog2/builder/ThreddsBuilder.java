package thredds.catalog2.builder;

import java.util.List;

/**
 * Parent type for all THREDDS builders.
 *
 * @author edavis
 * @since 4.0
 */
public interface ThreddsBuilder
{
  /**
   * Check whether the state of this ThreddsBuilder is such that build() will succeed.
   *
   * @param issues a list in which to add any issues that come up during isFinished().
   * @return true if this ThreddsBuilder is in a state where finish() will succeed.
   */
//  public boolean isReadyToBuild( List<BuilderFinishIssue> issues );

  /**
   *
   * @return
   * @throws BuildException if this ThreddsBuilder was not ready to build.
   */
//  public ThreddsBuilder build() throws BuildException;

}
