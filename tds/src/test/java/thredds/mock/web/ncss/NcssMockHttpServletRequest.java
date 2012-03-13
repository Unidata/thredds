package thredds.mock.web.ncss;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * 
 * Builds query strings for the ncss test request
 * 
 * @author mhermida
 *
 */
public class NcssMockHttpServletRequest {

	//private StringBuilder sb;
	private MockHttpServletRequest request;
	
	private NcssMockHttpServletRequest( MockHttpServletRequest request){
		this.request = request;
	}
	
	public static NcssMockHttpServletRequestBuilder createBuilder(){
		return new NcssMockHttpServletRequestBuilder();
	}
	
	public MockHttpServletRequest getRequest(){
		return this.request;
	}
	


	public static class NcssMockHttpServletRequestBuilder{
		
		private final NcssMockHttpServletRequest ncssQueryString= new NcssMockHttpServletRequest( new MockHttpServletRequest());
		
		private boolean done;
		
		private NcssMockHttpServletRequestBuilder(){}
		
		public NcssMockHttpServletRequest build(){
			
				done = true;			
				List<String> attNames = Collections.list(ncssQueryString.getRequest().getParameterNames() );
				Iterator<String> it = attNames.iterator();
				StringBuilder sb = new StringBuilder();
				while( it.hasNext() ){			
					String paramName = it.next();
					sb.append( paramName+"="+ncssQueryString.getRequest().getParameter(paramName) );
					if( it.hasNext() ) sb.append("&");				
				}
				
				ncssQueryString.getRequest().setQueryString(sb.toString() );										
				return ncssQueryString;
		}
		
		
		public NcssMockHttpServletRequestBuilder setRequestMethod(String method){
			check();
			ncssQueryString.getRequest().setMethod(method);
			return this;			
		}		
		
		public NcssMockHttpServletRequestBuilder setRequestURI(String uri){
			check();
			ncssQueryString.getRequest().setRequestURI(uri);
			return this;			
		}
		
		public NcssMockHttpServletRequestBuilder setContextPath(String contextPath){
			check();
			ncssQueryString.getRequest().setContextPath(contextPath);
			return this;
		}
		
		public NcssMockHttpServletRequestBuilder setPathInfo(String pathInfo){
			check();
			ncssQueryString.getRequest().setPathInfo(pathInfo);
			return this;
		}
		
		/*
		 * bunch of methods to set parameters in the mock request.
		 * We don't care if they are valid values or not, servlet or controller will validate them 
		 *
		 */
		public NcssMockHttpServletRequestBuilder setVar(String vars){
			check();
			ncssQueryString.getRequest().setParameter("var", vars);
			return this;
		}
		
		public NcssMockHttpServletRequestBuilder setLatitude(String latitude){
			check();
			ncssQueryString.getRequest().setParameter("latitude", latitude);
			return this;
		}		
		
		public NcssMockHttpServletRequestBuilder setLongitude(String longitude){
			check();
			ncssQueryString.getRequest().setParameter("longitude", longitude);
			return this;
		}
		
		public NcssMockHttpServletRequestBuilder setTimeStart(String time_start){
			check();
			ncssQueryString.getRequest().setParameter("time_start", time_start);
			return this;
		}		
		
		public NcssMockHttpServletRequestBuilder setTimeEnd(String time_end){
			check();
			ncssQueryString.getRequest().setParameter("time_end", time_end);
			return this;
		}		
		
		public NcssMockHttpServletRequestBuilder setTemporal(String temporal){
			check();
			ncssQueryString.getRequest().setParameter("temporal", temporal);
			return this;
		}		
		
		public NcssMockHttpServletRequestBuilder setTime(String time){
			check();
			ncssQueryString.getRequest().setParameter("time", time);
			return this;
		}
		
		public NcssMockHttpServletRequestBuilder setTimeDuration(String time_duration){
			check();
			ncssQueryString.getRequest().setParameter("time_duration", time_duration);
			return this;
		}		
		
		public NcssMockHttpServletRequestBuilder setVertCoord(String vertCoord){
			check();
			ncssQueryString.getRequest().setParameter("vertCoord", vertCoord);
			return this;
		}
		
		public NcssMockHttpServletRequestBuilder setAccept(String accept){
			check();
			ncssQueryString.getRequest().setParameter("accept", accept);
			return this;
		}		
		
		public NcssMockHttpServletRequestBuilder setPoint(String point){
			check();
			ncssQueryString.getRequest().setParameter("point", point);
			return this;
		}
		
		private void check(){
			if(done) 
				throw new IllegalStateException("Response object was already built");
		}
	}
	
	
	
}
