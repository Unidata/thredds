// $Id: MakeGribAggXML.java 51 2006-07-12 17:13:13Z caron $
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
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayInt;

import java.io.IOException;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.util.*;


public class MakeGribAggXML implements CatalogCrawler.Listener {

  private ThreddsDataFactory tdataFactory = new ThreddsDataFactory();
  private PrintStream out = null;

  MakeGribAggXML(String catalog, PrintStream out) {
    this.out = out;

    CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.USE_ALL_DIRECT, false, this);
    crawler.crawl( catalog, null, out, null);
    report( out);
  }

  MakeGribAggXML(InvDataset top, PrintStream out) {
    this.out = out;
    CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.USE_ALL_DIRECT, false, this);
    crawler.crawlDirectDatasets( top, null, out, null);
    report( out);
  }

  private boolean first = true;
  public void getDataset(InvDataset dd, Object context) {
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
      result.featureDataset.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public boolean getCatalogRef(InvCatalogRef dd, Object context) { return true; }

  private HashMap varHash = new HashMap();
  private void process(NetcdfFile ncd) {
    out.println(" process "+ncd.getLocation());

    List vars = ncd.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable v = (Variable) vars.get(i);

      if (v.isCoordinateVariable()) // data variables only
        continue;

      ArrayInt.D1 data = getTimeCoordinateData( v);
      if (data == null)
        continue; // only those with the time dimension

      TimeCoord tc = getTimeCoordinate(data);

      UberVariable uv = (UberVariable) varHash.get( v.getName());
      if (uv == null) {
        uv = new UberVariable(v);
        varHash.put( v.getName(), uv);
      }
      uv.addTimeCoord(tc);
    }
  }

  private void report(PrintStream out) {
    Collections.sort( times);
    showCoordinates(out);

    List varList = new ArrayList(varHash.values());

    // assign to a time sequence
    for (int i = 0; i < varList.size(); i++) {
      UberVariable uv = (UberVariable) varList.get(i);
      uv.seq = findTimeSequence( uv.timeCoords);
    }

    Collections.sort( varList);

    TimeSeq seq = null;
    for (int i = 0; i < varList.size(); i++) {
      UberVariable uv = (UberVariable) varList.get(i);

      if (seq != uv.seq)
        out.println("\n"+uv.seq.name+"=");
      seq = uv.seq;

      uv.report(out);
    }
  }

  private class UberVariable implements Comparable {
    ArrayList timeCoords = new ArrayList();
    String name;
    TimeSeq seq;

    UberVariable(Variable v) {
      this.name = v.getNameAndDimensions();
    }

    void addTimeCoord( TimeCoord tc) {
      timeCoords.add(tc);
    }

    TimeCoord getTimeCoord() {
      return (TimeCoord) timeCoords.get(0);
    }

    public int compareTo(Object o) {
      UberVariable ov = (UberVariable) o;

      int tcval = seq.compareTo( ov.seq);
      if (tcval == 0)
        return getName().compareTo( ov.getName());
      else
        return tcval;
    }

    public String getName() { return name; }

    private void report(PrintStream out) {
      out.print(name+":\n  ");

      for (int i = 0; i < timeCoords.size(); i++) {
        TimeCoord tc = (TimeCoord) timeCoords.get(i);
        if (i>0)out.print(",");
        if (tc == null)
          out.print("null");
        else
          out.print(tc.name);
      }
      out.println();
    }

  } // end UberVariable

  /////////////////////////////////////////////////

  private int seqno = 0;
  private ArrayList timeSequences = new ArrayList();
  private class TimeSeq implements Comparable {
    ArrayList seq;
    String name;

    TimeSeq( ArrayList seq) {
      this.seq = seq;
      name = "seq"+seqno;
      seqno++;
    }

    public boolean equalsData(ArrayList oseq) {
      if (seq.size() != oseq.size()) return false;
      for (int i=0; i<seq.size(); i++) {
        TimeCoord tc = (TimeCoord) seq.get(i);
        TimeCoord otc = (TimeCoord) oseq.get(i);
        if (!tc.equalsData( otc.data)) return false;
      }
      return true;
    }

    public int compareTo(Object o) {
      TimeSeq ov = (TimeSeq) o;
      return name.compareTo( ov.name);
    }
  }

  private TimeSeq findTimeSequence( ArrayList oseq) {
    for (int i = 0; i < timeSequences.size(); i++) {
      TimeSeq seq = (TimeSeq) timeSequences.get(i);
      if (seq.equalsData( oseq)) return seq;
    }
    TimeSeq seq = new TimeSeq( oseq);
    timeSequences.add(seq);
    return seq;
  }

    /////////////////////////////////////////////////
  private ArrayInt.D1 getTimeCoordinateData(Variable v) {
    if (v.getRank() == 0) return null;
    Dimension d = v.getDimension(0);
    List list = d.getCoordinateVariables();
    if (list.size() == 0) return null;
    Variable timeV = (Variable) list.get(0);

    Array data = null;
    try {
      data = timeV.read();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return (ArrayInt.D1) data;
  }

  private ArrayList times = new ArrayList();
  private class TimeCoord implements Comparable {
    ArrayInt.D1 data;
    String name;

    TimeCoord( Array data) {
      this.data = (ArrayInt.D1) data;
      name = getTimeCoordinateName("" + data.getSize());
    }

    public boolean equalsData(ArrayInt.D1 odata) {
      if (data.getSize() != odata.getSize()) return false;
      for (int i=0; i<data.getSize(); i++) {
        if (data.get(i) != odata.get(i)) return false;
      }
      return true;
    }

    public int compareTo(Object o) {
      TimeCoord ov = (TimeCoord) o;
      return name.compareTo( ov.name);
    }
  }

  private TimeCoord getTimeCoordinate( ArrayInt.D1 data) {
    if (data == null) return null;
    for (int i = 0; i < times.size(); i++) {
      TimeCoord tc = (TimeCoord) times.get(i);
      if (tc.equalsData( data)) return tc;
    }
    TimeCoord tc = new TimeCoord( data);
    times.add(tc);
    return tc;
  }

  private String getTimeCoordinateName( String n) {
    for (int i = 0; i < times.size(); i++) {
      TimeCoord tc = (TimeCoord) times.get(i);
      if (tc.name.equals(n))
        return getTimeCoordinateName(n+"p");
    }
    return n;
  }

  private void showCoordinates( PrintStream out) {
    for (int i = 0; i < times.size(); i++) {
      TimeCoord tc = (TimeCoord) times.get(i);
      out.print(tc.name+"=[");
      for (int j = 0; j < tc.data.getSize(); j++) {
        if (j>0)out.print(",");
        out.print(tc.data.get(j));
      }
      out.println("]");
    }
  }

  /////////////////////////////////////
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
        new MakeGribAggXML(ds, out);
      } else {
        findDatasetScan( ds.getDatasets(), out);
      }
    }

  }

    public static void main(String args[]) throws IOException {
      FileOutputStream fos = new FileOutputStream("C:/dev/netcdf-java-2.2/doc/TestGribAgg.txt");
      PrintStream out = new PrintStream( fos);
      findDatasetScan("http://motherlode.ucar.edu:9080/thredds/idd/dgex_model.xml", out);
      findDatasetScan("http://motherlode.ucar.edu:9080/thredds/idd/gfs_model.xml", out);
      findDatasetScan("http://motherlode.ucar.edu:9080/thredds/idd/ruc_model.xml", out);
      findDatasetScan("http://motherlode.ucar.edu:9080/thredds/idd/nam_model.xml", out); 

      //showAll =  true;
      //new MakeGribAggXML("http://motherlode.ucar.edu:9080/thredds/idd/model/NCEP/NAM/CONUS_80km/catalog.xml", System.out);
      //new MakeGribAggXML("http://motherlode.ucar.edu:9080/thredds/idd/model/RUC/CONUS_20km/surface/catalog.xml", System.out);
    }


}


  /* public void writeXML(OutputStream os) throws IOException {
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    fmt.output( makeDocument(), os);
  }

  /** Create an XML document from this info
  public Document makeDocument() {
    Element rootElem = new Element("GribAgg");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("location", ds.getLocation());
    if (coordSysBuilderName != null)
      rootElem.addContent( new Element("convention").setAttribute("name", coordSysBuilderName));

    int nDataVariables = 0;
    int nOtherVariables = 0;

    List axes = ds.getCoordinateAxes();
    int nCoordAxes = axes.size();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis =  (CoordinateAxis) axes.get(i);
      Element axisElem = new Element("axis");
      rootElem.addContent(axisElem);

      axisElem.setAttribute("name", axis.getName());
      axisElem.setAttribute("decl", getDecl(axis));
      if (axis.getAxisType() != null)
        axisElem.setAttribute("type", axis.getAxisType().toString());
      if (axis.getUnitsString() != null) {
        axisElem.setAttribute("units", axis.getUnitsString());
        axisElem.setAttribute("udunits", isUdunits(axis.getUnitsString()));
      }
      if (axis instanceof CoordinateAxis1D) {
        CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;
        if (axis1D.isRegular())
          axisElem.setAttribute("regular",  ucar.unidata.util.Format.d( axis1D.getIncrement(), 5));
      }
    }

    return doc;
  }    */