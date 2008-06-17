package thredds.server.config;

import junit.framework.*;
import thredds.TestAll;
import thredds.util.filesource.BasicDescendantFileSource;
import thredds.util.filesource.BasicWithExclusionsDescendantFileSource;
import thredds.util.filesource.ChainedFileSource;
import thredds.util.filesource.DescendantFileSource;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import ucar.unidata.util.TestUtils;

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
    tmpDir = TestUtils.addDirectory( new File( TestAll.temporaryDataDir ), "TestChainedFileSource" );

    contentDir = TestUtils.addDirectory( tmpDir, "content" );
    TestUtils.addFile( contentDir, "myCat.xml" );
    File myDir = TestUtils.addDirectory( contentDir, "myDir" );
    TestUtils.addFile( myDir, "myCat.xml" );
    publicDir = TestUtils.addDirectory( contentDir, "public" );
    TestUtils.addFile( publicDir, "myCat.xml" );
    TestUtils.addFile( publicDir, "myCatPublic.xml" );

    iddDir = TestUtils.addDirectory( tmpDir, "web/alt/idd" );
    TestUtils.addFile( iddDir, "iddCat.xml");
    TestUtils.addFile( iddDir, "myCat.xml");

    mlodeDir = TestUtils.addDirectory( tmpDir, "web/alt/mlode" );
    TestUtils.addFile( mlodeDir, "mlodeCat.xml" );
    TestUtils.addFile( mlodeDir, "iddCat.xml" );
    TestUtils.addFile( mlodeDir, "myCat.xml" );

  }

  protected void tearDown()
  {
    // Delete temp directory.
    TestUtils.deleteDirectoryAndContent( tmpDir );
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