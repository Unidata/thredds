/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.grib.grib1;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.collection.Grib1Iosp;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;

/**
 * Used to group records into a CDM variable
 * Herein lies the semantics of a variable object identity.
 * Read it and weep.
 *
 * @author caron
 * @since 12/28/2014
 */
public class Grib1Variable {

  public static int cdmVariableHash(Grib1Customizer cust, Grib1Record gr, int gdsHashOverride, boolean useTableVersion, boolean intvMerge, boolean useCenter) {
    Grib1Variable gv = new Grib1Variable(cust, gr, gdsHashOverride, useTableVersion, intvMerge, useCenter);
    return gv.hashCode();
  }

  /////////////////////////////////////////

  private final Grib1Customizer cust;
  private final Grib1SectionProductDefinition pds;
  private final Grib1Gds gds;
  private final int gdsHash;
  private final boolean useTableVersion;
  private final boolean intvMerge;
  private final boolean useCenter;

  /**
   * Used when processing the gbx9 files
   *
   * @param cust     customizer
   * @param gr       grib record
   * @param gdsHashOverride   can override the gdsHash, 0 for no override
   * @param useTableVersion  use pdss.getTableVersion(), default is false
   * @param intvMerge        put all intervals together, default true
   * @param useCenter        use center id when param no > 127, default is false
   */
  public Grib1Variable(Grib1Customizer cust, Grib1Record gr, int gdsHashOverride, boolean useTableVersion, boolean intvMerge, boolean useCenter) {
    this.cust = cust;
    this.pds = gr.getPDSsection();
    this.gds = gr.getGDS();
    this.gdsHash = gdsHashOverride != 0 ? gdsHashOverride : gr.getGDS().hashCode();
    this.useTableVersion = useTableVersion;
    this.intvMerge = intvMerge;
    this.useCenter = useCenter;
  }

  /**
   * Used when processing the ncx3 files
   *
   * @param cust              customizer
   * @param pds               pds section
   * @param gds               the group gds
   * @param useTableVersion   use pdss.getTableVersion(), default is false
   * @param intvMerge         put all intervals together, default true
   * @param useCenter         use center id when param no > 127, default is false
   */
  public Grib1Variable(Grib1Customizer cust, Grib1SectionProductDefinition pds, Grib1Gds gds, boolean useTableVersion, boolean intvMerge, boolean useCenter) {
    this.cust = cust;
    this.pds = pds;
    this.gds = gds;
    this.gdsHash = gds.hashCode();             // this assumes no overridden gds hashCodes have made it into the ncx3
    this.useTableVersion = useTableVersion;
    this.intvMerge = intvMerge;
    this.useCenter = useCenter;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Grib1Variable var2 = (Grib1Variable) o;
    if (gdsHash != var2.gdsHash) return false;
    if (!gds.equals(var2.gds)) return false;

    Grib1SectionProductDefinition pds2 = var2.pds;
    if (pds.getParameterNumber() != pds2.getParameterNumber()) return false;
    if (pds.getLevelType() != pds2.getLevelType()) return false;

    if (useTableVersion) {
      if (pds.getTableVersion() != pds2.getTableVersion()) return false;
    }

    Grib1ParamTime ptime = cust.getParamTime(pds);
    Grib1ParamTime ptime2 = cust.getParamTime(pds2);
    if (ptime.isInterval() != ptime2.isInterval()) return false;
    if (ptime.isInterval()) {
      if (!intvMerge) {
        if (ptime.getIntervalSize() != ptime2.getIntervalSize()) return false;
      }
      if (ptime.getStatType() != ptime2.getStatType()) return false;
    }

    if (useCenter && pds.getParameterNumber() > 127) {
      if (pds.getCenter() != pds2.getCenter()) return false;
      if (pds.getSubCenter() != pds2.getSubCenter()) return false;
    }

    return true;
  }

  @Override
  public int hashCode() {   // could switch to using guava goodFastHash, if not storing (?)
    int result = 17;        // Warning: a new random seed for these functions is chosen each time the Hashing class is loaded. Do not use this method if hash codes may escape the current process in any way, for example being sent over RPC, or saved to disk.

    result += result * 31 + pds.getParameterNumber();
    result += result * 31 + gdsHash;

    result += result * 31 + pds.getLevelType();
    // if (cust.isLayer(pds.getLevelType())) result += result * 31 + 1;  // LOOK not needed if same level type

    if (useTableVersion)
      result += result * 31 + pds.getTableVersion();

    Grib1ParamTime ptime = cust.getParamTime(pds);
    if (ptime.isInterval()) {
      if (!intvMerge) result += result * 31 + ptime.getIntervalSize();  // create new variable for each interval size
      if (ptime.getStatType() != null) result += result * 31 + ptime.getStatType().ordinal(); // create new variable for each stat type
    }

    // if useCenter, and this uses any local tables, then we have to add the center id, and subcenter if present
    if (useCenter && pds.getParameterNumber() > 127) {
      result += result * 31 + pds.getCenter();
      if (pds.getSubCenter() > 0)
        result += result * 31 + pds.getSubCenter();
    }
    return result;
  }

  public String makeVariableName(FeatureCollectionConfig.GribConfig gribConfig) {
    return Grib1Iosp.makeVariableName(cust, gribConfig, pds);
  }
}
