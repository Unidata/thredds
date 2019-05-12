/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.coverage2;

import ucar.ma2.*;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.ui.grid.ColorScale;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.Format;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.Debug;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Formatter;

/**
 * ft2.coverage widget for displaying using Java2D API.
 * more or less the view in MVC
 *
 * @author John
 * @since 12/27/12
 */
public class CoverageRenderer {
  // draw state
  private boolean drawGrid = true;
  private boolean drawGridLines = true;
  private boolean drawContours = false;
  private boolean drawContourLabels = false;
  private boolean drawBB = false;
  private boolean isNewField = true;

  private ColorScale colorScale = null;
  private ColorScale.MinMaxType dataMinMaxType = ColorScale.MinMaxType.horiz;
  private ProjectionImpl drawProjection = null;    // current drawing Projection
  private ProjectionImpl dataProjection = null;    // current data Projection

  // data stuff
  private DataState dataState;
  private GeoReferencedArray dataH;
  private Array geodata; // reduced to 2D
  private double useLevel;
  private int wantLevel = -1, wantSlice = -1, wantTime = -1, horizStride = 1;   // for next draw()
  private int wantRunTime = -1, wantEnsemble = -1;
  private int lastLevel = -1, lastTime = -1, lastStride = -1;   // last data read
  private int lastRunTime = -1, lastEnsemble = -1;   // last data read
  private Coverage lastGrid = null;

  // drawing optimization
  private boolean useModeForProjections = false; // use colorMode optimization for different projections
  private boolean sameProjection = true;
  private LatLonProjection projectll;       // special handling for LatLonProjection

  private static final boolean debugHorizDraw = false, debugMiss = false;
  private boolean debugPts = false, debugPathShape = true;

  /**
   * constructor
   */
  public CoverageRenderer(PreferencesExt store) {
    //rects[0] = new ProjectionRect();
  }

  ///// bean properties

  /* get the current ColorScale */
  public ColorScale getColorScale() {
    return colorScale;
  }

  /* set the ColorScale to use */
  public void setColorScale(ColorScale cs) {
    this.colorScale = cs;
  }

  /* set the ColorScale data min/max type */
  public void setDataMinMaxType(ColorScale.MinMaxType type) {
    if (type != dataMinMaxType) {
      dataMinMaxType = type;
    }
  }

  /* set the Grid */
  public DataState setCoverage(CoverageCollection coverageDataset, Coverage grid) {
    this.dataState = new DataState(coverageDataset, grid);
    this.lastGrid = null;
    isNewField = true;
    return this.dataState;
  }

  /* get the current data projection */
  public ProjectionImpl getDataProjection() {
    return dataProjection;
  }

  public void setDataProjection(ProjectionImpl dataProjection) {
    this.dataProjection = dataProjection;
  }

  /* get the current display projection */
  public ProjectionImpl getDisplayProjection() {
    return drawProjection;
  }

  /* set the Projection to use for drawing */
  public void setViewProjection(ProjectionImpl project) {
    drawProjection = project;
  }

  /* set the Projection to use for drawing */
  public void setDrawBB(boolean drawBB) {
    this.drawBB = drawBB;
  }

  /* set whether grid should be drawn */
  public void setDrawGridLines(boolean drawGrid) {
    this.drawGridLines = drawGrid;
  }

  /* set whether countours  should be drawn */
  public void setDrawContours(boolean drawContours) {
    this.drawContours = drawContours;
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

  /* set what runtime slice to draw */
  public void setRunTime(int runtime) {
    wantRunTime = runtime;
  }

  /* set what ensemble slice to draw */
  public void setEnsemble(int ensemble) {
    wantEnsemble = ensemble;
  }

  /* set what y-z slice to draw */
  public void setSlice(int slice) {
    wantSlice = slice;
  }

  public void setHorizStride(int horizStride) {
    this.horizStride = horizStride;
  }

  /*
   * Get the data value at this projection (x,y) point.
   *
   * @param loc : point in display projection coordinates (plan view)
   * @return String representation of value
   */
  public String getXYvalueStr(ProjectionPoint loc) {
    if ((lastGrid == null) || (geodata == null))
      return "";

    // convert to dataProjection, where x and y are orthogonal
    if (!sameProjection) {
      LatLonPoint llpt = drawProjection.projToLatLon(loc);
      loc = dataProjection.latLonToProj(llpt);
    }

    // find the grid indexes
    HorizCoordSys hcs = lastGrid.getCoordSys().getHorizCoordSys();
    Optional<HorizCoordSys.CoordReturn> opt = hcs.findXYindexFromCoord(loc.getX(), loc.getY());

    // get value, construct the string
    if (!opt.isPresent())
      return opt.getErrorMessage();
    else {
      HorizCoordSys.CoordReturn cr = opt.get();
      try {
        Index imaH = geodata.getIndex();
        double dataValue = geodata.getDouble(imaH.set(cr.y, cr.x));
        // int wantz = (geocs.getZAxis() == null) ? -1 : lastLevel;
        return makeXYZvalueStr(dataValue, cr);
      } catch (Exception e) {
        return "error " + cr.x + " " + cr.y;
      }
    }
  }

  private String makeXYZvalueStr(double value, HorizCoordSys.CoordReturn cr) {
    String val = lastGrid.isMissing(value) ? "missing value" : Format.d(value, 6);
    Formatter sbuff = new Formatter();
    sbuff.format("%s %s", val, lastGrid.getUnitsString());
    sbuff.format(" @ (%f,%f)", cr.xcoord, cr.ycoord);
    sbuff.format("  [%d,%d]", cr.x, cr.y);
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

  private GeoReferencedArray readHSlice(int level, int time, int ensemble, int runtime) {

    /* make sure x, y exists
    CoverageCS gcs = useG.getCoordinateSystem();
    CoordinateAxis xaxis = gcs.getXHorizAxis();
    CoordinateAxis yaxis = gcs.getYHorizAxis();
    if ((xaxis == null) || (yaxis == null))    // doesnt exist
      return null;
    if ((xaxis.getSize() <= 1) || (yaxis.getSize() <= 1)) // LOOK ??
      return null;   */

    // make sure we need new one
    if (dataState.grid.equals(lastGrid) && (time == lastTime) && (level == lastLevel) && (horizStride == lastStride)
            && (ensemble == lastEnsemble) && (runtime == lastRunTime))
      return dataH; // nothing changed

    // get the data slice
    //dataH = useG.readDataSlice(runtime, ensemble, time, level, -1, -1);
    SubsetParams subset = new SubsetParams();
    if (level >= 0 && dataState.zaxis != null) {
      double levelVal = dataState.zaxis.getCoordMidpoint(level);
      subset.set(SubsetParams.vertCoord, levelVal);
    }
    if (time >= 0 && dataState.taxis != null) {
      double timeVal = dataState.taxis.getCoordMidpoint(time);
      CalendarDate date = dataState.taxis.makeDate(timeVal);
      subset.set(SubsetParams.time, date);
    }
    if (runtime >= 0 && dataState.rtaxis != null) {
      double rtimeVal = dataState.rtaxis.getCoordMidpoint(runtime);
      CalendarDate date = dataState.rtaxis.makeDate(rtimeVal);
      subset.set(SubsetParams.runtime, date);
    }
    if (ensemble >= 0 && dataState.ensaxis != null) {
      double ensVal = dataState.ensaxis.getCoordMidpoint(ensemble);
      subset.set(SubsetParams.ensCoord, ensVal);
    }
    if (horizStride != 1)
      subset.setHorizStride( horizStride);

    try {
      dataH = dataState.grid.readData(subset);
      geodata = dataH.getData().reduce(); // get rid of n=1 dimensions

    } catch (IOException | InvalidRangeException e) {
      e.printStackTrace();
    }

    lastGrid = dataState.grid;
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
    }

    if (debugArrayShape) {
      System.out.println("Horiz shape = ");
      for (int i = 0; i < dataH.getRank(); i++)
        System.out.println("   shape = " + dataH.getShape()[i]);
    } */

    return dataH;
  }

 /*  private Array makeVSlice(Coverage g, int vSlice, int time, int ensemble, int runtime) {
    // make sure we have x, z
    CoverageCS gcs = g.getCoordinateSystem();
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
      System.out.println("GridRender.readHSlice Error reading netcdf file= " + e);
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
  }   */

  //////////// Renderer stuff

  /**
   * returns null
   * public ucar.unidata.geoloc.LatLonRect getPreferredArea() {
   * /* if (dataProjection != null)
   * return dataProjection.getPreferredArea();
   * else
   * return null;
   * }
   * <p>
   * /* private java.awt.Color color, backColor;
   * public void setBackgroundColor(java.awt.Color backColor) { this.backColor = backColor;}
   * public void setColor(java.awt.Color color) { this.color = color;}
   * public java.awt.Color getColor() { return color; }
   */

  // set colorscale limits, missing data
  private void setColorScaleParams() {
    if (dataMinMaxType == ColorScale.MinMaxType.hold && !isNewField)
      return;
    isNewField = false;

    GeoReferencedArray dataArr = readHSlice(wantLevel, wantTime, wantEnsemble, wantRunTime);

    //else
    //  dataArr = makeVSlice(stridedGrid, wantSlice, wantTime, wantEnsemble, wantRunTime);

    if (dataArr != null) {
      MAMath.MinMax minmax = MAMath.getMinMaxSkipMissingData(dataArr.getData(), dataState.grid);
      colorScale.setMinMax(minmax.min, minmax.max);
      colorScale.setGeoGrid(dataState.grid);
    }
  }

  /*
   * Do the rendering to the given Graphics2D object.
   *
   * @param g      Graphics2D object: has clipRect and AffineTransform set.
   * @param dFromN transforms "Normalized Device" to Device coordinates
   *
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

    CoverageCS geocs = stridedGrid.getCoordinateSystem();
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
        ContourGrid conGrid = new ContourGrid(dataV.transpose(0, 1), levels, yedges, zedges, stridedGrid);
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
  } */

  /**
   * Do the rendering to the given Graphics2D object.
   *
   * @param g      Graphics2D object: has clipRect and AffineTransform set.
   * @param dFromN transforms "Normalized Device" to Device coordinates
   */
  public void renderPlanView(java.awt.Graphics2D g, AffineTransform dFromN) {
    if ((dataState.grid == null) || (colorScale == null) || (drawProjection == null))
      return;

    if (!drawGrid && !drawContours)
      return;

    // no anitaliasing
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

    dataH = readHSlice(wantLevel, wantTime, wantEnsemble, wantRunTime);
    if (dataH == null)
      return;

    setColorScaleParams();

    if (drawGrid)
      drawGridHoriz(g, dataH);
    //if (drawContours)
    //  drawContours(g, dataH.transpose(0, 1), dFromN);
    if (drawGridLines)
      drawGridLines(g, dataH);
    if (drawBB)
      drawGridBB(g, this.dataState.coverageDataset.getLatlonBoundingBox());
  }

  private boolean drawGridBB(Graphics2D g, LatLonRect latLonRect) {
    g.setColor(Color.BLACK);
    rect.setRect(latLonRect.getLonMin(), latLonRect.getLatMin(), latLonRect.getWidth(), latLonRect.getHeight());
    g.draw(rect);
    return true;
  }

  private void drawGridHoriz(java.awt.Graphics2D g, GeoReferencedArray geo) {
    CoverageCoordSys csData = geo.getCoordSysForData();
    HorizCoordSys hcs = csData.getHorizCoordSys();
    if (!hcs.isLatLon2D()) {
      drawGridHorizRegular(g, geo);
      return;
    }

    // 2D case
    Array data = geo.getData();
    data = data.reduce();
    if (data.getRank() != 2)
      throw new IllegalArgumentException("must be 2D");
    Index ima = data.getIndex();

    LatLonAxis2D xaxis2D = hcs.getLonAxis2D();
    LatLonAxis2D yaxis2D = hcs.getLatAxis2D();

    /* String stag = geocs.getHorizStaggerType();
    if (stag != null && stag.equals(CDM.ARAKAWA_E)) {
      drawGridHorizRotated(g, data, xaxis2D, yaxis2D);
      return;
    } */

    ArrayDouble.D2 edgex = (ArrayDouble.D2) xaxis2D.getCoordBoundsAsArray();
    ArrayDouble.D2 edgey = (ArrayDouble.D2) yaxis2D.getCoordBoundsAsArray();

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
        int colorIndex = colorScale.getIndexFromValue(val);
        g.setColor(colorScale.getColor(colorIndex));
        g.fill(gp);
      }
    }

  }

  private void drawGridLines(java.awt.Graphics2D g, GeoReferencedArray geo) {
    CoverageCoordSys geocs = geo.getCoordSysForData();
    LatLonAxis2D lataxis = geocs.getHorizCoordSys().getLatAxis2D();
    LatLonAxis2D lonaxis = geocs.getHorizCoordSys().getLonAxis2D();

    if (lataxis == null || lonaxis == null)
      return;

    ArrayDouble.D2 edgex = (ArrayDouble.D2) lonaxis.getCoordBoundsAsArray();
    ArrayDouble.D2 edgey = (ArrayDouble.D2) lataxis.getCoordBoundsAsArray();

    GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 5);
    g.setColor(Color.BLACK);

    int[] shape = lataxis.getShape(); // should both be the same
    int ny = shape[0];
    int nx = shape[1];

    for (int y = 0; y < ny + 1; y += 10) {
      gp.reset();
      for (int x = 0; x < nx + 1; x++) {
        if (x == 0)
          gp.moveTo((float) edgex.get(y, x), (float) edgey.get(y, x));
        else
          gp.lineTo((float) edgex.get(y, x), (float) edgey.get(y, x));
      }
      g.draw(gp);
    }

    for (int x = 0; x < nx + 1; x += 10) {
      gp.reset();
      for (int y = 0; y < ny + 1; y++) {
        if (y == 0)
          gp.moveTo((float) edgex.get(y, x), (float) edgey.get(y, x));
        else
          gp.lineTo((float) edgex.get(y, x), (float) edgey.get(y, x));
      }
      g.draw(gp);
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

  private void drawGridHorizRegular(java.awt.Graphics2D g, GeoReferencedArray array) {
    int count = 0;

    CoverageCoordSys gsys = array.getCoordSysForData();
    CoverageCoordAxis1D xaxis = (CoverageCoordAxis1D) gsys.getXAxis();
    CoverageCoordAxis1D yaxis = (CoverageCoordAxis1D) gsys.getYAxis();
    Array data = array.getData().reduce();
    if (data.getRank() != 2) {
      System.out.printf("drawGridHorizRegular Rank equals %d, must be 2%n", data.getRank());
      return;
    }

    int nx = xaxis.getNcoords();
    int ny = yaxis.getNcoords();

    //// drawing optimizations
    sameProjection = drawProjection.equals(dataProjection);
    if (drawProjection.isLatLon()) {
      projectll = (LatLonProjection) drawProjection;
      double centerLon = projectll.getCenterLon();
      if (Debug.isSet("projection/LatLonShift")) System.out.println("projection/LatLonShift: gridDraw = " + centerLon);
    }

    // find the most common color and fill the entire area with it
    colorScale.resetHist();
    IndexIterator iiter = array.getData().getIndexIterator();
    while (iiter.hasNext()) {
      double val = iiter.getDoubleNext();
      colorScale.getIndexFromValue(val);                // accum in histogram
    }
    int modeColor = colorScale.getHistMax();
    if (debugMiss) System.out.println("mode = " + modeColor + " sameProj= " + sameProjection);

    if (sameProjection) {
      double xmin = Math.min(xaxis.getCoordEdge1(0), xaxis.getCoordEdgeLast());
      double xmax = Math.max(xaxis.getCoordEdge1(0), xaxis.getCoordEdgeLast());
      double ymin = Math.min(yaxis.getCoordEdge1(0), yaxis.getCoordEdgeLast());
      double ymax = Math.max(yaxis.getCoordEdge1(0), yaxis.getCoordEdgeLast());

      // pre color the drawing area with the most used color
      count += drawRect(g, modeColor, xmin, ymin, xmax, ymax, drawProjection.isLatLon());

    } else if (useModeForProjections)
      drawPathShape(g, modeColor, xaxis, yaxis);

    debugPts = Debug.isSet("GridRenderer/showPts");

    // draw individual rects with run length
    Index imaH = data.getIndex();
    for (int y = 0; y < ny; y++) {
      double ybeg = yaxis.getCoordEdge1(y);
      double yend = yaxis.getCoordEdge2(y);

      int thisColor, lastColor = 0;
      int run = 0;
      int xbeg = 0;

      for (int x = 0; x < nx; x++) {
        double val = data.getDouble(imaH.set(y, x));
        thisColor = colorScale.getIndexFromValue(val);

        if ((run == 0) || (lastColor == thisColor)) { // same color - keep running
          run++;
        } else {
          if (sameProjection) {
            if (lastColor != modeColor) // dont have to draw these
              count += drawRect(g, lastColor, xaxis.getCoordEdge1(xbeg), ybeg, xaxis.getCoordEdge2(x), yend, drawProjection.isLatLon());
          } else {
            if (!useModeForProjections || (lastColor != modeColor)) // dont have to draw mode
              count += drawPathRun(g, lastColor, ybeg, yend, xaxis, xbeg, x-1, debugPts);
          }
          xbeg = x;
        }
        lastColor = thisColor;
      }

      // get the ones at the end
      if (sameProjection) {
        if (lastColor != modeColor)
          count += drawRect(g, lastColor, xaxis.getCoordEdge1(xbeg), ybeg, xaxis.getCoordEdgeLast(), yend, drawProjection.isLatLon());
      } else {
        if (!useModeForProjections || (lastColor != modeColor))
          count += drawPathRun(g, lastColor, ybeg, yend, xaxis, xbeg, nx - 1, false); // needed ?
      }

      // if (debugPts) break;
    }
    if (debugHorizDraw) System.out.println("debugHorizDraw = " + count);
  }

  //// draw using Rectangle when possible
  private Rectangle2D rect = new Rectangle2D.Double();

  private int drawRectLatLon(Graphics2D g, int color, double lon1, double lat1, double lon2, double lat2) {
    g.setColor(colorScale.getColor(color));

    int count = 0;
    ProjectionRect[] rects = projectll.latLonToProjRect(lat1, lon1, lat2, lon2);
    for (int i = 0; i < 2; i++)
      if (null != rects[i]) {
        ProjectionRect r2 = rects[i];
        Rectangle2D.Double r = new Rectangle2D.Double(r2.getX(), r2.getY(), r2.getWidth(), r2.getHeight());
        g.fill(r);
        count++;
      }
    return count;
  }

  private int drawRect(Graphics2D g, int color, double w1, double h1, double w2, double h2, boolean useLatlon) {
    if (useLatlon)
      return drawRectLatLon(g, color, w1, h1, w2, h2);

    g.setColor(colorScale.getColor(color));
    double wmin = Math.min(w1, w2);
    double hmin = Math.min(h1, h2);
    double width = Math.abs(w1 - w2);
    double height = Math.abs(h1 - h2);
    rect.setRect(wmin, hmin, width, height);
    g.fill(rect);
    return 1;
  }

  private int drawPathShape(Graphics2D g, int color, CoverageCoordAxis1D xaxis, CoverageCoordAxis1D yaxis) {
    int count = 0;
    for (int y = 0; y < yaxis.getNcoords() - 1; y++) {
      double y1 = yaxis.getCoordEdge1(y);
      double y2 = yaxis.getCoordEdge2(y);
      count += drawPathRun(g, color, y1, y2, xaxis, 0, xaxis.getNcoords() - 1, false);
    }

    return count;
  }

  private GeneralPath gpRun = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 25);

  private int drawPathRun(Graphics2D g, int color, double y1, double y2, CoverageCoordAxis1D xaxis, int x1, int x2, boolean debugPts) {
    int nx = xaxis.getNcoords();
    if ((x1 < 0) || (x2 < 0) || (x2 > nx) || (x1 > x2)) // from the recursion
      return 0;

    int count = 0;
    gpRun.reset();

    // first point
    LatLonPoint llp = dataProjection.projToLatLon(xaxis.getCoordEdge1(x1), y1);
    ProjectionPoint pt = drawProjection.latLonToProj(llp);
    if (debugPts) System.out.printf("** moveTo = x1=%d (%f, %f)%n", x1, pt.getX(), pt.getY());
    gpRun.moveTo((float) pt.getX(), (float) pt.getY());

    ProjectionPointImpl ptP1 = new ProjectionPointImpl();
    ptP1.setLocation(pt);

    for (int e = x1; e <= x2; e++) {
      llp = dataProjection.projToLatLon(xaxis.getCoordEdge2(e), y1);
      pt = drawProjection.latLonToProj(llp);
 /*   if (drawProjection.crossSeam(ptP1, pt)) { // break it in two & recurse
        // int x = e - 1;  // which col has to be dropped ?
        if (debugPathShape) System.out.println("split1 at x = " + e + " " + x1 + " " + x2);
        int count = 0;
        count += drawPathRun(g, color, y1, y2, xaxis, x1, e - 1);
        count += drawPathRun(g, color, y1, y2, xaxis, e + 1, x2);
        return count;
      } */
      if (debugPts) System.out.printf("%d x2=%d lineTo = (%f, %f)%n", count++, e, pt.getX(), pt.getY());
      gpRun.lineTo((float) pt.getX(), (float) pt.getY());
      ptP1.setLocation(pt);
    }

    for (int e = x2; e >= x1; e--) {
      llp = dataProjection.projToLatLon(xaxis.getCoordEdge2(e), y2);
      pt = drawProjection.latLonToProj(llp);
/*    if (drawProjection.crossSeam(ptP1, pt)) { // break it in two & recurse
        int x = (e == x2 + 1) ? x2 : e;  // which col has to be dropped ?
        if (debugPathShape) System.out.println("split2 at x = " + x + " " + x1 + " " + x2);
        int count = 0;
        count += drawPathRun(g, color, y1, y2, xaxis, x1, x - 1);
        count += drawPathRun(g, color, y1, y2, xaxis, x + 1, x2);
        return count;
      } */
      if (debugPts) System.out.printf("%d x2=%d lineTo = (%f, %f)%n", count++, e, pt.getX(), pt.getY());
      gpRun.lineTo((float) pt.getX(), (float) pt.getY());
      ptP1.setLocation(pt);
    }

    // finish
    llp = dataProjection.projToLatLon(xaxis.getCoordEdge1(x1), y2);
    pt = drawProjection.latLonToProj(llp);
    if (debugPts) System.out.printf("%d (%d,y2) lineTo = [%f, %f]%n", count, x1, pt.getX(), pt.getY());
    gpRun.lineTo((float) pt.getX(), (float) pt.getY());

    g.setColor( colorScale.getColor(color));
    try {
      g.fill(gpRun);
    } catch (Throwable e) {
      System.out.println("Exception in drawPathRun = " + e);
      return 0;
    }
    return 1;
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

  /* private void drawContours(java.awt.Graphics2D g, Array hslice, AffineTransform dFromN) {
    // make ContourGrid object
    CoverageCS geocs = stridedGrid.getCoordinateSystem();
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
  }  */

}
