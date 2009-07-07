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
package ucar.nc2.ncml;

import thredds.inventory.*;
import thredds.inventory.RegExpMatchOnName;
import thredds.inventory.WildcardMatchOnPath;
import thredds.inventory.DateExtractorFromFilename;

import java.util.*;
import java.io.IOException;

import ucar.nc2.util.CancelTask;
import ucar.nc2.units.TimeUnit;

/**
 * DatasetScanner implements the scan element, using thredds.inventory.MController.
 *
 * @author caron
 * @since June 26, 2009
 */
public class DatasetScanner2 {
  static protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatasetScanner.class);
  static private MController controller;

  static public void setController(MController _controller) {
    controller = _controller;
  }

  /////////////////////////////////////////////////////////////////////////////////////
  private MCollection mc;
  private boolean wantSubdirs = true;

  // filters
  private long olderThan_msecs; // files must not have been modified for this amount of time (msecs)
  private boolean debugScan = false;

  DatasetScanner2(String dirName, String suffix, String regexpPatternString, String subdirsS, String olderS, String dateFormatString) {
    if (null == controller) controller = new thredds.filesystem.ControllerOS();  // default

    MFileFilter filter = null;
    if (null != regexpPatternString)
      filter = new RegExpMatchOnName(regexpPatternString);
    else if (suffix != null)
      filter = new WildcardMatchOnPath("*" + suffix);

    DateExtractor dateExtractor = (dateFormatString == null) ? null : new DateExtractorFromFilename(dateFormatString);

    if ((subdirsS != null) && subdirsS.equalsIgnoreCase("false"))
      wantSubdirs = false;

    mc = new thredds.inventory.MCollection(dirName, dirName, wantSubdirs, filter, dateExtractor);

    if (olderS != null) {
      try {
        TimeUnit tu = new TimeUnit(olderS);
        this.olderThan_msecs = (long) (1000 * tu.getValueInSeconds());
      } catch (Exception e) {
        logger.error("Invalid time unit for olderThan = {}", olderS);
      }
    }
  }

  public List<MFile> scanDirectory(CancelTask cancelTask) throws IOException {
    List<MFile> result = new ArrayList<MFile>();
    scanDirectory(mc, new Date().getTime(), result, cancelTask);
    return result;
  }

  private void scanDirectory(MCollection mc, long now, List<MFile> result, CancelTask cancelTask) throws IOException {

    Iterator<MFile> iter = controller.getInventory(mc);
    if (iter == null) {
      logger.error("Invalid collection= "+mc);
      return;
    }

    while (iter.hasNext()) {
      MFile child = iter.next();

      if (child.isDirectory()) {
        if (wantSubdirs) scanDirectory(mc.subdir(child), now, result, cancelTask);

      } else {

        // dont allow recently modified (LOOK move to MCollection ??)
        if (olderThan_msecs > 0) {
          long lastModifiedMsecs = child.getLastModified();
          if (now - lastModifiedMsecs < olderThan_msecs)
            continue;
        }

        // add to result
        result.add(child);
        if (debugScan) System.out.println(" accept " + child);
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }
  }

}
