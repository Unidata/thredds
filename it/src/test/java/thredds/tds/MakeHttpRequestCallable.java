package thredds.tds;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.AllClientPNames;
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

    HttpGet method = null;
    try {
      method = new HttpGet( reqUrl );

      method.getParams().setParameter(AllClientPNames.HANDLE_REDIRECTS,true);
      HttpResponse response = httpClient.execute(method);
      result.setStatusCode(response.getStatusLine().getStatusCode());

      InputStream is = response.getEntity().getContent();
//versus      InputStream is = method.getResponseBodyAsStream();
      if ( is != null )
        result.setBytesRead( IO.copy2null(is, 10 * 1000 )); // read data and throw away
//versus	result.setBytesRead( IO.copy2null( method.getResponseBodyAsStream(), 10 * 1000 )); // read data and throw away
    } finally {
      if ( method != null )
        method.releaseConnection();
    }
  }
}
