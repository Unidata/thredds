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
package thredds.server.cdmremote.stream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import thredds.server.cdmremote.PointWriter;
import thredds.server.cdmremote.StationWriter;
import thredds.server.cdmremote.params.CdmrfQueryBean;
import thredds.server.config.TdsContext;
import thredds.servlet.ServletUtil;
import thredds.server.config.ThreddsConfig;
import thredds.util.ContentType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamWriter;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.util.StringUtil2;

/**
 * @author mhermida
 */
public final class CdmrfStreamFactory {

  private static final Logger log = org.slf4j.LoggerFactory.getLogger(CdmrfStreamFactory.class);

  private static CdmrfStreamFactory INSTANCE;

  private DiskCache2 cdmrCache;

  private CdmrfStreamFactory(DiskCache2 cdmrCache) {
    this.cdmrCache = cdmrCache;
  }

  public static CdmrfStreamFactory getInstance(TdsContext tdsContext) {
    if (INSTANCE == null) {
      String dir = ThreddsConfig.get("CdmRemote.dir", new File(tdsContext.getContentDirectory().getPath(), "/cache/cdmr/").getPath());
      int scourSecs = ThreddsConfig.getSeconds("CdmRemote.scour", 30 * 60);
      int maxAgeSecs = ThreddsConfig.getSeconds("CdmRemote.maxAge", 60 * 60);
      DiskCache2 cdmrCache = new DiskCache2(dir, false, maxAgeSecs / 60, scourSecs / 60);
      INSTANCE = new CdmrfStreamFactory(cdmrCache);
    }
    return INSTANCE;
  }

  public void headerStream(String absPath, HttpServletResponse res, FeatureDatasetPoint fdp, CdmrfQueryBean query) throws IOException {

    res.setContentType(ContentType.binary.getContentHeader());
    res.setHeader("Content-Description", "ncstream");

    NetcdfFile ncfile = fdp.getNetcdfFile(); // LOOK will fail
    NcStreamWriter ncWriter = new NcStreamWriter(ncfile, absPath);

    OutputStream out = new BufferedOutputStream(res.getOutputStream(), 10 * 1000);
    long size = ncWriter.sendHeader(out);
    NcStream.writeVInt(out, 0);

    out.flush();
    res.flushBuffer();
  }

  public void dataStream(HttpServletRequest req, HttpServletResponse res, FeatureDatasetPoint fdp, String path, CdmrfQueryBean qb) throws IOException {

    switch (fdp.getFeatureType()) {
      case POINT:
        pointDataStream(req, res, fdp, path, qb);
        break;
      case STATION:
        stationDataStream(req, res, fdp, path, qb);
        break;
    }

  }

  private void pointDataStream(HttpServletRequest req, HttpServletResponse res, FeatureDatasetPoint fdp, String path, CdmrfQueryBean qb) throws IOException {

    List<FeatureCollection> coll = fdp.getPointFeatureCollectionList();
    PointFeatureCollection pfc = (PointFeatureCollection) coll.get(0);
    PointWriter pointWriter = new PointWriter(fdp, pfc, qb, cdmrCache);

    // query validation - second pass
    if (!pointWriter.validate(res)) {
      return;
    }

    // set content type, description
    //res.setContentType(getContentType(qb));
    res.setContentType(qb.getResponseType().toString());
    //if (null != getContentDescription(qb))
    //  res.setHeader("Content-Description", getContentDescription(qb));

    // special handling for netcdf files
    CdmrfQueryBean.ResponseType resType = qb.getResponseType();
    if (resType == CdmrfQueryBean.ResponseType.netcdf) {
      if (path.startsWith("/")) path = path.substring(1);
      path = StringUtil2.replace(path, "/", "-");
      res.setHeader("Content-Disposition", "attachment; filename=" + path + ".nc");

      File file = pointWriter.writeNetcdf();
      //ServletUtil.returnFile(req, res, file, getContentType(qb));
      ServletUtil.returnFile(req, res, file, "application/x-netcdf");
      if (!file.delete()) {
        log.warn("file delete failed =" + file.getPath());
      }


    } else {

      // otherwise stream it out
      PointWriter.Writer w = pointWriter.write(res);
    }
  }

  private void stationDataStream(HttpServletRequest req, HttpServletResponse res, FeatureDatasetPoint fdp, String path, CdmrfQueryBean qb) throws IOException {
    long start = 0;
    //if (showTime) {
    //  start = System.currentTimeMillis();
    //  ucar.unidata.io.RandomAccessFile.setDebugAccess(true);  // LOOK !!
    //}

    List<FeatureCollection> coll = fdp.getPointFeatureCollectionList();
    StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection) coll.get(0);

    StationWriter stationWriter = new StationWriter(fdp, sfc, qb, cdmrCache);
    if (!stationWriter.validate(res)) {
      return; // error was sent
    }

    // set content type, description
    //res.setContentType(getContentType(qb));
    res.setContentType(qb.getResponseType().toString());

    //if (null != getContentDescription(qb))
    //  res.setHeader("Content-Description", getContentDescription(qb));

    // special handling for netcdf files
    CdmrfQueryBean.ResponseType resType = qb.getResponseType();
    if (resType == CdmrfQueryBean.ResponseType.netcdf) {
      if (path.startsWith("/")) path = path.substring(1);
      path = StringUtil2.replace(path, "/", "-");
      res.setHeader("Content-Disposition", "attachment; filename=" + path + ".nc");

      File file = stationWriter.writeNetcdf();
      //ServletUtil.returnFile(req, res, file, getContentType(qb));
      ServletUtil.returnFile(req, res, file, "application/x-netcdf");
      if (!file.delete()) {
        log.warn("file delete failed =" + file.getPath());
      }

      //if (showTime) {
      //  long took = System.currentTimeMillis() - start;
      //  System.out.println("\ntotal response took = " + took + " msecs");
      //}

      //return null;
    }

    // otherwise stream it out
    StationWriter.Writer w = stationWriter.write(res);

    //if (showTime) {
    //  long took = System.currentTimeMillis() - start;
    //  System.out.printf("%ntotal response took %d msecs nobs = %d%n  seeks= %d nbytes read= %d%n", took, w.count,
    //          ucar.unidata.io.RandomAccessFile.getDebugNseeks(), ucar.unidata.io.RandomAccessFile.getDebugNbytes());
    //  ucar.unidata.io.RandomAccessFile.setDebugAccess(false);  // LOOK !!
    //}
  }
}
