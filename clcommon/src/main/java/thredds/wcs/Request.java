/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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
      for ( int i = 0; i < minPoint.length; i++ )
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
