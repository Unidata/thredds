/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
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

package thredds.server.ncss.controller;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import thredds.server.config.TdsContext;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;
import java.util.List;

@Component
public final class NcssRequestUtils implements ApplicationContextAware {

  private static ApplicationContext applicationContext;

  private NcssRequestUtils() {
  }

  public static TdsContext getTdsContext() {
    return applicationContext.getBean(TdsContext.class);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    NcssRequestUtils.applicationContext = applicationContext;
  }

  public static String getFileNameForResponse(String pathInfo, NetcdfFileWriter.Version version) {
    Preconditions.checkNotNull(version, "version == null");
    return getFileNameForResponse(pathInfo, version.getSuffix());
  }

  public static String getFileNameForResponse(String pathInfo, String extension) {
    Preconditions.checkArgument(pathInfo != null && !pathInfo.isEmpty(), "pathInfo == %s", pathInfo);
    Preconditions.checkNotNull(extension, "extension == null");

    String parentDirName = FilenameUtils.getBaseName(FilenameUtils.getPathNoEndSeparator(pathInfo));
    String baseName = FilenameUtils.getBaseName(pathInfo);
    String dotExtension = extension.startsWith(".") ? extension : "." + extension;

    if (!parentDirName.isEmpty()) {
      return parentDirName + "_" + baseName + dotExtension;
    } else {
      return baseName + dotExtension;
    }
  }

  public static GridAsPointDataset buildGridAsPointDataset(CoverageDataset gcd, List<String> vars) {
    return null;
  }

  public static Double getTargetLevelForVertCoord(CoverageCoordAxis zAxis, Double vertLevel) {
    return 0.0;
  }

  /**
   * Returns the actual vertical level if the grid has vertical transformation or -9999.9 otherwise
   */
  public static double getActualVertLevel(CoverageDataset gcd, Coverage grid, CalendarDate date, LatLonPoint point, double targetLevel) { //} throws IOException, InvalidRangeException {

    double actualLevel = -9999.9;
    return actualLevel;
  }
}
