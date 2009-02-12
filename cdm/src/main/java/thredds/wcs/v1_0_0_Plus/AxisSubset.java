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

    if ( this.coordAxis.getPositive().equalsIgnoreCase( ucar.nc2.constants.CF.POSITIVE_DOWN ) )
      return new Range( maxIndex, minIndex, stride );
    else
      return new Range( minIndex, maxIndex, stride );
  }
}
