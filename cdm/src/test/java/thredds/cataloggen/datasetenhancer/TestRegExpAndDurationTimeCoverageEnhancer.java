// $Id: TestRegExpAndDurationTimeCoverageEnhancer.java 61 2006-07-12 21:36:00Z edavis $
package thredds.cataloggen.datasetenhancer;

import junit.framework.*;
import thredds.catalog.InvDatasetImpl;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;

import java.io.IOException;
import java.util.List;
import java.util.Date;

/**
 * _more_
 *
 * @author edavis
 * @since Mar 27, 2006 1:21:49 PM
 */
public class TestRegExpAndDurationTimeCoverageEnhancer extends TestCase
{
//  static private org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( TestRegExpAndDurationTimeCoverageEnhancer.class );


  public TestRegExpAndDurationTimeCoverageEnhancer( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  /**
   * Test ...
   */
  public void testSuccess()
  {
    String matchPattern = "NDFD_CONUS_5km_([0-9]{4})([0-9]{2})([0-9]{2})_([0-9]{2})([0-9]{2}).grib2";
    String substitutionPattern = "$1-$2-$3T$4:$5:00";
    String duration = "96 hours";

    String dsName = "NDFD_CONUS_5km_20060325_1200.grib2";

    RegExpAndDurationTimeCoverageEnhancer me =
            new RegExpAndDurationTimeCoverageEnhancer(
                    matchPattern, substitutionPattern, duration );
    assertTrue( me != null );

    InvDatasetImpl ds = new InvDatasetImpl( null, dsName );
    CrawlableDataset crDs = new MyCrDs( dsName, dsName );

    assertTrue( "Failed to add metadata.",
                me.addMetadata( ds, crDs ));
  }

  public void testFail()
  {
    String matchPattern = "NDFD_CONUS_5km_([0-9]{4})([0-9]{2})([0-9]{2})_([0-9]{2})([0-9]{2}).grib2";
    String substitutionPattern = "$1-$2-$3T$4:$5:00";
    String duration = "96 hours";

    String dsName = "NDFD_CONUS_5km_200600325_1200.grib2";

    RegExpAndDurationTimeCoverageEnhancer me =
            new RegExpAndDurationTimeCoverageEnhancer(
                    matchPattern, substitutionPattern, duration );
    assertTrue( me != null );

    InvDatasetImpl ds = new InvDatasetImpl( null, dsName );
    CrawlableDataset crDs = new MyCrDs( dsName, dsName );

    assertTrue( "Unexpected success adding metadata.",
                ! me.addMetadata( ds, crDs ));
  }

  private class MyCrDs implements CrawlableDataset
  {
    private String _path;
    private String _name;

    private MyCrDs( String path, String name )
    {
      if ( ! path.endsWith( name)) throw new IllegalArgumentException( "Path <"+path+"> must end with name <"+name+">.");
      if ( name.indexOf( "/") != -1 ) throw new IllegalArgumentException( "Name <"+name+"> must not contain slash (\"/\".");
      this._path = path;
      this._name = name;
    }

    public Object getConfigObject()
    {
      return null;
    }

    public String getPath()
    {
      return _path;
    }

    public String getName()
    {
      return _name;
    }

    public CrawlableDataset getParentDataset() throws IOException
    {
      return null;
    }

    public boolean isCollection()
    {
      return false;
    }

    public List listDatasets() throws IOException
    {
      return null;
    }

    public List listDatasets( CrawlableDatasetFilter filter ) throws IOException
    {
      return null;
    }

    public long length()
    {
      return 0;
    }

    public Date lastModified() // or long milliseconds?
    {
      return null;
    }
  }
}
/*
 * $Log: TestRegExpAndDurationTimeCoverageEnhancer.java,v $
 * Revision 1.1  2006/03/27 22:30:24  edavis
 * Add some tests for RegExpAndDurationTimeCoverageEnhancer. (Was getting
 * java.lang.NumberFormatException in certain situations but looks like a synchronize
 * issue in thredds.datatype.DateType rather than a problem here.)
 *
 */