package thredds.wcs.v1_0_0_Plus;

import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class AxisSubset
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( AxisSubset.class );

  private CoordinateAxis1D coordAxis;

  private double min, max;
  private int stride;

    public AxisSubset( CoordinateAxis1D coordAxis, double minimum, double maximum, int stride )
    {
      if ( coordAxis == null )
      {
        log.error( "AxisSubset(): Minimum <" + minimum + "> is greater than maximum <" + maximum + ">." );
        throw new IllegalArgumentException( "AxisSubset minimum <" + minimum + "> greater than maximum <" + maximum + ">." );
      }
      this.coordAxis = coordAxis;

      if ( minimum > maximum )
      {
        log.error( "AxisSubset(): Minimum <" + minimum + "> is greater than maximum <" + maximum + ">." );
        throw new IllegalArgumentException( "AxisSubset minimum <" + minimum + "> greater than maximum <" + maximum + ">." );
      }
      if ( stride < 1 )
      {
        log.error( "AxisSubset(): stride <" + stride + "> less than one (1 means all points)." );
        throw new IllegalArgumentException( "AxisSubset stride <" + stride + "> less than one (1 means all points)." );
      }
      this.min = minimum;
      this.max = maximum;
      this.stride = stride;
    }

  public double getMinimum() { return min; }
  public double getMaximum() { return max; }
  public int getStride() { return stride; }

  public String toString()
  {
    return "[min=" + min + ",max=" + max + ",stride=" + stride + "]";
  }

  public Range getRange()
          throws InvalidRangeException
  {
    if ( ! this.coordAxis.isNumeric() )
    {
      log.error( "getRange(): GridCoordSystem must have numeric vertical axis to support min/max range." );
      throw new IllegalArgumentException( "GridCoordSystem must have numeric vertical axis to support min/max range." );
    }
    int minIndex = this.coordAxis.findCoordElement( min );
    int maxIndex = this.coordAxis.findCoordElement( max );
    if ( minIndex == -1 || maxIndex == -1 )
    {
      log.error( "getRange(): GridCoordSystem vertical axis does not contain min/max points." );
      throw new IllegalArgumentException( "GridCoordSystem vertical axis does not contain min/max points." );
    }

    if ( this.coordAxis.getPositive().equalsIgnoreCase( CoordinateAxis.POSITIVE_DOWN ) )
      return new Range( maxIndex, minIndex, stride );
    else
      return new Range( minIndex, maxIndex, stride );
  }
}
