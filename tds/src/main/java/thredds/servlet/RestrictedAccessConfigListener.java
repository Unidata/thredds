package thredds.servlet;

import thredds.catalog.InvCatalog;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetImpl;

/**
 * Listen for DataRootHandler configuration events and register any restricted access datasets.
 *
 * Extracted from DataRootHandler for use elsewhere. [ERD - 2008-08-29]
 *
 * @author John Caron
 * @since 4.0
 */
public class RestrictedAccessConfigListener
        implements DataRootHandler.ConfigListener
{
  volatile boolean initializing;

  public RestrictedAccessConfigListener() {
    initializing = false;
  }

  public void configStart() {
    this.initializing = true;
  }

  public void configEnd() {
    this.initializing = false;
  }

  public void configCatalog( InvCatalog catalog) {
  }

  public void configDataset( InvDataset dataset) {
    // check for resource control
    if (dataset.getRestrictAccess() != null)
      DatasetHandler.putResourceControl((InvDatasetImpl) dataset);
  }
}
