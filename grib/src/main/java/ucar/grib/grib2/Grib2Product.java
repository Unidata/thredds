/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */


/**
 * Grib2Product.java
 * @author Robb Kambic
 */
package ucar.grib.grib2;


import java.util.*;


/**
 * Class which has all the necessary information about
 * a record in a Grib2 File to extract the data.
 */

public final class Grib2Product {

  /**
   * Grib header
   */
  private final String header;

  /**
   * parameter discipline
   */
  private final int discipline;

  /**
   *reference time as a Date and as a long
   */
  private final Date baseTime;
  private final long refTime;

  /**
   * Grib2IdentificationSection
   */
  private Grib2IdentificationSection id = null;

  /**
   * gdsKey as a String and as a int, both for backwards compatability
   */
  private final String gdsKey;
  private final int gdskey;
  /**
   * Grib2ProductDefinitionSection
   */
  private Grib2ProductDefinitionSection pds = null;

  /**
   * GdsOffset
   */
  private long GdsOffset = -1;

  /**
   * PdsOffset
   */
  private long PdsOffset = -1;

  /**
   * Constructor.
   *
   * @param header Grib header
   * @param is Grib2IndicatorSection
   * @param id Grib2IdentificationSection
   * @param gdsKey gdsKey
   * @param pds Grib2ProductDefinitionSection
   * @param GdsOffset GDS offset in Grib file
   * @param PdsOffset PDS offset in Grib file
   */
  public Grib2Product(String header, Grib2IndicatorSection is,
          Grib2IdentificationSection id, String gdsKey,
          Grib2ProductDefinitionSection pds, long GdsOffset,
          long PdsOffset) {

    this.header = header;
    this.discipline = is.getDiscipline();
    this.id = id;
    this.baseTime = id.getBaseTime();
    this.refTime = id.getRefTime();
    this.gdsKey = gdsKey;
    this.gdskey = Integer.parseInt( gdsKey );
    this.pds = pds;
    this.GdsOffset = GdsOffset;
    this.PdsOffset = PdsOffset;
  }

  /**
   * Constructor.
   *
   * @param header Grib header
   * @param is Grib2IndicatorSection
   * @param id Grib2IdentificationSection
   * @param gdsKey gdsKey as a String
   * @param gdskey as a int
   * @param pds Grib2ProductDefinitionSection
   * @param GdsOffset GDS offset in Grib file
   * @param PdsOffset PDS offset in Grib file
   *
   */
  public Grib2Product(String header, Grib2IndicatorSection is,
          Grib2IdentificationSection id, String gdsKey, int gdskey,
          Grib2ProductDefinitionSection pds, long GdsOffset,
          long PdsOffset) {

    this.header = header;
    this.discipline = is.getDiscipline();
    this.id = id;
    this.baseTime = id.getBaseTime();
    this.refTime = id.getRefTime();
    this.gdsKey = gdsKey;
    this.gdskey = gdskey;
    this.pds = pds;
    this.GdsOffset = GdsOffset;
    this.PdsOffset = PdsOffset;
  }

  /**
   * Discipline number for this record.
   *
   * @return discipline
   */
  public final int getDiscipline() {
    return discipline;
  }

  /**
   * base time for this product as date.
   *
   * @return baseTime
   */
  public final Date getBaseTime() {
    return baseTime;
  }

  /**
   * Reference time for this product.
   *
   * @return referenceTime
   */
  public final long getRefTime() {
    return refTime;
  }

  /**
   * GDSkey is a hashcode of the GDS for this record.
   *
   * @return gdsKey
   */
  public final String getGDSkey() {
    return gdsKey;
  }
  /**
   * GDSkey is a double of the hashcode of the GDS for this record.
   *
   * @return gdskey
   */
  public final int getGDSkeyInt() {
    return gdskey;
  }
  /**
   * Actual PDS of this record.
   *
   * @return pds
   */
  public final Grib2ProductDefinitionSection getPDS() {
    return pds;
  }

  /**
   * ID of this record.
   *
   * @return id
   */
  public final Grib2IdentificationSection getID() {
    return id;
  }

  /**
   * Actual GDS offset in the Grib2 file.
   *
   * @return GdsOffset
   */
  public final long getGdsOffset() {
    return GdsOffset;
  }

  /**
   * PDS offset in the file.
   *
   * @return PdsOffset
   */
  public final long getPdsOffset() {
    return PdsOffset;
  }
}

