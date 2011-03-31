package thredds.server.ncSubset;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class GridServletTest
{
  @After
  public void resetGridServletStatics()
  {
    // Smells! Yuck!
    // Static values affect further tests.
    // ToDo LOOK Convert GridServlet to use Spring MVC Controller and TdsConfig
    GridServlet.setContextPath( "/thredds" );
    GridServlet.setServletPath( "/ncss/grid" );
    GridServlet.setServletCachePath( "/ncSubset/cache" );
  }

  @Test
  public void checkContextPath()
  {
    GridServlet gs = new GridServlet();
    assertEquals( "/thredds", gs.getContextPath() );

    GridServlet.setContextPath( "/mine" );
    assertEquals( "/mine", gs.getContextPath() );
  }

  @Test
  public void checkDatasetUrl()
  {
    GridServlet gs = new GridServlet();
    assertEquals( "ncss/grid/", gs.getPath() );
    assertEquals( "/thredds/ncss/grid/junk", gs.buildDatasetUrl( "junk" ) );

    GridServlet.setServletPath( "/mine/grod" );
    assertEquals( "mine/grod/", gs.getPath() );
    assertEquals( "/thredds/mine/grod/junk", gs.buildDatasetUrl( "junk" ) );
  }

  @Test
  public void checkCacheUrl()
  {
    GridServlet gs = new GridServlet();
    assertEquals( "/thredds/ncSubset/cache/junk", gs.buildCacheUrl( "junk" ));

    GridServlet.setServletCachePath( "/my/couch" );
    assertEquals( "/thredds/my/couch/junk", gs.buildCacheUrl( "junk" ) );
  }
}
