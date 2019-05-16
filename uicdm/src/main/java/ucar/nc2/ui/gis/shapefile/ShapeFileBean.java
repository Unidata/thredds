/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.gis.shapefile;

import ucar.nc2.ui.gis.MapBean;
import ucar.nc2.ui.util.Renderer;
import ucar.ui.util.Resource;
import ucar.ui.widget.BAMutil;

/**
 * Wraps shapefile maps into a MapBean
 *
 * @author John Caron
 */

public class ShapeFileBean extends MapBean {
  private String name, desc, iconName, shapefileName;
  private Renderer rend = null;

  /**
   * contructor for a specific shapefile
   */
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
    java.io.InputStream is = Resource.getFileResource(shapefileName);
    if (is == null) {
      System.err.println("ShapeFileBean read failed on resource " + shapefileName);
    } else {
      rend = EsriShapefileRenderer.factory(shapefileName, is);
    }

    if (ucar.ui.prefs.Debug.isSet("timing.readShapefile")) {
      long tookTime = System.currentTimeMillis() - startTime;
      System.out.println("timing.readShapefile: " + tookTime * .001 + " seconds");
    }
  }

  public javax.swing.ImageIcon getIcon() {
    return BAMutil.getIcon(iconName, true);
  }

  public String getActionName() {
    return name;
  }

  public String getActionDesc() {
    return desc;
  }

}

