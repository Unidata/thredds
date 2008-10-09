package thredds.catalog2.builder;

import thredds.catalog2.Metadata;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface MetadataBuilder extends ThreddsBuilder
{
  public boolean isBuildable( List<BuilderFinishIssue> issues );
  public Metadata build() throws BuilderException;
}
