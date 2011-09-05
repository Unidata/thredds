/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib;

import ucar.ma2.Array;
import ucar.unidata.geoloc.ProjectionImpl;

/**
 * Description
 *
 * @author John
 * @since 9/5/11
 */
public class GdsHorizCoordSys {
  public ucar.unidata.geoloc.ProjectionImpl proj;
  public double startx, dx; // km
  public double starty, dy; // km
  public int nx, ny;
  public Array gaussLats;
  public Array gaussw; // ??

  public GdsHorizCoordSys(ProjectionImpl proj, double startx, double dx, double starty, double dy, int nx, int ny) {
    this.proj = proj;
    this.startx = startx;
    this.dx = dx;
    this.starty = starty;
    this.dy = dy;
    this.nx = nx;
    this.ny = ny;
  }

  public double getStartX() {
    return startx;
  }

  public double getStartY() {
    if (gaussLats != null) return gaussLats.getDouble(0);
    return starty;
  }

  public double getEndX() {
    return startx + dx * nx;
  }

  public double getEndY() {
    if (gaussLats != null) return gaussLats.getDouble((int) gaussLats.getSize() - 1);
    return starty + dy * ny;
  }
}


