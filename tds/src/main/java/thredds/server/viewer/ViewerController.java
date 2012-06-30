package thredds.server.viewer;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import thredds.server.viewer.dataservice.ViewerService;
import thredds.servlet.ServletUtil;
import ucar.unidata.util.StringUtil2;

@Controller
@RequestMapping("/view/*")
public class ViewerController {
	
	private static Logger log = LoggerFactory.getLogger( ViewerController.class );
	
	
	@Autowired
	ViewerService viewerService;
	
	@RequestMapping(value="{viewer}.jnlp", method=RequestMethod.GET)
	public void launchViewer(@Valid ViewerRequestParamsBean params, BindingResult result, HttpServletResponse res, HttpServletRequest req) throws IOException{
			
		if(result.hasErrors()){
			res.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		params.setViewer( params.getViewer()+".jnlp" ); //??
		
		//Check paths
		String template = viewerService.getViewerTemplate(ServletUtil.getRootPath() + "/WEB-INF/views/" +params.getViewer());
			   
	    if (template == null)	    
	    	template = viewerService.getViewerTemplate(ServletUtil.getContentPath()+"views/" +params.getViewer());
	    if (template == null) { 	
	      res.sendError(HttpServletResponse.SC_NOT_FOUND);
	      return;
	    }		
	
	    String strResp = fillTemplate(req, template );
	    
	    try{
	    	res.setContentType("application/x-java-jnlp-file");    	    	    	
	    	ServletUtil.returnString(strResp , res);
	    }catch (Throwable t) {
	        log.error(" jnlp="+strResp, t);
	        if ( ! res.isCommitted() ) res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	      }	
	
	}
	
	
	
	@SuppressWarnings("unchecked")
	private String fillTemplate(HttpServletRequest req, String template){
		
		StringBuilder sbuff = new StringBuilder( template);
		
        Enumeration<String> params = req.getParameterNames();
        while (params.hasMoreElements()) {
          String name = params.nextElement();
          String values[] = req.getParameterValues(name);
          if (values != null) {
            String sname = "{"+name+"}";
            for (String value : values) {
              StringUtil2.substitute(sbuff, sname, value); // multiple ok
            }
          }
        }		
		
        return sbuff.toString();
	}
}
