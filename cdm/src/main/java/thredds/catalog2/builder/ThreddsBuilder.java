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
   * @param issues a list into which any issues that come up during isBuildable() will be add.
   * @return true if this ThreddsBuilder is in a state where build() will succeed.
   */
  public boolean isBuildable( List<BuilderFinishIssue> issues );

  /**
   * Generate the object being built by this ThreddsBuilder.
   *
   * @return the THREDDS catalog object being built by this ThreddsBuilder.
   * @throws BuilderException if this ThreddsBuilder is not in a valid state.
   */
  public Object build() throws BuilderException;

}
