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

  private MockHttpServletRequest req;
  private BindingResult bindingResult;
  private String bindResultMsg;
  private LocalCatalogRequest lcr;

  private boolean htmlView;
  private String xmlPath = "my/cool/catalog.xml";
  private String htmlPath = "my/cool/catalog.html";
  private String cmdShow = "show";
  private String cmdSubset = "subset";
  private String dsId = "my/cool/dataset";


  public TestLocalCatalogRequest( String name )
  {
    super( name );
  }

  public void testCommandDefaultValues()
  {
    // Command defaults to SHOW when dataset ID not given [xml]:
    //     check that [/catalog/**/*.xml, command=null, dataset=null] is
    //     valid and becomes [**/*.xml, command=SHOW, dataset=null]
    req = this.basicSetup( "/catalog", "/" + xmlPath, null, null );
    htmlView = false;
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), xmlPath );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( lcr.getDataset(), "" );

    // Command defaults to SHOW when dataset ID not given [html]:
    //     check that [/catalog/**/*.html, command=null, dataset=null] is
    //     valid and becomes [**/*.html, command=SHOW, dataset=null]
    req = this.basicSetup( "/catalog", "/" + htmlPath, null, null );
    htmlView = true;
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), htmlPath );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( lcr.getDataset(), "" );

    // Command defaults to SUBSET when dataset ID is given [xml]:
    //     check that [/catalog/**/*.xml, command=null, dataset=ID] is
    //     valid and becomes [**/*.xml, command=SUBSET, dataset=ID]
    req = this.basicSetup( "/catalog", "/" + xmlPath, null, dsId );
    htmlView = false;
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), xmlPath );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( lcr.getDataset(), dsId );

    // Command defaults to SUBSET when dataset ID is given [html]:
    //     check that [/catalog/**/*.html, command=null, dataset=ID] is
    //     valid and becomes [**/*.html, command=SUBSET, dataset=ID]
    req = this.basicSetup( "/catalog", "/" + htmlPath, null, dsId );
    htmlView = true;
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), htmlPath );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( lcr.getDataset(), dsId );
  }

  public void testCommandShow()
  {
    // Test validity:
    // - path="/catalog/**/*.xml";
    // - command=SHOW;
    // - dataset=null
    req = this.basicSetup( "/catalog", "/" + xmlPath, cmdShow, null );
    this.htmlView = false;
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg);
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), xmlPath);
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow) );
    assertEquals( lcr.getDataset(), "");

    // Test validity:
    // - path="/catalog/**/*.xml",
    // - command=SHOW
    // - dataset=ID
    req = this.basicSetup( "/catalog", "/" + xmlPath, cmdShow, dsId );
    this.htmlView = false;
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), xmlPath );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( lcr.getDataset(), dsId );
  }

  public void testCommandSubsetValid()
  {
    // Test validity:
    // - path="/catalog/**/*.xml",
    // - command=SUBSET
    // - dataset=ID
    req = this.basicSetup( "/catalog", "/" + xmlPath, cmdSubset, dsId );
    this.htmlView = false;
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), xmlPath );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( lcr.getDataset(), dsId );
  }

  public void testCommandSubsetNoDatasetInvalid()
  {
    // Test that invalid:
    // - path="/catalog/**/*.xml",
    // - command=SUBSET
    // - dataset=null
    req = this.basicSetup( "/catalog", "/" + xmlPath, cmdSubset, null );
    this.htmlView = false;
    req.setParameter( "dataset", "" );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    String msg = checkBindingResults( bindingResult );
    if ( msg == null )
      fail( "No binding error for command=SUBSET&dataset==\"\"");
    System.out.println( "As expected, command=SUBSET&dataset==\"\" got binding error: " + msg );
  }

  public void testCatalogServletPathWithHtml()
  {
    // Test validity when "command"==SHOW
    req = basicSetup( "/catalog", htmlPath, cmdShow, null );
    htmlView = true;
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg);
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), htmlPath);
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow) );
    assertEquals( lcr.getDataset(), "");

    // Test validity with "command"==SHOW and "dataset"!=null
    req = basicSetup( "/catalog", htmlPath, cmdShow, dsId );
    htmlView = true;
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), htmlPath );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( lcr.getDataset(), dsId );

    // Test validity with "command"==SUBSET and "dataset"!=null
    req = basicSetup( "/catalog", htmlPath, cmdSubset, dsId );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), htmlPath );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( lcr.getDataset(), dsId );

    // Test failure with "command"==SUBSET and "dataset"==""
    req = basicSetup( "/catalog", htmlPath, cmdSubset, "" );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    String msg = checkBindingResults( bindingResult );
    if ( msg == null )
      fail( "No binding error for command=SUBSET&dataset==\"\"");
    System.out.println( "As expected, command=SUBSET&dataset==\"\" got binding error: " + msg );
  }

  public void testStarDotXmlServletPathWithXml()
  {
    // Test validity when "command"==SHOW
    req = basicSetup( xmlPath, null, cmdShow, null );
    htmlView = false;
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg);
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), xmlPath);
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow) );
    assertEquals( lcr.getDataset(), "");

    // Test validity with "command"==SHOW and "dataset"!=null
    req = basicSetup( xmlPath, null, cmdShow, dsId );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), xmlPath );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( lcr.getDataset(), dsId );

    // Test validity with "command"==SUBSET and "dataset"!=null
    req = basicSetup( xmlPath, null, cmdSubset, dsId );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), xmlPath );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( lcr.getDataset(), dsId );

    // Test failure with "command"==SUBSET and "dataset"==""
    req = basicSetup( xmlPath, null, cmdSubset, "" );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    String msg = checkBindingResults( bindingResult );
    if ( msg == null )
      fail( "No binding error for command=SUBSET&dataset==\"\"");
    System.out.println( "As expected, command=SUBSET&dataset==\"\" got binding error: " + msg );
  }

  public void testStarDotHtmlServletPathWithHtml()
  {
    // Test validity when "command"==SHOW
    req = basicSetup( htmlPath, null, cmdShow, null );
    htmlView = true;
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg);
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), htmlPath);
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow) );
    assertEquals( lcr.getDataset(), "");

    // Test validity with "command"==SHOW and "dataset"!=null
    req = basicSetup( htmlPath, null, cmdShow, dsId );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), htmlPath );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdShow ) );
    assertEquals( lcr.getDataset(), dsId );

    // Test validity with "command"==SUBSET and "dataset"!=null
    req = basicSetup( htmlPath, null, cmdSubset, dsId );
    bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( req, htmlView );
    bindResultMsg = checkBindingResults( bindingResult );
    if ( bindResultMsg != null )
      fail( bindResultMsg );
    lcr = (LocalCatalogRequest) bindingResult.getTarget();
    assertEquals( lcr.getPath(), htmlPath );
    assertTrue( lcr.getCommand().toString().equalsIgnoreCase( cmdSubset ) );
    assertEquals( lcr.getDataset(), dsId );

    // Test failure with "command"==SUBSET and "dataset"==""
    req = basicSetup( htmlPath, null, cmdSubset, "" );
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

  public MockHttpServletRequest basicSetup( String servletPath, String pathInfo,
                                            String command, String datasetId )
  {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setMethod( "GET" );
    req.setContextPath( "/thredds" );
    req.setServletPath( servletPath );
    req.setPathInfo( pathInfo );
    req.setParameter( parameterNameCommand, command );
    req.setParameter( parameterNameDatasetId, datasetId );
    return req;
  }
}