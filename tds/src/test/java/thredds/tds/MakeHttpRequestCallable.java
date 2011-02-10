package thredds.tds;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import ucar.nc2.util.IO;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class MakeHttpRequestCallable implements Callable<MakeHttpRequestResult>
{
  private HttpClient httpClient;
  private String reqUrl;

  private MakeHttpRequestResult result;

  MakeHttpRequestCallable( HttpClient httpClient, String reqUrl, int reqNumber ) {
    this.httpClient = httpClient;
    this.reqUrl = reqUrl;
    result = new MakeHttpRequestResult( reqNumber, reqUrl);
  }

  public MakeHttpRequestResult call() throws Exception
  {
    long start = System.nanoTime();

    try
    {
      send();

      long took = System.nanoTime() - start;
      result.setResponseTimeInMilliseconds( took / 1000 / 1000);
    }
    catch ( Throwable t )
    {
      result.setFailed( true);
      result.setFailMessage( t.getMessage());
    }

    return result;
  }

  void send() throws IOException
  {

    HttpMethod method = null;
    try {
      method = new GetMethod( reqUrl );

      method.setFollowRedirects( true );
      result.setStatusCode( httpClient.executeMethod( method ));

      InputStream is = method.getResponseBodyAsStream();
      if ( is != null )
        result.setBytesRead( IO.copy2null( method.getResponseBodyAsStream(), 10 * 1000 )); // read data and throw away
    } finally {
      if ( method != null )
        method.releaseConnection();
    }
  }
}
