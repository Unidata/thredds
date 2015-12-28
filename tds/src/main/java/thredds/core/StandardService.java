/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
  resolver(ServiceType.Resolver, ""),
  netcdfSubsetGrid(ServiceType.NetcdfSubset, "/ncss/grid/"),    // heres a wrinkle
  netcdfSubsetPoint(ServiceType.NetcdfSubset, "/ncss/point/"),
  opendap(ServiceType.OPENDAP, "/dodsC/"),
  wcs(ServiceType.WCS, "/wcs/"),
  wms(ServiceType.WMS, "/wms/"),

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
