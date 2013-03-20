package thredds.server.cdmremote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;

import thredds.mock.web.MockTdsContextLoader;
import thredds.mock.web.TdsContentRootPath;
import thredds.test.util.xml.NcmlParserUtil;
import thredds.test.util.xml.XmlUtil;
import ucar.nc2.NetcdfFile;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamIosp;
import ucar.unidata.io.InMemoryRandomAccessFile;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml","/WEB-INF/cdmRemote-servlet.xml" }, loader=MockTdsContextLoader.class)
public class CdmRemoteControllerTest {
	
	@Autowired
	private CdmRemoteController cdmRemoteController;
	
	
	private MockHttpServletRequest request; 
	
	@Before
	public void setUp(){

		//Same dataset for all tests ???
		request = new MockHttpServletRequest("GET", "/thredds/cdmremote/NCOF/POLCOMS/IRISH_SEA/files/20060925_0600.nc");
		request.setServletPath("/thredds/cdmremote");
		request.setPathInfo("/NCOF/POLCOMS/IRISH_SEA/files/20060925_0600.nc");		
		
	}
	
	@Test
	public void cdmRemoteCapabilitiesRequestTest() throws Exception{
		
		request.setQueryString("req=capabilities");
		request.setParameter("req", "capabilities");
        MockHttpServletResponse response = new MockHttpServletResponse();
		
        ModelAndView mv =cdmRemoteController.handleRequest(request, response);        
        assertNull(mv);

        assertEquals(
        		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"+
        		"<cdmRemoteCapabilities location=\"http://localhost:80/thredds/cdmremote/NCOF/POLCOMS/IRISH_SEA/files/20060925_0600.nc\">\r\n"+
        		"  <featureDataset type=\"GRID\" url=\"http://localhost:80/thredds/cdmremote/NCOF/POLCOMS/IRISH_SEA/files/20060925_0600.nc\" />\r\n"+
        		"</cdmRemoteCapabilities>\r\n\r\n", 
        		response.getContentAsString());        				
	}
	
	@Test
	public void cdmRemoteCDLRequestTest() throws Exception{

		request.setQueryString("req=cdl");
		request.setParameter("req", "cdl");
		
        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView mv =cdmRemoteController.handleRequest(request, response);        
        assertNull(mv);
        assertEquals( "text/plain", response.getContentType() );        
        //Parse CDL and check ??
        
	}
	
	@Test
	public void cdmRemoteNcMLRequestTest() throws Exception{

		request.setQueryString("req=NcML");
		request.setParameter("req", "NcML");
		
        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView mv =cdmRemoteController.handleRequest(request, response);        
        assertNull(mv);
        assertEquals( 200, response.getStatus() );
        assertEquals( "application/xml", response.getContentType() );

        Document doc = XmlUtil.getStringResponseAsDoc(response);
        
        //Not really checking the content just the number of elements
        assertEquals( 5 ,  NcmlParserUtil.getNcMLElements("netcdf/dimension", doc).size());
        assertEquals( 12,  NcmlParserUtil.getNcMLElements("netcdf/attribute", doc).size());
        assertEquals( 16,  NcmlParserUtil.getNcMLElements("//variable", doc).size() );

	}	
	
	@Test
	public void cdmRemoteDataRequestTest() throws Exception{

		request.setQueryString("req=data&var=Precipitable_water(0:1,43:53,20:40)");
		request.setParameter("req", "data");
		request.setParameter("var", "Precipitable_water(0:1,43:53,20:40)");
		
        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView mv =cdmRemoteController.handleRequest(request, response);        
        assertNull(mv);
        assertEquals(200 , response.getStatus() );
        assertEquals("application/octet-stream" , response.getContentType() );
        
        //response is a ncstream??...open with??         
        //ByteArrayInputStream bais = new ByteArrayInputStream( response.getContentAsByteArray() );
        //assertTrue( NcStream.readAndTest(bais, NcStream.MAGIC_DATA ));
        
        //NetcdfFile in memory...
        byte[] content = response.getContentAsByteArray();
        //NetcdfFile nf = NetcdfFile.openInMemory("test_data", content );
        
        InMemoryRandomAccessFile raf = new InMemoryRandomAccessFile("test_data.ncs", content);
        NcStreamIosp ncsiosp = new NcStreamIosp();        
        ncsiosp.open(raf, null, null);
        
        //NetcdfDataset nfd = new NetcdfDataset(nf); 
        
        
        // just checking if there is some more magic in the stream...
        /*        
        int nBytes =0;   
        byte[] tmp = new byte[ 4 ];       
        boolean isMagicStart=false; 
        boolean isMagicHeader=false;
        boolean isMagicData=false;
        boolean isMagicVData=false;
        boolean isMagicVEnd=false;
        boolean isMagicErr=false; 
        boolean isMagicEnd =false;
        
        while( nBytes !=-1 ){
        	
        	nBytes = bais.read(tmp, 0, 4);
        	if( checkBytes(tmp, NcStream.MAGIC_START) )   isMagicStart=true;
        	if( checkBytes(tmp, NcStream.MAGIC_HEADER) )  isMagicHeader=true;
        	if( checkBytes(tmp, NcStream.MAGIC_DATA) )    isMagicData=true;
        	if( checkBytes(tmp, NcStream.MAGIC_VDATA) )   isMagicVData=true;
        	if( checkBytes(tmp, NcStream.MAGIC_VEND) )    isMagicVEnd=true;
        	if( checkBytes(tmp, NcStream.MAGIC_ERR) )     isMagicErr=true;
        	if( checkBytes(tmp, NcStream.MAGIC_END) )     isMagicEnd=true;
        	        	
        }*/
        
        fail("Not yet implemented");
	}
	
	
	@Test
	public void cdmRemoteHeaderRequestTest() throws Exception{
		
		request.setQueryString("req=header");
		request.setParameter("req", "header");		
		
        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView mv =cdmRemoteController.handleRequest(request, response);        
        assertNull(mv);
        assertEquals(200 , response.getStatus() );
        assertEquals("application/octet-stream" , response.getContentType() );		
		
        //response is a ncstream??...open with??         
        ByteArrayInputStream bais = new ByteArrayInputStream( response.getContentAsByteArray() );
        assertTrue( NcStream.readAndTest(bais, NcStream.MAGIC_HEADER  ));
        
        NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", response.getContentAsByteArray() );
        
        fail("Not yet implemented");
	}
	
	@Test
	public void cdmrFeatureRequestTest(){
		
		fail("Not yet implemented");
	}
	
	
	private boolean checkBytes(byte[] read, byte[] expected){
		
		if( read.length != expected.length ) return false;
		int count =0;		
		while( read[count]== expected[count] && count < expected.length-1 ){
			count++;
		}
				
		return count == expected.length-1;
	}

}
