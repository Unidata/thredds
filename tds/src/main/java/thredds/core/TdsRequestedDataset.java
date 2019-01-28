/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.core;

import thredds.servlet.ServletUtil;
import thredds.util.TdsPathUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.simpgeometry.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This is the public interface to TDS objects for all services.
 *
 * Open the requested dataset as a FeatureDataset/GridDataset/NetcdfFile.
 * If the request requires an authentication challenge, a challenge will be sent back to the client using
 * the response object, and this method will return null.  (This is the only
 * circumstance in which this method will return null.)
 * The client will then repeat the request.
 *
 * LOOK: should we use Optional ?
 */
public class TdsRequestedDataset {
  // private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TdsRequestedDataset.class);

  // LOOK maybe static not such a good idea? can 3rd parties wire in an instance ??
  static public void setDatasetManager(DatasetManager _datasetManager) {
    datasetManager = _datasetManager;
  }

  static public DatasetManager getDatasetManager() {
    return datasetManager;
  }

  static private DatasetManager datasetManager;

  // return null means request has been handled, and calling routine should exit without further processing
  public static FeatureDatasetPoint getPointDataset(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
    TdsRequestedDataset trd = new TdsRequestedDataset(request, null);
    if (path != null) trd.path = path;
    return trd.openAsPointDataset(request, response);
  }

  // return null means request has been handled, and calling routine should exit without further processing
  public static GridDataset getGridDataset(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
    TdsRequestedDataset trd = new TdsRequestedDataset(request, null);
    if (path != null) trd.path = path;
    return trd.openAsGridDataset(request, response);
  }

  // return null means request has been handled, and calling routine should exit without further processing
  public static CoverageCollection getCoverageCollection(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
    TdsRequestedDataset trd = new TdsRequestedDataset(request, null);
    if (path != null) trd.path = path;
    return trd.openAsCoverageDataset(request, response);
  }
  public static SimpleGeometryFeatureDataset getSimpleGeometryFeatureDataset(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
    TdsRequestedDataset trd = new TdsRequestedDataset(request, null);
    if (path != null) trd.path = path;
    return trd.openAsSimpleGeometryDataset(request, response);
  }
  // return null means request has been handled, and calling routine should exit without further processing
  public static NetcdfFile getNetcdfFile(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
    TdsRequestedDataset trd = new TdsRequestedDataset(request, null);
    if (path != null) trd.path = path;
    return trd.openAsNetcdfFile(request, response);
  }

  public static long getLastModified(String reqPath) {
    File file = getFile(reqPath);
    return (file == null) ? -1 : file.lastModified();
  }

  public static File getFile(String reqPath) {
    String location = getLocationFromRequestPath(reqPath);
    return (location == null) ? null : new File(location);
  }

  public static String getLocationFromRequestPath(String reqPath) {
    return datasetManager.getLocationFromRequestPath(reqPath);
  }

  public static boolean resourceControlOk(HttpServletRequest request, HttpServletResponse response, String path) {
    return datasetManager.resourceControlOk(request, response, path);
  }

  ///////////////////////////////////////////////////////////////////////////////////
  private boolean isRemote = false;
  private String path;

  public TdsRequestedDataset(HttpServletRequest request, String removePrefix) throws IOException {
    this.path = TdsPathUtils.extractPath(request, removePrefix);
    if (this.path == null) {
      this.path = ServletUtil.getParameterIgnoreCase(request, "dataset");
      isRemote = (path != null);
    }
    if (this.path == null) {
      throw new FileNotFoundException("Request does not specify a dataset.");
    }
  }

  // return null means request has been handled, and calling routine should exit without further processing
  public FeatureDatasetPoint openAsPointDataset(HttpServletRequest request, HttpServletResponse response) throws IOException {
    return datasetManager.openPointDataset(request, response, path);
  }

  // return null means request has been handled, and calling routine should exit without further processing
  public CoverageCollection openAsCoverageDataset(HttpServletRequest request, HttpServletResponse response) throws IOException {
    return datasetManager.openCoverageDataset(request, response, path);
  }
  public SimpleGeometryFeatureDataset openAsSimpleGeometryDataset(HttpServletRequest request, HttpServletResponse response) throws IOException {
    return isRemote? SimpleGeometryFeatureDataset.open(path) : datasetManager.openSimpleGeometryDataset(request, response, path);
  }
  // return null means request has been handled, and calling routine should exit without further processing
  public GridDataset openAsGridDataset(HttpServletRequest request, HttpServletResponse response) throws IOException {
    return isRemote ? ucar.nc2.dt.grid.GridDataset.open(path) : datasetManager.openGridDataset(request, response, path);
  }

  // return null means request has been handled, and calling routine should exit without further processing
  public NetcdfFile openAsNetcdfFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
    return isRemote ? NetcdfDataset.openDataset(path) : datasetManager.openNetcdfFile(request, response, path);
  }

  public boolean isRemote() {
    return isRemote;
  }

  public String getPath() {
    return path;
  }

}
