// $Id: FmrcDefinition.java,v 1.5 2006/06/06 16:07:13 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.dt.grid;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.*;
import java.util.*;

import ucar.nc2.units.DateUnit;

/**
 * Defines the expected inventory of a Forecast Model Run Collection.
 *
 * <pre>
 * Data Structures
 *
 *  List VertTimeCoord
 *    double[] values
 *
 *  List TimeCoord
 *    double[] offsetHour
 *
 *  List RunSeq
 *    List Run - runHour dependent TimeCoord
 *      double hour
 *      TimeCoord
 *
 *    List Grid
 *     String name
 *     VertTimeCoord - time dependent vertical coordinate
 *       VertCoord
 *       TimeCoord (optional)
 *
 *
 * Abstractly, the data is a table:
 *   Run  Grid  TimeCoord  VertCoord
 *   Run  Grid  TimeCoord  VertCoord
 *   Run  Grid  TimeCoord  VertCoord
 *   ...
 *
 * We will use the notation ({} means list)
 *  {Run, Grid, TimeCoord, VertCoord}
 *
 * The simplest case would be if all runs have the same grids, which all use the same time coord, and each grid always
 *  uses the same vert coord :
 * (1) {runTime} X {Grid, VertCoord} X TimeCoord      (X means product)
 *
 * The usual case is that there are multiple TimeCoords, but a grid always uses the same one:
 * (2) {runTime} X {Grid, VertCoord, TimeCoord}
 *
 * Since all runTimes are the same, the definition only need be:
 * (2d) {Grid, VertCoord, TimeCoord}

 * Another case is that different run hours use different TimeCoords. We will call this a RunSeq, and we associate with each
 * RunSeq the list of grids that use it:
 *   Run = runHour, TimeCoord
 *   RunSeq = {runHour, TimeCoord} X {Grid, VertCoord}
 *
 * Different grids use different RunSeqs, so we have a list of RunSeq:
 * (3d) {{runHour, TimeCoord} X {Grid, VertCoord}}
 *
 * We can recast (2d), when all runHours are the same,  as:
 * (2d') {TimeCoord X {Grid, VertCoord}}
 * which means that we are grouping grids by unique TimeCoord. (1d) would be the case where there is only one in the list.
 *
 * Another case is when the VertCoord depends on the TimeCoord, but all run hours are the same:
 * (4d) {TimeCoord X {Grid, VertCoord(TimeCoord)}}
 *
 * Which lead us to generalize a VertCoord to a time-dependent one, called VertTimeCoord.
 *
 * The most general case is then
 *   {{runHour, TimeCoord} X {Grid, VertTimeCoord}}
 *
 * </pre>
 */
public class FmrcDefinition {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FmrcDefinition.class);

  //  utilities - move to fmrc ?
  private Calendar cal = new GregorianCalendar(); // for date computations

  double getHour( Date d) {
    cal.setTime( d);
    int hour = cal.get(Calendar.HOUR_OF_DAY);
    double min = (double) cal.get(Calendar.MINUTE);
    return hour + min/60;
  }

  ///////////////////////////////////////////////////////////////////

  private ArrayList vertTimeCoords; // VertTimeCoord
  private ArrayList timeCoords; // ForecastModelRun.TimeCoord
  private ArrayList runSequences; // RunSeq

  private String name;
  private String suffixFilter;

  public FmrcDefinition() {
    cal.setTimeZone( TimeZone.getTimeZone("UTC"));
  }

  public String getSuffixFilter() { return suffixFilter; }

  /* public void report( ForecastModelRunCollection fmrc, PrintStream out, boolean showMissing) {
    List runSequences = fmrc.getRunSequences();
    for (int i = 0; i < runSequences.size(); i++) {
      ForecastModelRunCollection.RunSeq have = (ForecastModelRunCollection.RunSeq) runSequences.get(i);

      List vars = have.getVariables();
      for (int j = 0; j < vars.size(); j++) {
        ForecastModelRunCollection.UberGrid uv = (ForecastModelRunCollection.UberGrid) vars.get(j);
        RunSeq want = findSeqForVariable( uv.getName());
        if (want == null)
          out.println("Cant find variable "+uv.getName());
        else {
          StringBuffer sbuff = new StringBuffer();
          if (want.compare( have, sbuff, showMissing)) {
            out.println("Checking variable= "+uv.getName());
            out.println(sbuff);
          }
        }
      }
    }
  }

  boolean compareOffsets(double[] haveOffset, double[] wantOffset, StringBuffer sbuff, boolean showMissing) {
    int countHave = 0;
    int countWant = 0;
    boolean errs = false;

    while ((countHave < haveOffset.length) && (countWant < wantOffset.length)) {
      if (wantOffset[countWant] == haveOffset[countHave]) {
        countHave++;
        countWant++;
      } else if (wantOffset[countWant] < haveOffset[countHave]) {
        if (showMissing) {
          sbuff.append("  missing " + wantOffset[countWant] + "\n");
          errs = true;
        }
        countWant++;
      } else if (wantOffset[countWant] > haveOffset[countHave]) {
        sbuff.append("  extra " + haveOffset[countWant] + "\n");
        errs = true;
        countHave++;
      }
    }

    while (countHave < haveOffset.length) {
      sbuff.append("  extra " + haveOffset[countWant] + "\n");
      errs = true;
      countHave++;
    }

    while (countWant < wantOffset.length) {
      if (showMissing) {
        sbuff.append("  missing " + wantOffset[countWant] + "\n");
        errs = true;
      }
      countWant++;
    }

    return errs;
  }  */

  private ForecastModelRun.TimeCoord findTimeCoord( String id) {
    for (int i = 0; i < timeCoords.size(); i++) {
      ForecastModelRun.TimeCoord tc = (ForecastModelRun.TimeCoord) timeCoords.get(i);
      if (tc.getId().equals(id))
        return tc;
    }
    return null;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  // encapsolates a Grid and its vertical coordinate, which may be time-dependent
  public static class Grid implements Comparable {
    private String name;
    private VertTimeCoord vtc = null;

    Grid (String name) {
      this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public VertTimeCoord getVertTimeCoord() { return vtc; }

    public int countVertCoords( double offsetHour) {
      return (vtc == null) ? 1 : vtc.countVertCoords( offsetHour);
    }

    /**
     * Get inventory as an array of vert coords, at a particular time coord = hourOffset
     *
     * @param hourOffset : may or may not be in the list of time coords
     * @return array of vert coords. NaN = missing; -0.0 = surface.
     */
    public double[] getVertCoords(double hourOffset) {

      if (vtc == null) {
        double[] result = new double[1]; // if 2D return -0.0
        result[0] = -0.0;
        return result;
      }

      return vtc.getVertCoords(hourOffset);
    }

    public int compareTo(Object o) {
      Grid other = (Grid) o;
      return name.compareTo(other.name);
    }
  }

  class Run {
    double runHour; // hour since 00:00
    ForecastModelRun.TimeCoord tc;

    Run(ForecastModelRun.TimeCoord tc, double runHour) {
      this.tc = tc;
      this.runHour = runHour;
    }
  }

  // A sequence of Runs.
  class RunSeq {
    boolean isAll = false;    // true if they all use the same OffsetHours
    ForecastModelRun.TimeCoord allUseOffset; // they all use this one
    ArrayList runs = new ArrayList(); // list of Run
    ArrayList vars = new ArrayList(); // list of ForecastModelRun.Grid

    RunSeq(String id) {
      this.isAll = true;
      this.allUseOffset = findTimeCoord( id);
    }

    RunSeq(ArrayList runs) {
      this.runs = runs;

      // complete a 24 hour cycle
      int matchIndex = 0;
      Run last = (Run) runs.get(runs.size()-1);
      double runHour = last.runHour;
      while (runHour < 24.0) {
        Run match = (Run) runs.get(matchIndex);
        Run next = (Run) runs.get(matchIndex+1);
        double incr = next.runHour - match.runHour;
        if (incr < 0)
          break;
        runHour += incr;
        runs.add( new Run(next.tc, runHour));
        matchIndex++;
      }
    }

    /**
     * Find the TimeCoord the should be used for this runTime
     * @param runTime  run date
     * @return TimeCoord, or null if no match.
     */
    ForecastModelRun.TimeCoord findTimeCoordByRuntime( Date runTime) {
      if (isAll)
        return allUseOffset;
      double hour = getHour( runTime);
      Run run = findRun( hour);
      if (run == null) return null;
      return run.tc;
    }

    /** find the Run that matches the run hour */
    Run findRun( double hour) {
      for (int i = 0; i < runs.size(); i++) {
        Run run = (Run) runs.get(i);
        if (run.runHour == hour) return run;
      }
      return null;
    }

    Grid findGrid( String name) {
      for (int j = 0; j < vars.size(); j++) {
        Grid grid = (Grid) vars.get(j);
        if (grid.name.equals(name))
          return grid;
      }
      return null;
    }

   /* boolean compare(ForecastModelRunCollection.RunSeq have, StringBuffer sbuff, boolean showMissing) {
      boolean hasErrs = false;

      for (int j = 0; j < have.runs.size(); j++) {
        ForecastModelRunCollection.Run run = (ForecastModelRunCollection.Run) have.runs.get(j);

        ForecastModelRun.TimeCoord want;
        if (isAll)
          want = allUseOffset;
        else {
          double hour = getHour( run.runTime);
          Run wantRun = findRun( hour);
          want = wantRun.tc;
        }

        StringBuffer sbuff2 = new StringBuffer();
        if (compareOffsets( run.getOffsetHours(), want.getOffsetHours(), sbuff2, showMissing)) {
          sbuff.append(" Run "+ DateUnit.getStandardDateString( run.runTime)+"\n");
          sbuff.append(sbuff2);
          hasErrs = true;
        }
      }

      return hasErrs;
    } */
  }

  RunSeq findSeqForVariable( String name) {
    for (int i = 0; i < runSequences.size(); i++) {
      RunSeq runSeq = (RunSeq) runSequences.get(i);
      if (runSeq.findGrid(name) != null)
        return runSeq;
    }
    return null;
  }

  Grid findGrid( String name) {
    for (int i = 0; i < runSequences.size(); i++) {
      RunSeq runSeq = (RunSeq) runSequences.get(i);
      Grid grid = runSeq.findGrid( name);
      if (null != grid)
        return grid;
    }
    return null;
  }

  VertTimeCoord findVertCoord( String id) {
    if (id == null)
      return null;

    for (int i = 0; i < vertTimeCoords.size(); i++) {
      VertTimeCoord vc = (VertTimeCoord) vertTimeCoords.get(i);
      if (vc.getId().equals(id))
        return vc;
    }
    return null;
  }


  ///////////////////////////////////////////////////////////////////////////////////////////////

  /* vertical coordinates that depend on the offset hour */
  class VertTimeCoord {
    ForecastModelRun.VertCoord vc;
    ForecastModelRun.TimeCoord tc; // optional
    int ntimes, nverts;
    double[][] vcForTimeIndex;  // vcForTimeIndex[ntimes]
    ArrayList restrictList;

    VertTimeCoord(ForecastModelRun.VertCoord vc) {
      this.vc = vc;
      ntimes = (tc == null) ? 1 : tc.getOffsetHours().length;
      nverts = vc.getValues().length;
    }

    VertTimeCoord(ForecastModelRun.VertCoord vc, RunSeq runSeq) {
      if (runSeq.isAll) {
        this.tc = runSeq.allUseOffset;
      } else {
        // make union timeCoord
        HashSet valueSet = new HashSet();
        for (int i = 0; i < runSeq.runs.size(); i++) {
          Run run = (Run) runSeq.runs.get(i);
          addValues( valueSet, run.tc.getOffsetHours());
        }
        List valueList = Arrays.asList( valueSet.toArray());
        Collections.sort( valueList);
        double[] values = new double[valueList.size()];
        for (int i = 0; i < valueList.size(); i++) {
          Double d = (Double) valueList.get(i);
          values[i] = d.doubleValue();
        }
        this.tc = new ForecastModelRun.TimeCoord();
        this.tc.setOffsetHours( values);
        this.tc.setId( "union");
      }

      this.vc = vc;

      ntimes = (tc == null) ? 1 : tc.getOffsetHours().length;
      nverts = vc.getValues().length;
    }

    private void addValues(HashSet valueSet, double[] values) {
      for (int i = 0; i < values.length; i++)
        valueSet.add( new Double(values[i]));
    }

    String getId() { return vc.getId(); }
    String getName() { return vc.getName(); }
    //double[] getValues() { return vc.getValues(); }

    void addRestriction(String vertCoordsString, String timeCoords) {
      StringTokenizer stoker = new StringTokenizer( vertCoordsString, " ,");
      int n = stoker.countTokens();
      double[] vertCoords = new double[n];
      int count = 0;
      while (stoker.hasMoreTokens()) {
        vertCoords[count++] = Double.parseDouble(stoker.nextToken());
      }

      if (vcForTimeIndex == null) {
        restrictList = new ArrayList();
        vcForTimeIndex = new double[ntimes][];
        for (int i = 0; i < vcForTimeIndex.length; i++) {
          vcForTimeIndex[i] = vc.getValues();
        }
      }

      // save these in case we have to write them back out
      restrictList.add( vertCoordsString);
      restrictList.add( timeCoords);

      stoker = new StringTokenizer(timeCoords, " ,");
      while (stoker.hasMoreTokens()) {
        double hour = Double.parseDouble(stoker.nextToken());
        int index = tc.findIndex(hour);
        if (index < 0)
          log.error("hour Offset"+hour+" not found in TimeCoord "+tc.getId());
        vcForTimeIndex[index] = vertCoords;
      }
    }

    double[] getVertCoords( double offsetHour) {
      if ((tc == null) || (null == vcForTimeIndex))
        return vc.getValues();

      int index = tc.findIndex(offsetHour);
      if (index < 0)
        return new double[0];

      return vcForTimeIndex[index];
    }

    int countVertCoords( double offsetHour) {
      if ((tc == null) || (null == vcForTimeIndex))
        return vc.getValues().length;

      int index = tc.findIndex(offsetHour);
      if (index < 0)
        return 0;

      return vcForTimeIndex[index].length;
    }

  }


  //////////////////////////////////////////////////////////////////////////////////

  public String writeDefinitionXML() {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    return fmt.outputString(makeDefinitionXML());
  }

  public void writeDefinitionXML(OutputStream os) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(makeDefinitionXML(), os);
  }

  /**
   * Create an XML document for the entire collection
   */
  public Document makeDefinitionXML() {
    Element rootElem = new Element("fmrcDefinition");
    Document doc = new Document(rootElem);
    if (name != null)
      rootElem.setAttribute("dataset", name);
    if (null != suffixFilter)
      rootElem.setAttribute("suffixFilter", suffixFilter);

    // list all the vertical coordinaates
    for (int i = 0; i < vertTimeCoords.size(); i++) {
      VertTimeCoord vtc = (VertTimeCoord) vertTimeCoords.get(i);
      ForecastModelRun.VertCoord vc = vtc.vc;

      Element vcElem = new Element("vertCoord");
      rootElem.addContent(vcElem);
      vcElem.setAttribute("id", vc.getId());
      vcElem.setAttribute("name", vc.getName());
      if (null != vc.getUnits())
        vcElem.setAttribute("units", vc.getUnits());

      StringBuffer sbuff = new StringBuffer();
      double[] values = vc.getValues();
      for (int j = 0; j < values.length; j++) {
        if (j > 0) sbuff.append(" ");
        sbuff.append( Double.toString(values[j]));
      }
      vcElem.addContent(sbuff.toString());
    }

    // list all the offset hours
    for (int i = 0; i < timeCoords.size(); i++) {
      ForecastModelRun.TimeCoord tc = (ForecastModelRun.TimeCoord) timeCoords.get(i);

      Element offsetElem = new Element("offsetHours");
      rootElem.addContent(offsetElem);
      offsetElem.setAttribute("id", tc.getId());

      StringBuffer sbuff = new StringBuffer();
      double[] offset = tc.getOffsetHours();
      for (int j = 0; j < offset.length; j++) {
        if (j > 0) sbuff.append(" ");
        sbuff.append( Double.toString(offset[j]));
      }
      offsetElem.addContent(sbuff.toString());
    }

    // list all the time sequences, containing variables
    for (int i = 0; i < runSequences.size(); i++) {
      RunSeq runSeq = (RunSeq) runSequences.get(i);

      Element seqElem = new Element("runSequence");
      rootElem.addContent(seqElem);

      if (runSeq.isAll) {
        seqElem.setAttribute("allUseSeq", runSeq.allUseOffset.getId());

      }  else { // otherwise show each run
        for (int j = 0; j < runSeq.runs.size(); j++) {
          Element runElem = new Element("run");
          seqElem.addContent(runElem);

          Run run = (Run) runSeq.runs.get(j);
          runElem.setAttribute("runHour", Double.toString(run.runHour));
          runElem.setAttribute("offsetHourSeq", run.tc.getId());
        }
      }

      for (int j = 0; j < runSeq.vars.size(); j++) {
        Grid grid = (Grid) runSeq.vars.get(j);
        Element varElem = new Element("variable");
        seqElem.addContent(varElem);
        varElem.setAttribute("name", grid.name);
        if (grid.vtc != null) {
          varElem.setAttribute("vertCoord", grid.vtc.getId());

          // look for time-dependent vert coordinates - always specific to one variable
          if (grid.vtc.restrictList != null) {
            java.util.Iterator iter = grid.vtc.restrictList.iterator();
            while( iter.hasNext()) {
              Element vtElem = new Element("vertCoord");
              varElem.addContent(vtElem);
              vtElem.setAttribute("restrict", (String) iter.next());
              vtElem.setText((String) iter.next());
            }
          }
        }
      }
    }
    return doc;
  }

  public boolean readDefinitionXML(String xmlLocation) throws IOException {
    File xml = new File(xmlLocation);
    if (!xml.exists()) return false;

    InputStream is = new BufferedInputStream( new FileInputStream( xmlLocation));
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(is);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

    Element rootElem = doc.getRootElement();
    name = rootElem.getAttributeValue("name");
    suffixFilter = rootElem.getAttributeValue("suffixFilter");

    vertTimeCoords = new ArrayList();
    java.util.List vList = rootElem.getChildren("vertCoord");
    for (int i=0; i< vList.size(); i++) {
      Element vcElem = (Element) vList.get(i);
      ForecastModelRun.VertCoord vc = new ForecastModelRun.VertCoord();
      vc.setId( vcElem.getAttributeValue("id"));
      vc.setName( vcElem.getAttributeValue("name"));
      vc.setUnits( vcElem.getAttributeValue("units"));

      // parse the values
      String values = vcElem.getText();
      StringTokenizer stoke = new StringTokenizer(values);
      int n = stoke.countTokens();
      double[] vals = new double[n];
      int count = 0;
      while (stoke.hasMoreTokens()) {
        vals[count++] = Double.parseDouble( stoke.nextToken());
      }
      vc.setValues( vals);

      // wrap it as a VertTimeCoord
      VertTimeCoord vtc = new VertTimeCoord(vc);
      vertTimeCoords.add( vtc);
    }

    timeCoords = new ArrayList();
    java.util.List tList = rootElem.getChildren("offsetHours");
    for (int i=0; i< tList.size(); i++) {
      Element timeElem = (Element) tList.get(i);
      ForecastModelRun.TimeCoord tc = new ForecastModelRun.TimeCoord();
      timeCoords.add( tc);
      tc.setId( timeElem.getAttributeValue("id"));

      // parse the values
      String values = timeElem.getText();
      StringTokenizer stoke = new StringTokenizer(values);
      int n = stoke.countTokens();
      double[] offset = new double[n];
      int count = 0;
      while (stoke.hasMoreTokens()) {
        offset[count++] = Double.parseDouble( stoke.nextToken());
      }
      tc.setOffsetHours( offset);
    }

    runSequences = new ArrayList();
    java.util.List runseqList = rootElem.getChildren("runSequence");
    for (int i=0; i< runseqList.size(); i++) {
      Element runseqElem = (Element) runseqList.get(i);
      RunSeq rseq;

      String allUseId = runseqElem.getAttributeValue("allUseSeq");
      if (allUseId != null) {
        rseq = new RunSeq( allUseId);
      } else {
        ArrayList runs = new ArrayList();
        java.util.List runList = runseqElem.getChildren("run");
        for (int j=0; j< runList.size(); j++) {
          Element runElem = (Element) runList.get(j);
          String id = runElem.getAttributeValue("offsetHourSeq");
          ForecastModelRun.TimeCoord tc = findTimeCoord( id);
          String hour = runElem.getAttributeValue("runHour");

          Run run = new Run(tc, Double.parseDouble( hour));
          runs.add( run);
        }
        rseq = new RunSeq( runs);
      }

      runSequences.add( rseq);

      java.util.List varList = runseqElem.getChildren("variable");
      for (int j=0; j< varList.size(); j++) {
        Element varElem = (Element) varList.get(j);
        String name = varElem.getAttributeValue("name");
        Grid grid = new Grid(name);
        rseq.vars.add( grid);
        grid.vtc = findVertCoord( varElem.getAttributeValue("vertCoord"));

        // look for time dependent vert coordinates - always specific to one variable
        java.util.List rList = varElem.getChildren("vertTimeCoord");
        // gotta be inside a useAllSeq runSeq
        if (rList.size() > 0) {
          grid.vtc = new VertTimeCoord( grid.vtc.vc, rseq);

          for (int k = 0; k < rList.size(); k++) {
            Element vtElem = (Element) rList.get(k);
            String vertCoords = vtElem.getAttributeValue("restrict");
            String timeCoords = vtElem.getText();
            grid.vtc.addRestriction(vertCoords, timeCoords);
          }
        }
      }

    }

    return true;
  }

  //////////////////////////////////////////////////////////////////////

  public void makeFromCollectionInventory(ForecastModelRunCollection fmrc) {
    this.name = fmrc.getName();

    this.timeCoords = fmrc.getTimeCoords();

    this.vertTimeCoords = new ArrayList();
    for (int i = 0; i < fmrc.getVertCoords().size(); i++) {
      ForecastModelRun.VertCoord vc = (ForecastModelRun.VertCoord) fmrc.getVertCoords().get(i);
      vertTimeCoords.add( new VertTimeCoord(vc));
    }

    this.runSequences = new ArrayList();

    // Convert the run sequences, containing variables
    List seqs = fmrc.getRunSequences();
    for (int i = 0; i < seqs.size(); i++) {
      ForecastModelRunCollection.RunSeq invSeq = (ForecastModelRunCollection.RunSeq) seqs.get(i);

      // check to see if all runs use the same tc
      boolean isAll = true;
      ForecastModelRun.TimeCoord oneTc = null;
      for (int j = 0; j < invSeq.runs.size(); j++) {
        ForecastModelRunCollection.Run run = (ForecastModelRunCollection.Run) invSeq.runs.get(j);
        if (j == 0)
          oneTc = run.tc;
        else {
          if (oneTc != run.tc)
            isAll = false;
        }
      }

      RunSeq runSeq;
      if (isAll) {
        runSeq = new RunSeq( oneTc.getId());
      }  else {
        ArrayList runs = new ArrayList();
        for (int j = 0; j < invSeq.runs.size(); j++) {
          ForecastModelRunCollection.Run invRun = (ForecastModelRunCollection.Run) invSeq.runs.get(j);

          Run run = new Run(  invRun.tc, getHour( invRun.runTime) );
          runs.add( run);
        }
        runSeq = new RunSeq( runs);
      }
      runSequences.add( runSeq);

      // convert UberGrids to Grid
      List vars = invSeq.getVariables();
      for (int j = 0; j < vars.size(); j++) {
        ForecastModelRunCollection.UberGrid uv = (ForecastModelRunCollection.UberGrid) vars.get(j);
        Grid grid = new Grid( uv.getName());
        runSeq.vars.add( grid);
        if (uv.vertCoordUnion != null)
          grid.vtc = new VertTimeCoord( uv.vertCoordUnion);
      }
    }
  }

  /** Add just the vertical coord info to the definition */
  public void addVertCoordsFromCollectionInventory(ForecastModelRunCollection fmrc) {
    this.vertTimeCoords = new ArrayList();
    for (int i = 0; i < fmrc.getVertCoords().size(); i++) {
      ForecastModelRun.VertCoord vc = (ForecastModelRun.VertCoord) fmrc.getVertCoords().get(i);
      vertTimeCoords.add( new VertTimeCoord(vc));
    }

    // Convert the run sequences, containing variables
    List seqs = fmrc.getRunSequences();
    for (int i = 0; i < seqs.size(); i++) {
      ForecastModelRunCollection.RunSeq invSeq = (ForecastModelRunCollection.RunSeq) seqs.get(i);

      // convert UberGrids to Grid
      List vars = invSeq.getVariables();
      for (int j = 0; j < vars.size(); j++) {
        ForecastModelRunCollection.UberGrid uv = (ForecastModelRunCollection.UberGrid) vars.get(j);
        if (uv.vertCoordUnion != null) {
          Grid grid = findGrid( uv.getName());
          grid.vtc = new VertTimeCoord( uv.vertCoordUnion);
        }
      }
    }
  }


}
