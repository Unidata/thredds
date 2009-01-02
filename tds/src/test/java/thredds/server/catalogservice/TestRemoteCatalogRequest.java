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
    String cmdValidate = "validate";
    String datasetId = "my/cool/dataset";

    // Basic setup
    MockHttpServletRequest req;
    BindingResult bindingResult;
    String bindResultMsg;

    // Test that valid when "command"==null
    req = basicSetup( catUriString, null, null, null, null );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    RemoteCatalogRequest rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdShow ) ); // default
    assertEquals( rcr.getDataset(), "" ); //default
    assertFalse( rcr.isVerbose()); //default
    assertFalse( rcr.isHtmlView()); // default

    // Test that valid when "command"==SHOW
    req = basicSetup( catUriString, cmdShow, null, null, null );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( rcr.getDataset(), "" );
    assertFalse( rcr.isVerbose() );
    assertFalse( rcr.isHtmlView() );

    // Test that valid when "command"==SHOW, "htmlView"=false
    req = basicSetup( catUriString, cmdShow, null, "false", null );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( rcr.getDataset(), "" );
    assertFalse( rcr.isVerbose());
    assertFalse( rcr.isHtmlView());

    // Test that valid when "command"==SHOW, "htmlView"=true
    req = basicSetup( catUriString, cmdShow, null, "true", null );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( rcr.getDataset(), "" ); // default
    assertFalse( rcr.isVerbose() );  // default
    assertTrue( rcr.isHtmlView() );

    // Test that valid when "command"==SUBSET, "dataset"=dsId
    req = basicSetup( catUriString, cmdSubset, datasetId, null, null );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( rcr.getDataset(), datasetId );
    assertFalse( rcr.isVerbose() ); // default
    assertFalse( rcr.isHtmlView() ); // default

    // Test that valid when "command"==SUBSET, "dataset"=dsId "htmlView"=false
    req = basicSetup( catUriString );
    req.setParameter( parameterNameCommand, cmdSubset );
    req.setParameter( parameterNameDatasetId, datasetId );
    req.setParameter( parameterNameHtmlView, "false" );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( rcr.getDataset(), datasetId );
    assertFalse( rcr.isVerbose() ); // default
    assertFalse( rcr.isHtmlView() );

    // Test that valid when "command"==SUBSET, "dataset"=dsId "htmlView"=true
    req = basicSetup( catUriString );
    req.setParameter( parameterNameCommand, cmdSubset );
    req.setParameter( parameterNameDatasetId, datasetId );
    req.setParameter( parameterNameHtmlView, "true" );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( rcr.getDataset(), datasetId );
    assertFalse( rcr.isVerbose() ); // default
    assertTrue( rcr.isHtmlView() );

    // Test that valid when "command"==VALIDATE, "dataset"=dsId
    req = basicSetup( catUriString );
    req.setParameter( parameterNameCommand, cmdValidate );
    req.setParameter( parameterNameDatasetId, datasetId );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdValidate ) );
    assertEquals( rcr.getDataset(), datasetId );
    assertFalse( rcr.isVerbose() ); // default
    assertFalse( rcr.isHtmlView() ); // default

    // Test that valid when "command"==VALIDATE, "dataset"=dsId, "verbose"=true
    req = basicSetup( catUriString );
    req.setParameter( parameterNameCommand, cmdValidate );
    req.setParameter( parameterNameDatasetId, datasetId );
    req.setParameter( parameterNameVerbose, "true" );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdValidate ) );
    assertEquals( rcr.getDataset(), datasetId );
    assertTrue( rcr.isVerbose() );
    assertFalse( rcr.isHtmlView() ); // default

    // Test that valid when "command"==VALIDATE, "dataset"=dsId, "verbose"=false
    req = basicSetup( catUriString );
    req.setParameter( parameterNameCommand, cmdValidate );
    req.setParameter( parameterNameDatasetId, datasetId );
    req.setParameter( parameterNameVerbose, "false" );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdValidate ) );
    assertEquals( rcr.getDataset(), datasetId );
    assertFalse( rcr.isVerbose() );
    assertFalse( rcr.isHtmlView() ); // default
  }

  public void testBadRequests()
  {
    String catUriString = "http://motherlode.ucar.edu:8080/thredds/catalog.xml";
    String cmdShow = "show";
    String cmdSubset = "subset";
    String cmdValidate = "validate";
    String datasetId = "my/cool/dataset";

    // Test that invalid when "catalog"==null
    MockHttpServletRequest req = basicSetup( null );
    req.setParameter( parameterNameCommand, cmdShow );

    BindingResult bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    String bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg == null )
      fail( "No binding error for catalog=null" );
    System.out.println( "As expected, catalog=null got binding error: " + bindResultMsg );

    // Test that invalid when catalog URI is not absolute
    req = basicSetup( "/thredds/catalog.xml" );
    req.setParameter( parameterNameCommand, cmdShow );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg == null )
      fail( "No binding error for catalog URI not absolute" );
    System.out.println( "As expected, catalog URI not absolute got binding error: " + bindResultMsg );

    // Test that invalid when catalog URI is not HTTP
    req = basicSetup( "ftp://ftp.unidata.ucar.edu/pub/thredds/" );
    req.setParameter( parameterNameCommand, cmdShow );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg == null )
      fail( "No binding error for catalog URI not HTTP" );
    System.out.println( "As expected, catalog URI not HTTP got binding error: " + bindResultMsg );

    // Test that invalid when "command"==SUBSET, "dataset"==null
    req = basicSetup( catUriString );
    req.setParameter( parameterNameCommand, cmdSubset );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg == null )
      fail( "No binding error for command=SUBSET&dataset==null" );
    System.out.println( "As expected, command=SUBSET&dataset==null got binding error: " + bindResultMsg );
  }

  public MockHttpServletRequest basicSetup( String catUriString )
  {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod( "GET" );
    req.setContextPath( "/thredds" );
    req.setServletPath( "/remoteCatalogService" );
    req.setParameter( parameterNameCatalog, catUriString );
    return req;
  }

  public MockHttpServletRequest basicSetup( String catUriString, String command,
                                            String datasetId, String htmlView,
                                            String verbose )
  {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod( "GET" );
    req.setContextPath( "/thredds" );
    req.setServletPath( "/remoteCatalogService" );
    req.setParameter( parameterNameCatalog, catUriString );
    req.setParameter( parameterNameCommand, command );
    req.setParameter( parameterNameDatasetId, datasetId );
    req.setParameter( parameterNameHtmlView, htmlView );
    req.setParameter( parameterNameVerbose, verbose );
    return req;
  }
}