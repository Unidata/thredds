package thredds.catalog2.simpleImpl;

import thredds.catalog2.Metadata;
import thredds.catalog2.builder.MetadataBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.BuilderFinishIssue;

import java.net.URI;
import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class MetadataImpl implements Metadata, MetadataBuilder
{
  private final boolean isContainedContent;

  public final String title;
  public final URI externalReference;
  public final String content;

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
  }

  public MetadataImpl( String content )
  {
    if ( content == null )
      throw new IllegalArgumentException( "Content string may not be null.");

    this.isContainedContent = true;
    this.title = null;
    this.externalReference = null;
    this.content = content;
  }

  public boolean isContainedContent()
  {
    return this.isContainedContent;
  }

  public String getTitle()
  {
    if ( this.isContainedContent )
      throw new IllegalStateException( "Metadata with contained content has no title.");
    return this.title;
  }

  public URI getExternalReference()
  {
    if ( this.isContainedContent )
      throw new IllegalStateException( "Metadata with contained content has no external reference." );
    return this.externalReference;
  }

  public String getContent()
  {
    if ( ! this.isContainedContent )
      throw new IllegalStateException( "Use external reference to obtain metadata content." );
    return this.content;
  }

  public boolean isBuilt()
  {
    return true;
  }

  public boolean isBuildable( List<BuilderFinishIssue> issues )
  {
    return true;
  }

  public Metadata build() throws BuilderException
  {
    return this;
  }
}
