/* Copyright */
package thredds.server.catalog;

/**
 * Describe
 *
 * @author caron
 * @since 6/9/2015
 */
public class CatalogScan {

  String location, watch;

  public String getLocation() {
    return location;
  }

  public String getWatch() {
    return watch;
  }

  public CatalogScan(String location, String watch) {
    this.location = location;
    this.watch = watch;
  }
}
