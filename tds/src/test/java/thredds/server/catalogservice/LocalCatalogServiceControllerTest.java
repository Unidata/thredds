package thredds.server.catalogservice;

import org.junit.Test;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class LocalCatalogServiceControllerTest
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( LocalCatalogServiceControllerTest.class );

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @Test
  public void test1()
  {
    
  }

  @Before
  public void basicSetup( )
  {
    this.request = new MockHttpServletRequest();
    this.request.setMethod( "GET" );
    this.request.setContextPath( "/thredds" );
    this.request.setServletPath( "/remoteCatalogService" );
    this.request.setParameter( "catalog", "http://m/thredds/catalog.xml" );

    this.response = new MockHttpServletResponse();
  }


}
