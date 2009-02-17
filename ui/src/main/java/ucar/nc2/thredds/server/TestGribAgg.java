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

import thredds.catalog.crawl.CatalogCrawler;
import thredds.catalog.*;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.Variable;
import ucar.nc2.NCdump;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.ma2.Array;

import java.io.IOException;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.util.*;


public class TestGribAgg implements CatalogCrawler.Listener {
  static boolean showAll = false;

  private ThreddsDataFactory tdataFactory = new ThreddsDataFactory();
  private PrintStream out = null;
  private ArrayList fileList = new ArrayList();

  TestGribAgg(String catalog, PrintStream out, boolean showTimeDims) throws IOException {
    this.out = out;

    CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.USE_ALL, false, this);
    crawler.crawl( catalog, null, out);
    report( out, showTimeDims);

    for (int i = 0; i < fileList.size(); i++) {
      GridDataset ds = (GridDataset) fileList.get(i);
      ds.close();
    }
  }

  TestGribAgg(InvDataset top, PrintStream out) throws IOException {
    this.out = out;
    CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.USE_ALL, false, this);
    crawler.crawlDirectDatasets( top, null, out);
    report( out, false);

    for (int i = 0; i < fileList.size(); i++) {
      GridDataset ds = (GridDataset) fileList.get(i);
      ds.close();
    }
  }

  private boolean first = true;
  public void getDataset(InvDataset dd) {
    if (null != dd.getAccess( ServiceType.RESOLVER))
      return;

    // throw the first one away, in case its not complete
    if (first) {
      first = false;
      return;
    }

    ThreddsDataFactory.Result result;
    try {
      result = tdataFactory.openFeatureDataset( dd, null);
      if (result.fatalError) {
        out.println("***CAN'T OPEN "+dd.getName());
        return;
      }
      process( result.featureDataset.getNetcdfFile());
      fileList.add( result.featureDataset);

    } catch (IOException e) {
      e.printStackTrace();
    }
    return;
  }

  public boolean getCatalogRef(InvCatalogRef dd) { return true; }
  
  private HashMap hash = new HashMap();
  private void process(NetcdfFile ncd) {
    out.println(" process "+ncd.getLocation());

    List vars = ncd.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable v = (Variable) vars.get(i);

      if (!v.isCoordinateVariable()) { // only data variables
        UberVariable uv = (UberVariable) hash.get( v.getName());
        if (uv == null) {
          uv = new UberVariable(v);
          hash.put( v.getName(), uv);
        } else
          uv.vars.add( v);
      }
    }
  }

  private void report(PrintStream out, boolean showTimeDims) {
    List values = new ArrayList(hash.values());
    Collections.sort( values);
    for (int i = 0; i < values.size(); i++) {
      UberVariable uv = (UberVariable) values.get(i);
      uv.reportDims(out);
      if (showTimeDims) uv.reportTimeDims( out);
    }
  }

  private class UberVariable implements Comparable {
    Variable firstv;
    ArrayList vars = new ArrayList();

    UberVariable( Variable v) { this.firstv = v; }

    public int compareTo(Object o) {
      UberVariable ov = (UberVariable) o;
      return getName().compareTo( ov.getName());
    }

    public String getName() { return firstv.getName(); }

    private void reportTimeDims(PrintStream out) {
      /* StringBuffer buff = new StringBuffer();
      firstv.getNameAndDimensions(buff, false, true);
      out.println(buff+": "); */

      boolean showed = false;
      int[] firstshape = firstv.getShape();
      int dim = 0;
      for (int i = 0; i < vars.size(); i++) {
        Variable v = (Variable) vars.get(i);
        int[] shape = v.getShape();
        if ((dim >= shape.length) || (shape[dim] != firstshape[dim] ) || showAll) {
          showTimeCoordinates(out);
          showed = true;
          break;
        }
      }
      if (showed) out.println();
    }

    private void showTimeCoordinates(PrintStream out) {
      showTimeCoordinates( out, firstv);
       for (int i = 0; i < vars.size(); i++) {
         Variable v = (Variable) vars.get(i);
         showTimeCoordinates( out, v);
       }
     }

    private void showTimeCoordinates(PrintStream out, Variable v) {
      if (v.getRank() == 0) return;
      Dimension d = v.getDimension(0);
      List list = d.getCoordinateVariables();
      if (list.size() == 0) return;
      Variable timeV = (Variable) list.get(0);

      Array data = null;
      try {
        data = timeV.read();
      } catch (IOException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      NCdump.printArray(data, timeV.getName(), out, null);
    }

    private void reportDims(PrintStream out) {
      StringBuffer buff = new StringBuffer();
      firstv.getNameAndDimensions(buff);
      out.println(buff+": ");

      boolean showed = false;
      int[] firstshape = firstv.getShape();
      for (int dim=0; dim<firstshape.length; dim++) {

        for (int i = 0; i < vars.size(); i++) {
          Variable v = (Variable) vars.get(i);
          int[] shape = v.getShape();
          if ((dim >= shape.length) || (shape[dim] != firstshape[dim] ) || showAll) {
            showShape(out, dim);
            // showDimNames(out, dim);
            showed = true;
            break;
          }
        }
      }
      if (showed) out.println();
    }

    private void showShape(PrintStream out, int dim) {
      out.print(" dim "+dim+"=[");
      int[] shape = firstv.getShape();
      if (dim < shape.length)
         out.print(shape[dim]);
      else
         out.print("*");

      for (int i = 0; i < vars.size(); i++) {
        Variable v = (Variable) vars.get(i);
        shape = v.getShape();
        if (dim < shape.length)
           out.print(","+shape[dim]);
        else
           out.print(",*");
      }
      out.println("]");
    }

    private void showDimNames(PrintStream out, int dim) {
      out.print(" dim "+dim+"=[");
      if (dim < firstv.getRank())
         out.print(firstv.getDimension(dim).getName());
      else
         out.print("*");

      for (int i = 0; i < vars.size(); i++) {
        Variable v = (Variable) vars.get(i);
        if (dim < v.getRank())
          out.print(","+v.getDimension(dim).getName());
        else
           out.print(",*");
      }
      out.println("]");
    }

  } // end class

  private static void findDatasetScan( String catUrl, PrintStream out) throws IOException {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);
    InvCatalogImpl cat = catFactory.readXML(catUrl);
    StringBuilder buff = new StringBuilder();
    boolean isValid = cat.check(buff, false);
    out.println("catalog <" + cat.getName() + "> " + (isValid ? "is" : "is not") + " valid");
    out.println(" validation output=\n" + buff);
    if (!isValid) return;

    List datasets = cat.getDatasets();
    findDatasetScan( datasets, out);
  }

  private static void findDatasetScan( List datasets, PrintStream out) throws IOException {
    for (int i = 0; i < datasets.size(); i++) {
      InvDataset ds = (InvDataset) datasets.get(i);
      if (null != ds.findProperty("DatasetScan")) {
        out.println(ds.getName());
        new TestGribAgg(ds, out);
      } else {
        findDatasetScan( ds.getDatasets(), out);
      }
    }

  }

    public static void main(String args[]) throws IOException {
      FileOutputStream fos = new FileOutputStream("C:/dev/netcdf-java-2.2/doc/TestGribAgg.txt");
      PrintStream out = new PrintStream( fos);
      /* findDatasetScan("http://motherlode.ucar.edu:9080/thredds/idd/dgex_model.xml", out);
      findDatasetScan("http://motherlode.ucar.edu:9080/thredds/idd/gfs_model.xml", out);  */
      findDatasetScan("http://motherlode.ucar.edu:9080/thredds/idd/ruc_model.xml", out);
      findDatasetScan("http://motherlode.ucar.edu:9080/thredds/idd/nam_model.xml", out);

      //showAll =  true;
      //new TestGribAgg("http://motherlode.ucar.edu:9080/thredds/idd/model/GFS/N_Hemisphere_381km/catalog.xml", System.out, true);
    }


}
