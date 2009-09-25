package thredds.catalog2.builder;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class BuilderIssues
{
  private final List<BuilderIssue> issues;
  private int numFatalIssues = 0;
  private int numErrorIssues = 0;
  private int numWarningIssues = 0;


  public BuilderIssues()
  {
    this.issues = new ArrayList<BuilderIssue>();
  }

  public BuilderIssues( BuilderIssue issue )
  {
    this();
    if ( issue == null )
    {
      throw new IllegalArgumentException( "Issue may not be null." );
    }
    this.issues.add( issue );
  }

  public BuilderIssues( BuilderIssue.Severity severity, String message, ThreddsBuilder builder, Exception cause )
  {
    this();
    this.issues.add( new BuilderIssue( severity, message, builder, cause ) );
    trackSeverity( severity );
  }

  public void addIssue( BuilderIssue.Severity severity, String message, ThreddsBuilder builder, Exception cause )
  {
    this.issues.add( new BuilderIssue( severity, message, builder, cause ) );
    trackSeverity( severity );
  }

  public void addIssue( BuilderIssue issue )
  {
    if ( issue == null )
    {
      return;
    }
    this.issues.add( issue );
    trackSeverity( issue.getSeverity() );
  }

  public void addAllIssues( BuilderIssues issues )
  {
    if ( issues == null )
    {
      return;
    }
    if ( issues.isEmpty() )
    {
      return;
    }
    this.issues.addAll( issues.getIssues() );
    for ( BuilderIssue curIssue : issues.getIssues() )
    {
      trackSeverity( curIssue.getSeverity() );
    }
  }

  public boolean isEmpty()
  {
    return this.issues.isEmpty();
  }

  public int size()
  {
    return this.issues.size();
  }

  public List<BuilderIssue> getIssues()
  {
    if ( issues.isEmpty() )
    {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList( this.issues );
  }

  public boolean isValid() {
    if ( this.numFatalIssues > 0 || this.numErrorIssues > 0)
      return false;
    return true;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    for ( BuilderIssue bfi : this.issues )
    {
      sb.append( bfi.getMessage() ).append( "\n" );
    }
    return sb.toString();
  }

  private void trackSeverity( BuilderIssue.Severity severity )
  {
    if ( severity.equals( BuilderIssue.Severity.FATAL ) )
      this.numFatalIssues++;
    else if ( severity.equals( BuilderIssue.Severity.ERROR ) )
      this.numErrorIssues++;
    else if ( severity.equals( BuilderIssue.Severity.WARNING ) )
      this.numWarningIssues++;
  }
}
