/* Copyright */
package thredds.server.ncss.params;

import java.util.List;

/**
 * Parameters specific to ncss point
 *
 * @author caron
 * @since 4/29/2015
 */
public class NcssPointParamsBean  extends NcssParamsBean {

  //// station only
	private List<String> stns;

  public List<String> getStns() {
    return stns;
  }

  public void setStns(List<String> stns) {
    this.stns = stns;
  }

  public boolean hasStations() {
    return stns != null && !stns.isEmpty();
  }

}
