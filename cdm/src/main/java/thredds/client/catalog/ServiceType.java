/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

/**
 * Service Type enums
 *
 * @author caron
 * @since 1/7/2015
 */
public enum ServiceType {
  ADDE,           //  not used
  Catalog,       //
  CdmRemote,     //
  CdmrFeature,   //
  Compound,      //
  DAP4,          //
  DODS,          // deprecated
  File,         //  deprecated
  FTP,          //
  GRIDFTP,     //
  H5Service,    //
  HTTPServer,    //
  ISO,           //
  LAS,           //
  NcJSON,          //
  NCML,          //
  NetcdfSubset,   //
  OPENDAP,        //
  OPENDAPG,       //
  Resolver,      //
  THREDDS,       //
  UDDC,         //
  WebForm,      //    ??
  WCS,          //
  WFS,          //
  WMS,          //
  WSDL,         //
  ;

  // ignore case
  static public ServiceType getServiceTypeIgnoreCase(String typeS) {
    for (ServiceType s : values()) {
      if (s.toString().equalsIgnoreCase(typeS)) return s;
    }
    return null;
  }

  public boolean isStandardTdsService() {
    return this == Catalog || this == CdmRemote || this == CdmrFeature || this == DAP4 ||
      this == DODS || this == File || this == HTTPServer || this == ISO || this == NCML ||
      this == NetcdfSubset || this == OPENDAP || this == UDDC || this == WCS || this == WMS;
  }

}
