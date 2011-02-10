package thredds.tds;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class MakeHttpRequestResult
{
  private long requestNumber;
  private String requestUrl;

  private int statusCode;
  private long bytesRead;
  private boolean failed = false;
  private String failMessage;
  private long responseTimeInMilliseconds;

  public MakeHttpRequestResult( long requestNumber, String requestUrl ) {
    this.requestNumber = requestNumber;
    this.requestUrl = requestUrl;
  }

  public long getRequestNumber() {
    return this.requestNumber;
  }

  public String getRequestUrl() {
    return this.requestUrl;
  }

  public int getStatusCode() {
    return this.statusCode;
  }

  public void setStatusCode( int statusCode ) {
    this.statusCode = statusCode;
  }

  public long getBytesRead() {
    return this.bytesRead;
  }
  public void setBytesRead( long bytesRead ) {
    this.bytesRead = bytesRead;
  }

  public boolean isFailed() {
    return this.failed;
  }
  public void setFailed( boolean failed ) {
    this.failed = failed;
  }

  public String getFailMessage() {
    return this.failMessage;
  }
  public void setFailMessage( String failMessage ) {
    this.failMessage = failMessage;
  }

  public long getResponseTimeInMilliseconds() {
    return this.responseTimeInMilliseconds;
  }
  public void setResponseTimeInMilliseconds( long responseTimeInMilliseconds ) {
    this.responseTimeInMilliseconds = responseTimeInMilliseconds;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append( "[").append( requestNumber).append( "] ")
            .append( requestUrl).append( " ");
    if ( failed )
      sb.append( "FAILED - ").append( failMessage);
    else
      sb.append( statusCode).append( " ")
              .append( bytesRead). append( " ")
              .append( responseTimeInMilliseconds);
    
    return sb.toString();
  }
}
