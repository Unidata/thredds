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

import junit.framework.*;

import java.net.URI;
import java.net.URISyntaxException;

import thredds.catalog2.builder.BuilderIssue;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.BuilderIssues;
import thredds.catalog2.Metadata;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestMetadataImpl extends TestCase
{
  private MetadataImpl mdImpl1;
  private Metadata md1;
  private MetadataImpl mdImpl2;
  private Metadata md2;

  private URI uri;
  private String title;
  private String content;

  public TestMetadataImpl( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    try
    { uri = new URI( "http://server/thredds/md.xml" ); }
    catch ( URISyntaxException e )
    { fail("Bad URI syntax: " + e.getMessage()); return; }
    this.title = "metadata title";
    this.content = "<x>some content</x>";

    mdImpl1 = new MetadataImpl( this.title, this.uri );
    mdImpl2 = new MetadataImpl( this.content );
  }

  public void testCtorGet()
  {
    assertFalse( mdImpl1.isBuilt() );

    assertFalse( mdImpl1.isContainedContent());
    assertTrue( mdImpl1.getTitle().equals(  this.title ));
    assertTrue( mdImpl1.getExternalReference().equals( this.uri ));

    assertTrue( mdImpl2.isContainedContent());
    assertTrue( mdImpl2.getContent().equals( this.content));
  }

  public void testBuilderIllegalState()
  {
    // Test getContent() when isContainedContent()==false;
    // Should throw IllegalStateException.
    try
    { mdImpl1.getContent(); }
    catch ( IllegalStateException ise1 )
    {
      // Test getTitle() when isContainedContent()==true;
      // Should throw IllegalStateException.
      try
      { mdImpl2.getTitle(); }
      catch ( IllegalStateException ise2 )
      {
        // Test ExternalReference() when isContainedContent()==true;
        // Should throw IllegalStateException.
        try
        { mdImpl2.getExternalReference(); }
        catch ( IllegalStateException ise3 )
        {
          return;
        }
        catch( Exception e )
        { fail( "Unexpected non-IllegalStateException."); }
      }
      catch( Exception e )
      { fail( "Unexpected non-IllegalStateException."); }
    }
    catch( Exception e )
    { fail( "Unexpected non-IllegalStateException."); }
    fail( "Did not throw expected IllegalStateException." );
  }

  public void testBuild()
  {
    // Check if buildable
    BuilderIssues issues = mdImpl1.getIssues();
    if ( ! issues.isValid() )
    {
      StringBuilder stringBuilder = new StringBuilder( "Invalid metadata: " ).append( issues.toString());
      fail( stringBuilder.toString() );
    }
    issues = mdImpl2.getIssues();
    if ( ! issues.isValid() )
    {
      StringBuilder stringBuilder = new StringBuilder( "Invalid metadata: " ).append( issues.toString());
      fail( stringBuilder.toString() );
    }

    // Build
    try
    { md1 = mdImpl1.build(); }
    catch ( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); }

    try
    { md2 = mdImpl2.build(); }
    catch ( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); }
  }

  public void testBuiltGet()
  {
    // *** Build doesn't do anything since MetadataImpl is immutable. ***
//    this.testBuild();
//
//    assertFalse( md1.isContainedContent() );
//    assertTrue( md1.getTitle().equals( this.title ) );
//    assertTrue( md1.getExternalReference().equals( this.uri ) );
//
//    assertTrue( md2.isContainedContent() );
//    assertTrue( md2.getContent().equals( this.content ) );
  }

  public void testBuiltGetIllegalState()
  {
    // *** Build doesn't do anything since MetadataImpl is immutable. ***
//    this.testBuild();
//
//    // Test getContent() when isContainedContent()==false;
//    // Should throw IllegalStateException.
//    try
//    { md1.getContent(); }
//    catch ( IllegalStateException ise1 )
//    {
//      // Test getTitle() when isContainedContent()==true;
//      // Should throw IllegalStateException.
//      try
//      { md2.getTitle(); }
//      catch ( IllegalStateException ise2 )
//      {
//        // Test getExternalReference() when isContainedContent()==true;
//        // Should throw IllegalStateException.
//        try
//        { md2.getExternalReference(); }
//        catch ( IllegalStateException ise3 )
//        {
//          return;
//        }
//        catch( Exception e )
//        { fail( "Unexpected non-IllegalStateException."); }
//      }
//      catch( Exception e )
//      { fail( "Unexpected non-IllegalStateException."); }
//    }
//    catch( Exception e )
//    { fail( "Unexpected non-IllegalStateException."); }
//    fail( "Did not throw expected IllegalStateException." );
  }
}
