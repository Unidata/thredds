/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.catalog;

import thredds.client.catalog.Service;
import ucar.nc2.constants.FeatureType;

/**
 * Describe
 *
 * @author caron
 * @since 3/16/2016.
 */
public interface AllowedServicesIF {
  Service getStandardServices(FeatureType featType);
  boolean isAThreddsDataset(String filename);
}
