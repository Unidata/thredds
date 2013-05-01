package thredds.server.ncSubset.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import thredds.servlet.DatasetHandler;
import thredds.servlet.ThreddsConfig;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dt.GridDataset;

public class NcssInterceptor extends HandlerInterceptorAdapter {

	static private final Logger log = LoggerFactory.getLogger(NcssInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

		//Check allow
		boolean allow = ThreddsConfig.getBoolean("NetcdfSubsetService.allow", false);

		//if( ((HandlerMethod) handler).getBean() instanceof  AbstractNcssController){
		if( handler instanceof  HandlerMethod ){					

			AbstractNcssController hm =  (AbstractNcssController)((HandlerMethod) handler).getBean();

			// pathInfo is the string containing any additional path information, that is, anything following the servlet path and preceding the query string.
			// For Spring, as the DispatcherServlet is mapped into "/" the servletPath is everything preceding the query string so the request.getPathInfo() 
			// returns an empty string.
			// Here we get the pathInfo form the request servletPath and the AbstractNcssController servletPath		
			String servletPath = request.getServletPath();
			String pathInfo = servletPath.substring(AbstractNcssController.getServletPath().length()  , servletPath.length());

			if(pathInfo ==null){
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return false;
			}	    
			
			//hm.setRequestPathInfo( pathInfo );
			
			GridDataset gds = getGridDataset(request, response, hm.extractRequestPathInfo(pathInfo));
			hm.setGridDataset(gds);

			//Check dataset ??		
			return allow && ( gds != null );

		}

		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

		closeGridDataset(handler);


	}

	@Override
	public void afterCompletion(HttpServletRequest request,	HttpServletResponse response, Object handler, Exception ex)
			throws Exception {

		closeGridDataset(handler );

	}


	private GridDataset getGridDataset(HttpServletRequest req, HttpServletResponse res, String pathInfo) throws IOException{
		GridDataset gds = null;

		/*if( pathInfo.endsWith("xml") || pathInfo.endsWith("html") || pathInfo.endsWith("datasetBoundaries")  ){
			pathInfo = pathInfo.trim(); 
			String[] pathInfoArr = pathInfo.split("/");			  
			StringBuilder sb = new StringBuilder();
			int len = pathInfoArr.length;
			sb.append(pathInfoArr[1]);
			for(int i= 2;  i<len-1; i++  ){
				sb.append("/"+pathInfoArr[i]);
			}
			pathInfo = sb.toString();
		}*/

		try {
			
			//Enhance mode for netcdf subset does not use Enhance.ScaleMissing so fill values are not replaced in the files 
			Set<Enhance> enhance = Collections.unmodifiableSet(EnumSet.of(Enhance.CoordSystems, Enhance.ConvertEnums));			 			
			gds = DatasetHandler.openGridDataset(req, res, pathInfo, enhance);
			if (null == gds) {
				res.sendError(HttpServletResponse.SC_NOT_FOUND);
			}

		} catch (java.io.FileNotFoundException ioe) {
			if (!res.isCommitted()) res.sendError(HttpServletResponse.SC_NOT_FOUND);

		} catch (Throwable e) {
			log.error("GridServlet.showForm", e);
			if (!res.isCommitted()) res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

		}

		return gds;
	}

	private void closeGridDataset(Object handler){

		if(handler instanceof HandlerMethod){

			AbstractNcssController hm =  (AbstractNcssController)((HandlerMethod) handler).getBean();				
			GridDataset gds = hm.getGridDataset();

			//Check dataset is closed ??
			if (null != gds){
				try {
					gds.close();
				} catch (IOException ioe) {
					log.error("Failed to close = " + hm.getRequestPathInfo() );
				}
			}		

			//hm.setRequestPathInfo(null);
			//hm.setGridDataset(null);

		}

	}

}
