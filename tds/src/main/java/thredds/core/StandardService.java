/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.core;

import thredds.client.catalog.ServiceType;

/**
 * These are the built-in services of the TDS
 *
 * @author caron
 * @since 4/29/2015
 */
public enum StandardService {
  catalogRemote(ServiceType.Catalog, "/catalog/"),
  cdmRemote(ServiceType.CdmRemote, "/cdmremote/"),
  cdmrFeatureGrid(ServiceType.CdmrFeature, "/cdmrfeature/grid/"),
  cdmrFeaturePoint(ServiceType.CdmrFeature, "/cdmrfeature/point/"),
  dap4(ServiceType.DAP4, "/dap4/"),
  httpServer(ServiceType.HTTPServer, "/fileServer/"),
  jupyterNotebook(ServiceType.JupyterNotebook, "/notebook/"),
  resolver(ServiceType.Resolver, ""),
  netcdfSubsetGrid(ServiceType.NetcdfSubset, "/ncss/grid/"),    // heres a wrinkle
  netcdfSubsetPoint(ServiceType.NetcdfSubset, "/ncss/point/"),
  opendap(ServiceType.OPENDAP, "/dodsC/"),
  wcs(ServiceType.WCS, "/wcs/"),
  wms(ServiceType.WMS, "/wms/"),
  wfs(ServiceType.WFS, "/wfs/"),

  iso(ServiceType.ISO, "/iso/"),
  iso_ncml(ServiceType.NCML, "/ncml/"),
  uddc(ServiceType.UDDC, "/uddc/");

  static public StandardService getStandardServiceIgnoreCase(String typeS) {
    for (StandardService s : values())
      if (s.toString().equalsIgnoreCase(typeS)) return s;
    return null;
  }

  final ServiceType type;
  final String base;

  StandardService(ServiceType type, String base) {
    this.type = type;
    this.base = base;
  }

  public ServiceType getType() {
    return type;
  }

  public String getBase() {
    return base;
  }
}
