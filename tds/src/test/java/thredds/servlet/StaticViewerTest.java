package thredds.servlet;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.server.viewer.ViewerLinkProvider;
import thredds.server.viewer.ViewerServiceImpl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.*;

public class StaticViewerTest {
  @Test
  public void checkViewerPropertyWithUrlReplacement() throws URISyntaxException {
    String host = "http://test.thredds.servlet.StaticViewerTest";
    String contextPath = "/thredds";
    String servletPath = "/catalog";
    String catPathNoExtension = "/checkViewerPropertyWithUrlReplacement";
    String docBaseUriString = host + contextPath + servletPath + catPathNoExtension + ".xml";
    URI docBaseUri = new URI(docBaseUriString);

    String catalogAsString = setupCatDsWithViewerProperty("viewer1", "{url}.info,DS info");

    Dataset ds1 = constructCatalogAndAssertAsExpected(docBaseUri, catalogAsString);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    request.setContextPath(contextPath);
    request.setServletPath(servletPath);
    request.setPathInfo(catPathNoExtension + ".html");
    request.setParameter("dataset", "ds1");

    ViewerLinkProvider sv = ViewerServiceImpl.getStaticView();
    List<ViewerLinkProvider.ViewerLink> viewerLinks = sv.getViewerLinks( ds1, request);
    assertNotNull(viewerLinks);
    Assert.assertEquals(1, viewerLinks.size());

    ViewerLinkProvider.ViewerLink vl = viewerLinks.get(0);
    assertNotNull(vl);
    Assert.assertEquals("DS info", vl.getTitle());
    Assert.assertEquals(host + contextPath + "/dodsC" + "/test/ds1.nc.info", vl.getUrl());
  }

  @Test
  public void checkViewerPropertyWithOpendapReplacement() throws URISyntaxException {
    String host = "http://test.thredds.servlet.StaticViewerTest";
    String contextPath = "/thredds";
    String servletPath = "/catalog";
    String catPathNoExtension = "/checkViewerPropertyWithOpendapReplacement";
    String docBaseUriString = host + contextPath + servletPath + catPathNoExtension + ".xml";
    URI docBaseUri = new URI(docBaseUriString);

    String catalogAsString = setupCatDsWithViewerProperty("viewer1", "{OPENDAP}.info,ODAP DS info");

    Dataset ds1 = constructCatalogAndAssertAsExpected(docBaseUri, catalogAsString);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    request.setContextPath(contextPath);
    request.setServletPath(servletPath);
    request.setPathInfo(catPathNoExtension + ".html");
    request.setParameter("dataset", "ds1");

    ViewerLinkProvider sv = ViewerServiceImpl.getStaticView();
    List<ViewerLinkProvider.ViewerLink> viewerLinks = sv.getViewerLinks( ds1, request);
    assertNotNull(viewerLinks);
    Assert.assertEquals(1, viewerLinks.size());

    ViewerLinkProvider.ViewerLink vl = viewerLinks.get(0);
    assertNotNull(vl);
    Assert.assertEquals("ODAP DS info", vl.getTitle());
    Assert.assertEquals(host + contextPath + "/dodsC" + "/test/ds1.nc.info", vl.getUrl());
  }

  @Test
  public void checkViewerPropertyWithRemoteWmsReplacement() throws URISyntaxException {
    String host = "http://test.thredds.servlet.StaticViewerTest";
    String contextPath = "/thredds";
    String servletPath = "/catalog";
    String catPathNoExtension = "/checkViewerPropertyWithOpendapReplacement";
    String docBaseUriString = host + contextPath + servletPath + catPathNoExtension + ".xml";
    URI docBaseUri = new URI(docBaseUriString);

    String catalogAsString = setupCatDsWithViewerProperty("viewer1", "{REMOTEWMS}.info,RemoteWms DS info");

    Dataset ds1 = constructCatalogAndAssertAsExpected(docBaseUri, catalogAsString);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    request.setContextPath(contextPath);
    request.setServletPath(servletPath);
    request.setPathInfo(catPathNoExtension + ".html");
    request.setParameter("dataset", "ds1");

    ViewerLinkProvider sv = ViewerServiceImpl.getStaticView();
    List<ViewerLinkProvider.ViewerLink> viewerLinks = sv.getViewerLinks( ds1, request);
    assertNotNull(viewerLinks);
    Assert.assertEquals(1, viewerLinks.size());

    ViewerLinkProvider.ViewerLink vl = viewerLinks.get(0);
    assertNotNull(vl);
    Assert.assertEquals("RemoteWms DS info", vl.getTitle());
    Assert.assertEquals("http://server/thredds/wms/test/ds1.nc.info", vl.getUrl());
  }

  private Dataset constructCatalogAndAssertAsExpected(URI docBaseUri, String catalogAsString) {

    try {
      CatalogBuilder builder = new CatalogBuilder();
      Catalog cat = builder.buildFromString(catalogAsString, docBaseUri);

      if (builder.hasFatalError()) {
        System.out.printf("Validate failed %s %n%s%n", docBaseUri, builder.getErrorMessage());
        assert false;
        return null;
      }

      List<Dataset> datasets = cat.getDatasets();
      assertNotNull(datasets);
      Assert.assertEquals(1, datasets.size());
      Dataset ds1 = datasets.get(0);
      assertNotNull(ds1);
      Assert.assertEquals("ds 1", ds1.getName());
      return ds1;

    } catch (IOException e) {
      e.printStackTrace();
      assert false;
      return null;
    }
  }

  private static String setupCatDsWithViewerProperty(String viewerName, String viewerValue) {
    return "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<catalog xmlns='http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0'\n" +
            "         xmlns:xlink='http://www.w3.org/1999/xlink'\n" +
            "         name='Catalog 1'\n" +
            "         version='1.0.3'>\n" +
            "  <service name='all' serviceType='Compound' base=''>\n" +
            "    <service name='odap' serviceType='OPENDAP' base='/thredds/dodsC/' />\n" +
            "    <service name='wcs' serviceType='WCS' base='/thredds/wcs/' />\n" +
            "    <service name='wms' serviceType='WMS' base='/thredds/wms/' />\n" +
            "    <service name='remoteWMS' serviceType='REMOTEWMS' base='http://server/thredds/wms/' />\n" +
            "  </service>\n" + "  <dataset name='ds 1' ID='ds1' urlPath='test/ds1.nc'>\n" +
            "    <metadata inherited='true'>\n" + "      <serviceName>all</serviceName>\n" +
            "    </metadata>\n" +
            "    <property name='" + viewerName + "' value='" + viewerValue + "' />\n" +
            "  </dataset>\n" +
            "</catalog>";
  }

}
