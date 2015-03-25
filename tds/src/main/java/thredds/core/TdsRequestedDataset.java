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

package thredds.core;

import org.springframework.beans.factory.annotation.Autowired;
import thredds.server.dataset.DatasetException;
import thredds.servlet.ServletUtil;
import thredds.util.TdsPathUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.FeatureDataset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Extracts the dataset ID from the HttpServletRequest and determines if it is
 * a local dataset path or a remote dataset URL.
 * <p/>
 * <p>The requested dataset can be opened by using the
 * {@link #openAsGridDataset(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
 * method.
 */
public class TdsRequestedDataset {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TdsRequestedDataset.class);

  @Autowired
  private DatasetManager datasetManager;

  public static FeatureDataset getFeatureCollection(HttpServletRequest request, HttpServletResponse response) {
    return null;
  }

  public static GridDataset openGridDataset(HttpServletRequest request, HttpServletResponse response, String path) {
      return null;
  }

  public static FeatureDataset getFeatureCollection(HttpServletRequest request, HttpServletResponse response, String path) {
    return null;
  }

  public static NetcdfFile getNetcdfFile(HttpServletRequest request, HttpServletResponse response) {
    return null;
  }

  public static NetcdfFile getNetcdfFile(HttpServletRequest request, HttpServletResponse response, String path) {
    return null;
  }

  static public String getNetcdfFilePath(HttpServletRequest req, String reqPath) throws IOException {
    /* if (log.isDebugEnabled()) log.debug("DatasetHandler wants " + reqPath);

    if (reqPath == null)
      return null;

    if (reqPath.startsWith("/"))
      reqPath = reqPath.substring(1);

    // look for a match
    DataRootHandler.DataRootMatch match = datasetManager.findDataRootMatch(reqPath);

    String fullpath = null;
    if (match != null)
      fullpath = match.dirLocation + match.remaining;
    else {
      File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(reqPath);
      if (file != null)
        fullpath = file.getAbsolutePath();
    }
    return fullpath;  */
    return null;
  }

  public static long getLastModified(String path) {
    return -1;
  }

  public static File getFile(String path) {
    return null;
  }


  private boolean isRemote = false;
  private String path;

  public TdsRequestedDataset(HttpServletRequest request) throws DatasetException {
    path = TdsPathUtils.extractPath(request, null);
    if (path == null) {
      path = ServletUtil.getParameterIgnoreCase(request, "dataset");
      isRemote = (path != null);
    }
    if (path == null) {
      log.debug("Request does not specify a dataset.");
      throw new DatasetException("Request does not specify a dataset.");
    }
  }

  /**
   * Open the requested dataset as a GridDataset. If the request requires an
   * authentication challenge, a challenge will be sent back to the client using
   * the response object, and this method will return null.  (This is the only
   * circumstance in which this method will return null.)
   *
   * @param request  the HttpServletRequest
   * @param response the HttpServletResponse
   * @return the requested dataset as a GridDataset
   * @throws java.io.IOException if have trouble opening the dataset
   */
  public GridDataset openAsGridDataset(HttpServletRequest request, HttpServletResponse response) throws IOException {
    return isRemote ? ucar.nc2.dt.grid.GridDataset.open(path)
            : datasetManager.openGridDataset(request, response, path);
  }

  /**
   * Open the requested dataset as a NetcdfFile. If the request requires an
   * authentication challenge, a challenge will be sent back to the client using
   * the response object, and this method will return null.  (This is the only
   * circumstance in which this method will return null.)
   *
   * @param request  the HttpServletRequest
   * @param response the HttpServletResponse
   * @return the requested dataset as a NetcdfFile
   * @throws java.io.IOException if have trouble opening the dataset
   */
  public NetcdfFile openAsNetcdfFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
    return isRemote ? NetcdfDataset.openDataset(path)
            : datasetManager.getNetcdfFile(request, response, path);
  }

  public boolean isRemote() {
    return isRemote;
  }

  public String getPath() {
    return path;
  }
}
