/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.gis.worldmap;

import ucar.nc2.ui.gis.MapBean;
import ucar.nc2.ui.util.Renderer;
import ucar.ui.widget.BAMutil;

/** Wraps the default WorldMap into a MapBean */

public class WorldMapBean extends MapBean {
  private Renderer rend;

  public WorldMapBean() {
  }

  public Renderer getRenderer() {
     if (rend == null)
       rend = new WorldMap();
     return rend;
  }

  public javax.swing.ImageIcon getIcon() {
    return BAMutil.getIcon("nj22/WorldMap", true);
  }
  public String getActionName() { return "WorldMap"; }
  public String getActionDesc() { return "use World Map"; }

}