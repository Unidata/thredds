package thredds.catalog2.builder;

import java.util.List;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class BuilderException extends Exception
{
  private final List<BuilderIssue> issues;

  public BuilderException( BuilderIssue issue )
  {
    super();
    this.issues = Collections.singletonList( issue );
  }

  public BuilderException( List<BuilderIssue> issues )
  {
    super();
    this.issues = issues;
  }

  public BuilderException( BuilderIssue issue, Throwable cause )
  {
    super( cause );
    this.issues = Collections.singletonList( issue );
  }

  public BuilderException( List<BuilderIssue> issues, Throwable cause )
  {
    super( cause );
    this.issues = issues;
  }

  public List<BuilderIssue> getSources()
  {
    return Collections.unmodifiableList( this.issues );
  }

  @Override
  public String getMessage()
  {
    StringBuilder sb = new StringBuilder();
    for ( BuilderIssue bfi : this.issues )
      sb.append( bfi.getMessage() ).append( "\n");
    return sb.toString();
  }
}
