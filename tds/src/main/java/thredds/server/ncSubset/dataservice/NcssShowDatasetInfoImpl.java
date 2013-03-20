package thredds.server.ncSubset.dataservice;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextResource;

import thredds.server.config.FormatsAvailabilityService;
import thredds.server.ncSubset.format.SupportedFormat;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridDatasetInfo;

@Service
public class NcssShowDatasetInfoImpl implements NcssShowDatasetInfo, ServletContextAware{

	static private final Logger log = LoggerFactory.getLogger(NcssShowDatasetInfoImpl.class);
	
	private ServletContext servletContext;
	
	
	@Override
	public String showForm(GridDataset gds, String datsetUrlPath, boolean wantXml, boolean isPoint){

		boolean isNetcdf4Availalbe = FormatsAvailabilityService.isFormatAvailable(SupportedFormat.NETCDF4); 
		
	    String infoString=null;
	    GridDatasetInfo writer = new GridDatasetInfo(gds, "path");

	    Document datsetDescription =null;
	    
	    if (wantXml) {
	    	
	    	datsetDescription = writer.makeDatasetDescription();
   
	      if( isNetcdf4Availalbe ){	    	  
	    	  addNetcdf4Format(datsetDescription, "/gridDataset");
	      }
	      infoString = writer.writeXML(datsetDescription);

	    } else {
	    	
	    	try {
	    		
	    		InputStream xslt = getXSLT(isPoint ? "/WEB-INF/xsl/ncssGridAsPoint.xsl" : "/WEB-INF/xsl/ncssGrid.xsl");
	    		Document doc = writer.makeGridForm();
	    		
	  	      	if( isNetcdf4Availalbe ){	    	  
	  	      		addNetcdf4Format(doc, "/gridForm");
	  	      	}	    		
	    		
	    		Element root = doc.getRootElement();
	    		root.setAttribute("location", datsetUrlPath);	    			    		
	    		Transformer xslTransformer = TransformerFactory.newInstance().newTransformer(new StreamSource(xslt));
	    		String context= servletContext.getContextPath();
	    		xslTransformer.setParameter("tdsContext", context);
	    		
	    		xslTransformer.setParameter("gridWKT", writer.getDatasetBoundariesWKT());
	    		
	    		JDOMSource in = new JDOMSource(doc);
	    		JDOMResult out = new JDOMResult();
	    		xslTransformer.transform(in, out);  		
	    		Document html =out.getDocument();
	    		
	    		XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
	    		infoString = fmt.outputString(html);
	    		//infoString = fmt.outputString(doc);
	    			    		

	      }catch(IOException ioe){
		        log.error("IO error opening xsl", ioe);
	      }catch (Throwable e) {
	        log.error("ForecastModelRunServlet internal error", e);

	        //if (!res.isCommitted())
	        //  res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "ForecastModelRunServlet internal error");
	        //return;
	      }
	    }
        
	    return infoString;
	    		
	}
	
	private InputStream getXSLT(String xslName) throws IOException{
		ServletContextResource r = new ServletContextResource( servletContext , xslName);  
	    return r.getInputStream();
	}

	
	@Override
	public void setServletContext(ServletContext servletContext) {
		
		this.servletContext = servletContext;
		
	}
	
	private void addNetcdf4Format(Document datasetDescriptionDoc, String rootElementName){
		
	    String xPathForGridElement =rootElementName+"/AcceptList/Grid";
	    addElement(datasetDescriptionDoc, xPathForGridElement, new Element("accept").addContent("netcdf4"));
	    
	    String xPathForGridAsPointElement =rootElementName+"/AcceptList/GridAsPoint";
	    addElement(datasetDescriptionDoc, xPathForGridAsPointElement, new Element("accept").addContent("netcdf4"));	    
	}
	

	private void addElement(Document datasetDescriptionDoc, String xPath, Element element){

		try {
			XPath gridXpath = XPath.newInstance(xPath);
			Element acceptListParent = (Element)gridXpath.selectSingleNode(datasetDescriptionDoc);
			acceptListParent.addContent(element);							
			
		} catch (JDOMException e) {
			//Log the error...
			
		}
	}

}
