package thredds.tds;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.junit.Test;
import ucar.nc2.util.IO;
import ucar.nc2.util.net.HttpClientManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class PoundTdsWmsTest
{
  @Test
  public void hitLocalTdsWms() throws IOException
  {
    String curUrl;
    long curUrlResponseSize;
    for ( int i=0; i < timeStrings.length; i++) {
      curUrl = baseUrl + timeStrings[ i];
      curUrlResponseSize = IO.copyUrlB( curUrl, null, 8000);
      System.out.println( "[" + i + "] " + timeStrings[i] + ": " + curUrlResponseSize );
    }
  }

  @Test
  public void hitMl8081TdsWms() throws IOException
  {
    String curUrl;
    long curUrlResponseSize;
    for ( int i=0; i < ml8081GfsHalfDegreeBestWmsTimeStrings.length; i++) {
      curUrl = ml8081GfsHalfDegreeBestWmsGetMapBaseUrl + ml8081GfsHalfDegreeBestWmsTimeStrings[ i];
      curUrlResponseSize = IO.copyUrlB( curUrl, null, 8000);
      System.out.println( "[" + i + "] " + ml8081GfsHalfDegreeBestWmsTimeStrings[i] + ": " + curUrlResponseSize );
    }
  }

  @Test
  public void hitLocalTdsWms_MultiThreaded()
          throws IOException,
                 InterruptedException,
                 ExecutionException
  {
    final int numThreads = 60;
    final int numToRepeat = 5;
    int timeout = 20 * 1000;
    final String httpUserAgentName = "PoundTdsWmsTest_hitLocalTdsWms";
    String wmsGetMapBaseUrl = baseUrl;
    String[] timeSeriesStrings = timeStrings;

    executeAndLogRequests( numThreads, numToRepeat, timeout, httpUserAgentName, wmsGetMapBaseUrl, timeSeriesStrings );
  }

  @Test
  public void hitMl8081TdsWms_MultiThreaded()
          throws IOException,
                 InterruptedException,
                 ExecutionException
  {
    final int numThreads = 60;
    final int numToRepeat = 5;
    int timeout = 20 * 1000;
    final String httpUserAgentName = "PoundTdsWmsTest_hitMl8081";
    String wmsGetMapBaseUrl = ml8081GfsHalfDegreeBestWmsGetMapBaseUrl;
    String[] timeSeriesStrings = ml8081GfsHalfDegreeBestWmsTimeStrings;

    executeAndLogRequests( numThreads, numToRepeat, timeout, httpUserAgentName, wmsGetMapBaseUrl, timeSeriesStrings );
  }

  private void executeAndLogRequests( int numThreads, int numToRepeat, int timeout, String httpUserAgentName, String wmsGetMapBaseUrl, String[] timeSeriesStrings )
          throws InterruptedException, ExecutionException
  {
//    HttpClient httpClient = HttpClientManager.init( null, httpUserAgentName );
//    MultiThreadedHttpConnectionManager cm = (MultiThreadedHttpConnectionManager) httpClient.getHttpConnectionManager();
//    HttpConnectionManagerParams cmParams = cm.getParams();
//    cmParams.setMaxConnectionsPerHost( null, numThreads );
//    cmParams.setConnectionTimeout( timeout );
//    cmParams.setSoTimeout( timeout );
//
//    ExecutorService executor = Executors.newFixedThreadPool( numThreads );
//    ExecutorCompletionService<MakeHttpRequestResult> completionService
//            = new ExecutorCompletionService<MakeHttpRequestResult>( executor );
//
//    List<Future<MakeHttpRequestResult>> futures = new ArrayList<Future<MakeHttpRequestResult>>();
//    Future<MakeHttpRequestResult> curFuture = null;
//
//    int numRequests = executeRequests( wmsGetMapBaseUrl, timeSeriesStrings, numToRepeat, httpClient, completionService, futures );

    boolean huh = false;
    boolean cancel = false;
    int numCancelled = 0;
    MakeHttpRequestResult curResult = null;
//    while ( ! futures.isEmpty() ) {
//      if ( huh )
//        curFuture = completionService.take();
//      else {
//        curFuture = completionService.poll();
//        if ( curFuture == null ) {
//          if ( cancel ) break;
//          curFuture = completionService.take();
//        }
//      }
//
//
//      if ( ! curFuture.isCancelled() ) {
//        curResult = curFuture.get();
//        System.out.println( curResult.toString() );
//      } else {
//        System.out.println( "CANCELLED");
//        numCancelled++;
//      }
//
//      if ( ! futures.remove( curFuture )) System.out.println( "Future not in list." );
//    }
//    System.out.println( "Number of Requests     : " + numRequests );
    System.out.println( "Number of Cancellations: " + numCancelled );
  }

  private int executeRequests( String baseUrl, String[] timeSeriesStrings, int timesToRepeatTimeSeriesRequests,
                               HttpClient httpClient,
                               ExecutorCompletionService<MakeHttpRequestResult> completionService,
                               List<Future<MakeHttpRequestResult>> futures )
  {
    Future<MakeHttpRequestResult> curFuture;
    String curUrl;
    int numRequests = 0;
    for ( int j=0; j < timesToRepeatTimeSeriesRequests; j++ )
    {
      for ( int i=0; i < timeSeriesStrings.length; i++) {
        curUrl = baseUrl + timeSeriesStrings[ i];
        curFuture = completionService.submit( new MakeHttpRequestCallable( httpClient, curUrl, i + j*timeSeriesStrings.length ) );
        numRequests++;
        futures.add( curFuture);
      }
    }
    return numRequests;
  }

  private static String baseUrl = "http://localhost:8080/thredds/wms/ncWmsPixelMapProblem/GFS_Global_0p5deg_20101026_0000.grib2?service=WMS&version=1.3.0&REQUEST=GetMap&CRS=EPSG:4326&BBOX=-180,-90,180,90&WIDTH=1000&HEIGHT=500&FORMAT=image/png&LAYERS=Precipitable_water&STYLES=boxfill/redblue&COLORSCALERANGE=0,100&TIME=";
  private static String[] timeStrings = new String[]
          {
                  "2010-10-26T00:00:00.000Z",
                  "2010-10-26T03:00:00.000Z",
                  "2010-10-26T06:00:00.000Z",
                  "2010-10-26T09:00:00.000Z",
                  "2010-10-26T12:00:00.000Z",
                  "2010-10-26T15:00:00.000Z",
                  "2010-10-26T18:00:00.000Z",
                  "2010-10-26T21:00:00.000Z",
                  "2010-10-27T00:00:00.000Z",
                  "2010-10-27T03:00:00.000Z",
                  "2010-10-27T06:00:00.000Z",
                  "2010-10-27T09:00:00.000Z",
                  "2010-10-27T12:00:00.000Z",
                  "2010-10-27T15:00:00.000Z",
                  "2010-10-27T18:00:00.000Z",
                  "2010-10-27T21:00:00.000Z",
                  "2010-10-28T00:00:00.000Z",
                  "2010-10-28T03:00:00.000Z",
                  "2010-10-28T06:00:00.000Z",
                  "2010-10-28T09:00:00.000Z",
                  "2010-10-28T12:00:00.000Z",
                  "2010-10-28T15:00:00.000Z",
                  "2010-10-28T18:00:00.000Z",
                  "2010-10-28T21:00:00.000Z",
                  "2010-10-29T00:00:00.000Z",
                  "2010-10-29T03:00:00.000Z",
                  "2010-10-29T06:00:00.000Z",
                  "2010-10-29T09:00:00.000Z",
                  "2010-10-29T12:00:00.000Z",
                  "2010-10-29T15:00:00.000Z",
                  "2010-10-29T18:00:00.000Z",
                  "2010-10-29T21:00:00.000Z",
                  "2010-10-30T00:00:00.000Z",
                  "2010-10-30T03:00:00.000Z",
                  "2010-10-30T06:00:00.000Z",
                  "2010-10-30T09:00:00.000Z",
                  "2010-10-30T12:00:00.000Z",
                  "2010-10-30T15:00:00.000Z",
                  "2010-10-30T18:00:00.000Z",
                  "2010-10-30T21:00:00.000Z",
                  "2010-10-31T00:00:00.000Z",
                  "2010-10-31T03:00:00.000Z",
                  "2010-10-31T06:00:00.000Z",
                  "2010-10-31T09:00:00.000Z",
                  "2010-10-31T12:00:00.000Z",
                  "2010-10-31T15:00:00.000Z",
                  "2010-10-31T18:00:00.000Z",
                  "2010-10-31T21:00:00.000Z",
                  "2010-11-01T00:00:00.000Z",
                  "2010-11-01T03:00:00.000Z",
                  "2010-11-01T06:00:00.000Z",
                  "2010-11-01T09:00:00.000Z",
                  "2010-11-01T12:00:00.000Z",
                  "2010-11-01T15:00:00.000Z",
                  "2010-11-01T18:00:00.000Z",
                  "2010-11-01T21:00:00.000Z",
                  "2010-11-02T00:00:00.000Z",
                  "2010-11-02T03:00:00.000Z",
                  "2010-11-02T06:00:00.000Z",
                  "2010-11-02T09:00:00.000Z",
                  "2010-11-02T12:00:00.000Z",
                  "2010-11-02T15:00:00.000Z",
                  "2010-11-02T18:00:00.000Z",
                  "2010-11-02T21:00:00.000Z",
                  "2010-11-03T00:00:00.000Z"
          };
  private static String ml8081GfsHalfDegreeBestWmsGetCapUrl = "http://motherlode.ucar.edu:8081/thredds/wms/fmrc/NCEP/GFS/Global_0p5deg/NCEP-GFS-Global_0p5deg_best.ncd?service=WMS&version=1.3.0&request=GetCapabilities";
  private static String ml8081GfsHalfDegreeBestWmsGetMapBaseUrl = "http://motherlode.ucar.edu:8081/thredds/wms/fmrc/NCEP/GFS/Global_0p5deg/NCEP-GFS-Global_0p5deg_best.ncd?service=WMS&version=1.3.0&request=GetMap&TRANSPARENT=true&STYLES=boxfill%2Frainbow&CRS=EPSG%3A4326&COLORSCALERANGE=0.2%2C62.9&NUMCOLORBANDS=20&LOGSCALE=false&EXCEPTIONS=XML&FORMAT=image%2Fpng&BBOX=-180,-90,180,90&WIDTH=256&HEIGHT=256&LAYERS=Precipitable_water&ELEVATION=0&TIME=";
  private static String[] ml8081GfsHalfDegreeBestWmsTimeStrings = new String[]
          {
                  "2010-11-06T00:00:00.000Z",
                  "2010-11-06T03:00:00.000Z",
                  "2010-11-06T06:00:00.000Z",
                  "2010-11-06T09:00:00.000Z",
                  "2010-11-06T12:00:00.000Z",
                  "2010-11-06T15:00:00.000Z",
                  "2010-11-06T18:00:00.000Z",
                  "2010-11-06T21:00:00.000Z",
                  "2010-11-07T00:00:00.000Z",
                  "2010-11-07T03:00:00.000Z",
                  "2010-11-07T06:00:00.000Z",
                  "2010-11-07T09:00:00.000Z",
                  "2010-11-07T12:00:00.000Z",
                  "2010-11-07T15:00:00.000Z",
                  "2010-11-07T18:00:00.000Z",
                  "2010-11-07T21:00:00.000Z",
                  "2010-11-08T00:00:00.000Z",
                  "2010-11-08T03:00:00.000Z",
                  "2010-11-08T06:00:00.000Z",
                  "2010-11-08T09:00:00.000Z",
                  "2010-11-08T12:00:00.000Z",
                  "2010-11-08T15:00:00.000Z",
                  "2010-11-08T18:00:00.000Z",
                  "2010-11-08T21:00:00.000Z",
                  "2010-11-09T00:00:00.000Z",
                  "2010-11-09T03:00:00.000Z",
                  "2010-11-09T06:00:00.000Z",
                  "2010-11-09T09:00:00.000Z",
                  "2010-11-09T12:00:00.000Z",
                  "2010-11-09T15:00:00.000Z",
                  "2010-11-09T18:00:00.000Z",
                  "2010-11-09T21:00:00.000Z",
                  "2010-11-10T00:00:00.000Z",
                  "2010-11-10T03:00:00.000Z",
                  "2010-11-10T06:00:00.000Z",
                  "2010-11-10T09:00:00.000Z",
                  "2010-11-10T12:00:00.000Z",
                  "2010-11-10T15:00:00.000Z",
                  "2010-11-10T18:00:00.000Z",
                  "2010-11-10T21:00:00.000Z",
                  "2010-11-11T00:00:00.000Z",
                  "2010-11-11T03:00:00.000Z",
                  "2010-11-11T06:00:00.000Z",
                  "2010-11-11T09:00:00.000Z",
                  "2010-11-11T12:00:00.000Z",
                  "2010-11-11T15:00:00.000Z",
                  "2010-11-11T18:00:00.000Z",
                  "2010-11-11T21:00:00.000Z",
                  "2010-11-12T00:00:00.000Z",
                  "2010-11-12T03:00:00.000Z",
                  "2010-11-12T06:00:00.000Z",
                  "2010-11-12T09:00:00.000Z",
                  "2010-11-12T12:00:00.000Z",
                  "2010-11-12T15:00:00.000Z",
                  "2010-11-12T18:00:00.000Z",
                  "2010-11-12T21:00:00.000Z",
                  "2010-11-13T00:00:00.000Z",
                  "2010-11-13T03:00:00.000Z",
                  "2010-11-13T06:00:00.000Z",
                  "2010-11-13T09:00:00.000Z",
                  "2010-11-13T12:00:00.000Z",
                  "2010-11-13T15:00:00.000Z",
                  "2010-11-13T18:00:00.000Z",
                  "2010-11-13T21:00:00.000Z",
                  "2010-11-14T00:00:00.000Z",
                  "2010-11-14T03:00:00.000Z",
                  "2010-11-14T06:00:00.000Z",
                  "2010-11-14T09:00:00.000Z",
                  "2010-11-14T12:00:00.000Z",
                  "2010-11-14T15:00:00.000Z",
                  "2010-11-14T18:00:00.000Z",
                  "2010-11-14T21:00:00.000Z",
                  "2010-11-15T00:00:00.000Z",
                  "2010-11-15T03:00:00.000Z",
                  "2010-11-15T06:00:00.000Z",
                  "2010-11-15T09:00:00.000Z",
                  "2010-11-15T12:00:00.000Z",
                  "2010-11-15T15:00:00.000Z",
                  "2010-11-15T18:00:00.000Z",
                  "2010-11-15T21:00:00.000Z",
                  "2010-11-16T00:00:00.000Z",
                  "2010-11-16T03:00:00.000Z",
                  "2010-11-16T06:00:00.000Z",
                  "2010-11-16T09:00:00.000Z",
                  "2010-11-16T12:00:00.000Z",
                  "2010-11-16T15:00:00.000Z",
                  "2010-11-16T18:00:00.000Z",
                  "2010-11-16T21:00:00.000Z",
                  "2010-11-17T00:00:00.000Z",
                  "2010-11-17T03:00:00.000Z",
                  "2010-11-17T06:00:00.000Z",
                  "2010-11-17T09:00:00.000Z",
                  "2010-11-17T12:00:00.000Z",
                  "2010-11-17T15:00:00.000Z",
                  "2010-11-17T18:00:00.000Z",
                  "2010-11-17T21:00:00.000Z",
                  "2010-11-18T00:00:00.000Z",
                  "2010-11-18T03:00:00.000Z",
                  "2010-11-18T06:00:00.000Z",
                  "2010-11-18T09:00:00.000Z",
                  "2010-11-18T12:00:00.000Z",
                  "2010-11-18T15:00:00.000Z",
                  "2010-11-18T18:00:00.000Z",
                  "2010-11-18T21:00:00.000Z",
                  "2010-11-19T00:00:00.000Z",
                  "2010-11-19T03:00:00.000Z",
                  "2010-11-19T06:00:00.000Z",
                  "2010-11-19T09:00:00.000Z",
                  "2010-11-19T12:00:00.000Z",
                  "2010-11-19T15:00:00.000Z",
                  "2010-11-19T18:00:00.000Z",
                  "2010-11-19T21:00:00.000Z",
                  "2010-11-20T00:00:00.000Z",
                  "2010-11-20T03:00:00.000Z",
                  "2010-11-20T06:00:00.000Z",
                  "2010-11-20T09:00:00.000Z",
                  "2010-11-20T12:00:00.000Z",
                  "2010-11-20T15:00:00.000Z",
                  "2010-11-20T18:00:00.000Z",
                  "2010-11-20T21:00:00.000Z",
                  "2010-11-21T00:00:00.000Z",
                  "2010-11-21T03:00:00.000Z",
                  "2010-11-21T06:00:00.000Z",
                  "2010-11-21T09:00:00.000Z",
                  "2010-11-21T12:00:00.000Z",
                  "2010-11-21T15:00:00.000Z",
                  "2010-11-21T18:00:00.000Z",
                  "2010-11-21T21:00:00.000Z",
                  "2010-11-22T00:00:00.000Z",
                  "2010-11-22T03:00:00.000Z",
                  "2010-11-22T06:00:00.000Z",
                  "2010-11-22T09:00:00.000Z",
                  "2010-11-22T12:00:00.000Z",
                  "2010-11-22T15:00:00.000Z",
                  "2010-11-22T18:00:00.000Z",
                  "2010-11-22T21:00:00.000Z",
                  "2010-11-23T00:00:00.000Z",
                  "2010-11-23T03:00:00.000Z",
                  "2010-11-23T06:00:00.000Z",
                  "2010-11-23T09:00:00.000Z",
                  "2010-11-23T12:00:00.000Z",
                  "2010-11-23T15:00:00.000Z",
                  "2010-11-23T18:00:00.000Z",
                  "2010-11-23T21:00:00.000Z",
                  "2010-11-24T00:00:00.000Z",
                  "2010-11-24T03:00:00.000Z",
                  "2010-11-24T06:00:00.000Z",
                  "2010-11-24T09:00:00.000Z",
                  "2010-11-24T12:00:00.000Z",
                  "2010-11-24T15:00:00.000Z",
                  "2010-11-24T18:00:00.000Z",
                  "2010-11-24T21:00:00.000Z",
                  "2010-11-25T00:00:00.000Z",
                  "2010-11-25T03:00:00.000Z",
                  "2010-11-25T06:00:00.000Z",
                  "2010-11-25T09:00:00.000Z",
                  "2010-11-25T12:00:00.000Z",
                  "2010-11-25T15:00:00.000Z",
                  "2010-11-25T18:00:00.000Z",
                  "2010-11-25T21:00:00.000Z",
                  "2010-11-26T00:00:00.000Z",
                  "2010-11-26T03:00:00.000Z",
                  "2010-11-26T06:00:00.000Z",
                  "2010-11-26T09:00:00.000Z",
                  "2010-11-26T12:00:00.000Z",
                  "2010-11-26T15:00:00.000Z",
                  "2010-11-26T18:00:00.000Z",
                  "2010-11-26T21:00:00.000Z",
                  "2010-11-27T00:00:00.000Z",
                  "2010-11-27T03:00:00.000Z",
                  "2010-11-27T06:00:00.000Z",
                  "2010-11-27T09:00:00.000Z",
                  "2010-11-27T12:00:00.000Z",
                  "2010-11-27T15:00:00.000Z",
                  "2010-11-27T18:00:00.000Z",
                  "2010-11-27T21:00:00.000Z",
                  "2010-11-28T00:00:00.000Z",
                  "2010-11-28T03:00:00.000Z",
                  "2010-11-28T06:00:00.000Z",
                  "2010-11-28T09:00:00.000Z",
                  "2010-11-28T12:00:00.000Z",
                  "2010-11-28T15:00:00.000Z",
                  "2010-11-28T18:00:00.000Z",
                  "2010-11-28T21:00:00.000Z",
                  "2010-11-29T00:00:00.000Z",
                  "2010-11-29T03:00:00.000Z",
                  "2010-11-29T06:00:00.000Z",
                  "2010-11-29T09:00:00.000Z",
                  "2010-11-29T12:00:00.000Z",
                  "2010-11-29T15:00:00.000Z",
                  "2010-11-29T18:00:00.000Z",
                  "2010-11-29T21:00:00.000Z",
                  "2010-11-30T00:00:00.000Z",
                  "2010-11-30T03:00:00.000Z",
                  "2010-11-30T06:00:00.000Z",
                  "2010-11-30T09:00:00.000Z",
                  "2010-11-30T12:00:00.000Z",
                  "2010-11-30T15:00:00.000Z",
                  "2010-11-30T18:00:00.000Z",
                  "2010-11-30T21:00:00.000Z",
                  "2010-12-01T00:00:00.000Z",
                  "2010-12-01T03:00:00.000Z",
                  "2010-12-01T06:00:00.000Z",
                  "2010-12-01T09:00:00.000Z",
                  "2010-12-01T12:00:00.000Z",
                  "2010-12-01T15:00:00.000Z",
                  "2010-12-01T18:00:00.000Z",
                  "2010-12-01T21:00:00.000Z"
          };
  public static class TimeSeriesModelWmsAccessUrl
  {
    private String wmsGetMapBaseUrl;
    private String[] wmsTimeSeries;
    private String[] wmsGetMapUrls;
    public TimeSeriesModelWmsAccessUrl( String wmsGetMapBaseUrl, int numDaysBack, int numDays, int[] dailyRunTimes)
    {
      wmsGetMapUrls = new String[ numDays * dailyRunTimes.length];
      
    }

    public String[] getWmsGetMapUrls() {
      return wmsGetMapUrls;
    }
  }
}
