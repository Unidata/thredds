/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.catalog;

import javax.annotation.concurrent.Immutable;

/**
 * Encapsolates the datasetRoot element
 *
 * @author caron
 * @since 1/15/2015
 */
@Immutable
public class DatasetRootConfig {
  public final String path;
  public final String location;

  public DatasetRootConfig(String path, String location) {
    this.path = path;
    this.location = location;
  }

  public String getPath() {
    return path;
  }

  public String getLocation() {
    return location;
  }
}
