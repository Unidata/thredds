package thredds.wcs;

import ucar.nc2.dt.GridDataset;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface Request
{
  public String getVersionString();
  public Operation getOperation();
  public RequestEncoding getRequestEncoding();
  public GridDataset getDataset();
  public String getDatasetPath();

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

  /**
   * Represent a bounding box in some CRS.
   */
  public class BoundingBox
  {
    private int dimensionLength;
    private double[] minPoint;
    private double[] maxPoint;
    private String minPointString;
    private String maxPointString;

    public BoundingBox( double[] minPoint, double[] maxPoint)
    {
      if ( minPoint.length != maxPoint.length )
        throw new IllegalArgumentException( "The dimension length of the minimum point [" + minPoint.length + "] and maximum point [" + maxPoint.length + "] must be equal.");

      boolean minMaxOk = true;
      StringBuilder minPSB = new StringBuilder("[");
      StringBuilder maxPSB = new StringBuilder("[");
      for ( int i = 0; i < this.dimensionLength; i++ )
      {
        minPSB.append( minPoint[ i] ).append( ",");
        maxPSB.append( maxPoint[ i] ).append( "," );
        if ( minPoint[ i] >= maxPoint[ i] )
          minMaxOk = false;
      }
      int indx = minPSB.lastIndexOf( ",");
      minPSB.replace( indx, indx+1, "]" );
      indx = maxPSB.lastIndexOf( ",");
      maxPSB.replace( indx, indx + 1, "]" );

      if ( ! minMaxOk )
        throw new IllegalArgumentException( "Minimum point " + minPSB.toString() + " not always smaller than maximum point " + maxPSB.toString() + ".");

      this.dimensionLength = minPoint.length;
      this.minPoint = minPoint;
      this.maxPoint = maxPoint;
      this.minPointString = minPSB.toString();
      this.maxPointString = maxPSB.toString();
    }

    public int getDimensionLength() { return this.dimensionLength; }
    public double getMinPointValue( int index ) { return this.minPoint[ index]; }
    public double getMaxPointValue( int index ) { return this.maxPoint[ index]; }
    public String toString() { return "Min " + this.minPointString + "; Max " + this.maxPointString;}
  }
}
