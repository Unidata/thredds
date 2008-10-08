package thredds.catalog2.builder;

import thredds.catalog2.Dataset;
import thredds.catalog.ServiceType;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface DatasetBuilder extends DatasetNodeBuilder
{
  public AccessBuilder addAccessBuilder();

  public boolean isAccessible();
  public List<AccessBuilder> getAccessBuilders();
  public List<AccessBuilder> getAccessBuildersByType( ServiceType type );

  public boolean isFinished();
  public Dataset finish( List<BuilderFinishIssue> issues );
}
