/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.dt.grid;

import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.ma2.ArrayDouble;
import ucar.ma2.MAMath;

/**
 * Class Description
 *
 * @author caron
 * @since Jul 10, 2009
 */


public class GridCoordinate2D {
  private CoordinateAxis2D latCoord, lonCoord;
  private MAMath.MinMax latMinMax, lonMinMax;

  GridCoordinate2D(CoordinateAxis2D latCoord, CoordinateAxis2D lonCoord) {
    this.latCoord = latCoord;
    this.lonCoord = lonCoord;
  }


  public int[] findCoordElement(double lat, double lon) {
    findBounds();
    if (lat < latMinMax.min) return null;
    if (lat > latMinMax.max) return null;
    if (lon < lonMinMax.min) return null;
    if (lon > lonMinMax.max) return null;

    ArrayDouble.D2 lats = latCoord.getMidpoints();
    ArrayDouble.D2 lons = latCoord.getMidpoints();

    return null;
  }

  private void findBounds() {
    if (latMinMax != null) return;

    ArrayDouble.D2 lats = latCoord.getMidpoints();
    ArrayDouble.D2 lons = latCoord.getMidpoints();

    // assume missing values have been converted to NaNs
    latMinMax = MAMath.getMinMax(lats);
    lonMinMax = MAMath.getMinMax(lons);
  }


   private int[] findCoordElement(double wantLat, double wantLon, int i, int j) {
     return null;
   }

   private boolean contains(ArrayDouble.D2 lats, ArrayDouble.D2 lons, double wantLat, double wantLon, int i, int j ) {
     if (lats.get(i, j) > wantLat) return false; 
     return true;
   }
}
