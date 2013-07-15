/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib;

import org.jdom2.Element;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.ncml.NcMLReader;

/**
 * Superclass for Grib1Iosp and Grib2Iosp
 * To share some common methods.
 *
 * @author caron
 * @since 1/22/12
 */
public abstract class GribIosp extends AbstractIOServiceProvider {
  static public final String VARIABLE_ID_ATTNAME = "Grib_Variable_Id";

  private static final boolean debug = false;

  // store custom tables in here
  protected FeatureCollectionConfig.GribConfig gribConfig = new FeatureCollectionConfig.GribConfig();

  public void setParamTable(Element paramTable) {
    gribConfig.paramTable = paramTable;
  }

  public void setLookupTablePath(String lookupTablePath) {
    gribConfig.lookupTablePath = lookupTablePath;
  }

  public void setParamTablePath(String paramTablePath) {
    gribConfig.paramTablePath = paramTablePath;
  }

  @Override
  public Object sendIospMessage(Object special) {
    if (special instanceof String) {
      String s = (String) special;
      if (s.startsWith("gribParameterTableLookup")) {
        int pos = s.indexOf("=");
        if (pos > 0)
          gribConfig.lookupTablePath = s.substring(pos+1).trim();

      } else if (s.startsWith("gribParameterTable")) {
        int pos = s.indexOf("=");
        if (pos > 0)
          gribConfig.paramTablePath = s.substring(pos+1).trim();
      }

      if (debug) System.out.printf("GRIB got IOSP message=%s%n", special);
      return null;
    }

    if (special instanceof org.jdom2.Element) {  // the root element will be <iospParam>
      Element root = (org.jdom2.Element) special;
      gribConfig.configFromXml(root, NcMLReader.ncNS);
      return null;
    }

    return super.sendIospMessage(special);
  }
}
