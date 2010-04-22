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
package thredds.util.filesource;

import junit.framework.*;
import thredds.TestAll;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import ucar.unidata.util.TestFileDirUtils;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestChainedFileSource extends TestCase
{
  private File tmpDir;
  private File contentDir;
  private File publicDir;
  private File iddDir;
  private File mlodeDir;

  public TestChainedFileSource( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    // Create a data directory and some data files.
    tmpDir = TestFileDirUtils.addDirectory( new File( TestAll.temporaryDataDir ), "TestChainedFileSource" );

    contentDir = TestFileDirUtils.addDirectory( tmpDir, "content" );
    TestFileDirUtils.addFile( contentDir, "myCat.xml" );
    File myDir = TestFileDirUtils.addDirectory( contentDir, "myDir" );
    TestFileDirUtils.addFile( myDir, "myCat.xml" );
    publicDir = TestFileDirUtils.addDirectory( contentDir, "public" );
    TestFileDirUtils.addFile( publicDir, "myCat.xml" );
    TestFileDirUtils.addFile( publicDir, "myCatPublic.xml" );

    iddDir = TestFileDirUtils.addDirectory( tmpDir, "web/alt/idd" );
    TestFileDirUtils.addFile( iddDir, "iddCat.xml");
    TestFileDirUtils.addFile( iddDir, "myCat.xml");

    mlodeDir = TestFileDirUtils.addDirectory( tmpDir, "web/alt/mlode" );
    TestFileDirUtils.addFile( mlodeDir, "mlodeCat.xml" );
    TestFileDirUtils.addFile( mlodeDir, "iddCat.xml" );
    TestFileDirUtils.addFile( mlodeDir, "myCat.xml" );

  }

  protected void tearDown()
  {
    // Delete temp directory.
    TestFileDirUtils.deleteDirectoryAndContent( tmpDir );
  }

  /**
   * Test ...
   */
  public void testCtorGivenNullOrEmptyChain()
  {
    try
    {
      new ChainedFileSource( null );

      List<DescendantFileSource> chain = Collections.emptyList();
      new ChainedFileSource( chain );

      chain = Collections.singletonList( null );
      new ChainedFileSource( chain);
    }
    catch( IllegalArgumentException e )
    {
      return;
    }
    fail( "Did not throw IllegalArgumentException for null chain, empty chain, or null item in chain.");
  }

  public void testNewGivenNonexistentDirectory()
  {
    List<DescendantFileSource> chain = new ArrayList<DescendantFileSource>();
    DescendantFileSource contentSource = new BasicDescendantFileSource( contentDir );
    DescendantFileSource publicSource = new BasicDescendantFileSource( publicDir );
    DescendantFileSource contentMinusPublicSource = new BasicWithExclusionsDescendantFileSource( contentDir, Collections.singletonList( "public" ) );
    DescendantFileSource iddSource = new BasicDescendantFileSource( iddDir );
    DescendantFileSource mlodeSource = new BasicDescendantFileSource( mlodeDir );
    chain.add( contentMinusPublicSource );
    chain.add( iddSource );
    chain.add( mlodeSource );
    ChainedFileSource configChain = new ChainedFileSource( chain);
    assertNotNull( configChain);

    File myCat = configChain.getFile( "myCat.xml" );
    assertNotNull( myCat );
    assertTrue( contentSource.isDescendant( myCat ));
    assertFalse( publicSource.isDescendant( myCat ));
    assertFalse( iddSource.isDescendant( myCat ));
    assertFalse( mlodeSource.isDescendant( myCat ));

    File myDirCat = configChain.getFile( "myDir/myCat.xml" );
    assertNotNull( myDirCat );
    assertTrue( contentSource.isDescendant( myDirCat ));
    assertFalse( publicSource.isDescendant( myDirCat ));
    assertFalse( iddSource.isDescendant( myDirCat ));
    assertFalse( mlodeSource.isDescendant( myDirCat ));

    File myPublicCat = configChain.getFile( "myPublicCat.xml" );
    assertNull( myPublicCat );

    File iddCat = configChain.getFile( "iddCat.xml" );
    assertNotNull( iddCat );
    assertFalse( contentSource.isDescendant( iddCat ) );
    assertFalse( publicSource.isDescendant( iddCat ) );
    assertTrue( iddSource.isDescendant( iddCat ) );
    assertFalse( mlodeSource.isDescendant( iddCat ) );

    File mlodeCat = configChain.getFile( "mlodeCat.xml" );
    assertNotNull( mlodeCat );
    assertFalse( contentSource.isDescendant( mlodeCat ) );
    assertFalse( publicSource.isDescendant( mlodeCat ) );
    assertFalse( iddSource.isDescendant( mlodeCat ) );
    assertTrue( mlodeSource.isDescendant( mlodeCat ) );
  }
}