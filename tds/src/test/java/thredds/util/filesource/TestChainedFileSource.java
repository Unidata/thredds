/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.util.filesource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.TestFileDirUtils;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;


/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestChainedFileSource
{
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private File tmpDir;
  private File contentDir;
  private File publicDir;
  private File iddDir;
  private File mlodeDir;

  @Before
  public void setUp() throws IOException {
    // Create a data directory and some data files.
    tmpDir = tempFolder.newFolder();

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

  @Test
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

  @Test
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
