package thredds.wcs;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface WcsRequest
{
  public Version getVersion();
  public Operation getOperation();
  public RequestEncoding getRequestEncoding();

  public enum Operation
  {
    GetCapabilities, DescribeCoverage, GetCoverage
  }

  public enum RequestEncoding
  {
    GET_KVP, POST_XML, POST_SOAP
  }

  public enum Format
  {
    NONE( "" ),
    GeoTIFF( "image/tiff" ),
    GeoTIFF_Float( "image/tiff" ),
    NetCDF3( "application/x-netcdf" );

    private String mimeType;

    Format( String mimeType ) { this.mimeType = mimeType; }

    public String getMimeType() { return mimeType; }

    public static Format getFormat( String mimeType )
    {
      for ( Format curSection : Format.values() )
      {
        if ( curSection.mimeType.equals( mimeType ) )
          return curSection;
      }
      throw new IllegalArgumentException( "No such instance <" + mimeType + ">." );
    }
  }
}
