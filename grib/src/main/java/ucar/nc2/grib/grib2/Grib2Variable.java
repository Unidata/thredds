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
package ucar.nc2.grib.grib2;

import ucar.nc2.grib.grib2.table.Grib2Customizer;

/**
 * Used to group records into a CDM variable
 * Herein lies the semantics of variable object identity.
 * Read it and weep.
 *
 * @author caron
 * @since 12/28/2014
 */
public class Grib2Variable {

  public static int cdmVariableHash(Grib2Customizer cust, Grib2Record gr, int gdsHashOverride, boolean intvMerge, boolean useGenType) {
    Grib2Variable gv = new Grib2Variable(cust, gr, gdsHashOverride, intvMerge, useGenType);
    return gv.hashCode();
  }

  ////////////////////////////////////////////////////////////////////////
  private final Grib2Customizer cust;
  private final int discipline, center, subcenter;
  private final int gdsHash;
  private final Grib2Gds gds;
  private final Grib2Pds pds;
  private final  boolean intvMerge;
  private final boolean useGenType;

  /**
   * Used when building from gbx9 (full records)
   *
   * @param cust       customizer
   * @param gr         the Grib record
   * @param gdsHashOverride    can override the gdsHash, 0 for no override
   * @param intvMerge  should intervals be merged? default true
   * @param useGenType should genProcessType be used in hash? default false
   */
  public Grib2Variable(Grib2Customizer cust, Grib2Record gr, int gdsHashOverride, boolean intvMerge, boolean useGenType) {
    this.cust = cust;
    this.discipline = gr.getDiscipline();
    this.center = gr.getId().getCenter_id();
    this.subcenter = gr.getId().getSubcenter_id();
    this.gds = gr.getGDS();
    this.gdsHash = gdsHashOverride != 0 ? gdsHashOverride : gr.getGDS().hashCode();
    this.pds = gr.getPDS();
    this.intvMerge = intvMerge;
    this.useGenType = useGenType;
  }

  /**
   * Used when building from ncx3 (full records)
   */
  public Grib2Variable(Grib2Customizer cust, int discipline, int center, int subcenter, Grib2Gds gds, Grib2Pds pds, boolean intvMerge, boolean useGenType) {
    this.cust = cust;
    this.discipline = discipline;
    this.center = center;
    this.subcenter = subcenter;
    this.gds = gds;
    this.gdsHash = gds.hashCode;             // this requires no overridden gds hashCodes have made it into the ncx3
    this.pds = pds;
    this.intvMerge = intvMerge;
    this.useGenType = useGenType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Grib2Variable var2 = (Grib2Variable) o;
    if (gdsHash != var2.gdsHash) return false;
    if (!gds.equals(var2.gds)) return false;

    Grib2Pds pds2 = var2.pds;

    if (pds.getParameterNumber() != pds2.getParameterNumber()) return false;
    if (pds.getParameterCategory() != pds2.getParameterCategory()) return false;
    if (pds.getTemplateNumber() != pds2.getTemplateNumber()) return false;
    if (discipline != var2.discipline) return false;

    if (Grib2Utils.isLayer(pds) != Grib2Utils.isLayer(pds2)) return false;
    if (pds.getLevelType1() != pds2.getLevelType1()) return false;

    if (pds.isTimeInterval() != pds2.isTimeInterval()) return false;
    if (pds.isTimeInterval()) {
      if (!intvMerge) {
        double size = cust.getForecastTimeIntervalSizeInHours(pds); // LOOK using an Hour here, but will need to make this configurable
        double size2 = cust.getForecastTimeIntervalSizeInHours(pds2);
        if (size != size2) return false;
      }
    }

    if (pds.isSpatialInterval() != pds2.isSpatialInterval()) return false;
    if (pds.isSpatialInterval()) {
      if (pds.getStatisticalProcessType() != pds2.getStatisticalProcessType()) return false;
    }

    int ensDerivedType = -1;
    if (pds.isEnsembleDerived() != pds2.isEnsembleDerived()) return false;
    if (pds.isEnsembleDerived()) {
      Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds;
      Grib2Pds.PdsEnsembleDerived pdsDerived2 = (Grib2Pds.PdsEnsembleDerived) pds2;
      if (pdsDerived.getDerivedForecastType() != pdsDerived2.getDerivedForecastType()) return false;
      ensDerivedType = pdsDerived.getDerivedForecastType(); // derived type (table 4.7)

    } else {
      if (pds.isEnsemble() != pds2.isEnsemble()) return false;
    }

    int probType = -1;
    if (pds.isProbability() != pds2.isProbability()) return false;
    if (pds.isProbability()) {
      Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
      Grib2Pds.PdsProbability pdsProb2 = (Grib2Pds.PdsProbability) pds2;
      if (pdsProb.getProbabilityHashcode() != pdsProb2.getProbabilityHashcode()) return false;
      probType = pdsProb.getProbabilityType();
    }

    // if this uses any local tables, then we have to add the center id, and subcenter if present
    if ((pds2.getParameterCategory() > 191) || (pds2.getParameterNumber() > 191) || (pds2.getLevelType1() > 191)
            || (pds2.isTimeInterval() && pds2.getStatisticalProcessType() > 191)
            || (ensDerivedType > 191) || (probType > 191)) {

      if (center!= var2.center) return false;
      if (subcenter != var2.subcenter) return false;
    }

    return true;
  }


  @Override
  public int hashCode() {
    int result = 17;

    result += result * 31 + discipline;
    result += result * 31 + pds.getLevelType1();
    if (Grib2Utils.isLayer(pds)) result += result * 31 + 1;

    result += result * 31 + this.gdsHash; // the horizontal grid

    result += result * 31 + pds.getParameterCategory();
    result += result * 31 + pds.getTemplateNumber();

    if (pds.isTimeInterval()) {
      if (!intvMerge) {
        double size = cust.getForecastTimeIntervalSizeInHours(pds); // LOOK using an Hour here, but will need to make this configurable
        result += result * (int) (31 + (1000 * size)); // create new variable for each interval size - default not
      }
      // result += result * 31 + pds.getStatisticalProcessType(); // create new variable for each stat type LOOK WTF ??
    }

    if (pds.isSpatialInterval()) {
       result += result * 31 + pds.getStatisticalProcessType(); // template 15
     }

     result += result * 31 + pds.getParameterNumber();

    int ensDerivedType = -1;
    if (pds.isEnsembleDerived()) {  // a derived ensemble must have a derivedForecastType
      Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds;
      ensDerivedType = pdsDerived.getDerivedForecastType(); // derived type (table 4.7)
      result += result * 31 + ensDerivedType;

    } else if (pds.isEnsemble()) {
      result += result * 31 + 1;
    }

    // each probability interval generates a separate variable; could be a dimension instead
    int probType = -1;
    if (pds.isProbability()) {
      Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
      probType = pdsProb.getProbabilityType();
      result += result * 31 + pdsProb.getProbabilityHashcode();
    }

    // if this uses any local tables, then we have to add the center id, and subcenter if present
    if ((pds.getParameterCategory() > 191) || (pds.getParameterNumber() > 191) || (pds.getLevelType1() > 191)
            || (pds.isTimeInterval() && pds.getStatisticalProcessType() > 191)
            || (ensDerivedType > 191) || (probType > 191)) {
      result += result * 31 + center;
      if (subcenter > 0)
        result += result * 31 + subcenter;
    }

    // always use the GenProcessType when "error" (6 or 7) 2/8/2012
    int genType = pds.getGenProcessType();
    if (genType == 6 || genType == 7 || (useGenType && genType > 0)) {
      result += result * 31 + genType;
    }

    return result;
  }

}
