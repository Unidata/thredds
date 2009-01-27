// $Id: GisFeatureRenderer.java 50 2006-07-12 16:30:06Z caron $
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
package thredds.viewer.gis;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import thredds.datamodel.gis.AbstractGisFeature;

import ucar.util.prefs.ui.Debug;

import java.awt.Color;
import java.awt.Shape;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Superclass for rendering collections of GisFeatures.
 *
 * @author: John Caron
 * @version: $Id: GisFeatureRenderer.java 50 2006-07-12 16:30:06Z caron $
 */
public abstract class GisFeatureRenderer implements thredds.viewer.ui.Renderer {
  private Color color = Color.blue;                   // default color of polylines
  protected ProjectionImpl displayProject = null;     // the current display Projection
  protected ArrayList shapeList = null;

  ////// this is what the subclasses have to implement (besides the constructor)
    /**
     * Preferred map area on opening for first time.
     * @return lat/lon bounding box that specifies preferred area.
     */
  public abstract LatLonRect getPreferredArea();
  protected abstract java.util.List getFeatures();      // collection of AbstractGisFeature

    // what projection is the data in? set to null if no Projection (no conversion)
    // assumes data projection doesnt change
  protected abstract ProjectionImpl getDataProjection();

  ///////////
  public java.awt.Color getColor() { return color; }
  public void setColor( Color color) { this.color = color; }
  public void setProjection(ProjectionImpl project) {
      displayProject = project;
      shapeList = null;
      //System.out.println("GisFeatureRenderer setProjection "+displayProject);

      //if (Debug.isSet("event.barf") && (displayProject instanceof LatLonProjection))
      //  throw new IllegalArgumentException();
  }

    /**
     * Draws all the features that are within the graphics clip rectangle,
     * using the previously set displayProjection.
     *
     * @param g the Graphics2D context on which to draw
     * @param pixelAT  transforms "Normalized Device" to Device coordinates
     */
  public void draw(java.awt.Graphics2D g, AffineTransform pixelAT) {
    g.setColor(color);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    g.setStroke(new java.awt.BasicStroke(0.0f));

    Rectangle2D clipRect = (Rectangle2D) g.getClip();
    Iterator siter = getShapes(g, pixelAT);
    while (siter.hasNext()) {
      Shape s = (Shape) siter.next();
      Rectangle2D shapeBounds = s.getBounds2D();
      if (shapeBounds.intersects(clipRect))
        g.draw(s);
    }
  }

  // get the set of shapes to draw, convert projections if need be
  protected Iterator getShapes(java.awt.Graphics2D g, AffineTransform normal2device) {
    if (shapeList != null)
      return shapeList.iterator();

    if(Debug.isSet("projection/LatLonShift"))
      System.out.println("projection/LatLonShift GisFeatureRenderer.getShapes called");

    ProjectionImpl dataProject = getDataProjection();

    // a list of GisFeatureAdapter-s
    List featList = getFeatures();

    shapeList = new ArrayList(featList.size());

    Iterator iter = featList.iterator();

    while (iter.hasNext())
        {
        AbstractGisFeature feature = (AbstractGisFeature) iter.next();
        Shape shape;
        if (dataProject == null)
            shape = feature.getShape();
        else if (dataProject.isLatLon())  {
            // always got to run it through if its lat/lon
            shape = feature.getProjectedShape(displayProject);
            //System.out.println("getShapes dataProject.isLatLon() "+displayProject);
        } else if (dataProject == displayProject) {
            shape = feature.getShape();
            //System.out.println("getShapes dataProject == displayProject");
        } else {
            shape = feature.getProjectedShape(dataProject, displayProject);
            //System.out.println("getShapes dataProject != displayProject");
        }

        shapeList.add(shape);
        }

    return shapeList.iterator();
  }

}

/* Change History:
   $Log: GisFeatureRenderer.java,v $
   Revision 1.4  2004/09/24 03:26:37  caron
   merge nj22

   Revision 1.3  2003/04/08 18:16:21  john
   nc2 v2.1

   Revision 1.2  2003/03/17 21:12:35  john
   new viewer

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:49  caron
   import sources

   Revision 1.9  2001/04/30 23:38:22  caron
   debug

   Revision 1.8  2000/08/18 04:15:24  russ
   Licensed under GNU LGPL.

   Revision 1.7  2000/05/26 21:19:16  caron
   new GDV release

   Revision 1.6  2000/05/26 19:54:58  wier
   minor reformatting

   Revision 1.5  2000/05/16 22:38:01  caron
   factor GisFeatureRenderer

   Revision 1.4  2000/03/01 19:31:24  caron
   setProjection bug

   Revision 1.3  2000/02/17 20:18:02  caron
   make printing work for zoom resolution maps

   Revision 1.2  2000/02/11 01:24:42  caron
   add getDataProjection()

   Revision 1.1  2000/02/10 17:45:11  caron
   add GisFeatureRenderer,GisFeatureAdapter

*/
