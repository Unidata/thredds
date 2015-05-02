/* Copyright */
package ucar.nc2.ft.remote;

import ucar.nc2.Attribute;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;

import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 5/1/2015
 */
public class CdmrGridAdapter {
  GridDataset proxy;
  public CdmrGridAdapter(GridDataset proxy) {
    this.proxy = proxy;
  }

  String name;
  List<GridDatatype> grids;
  List<Attribute> atts;

  public String getName() {
    return proxy.getLocation();
  }

  public List<GridDatatype> getGrids() {
    return proxy.getGrids();
  }

  public List<Attribute> getAttributes() {
    return proxy.getGlobalAttributes();
  }
}
