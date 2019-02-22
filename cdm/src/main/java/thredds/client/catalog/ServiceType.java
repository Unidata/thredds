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
  Catalog("Provide subsetting and HTML conversion services for THREDDS catalogs.", AccessType.Catalog),                 //
  CdmRemote("Provides index subsetting on remote CDM datasets, using ncstream.", AccessType.DataAccess),                //
  CdmrFeature("Provides coordinate subsetting on remote CDM Feature Datasets, using ncstream.", AccessType.DataAccess), //
  Compound,       //
  DAP4("Access dataset through OPeNDAP using the DAP4 protocol.", AccessType.DataAccess),                               //
  DODS,           // deprecated
  File,           //  deprecated
  FTP,            //
  GRIDFTP,        //
  H5Service,      //
  HTTPServer("HTTP file download.", AccessType.DataAccess),                                                             //
  JupyterNotebook("Generate a Jupyter Notebook that uses Siphon to access this dataset.", AccessType.DataAccess),                          //
  ISO("Provide ISO 19115 metdata representation of a dataset's structure and metadata.", AccessType.Metadata),          //
  LAS,            //
  NcJSON,         //
  NCML("Provide NCML representation of a dataset.", AccessType.Metadata),                                               //
  NetcdfSubset("A web service for subsetting CDM scientific datasets.", AccessType.DataAccess),                         //
  OPENDAP("Access dataset through OPeNDAP using the DAP2 protcol.", AccessType.DataAccess),                             //
  OPENDAPG,       //
  Resolver,       //
  THREDDS,        //
  UDDC("An evaluation of how well the metadata contained in the dataset" +
          " conforms to the NetCDF Attribute Convention for Data Discovery (NACDD)", AccessType.Metadata),              //
  WebForm,        //    ??
  WCS("Supports access to geospatial data as 'coverages'.", AccessType.DataAccess),                                     //
  WFS("Supports access to geospatial data as simple geometry objects (such as polygons and lines).", AccessType.DataAccess),            //
  WMS("Supports access to georegistered map images from geoscience datasets.", AccessType.DataAccess),                  //
  WSDL,           //
  ;

  final String desc;
  final AccessType accessType;

  ServiceType() {
    this.desc = null;
    this.accessType = AccessType.Unknown;
  }

  ServiceType(String desc, AccessType accessType) {
    this.desc = desc;
    this.accessType = accessType;
  }

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
      this == NetcdfSubset || this == OPENDAP || this == UDDC || this == WCS || this == WMS || this == WFS;
  }

  public String getDescription() { return this.desc; }

  public String getAccessType() { return this.accessType.name; }

  public enum AccessType {
    Catalog("Catalog"),
    Metadata("Metadata"),
    DataAccess("Data Access"),
    Unknown("Unknown");

    protected String name;

    AccessType(String name) {
      this.name = name;
    }

    public String getName() { return this.name; }
  }
}

