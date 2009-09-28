/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.catalog2.simpleImpl;

import thredds.catalog2.Metadata;
import thredds.catalog2.builder.MetadataBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.BuilderIssue;
import thredds.catalog2.builder.BuilderIssues;

import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class MetadataImpl implements Metadata, MetadataBuilder
{
  private boolean isContainedContent;

  private String title;
  private URI externalReference;
  private String content;

  private boolean isBuilt;

  MetadataImpl()
  {
    this.isContainedContent = true;
    this.isBuilt = false;
  }

  MetadataImpl( boolean isContainedContent )
  {
    this.isContainedContent = isContainedContent;
    this.isBuilt = false;
  }

  MetadataImpl( String title, URI externalReference )
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

  MetadataImpl( String content )
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

    this.content = content;
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

  public BuilderIssues getIssues()
  {
    if ( this.isContainedContent )
    {
      if ( this.content == null )
        return new BuilderIssues( BuilderIssue.Severity.WARNING, "MetadataBuilder contains null content.", this, null );
    }
    else
      if ( this.title == null || this.externalReference == null )
        return new BuilderIssues( BuilderIssue.Severity.WARNING, "MetadataBuilder with link has null title and/or link URI.", this, null );

    return new BuilderIssues();
  }

  public Metadata build() throws BuilderException
  {
    this.isBuilt = true;
    return this;
  }
}
