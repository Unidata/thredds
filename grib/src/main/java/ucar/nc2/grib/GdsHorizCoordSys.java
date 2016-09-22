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

import net.jcip.annotations.Immutable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.GaussianLatitudes;
import ucar.unidata.util.StringUtil2;

/**
 * A Horizontal coordinate system generated from a GRIB-1 or GRIB-2 GDS.
 *
 * @author John
 * @since 9/5/11
 */
@Immutable
public class GdsHorizCoordSys {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GdsHorizCoordSys.class);

  private final String name;
  public final int template, gdsNumberPoints, scanMode;
  public final ucar.unidata.geoloc.ProjectionImpl proj;
  public final double startx, dx; // km
  public final double starty, dy; // km
  public final int nx, ny;        // regridded
  public final int nxRaw, nyRaw;  // raw
  public final int[] nptsInLine; // non-null id thin grid

  // hmmmm
  private Array gaussLats;
  private Array gaussw;

  public GdsHorizCoordSys(String name, int template, int gdsNumberPoints, int scanMode, ProjectionImpl proj,
                          double startx, double dx, double starty, double dy, int nxRaw, int nyRaw, int[] nptsInLine) {

    this.name = name;
    this.template = template;
    this.gdsNumberPoints = gdsNumberPoints; // only used by GRIB2
    this.scanMode = scanMode;
    this.proj = proj;
    this.startx = startx;
    this.dx = dx;
    this.starty = starty;
    this.dy = dy;
    this.nxRaw = nxRaw;
    this.nyRaw = nyRaw;
    this.nptsInLine = nptsInLine;

    // thin grids
    if (nptsInLine != null) {
      if (nxRaw > 0) {
        nx = nxRaw;
        ny = QuasiRegular.getMax(nptsInLine);
      } else if (nyRaw > 0) {
        ny = nyRaw;
        nx = QuasiRegular.getMax(nptsInLine);
      } else {
        throw new IllegalArgumentException("Quasi Grids nx,ny="+nxRaw+","+nyRaw);
      }
    } else {
      nx = nxRaw;
      ny = nyRaw;
    }
  }

  public String getName() {
    return name;
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

  public boolean isLatLon() {
    return proj instanceof LatLonProjection;
  }

  public LatLonPoint getCenterLatLon() {
    return proj.projToLatLon(startx + dx * nx / 2, starty + dy * ny / 2);
  }

  public String makeDescription() {
    return name + "_" + ny + "X" + nx+" (Center "+getCenterLatLon()+")";
  }

  public String makeId() {
    StringBuilder result = new StringBuilder(name + "_" + ny + "X" + nx+"-"+getCenterLatLon());
    StringUtil2.replace(result, ". ","p-");
    return result.toString();
  }

  ////////////////////////////////////////////////

  // set gaussian weights based on nparallels
  // some weird adjustment for la1 and la2.
  public void setGaussianLats(int nparallels, float la1, float la2) {
    log.debug ("la1 {}, la2 {}", la1, la2);
    if (this.gaussLats != null) throw new RuntimeException("Cant modify GdsHorizCoordSys");

    int nlats = (2 * nparallels);

    log.debug ("nlats {}", nlats);

    GaussianLatitudes gaussLats = new GaussianLatitudes(nlats);

    int bestStartIndex = 0, bestEndIndex = 0;
    double bestStartDiff = Double.MAX_VALUE;
    double bestEndDiff   = Double.MAX_VALUE;
    for (int i = 0; i < nlats; i++) {
      double diff = Math.abs(gaussLats.latd[i] - la1);
      if (diff < bestStartDiff) {
        bestStartDiff = diff;
        bestStartIndex = i;
      }
      diff = Math.abs(gaussLats.latd[i] - la2);
      if (diff < bestEndDiff) {
        bestEndDiff = diff;
        bestEndIndex = i;
      }
    }

    log.debug ("first pass: bestStartIndex {}, bestEndIndex {}", bestStartIndex, bestEndIndex);

    if (Math.abs(bestEndIndex - bestStartIndex) + 1 != nyRaw) {
      log.warn("GRIB gaussian lats: NP != NY, use NY");  // see email from Toussaint@dkrz.de datafil:
      nlats = nyRaw;
      gaussLats = new GaussianLatitudes(nlats);
      bestStartIndex = 0;
      bestEndIndex = nyRaw - 1;
    }
    boolean goesUp = bestEndIndex > bestStartIndex;

    log.debug ("bestStartIndex {}, bestEndIndex {}, goesUp {}", bestStartIndex, bestEndIndex, goesUp);

    // create the data
    int useIndex = bestStartIndex;
    float[] data = new float[nyRaw];
    float[] gaussw = new float[nyRaw];
    for (int i = 0; i < nyRaw; i++) {
      data[i] = (float) gaussLats.latd[useIndex];
      gaussw[i] = (float) gaussLats.gaussw[useIndex];

        log.trace ("i {}, useIndex {}, data {}, gaussw {}", i, useIndex, data[i], gaussw[i]);
      if (goesUp) {
        useIndex++;
      } else {
        useIndex--;
      }
    }

    this.gaussLats = Array.factory(DataType.FLOAT, new int[]{nyRaw}, data);
    this.gaussw    = Array.factory(DataType.FLOAT, new int[]{nyRaw}, gaussw);
  }

  public Array getGaussianLats() {
    return gaussLats;
  }

  public Array getGaussianWeights() {
    return gaussw;
  }

  @Override
  public String toString() {
    return "GdsHorizCoordSys" +
            "\n name='" + name + '\'' +
            "\n  template=" + template +
            "\n  gdsNumberPoints=" + gdsNumberPoints +
            "\n  scanMode=" + scanMode +
            "\n  proj=" + proj +
            "\n  startx=" + startx +
            "\n  dx=" + dx +
            "\n  starty=" + starty +
            "\n  dy=" + dy +
            "\n  nx=" + nx +
            "\n  ny=" + ny +
            "\n  gaussLats=" + gaussLats +
            "\n  gaussw=" + gaussw +
            '\n';
  }
}
