package thredds.server.services;

import static com.eclipsesource.restfuse.Assert.assertOk;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import com.eclipsesource.restfuse.Destination;
import com.eclipsesource.restfuse.HttpJUnitRunner;
import com.eclipsesource.restfuse.Method;
import com.eclipsesource.restfuse.Response;
import com.eclipsesource.restfuse.annotation.Context;
import com.eclipsesource.restfuse.annotation.HttpTest;

@RunWith( HttpJUnitRunner.class )
public class ConsistentDatesTest {
	
	  @Rule
	  public Destination destination = new Destination( "http://localhost:8081" ); 

	  @Context
	  private Response response; // will be injected after every request
	    
	  private final DateTime[] expectedDateTime ={
			  new DateTime("0000-01-16T06:00:00Z"),
			  new DateTime("0000-02-15T16:29:05.999Z"),
			  new DateTime("0000-03-17T02:58:12Z"),
			  new DateTime("0000-04-16T13:27:18Z"),
              new DateTime("0000-05-16T23:56:24Z"),
              new DateTime("0000-06-16T10:25:30Z"),
              new DateTime("0000-07-16T20:54:36Z"),
              new DateTime("0000-08-16T07:23:42Z"),
              new DateTime("0000-09-15T17:52:48Z"),
              new DateTime("0000-10-16T04:21:53.999Z"),
              new DateTime("0000-11-15T14:51:00Z"),
              new DateTime("0000-12-16T01:20:06Z")};	  
	  
	  private final List<DateTime> expectedDatesAsDateTime = Collections.unmodifiableList(Arrays.asList(expectedDateTime));
	  
	  @Before
	  public void setUp(){
		  
	  }
	  
	  @HttpTest( method = Method.GET, path = "/thredds/wms/testStandardTds/climatology/PF5_SST_Climatology_Monthly_1985_2001.nc?service=WMS&version=1.3.0&request=GetCapabilities" )
	  public void checkWMSDates() throws JDOMException, IOException{
		  
			assertOk( response );
			assertTrue(response.hasBody());
			String xml = response.getBody(String.class);
			Reader in = new StringReader(xml);		
			SAXBuilder sb=new SAXBuilder();
			Document doc=sb.build(in);		
			XPath xPath = XPath.newInstance("//wms:Dimension");
			xPath.addNamespace("wms", doc.getRootElement().getNamespaceURI());
            Element dimNode = (Element)xPath.selectSingleNode(doc);
            //List<String> content = Arrays.asList(dimNode.getText().trim().split(","));
            List<DateTime> content = new ArrayList<DateTime>();
            for(String d : Arrays.asList(dimNode.getText().trim().split(","))){
            	content.add(new DateTime(d));
            }
            
            assertTrue( content.equals(expectedDatesAsDateTime) );	  
		  
	  }

	  @HttpTest( method = Method.GET, path = "/thredds/wcs/testStandardTds/climatology/PF5_SST_Climatology_Monthly_1985_2001.nc?service=WCS&version=1.0.0&request=DescribeCoverage&coverage=sst" )
	  public void checkWCSDates() throws JDOMException, IOException{
			assertOk( response );
			assertTrue(response.hasBody());
			String xml = response.getBody(String.class);
			Reader in = new StringReader(xml);		
			SAXBuilder sb=new SAXBuilder();
			Document doc=sb.build(in);		
			XPath xPath = XPath.newInstance("//wcs:temporalDomain/gml:timePosition");
			xPath.addNamespace("wcs", doc.getRootElement().getNamespaceURI());
			List<Element> timePositionNodes = xPath.selectNodes(doc);
			List<DateTime>  timePositionDateTime = new ArrayList<DateTime>();
			for(Element e : timePositionNodes){
				timePositionDateTime.add(new DateTime( e.getText()));			
			}

			assertTrue( timePositionDateTime.equals(expectedDatesAsDateTime) );
		  
	  }	  
	  
	  @HttpTest( method = Method.GET, path = "thredds/ncss/grid/testStandardTds/climatology/PF5_SST_Climatology_Monthly_1985_2001.nc?var=sst&latitude=45&longitude=-20&temporal=all" )
	  public void checkNCSSDates() throws JDOMException, IOException{
		  
			assertOk( response );
			assertTrue(response.hasBody());
			String xml = response.getBody(String.class);
			Reader in = new StringReader(xml);		
			SAXBuilder sb=new SAXBuilder();
			Document doc=sb.build(in);		
			XPath xPath = XPath.newInstance("/grid/point/data[@name='date']");
			List<Element> dataTimeNodes = xPath.selectNodes(doc);
			List<DateTime>  timePositionDateTime = new ArrayList<DateTime>();
			for(Element e : dataTimeNodes){
				timePositionDateTime.add(new DateTime( e.getText()));			
			}

			assertTrue( timePositionDateTime.equals(expectedDatesAsDateTime) );		  
		  
	  }
	  
}
