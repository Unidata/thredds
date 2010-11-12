/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.server.cdmremote;

import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.point.collection.CompositeDatasetFactory;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Attribute;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.TimeDuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.HashMap;
import java.util.Formatter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.text.ParseException;

import thredds.servlet.DatasetHandler;
import thredds.servlet.UsageLog;

/**
 * Describe
 *
 * @author caron
 * @since Aug 19, 2009
 */
public class CollectionManager {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
  private HashMap<String, CollectionBean> collectionDatasets = new HashMap<String, CollectionBean>();

  public void setCollections(List<CollectionBean> beans) {
    for (CollectionBean bean : beans)
      collectionDatasets.put(bean.getPath(), bean);
  }

    /////////////////////////////////////////////////////////////////

  // create it each time for thread safety, and so that collection is updated
  public FeatureDatasetPoint getFeatureCollectionDataset(String uri, String path) throws IOException {

    //FeatureDatasetPoint fd = fdmap.get(path);
    //if (fd == null) {
      CollectionBean config = collectionDatasets.get(path);
      if (config == null) return null;

      Formatter errlog = new Formatter();
      FeatureDatasetPoint fd = (FeatureDatasetPoint) CompositeDatasetFactory.factory(uri, FeatureType.getType(config.getFeatureType()), config.getSpec(), errlog);
      if (fd == null) {
        log.error("Error opening CompositeDataset path = "+path+"  errlog = ", errlog);
        return null;
      }

    // kludge
    DateRange dr = fd.getDateRange();
    if ((dr != null) && (config.getResolution() != null))
      try {
        dr.setResolution( new TimeDuration(config.getResolution()));
      } catch (ParseException e) {
        log.error("TimeDuration incorrect= "+config.getResolution(), e);
      }

    //if (config.getRaw() != null) {
    //  fd.addGlobalAttribute(new Attribute("_raw", config.getRaw()));
    //}
      //fdmap.put(path, fd);
    //}
    return fd;
  }

  // one could use this for non-collection datasets
  public FeatureDatasetPoint getFeatureDataset(HttpServletRequest req, HttpServletResponse res, String path) throws IOException {
    NetcdfDataset ncd = null;
    try {
      NetcdfFile ncfile = DatasetHandler.getNetcdfFile(req, res, path);
      if (ncfile == null) {
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return null;
      }

      ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getEnhanceAll());
      Formatter errlog = new Formatter();
      FeatureDatasetPoint fd = (FeatureDatasetPoint) FeatureDatasetFactoryManager.wrap(FeatureType.STATION, ncd, null, errlog);
      if (fd == null) {
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, -1));
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, errlog.toString());
        if (ncd != null) ncd.close();
        return null;
      }

      return fd;

    } catch (FileNotFoundException e) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());

    } catch (Throwable e) {
      e.printStackTrace();
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }

    if (ncd != null) ncd.close();
    return null;
  }

  public void show(Formatter f) {
    for (CollectionBean bean : collectionDatasets.values()) {
      f.format(" %s == %s%n", bean.getPath(), bean.getSpec());
    }    
  }

}
