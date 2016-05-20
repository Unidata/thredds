package thredds.server.cdmremote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jdom2.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import thredds.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.mock.web.MockTdsContextLoader;
import thredds.util.ContentType;
import thredds.util.xml.NcmlParserUtil;
import thredds.util.xml.XmlUtil;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.stream.NcStream;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"/WEB-INF/applicationContext-tdsConfig.xml"}, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class CdmrControllerTest {

  @Autowired
 	private WebApplicationContext wac;

 	private MockMvc mockMvc;

 	@Before
 	public void setup(){
 		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
 	}

  @SpringJUnit4ParameterizedClassRunner.Parameters
 	public static Collection<Object[]> getTestParameters(){
 		return Arrays.asList(new Object[][]{
            {"/cdmremote/NCOF/POLCOMS/IRISH_SEA/files/20060925_0600.nc", 5, 12, 16, "Precipitable_water(0:1,43:53,20:40)"},        // FMRC
            {"/cdmremote/testStationFeatureCollection/files/Surface_METAR_20060325_0000.nc", 9, 22, 47, "wind_speed(0:1)"},  // station
            {"/cdmremote/testBuoyFeatureCollection/files/Surface_Buoy_20130804_0000.nc", 5, 2, 58, "meanWind(0:1)"},  // point
            {"/cdmremote/testSurfaceSynopticFeatureCollection/files/Surface_Synoptic_20130804_0000.nc", 5, 2, 46, "humidity(0:1)"},  // point
    });
 	}

  String path, dataReq;
  int ndims, natts, nvars;
  public CdmrControllerTest(String path, int ndims, int natts, int nvars, String dataReq) {
    this.path = path;
    this.ndims = ndims;
    this.natts = natts;
    this.nvars = nvars;
    this.dataReq = dataReq;
  }

  @Test
   public void cdmRemoteRequestCapabilitiesTest() throws Exception {
     RequestBuilder rb = MockMvcRequestBuilders.get(path).servletPath(path)
   				.param("req", "capabilities");

     MvcResult result = this.mockMvc.perform( rb )
               .andExpect(MockMvcResultMatchers.status().is(200))
               .andExpect(MockMvcResultMatchers.content().contentType(ContentType.xml.getContentHeader()))
               .andReturn();

    //System.out.printf("content = %s%n", result.getResponse().getContentAsString());

    /* String content =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
            "<cdmRemoteCapabilities location=\"http://localhost:80/cdmremote/NCOF/POLCOMS/IRISH_SEA/files/20060925_0600.nc\">"+
            "  <featureDataset type=\"GRID\" url=\"http://localhost:80/cdmremote/NCOF/POLCOMS/IRISH_SEA/files/20060925_0600.nc\" />"+
            "</cdmRemoteCapabilities>";  // LAME

    assert content.equals(result.getResponse().getContentAsString());  */
   }

  @Test
  public void cdmRemoteRequestCdlTest() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get(path).servletPath(path)
  				.param("req", "cdl");

    MvcResult result = this.mockMvc.perform( rb )
              .andExpect(MockMvcResultMatchers.status().is(200))
              .andExpect(MockMvcResultMatchers.content().contentType(ContentType.text.getContentHeader()))
              .andReturn();


    //System.out.printf("content = %s%n", result.getResponse().getContentAsString());
  }

  @Test
   public void cdmRemoteRequestNcmlTest() throws Exception {
     RequestBuilder rb = MockMvcRequestBuilders.get(path).servletPath(path)
   				.param("req", "ncml");

     MvcResult result = this.mockMvc.perform( rb )
               .andExpect(MockMvcResultMatchers.status().is(200))
               .andExpect(MockMvcResultMatchers.content().contentType(ContentType.xml.getContentHeader()))
               .andReturn();


    //System.out.printf("content = %s%n", result.getResponse().getContentAsString());

    Document doc = XmlUtil.getStringResponseAsDoc(result.getResponse());

    int hasDims = NcmlParserUtil.getNcMLElements("netcdf/dimension", doc).size();
    int hasAtts = NcmlParserUtil.getNcMLElements("netcdf/attribute", doc).size();
    int hasVars = NcmlParserUtil.getNcMLElements("//variable", doc).size();

    System.out.printf("ndims = %s%n", hasDims);
    System.out.printf("natts = %s%n", hasAtts);
    System.out.printf("nvars = %s%n", hasVars);

    //Not really checking the content just the number of elements
    assertEquals(this.ndims, hasDims);
    assertEquals(this.natts, hasAtts);
    assertEquals(this.nvars, hasVars);
   }

  @Test
   public void cdmRemoteRequestHeaderTest() throws Exception {
     RequestBuilder rb = MockMvcRequestBuilders.get(path).servletPath(path)
   				.param("req", "header");

     MvcResult result = this.mockMvc.perform( rb )
               .andExpect(MockMvcResultMatchers.status().is(200))
               .andExpect(MockMvcResultMatchers.content().contentType(ContentType.binary.getContentHeader()))
               .andReturn();

        //response is a ncstream
    ByteArrayInputStream bais = new ByteArrayInputStream(result.getResponse().getContentAsByteArray());
    CdmRemote cdmr = new CdmRemote(bais, "test");
    System.out.printf("%s%n", cdmr);
    cdmr.close();
   }

  @Test
   public void cdmRemoteRequestDataTest() throws Exception {
     RequestBuilder rb = MockMvcRequestBuilders.get(path).servletPath(path)
   				.param("req", "data")
   				.param("var", dataReq);

     MvcResult result = this.mockMvc.perform( rb )
               .andExpect(MockMvcResultMatchers.status().is(200))
               .andExpect(MockMvcResultMatchers.content().contentType(ContentType.binary.toString()))
               .andReturn();

    ByteArrayInputStream bais = new ByteArrayInputStream( result.getResponse().getContentAsByteArray() );
    assertTrue( NcStream.readAndTest(bais, NcStream.MAGIC_DATA));
   }


  private boolean checkBytes(byte[] read, byte[] expected) {

    if (read.length != expected.length) return false;
    int count = 0;
    while (read[count] == expected[count] && count < expected.length - 1) {
      count++;
    }

    return count == expected.length - 1;
  }

}
