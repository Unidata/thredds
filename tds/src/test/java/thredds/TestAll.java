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
package thredds;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import thredds.server.opendap.TestCEEvaluator;

/**
 * TestSuite that runs all TDS unit tests.
 *
 */
public class TestAll
{
  public static junit.framework.Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTestSuite( thredds.servlet.TestDataRootHandler.class );
    suite.addTest( new JUnit4TestAdapter( thredds.servlet.StaticViewerTest.class ));
    // suite.addTest( new JUnit4TestAdapter( thredds.server.catalogservice.LocalCatalogServiceControllerTest.class ) );
    suite.addTestSuite( thredds.server.catalogservice.TestLocalCatalogRequest.class );
    suite.addTestSuite( thredds.server.catalogservice.TestRemoteCatalogRequest.class );
    suite.addTest( new JUnit4TestAdapter( thredds.server.ncSubset.GridServletTest.class ));
    suite.addTestSuite( thredds.util.TestStartsWithPathAliasReplacement.class );
    suite.addTestSuite( thredds.util.TestStringValidateEncodeUtils.class );
    suite.addTest( thredds.util.filesource.TestAll.suite() );

    suite.addTest(new TestSuite(TestCEEvaluator.class));    

    return suite;
  }

  // TODO all the static stuff below is also in ucar.nc2.unidata.testUtil.TestAll, can we unify?

  // Determine how Unidata "/upc/share" directory is mounted
  // on local machine by reading system or THREDDS property.
  static
  {
    String upcSharePropName = "unidata.upc.share.path";
    String threddsPropFileName = "thredds.properties";

    // Get system property
    String path = System.getProperty( upcSharePropName );
    if ( path == null )
    {
      // Get user property.
      File userHomeDirFile = new File( System.getProperty( "user.home" ) );
      File userThreddsPropsFile = new File( userHomeDirFile, threddsPropFileName );
      if ( userThreddsPropsFile.exists() && userThreddsPropsFile.canRead() )
      {
        Properties userThreddsProps = new Properties();
        try
        {
          userThreddsProps.load( new FileInputStream( userThreddsPropsFile ) );
        }
        catch ( IOException e )
        {
          System.out.println( "**Failed loading user THREDDS property file: " + e.getMessage() );
        }
        if ( userThreddsProps != null && !userThreddsProps.isEmpty() )
        {
          path = userThreddsProps.getProperty( upcSharePropName );
        }
      }
    }

    if ( path == null )
    {
      // Get default path.
      System.out.println( "**No \"unidata.upc.share.path\"property, defaulting to \"/share/\"." );
      path = "/share/";
    }
    // Make sure path ends with a slash.
    if ( ( !path.endsWith( "/" ) ) && !path.endsWith( "\\" ) )
    {
      path = path + "/";
    }
    upcShareDir = path;
  }

  /**
   * Unidata "/upc/share" directory (MAY NOT be used in Unidata nightly testing).
   */
  public static String upcShareDir;

  /**
   * Level 3 test data directory (MAY NOT be used in Unidata nightly testing).
   * Unidata "/upc/share/testdata2" directory. For once off testing and debuging.
   */
  public static String upcShareTestDataDir = upcShareDir + "testdata/";

  /**
   * Level 2 test data directory (MAY be used in Unidata nightly testing).
   * Unidata "/upc/share/thredds/data" directory
   */
  public static String upcShareThreddsDataDir = upcShareDir + "thredds/data/";

  // Make sure the temp data dir is created.
  static
  {
    File file = new File( upcShareDir );
    if ( !file.exists() )
    {
      System.out.println( "**WARN: Non-existence of \"/upc/share\" directory <" + file.getAbsolutePath() + ">." );
    }

    file = new File( upcShareTestDataDir );
    if ( !file.exists() )
    {
      System.out.println( "**WARN: Non-existence of Level 3 test data directory <" + file.getAbsolutePath() + ">." );
    }
    file = new File( upcShareThreddsDataDir );
    if ( !file.exists() )
    {
      System.out.println( "**WARN: Non-existence if Level 2 test data directory <" + file.getAbsolutePath() + ">." );
    }
  }

  /**
   * Level 1 test data directory (distributed with code and MAY be used in Unidata nightly testing).
   */
  public static String cdmTestDataDir = "src/test/data/";

  /**
   * Temporary data directory (for writing temporary data).
   */
  public static String temporaryDataDir = "target/test/tmp/";

  // Make sure the temp data dir is created.
  static
  {
    File tmpDataDir = new File( temporaryDataDir );
    if ( !tmpDataDir.exists() )
    {
      if ( !tmpDataDir.mkdirs() )
      {
        System.out.println( "**ERROR: Could not create temporary data dir <" + tmpDataDir.getAbsolutePath() + ">." );
      }
    }
  }
}
