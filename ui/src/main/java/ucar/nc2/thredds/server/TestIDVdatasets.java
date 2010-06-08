// $Id: TestIDVdatasets.java 68 2006-07-13 00:08:20Z caron $
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
package ucar.nc2.thredds.server;

import thredds.catalog.*;
import ucar.nc2.units.DateRange;

import java.io.*;
import java.util.List;

import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.VerticalCT;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.units.TimeUnit;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.Format;

/**
 * @author john
 */
public class TestIDVdatasets {
  boolean extract = false;
  String skip;
  int countDone = 0, maxDone= Integer.MAX_VALUE;
  int errCount = 0;
  InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);
  ThreddsDataFactory tdataFactory = new ThreddsDataFactory();

  /**
   *
   * @param out
   * @param catName catalog URL
   * @param doOneOnly do one, then exit
   * @param skip ship any dataset with this name
   * @param maxDone dif > 0, only do up to this number of datasets
   */
  void extract( PrintStream out, String catName, boolean doOneOnly, String skip, int maxDone) {
    this.skip = skip;
    if (maxDone > 0) this.maxDone = maxDone;

    out.println("******* read "+catName);
    InvCatalogImpl cat;
    try {
      cat = catFactory.readXML(catName);
      StringBuilder buff = new StringBuilder();
      boolean isValid = cat.check( buff, false);
      out.println("catalog <" + cat.getName()+ "> "+ (isValid ? "is" : "is not") + " valid");
      out.println(" validation output=\n" + buff);
    } catch (Exception e) {
      e.printStackTrace(out);
      return;
    }

    out.println("***CATALOG "+cat.getCreateFrom());
    extractDatasetInfo(out, cat.getDatasets(), doOneOnly);
  }

  // breadth first
  public void extractDatasetInfo(PrintStream out, List datasets, boolean doOneOnly) {
    if (countDone > maxDone) return;

    for (int i = 0; i < datasets.size(); i++) {

      InvDataset ds = (InvDataset) datasets.get(i);
      out.print(" DATASET "+ds.getName()+" id= "+ds.getID());
      if (ds instanceof InvCatalogRef) {
        InvCatalogRef catref = (InvCatalogRef) ds;
        out.print(" catref=" + catref.getURI());
      }

      if (ds.getName().equals(skip)) {
        out.println(" SKIP ");
        continue;
      }

      if (ds.hasAccess()) {
        ThreddsDataFactory.Result tdata = null;
        try {
          long start = System.currentTimeMillis();
          try {
            tdata = tdataFactory.openFeatureDataset(ds, null);
            if (tdata.fatalError) {
              out.println("  *ERROR " + tdata.errLog);
              if (doOneOnly) break;
            }

          } catch (Throwable e) {
            out.println(errCount + "  *FAILED to open ");
            e.printStackTrace(out);
            errCount++;
            continue;
          }
          int took = (int) (System.currentTimeMillis() - start);
          InvAccess access = tdata.accessUsed;
          String st = (access == null) ? " UNKNOWN" : access.getService().getServiceType().toString();

          if (tdata.featureType == FeatureType.GRID) {
            out.println(" *Opened " + countDone + " GRID " + tdata.location + " " + st + " (" + took + " msecs)");
            if (extract) extractGrid(out, (GridDataset) tdata.featureDataset);
          } else if (tdata.featureType == FeatureType.POINT) {
            out.println(" *Opened " + countDone + " TYPE " + ds.getDataType() + " " + tdata.location + " " + st);
          } else if (tdata.featureType == FeatureType.STATION) {
            out.println(" *Opened " + countDone + " TYPE " + ds.getDataType() + " " + tdata.location + " " + st);
          }

        } finally {
            try {
              if ((tdata != null) &&(tdata.featureDataset != null)) tdata.featureDataset.close();
            } catch (IOException ioe) {
              ioe.printStackTrace();
            }
        }

        countDone++;
        if (countDone > maxDone) return;
        if (doOneOnly) break;
      } else
        out.println();
    }

    if (countDone > maxDone) return;

    // recurse
    for (int i = 0; i < datasets.size(); i++) {
      InvDataset ds = (InvDataset) datasets.get(i);
      if (ds.getName().equals(skip)) {
        out.println(" SKIP ");
        continue;
      }

      if (countDone > maxDone)
        return;
      extractDatasetInfo(out, ds.getDatasets(), doOneOnly);
    }
  }

  private void extractGrid(PrintStream out, GridDataset gridDs) {

    NetcdfFile ds = gridDs.getNetcdfFile();
      if (gridDs == null) return;

      java.util.List grids = gridDs.getGrids();
      String fileFormat = ds.findAttValueIgnoreCase(null, "FileFormat", "");
      out.println("   "+grids.size()+" grids; file format="+fileFormat);

      GridCoordSystem gcsMax = null;
      LatLonRect llbbMax = null;

      LatLonRect llbb = null;
      DateRange dateRange = null;
      long nx = 0, ny = 0;

      java.util.Iterator iter = gridDs.getGridsets().iterator();
      while (iter.hasNext()) {
        GridDataset.Gridset gset = (GridDataset.Gridset) iter.next();
        GridCoordSystem gcs = gset.getGeoCoordSystem();

        // horizontal
        long nx2 = gcs.getXHorizAxis().getSize();
        long ny2 = gcs.getYHorizAxis().getSize();

        if ((nx != nx2) || (ny != ny2)) {

          if ((gcs.getXHorizAxis() instanceof CoordinateAxis1D) &&
              (gcs.getYHorizAxis() instanceof CoordinateAxis1D)) {

            CoordinateAxis1D xaxis = (CoordinateAxis1D) gcs.getXHorizAxis();
            CoordinateAxis1D yaxis = (CoordinateAxis1D) gcs.getYHorizAxis();

            nx = nx2;
            ny = ny2;
            double dx = xaxis.getIncrement();
            double dy = yaxis.getIncrement();
            out.print("   horizontal = " + nx + " by " + ny + " points, resolution " + Format.d(dx, 4) + " " + Format.d(dy, 4)
                + " " + xaxis.getUnitsString());

          } else {
            out.print("   horizontal 2D = " + nx + " by " + ny+ " " + gcs.getXHorizAxis().getUnitsString());
          }

          ProjectionImpl proj = gcs.getProjection();
          if (proj != null) {
            out.print(", " + proj.getClassName() + " projection;");

            List params = proj.getProjectionParameters();
            for (int i = 0; i < params.size(); i++) {
              ucar.unidata.util.Parameter p = (ucar.unidata.util.Parameter) params.get(i);
              out.print("       " + p.getName() + " " + p.getStringValue());
            }
          }
          out.println();
        }

        LatLonRect llbb2 = gcs.getLatLonBoundingBox();
        if ((llbb == null) || !llbb2.equals(llbb)) {
          llbb = llbb2;

          if (llbbMax == null)
            llbbMax= llbb;
          else
            llbbMax.extend(llbb);

          if (llbb.getWidth() >= 360.0) {
            out.println("   BoundingBox == GLOBAL");

          } else {
            StringBuffer buff = new StringBuffer();
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
            out.println("   BoundingBox == " + llbb + " width= " + llbb.getWidth() + " " + (llbb.getWidth() >= 360.0 ? "global" : ""));
          }

        }

        // time
        CoordinateAxis1DTime taxis = gcs.getTimeAxis1D();
        DateRange dateRange2 = gcs.getDateRange();
        if (dateRange2 == null) {
          out.println("  NO DateRange");
        } else {
          if ((taxis != null) && ((dateRange == null) || !dateRange2.equals(dateRange))) {

            long ntimes = taxis.getSize();
            try {
              TimeUnit tUnit = null;
              if (taxis.isRegular()) {
                tUnit = taxis.getTimeResolution();
              }
              dateRange = new DateRange(dateRange2, "1 hour");
              out.println("   DateRange == " + "start= "+dateRange.getStart() +" end= "+dateRange.getEnd()+
                  " duration= "+ dateRange.getDuration()+" ntimes = "+ntimes+" data resolution = "+tUnit);
            } catch (Exception e) {
              e.printStackTrace(out);
            }
          }
        }

        // vertical
        CoordinateAxis1D vaxis = gcs.getVerticalAxis();
        if (vaxis != null) {
          long nvert = vaxis.getSize();
          out.print("   Vertical axis= "+vaxis.getName()+" units="+vaxis.getUnitsString()+" size= "+ nvert);
          VerticalCT vt = gcs.getVerticalCT();
          if (vt != null)
            out.print(" transform= "+vt.getVerticalTransformType());

          List vertNames = vaxis.getNames();
          for (int i = 0; i < vertNames.size(); i++) {
            out.print(" "+vertNames.get(i));
          }
          out.println();

          if ((gcsMax == null) || (gcsMax.getVerticalAxis().getSize() < vaxis.getSize()))
            gcsMax= gcs;
        }
      }

      if (llbbMax == null)
        out.println("***NO BB");
      else {
        ThreddsMetadata.GeospatialCoverage gc = new ThreddsMetadata.GeospatialCoverage();
        gc.setBoundingBox(llbbMax);
        if (gcsMax != null) {
          gc.setVertical( gcsMax.getVerticalAxis());
          gc.setZPositiveUp( gcsMax.isZPositive());
       }
      /*  try {
          gc.toXML( out);
        } catch (IOException e) {
          e.printStackTrace();
        } */
      }

  }

  
  static public void main( String[] args)  throws Exception {
    String server = "http://motherlode.ucar.edu:8081/thredds";
    if (args.length > 0)
      server = args[0];

    TestIDVdatasets ts = new TestIDVdatasets();
    OutputStream out = new BufferedOutputStream(new FileOutputStream("C:/temp/servertest4.txt"));
    PrintStream pout = System.out; // new PrintStream( out);

    //ts.extract(System.out, "http://whoopee:8080/thredds/dodsC/model/catalog.xml", false);
    //ts.extract(System.out, "http://whoopee:8080/thredds/dodsC/radars.xml", false);


    //ts.extract(System.out, "http://lead.unidata.ucar.edu:8080/thredds/idv/latestModels.xml", false, null, 0);

    ts.extract(System.out, server + "/idv/models.xml", false, null, 0);
    //ts.extract(System.out, "http://motherlode.ucar.edu:8081/thredds/idv/rt-models.1.0.xml", false, null, 0);

    //ts.extract( pout, "http://motherlode.ucar.edu:9080/thredds/catalog.xml", true, "NEXRAD Radar", 0);
    //ts.extract( pout, "http://motherlode.ucar.edu:8088/thredds/dodsC/radars.xml", true, null, 0);

    //ts.extract(System.out, "http://motherlode.ucar.edu:8088/thredds/dodsC/radars.xml", true);
    // ts.extract(System.out, "http://whoopee:8080/thredds/dodsC/shared/testdata2/radar/nexrad/level3/catalog.xml", false);
  }


}