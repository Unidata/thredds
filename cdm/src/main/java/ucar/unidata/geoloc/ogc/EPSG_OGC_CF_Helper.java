/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.ogc;

import ucar.nc2.constants.CF;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.util.Parameter;

/**
 * Helper class for using EPSG codes.
 *
 * @author edavis
 * @since 4.0
 */
public class EPSG_OGC_CF_Helper {
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger(EPSG_OGC_CF_Helper.class);

  public enum ProjectionStandardsInfo {
    Unknown(0, "Unknown", "unknown"),
    // From CF 1.0
    Albers_Conic_Equal_Area(9822, "Albers Equal Area", "albers_conical_equal_area"),
    Azimuthal_Equidistant(-1, "", "azimuthal_equidistant"), // 9832, "Modified Azimuthal Equidistant" [?]
    Lambert_Azimuthal_Equal_Area(9820, "Lambert Azimuthal Equal Area", "lambert_azimuthal_equal_area"),
    Lambert_Conformal_Conic_2SP(9802, "Lambert Conic Conformal (2SP)", "lambert_conformal_conic"),
    Polar_Stereographic(9810, "Polar Stereographic (Variant A)", "polar_stereographic"),
    Rotated_Pole(-2, "", "rotated_latitude_longitude"), // 9601 "Rotated Latitude" [?????]
    Stereographic(-3, "", "stereographic"), // 9809, "Oblique Stereographic" [?]
    Transverse_Mercator(9807, "Transverse Mercator", "transverse_mercator"),

    // Added in CF 1.2
    Latitude_Longitude(0, "", "latitude_longitude"),
    Vertical_Perspective(9838, "Vertical Perspective", "vertical_perspective"),

    // Added in CF 1.4
    Lambert_Cylindrical_Equal_Area(9835, "Lambert Cylindrical Equal Area", "lambert_cylindrical_equal_area"),
    Mercator(9805, "Mercator (2SP)", "mercator"),
    Orthographic(9840, "Orthographic", "orthographic");

    private final int epsgCode;
    private final String epsgName;
    private final String cfName;

    public String getOgcName() {
      return this.name();
    }

    public int getEpsgCode() {
      return this.epsgCode;
    }

    public String getEpsgName() {
      return this.epsgName;
    }

    public String getCfName() {
      return this.cfName;
    }

    ProjectionStandardsInfo(int epsgCode, String epsgName, String cfName) {
      this.epsgCode = epsgCode;
      this.epsgName = epsgName;
      this.cfName = cfName;
    }

    public String toString() {
      StringBuilder buf = new StringBuilder();
      buf.append("[[OGC: ").append(this.name())
              .append("] [EPSG ").append(this.getEpsgCode()).append(": ").append(this.getEpsgName())
              .append("] [CF: ").append(this.getCfName()).append("]]");
      return buf.toString();
    }

    public static ProjectionStandardsInfo getProjectionByOgcName(String ogcName) {
      for (ProjectionStandardsInfo curProjStdInfo : values()) {
        if (curProjStdInfo.name().equals(ogcName))
          return curProjStdInfo;
      }
      return Unknown;
    }

    public static ProjectionStandardsInfo getProjectionByEpsgCode(int epsgCode) {
      for (ProjectionStandardsInfo curProjStdInfo : values()) {
        if (curProjStdInfo.getEpsgCode() == epsgCode)
          return curProjStdInfo;
      }
      return Unknown;
    }

    public static ProjectionStandardsInfo getProjectionByEpsgName(String epsgName) {
      for (ProjectionStandardsInfo curProjStdInfo : values()) {
        if (curProjStdInfo.getEpsgName().equals(epsgName))
          return curProjStdInfo;
      }
      return Unknown;
    }

    public static ProjectionStandardsInfo getProjectionByCfName(String cfName) {
      for (ProjectionStandardsInfo curProjStdInfo : values()) {
        if (curProjStdInfo.getCfName().equals(cfName))
          return curProjStdInfo;
      }
      return Unknown;
    }

  }

  public static String getWcs1_0CrsId(Projection proj) {
    String paramName = null;
    if (proj == null)
      paramName = "LatLon";
    else {
      for (Parameter curParam : proj.getProjectionParameters())
        if (curParam.getName().equalsIgnoreCase(CF.GRID_MAPPING_NAME) && curParam.isString())
          paramName = curParam.getStringValue();
    }
    if (paramName == null) {
      log.warn("getWcs1_0CrsId(): Unknown projection - " + projToString(proj));
      return ProjectionStandardsInfo.Unknown.getOgcName();
    }
    if (paramName.equalsIgnoreCase("LatLon")) {
      paramName = "latitude_longitude";
      return "OGC:CRS84";
    }

    ProjectionStandardsInfo psi = ProjectionStandardsInfo.getProjectionByCfName(paramName);
    String crsId = "EPSG:" + psi.getEpsgCode() + " [" + psi.name();
    if (psi.equals(ProjectionStandardsInfo.Unknown)) {
      log.warn("getWcs1_0CrsId(): Unknown projection - " + projToString(proj));
      crsId += " - " + paramName;
    }
    return crsId + "]";
  }

  private static String projToString(Projection proj) {
    if (proj == null) return "null";
    StringBuilder sb = new StringBuilder();
    sb.append(proj.getName())
            .append(" [").append(proj.getClassName())
            .append("] - parameters=[");
    for (Parameter curProjParam : proj.getProjectionParameters()) {
      sb.append("(").append(curProjParam.toString()).append(")");
    }
    sb.append("]");
    return sb.toString();
  }

  public String getWcs1_0CrsId(GridDatatype gridDatatype, GridDataset gridDataset) throws IllegalArgumentException {
    gridDataset.getTitle();
    gridDatatype.getFullName();

    StringBuilder buf = new StringBuilder();

    Attribute gridMappingAtt = gridDatatype.findAttributeIgnoreCase(CF.GRID_MAPPING);
    if (gridMappingAtt != null) {
      String gridMapping = gridMappingAtt.getStringValue();
      Variable gridMapVar = gridDataset.getNetcdfFile().getRootGroup().findVariable(gridMapping);
      if (gridMapVar != null) {
        Attribute gridMappingNameAtt = gridMapVar.findAttributeIgnoreCase(CF.GRID_MAPPING_NAME);
        if (gridMappingNameAtt != null)
          buf.append("EPSG:").append(ProjectionStandardsInfo.getProjectionByCfName(gridMappingNameAtt.getStringValue()));
      }
    }

    return buf.toString();
  }
}
