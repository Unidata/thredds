// $Id: GridRenderer.java 50 2006-07-12 16:30:06Z caron $
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
package ucar.nc2.ui.grid;

import ucar.ma2.*;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.util.Format;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.Debug;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/**
 * Render grids using Java2D API.
 *
 * @author caron
 * @version $Revision: 50 $ $Date: 2006-07-12 16:30:06Z $
 */

public class GridRenderer {
  public static final int HORIZ_MinMaxType = 0;
  public static final int VERT_MinMaxType = 1;
  public static final int VOL_MinMaxType = 2;
  public static final int HOLD_MinMaxType = 3;
  public static final int USER_MinMaxType = 4;

  private PreferencesExt store;

  // draw state
  private boolean drawGrid = true;
  private boolean drawGridLines = true;
  private boolean drawContours = false;
  private boolean drawContourLabels = false;

  private ColorScale cs = null;
  private int dataMinMaxType;
  private ProjectionImpl drawProjection = null;    // current drawing Projection
  private ProjectionImpl dataProjection = null;    // current GridDatatype Projection
  private GridDatatype orgGrid = null;
  private GridDatatype stridedGrid = null;

  // data stuff
  private Array dataH, dataV;
  //private Index imaH, imaV;
  private int wantLevel = -1, wantSlice = -1, wantTime = -1, horizStride = 1;   // for next draw()
  private int wantRunTime = -1, wantEnsemble = -1;
  private int lastLevel = -1, lastTime = -1, lastSlice = -1, lastStride = -1;   // last data read
  private int lastRunTime = -1, lastEnsemble = -1;   // last data read
  private GridDatatype lastGrid = null;

  // drawing optimization
  private boolean colorScaleChanged = true, dataVolumeChanged = true;
  private boolean useModeForProjections = false; // use colorMode optimization for different projections
  private boolean sameProjection = false;
  private LatLonProjection projectll;       // special handling for LatLonProjection

  // working objects to minimize excessive gc
  //private StringBuffer sbuff = new StringBuffer(200);
  private LatLonPointImpl ptL1 = new LatLonPointImpl();
  private LatLonPointImpl ptL2 = new LatLonPointImpl();
  private ProjectionPointImpl ptP1 = new ProjectionPointImpl();
  private ProjectionPointImpl ptP2 = new ProjectionPointImpl();
  private ProjectionRect[] rects = new ProjectionRect[2];
  private Point pt = new Point();

  private final boolean debugHorizDraw = false, debugSeam = false, debugLatLon = false, debugMiss = false;
  private boolean debugPathShape = false, debugArrayShape = false, debugPts = false;

  private final double TOLERANCE = 1.0e-5;

  /**
   * constructor
   */
  public GridRenderer(PreferencesExt store) {
    this.store = store;
    rects[0] = new ProjectionRect();
  }

  ///// bean properties

  /**
   * get the current ColorScale
   */
  public ColorScale getColorScale() {
    return cs;
  }

  /**
   * set the ColorScale to use
   */
  public void setColorScale(ColorScale cs) {
    this.cs = cs;
    colorScaleChanged = true;
  }

  /**
   * get the ColorScale data min/max type
   */
  public int getDataMinMaxType() {
    return dataMinMaxType;
  }

  /**
   * set the ColorScale data min/max type
   */
  public void setDataMinMaxType(int type) {
    if (type != dataMinMaxType) {
      dataMinMaxType = type;
      colorScaleChanged = true;
    }
  }

  /**
   * get the current GridDatatype
   */
  public GridDatatype getGeoGrid() {
    return orgGrid;
  }

  /**
   * set the Grid
   */
  public void setGeoGrid(GridDatatype grid) {
    this.orgGrid = grid;
    this.lastGrid = null;
    dataProjection = grid.getProjection();
    makeStridedGrid();
  }

  public Array getCurrentHorizDataSlice() {
    return dataH;
  }

  /**
   * get the current GridDatatype data projection
   */
  public ProjectionImpl getDataProjection() {
    return dataProjection;
  }

  /**
   * get the current display projection
   */
  public ProjectionImpl getProjection() {
    return drawProjection;
  }

  /**
   * set the Projection to use for drawing
   */
  public void setProjection(ProjectionImpl project) {
    drawProjection = project;
  }

  /* get whether grid should be drawn */
  public boolean getDrawGrid() {
    return drawGrid;
  }

  /* set whether grid should be drawn */
  public void setDrawGridLines(boolean drawGrid) {
    this.drawGridLines = drawGrid;
  }

  /* get whether countours  should be drawn */
  public boolean getDrawContours() {
    return drawContours;
  }

  /* set whether countours  should be drawn */
  public void setDrawContours(boolean drawContours) {
    this.drawContours = drawContours;
  }

  /* set whether contour labels should be drawn */
  public boolean getDrawContourLabels() {
    return drawContourLabels;
  }

  /* set whether contour labels should be drawn */
  public void setDrawContourLabels(boolean drawContourLabels) {
    this.drawContourLabels = drawContourLabels;
  }


  /* get what vertical level to draw */
  public int getLevel() {
    return wantLevel;
  }

  /* set what vertical level to draw */
  public void setLevel(int level) {
    wantLevel = level;
  }

  /* get what time slice to draw */
  public int getTime() {
    return wantTime;
  }

  /* set what time slice to draw */
  public void setTime(int time) {
    wantTime = time;
  }

  /* get what runtime slice to draw */
  public int getRunTime() {
    return wantRunTime;
  }

  /* set what runtime slice to draw */
  public void setRunTime(int runtime) {
    wantRunTime = runtime;
  }

  /* get what ensemble slice to draw */
  public int getEnsemble() {
    return wantEnsemble;
  }

  /* set what ensemble slice to draw */
  public void setEnsemble(int ensemble) {
    wantEnsemble = ensemble;
  }

  /* get what y-z slice to draw */
  public int getSlice() {
    return wantSlice;
  }

  /* set what y-z slice to draw */
  public void setSlice(int slice) {
    wantSlice = slice;
  }

  public int getHorizStride() {
    return horizStride;
  }

  public void setHorizStride(int horizStride) {
    this.horizStride = horizStride;
  }

  void makeStridedGrid() {

    if (horizStride > 1) {
      try {
        stridedGrid = orgGrid.makeSubset(null, null, null, 1, horizStride, horizStride);
      } catch (InvalidRangeException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
    } else {
      stridedGrid = orgGrid;
    }

  }

  /// info at a point

  /**
   * find the level (z) index that is represented by this point
   *
   * @param pos coord in data z coordinate space.
   */
  public int findLevelCoordElement(double pos) {
    if (null == orgGrid)
      return -1;

    // find the grid index
    GridCoordSystem geocs = orgGrid.getCoordinateSystem();
    CoordinateAxis1D zaxis = geocs.getVerticalAxis();
    return (zaxis == null) ? -1 : zaxis.findCoordElement(pos);
  }

  /**
   * @param pp
   * @return x index for this point
   */
  public int findSliceFromPoint(ProjectionPoint pp) {
    if ((null == drawProjection) || (null == stridedGrid))
      return -1;

    // convert to dataProjection, where x and y are orthogonal
    if (!sameProjection) {
      LatLonPoint llpt = drawProjection.projToLatLon(pp);
      pp = dataProjection.latLonToProj(llpt);
    }

    // find the grid index
    GridCoordSystem geocs = stridedGrid.getCoordinateSystem();
    CoordinateAxis xaxis = geocs.getXHorizAxis();
    if (xaxis == null || !(xaxis instanceof CoordinateAxis1D))
      return -1;
    int[] index = geocs.findXYindexFromCoord(pp.getX(), pp.getY(), null);
    return index[0];
  }

  /**
   * Get the data value at this projection (x,y) point.
   *
   * @param loc : point in display projection coordinates (plan view)
   * @return String representation of value
   */
  public String getXYvalueStr(ProjectionPoint loc) {
    if ((stridedGrid == null) || (dataH == null))
      return "";

    // convert to dataProjection, where x and y are orthogonal
    if (!sameProjection) {
      LatLonPoint llpt = drawProjection.projToLatLon(loc);
      loc = dataProjection.latLonToProj(llpt);
    }

    // find the grid indexes
    GridCoordSystem geocs = stridedGrid.getCoordinateSystem();
    valueIndex = geocs.findXYindexFromCoord(loc.getX(), loc.getY(), valueIndex);
    int wantx = valueIndex[0];
    int wanty = valueIndex[1];

    // get value, construct the string
    if ((wantx == -1) || (wanty == -1))
      return "outside grid area";
    else {
      try {
        Index imaH = dataH.getIndex();
        double value = dataH.getDouble(imaH.set(wanty, wantx));
        int wantz = (geocs.getVerticalAxis() == null) ? -1 : lastLevel;
        return makeXYZvalueStr(value, wantx, wanty, wantz);
      } catch (Exception e) {
        return "error " + wantx + " " + wanty;
      }
    }
  }

  private int[] valueIndex = new int[2];

  /**
   * Get the (y,z) position from the vertical view coordinates.
   *
   * @param loc : point in display projection coordinates (vertical view)
   * @return String representation of position
   */
  public String getYZpositionStr(Point2D loc) {
    if ((stridedGrid == null) || (dataV == null))
      return "";

    GridCoordSystem geocs = stridedGrid.getCoordinateSystem();
    /* CoordinateAxis1D xaxis = (CoordinateAxis1D) geocs.getXHorizAxis();
    double x = (xaxis == null) ? 0.0 : xaxis.getCoordValue(lastSlice);
    double y = loc.getX();
    LatLonPointImpl lpt = dataProjection.projToLatLon(x, y);
    sbuff.setLength(0);
    sbuff.append(LatLonPointImpl.latToString(lpt.getLatitude(), 3)); */

    StringBuilder sbuff = new StringBuilder();
    sbuff.setLength(0);
    sbuff.append(Format.d(loc.getX(), 3));
    CoordinateAxis yaxis = geocs.getYHorizAxis();
    sbuff.append(" " + yaxis.getUnitsString());
    sbuff.append(" ");
    sbuff.append(Format.d(loc.getY(), 3));
    CoordinateAxis1D zaxis = geocs.getVerticalAxis();
    sbuff.append(" " + zaxis.getUnitsString());

    return sbuff.toString();
  }

  /**
   * find the data value at this point
   *
   * @param loc : point in display projection coordinates (vertical view)
   * @return String representation of value
   */
  public String getYZvalueStr(Point2D loc) {
    if ((stridedGrid == null) || (dataV == null))
      return "";

    // find the grid indexes
    GridCoordSystem geocs = stridedGrid.getCoordinateSystem();
    CoordinateAxis1D zaxis = geocs.getVerticalAxis();
    if (zaxis == null)
      return "";

    valueIndex = geocs.findXYindexFromCoord(loc.getX(), lastSlice, valueIndex);

    int wanty = valueIndex[1];
    int wantz = zaxis.findCoordElement(loc.getY());

    // get value, construct the string
    if ((wanty == -1) || (wantz == -1))
      return "outside grid area";
    else {
      Index imaV = dataV.getIndex();
      double value = dataV.getDouble(imaV.set(wantz, wanty));
      int wantx = (geocs.getXHorizAxis() == null) ? -1 : lastSlice;
      return makeXYZvalueStr(value, wantx, wanty, wantz);
    }
  }

  private String makeXYZvalueStr(double value, int wantx, int wanty, int wantz) {
    if (stridedGrid.isMissingData(value)) {
      //if (debugMiss) System.out.println("debug miss = "+value+" "+cs.getIndexFromValue(value));
      if (Debug.isSet("pick/showGridIndexes"))
        return ("missing data @ (" + wantx + "," + wanty + ")");
      else
        return "missing data";
    }

    StringBuilder sbuff = new StringBuilder();
    sbuff.append(Format.d(value, 6));
    sbuff.append(" " + stridedGrid.getUnitsString());

    GridCoordSystem geocs = stridedGrid.getCoordinateSystem();
    if (!(geocs.getXHorizAxis() instanceof CoordinateAxis1D) || !(geocs.getXHorizAxis() instanceof CoordinateAxis1D)) {
      if (Debug.isSet("pick/showGridIndexes"))
        sbuff.append("@ (" + wantx + "," + wanty + ")");
      return sbuff.toString();
    }

    CoordinateAxis1D xaxis = (CoordinateAxis1D) geocs.getXHorizAxis();
    CoordinateAxis1D yaxis = (CoordinateAxis1D) geocs.getYHorizAxis();
    CoordinateAxis1D zaxis = geocs.getVerticalAxis();

    sbuff.append(" @ ");

    if ((wantx >= 0) && (wanty >= 0)) {
      LatLonPointImpl lpt;
      if (dataProjection.isLatLon())
        lpt = new LatLonPointImpl(yaxis.getCoordValue(wanty), xaxis.getCoordValue(wantx));
      else
        lpt = dataProjection.projToLatLon(xaxis.getCoordValue(wantx), yaxis.getCoordValue(wanty));

      sbuff.append(lpt.toString());
      if (Debug.isSet("pick/showDataProjectionCoords")) {
        sbuff.append("(" + Format.d(xaxis.getCoordValue(wantx), 3));
        sbuff.append(" " + Format.d(yaxis.getCoordValue(wanty), 3));
        sbuff.append(" " + xaxis.getUnitsString() + ")");
      }
      if (Debug.isSet("pick/showDisplayProjectionCoords")) {
        ProjectionPoint pt = drawProjection.latLonToProj(lpt);
        sbuff.append("(" + Format.d(pt.getX(), 3));
        sbuff.append(" " + Format.d(pt.getY(), 3) + ")");
      }
      if (Debug.isSet("pick/showGridIndexes")) {
        sbuff.append("(" + wantx + "," + wanty + ")");
      }
    } else if (wantx >= 0) {
      if (dataProjection.isLatLon())
        sbuff.append(LatLonPointImpl.latToString(xaxis.getCoordValue(wantx), 3));
      else {
        sbuff.append(" " + Format.d(xaxis.getCoordValue(wantx), 3));
        sbuff.append(" " + xaxis.getUnitsString());
      }
    } else if (wanty >= 0) {
      if (dataProjection.isLatLon())
        sbuff.append(LatLonPointImpl.latToString(yaxis.getCoordValue(wanty), 3));
      else {
        sbuff.append(" " + Format.d(yaxis.getCoordValue(wanty), 3));
        sbuff.append(" " + yaxis.getUnitsString());
      }
    }

    if (wantz >= 0) {
      sbuff.append(" " + Format.d(zaxis.getCoordValue(wantz), 3));
      sbuff.append(" " + zaxis.getUnitsString());
    }

    return sbuff.toString();
  }

  //////// data routines

  /* get an x,y,z data volume for the given time
 private void makeDataVolume( GridDatatype g, int time) {
   try {
     dataVolume = g.readVolumeData( time);
   } catch (java.io.IOException e) {
     System.out.println("Error reading netcdf file "+e);
     dataVolume = null;
   }
   lastGrid = g;
   lastTime = time;
   lastLevel = -1;  // invalidate
   lastSlice = -1;  // invalidate
   dataVolumeChanged = true;
 } */

  private Array makeHSlice(GridDatatype useG, int level, int time, int ensemble, int runtime) {

    // make sure x, y exists
    GridCoordSystem gcs = useG.getCoordinateSystem();
    CoordinateAxis xaxis = gcs.getXHorizAxis();
    CoordinateAxis yaxis = gcs.getYHorizAxis();
    if ((xaxis == null) || (yaxis == null))    // doesnt exist
      return null;
    if ((xaxis.getSize() <= 1) || (yaxis.getSize() <= 1)) // LOOK ??
      return null;

    // make sure we need new one
    if (useG.equals(lastGrid) && (time == lastTime) && (level == lastLevel) && (horizStride == lastStride)
            && (ensemble == lastEnsemble) && (runtime == lastRunTime))
      return dataH; // nothing changed

    // get the data slice
    try {
      dataH = useG.readDataSlice(runtime, ensemble, time, level, -1, -1);
      // imaH = dataH.getIndex();
    } catch (java.io.IOException e) {
      System.out.println("GridRender.makeHSlice Error reading netcdf file= " + e);
      return null;
    }

    lastGrid = useG;
    lastTime = time;
    lastLevel = level;
    lastEnsemble = ensemble;
    lastRunTime = runtime;
    lastStride = horizStride;

    /*
    CoordinateAxis1D zaxis = gcs.getVerticalAxis();
    if ((zaxis == null) || (zaxis.getSize() < 1)) {
      dataH = dataVolume;  // volume is xy plane
    } else {
      dataH = dataVolume.slice(0, level);  // if z exists, always first (logical) dimension
    } */

    if (debugArrayShape) {
      System.out.println("Horiz shape = ");
      for (int i = 0; i < dataH.getRank(); i++)
        System.out.println("   shape = " + dataH.getShape()[i]);
    }

    return dataH;
  }

  private Array makeVSlice(GridDatatype g, int vSlice, int time, int ensemble, int runtime) {
    // make sure we have x, z
    GridCoordSystem gcs = g.getCoordinateSystem();
    CoordinateAxis xaxis = gcs.getXHorizAxis();
    CoordinateAxis zaxis = gcs.getVerticalAxis();
    if ((xaxis == null) || (zaxis == null))    // doesnt exist
      return null;
    if ((xaxis.getSize() <= 1) || (zaxis.getSize() <= 1)) // LOOK ??
      return null;

    // make sure we need it
    if (g.equals(lastGrid) && (time == lastTime) && (vSlice == lastSlice)
            && (ensemble == lastEnsemble) && (runtime == lastRunTime))
      return dataV; // nothing changed

    // get the slice
    try {
      dataV = g.readDataSlice(runtime, ensemble, time, -1, -1, vSlice);
    } catch (java.io.IOException e) {
      System.out.println("GridRender.makeHSlice Error reading netcdf file= " + e);
      return null;
    }

    lastGrid = g;
    lastTime = time;
    lastSlice = vSlice;
    lastEnsemble = ensemble;
    lastRunTime = runtime;

    if (debugArrayShape) {
      System.out.println("Vert shape = ");
      for (int i = 0; i < dataV.getRank(); i++)
        System.out.println("   shape = " + dataV.getShape()[i]);
    }

    return dataV;
  }

  //////////// Renderer stuff

  /**
   * returns null
   * public ucar.unidata.geoloc.LatLonRect getPreferredArea() {
   * /* if (dataProjection != null)
   * return dataProjection.getPreferredArea();
   * else
   * return null;
   * }
   * <p/>
   * /* private java.awt.Color color, backColor;
   * public void setBackgroundColor(java.awt.Color backColor) { this.backColor = backColor;}
   * public void setColor(java.awt.Color color) { this.color = color;}
   * public java.awt.Color getColor() { return color; }
   */

  // set colorscale limits, missing data
  private void setColorScaleParams() {
    if (dataMinMaxType == HOLD_MinMaxType)
      return;

    Array dataArr;
    if (dataMinMaxType == HORIZ_MinMaxType)
      dataArr = makeHSlice(stridedGrid, wantLevel, wantTime, wantEnsemble, wantRunTime);
    else
      dataArr = makeVSlice(stridedGrid, wantSlice, wantTime, wantEnsemble, wantRunTime);

    if (dataArr != null) {
      MAMath.MinMax minmax = stridedGrid.hasMissingData() ?
              stridedGrid.getMinMaxSkipMissingData(dataArr) : MAMath.getMinMax(dataArr);
      cs.setMinMax(minmax.min, minmax.max);
      cs.setGeoGrid(stridedGrid);
    }
    dataVolumeChanged = false;
    colorScaleChanged = false;
  }

  /**
   * Do the rendering to the given Graphics2D object.
   *
   * @param g      Graphics2D object: has clipRect and AffineTransform set.
   * @param dFromN transforms "Normalized Device" to Device coordinates
   */
  public void renderVertView(java.awt.Graphics2D g, AffineTransform dFromN) {
    if ((stridedGrid == null) || (cs == null) || (drawProjection == null))
      return;

    if (!drawGrid && !drawContours)
      return;

    dataV = makeVSlice(stridedGrid, wantSlice, wantTime, wantEnsemble, wantRunTime);
    if (dataV == null)
      return;

    if (Debug.isSet("GridRenderer/vert"))
      System.out.println("GridRenderer/vert: redraw grid");

    GridCoordSystem geocs = stridedGrid.getCoordinateSystem();
    CoordinateAxis1D zaxis = geocs.getVerticalAxis();
    CoordinateAxis1D yaxis = (CoordinateAxis1D) geocs.getYHorizAxis();
    if ((yaxis == null) || (zaxis == null))
      return;
    int nz = (int) zaxis.getSize();
    int ny = (int) yaxis.getSize();

    if (drawGrid) {
      int count = 0;
      Index imaV = dataV.getIndex();
      for (int z = 0; z < nz; z++) {
        double zbeg = zaxis.getCoordEdge(z);
        double zend = zaxis.getCoordEdge(z + 1);

        for (int y = 0; y < ny; y++) {
          double ybeg = yaxis.getCoordEdge(y);
          double yend = yaxis.getCoordEdge(y + 1);

          double val = dataV.getDouble(imaV.set(z, y));
          int thisColor = cs.getIndexFromValue(val);
          count += drawRect(g, thisColor, ybeg, zbeg, yend, zend, false);
        }
      }
    }

    if (drawContours) {
      double[] zedges = zaxis.getCoordValues();
      double[] yedges = yaxis.getCoordValues();

      int nlevels = cs.getNumColors();
      ArrayList levels = new ArrayList(nlevels);
      for (int i = 1; i < nlevels - 1; i++)
        levels.add(new Double(cs.getEdge(i)));

      long startTime = System.currentTimeMillis();
      ContourFeatureRenderer contourRendererV;
      try {
        ContourGrid conGrid = new ContourGrid(dataV.transpose(0, 1), levels, yedges, zedges,
                stridedGrid);
        contourRendererV = new ContourFeatureRenderer(conGrid, null);

        //contourRendererV.setProjection(drawProjection);
        contourRendererV.setColor(Color.black);
        contourRendererV.setShowLabels(drawContourLabels);
        contourRendererV.draw(g, dFromN);
      } catch (Exception e) {
        System.out.println("draw Contours Vert exception = " + e);
        e.printStackTrace(System.err);
      }
      if (Debug.isSet("timing/contourDraw")) {
        long tookTime = System.currentTimeMillis() - startTime;
        System.out.println("timing/contourDraw: Vert:" + tookTime * .001 + " seconds");
      }
    }

    if ((lastLevel >= 0) && (lastLevel < nz))
      drawXORline(g, yaxis.getCoordEdge(0), zaxis.getCoordValue(lastLevel),
              yaxis.getCoordEdge(ny), zaxis.getCoordValue(lastLevel));
  }

  /**
   * Do the rendering to the given Graphics2D object.
   *
   * @param g      Graphics2D object: has clipRect and AffineTransform set.
   * @param dFromN transforms "Normalized Device" to Device coordinates
   */
  public void renderPlanView(java.awt.Graphics2D g, AffineTransform dFromN) {
    if ((stridedGrid == null) || (cs == null) || (drawProjection == null))
      return;

    if (!drawGrid && !drawContours)
      return;

    // no anitaliasing
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

    dataH = makeHSlice(stridedGrid, wantLevel, wantTime, wantEnsemble, wantRunTime);
    if (dataH == null)
      return;

    setColorScaleParams();

    if (drawGrid)
      drawGridHoriz(g, dataH);
    if (drawContours)
      drawContours(g, dataH.transpose(0, 1), dFromN);
    if (drawGridLines)
      drawGridLines(g);

    // LOOK removed this to allow 2D x, y coordinates 10/29/06
    /* draw the vertical line indicating current slice
  gpRun.reset();

  Point2D.Double ptSrc = new Point2D.Double();
  Point2D.Double ptDest = new Point2D.Double();
  AffineTransform at = g.getTransform();

  boolean showPts = Debug.isSet("GridRenderer/XORline");
  if (showPts)
    System.out.println("GridRenderer/XORline: drawXORline:"+at);

  GridCoordSystem geocs = stridedGrid.getCoordinateSystem();
  CoordinateAxis xaxis =  geocs.getXHorizAxis();
  CoordinateAxis1D zaxis = geocs.getVerticalAxis();
  int nx = (int) xaxis.getSize();
  if ((zaxis != null) && (xaxis != null) && (wantSlice >=0) && (wantSlice < nx)) {
    double xval = xaxis.getCoordValue(wantSlice);

    CoordinateAxis1D yaxis = (CoordinateAxis1D) geocs.getYHorizAxis();
    int ny = (int) yaxis.getSize();
    int count = 0;
    for (int i=0; i<ny; i++) {
      double yval = yaxis.getCoordValue(i);
      LatLonPoint llp = dataProjection.projToLatLon( xval, yval);
      ProjectionPoint pt = drawProjection.latLonToProj( llp);

      // workaround for bug in jdk 1.3, coord overflow hangs... fixed in 1.4
      ptSrc.setLocation( pt.getX(), pt.getY());
      at.transform( ptSrc, ptDest);
      if (showPts) System.out.println("  "+ptDest);
      if ( Math.abs(ptDest.getX()) > Short.MAX_VALUE ||
          Math.abs(ptDest.getY()) > Short.MAX_VALUE) {
        if (showPts) System.out.println("  --> skipped ");
        continue;
      }

      if (count == 0)
        gpRun.moveTo( (float) pt.getX(), (float) pt.getY());
      else
        gpRun.lineTo( (float) pt.getX(), (float) pt.getY());
      count++;
    }

    g.setColor(Color.black);
    if (!Debug.isSet("GridRenderer/XORlineDraw"))
      g.draw(gpRun);
  }  */
  }

  private void drawGridHoriz(java.awt.Graphics2D g, Array data) {
    int count = 0;

    GridCoordSystem geocs = stridedGrid.getCoordinateSystem();
    CoordinateAxis xaxis = geocs.getXHorizAxis();
    CoordinateAxis yaxis = geocs.getYHorizAxis();

    if ((xaxis instanceof CoordinateAxis1D) && (yaxis instanceof CoordinateAxis1D)) {
      drawGridHoriz1D(g, data, (CoordinateAxis1D) xaxis, (CoordinateAxis1D) yaxis);
      return;
    }

    data = data.reduce();
    if (data.getRank() != 2)
      throw new IllegalArgumentException("must be 2D");

    if (!(xaxis instanceof CoordinateAxis2D) || !(yaxis instanceof CoordinateAxis2D))
      throw new IllegalArgumentException("must be CoordinateAxis2D");

    // 2D case
    CoordinateAxis2D xaxis2D = (CoordinateAxis2D) xaxis;
    CoordinateAxis2D yaxis2D = (CoordinateAxis2D) yaxis;

    /* if (geocs.isStaggered()) {
      drawGridHorizStaggered(g, data, xaxis2D, yaxis2D);
      return;
    } */

    ArrayDouble.D2 edgex = CoordinateAxis2D.makeXEdges(xaxis2D.getMidpoints());
    ArrayDouble.D2 edgey = CoordinateAxis2D.makeYEdges(yaxis2D.getMidpoints());

    Index ima = data.getIndex();
    GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 5);

    int[] shape = xaxis2D.getShape(); // should both be the same
    int ny = shape[0];
    int nx = shape[1];

    for (int y = 0; y < ny; y++) {
      for (int x = 0; x < nx; x++) {
        gp.reset();
        gp.moveTo((float) edgex.get(y, x), (float) edgey.get(y, x));
        gp.lineTo((float) edgex.get(y, x + 1), (float) edgey.get(y, x + 1));
        gp.lineTo((float) edgex.get(y + 1, x + 1), (float) edgey.get(y + 1, x + 1));
        gp.lineTo((float) edgex.get(y + 1, x), (float) edgey.get(y + 1, x));

        double val = data.getDouble(ima.set(y, x));   // ordering LOOK
        int colorIndex = cs.getIndexFromValue(val);
        g.setColor(cs.getColor(colorIndex));
        g.fill(gp);
      }
    }

  }

  private void drawGridLines(java.awt.Graphics2D g) {
    int count = 0;

    GridCoordSystem geocs = stridedGrid.getCoordinateSystem();
    CoordinateAxis xaxis = geocs.getXHorizAxis();
    CoordinateAxis yaxis = geocs.getYHorizAxis();

    if (!(xaxis instanceof CoordinateAxis2D) || !(yaxis instanceof CoordinateAxis2D))
      return;

    // 2D case
    CoordinateAxis2D xaxis2D = (CoordinateAxis2D) xaxis;
    CoordinateAxis2D yaxis2D = (CoordinateAxis2D) yaxis;

    ArrayDouble.D2 edgex = CoordinateAxis2D.makeXEdges(xaxis2D.getMidpoints());
    ArrayDouble.D2 edgey = CoordinateAxis2D.makeYEdges(yaxis2D.getMidpoints());

    GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 5);
    g.setColor(Color.BLACK);

    int[] shape = xaxis2D.getShape(); // should both be the same
    int ny = shape[0];
    int nx = shape[1];

    for (int y = 0; y < ny+1; y += 10) {
      gp.reset();
      for (int x = 0; x < nx+1; x++) {
        if (x == 0)
          gp.moveTo((float) edgex.get(y, x), (float) edgey.get(y, x));
        else
          gp.lineTo((float) edgex.get(y, x), (float) edgey.get(y, x));
      }
      g.draw(gp);
    }

    for (int x = 0; x < nx+1; x += 10) {
      gp.reset();
      for (int y = 0; y < ny+1; y++) {
        if (y == 0)
          gp.moveTo((float) edgex.get(y, x), (float) edgey.get(y, x));
        else
          gp.lineTo((float) edgex.get(y, x), (float) edgey.get(y, x));
      }
      g.draw(gp);
    }

  }


  private void drawGridHorizStaggered(java.awt.Graphics2D g, Array data, CoordinateAxis2D xaxis2D, CoordinateAxis2D yaxis2D) {
    ArrayDouble.D2 edgex = CoordinateAxis2D.makeXEdgesRotated(xaxis2D.getMidpoints());
    ArrayDouble.D2 edgey = CoordinateAxis2D.makeYEdgesRotated(yaxis2D.getMidpoints());

    Index ima = data.getIndex();
    GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 5);

    int[] shape = xaxis2D.getShape(); // should both be the same
    int ny = shape[0];
    int nx = shape[1];

    // even y
    for (int y = 0; y < ny - 1; y += 2) {
      for (int x = 0; x < nx - 1; x++) {
        gp.reset();
        gp.moveTo((float) edgex.get(y, x), (float) edgey.get(y, x));
        gp.lineTo((float) edgex.get(y + 1, x), (float) edgey.get(y + 1, x));
        gp.lineTo((float) edgex.get(y + 2, x), (float) edgey.get(y + 2, x));
        gp.lineTo((float) edgex.get(y + 1, x + 1), (float) edgey.get(y + 1, x + 1));

        double val = data.getDouble(ima.set(y, x));   // ordering LOOK
        int colorIndex = cs.getIndexFromValue(val);
        g.setColor(cs.getColor(colorIndex));
        g.fill(gp);
      }
    }

    // odd y
    for (int y = 1; y < ny - 1; y += 2) {
      for (int x = 0; x < nx - 1; x++) {
        gp.reset();
        gp.moveTo((float) edgex.get(y, x + 1), (float) edgey.get(y, x + 1));
        gp.lineTo((float) edgex.get(y + 1, x), (float) edgey.get(y + 1, x));
        gp.lineTo((float) edgex.get(y + 2, x + 1), (float) edgey.get(y + 2, x + 1));
        gp.lineTo((float) edgex.get(y + 1, x + 1), (float) edgey.get(y + 1, x + 1));

        double val = data.getDouble(ima.set(y, x));   // ordering LOOK
        int colorIndex = cs.getIndexFromValue(val);
        g.setColor(cs.getColor(colorIndex));
        g.fill(gp);
      }
    }
  }

  /* draw using GeneralPath shape
 private GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 5);
 private Shape makeShape(double lon1, double lat1, double lon2, double lat2) {
   gp.reset();
   ProjectionPoint pt = drawProjection.latLonToProj( lat1, lon1);
   gp.moveTo( (float) pt.getX(), (float) pt.getY());

   ptP1.setLocation(pt);
   pt = drawProjection.latLonToProj( lat1, lon2);
   gp.lineTo( (float) pt.getX(), (float) pt.getY());
   if (drawProjection.crossSeam(ptP1, pt))
     return null;

   ptP1.setLocation(pt);
   pt = drawProjection.latLonToProj( lat2, lon2);
   gp.lineTo( (float) pt.getX(), (float) pt.getY());
   if (drawProjection.crossSeam(ptP1, pt))
     return null;

   ptP1.setLocation(pt);
   pt = drawProjection.latLonToProj( lat2, lon1);
   gp.lineTo( (float) pt.getX(), (float) pt.getY());
   if (drawProjection.crossSeam(ptP1, pt))
     return null;

   return gp;
 } */

  private void drawGridHoriz1D(java.awt.Graphics2D g, Array data, CoordinateAxis1D xaxis1D, CoordinateAxis1D yaxis1D) {
    int count = 0;

    int nx = (int) xaxis1D.getSize();
    int ny = (int) yaxis1D.getSize();

    /* how big is one pixel ?
if (debug) System.out.println("affine transform = "+g.getTransform());
if (debug) System.out.println("           scaleY= "+g.getTransform().getScaleY());
onePixel = Math.abs(1.5/g.getTransform().getScaleY());   // a little nudge more than 1 pixel
onePixel = 0;  */

    //// drawing optimizations
    sameProjection = drawProjection.equals(dataProjection);
    if (drawProjection.isLatLon()) {
      projectll = (LatLonProjection) drawProjection;
      double centerLon = projectll.getCenterLon();
      if (Debug.isSet("projection/LatLonShift")) System.out.println("projection/LatLonShift: gridDraw = " + centerLon);
    }

    // find the most common color and fill the entire area with it
    int modeColor = cs.getHistMax();
    cs.resetHist();
    IndexIterator iiter = data.getIndexIterator();
    while (iiter.hasNext()) {
      double val = iiter.getDoubleNext();
      cs.getIndexFromValue(val);                // accum in histogram
    }
    modeColor = cs.getHistMax();
    if (debugMiss) System.out.println("mode = " + modeColor + " sameProj= " + sameProjection);

    if (sameProjection) {
      count += drawRect(g, modeColor, xaxis1D.getCoordEdge(0), yaxis1D.getCoordEdge(0),
              xaxis1D.getCoordEdge(nx), yaxis1D.getCoordEdge(ny), drawProjection.isLatLon());

    } else if (useModeForProjections)
      drawPathShape(g, modeColor, xaxis1D, yaxis1D);

    debugPts = Debug.isSet("GridRenderer/showPts");

    // draw individual rects with run length
    Index imaH = dataH.getIndex();
    for (int y = 0; y < ny; y++) {
      double ybeg = yaxis1D.getCoordEdge(y);
      double yend = yaxis1D.getCoordEdge(y + 1);
      int thisColor = 0, lastColor = 0;
      int run = 0;
      int xbeg = 0;

      debugPts = debugPts && (y == 0);

      for (int x = 0; x < nx; x++) {
        try {
          double val = data.getDouble(imaH.set(y, x));
          thisColor = cs.getIndexFromValue(val);
        } catch (ArrayIndexOutOfBoundsException e) {
          System.out.println("bad");
          throw e;
        }

        if ((run == 0) || (lastColor == thisColor))  // same color - keep running
          run++;
        else {
          if (sameProjection) {
            if (lastColor != modeColor) // dont have to draw these
              count += drawRect(g, lastColor, xaxis1D.getCoordEdge(xbeg), ybeg, xaxis1D.getCoordEdge(x), yend, drawProjection.isLatLon());
          } else {
            //if (!useModeForProjections || (lastColor != modeColor)) // dont have to draw mode
            count += drawPathRun(g, lastColor, ybeg, yend, xaxis1D, xbeg, x);
          }
          xbeg = x;
        }
        lastColor = thisColor;
      }

      // get the ones at the end
      if (sameProjection) {
        if (lastColor != modeColor)
          count += drawRect(g, lastColor, xaxis1D.getCoordEdge(xbeg), ybeg, xaxis1D.getCoordEdge(nx), yend, drawProjection.isLatLon());
      } else {
        //if (!useModeForProjections || (lastColor != modeColor))
        count += drawPathRun(g, lastColor, ybeg, yend, xaxis1D, xbeg, nx - 1);
      }

      // if (debugPts) break;
    }
    if (debugHorizDraw) System.out.println("debugHorizDraw = " + count);
  }

  //// draw using Rectangle when possible
  private Rectangle2D rect = new Rectangle2D.Double();

  private int drawRectLatLon(Graphics2D g, int color, double lon1, double lat1, double lon2, double lat2) {
    g.setColor(cs.getColor(color));

    int count = 0;
    ProjectionRect[] rect = projectll.latLonToProjRect(lat1, lon1, lat2, lon2);
    for (int i = 0; i < 2; i++)
      if (null != rect[i]) {
        // if (Debug.isSet("drawRectLatLon") && rect[i].contains(0.0, 47.5)) System.out.println(rect[i]);
        g.fill(rect[i]);
        count++;
      }
    return count;
  }

  private int drawRect(Graphics2D g, int color, double w1, double h1, double w2, double h2, boolean useLatlon) {
    if (useLatlon)
      return drawRectLatLon(g, color, w1, h1, w2, h2);

    g.setColor(cs.getColor(color));
    double wmin = Math.min(w1, w2);
    double hmin = Math.min(h1, h2);
    double width = Math.abs(w1 - w2);
    double height = Math.abs(h1 - h2);
    rect.setRect(wmin, hmin, width, height);
    g.fill(rect);
    return 1;
  }

  private GeneralPath gpRun = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 25);

  private int drawPathRun(Graphics2D g, int color, double y1, double y2, CoordinateAxis1D xaxis, int x1, int x2) {
    int nx = (int) xaxis.getSize();
    if ((x1 < 0) || (x1 > nx) || (x2 < 0) || (x2 > nx) || (x1 > x2)) // from the recursion
      return 0;

    gpRun.reset();
    LatLonPoint llp = dataProjection.projToLatLon(xaxis.getCoordEdge(x1), y1);
    ProjectionPoint pt = drawProjection.latLonToProj(llp);
    if (debugPts) System.out.println("** moveTo = " + pt.getX() + " " + pt.getY());
    gpRun.moveTo((float) pt.getX(), (float) pt.getY());
    ptP1.setLocation(pt);

    for (int e = x1 + 1; e <= x2 + 1; e++) {
      llp = dataProjection.projToLatLon(xaxis.getCoordEdge(e), y1);
      pt = drawProjection.latLonToProj(llp);
      if (drawProjection.crossSeam(ptP1, pt)) { // break it in two & recurse
        int x = e - 1;  // which col has to be dropped ?
        if (debugPathShape) System.out.println("split1 at x = " + x + " " + x1 + " " + x2);
        int count = 0;
        count += drawPathRun(g, color, y1, y2, xaxis, x1, x - 1);
        count += drawPathRun(g, color, y1, y2, xaxis, x + 1, x2);
        return count;
      }
      if (debugPts) System.out.println("  lineTo = " + pt.getX() + " " + pt.getY());
      gpRun.lineTo((float) pt.getX(), (float) pt.getY());
      ptP1.setLocation(pt);
    }

    for (int e = x2 + 1; e >= x1; e--) {
      llp = dataProjection.projToLatLon(xaxis.getCoordEdge(e), y2);
      pt = drawProjection.latLonToProj(llp);
      if (drawProjection.crossSeam(ptP1, pt)) { // break it in two & recurse
        int x = (e == x2 + 1) ? x2 : e;  // which col has to be dropped ?
        if (debugPathShape) System.out.println("split2 at x = " + x + " " + x1 + " " + x2);
        int count = 0;
        count += drawPathRun(g, color, y1, y2, xaxis, x1, x - 1);
        count += drawPathRun(g, color, y1, y2, xaxis, x + 1, x2);
        return count;
      }
      if (debugPts) System.out.println("  lineTo = " + pt.getX() + " " + pt.getY());
      gpRun.lineTo((float) pt.getX(), (float) pt.getY());
      ptP1.setLocation(pt);
    }

    g.setColor(cs.getColor(color));
    try {
      g.fill(gpRun);
    } catch (Throwable e) {
      System.out.println("Exception in drawPathRun = " + e);
      return 0;
    }
    return 1;
  }

  private int drawPathShape(Graphics2D g, int color, CoordinateAxis1D xaxis, CoordinateAxis1D yaxis) {
    int count = 0;
    for (int y = 0; y < yaxis.getSize() - 1; y++) {
      double y1 = yaxis.getCoordEdge(y);
      double y2 = yaxis.getCoordEdge(y + 1);
      count += drawPathRun(g, color, y1, y2, xaxis, 0, (int) xaxis.getSize() - 1);
    }

    return count;
  }

  private void drawXORline(Graphics2D g, double x1, double y1, double x2, double y2) {
    gpRun.reset();
    gpRun.moveTo((float) x1, (float) y1);
    gpRun.lineTo((float) x2, (float) y2);

    //g.setXORMode(Color.black);
    g.setColor(Color.black);

    if (Double.isInfinite(x1) || Double.isInfinite(x2) || Double.isInfinite(y1) ||
            Double.isInfinite(y2))
      return;

    g.draw(gpRun);
    //g.setPaintMode();
  }

/*  private int drawPathShape(Graphics2D g, int color, CoordinateAxis xaxis, CoordinateAxis yaxis) {
    Point2D pt;
    gpRun.reset();
    int nx = xaxis.getNumElements();
    int ny = yaxis.getNumElements();
    boolean debugPathShape = true;
    int x, y;

    pt = drawProjection.latLonToProj( yaxis.getCoordValue(0), xaxis.getCoordValue(0));
    gpRun.moveTo( (float) pt.getX(), (float) pt.getY());
    ptP1.set(pt);

    y = 0;
    for (x=1; x<nx; x++) {
      pt = drawProjection.latLonToProj( yaxis.getCoordValue(y), xaxis.getCoordValue(x));
      gpRun.lineTo( (float) pt.getX(), (float) pt.getY());
      ptP1.set(pt);
      if (debugPathShape) System.out.println(x+" "+y+" "+pt+" "+drawProjection.crossSeam(ptP1, pt));
    }

    x = nx-1;
    for (y=0; y<ny; y++) {
      pt = drawProjection.latLonToProj( yaxis.getCoordValue(y), xaxis.getCoordValue(x));
      gpRun.lineTo( (float) pt.getX(), (float) pt.getY());
      ptP1.set(pt);
      if (debugPathShape) System.out.println(x+" "+y+" "+pt+" "+drawProjection.crossSeam(ptP1, pt));
    }

    y = ny-1;
    for (x=nx-1; x>=0; x--) {
      pt = drawProjection.latLonToProj( yaxis.getCoordValue(y), xaxis.getCoordValue(x));
      gpRun.lineTo( (float) pt.getX(), (float) pt.getY());
      ptP1.set(pt);
      if (debugPathShape) System.out.println(x+" "+y+" "+pt+" "+drawProjection.crossSeam(ptP1, pt));
    }

    x = 0;
    for (y=ny-1; y>=0; y--) {
      pt = drawProjection.latLonToProj( yaxis.getCoordValue(y), xaxis.getCoordValue(x));
      gpRun.lineTo( (float) pt.getX(), (float) pt.getY());
      ptP1.set(pt);
      if (debugPathShape) System.out.println(x+" "+y+" "+pt+" "+drawProjection.crossSeam(ptP1, pt));
    }

    g.setColor( cs.getColor(color));
    //g.fill(gpRun);
    return 1;
  }  */

/*  private void drawLine(Graphics2D g, double lat1, double lon1, double lat2, double lon2) {
    ptP1.set(drawProjection.latLonToProj( lat1, lon1));
    Point2D pt2 = drawProjection.latLonToProj( lat2, lon2);
    if (drawProjection.crossSeam(ptP1, pt2))
      if (debugSeam) System.out.println( "crossSeam: "+ ptP1+ " to "+ pt2);
    else {
      line2D.setLine( ptP1, pt2);
      g.draw( line2D);
    }
  } */

  //////// contouring

  private void drawContours(java.awt.Graphics2D g, Array hslice, AffineTransform dFromN) {
    // make ContourGrid object
    GridCoordSystem geocs = stridedGrid.getCoordinateSystem();
    CoordinateAxis1D xaxis = (CoordinateAxis1D) geocs.getXHorizAxis();
    CoordinateAxis1D yaxis = (CoordinateAxis1D) geocs.getYHorizAxis();
    double[] xedges = xaxis.getCoordValues();
    double[] yedges = yaxis.getCoordValues();

    int nlevels = cs.getNumColors();
    ArrayList levels = new ArrayList(nlevels);
    for (int i = 1; i < nlevels - 1; i++)
      levels.add(new Double(cs.getEdge(i)));

    ContourFeatureRenderer contourRenderer;
    long startTime = System.currentTimeMillis();
    try {
      ContourGrid conGrid = new ContourGrid((Array) hslice, levels, xedges, yedges, stridedGrid);
      contourRenderer = new ContourFeatureRenderer(conGrid, dataProjection);
    } catch (Exception e) {
      System.out.println("make Contours exception = " + e);
      e.printStackTrace(System.out);
      return;
    }
    if (Debug.isSet("timing/contourMake")) {
      long tookTime = System.currentTimeMillis() - startTime;
      System.out.println("timing/contourMake: " + tookTime * .001 + " seconds");
    }

    startTime = System.currentTimeMillis();
    try {
      contourRenderer.setProjection(drawProjection);
      contourRenderer.setColor(Color.black);
      contourRenderer.setShowLabels(drawContourLabels);
      contourRenderer.draw(g, dFromN);
    } catch (Exception e) {
      System.out.println("draw Contours exception = " + e);
      e.printStackTrace(System.err);
    }
    if (Debug.isSet("timing/contourDraw")) {
      long tookTime = System.currentTimeMillis() - startTime;
      System.out.println("timing/contourDraw: " + tookTime*.001 + " seconds");
    }
  }

}