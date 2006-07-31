// $Id: $
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

package thredds.catalog;

import org.jdom.Element;

import java.net.URI;

/**
 * InvDatasetFmrc deals with datasetFmrc elementss.
 *
 * @author caron
 */
public class InvDatasetFmrc extends InvDatasetImpl {
  private boolean init = false;
  private String path;

  public InvDatasetFmrc(InvDataset parent, String name, String path) {
    super(parent, name);
    this.path = path;
  }

  public boolean hasAccess() {
    return false;
  }

  public boolean hasNestedDatasets() {
    return true;
  }

      /** Get Datasets. This triggers a read of the referenced catalog the first time its called.*/
  public java.util.List getDatasets() {
    construct();
    return super.getDatasets();
  }

  private synchronized void construct() {
    if (init)
      return;

    Element ncml = getNcmlElement();
    String ok = (ncml != null) ? "ok" : "no";
    addProperty(new InvProperty("ncml", ok));

    String id = getID();

    InvDatasetImpl ds = new InvDatasetImpl(this, "Best Time Series Dataset");
    if (id != null) ds.setID(id+"/best");
    ds.setUrlPath(path+"/best");
    datasets.add( ds);

    ds = new InvDatasetImpl(this, "Forecast Model Run Datasets");
    if (id != null) ds.setID(id+"/runs");
    ds.setUrlPath(path+"/runs");
    datasets.add( ds);

    ds = new InvDatasetImpl(this, "Constant Forecast Date Datasets");
    if (id != null) ds.setID(id+"/forecast");
    ds.setUrlPath(path+"/forecast");
    datasets.add( ds);

    ds = new InvDatasetImpl(this, "Constant Forecast Offset Datasets");
    if (id != null) ds.setID(id+"/offset");
    ds.setUrlPath(path+"/offset");
    datasets.add( ds);

    init = true;
  }

  
}
