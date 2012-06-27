package thredds.server.ncSubset.dataservice;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.jdom.transform.XSLTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextResource;

import thredds.servlet.UsageLog;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridDatasetInfo;

@Service
public class NcssShowDatasetInfoImpl implements NcssShowDatasetInfo, ServletContextAware{

	static private final Logger log = LoggerFactory.getLogger(NcssShowDatasetInfoImpl.class);
	
	private ServletContext servletContext;
	
	
	@Override
	public String showForm(GridDataset gds, String datsetUrlPath, boolean wantXml, boolean isPoint){

	    String infoString=null;
	    GridDatasetInfo writer = new GridDatasetInfo(gds, "path");

	    if (wantXml) {
	      infoString = writer.writeXML(writer.makeDatasetDescription());

	    } else {
	    	
	    	try {
	    		
	    		InputStream xslt = getXSLT(isPoint ? "/WEB-INF/xsl/ncssGridAsPoint.xsl" : "/WEB-INF/xsl/ncssGrid.xsl");
	    		Document doc = writer.makeGridForm();
	    		
	    		Element root = doc.getRootElement();
	    		root.setAttribute("location", datsetUrlPath);	    			    		
	    		Transformer xslTransformer = TransformerFactory.newInstance().newTransformer(new StreamSource(xslt));
	    		String context= servletContext.getContextPath();
	    		xslTransformer.setParameter("tdsContext", context);
	    		JDOMSource in = new JDOMSource(doc);
	    		JDOMResult out = new JDOMResult();
	    		xslTransformer.transform(in, out);  		
	    		Document html =out.getDocument();
	    		
	    		XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
	    		infoString = fmt.outputString(html);


	      }catch(IOException ioe){
		        log.error("IO error opening xsl", ioe);
		        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));	    	  
	      }catch (Throwable e) {
	        log.error("ForecastModelRunServlet internal error", e);
	        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
	        
	        //if (!res.isCommitted())
	        //  res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "ForecastModelRunServlet internal error");
	        //return;
	      }
	    }
        
	    return infoString;
	    
	    //res.setContentLength(infoString.length());
	    //if (wantXml)
	    //  res.setContentType("text/xml; charset=iso-8859-1");
	    //else
	    //  res.setContentType("text/html; charset=iso-8859-1");

	    //OutputStream out = res.getOutputStream();
	    //out.write(infoString.getBytes());
	    //out.flush();

	    //log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, infoString.length()));
		
		
	}
	
	private InputStream getXSLT(String xslName) throws IOException{
		ServletContextResource r = new ServletContextResource( servletContext , xslName);  
	    return r.getInputStream();
	}

	
	@Override
	public void setServletContext(ServletContext servletContext) {
		
		this.servletContext = servletContext;
		
	}	

}
