// $Id: WorldMapBean.java,v 1.3 2004/09/28 21:39:11 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package thredds.viewer.gis.worldmap;

/** Wraps the default WorldMap into a MapBean */

public class WorldMapBean extends thredds.viewer.gis.MapBean {
  private thredds.viewer.ui.Renderer rend;

  public WorldMapBean() {
  }

  public thredds.viewer.ui.Renderer getRenderer() {
     if (rend == null)
       rend = new WorldMap();
     return rend;
  }

  public javax.swing.ImageIcon getIcon() {
    return thredds.ui.BAMutil.getIcon("WorldMap", true);
  }
  public String getActionName() { return "WorldMap"; }
  public String getActionDesc() { return "use World Map"; }

}