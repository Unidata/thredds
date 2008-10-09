package thredds.catalog2.builder;

import java.util.List;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class BuildException extends Exception
{
  private final List<BuilderFinishIssue> issues;

  public BuildException( BuilderFinishIssue issue )
  {
    super();
    this.issues = Collections.singletonList( issue );
  }

  public BuildException( List<BuilderFinishIssue> issues )
  {
    super();
    this.issues = issues;
  }

  public BuildException( BuilderFinishIssue issue, Throwable cause )
  {
    super( cause );
    this.issues = Collections.singletonList( issue );
  }

  public BuildException( List<BuilderFinishIssue> issues, Throwable cause )
  {
    super( cause );
    this.issues = issues;
  }

  public List<BuilderFinishIssue> getSources()
  {
    return Collections.unmodifiableList( this.issues );
  }

  @Override
  public String getMessage()
  {
    StringBuilder sb = new StringBuilder();
    for ( BuilderFinishIssue bfi : this.issues )
      sb.append( bfi.getMessage() ).append( "\n");
    return sb.toString();
  }
}
