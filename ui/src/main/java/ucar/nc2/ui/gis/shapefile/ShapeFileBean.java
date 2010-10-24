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
package ucar.nc2.ui.gis.shapefile;

import ucar.nc2.ui.gis.MapBean;
import ucar.nc2.ui.util.Renderer;
import ucar.nc2.ui.util.Resource;
import ucar.nc2.ui.widget.BAMutil;

/** Wraps shapefile maps into a MapBean
 *
 * @author John Caron
 */

public class ShapeFileBean extends MapBean {
  private String name, desc, iconName, shapefileName;
  private Renderer rend = null;

     /** contructor for a specific shapefile */
  public ShapeFileBean(String name, String desc, String iconName, String shapefileName) {
    this.name = name;
    this.desc = desc;
    this.iconName = iconName;
    this.shapefileName = shapefileName;
  }

  public Renderer getRenderer() {
    if (rend == null) fetchMap();
    return rend;
  }

  private void fetchMap() {
    long startTime = System.currentTimeMillis();
    java.io.InputStream is = Resource.getFileResource( shapefileName);
    if (is == null) {
      System.err.println("ShapeFileBean read failed on resource " + shapefileName);
    } else {
     rend = EsriShapefileRenderer.factory(shapefileName, is);
    }

    if (ucar.util.prefs.ui.Debug.isSet("timing.readShapefile")) {
      long tookTime = System.currentTimeMillis() - startTime;
      System.out.println("timing.readShapefile: " + tookTime*.001 + " seconds");
    }
  }

  public javax.swing.ImageIcon getIcon() {
    return BAMutil.getIcon(iconName, true);
  }
  public String getActionName() { return name; }
  public String getActionDesc() { return desc; }

}

