/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog.builder;

import thredds.client.catalog.Access;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.Service;
import ucar.nc2.constants.DataFormatType;

/**
 * build immutable access element
 *
 * @author caron
 * @since 1/8/2015
 */
public class AccessBuilder {
  DatasetBuilder dataset;
  String urlPath;
  Service service;
  String dataFormatS;
  long dataSize;

  public AccessBuilder(DatasetBuilder dataset, String urlPath, Service service, String dataFormatS, long dataSize) {
    this.dataset = dataset;
    this.urlPath = urlPath;
    this.service = service;
    this.dataFormatS = dataFormatS;
    this.dataSize = dataSize;
  }

  public AccessBuilder(DatasetBuilder dataset, Access from) {
    this.dataset = dataset;
    this.urlPath = from.getUrlPath();
    this.service = from.getService();
    this.dataFormatS = from.getDataFormatName();
    this.dataSize = from.getDataSize();
  }

  public Access makeAccess(Dataset dataset) {
    return new Access(dataset, urlPath, service, dataFormatS, dataSize);
  }
}
