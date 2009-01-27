// $Id: ContourFeatureRenderer.java 50 2006-07-12 16:30:06Z caron $
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

import ucar.unidata.geoloc.*;
import thredds.datamodel.gis.GisFeatureAdapter;

import ucar.unidata.util.Format;
import thredds.ui.FontUtil;
import ucar.util.prefs.ui.Debug;

import java.awt.RenderingHints;
import java.util.*;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Contour rendering.
 *
 * @author caron
 * @version $Revision: 50 $ $Date: 2006-07-12 16:30:06Z $
 */
public class ContourFeatureRenderer extends thredds.viewer.gis.GisFeatureRenderer {

  private ProjectionImpl dataProjection;
  private GisFeatureAdapter features;
  private ArrayList contourList = new ArrayList();  // list of ContourFeature-s
  private boolean ShowLabels;

  /**
   * cstr
   */
  public ContourFeatureRenderer(ContourGrid conGrid,
          ProjectionImpl dataProjection) {
    this.dataProjection = dataProjection;
    ShowLabels = true;
    contourList = conGrid.getContourLines();
  }

  /**
   * set switch whether contours labels are desired.
   * default true is set in cstr.
   */
  public void setShowLabels(boolean showlabels) {
    ShowLabels = showlabels;
  }


  public LatLonRect getPreferredArea() {
    return null;
  }

  protected java.util.List getFeatures() {
    // collection of ContourFeature-s
    return contourList;
  }

  protected ProjectionImpl getDataProjection() {
    return dataProjection;
  }


  /**
   * Overrides the GisFeatureRenderer draw() method, to draw contours
   * and with contour labels.
   *
   * @param g                  the Graphics2D context on which to draw
   * @param deviceFromNormalAT transforms "Normalized Device" to Device coordinates
   */
  public void draw(java.awt.Graphics2D g, AffineTransform deviceFromNormalAT) {
    /* OLD WAY
  // make & set desired font for contour label.
  // contour label size in "points" is last arg
  Font font1 = new Font("Helvetica", Font.PLAIN, 25);
  // make a transform to un-flip the font
  AffineTransform unflip = AffineTransform.getScaleInstance(1, -1);
  Font font = font1.deriveFont(unflip);
  g.setFont(font);  */

    /* from original GisFeatureRenderer method draw: */
    g.setColor(Color.black);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);
    g.setStroke(new java.awt.BasicStroke(0.0f));

    Rectangle2D clipRect = (Rectangle2D) g.getClip();
    Iterator siter = getShapes(g, deviceFromNormalAT);

    // draw the contours
    while (siter.hasNext()) {
      Shape s = (Shape) siter.next();
      Rectangle2D shapeBounds = s.getBounds2D();
      if (shapeBounds.intersects(clipRect))
        g.draw(s);
    }

    // additional code beyond GisFeatureRenderer method draw():
    // render contour value for this contour line. */
    if (ShowLabels) {
      Font f = FontUtil.getStandardFont(10).getFont();
      Font saveFont = g.getFont();

      // use world coordinates for position, but draw in "normal" coordinates
      // so that the symbols stay the same size
      AffineTransform deviceFromWorldAT = g.getTransform();
      AffineTransform normalFromWorldAT;
      // transform World to Normal coords:
      //    normalFromWorldAT = deviceFromNormalAT-1 * deviceFromWorldAT
      try {
        normalFromWorldAT = deviceFromNormalAT.createInverse();
        normalFromWorldAT.concatenate(deviceFromWorldAT);
      } catch (java.awt.geom.NoninvertibleTransformException e) {
        System.out.println(" ContourFeatureRenderer: NoninvertibleTransformException on " + deviceFromNormalAT);
        return;
      }
      g.setTransform(deviceFromNormalAT); // so g now wants "normal coords"
      g.setFont(f);

      siter = getShapes(g, deviceFromNormalAT);
      Iterator CViter = contourList.iterator();
      Point2D worldPt = new Point2D.Double();
      Point2D normalPt = new Point2D.Double();
      float [] coords = new float[6];
      while (siter.hasNext()) {
        Shape s = (Shape) siter.next();
        double contValue = ((ContourFeature) CViter.next()).getContourValue();

        // get position xpos,ypos on this contour where to put label
        // in current world coordinates in the current Shape s.
        PathIterator piter = s.getPathIterator(null);
        //int cs, count=-1;  original
        int cs, count = 12;
        while (! piter.isDone()) {
          count++;
          if (count % 25 == 0) {    // for every 25th position on this path
            cs = piter.currentSegment(coords);

            if (cs == PathIterator.SEG_MOVETO || cs == PathIterator.SEG_LINETO) {
              worldPt.setLocation(coords[0], coords[1]);
              normalFromWorldAT.transform(worldPt, normalPt);  // convert to normal
              // render the contour value to the screen
              g.drawString(Format.d(contValue, 4), (int) normalPt.getX(), (int) normalPt.getY());
            }
          }
          piter.next();
        } // while not done
      }  // end while shape.hasNext()

      // restore original transform and font
      g.setTransform(deviceFromWorldAT);
      g.setFont(saveFont);

    }  // end if ShowLabels == true


    if (Debug.isSet("contour/doLabels")) {
      // get iterator to the class member ArrayList of GisFeature-s
      Iterator iter = contourList.iterator();
      while (iter.hasNext()) {
        //ContourFeature cf = iter.next();
        System.out.println(" ContourFeatureRenderer: contour value = "
                + ((ContourFeature) iter.next()).getContourValue());
      }
    }
  }
}

/* Change History:
   $Log: ContourFeatureRenderer.java,v $
   Revision 1.1  2004/09/30 00:33:42  caron
   *** empty log message ***

   Revision 1.5  2004/09/25 00:09:44  caron
   add images, thredds tab

   Revision 1.4  2004/09/24 03:26:42  caron
   merge nj22

   Revision 1.3  2003/04/08 18:16:25  john
   nc2 v2.1

   Revision 1.2  2003/03/17 21:12:41  john
   new viewer

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:52  caron
   import sources

   Revision 1.3  2001/04/30 23:40:22  caron
   fix event bug

   Revision 1.2  2000/08/18 04:16:25  russ
   Licensed under GNU LGPL.

   Revision 1.1  2000/06/30 16:31:19  caron
   minor revs for GDV release

   Revision 1.3  2000/05/26 19:53:26  wier
   new draw() method renders contout labels

   Revision 1.2  2000/05/25 21:03:25  wier
   using the new ContourFeature

   Revision 1.1  2000/05/16 22:38:00  caron
   factor GisFeatureRenderer

*/
