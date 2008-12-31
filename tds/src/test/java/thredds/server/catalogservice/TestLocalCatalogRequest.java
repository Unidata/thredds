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
public class TestLocalCatalogRequest extends TestCase
{

  private String parameterNameCommand = "command";
  private String parameterNameDatasetId = "dataset";
  public TestLocalCatalogRequest( String name )
  {
    super( name );
  }

  public void testCatalogServletPathWithXml()
  {
    boolean htmlView = false;
    String path = "my/cool/catalog.xml";
    String cmdShow = "show";
    String cmdSubset = "subset";
    String dsId = "my/cool/dataset";

    // Basic setup
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod( "GET" );
    req.setContextPath( "/thredds" );
    req.setServletPath( "/catalog" );
    req.setPathInfo( "/" + path );

    // Test that valid when "command"==SHOW
    req.setParameter( this.parameterNameCommand, cmdShow );
    BindingResult bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    String bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg);
    LocalCatalogRequest lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), path);
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow) );
    assertEquals( lcr.getDataset(), "");

    // Test validity with "command"==SHOW and "dataset"!=null
    req.setParameter( this.parameterNameDatasetId, dsId );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), path );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( lcr.getDataset(), dsId );

    // Test validity with "command"==SUBSET and "dataset"!=null
    req.setParameter( this.parameterNameCommand, cmdSubset);
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), path );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( lcr.getDataset(), dsId );

    // Test validity with "command"==SUBSET and "dataset"==""
    req.setParameter( "dataset", "" );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    String msg = checkBindingResults( bindingResult );
    if ( msg == null )
      fail( "No binding error for command=SUBSET&dataset==\"\"");
    System.out.println( "As expected, command=SUBSET&dataset==\"\" got binding error: " + msg );
  }

  public void testCatalogServletPathWithHtml()
  {
    boolean htmlView = true;
    String path = "my/cool/catalog.html";
    String cmdShow = "show";
    String cmdSubset = "subset";
    String dsId = "my/cool/dataset";

    // Basic setup
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod( "GET" );
    req.setContextPath( "/thredds" );
    req.setServletPath( "/catalog" );
    req.setPathInfo( "/" + path );

    // Test that valid when "command"==SHOW
    req.setParameter( this.parameterNameCommand, cmdShow );
    BindingResult bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    String bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg);
    LocalCatalogRequest lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), path);
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow) );
    assertEquals( lcr.getDataset(), "");

    // Test validity with "command"==SHOW and "dataset"!=null
    req.setParameter( this.parameterNameDatasetId, dsId );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), path );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( lcr.getDataset(), dsId );

    // Test validity with "command"==SUBSET and "dataset"!=null
    req.setParameter( this.parameterNameCommand, cmdSubset);
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), path );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( lcr.getDataset(), dsId );

    // Test validity with "command"==SUBSET and "dataset"==""
    req.setParameter( "dataset", "" );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    String msg = checkBindingResults( bindingResult );
    if ( msg == null )
      fail( "No binding error for command=SUBSET&dataset==\"\"");
    System.out.println( "As expected, command=SUBSET&dataset==\"\" got binding error: " + msg );
  }

  public void testStarDotXmlServletPathWithXml()
  {
    boolean htmlView = false;
    String path = "my/cool/catalog.xml";
    String cmdShow = "show";
    String cmdSubset = "subset";
    String dsId = "my/cool/dataset";

    // Basic setup
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod( "GET" );
    req.setContextPath( "/thredds" );
    req.setServletPath( "/" + path );

    // Test that valid when "command"==SHOW
    req.setParameter( this.parameterNameCommand, cmdShow );
    BindingResult bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    String bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg);
    LocalCatalogRequest lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), path);
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow) );
    assertEquals( lcr.getDataset(), "");

    // Test validity with "command"==SHOW and "dataset"!=null
    req.setParameter( this.parameterNameDatasetId, dsId );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), path );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( lcr.getDataset(), dsId );

    // Test validity with "command"==SUBSET and "dataset"!=null
    req.setParameter( this.parameterNameCommand, cmdSubset);
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), path );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( lcr.getDataset(), dsId );

    // Test validity with "command"==SUBSET and "dataset"==""
    req.setParameter( "dataset", "" );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    String msg = checkBindingResults( bindingResult );
    if ( msg == null )
      fail( "No binding error for command=SUBSET&dataset==\"\"");
    System.out.println( "As expected, command=SUBSET&dataset==\"\" got binding error: " + msg );
  }

  public void testStarDotHtmlServletPathWithHtml()
  {
    boolean htmlView = true;
    String path = "my/cool/catalog.html";
    String cmdShow = "show";
    String cmdSubset = "subset";
    String dsId = "my/cool/dataset";

    // Basic setup
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod( "GET" );
    req.setContextPath( "/thredds" );
    req.setServletPath( "/" + path );

    // Test that valid when "command"==SHOW
    req.setParameter( this.parameterNameCommand, cmdShow );
    BindingResult bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    String bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg);
    LocalCatalogRequest lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), path);
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow) );
    assertEquals( lcr.getDataset(), "");

    // Test validity with "command"==SHOW and "dataset"!=null
    req.setParameter( this.parameterNameDatasetId, dsId );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), path );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( lcr.getDataset(), dsId );

    // Test validity with "command"==SUBSET and "dataset"!=null
    req.setParameter( this.parameterNameCommand, cmdSubset);
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), path );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( lcr.getDataset(), dsId );

    // Test validity with "command"==SUBSET and "dataset"==""
    req.setParameter( "dataset", "" );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    String msg = checkBindingResults( bindingResult );
    if ( msg == null )
      fail( "No binding error for command=SUBSET&dataset==\"\"");
    System.out.println( "As expected, command=SUBSET&dataset==\"\" got binding error: " + msg );
  }

  static String checkBindingResults( BindingResult bindingResult )
  {
    if ( bindingResult.hasErrors() )
    {
      //noinspection unchecked
      List<ObjectError> errors = bindingResult.getAllErrors();
      StringBuilder sb = new StringBuilder("\n");
      for ( ObjectError error : errors )
      {
        sb.append( "Binding error [" ).append( error.toString() ).append( "]\n" );
      }
      return sb.toString();
    }
    return null;
  }
}