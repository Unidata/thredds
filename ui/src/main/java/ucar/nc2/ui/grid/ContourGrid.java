// $Id: ContourGrid.java 50 2006-07-12 16:30:06Z caron $
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

import ucar.ma2.*;       // for Array, Index, MAMath
import ucar.nc2.dt.GridDatatype;

import ucar.util.prefs.ui.Debug;

import java.awt.geom.*;  // for Point2D.Double
import java.util.*;      // for Iterator and ArrayList


/**
 * ContourGrid
 * Class to make an ArrayList of ContourFeature-s from a regular 2D data grid.
 * <p/>
 * Creates contours, and adds all the contours
 * to an ArrayList of ContourFeatures, given some input data grid.
 * Contour levels desired are an input argument list.
 * <p/>
 * Constructor input:
 * ucar.ma2.Array dataGrid:
 * the 2D grid of values at grid points to contour;
 * double []-s xPosition, yPosition: the list of coordinates of grid points
 * along x and y axes;
 * ArrayList allContourValues a list of the desired contour level values;
 * GeoGridImpl geogrid
 * <p/>
 * How it works:
 * Contouring is activated by calling the function getContourLines().
 * The contours for the dataGrid input to the cstr are made.
 * <p/>
 * internal variables
 * <p/>
 * direction is indicated by the char values N, S, E, and W,
 * indicating directions in the grid
 * <p/>
 * contourValues are the contour levels used to make contours; derived
 * from the input contour values to the constructor. If the input
 * array contourValues is of zero length, contour values are automatically
 * generated.
 * <p/>
 * xMaxInd and yMaxInd are the largest array indices
 * in x and y permitted for this
 * grid. One less than dimension.
 * <p/>
 * contourOnVertlEdge is an int array the size of the data grid,
 * with each value 0 or 1.  This is used
 * for an indication whether a contour of a certain level crosses between this
 * point and the next one of one larger y index.
 * This array is recalculated for each contour level.
 * <p/>
 * contourOnHorizEdge is similar indication whether a contour of a
 * certain level crosses between this
 * point and the next one of one larger x index.
 * These arrays are recalculated for each contour level.
 * <p/>
 * conLevel is the active contour level during computations.
 * <p/>
 * The algorithm works as follows.
 * <p/>
 * Each grid cell is identified by the lower left grid point (i,j).
 * The cell edge to the right of (i,j) is the horizontal edge for grid cell
 * (i,j) and the cell edge above (i,j) is the vertical edge.
 * i increases to the  right and j increases upward.
 * <p/>
 * Contouring  proceeds one level at a time, starting at the lowest level.
 * For each level the crossing indicators contourOnVertlEdge and
 * contourOnHorizEdge are set first.
 * <p/>
 * When a contour level value lies between the grid value at (i,j) and value
 * at (i+1,j) then contourOnHorizEdge(i,j) is set to 1; otherwise 0.
 * When a contour level value
 * lies between the grid value at (i,j) and the value at (i,j+1) then
 * contourOnVertlEdge(i,j) is set to 1; otherwise 0. This is done
 * by the function setupForContoursAt(double level). Note that only one
 * contour crossing for one level is allowed
 * between two grid points. This is consistent with the sampling of the actual
 * data field by the grid points,
 * though it is possible that higher data resolution could sometimes
 * show two or more contours
 * at a level passing between two of the grid points.
 * <p/>
 * Then the cell edges along the west edge of the grid are checked to see
 * if any contour crossings occur
 * (function searchWestEdge()).
 * <p/>
 * If a contour is found crossing the edge associated with the cell (i,j) the
 * function followContour(Dir, i, j) is called, where Dir is one of the
 * directions indicating a contour was found starting on the west edge.
 * <p/>
 * followContour follows this contour through the grid until it reaches its end
 * against a grid edge (it
 * can't close on itself since it started on an edge and only one crossing is
 * permitted at a cell edge).
 * As it works through the contour, contour positions are appended to the
 * current line.
 * <p/>
 * This contouring code works in the (i,j) main GRID INDEX system, and
 * contour positions have double (non-integer) values
 * in the main grid "index" units. But the code ends by computing
 * all contour positions in the x and y coord system input to the cstr.
 * <p/>
 * So as followContour works along the contour it finds the points where the
 * contour crossed each cell
 * edge, using the indicators contourOnVertlEdge and contourOnHorizEdge, and
 * the function
 * contourEdgeIntersection(side, int i, int j). The function
 * contourEdgeIntersection() returns the position where the contour crosses
 * the edge in main grid coordinates. The conversion is
 * made to desired coordinate units,
 * and that position is appended to the current contourLine.
 * <p/>
 * Having a position where a contour enters a main grid cell, the heading or
 * side where the contour
 * leaves the cell is returned by function directionToGoFrom(side, i, j).
 * Intermediate points inside the
 * main cell are found by contourEdgeIntersection()
 * which also returns the next main grid point
 * on the cell edge. This process is continued until
 * the contour reaches its end against an edge.
 * The functions followContour(...),
 * contourEdgeIntersection(...)& directionToGoFrom(...)
 * are the core of contouring.
 * <p/>
 * Having found all contours starting on the west edge of the main grid, the
 * south and east edges are
 * checked in the same way with functions searchSouthEdge() and
 * searchEastEdge(). Then the north
 * edges of all cells on the north edge, and all interior points, are checked
 * to find any contours not
 * yet found, including internal contours which close on themselves.
 * <p/>
 * All this was for one contour level. The process is repeated for
 * every contour level.
 * <p/>
 * Some tricks to contouring appear when details of how contours can cross a
 * cell are considered.  Normally a single contour for one level
 * comes in one side of a cell and goes out a different side. It
 * is possible for a data grid point to exactly equal the contour level, in
 * which case the side associated
 * with that level is not determined. In this case the data value is shifted by
 * adding ((gridmax - gridmin) /100000.0)
 * This is simpler than changing the code to deal with this case,
 * and has no observable effect on earth science data
 * displays which is presumed to always have a much larger data range than
 * ((gridmax - gridmin) /100000.0).
 * <p/>
 * Two contours of the same level may cross one cell, each crossing two
 * differing edges. This is the
 * case of a saddle point. Crossing point pairs are selected by choosing the
 * nearest crossing point to
 * the incoming point, in function directionToGoFrom.
 * <p/>
 * It does not appear possible to have three edges be crossed or touched by a
 * single contour. If you think otherwise, please try to find four data
 * values for cell corners which fit the case.
 *
 * @author wier
 * @version $Revision: 50 $ $Date: 2006-07-12 16:30:06Z $
 */
public class ContourGrid {
  private GridDatatype geogrid;
  private ucar.ma2.Array dataArray;
  private Index dgIndex;

  private ArrayList contourValues = new ArrayList(); // contour levels
  private ArrayList contourLines = new ArrayList(); // the contours made here

  private int [][] contourOnVertlEdge; // flag if a cell has a contour in it
  private int [][] contourOnHorizEdge; // ditto - crossing bottom or top
  private double [] xpositions;     // list of x positions along the grid
  private double [] ypositions;     // list of y positions of the grid
  private double conLevel;          // a working contour level
  private int xMaxInd;              // the data grid's max x index (not size)
  private int yMaxInd;                 // the data grid's max y index
  //private boolean dataismissing;    // flag if a missing data value is used
  private int dimX;
  private int numLevel;
  private double gridmax, gridmin;

  /**
   * Construct the ContourGrid object for this input data of
   * datagrid, contour level values, and x and y edge position values.
   * Supplied contour levels may be an empty object;
   * if so then this cstr will
   * create some reasonable levels for the data supplied.
   * <p/>
   * The object made contains the data used to make contours.
   *
   * @param dataGrid         the data grid of value
   * @param allContourValues list of level or values or contour lines
   * @param xPosition        the x position values of columns in the grid
   * @param yPosition        the y position values of rows in the grid
   * @param geogrid          a GeoGridImpl which is used for its missing data methods.
   */
  public ContourGrid(ucar.ma2.Array dataGrid, ArrayList allContourValues,
          double [] xPosition, double [] yPosition,
          GridDatatype geogrid) {
    //long t1 = System.currentTimeMillis();
    this.geogrid = geogrid;

    dgIndex = dataGrid.getIndex();

    this.dataArray = dataGrid;

    dimX = (dataGrid.getShape())[1];

    xpositions = xPosition;
    ypositions = yPosition;

    /* Determine the largest possible x and y index in the data grid,
* the dimension less 1; getShape returns an int[] */
    xMaxInd = (dataGrid.getShape())[0] - 1;
    yMaxInd = (dataGrid.getShape())[1] - 1;

    conLevel = 0.0;

    /* Determine max and min values in the data grid,
    * used here to determine valid contour levels to use.
    * JCaron changes 2/15/2001
    */
    MAMath.MinMax minmax = geogrid.getMinMaxSkipMissingData(dataGrid);
    gridmax = minmax.max;
    gridmin = minmax.min;

    /* if a grid with missing values, reset grid max and grid min
   if (dataismissing && (gridmax == missvalue || gridmin == missvalue)) {
       //   first reset to any value not the missing data value
       if (gridmax == missvalue)
           gridmax = gridmin;
       else if (gridmin == missvalue)
           gridmin = gridmax;
       // then search grid for valid max and min
       for (int i=0; i<=xMaxInd; i++)
           for (int j=0; j<=yMaxInd; j++) {
               double val = value(i,j);
               if (val != missvalue) {
                   if (val > gridmax)
                       gridmax=val;
                   else if (val < gridmin)
                       gridmin = val;
               }
           }
   } */

    if (Debug.isSet("contour/debugContours")) {
      System.out.println("  cstr: grid x dim = " + xMaxInd +
              "  y dim = " + yMaxInd);
      System.out.println("  cstr: input grid has x coord limits "
              + xpositions[0]
              + " to " + xpositions[yMaxInd]);
      System.out.println("  cstr: input grid has y coord limits "
              + ypositions[0]
              + " to " + ypositions[yMaxInd]);
      System.out.println("  cstr: grid max value = "
              + gridmax + "  grid min = " + gridmin);
    }

    /**
     * Determine contour level values to use.
     * Find *working* contour levels, the levels that will actually be
     * used in making contours in this object, which may not
     * include some supplied to the constructor.
     * If any contour values were supplied:
     */
    if (allContourValues.size() > 0) {
      //boolean spanBottom=false, spanTop=false;

      if (Debug.isSet("contour/debugContours"))
        System.out.println
                ("  Supplied contour levels are" + allContourValues);

      /* Loop over all supplied contour values;
* if value is contained between gridmin and gridmax, add to
* working array of contour values "contourValues".
* Check if supplied contour values cover the entire data range */
      for (int i = 0; i < allContourValues.size(); i++) {
        Double dob = (Double) allContourValues.get(i);
        double cv = dob.doubleValue();
        if ((cv >= gridmin) && (cv <= gridmax))
          contourValues.add(allContourValues.get(i));

        // indicate if the supplied values cover the data supplied
        //if (cv <= gridmin  && !spanBottom)
        //    spanBottom=true;
        //if (cv >= gridmax && !spanTop)
        //    spanTop=true;
      }

      /*
          // spanning not in any way mandatory, so don't worry about it;
          // the user may only want to look at part of the values.
              if (Debug.isSet("contour.debugContours") &&
            !geogrid.hasMissingData())
                  if (!spanTop || !spanBottom)
                      System.out.println
              (" Supplied contour values fail to span " +
         "the entire range of data values in the grid.");
        */
    } else  // if not contour levels were supplied
    {
      double interval;
      /* No contour values were provided as input to cstr
      * so have to make up suitable contour values.
      * THE DIVISOR SETS HOW MANY CONTOUR LEVELS ARE MADE HERE AND USED.
      * May be non-integer values, which is ok:
      * non-integer values accomodate data grid with a very small range
      * such as absolute vorticity with all
* |values| < 0.005.
      */
      interval = (gridmax - gridmin) / 10.0;

      /* Error trap: same grid values at all points in the grid
      * no contours possible - set indicator contour value of -999;
      */
      if (interval == 0.0) {
        Double dob = new Double(-999.0);
        contourValues.add(dob);
      } else {
        /* determine integer multipliers of interval */
        int highestmult = (int) (gridmax / interval);
        int lowestmult = (int) (gridmin / interval) + 1;

        for (int i = lowestmult; i <= highestmult; i++) {
          Double dob = new Double(i * interval);
          contourValues.add(dob);
        }
      }
    } // end find working contour levels

    // set size of arrays of flags of where contours cross grid edges
    contourOnVertlEdge = new int
            [contourValues.size()][(xMaxInd + 1) * (yMaxInd + 1)];

    contourOnHorizEdge = new int
            [contourValues.size()][(xMaxInd + 1) * (yMaxInd + 1)];

    //long t2 = System.currentTimeMillis();
    //long dt = t2-t1;
    //System.out.println("  cstr used "+dt+" ms elapsed");

    //for (int m=0; m<contourValues.size(); m++)
    //    System.out.println("  contour value "+
    //		       ((Double)contourValues.get(m)).doubleValue());

    // System.out.println(" Completed ContourGrid cstr. ");

  }
  //  end ContourGrid constructor


  /**
   * Make contour lines from the datagrid input
   * to the constructor of this object.
   * Before calling this routine,
   * first construct the ContourGrid object like this:
   * ContourGrid gridToContour(dataGrid, contourValues, xPosition, yPosition,
   * geoGridImpl);
   * then call gridToContour.getContourLines();
   *
   * @return an ArrayList of ContourFeature objects
   */
  public ArrayList getContourLines() {
    // for timing tests only:
    long t1 = 0, t2 = 0, dt = 0;

    ArrayList contourFeatureList = new ArrayList();

    if (Debug.isSet("contour/contourTiming"))
      t1 = System.currentTimeMillis();

    /* set flags at all contour - cell edge crossings */
    setupContourCrossings();

    /* For each contour level, find all of its contours in this grid. */
    for (int i = 0; i < contourValues.size(); i++) {
      conLevel = ((Double) contourValues.get(i)).doubleValue();
      numLevel = i;

      // check for flag of -999 contour level set in cstr indicating all
      // grid values are exactly the same and no contours are possible.
      if (conLevel == -999) {
        System.err.println
                ("  No contours possible: all same grid values");
        ArrayList empty = new ArrayList();
        return empty;
      }

      searchWestEdge();
      searchSouthEdge();
      searchEastEdge();
      searchNorthEdge();
      sweepfromWest();
      sweepfromTop();

      /** contouring is done for this contour level; make a
       *  a ContourFeature which is an AbstractGisFeature
       *  from the ContourLine ArrayList just completed. */
      ContourFeature oneLevelLines = new ContourFeature(contourLines);

      contourFeatureList.add(oneLevelLines);

      // reset contourLines to none, ready for the next level
      contourLines.clear();
    }


    if (Debug.isSet("contour/contourTiming")) {
      t2 = System.currentTimeMillis();
      dt = t2 - t1;
      System.out.println("  getContourLines used " + dt + " ms elapsed");
    }

    return contourFeatureList;
  } // end getContourLines


  /**
   * Look at every grid point (lower left cell corner) and set the contour
   * crossing indicators for the associated cell edges for this contour level
   * There is one vertical edge and one horizontal edge
   * associated with each grid point.
   */
  private void setupContourCrossings() {
    int m, i, j;
    double clevel, v1 = 0.0, v2;
    boolean test;

    // create working array of contour levels
    // to save multiple access
    double [] clevels = new double[contourValues.size()];
    for (m = 0; m < contourValues.size(); m++)
      clevels[m] = ((Double) contourValues.get(m)).doubleValue();

    // for each row, look at each pair of grid point values;
    // see if a contour crossing of any value occurs there
    for (j = 0; j <= yMaxInd; j++) {
      v1 = value(0, j);  // call method "value(i,j)"
      for (i = 0; i < xMaxInd; i++) {
        v2 = value(i + 1, j);
        boolean v1IsMissingData = geogrid.isMissingData(v1);
        boolean v2IsMissingData = geogrid.isMissingData(v2);

        // check all contour levels - do not assume they are in order!
        for (m = 0; m < contourValues.size(); m++) {
          clevel = clevels[m];

          // first set crossing indicator to "off"
          contourOnHorizEdge[m][j + i * dimX] = 0;

          if (!v1IsMissingData && !v2IsMissingData) {
            if (((clevel >= v1 && clevel < v2) ||
                    (clevel >= v2 && clevel < v1))) {
              contourOnHorizEdge[m][j + i * dimX] = 1;
            }
          }
        }
        v1 = v2; // save repeated value for next step
      }  // loop on i
    }

    // same for all vertical grid edges defined by pairs of grid points
    for (i = 0; i <= xMaxInd; i++) {
      v1 = value(i, 0);
      for (j = 0; j < yMaxInd; j++) {
        v2 = value(i, j + 1);
        boolean v1IsMissingData = geogrid.isMissingData(v1);
        boolean v2IsMissingData = geogrid.isMissingData(v2);

        // check all contour levels - do not assume they are in order!
        for (m = 0; m < contourValues.size(); m++) {
          clevel = clevels[m];

          // first set crossing indicator to "off"
          contourOnVertlEdge[m][j + i * dimX] = 0;

          if (!v1IsMissingData && !v2IsMissingData) {
            if (((clevel >= v1 && clevel < v2) ||
                    (clevel >= v2 && clevel < v1)))
              contourOnVertlEdge[m][j + i * dimX] = 1;
          }
        }
        v1 = v2;
      }
    }

  }    // end setupContourCrossings


  /**
   * A grid data access convenience method used only for contouring.
   * Return the (possibly slightly adjusted) grid value at grid point i,j.
   * <p/>
   * Use ONLY for contour generation - special adjustment is possible -
   * If the value at a gridpoint is equal to the contour level, then the
   * value returned is offset slightly.  This avoids the special case
   * where a contour intersects at a gridpoint (cell corner) exactly,
   * and consequently which edge has the intersection is undefined.
   *
   * @return double
   */
  private double value(int i, int j) {
    // convert from array of any type to double.
    // AbstractArray does the type conversion to double from whatever.
    double truevalue = dataArray.getDouble(dgIndex.set(i, j));

    // check for missing value is now done in setupContourCrossings()

    if (truevalue != conLevel)
      return truevalue;
    else
      return truevalue + ((gridmax - gridmin) / 100000.0);
  }


  // searchWestEdge()
  // Check each cell along the west edge for contours crossing at this level.
  // Create the complete contour, if a crossing is found.
  // west edge is composed of cells with i=0.
  // Turn off contour crosing indicator after it is used.
  private void searchWestEdge() {
    int i, j;

    i = 0; // west edge of grid is where i == 0
    for (j = (yMaxInd - 1); j >= 0; j--) {
      if (contourOnVertlEdge[numLevel][j + i * dimX] == 1)
      // found an intersection on W edge of grid
      {
        ContourLine cline = new
                ContourLine(followContour('W', i, j), (double) conLevel);
        contourLines.add(cline);
        contourOnVertlEdge[numLevel][j + i * dimX] = 0;
      }
    }
  }


  // searchEastEdge()
  // Check each cell along the east edge for a contour crossing this level.
  //  Create the complete contour, if a crossing is found.
  //  east edge is composed of cells with i = xMaxInd the max index in x.
  //
  private void searchEastEdge() {
    int i, j;

    i = xMaxInd; // the east edge is at hgihest x index
    for (j = 0; j < yMaxInd; j++) {

      if (contourOnVertlEdge[numLevel][j + i * dimX] == 1)
      // found an intersection on east edge of grid; the west edge
      // of cell at i,j which is the east edge of cell (i-1, j)
      {
        ContourLine cline = new
                ContourLine(followContour('E', i - 1, j), (double) conLevel);
        contourLines.add(cline);
        contourOnVertlEdge[numLevel][j + i * dimX] = 0;
      }
    }
  }


  /**
   * Check each cell along the north grid edge for a contour crossing
   * at this level. Create the complete contour, if a crossing is found.
   * <p/>
   * "north" edge is composed of cell edges with j equal to yMaxInd.
   */
  private void searchNorthEdge() {
    int i, j;

    j = yMaxInd;  // top row of cells has index j = yMaxInd
    for (i = 0; i < xMaxInd; i++) {

      if (contourOnHorizEdge[numLevel][j + i * dimX] == 1)
      // found an intersection on the bottom edge of the
      // topmost row of grid cells
      // this will be the south side of that cell, which is
      // the N side of cell (i,j-1)
      {
        ContourLine cline =
                new ContourLine(followContour('N', i, j - 1), (double) conLevel);
        contourLines.add(cline);
        contourOnHorizEdge[numLevel][j + i * dimX] = 0;
      }
    }
  }

  /**
   * Check each cell along the south grid edge for a contour crossing
   * at this level. Create the complete contour, if a crossing is found.
   * <p/>
   * south edge is composed of cell edges with j equal to 0.
   */
  private void searchSouthEdge() {
    int i, j;

    j = 0;
    for (i = 0; i < xMaxInd; i++) {

      if (contourOnHorizEdge[numLevel][j + i * dimX] == 1)
      // found an intersec on S edge
      {
        ContourLine cline =
                new ContourLine(followContour('S', i, j), (double) conLevel);
        contourLines.add(cline);
        contourOnHorizEdge[numLevel][j + i * dimX] = 0;
      }
    }
  }


  /**
   * Search the north row of grid cells, and interior of the grid,
   * for horizontal cell edge contour crossings, complete the contours,
   * and add them.
   * This will find all contours except those with contour-cell edge
   * intersections not on the top of a grid cell, such as
   * pure horizontal contours, and ones starting on the right or
   * left edge of the grid.
   * north row of cells is where j = yMaxInd-1
   */
  private void sweepfromTop() {
    int i, j;

    for (j = 1; j < yMaxInd; j++)
      for (i = 0; i <= xMaxInd; i++) {

        if (contourOnHorizEdge[numLevel][j + i * dimX] == 1) {
          // there is an intersection on S edge of cell;
          // follow this contour line to its end.
          ContourLine cline =
                  new ContourLine(followContour('S', i, j),
                          (double) conLevel);
          contourLines.add(cline);
          // set false (turn off indicator of crossing);
          contourOnHorizEdge[numLevel][j + i * dimX] = 0;
        }
      }
  }


  /**
   * Working from west edge, check all vertical cell edges for
   * an indicator of a contour crossing at the current level value.
   * If found, construct the contour by following it using the method
   * followContour(), and add it to the list of contours.
   */
  private void sweepfromWest() {
    int i, j;

    for (i = 0; i < xMaxInd; i++)
      for (j = 0; j < yMaxInd; j++)
        if (contourOnVertlEdge[numLevel][j + i * dimX] == 1) {
          // there is an intersection on W edge of cell;
          // follow this contour line to its end.
          ContourLine cline =
                  new ContourLine(followContour('W', i, j),
                          (double) conLevel);
          contourLines.add(cline);
          // set false (turn off indicator of crossing):
          contourOnVertlEdge[numLevel][j + i * dimX] = 0;
        }
  }


  /**
   * Creates a new contour, a sequence of position points, as an
   * ArrayList of Point2D.Double.
   * Follow the contour which starts at cell i,j, on cell edge "firstside"
   * to its end, appending positions.
   * <p/>
   * If contour closes on itself, the last point is the same as the first
   * point. Both are included. Otherwise there will be a gap in the line.
   *
   * @return an ArrayList of Point2D.Double
   */
  private ArrayList followContour(char firstSide, int i, int j) {
    // the object to return:
    ArrayList cLinePts = new ArrayList();

    char heading;

    // for positions in main grid coordinates
    Point2D.Double mainGridPoint = new Point2D.Double();
    Point2D.Double startGridPoint = new Point2D.Double();

    // to hold contour line coords coordinates
    double oldx, oldy;

    char startSide = firstSide;

    if (Debug.isSet("contour/debugContours")) {
      System.out.println("  NEW CONTOUR starting at " + i + "," + j);
      System.out.println("   startside =" + startSide);
    }

    // find position on cell edge where contour is first detected
    // in main grid units

    startGridPoint = contourEdgeIntersection(startSide, i, j);

    if (Debug.isSet("contour/debugContours"))
      System.out.println("    point 1 x = " + (float) startGridPoint.getX()
              + "  y = " + (float) startGridPoint.getY());

    // Add the first position to the path.
    Point2D.Double dob = new Point2D.Double(startGridPoint.getX(),
            startGridPoint.getY());
    cLinePts.add(dob);

    // keep hold of point found on the contour for later use
    oldx = startGridPoint.getX();
    oldy = startGridPoint.getY();

    // Note: crossing indicator is not turned off here for the first point
    // on the contour, so that closed contours can truly close on start point.

    boolean match = false, onedge = false, atMissing = false;

    //  work through grid, appending points to the contour, until
    // it ends on an edge or it closes on itself.
    do {
      // get the next side of the this cell where this contour crosses
      heading = directionToGoFrom(startSide, i, j);

      if (heading == startSide) {
        // contour at dead end: contour entered cell surrounded by missing
        // data, or otherwise ended but not at a grid edge.
        if (cLinePts.size() <= 1) {
          ArrayList empty = new ArrayList();
          //System.out.println("  unexpected end of contour ");
          return empty;
        } else
          return cLinePts;
      }

      if (Debug.isSet("contour/debugContours"))
        System.out.println("    new cell; exit side = " + heading);

      // extend the contour across this cell, to the next crossing point
      // on a different side of the grid cell:
      mainGridPoint = contourEdgeIntersection(heading, i, j);

      // append this contour point on cell edge to the contour
      Point2D.Double dob2 =
              new Point2D.Double(mainGridPoint.getX(), mainGridPoint.getY());
      cLinePts.add(dob2);

      if (Debug.isSet("contour/debugContours"))
        System.out.println("    next position on contour = " +
                (float) mainGridPoint.getX() + "," + (float) mainGridPoint.getY());

      oldx = mainGridPoint.getX();
      oldy = mainGridPoint.getY();

      // change indicators to apply to new cell,the next cell on contour
      if (heading == 'N') {
        j = j + 1;
        startSide = 'S';

        // turn off crossing indicator on incoming edge(S) of new cell
        contourOnHorizEdge[numLevel][j + i * dimX] = 0;
      } else if (heading == 'S') {
        j = j - 1;
        startSide = 'N';

        // turn off crossing indicator on incoming edge(N) of new cell
        contourOnHorizEdge[numLevel][(j + 1) + i * dimX] = 0;
      } else if (heading == 'E') {
        i = i + 1;
        startSide = 'W';

        // turn off crossing indicator on incoming edge(W) of new cell
        contourOnVertlEdge[numLevel][j + i * dimX] = 0;
      } else if (heading == 'W') {
        i = i - 1;
        startSide = 'E';

        // turn off crossing indicator on incoming edge(E) of new cell
        contourOnVertlEdge[numLevel][j + (i + 1) * dimX] = 0;
      }

      if (Debug.isSet("contour/debugContours")) {
        System.out.println("    new startside =" + startSide + "  i,j =" +
                i + "," + j + "  old heading =" + heading
                + " xMaxInd=" + xMaxInd + "  yMaxInd=" + yMaxInd);
      }

      /* "while" checks if contour closed on its first point,
* or if the contour has come to an end against the edge of grid;
* if not, process another step along the contour line. */

      /*    test if have got back to starting position */
      if (mainGridPoint.getX() == startGridPoint.getX() &&
              mainGridPoint.getY() == startGridPoint.getY())
        match = true;

      /*     test if have hit an edge of the grid */
      if ((heading == 'N' && j >= yMaxInd) || (heading == 'S' && j < 0)
              || (heading == 'E' && i >= xMaxInd) || (heading == 'W' && i < 0))
        onedge = true;

    } while (!match && !onedge && !atMissing);

    if (Debug.isSet("contour/debugContours")) {
      if (match)
        System.out.println("  END contour at " + i + ", " + j + " by match");
      if (onedge)
        System.out.println("  END contour at " + i + ", " + j + " on edge");
      //System.out.println(" ");
    }

    return cLinePts;
  }   // end followContour


  /**
   * return the position of edge-contour crossing,
   * on the side of the cell called "side"
   * <p/>
   * Input cell coordinates (i,j) are main grid indices (integers).
   * Output is a "(x,y)" in main grid units system but usually
   * the coordinates are not integers. Probably never in fact.
   * Note the returned positions are in the coordinates for x and y input to cstr.
   *
   * @return Point2D.Double
   */
  private Point2D.Double contourEdgeIntersection(char side, int i, int j) {
    Point2D.Double intersection = new Point2D.Double();

    switch (side) {
      case 'N':
        intersection.setLocation(xpositions[i] +
                (xpositions[i + 1] - xpositions[i])
                        * interpPosition(conLevel, value(i, j + 1), value(i + 1, j + 1)),
                ypositions[j + 1]);
        break;

      case 'S':
        intersection.setLocation(xpositions[i] + (xpositions[i + 1] - xpositions[i])
                * interpPosition(conLevel, value(i, j), value(i + 1, j)), ypositions[j]);
        break;

      case 'E':
        intersection.setLocation(xpositions[i + 1],
                ypositions[j] + (ypositions[j + 1] - ypositions[j])
                        * interpPosition(conLevel, value(i + 1, j), value(i + 1, j + 1)));
        break;

      case 'W':
        intersection.setLocation(xpositions[i],
                ypositions[j] + (ypositions[j + 1] - ypositions[j])
                        * interpPosition(conLevel, value(i, j), value(i, j + 1)));
        break;

      default:
        System.out.println(
                "Impossible direction in contourEdgeIntersection" +
                        "  i=" + i + "  j=" + j + " direction=" + side);
        ;
    }

    return intersection;
  }


  /**
   * A contour has been found entering this cell on side called "startSide";
   * return the name of the side of this cell where the contour goes out.
   * <p/>
   * If fails to find the next or another crossing indicator, return the
   * input values which means failure since contour cannot enter and exit
   * the same cell on the same side, in this software design.
   * <p/>
   * Note special code if all three sides besides the startside are crossed:
   * you have a saddle point with 2 contours of the same value
   * crossing this cell.
   * In that case select other crossing nearest the incoming.
   * Otherwise this is just a simple case of finding the other side crossed.
   *
   * @return char
   */
  private char directionToGoFrom(char startSide, int i, int j) {

    switch (startSide) // switch on incoming cell edge
    {
      case 'N':
        if (contourOnVertlEdge[numLevel][j + i * dimX] == 1
                && contourOnHorizEdge[numLevel][j + i * dimX] == 1
                && contourOnVertlEdge[numLevel][j + (i + 1) * dimX] == 1)
        // saddle point; pick the adjacent edge closest to where
        // the contour crosses this edge.
        {
          if (Math.abs(conLevel - value(i, j + 1))
                  > Math.abs(conLevel - value(i + 1, j + 1)))
            return 'E';
          else
            return 'W';
        } else {
          if (contourOnVertlEdge[numLevel][j + i * dimX] == 1)
            return 'W';
          else if (contourOnVertlEdge[numLevel][j + (i + 1) * dimX] == 1)
            return 'E';
          else if (contourOnHorizEdge[numLevel][j + i * dimX] == 1)
            return 'S';
        }
        break;

      case 'S':
        if (contourOnVertlEdge[numLevel][j + (i + 1) * dimX] == 1
                && contourOnHorizEdge[numLevel][(j + 1) + i * dimX] == 1
                && contourOnVertlEdge[numLevel][j + i * dimX] == 1) {
          if (Math.abs(conLevel - value(i + 1, j))
                  > Math.abs(conLevel - value(i, j)))
            return 'W';
          else
            return 'E';
        } else {
          if (contourOnVertlEdge[numLevel][j + i * dimX] == 1)
            return 'W';
          else if (contourOnVertlEdge[numLevel][j + (i + 1) * dimX] == 1)
            return 'E';
          else if (contourOnHorizEdge[numLevel][(j + 1) + i * dimX] == 1)
            return 'N';
        }
        break;

      case 'E':
        if (contourOnHorizEdge[numLevel][(j + 1) + i * dimX] == 1
                && contourOnVertlEdge[numLevel][j + i * dimX] == 1
                && contourOnHorizEdge[numLevel][j + i * dimX] == 1) {
          if (Math.abs(conLevel - value(i + 1, j + 1))
                  > Math.abs(conLevel - value(i + 1, j)))
            return 'S';
          else
            return 'N';
        } else {
          if (contourOnHorizEdge[numLevel][(j + 1) + i * dimX] == 1)
            return 'N';
          else if (contourOnHorizEdge[numLevel][j + i * dimX] == 1)
            return 'S';
          else if (contourOnVertlEdge[numLevel][j + i * dimX] == 1)
            return 'W';
        }
        break;

      case 'W':
        if (contourOnHorizEdge[numLevel][j + i * dimX] == 1
                && contourOnVertlEdge[numLevel][j + (i + 1) * dimX] == 1
                && contourOnHorizEdge[numLevel][(j + 1) + i * dimX] == 1) {
          if (Math.abs(conLevel - value(i, j))
                  > Math.abs(conLevel - value(i, j + 1)))
            return 'N';
          else
            return 'S';
        } else {
          if (contourOnHorizEdge[numLevel][j + i * dimX] == 1)
            return 'S';
          else if (contourOnHorizEdge[numLevel][(j + 1) + i * dimX] == 1)
            return 'N';
          else if (contourOnVertlEdge[numLevel][j + (i + 1) * dimX] == 1)
            return 'E';
        }
        break;

      default:
        System.out.println("Impossible direction " + startSide +
                " in directionToGoFrom()");

    } // end switch

    if (Debug.isSet("contour/debugContours"))
      System.out.println("   Contour does not exit from cell i=" + i +
              " j=" + j + "   start side is " + startSide +
              "  level = " + conLevel);

    return startSide;
  }  //  end directionToGoFrom


  /**
   * Return the fractional distance along a grid cell edge where a
   * contour of value cv crosses the cell edge whose end points
   * have value v1 and v2. cv should be between v1 and v2.
   * <p/>
   * Position is found by simple linear interpolation between the two values
   * at the end of the cell edge.
   *
   * @return double
   */
  private double interpPosition(double cv, double v1, double v2) {
    /** (Might improve slightly by nonlinear fit to
     * four points along grid row or column. Probably would be big increase
     * in time required, for an undetectable improvement in
     * quality of contours and is not justified by data resolution anyway.)
     */
    if (v2 == v1) {
      return 0.5;
    } else
      return ((cv - v1) / (v2 - v1));
  }

}   /*  end class ContourGrid */
