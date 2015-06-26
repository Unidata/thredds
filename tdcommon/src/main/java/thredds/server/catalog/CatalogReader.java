/* Copyright */
package thredds.server.catalog;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 6/17/2015
 */
public interface CatalogReader {

  ConfigCatalog getFromAbsolutePath(String absPath) throws IOException;

}
