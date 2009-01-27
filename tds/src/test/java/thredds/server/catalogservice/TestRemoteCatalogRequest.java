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
package thredds.server.catalogservice;

import junit.framework.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.*;

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

  private MockHttpServletRequest req;
  private BindingResult bindingResult;
  private String bindResultMsg;
  private RemoteCatalogRequest rcr;

  private String catUriString = "http://motherlode.ucar.edu:8080/thredds/catalog.xml";
  private String cmdShow = "show";
  private String cmdSubset = "subset";
  private String cmdValidate = "validate";
  private String datasetId = "my/cool/dataset";

  public TestRemoteCatalogRequest( String name )
  {
    super( name );
  }

  public void testCommandDefaultValues()
  {
    // Command defaults to SHOW when dataset ID not given:
    //     check that [uri=http://**/*.xml, command=null, dataset=null] is
    //     valid and becomes [uri, command=SHOW, dataset=null]
    req = basicSetup( catUriString, null, null, null, null );
    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( rcr.getDataset(), "" );

    // Command defaults to SUBSET when dataset ID is given:
    //     check that [/catalog/**/*.xml, command=null, dataset=ID, htmlView=null] is
    //     valid and becomes [**/*.xml, command=SUBSET, dataset=ID, htmlView=true]
    req = basicSetup( catUriString, null, datasetId, null, null );
    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( rcr.getDataset(), datasetId );
    assertTrue( rcr.isHtmlView());

    // Command defaults to SUBSET when dataset ID is given:
    //     check that [/catalog/**/*.xml, command=null, dataset=ID, htmlView=true] is
    //     valid and becomes [**/*.xml, command=SUBSET, dataset=ID, htmlView=true]
    req = basicSetup( catUriString, null, datasetId, "true", null );
    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( rcr.getDataset(), datasetId );
    assertTrue( rcr.isHtmlView());

    // Command defaults to SUBSET when dataset ID is given:
    //     check that [/catalog/**/*.xml, command=null, dataset=ID, htmlView=false] is
    //     valid and becomes [**/*.xml, command=SUBSET, dataset=ID, htmlView=false]
    req = basicSetup( catUriString, null, datasetId, "false", null );
    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( rcr.getDataset(), datasetId );
    assertFalse( rcr.isHtmlView());
  }

  public void testGoodRequests()
  {
    // Test that valid when "command"==null
    req = basicSetup( catUriString, null, null, null, null );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdShow ) ); // default
    assertEquals( rcr.getDataset(), "" ); //default
    assertFalse( rcr.isVerbose()); //default
    assertTrue( rcr.isHtmlView()); // default

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
    assertTrue( rcr.isHtmlView() );

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
    assertTrue( rcr.isHtmlView() ); // default

    // Test that valid when "command"==SUBSET, "dataset"=dsId "htmlView"=false
    req = basicSetup( catUriString, cmdSubset, datasetId, "false", null );

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
    req = basicSetup( catUriString, cmdSubset, datasetId, "true", null );

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
    req = basicSetup( catUriString, cmdValidate, datasetId, null, null );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdValidate ) );
    assertEquals( rcr.getDataset(), datasetId );
    assertFalse( rcr.isVerbose() ); // default
    assertTrue( rcr.isHtmlView() ); // default

    // Test that valid when "command"==VALIDATE, "dataset"=dsId, "verbose"=true
    req = basicSetup( catUriString, cmdValidate, datasetId, null, "true" );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdValidate ) );
    assertEquals( rcr.getDataset(), datasetId );
    assertTrue( rcr.isVerbose() );
    assertTrue( rcr.isHtmlView() ); // default

    // Test that valid when "command"==VALIDATE, "dataset"=dsId, "verbose"=false
    req = basicSetup( catUriString, cmdValidate, datasetId, null, "false" );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    rcr = (RemoteCatalogRequest) bindingResult.getTarget();
    assertEquals( rcr.getCatalogUri().toString(), catUriString );
    assertTrue( rcr.getCommand().toString().equalsIgnoreCase( cmdValidate ) );
    assertEquals( rcr.getDataset(), datasetId );
    assertFalse( rcr.isVerbose() );
    assertTrue( rcr.isHtmlView() ); // default
  }

  public void testBadRequests()
  {
    // Test that invalid when "catalog"==null
    req = basicSetup( null, cmdShow, null, null, null );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg == null )
      fail( "No binding error for catalog=null" );
    System.out.println( "As expected, catalog=null got binding error: " + bindResultMsg );

    // Test that invalid when catalog URI is not absolute
    req = basicSetup( "/thredds/catalog.xml", cmdShow, null, null, null );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg == null )
      fail( "No binding error for catalog URI not absolute" );
    System.out.println( "As expected, catalog URI not absolute got binding error: " + bindResultMsg );

    // Test that invalid when catalog URI is not HTTP
    req = basicSetup( "ftp://ftp.unidata.ucar.edu/pub/thredds/", cmdShow, null, null, null );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg == null )
      fail( "No binding error for catalog URI not HTTP" );
    System.out.println( "As expected, catalog URI not HTTP got binding error: " + bindResultMsg );

    // Test that invalid when "command"==SHOW, "htmlView"=false
    req = basicSetup( catUriString, cmdShow, null, "false", null );
    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg == null )
      fail( "No binding error for command=SHOW&htmlView==false" );
    System.out.println( "As expected, command=SHOW&htmlView==false got binding error: " + bindResultMsg );

    // Test that invalid when "command"==SUBSET, "dataset"==null
    req = basicSetup( catUriString, cmdSubset, null, null, null );

    bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( req );
    bindResultMsg = TestLocalCatalogRequest.checkBindingResults( bindingResult );
    if ( bindResultMsg == null )
      fail( "No binding error for command=SUBSET&dataset==null" );
    System.out.println( "As expected, command=SUBSET&dataset==null got binding error: " + bindResultMsg );
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