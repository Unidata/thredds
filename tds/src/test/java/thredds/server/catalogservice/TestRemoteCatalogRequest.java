package thredds.server.catalogservice;

import junit.framework.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.*;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestRemoteCatalogRequest extends TestCase
{
  private String parameterNameCatalog = "catalog";
  private String parameterNameCommand = "command";
  private String parameterNameDatasetId = "dataset";
  private String parameterNameVerbose = "verbose";
  private String parameterNameHtmlView = "htmlView";

  public TestRemoteCatalogRequest( String name )
  {
    super( name );
  }

  public void testGoodRequests()
  {
    String catUriString = "http://motherlode.ucar.edu:8080/thredds/catalog.xml";
    String cmdShow = "show";
    String cmdSubset = "subset";
    String datasetId = "my/cool/dataset";

    // Basic setup
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod( "GET" );
    req.setContextPath( "/thredds" );
    req.setServletPath( "/remoteCatalogService" );

    // Test that valid when "command"==SHOW, "htmlView"=false
    req.setParameter( parameterNameCatalog, catUriString );
    req.setParameter( parameterNameCommand, cmdShow );
    req.setParameter( parameterNameVerbose, "false" );
    req.setParameter( parameterNameHtmlView, "false" );
    //req.setParameter( parameterNameDataset", datasetId );

    BindingResult bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );

    String bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    RemoteCatalogRequest rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( rcr.getDataset(), "" );
    assertFalse( rcr.isVerbose());
    assertFalse( rcr.isHtmlView());

    // Test that valid when "command"==SHOW, "htmlView"=true
    // Test that valid when "command"==SUBSET, "dataset"=dsId
    // Test that valid when "command"==SUBSET, "dataset"=dsId "htmlView"=false
    // Test that valid when "command"==SUBSET, "dataset"=dsId "htmlView"=true
    // Test that valid when "command"==VALIDATE, "dataset"=dsId
    // Test that valid when "command"==VALIDATE, "dataset"=dsId, "verbose"=true
    // Test that valid when "command"==VALIDATE, "dataset"=dsId, "verbose"=false
  }

  public void testBadRequests()
  {
    String catUriString = "http://motherlode.ucar.edu:8080/thredds/catalog.xml";
    String cmdShow = "show";
    String cmdSubset = "subset";
    String datasetId = "my/cool/dataset";

    // Basic setup
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod( "GET" );
    req.setContextPath( "/thredds" );
    req.setServletPath( "/remoteCatalogService" );

    // Test that valid when "command"==SUBSET, "dataset"==null
    req.setParameter( parameterNameCatalog, catUriString );
    req.setParameter( parameterNameCommand, cmdSubset );

    BindingResult bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );

    String bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg == null )
      fail( "No binding error for command=SUBSET&dataset==\"\"" );
    System.out.println( "As expected, command=SUBSET&dataset==\"\" got binding error: " + bindResultMsg );

  }
}