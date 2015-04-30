package ucar.httpservices;

import java.io.IOException;
import java.util.List;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

public class HTTPCloseableSession {
	
	private static CookieStore cookieStore = new BasicCookieStore();
	private static HttpContext httpContext = new BasicHttpContext();
	private static CredentialsProvider provider = new BasicCredentialsProvider();
	private static CloseableHttpClient closeableHttpClient = HttpClients.custom().setDefaultCredentialsProvider(provider).build();
	private static AuthScope authScope = AuthScope.ANY;
	
	
	public static void setCredentials(String username, String password){
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
		provider.setCredentials(authScope, credentials);
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
	}
	
	public static CloseableHttpResponse execute(HttpRequestBase request) throws ClientProtocolException, IOException{
		log.debug("Connecting to " + request.getURI() + " Cookies: " + cookieStore.getCookies());
		return closeableHttpClient.execute(request, httpContext);
	}
	
	public static void closeClient() throws IOException{
		closeableHttpClient.close();
	}
	
	public static void addCookie(Cookie cookie){
		cookieStore.addCookie(cookie);
	}
	
	public static List<Cookie> getStoredCookies(){
		return cookieStore.getCookies();
	}
	
    public static AuthScope getAuthScope() {
		return authScope;
	}



	static public org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HTTPCloseableSession.class);
}
