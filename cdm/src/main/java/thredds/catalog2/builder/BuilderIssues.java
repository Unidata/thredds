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

    public BuilderIssues()
    {
        this.issues = new ArrayList<BuilderIssue>();
    }

    public BuilderIssues( BuilderIssue issue)
    {
        this();
        if ( issue == null )
            throw new IllegalArgumentException( "Issue may not be null.");
        this.issues.add( issue);
    }

    public BuilderIssues( String message, ThreddsBuilder builder)
    {
        this();
        this.issues.add( new BuilderIssue( message, builder));
    }

    public void addIssue( String message, ThreddsBuilder builder )
    {
        this.issues.add( new BuilderIssue( message, builder));
    }

    public void addIssue( BuilderIssue issue )
    {
        if ( issue == null )
            return;
        this.issues.add( issue);
    }

    public void addAllIssues( BuilderIssues issues )
    {
        if ( issues == null ) throw new IllegalArgumentException( "Issues may not be null.");
        if ( issues.isEmpty() )
            return;
        this.issues.addAll( issues.getIssues() );
    }

    public boolean isEmpty()
    {
        return this.issues.isEmpty();
    }

    public List<BuilderIssue> getIssues()
    {
        if ( issues.isEmpty() )
            return Collections.emptyList();
        return Collections.unmodifiableList( this.issues );
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for ( BuilderIssue bfi : this.issues )
            sb.append( bfi.getMessage() ).append( "\n" );
        return sb.toString();

    }
}
