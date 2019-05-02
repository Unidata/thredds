/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import com.google.common.base.MoreObjects;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.GaussianLatitudes;
import ucar.unidata.util.StringUtil2;

import javax.annotation.concurrent.Immutable;

/**
 * A Horizontal coordinate system generated from a GRIB-1 or GRIB-2 GDS.
 *
 * @author John
 * @since 9/5/11
 */
@Immutable
public class GdsHorizCoordSys {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GdsHorizCoordSys.class);

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
    return startx + dx * (nx-1);
  }

  public double getEndY() {
    if (gaussLats != null) return gaussLats.getDouble((int) gaussLats.getSize() - 1);
    return starty + dy * (ny-1);
  }

  public int getScanMode() {
    return scanMode;
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
    LatLonPointImpl center = (LatLonPointImpl) getCenterLatLon();
    StringBuilder result = new StringBuilder(name + "_" + ny + "X" + nx+"-"+center.toString(2));
    StringUtil2.replace(result, ". ","p-");
    return result.toString();
  }

  public ProjectionRect getProjectionBB() {
    return new ProjectionRect(new ProjectionPointImpl(getStartX(), getStartY()), getEndX() - getStartX(), getEndY() - getStartY());
  }

  public LatLonRect getLatLonBB() {
    if (isLatLon()) {
      return new LatLonRect(new LatLonPointImpl(getStartY(), getStartX()), dy * (ny-1), dx * (nx-1));
    } else {
      return proj.projToLatLonBB(getProjectionBB());
    }
  }

  ////////////////////////////////////////////////

  // set gaussian weights based on nparallels
  // some weird adjustment for la1 and la2.
  public void setGaussianLats(int nparallels, float la1, float la2) {
    log.debug ("la1 {}, la2 {}", la1, la2);
    if (this.gaussLats != null) throw new RuntimeException("Cant modify GdsHorizCoordSys");

    int nlats = (2 * nparallels);
    GaussianLatitudes gaussLats = GaussianLatitudes.factory(nlats);

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
      gaussLats = GaussianLatitudes.factory(nlats);
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
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("template", template)
        .add("gdsNumberPoints", gdsNumberPoints)
        .add("scanMode", scanMode)
        .add("proj", proj)
        .add("startx", startx)
        .add("dx", dx)
        .add("starty", starty)
        .add("dy", dy)
        .add("nx", nx)
        .add("ny", ny)
        .add("nxRaw", nxRaw)
        .add("nyRaw", nyRaw)
        .add("nptsInLine", nptsInLine)
        .add("gaussLats", gaussLats)
        .add("gaussw", gaussw)
        .toString();
  }
}
