package thredds.catalog2.simpleImpl;

import thredds.catalog2.Metadata;
import thredds.catalog2.builder.MetadataBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.BuilderIssue;

import java.net.URI;
import java.util.List;
import java.util.ArrayList;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class MetadataImpl implements Metadata, MetadataBuilder
{
  private boolean isContainedContent;

  private String title;
  private URI externalReference;
  private String content;

  private boolean isBuilt;

  public MetadataImpl()
  {
    this.isContainedContent = true;
    this.isBuilt = false;
  }

  public MetadataImpl( boolean isContainedContent )
  {
    this.isContainedContent = isContainedContent;
    this.isBuilt = false;
  }

  public MetadataImpl( String title, URI externalReference )
  {
    if ( title == null )
      throw new IllegalArgumentException( "Title may not be null." );
    if ( externalReference == null )
      throw new IllegalArgumentException( "External reference URI may not be null." );

    this.isContainedContent = false;
    this.title = title;
    this.externalReference = externalReference;
    this.content = null;

    this.isBuilt = false;
  }

  public MetadataImpl( String content )
  {
    if ( content == null )
      throw new IllegalArgumentException( "Content string may not be null.");

    this.isContainedContent = true;
    this.title = null;
    this.externalReference = null;
    this.content = content;

    this.isBuilt = false;
  }

  public void setContainedContent( boolean isContainedContent )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has already been built.");
    this.isContainedContent = isContainedContent;
  }

  public boolean isContainedContent()
  {
    return this.isContainedContent;
  }

  public void setTitle( String title)
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has already been built." );
    if ( this.isContainedContent )
      throw new IllegalStateException( "This MetadataBuilder contains content, cannot set title." );
    if ( title == null )
      throw new IllegalArgumentException( "Title may not be null." );

    this.title = title;
  }

  public String getTitle()
  {
    if ( this.isContainedContent )
      throw new IllegalStateException( "Metadata with contained content has no title.");
    return this.title;
  }

  public void setExternalReference( URI externalReference )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has already been built." );
    if ( this.isContainedContent )
      throw new IllegalStateException( "This MetadataBuilder contains content, cannot set external reference." );
    if ( externalReference == null )
      throw new IllegalArgumentException( "External reference may not be null.");

    this.externalReference = externalReference;
  }

  public URI getExternalReference()
  {
    if ( this.isContainedContent )
      throw new IllegalStateException( "Metadata with contained content has no external reference." );
    return this.externalReference;
  }

  public void setContent( String content )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has already been built." );
    if ( ! this.isContainedContent )
      throw new IllegalStateException( "This MetadataBuilder has external reference, cannot set content." );
    if ( externalReference == null )
      throw new IllegalArgumentException( "External reference may not be null." );

  }

  public String getContent()
  {
    if ( ! this.isContainedContent )
      throw new IllegalStateException( "Metadata with external reference has no content, dereference external reference to obtain metadata content." );
    return this.content;
  }

  public boolean isBuilt()
  {
    return this.isBuilt;
  }

  public boolean isBuildable( List<BuilderIssue> issues )
  {
    if ( this.isBuilt )
      return true;

    if ( this.isContainedContent )
    {
      if ( this.content == null )
      {
        issues.add( new BuilderIssue( "MetadataBuilder contains null content.", this ));
        return false;
      }
    }
    else
      if ( this.title == null || this.externalReference == null )
      {
        issues.add( new BuilderIssue( "MetadataBuilder with link has null title and/or link URI.", this ) );
        return false;
      }

    return true;
  }

  public Metadata build() throws BuilderException
  {
    if ( this.isBuilt )
      return this;

    List<BuilderIssue> issues = new ArrayList<BuilderIssue>();
    if ( ! this.isBuildable( issues ))
      throw new BuilderException( issues);

    // Clean up.
    if ( this.isContainedContent )
    {
      this.title = null;
      this.externalReference = null;
    }
    else
      this.content = null;

    this.isBuilt = true;
    return this;
  }
}
