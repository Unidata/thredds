package thredds.server.config;

import junit.framework.*;
import org.springframework.util.StringUtils;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestBasicFileLocator extends TestCase
{
  public TestBasicFileLocator( String name )
  {
    super( name );
  }

  /**
   * Test ...
   */
  public void testBasicFileLocator()
  {
    DescendantFileSource bfl = new BasicDescendantFileSource( "");
    assertEquals( "",
                  StringUtils.cleanPath( bfl.getRootDirectoryPath()),
                  bfl.getRootDirectoryPath());
//    me = new BasicDescendantFileSource( );
//    assertTrue( me != null );
  }
}
