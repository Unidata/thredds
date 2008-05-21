package thredds;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import junit.framework.TestSuite;
import thredds.server.config.TestBasicDescendantFileSource;

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
    suite.addTestSuite( thredds.server.config.TestAll.class );

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
      System.out.println( "**No \"unidata.upc.share.path\"property, defaulting to \"/upc/share/\"." );
      path = "/upc/share/";
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
   * Unidata "/upc/share/testdata" directory. For once off testing and debuging.
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
