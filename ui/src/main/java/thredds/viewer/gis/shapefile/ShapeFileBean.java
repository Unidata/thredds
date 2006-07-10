// $Id: ShapeFileBean.java,v 1.4 2004/09/28 21:39:10 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.viewer.gis.shapefile;

import thredds.util.Resource;
import thredds.viewer.ui.Renderer;

/** Wraps shapefile maps into a MapBean
 *
 * @author John Caron
 * @version $Id: ShapeFileBean.java,v 1.4 2004/09/28 21:39:10 caron Exp $
 */

public class ShapeFileBean extends thredds.viewer.gis.MapBean {
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
    return thredds.ui.BAMutil.getIcon(iconName, true);
  }
  public String getActionName() { return name; }
  public String getActionDesc() { return desc; }

}

/* Change History:
   $Log: ShapeFileBean.java,v $
   Revision 1.4  2004/09/28 21:39:10  caron
   *** empty log message ***

   Revision 1.3  2004/09/24 03:26:38  caron
   merge nj22

   Revision 1.2  2004/02/20 05:02:55  caron
   release 1.3

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.2  2002/04/29 22:31:01  caron
   add displayable name

   Revision 1.1.1.1  2002/02/26 17:24:50  caron
   import sources
*/
