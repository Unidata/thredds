package thredds.server.catalogservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.ModelAndViewAssert.assertAndReturnModelAttributeOfType;
import static org.springframework.test.web.ModelAndViewAssert.assertViewName;

import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.ModelAndView;

import thredds.mock.web.MockTdsContextLoader;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class AnyXmlControllerTest extends AbstractCatalogServiceTest {

  @Autowired
  private LocalCatalogServiceController anyXmlController;

  public void showCommandTest() throws Exception {

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/catalog/testGridScan/catalog.xml");
    request.setServletPath("/testGridScan/catalog.xml");
    MockHttpServletResponse response = new MockHttpServletResponse();

    ModelAndView mv = anyXmlController.handleXmlRequest(request, response);

    assertViewName(mv, "threddsInvCatXmlView");
    assertAndReturnModelAttributeOfType(mv, "catalog", thredds.catalog.InvCatalogImpl.class);

  }

  public void subsetCommandTest() throws Exception {

    // SUBSET COMMAND REQUEST
    // setting up the request with default values:
    // command =null
    // datasetId=testFeatureCollection/Test_Feature_Collection_best.ncd
    // htmlView= null
    // verbose = null
    // command null and a providing a datasetId becomes in a subset command
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/catalog/testGFSfmrc/catalog.xml");
    request.setServletPath("/testGFSfmrc/catalog.xml");
    //request.setParameter("command", "subset");
    request.setParameter("dataset", "testGFSfmrc/GFS_CONUS_80km_nc_best.ncd");
    MockHttpServletResponse response = new MockHttpServletResponse();

    ModelAndView mv = anyXmlController.handleXmlRequest(request, response);
    assertViewName(mv, "threddsInvCatXmlView");
    assertAndReturnModelAttributeOfType(mv, "catalog", thredds.catalog.InvCatalogImpl.class);

  }

  public void validateCommandTest() throws Exception {

    // VALIDATE REQUEST --> invalid command!!!!
    // command =validate
    // datasetId= null
    // htmlView= null
    // verbose = null
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/catalog/testGFSfmrc/catalog.xml");
    request.setServletPath("/testGFSfmrc/catalog.xml");
    request.setParameter("datasetId", "FMRC/NCEP/SREF");
    request.setParameter("command", "validate");

    MockHttpServletResponse response = new MockHttpServletResponse();

    ModelAndView mv = anyXmlController.handleXmlRequest(request, response);
    assertNull(mv);
    assertEquals(400, response.getStatus());
    assertEquals("Bad request: The \"command\" field may not be VALIDATE.", response.getErrorMessage());

  }

}
