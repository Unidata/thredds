/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.catalog.crawl;

import ucar.nc2.dataset.*;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.IO;
import ucar.nc2.util.NamedObject;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.geoloc.*;

import java.io.*;
import java.util.List;
import java.util.Formatter;

import thredds.catalog.*;
import ucar.unidata.util.Format;
import ucar.unidata.util.Parameter;
import ucar.unidata.util.StringUtil2;

/**
 * Utilities for extracting info from a catalog.
 *
 * @author John Caron
 */

public class CatalogExtractor implements CatalogCrawler.Listener {
  private boolean verbose = true;

  private InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);
  private ThreddsDataFactory tdataFactory = new ThreddsDataFactory();
  private int countDatasets, countNoAccess, countNoOpen;
  private PrintWriter out;
  private String transferDir = "C:/data/bad/";
  private String copyDir = null;

  /**
   * Constuctor
   * @param verbose output status messages
   */
  public CatalogExtractor(boolean verbose) {
    this.verbose = verbose;
  }

  public void copy(String catUrl, String copyToDir, CancelTask task) throws IOException {
    this.copyDir = copyToDir;

    InvCatalogImpl cat = catFactory.readXML(catUrl);
    StringBuilder buff = new StringBuilder();
    if (!cat.check(buff, false))
      return;

    countDatasets = 0;
    countNoAccess = 0;
    countNoOpen = 0;
    int countCatRefs = 0;
    CatalogCrawler crawler = new CatalogCrawler(CatalogCrawler.USE_ALL_DIRECT, false, new CatalogCrawler.Listener() {
      public void getDataset(InvDataset dd, Object context) {
        InvAccess access = tdataFactory.chooseDatasetAccess(dd.getAccess());
        if (null != access) transfer(access.getStandardUrlName(), copyDir);
      }
      public boolean getCatalogRef(InvCatalogRef dd, Object context) { return true; }
    });

    long start = System.currentTimeMillis();
    try {
      countCatRefs = crawler.crawl(cat, task, out, null);
    } finally {
      int took = (int) (System.currentTimeMillis() - start) / 1000;

      out.println("***Done " + catUrl + " took = " + took + " secs\n" +
              "   datasets=" + countDatasets + " no access=" + countNoAccess + " open failed=" + countNoOpen + " total catalogs=" + countCatRefs);
      if (verbose) System.out.println("***Done " + catUrl + " took = " + took + " secs\n" +
              "   datasets=" + countDatasets + " no access=" + countNoAccess + " open failed=" + countNoOpen + " total catalogs=" + countCatRefs);
    }
  }

  public void extractLoop(PrintWriter out, String catUrl, int type, boolean skipDatasetScan, CancelTask task) throws IOException {
    while (true) {
      extract(out, catUrl, type, skipDatasetScan, task);
      if ((task != null) && task.isCancel())
        break;
    }
  }

  public void extract(PrintWriter out, String catUrl, int type, boolean skipDatasetScan, CancelTask task) throws IOException {
    this.out = out;

    out.println("***read " + catUrl);

    InvCatalogImpl cat = catFactory.readXML(catUrl);
    StringBuilder buff = new StringBuilder();
    boolean isValid = cat.check(buff, false);
    if (!isValid) {
      System.out.println("***Catalog invalid= " + catUrl + " validation output=\n" + buff);
      out.println("***Catalog invalid= " + catUrl + " validation output=\n" + buff);
      return;
    }
    out.println("catalog <" + cat.getName() + "> is valid");
    out.println(" validation output=\n" + buff);

    countDatasets = 0;
    countNoAccess = 0;
    countNoOpen = 0;
    int countCatRefs = 0;
    CatalogCrawler crawler = new CatalogCrawler(type, skipDatasetScan, this);
    long start = System.currentTimeMillis();
    try {
      countCatRefs = crawler.crawl(cat, task, out, null);
    } finally {
      int took = (int) (System.currentTimeMillis() - start) / 1000;

      out.println("***Done " + catUrl + " took = " + took + " secs\n" +
              "   datasets=" + countDatasets + " no access=" + countNoAccess + " open failed=" + countNoOpen + " total catalogs=" + countCatRefs);

      if (verbose) {
        System.out.println("***Done " + catUrl + " took = " + took + " secs\n" +
                "   datasets=" + countDatasets + " no access=" + countNoAccess + " open failed=" + countNoOpen + " total catalogs=" + countCatRefs);
      }
    }
  }

  public void getDataset(InvDataset ds, Object context) {
    countDatasets++;
    openDataset(out, ds);
    //return extractTypedDatasetInfo( out, ds);
  }

  public boolean getCatalogRef(InvCatalogRef dd, Object context) { return true; }
  

  public boolean openDataset(PrintWriter out, InvDataset ds) {
    InvAccess access = tdataFactory.chooseDatasetAccess(ds.getAccess());
    if (access == null) {
      countNoAccess++;
      out.println("  **FAILED to find access " + ds.getName());
      System.out.println("  **FAILED to find access " + ds.getName());
      return false;
    }

    NetcdfDataset ncd = null;
    long start = System.currentTimeMillis();
    try {
      // ncd = NetcdfDataset.openDataset( access.getStandardUrlName());
      Formatter log = new Formatter();
      ncd = tdataFactory.openDataset(access, true, null, log);
      if (ncd == null) {
        countNoOpen++;
        out.println("  **FAILED to open " + access.getStandardUrlName() + " " + log);
        System.out.println("  **FAILED to open " + access.getStandardUrlName() + " " + log);
        transfer(access.getStandardUrlName(), transferDir);
        return false;
      }


      int took = (int) (System.currentTimeMillis() - start);
      boolean ok = true; // ucar.nc2.iosp.nexrad2.TestNexrad2.testCoordSystem( ncd);
      out.println("  **Open " + ds.getDataType() + " " + ncd.getLocation() + " (" + ds.getName() + ") " + took + " msecs");
      if (verbose)
        System.out.println("  **Open " + ds.getDataType() + " " + ncd.getLocation() + " (" + ds.getName() + ") " + took + " msecs");

    } catch (Throwable e) {
      countNoOpen++;
      out.println("  **FAILED to open " + access.getStandardUrlName());
      System.out.println("  **FAILED to open " + access.getStandardUrlName());
      transfer(access.getStandardUrlName(), transferDir);
      return false;

    } finally {

      if (ncd != null) try {
        ncd.close();
        out.println("   Close " + ncd.getLocation());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return true;
  }

  private void transfer(String url, String copyToDir) {
    String new_url = StringUtil2.substitute(url, "dodsC", "fileServer");
    int pos = url.lastIndexOf('/');
    String filename = url.substring(pos + 1);
    File file = new File(copyToDir, filename);
    IO.readURLtoFile(new_url, file);
    System.out.println("  **copied to " + file.getPath() + " size=" + file.length());
  }

  public boolean extractTypedDatasetInfo(PrintWriter out, InvDataset ds) {
    boolean ok = true;

    long start = System.currentTimeMillis();
    ThreddsDataFactory.Result result = null;
    try {
      result = tdataFactory.openFeatureDataset(ds, null);
      int took = (int) (System.currentTimeMillis() - start);
      if (verbose)
        System.out.println("  **Open " + result.featureType + " " + result.location + " (" + ds.getName() + ") " + took + " msecs");
      out.println("  **Open " + result.featureType + " " + result.location + " (" + ds.getName() + ") " + took + " msecs");

      if (result.location == null)
        ok = false;
      else if (result.featureType == FeatureType.GRID)
        extractGridDataset(out, (GridDataset) result.featureDataset);

    } catch (Throwable e) {
      out.println("   **FAILED " + ds.getName());
      e.printStackTrace(out);
      e.printStackTrace();
      return false;

    } finally {

      if ((result != null) && (result.featureDataset != null))  try {
        result.featureDataset.close();
        out.println("   Close " + result.featureType + " " + result.location);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return ok;
  }


  private void extractGridDataset(PrintWriter out, GridDataset gridDs) {

    if (!verbose) {
      out.println("    ngrids = " + gridDs.getGrids().size());
      return;
    }
    if (gridDs == null) return;

    out.println("Global Attributes");
    NetcdfDataset ds = (NetcdfDataset) gridDs.getNetcdfFile();
    showAtts(out, ds.getGlobalAttributes());
    out.println();

    GridCoordSystem gcsMax = null;
    LatLonRect llbbMax = null;

    LatLonRect llbb = null;
    CalendarDateRange dateRange = null;
    long nx = 0, ny = 0;

    for (GridDataset.Gridset gset : gridDs.getGridsets()) {
      GridCoordSystem gcs = gset.getGeoCoordSystem();

      CoordinateAxis1D xaxis = (CoordinateAxis1D) gcs.getXHorizAxis();
      CoordinateAxis1D yaxis = (CoordinateAxis1D) gcs.getYHorizAxis();
      long nx2 = xaxis.getSize();
      long ny2 = yaxis.getSize();
      if ((nx != nx2) || (ny != ny2)) {
        nx = nx2;
        ny = ny2;
        double dx = xaxis.getIncrement();
        double dy = yaxis.getIncrement();
        out.println("  horizontal = " + nx + " by " + ny + " points, resolution " + Format.d(dx, 4) + " " + Format.d(dy, 4)
            + " " + xaxis.getUnitsString());
      }

      ProjectionImpl proj = gcs.getProjection();
      if (proj != null) {
        out.println(", " + proj.getClassName() + " projection;");

        List<Parameter> params = proj.getProjectionParameters();
        for (Parameter p : params)
          out.println("       " + p.getName() + " " + p.getStringValue());

      } else
        out.println();

      LatLonRect llbb2 = gcs.getLatLonBoundingBox();
      if ((llbb == null) || !llbb2.equals(llbb)) {
        llbb = llbb2;

        if (llbbMax == null)
          llbbMax = llbb;
        else
          llbbMax.extend(llbb);

        if (llbb.getWidth() >= 360.0) {
          out.println("  BoundingBox == GLOBAL");

        } else {
          StringBuilder buff = new StringBuilder();
          LatLonPointImpl ll = llbb.getLowerLeftPoint();
          LatLonPointImpl ur = llbb.getUpperRightPoint();
          buff.append(Double.toString(ll.getLongitude()));
          buff.append(" ");
          buff.append(Double.toString(ll.getLatitude()));
          buff.append(" ");
          buff.append(Double.toString(ur.getLongitude()));
          buff.append(" ");
          buff.append(Double.toString(ur.getLatitude()));
          buff.append(" ");
          out.println("  BoundingBox == " + buff + " width= " + llbb.getWidth() + " " + (llbb.getWidth() >= 360.0 ? "global" : ""));
        }

      }

      CoordinateAxis1DTime taxis = gcs.getTimeAxis1D();
      CalendarDateRange dateRange2 = gcs.getCalendarDateRange();
      if ((taxis != null) && ((dateRange == null) || !dateRange2.equals(dateRange))) {

        long ntimes = taxis.getSize();
        try {
          TimeUnit tUnit = taxis.getTimeResolution();
          if (dateRange != null)
            out.printf("  DateRange == %s duration = %d secs; ntimes = %d  data resolution = %s%n", dateRange, dateRange.getDurationInSecs(), ntimes, tUnit);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      CoordinateAxis1D vaxis = gcs.getVerticalAxis();
      if (vaxis != null) {
        long nvert = vaxis.getSize();
        out.print("  Vertical axis= " + vaxis.getFullName() + " units=" + vaxis.getUnitsString() + " size= " + nvert);
        VerticalCT vt = gcs.getVerticalCT();
        if (vt != null)
          out.print(" transform= " + vt.getVerticalTransformType());

        List<NamedObject> vertNames = vaxis.getNames();
        for (NamedObject vertName : vertNames)
          out.print(" " + vertName);
        out.println();

        if ((gcsMax == null) || (gcsMax.getVerticalAxis().getSize() < vaxis.getSize()))
          gcsMax = gcs;
      }

    }

    /* ThreddsMetadata.GeospatialCoverage gc = new ThreddsMetadata.GeospatialCoverage();
gc.setBoundingBox(llbbMax);
if (gcsMax != null) {
 gc.setVertical(gcsMax.getVerticalAxis());
 gc.setZPositiveUp(gcsMax.isZPositive());
}

try {
 gc.toXML(out);
} catch (IOException e) {
 e.printStackTrace();
}     */

    /* java.util.List grids = gridDs.getGrids();
String fileFormat = ds.findAttValueIgnoreCase(null, "FileFormat", "");
if (fileFormat.equals("GRIB-1"))
makeGrib1Vocabulary(grids, out);   */


  }

  private void showAtts(PrintWriter out, List<Attribute> atts) {
    for (Attribute att : atts)
      out.println("  " + att);
  }

  private void makeGrib1Vocabulary(List<GridDatatype> grids, PrintWriter out) {
    String stdName;
    out.println("\n<variables vocabulary='GRIB-1'>");
    for (GridDatatype grid : grids) {
      Attribute att = grid.findAttributeIgnoreCase("GRIB_param_number");
      stdName = (att != null) ? att.getNumericValue().toString() : null;

      out.print("  <variable name='");
      out.print(grid.getFullName());
      out.print("' vocabulary_name='");
      out.print(stdName != null ? stdName : "dunno");
      out.print("' units='");
      out.print(grid.getUnitsString());
      out.println("'/>");
    }
    out.println("</variables>");
  }

  /* private void showDatasetInfo(InvDataset ds) {
    if (ds == null) return;

    String bbString = ds.findProperty("BoundingBox");
    if (bbString == null) {
      displayMap.drawBoundingBox( null);
      return;
    }
    if (Debug.isSet("extract/boundingBox")) System.out.println("showDatasetInfo BoundingBox attribute= "+bbString);

    ProjectionRect bb = null;
    if (bbString.equals("global")) {
      LatLonProjection displayProj = (LatLonProjection) displayMap.getProjectionImpl();
      double centerLon = displayProj.getCenterLon();
      bb = new ProjectionRect(centerLon-180.0, -90.0, centerLon+180.0, 90.0);

    } else {

      double[] dval = new double[4];
      int count = 0;
      StringTokenizer stoke = new StringTokenizer(bbString);
      while (stoke.hasMoreTokens() && (count < 4)) {
        String token = stoke.nextToken();
        try{
          dval[count] = Double.parseDouble(token);
          count++;
        } catch (NumberFormatException e) {}
      }

      bb = new ProjectionRect(dval[0], dval[1], dval[2], dval[3]);
    }

    displayMap.drawBoundingBox( bb);
  } */


}