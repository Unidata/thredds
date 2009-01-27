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
package thredds.catalog2.xml.util;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsMetadataElementUtils
{
  private ThreddsMetadataElementUtils() {}

  public static final String PROXY_ELEMENT_NAME = "threddsMetadataGroup";

  public static final String SERVICE_NAME_ELEMENT_NAME = "serviceName";
  public static final String AUTHORITY_ELEMENT_NAME = "authority";

  public static final String DOCUMENTATION_ELEMENT_NAME = "documentation";
  public static final String KEYPHRASE_ELEMENT_NAME = "keyword";
  public static final String CREATOR_ELEMENT_NAME = "creator";
  public static final String CONTRIBUTOR_ELEMENT_NAME = "contributor";
  public static final String PUBLISHER_ELEMENT_NAME = "publisher";
  public static final String PROJECT_ELEMENT_NAME = "project";
  public static final String DATE_ELEMENT_NAME = "date";

  public static final String GEOSPATIAL_COVERAGE_ELEMENT_NAME = "geospatialCoverage";
  public static final String TEMPORAL_COVERAGE_ELEMENT_NAME = "timeCoverage";

  public static final String VARIABLES_ELEMENT_NAME = "variables";

  public static final String DATA_SIZE_ELEMENT_NAME = "dataSize";
  public static final String DATA_FORMAT_ELEMENT_NAME = "dataFormat";
  public static final String DATA_TYPE_ELEMENT_NAME = "dataType";
}