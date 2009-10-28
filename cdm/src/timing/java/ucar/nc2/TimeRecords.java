// $Id: TimeRecords.java 51 2006-07-12 17:13:13Z caron $
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
package ucar.nc2;

import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.dt.*;

import java.io.IOException;
import java.util.List;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.StructurePseudo;

/**
 * @author john
 */
public class TimeRecords {

  static void doOne(String filename, boolean isTrajectory) throws IOException {
    System.out.println("\nTime " + filename);
    NetcdfFile ncfile = NetcdfFile.open(filename);
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    readColumns(ncfile);
    readRows(ncfile);
    if (isTrajectory)
      readTrajectoryIterator(filename);
    else
      readStationIterator(filename);
  }

  static private void readColumns(NetcdfFile ncfile) throws IOException {
    long start = System.currentTimeMillis();
    List varList = ncfile.getVariables();
    for (int i = 0; i < varList.size(); i++) {
      Variable variable = (Variable) varList.get(i);
      Array data = variable.read();
    }
    double took = (System.currentTimeMillis() - start) * .001;
    System.out.println("   nvars = " + varList.size());
    System.out.println(" readCols took=" + took + " secs");
  }

  static private void readRows(NetcdfFile ncfile) throws IOException {
    boolean first = true;
    long start = System.currentTimeMillis();
    Structure record = (Structure) ncfile.findVariable("record");
    if (record == null) {
      Dimension d = ncfile.findDimension("Time");
      record = new StructurePseudo(ncfile, null, "precord", d);
      System.out.println("   use psuedo record");
    }
    StructureDataIterator iter = record.getStructureIterator();
    while (iter.hasNext()) {
      StructureData sd = iter.next();
      if (first) {
        System.out.println("   record size = " + sd.getStructureMembers().getStructureSize() + " " +
                "   nvars = " + sd.getStructureMembers().getMembers().size());
      }
      first = false;

    }
    double took = (System.currentTimeMillis() - start) * .001;
    System.out.println(" readRows took=" + took + " secs");
  }

  static private void readTrajectoryIterator(String netcdfFileURI) throws IOException {
    TrajectoryObsDataset tob = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, netcdfFileURI, null, new StringBuilder());
    List trajList = tob.getTrajectories();
    for (int i = 0; i < trajList.size(); i++) {

      double count = 0;
      TrajectoryObsDatatype todt = (TrajectoryObsDatatype) trajList.get(i);
      long start = System.currentTimeMillis();
      DataIterator iter = todt.getDataIterator(0);
      while (iter.hasNext()) {
        PointObsDatatype pobs = (PointObsDatatype) iter.nextData();
        count += pobs.getObservationTime();
      }
      double took = (System.currentTimeMillis() - start) * .001;
      System.out.println(" readIterator took=" + took + " secs " + count);
    }
  }

  static private void readStationIterator(String netcdfFileURI) throws IOException {
    PointObsDataset tob = (PointObsDataset) TypedDatasetFactory.open(FeatureType.POINT, netcdfFileURI, null, new StringBuilder());

    double count = 0;
    long start = System.currentTimeMillis();
    DataIterator iter = tob.getDataIterator(0);
    while (iter.hasNext()) {
      PointObsDatatype pobs = (PointObsDatatype) iter.nextData();
      StructureData sdata = pobs.getData();
      count += pobs.getObservationTime();
    }
    double took = (System.currentTimeMillis() - start) * .001;
    System.out.println(" readIterator took=" + took + " secs " + count);

  }

  static public void main(String[] args) throws IOException {
    doOne("C:/data/trajectory/135_ordrd.nc", true);
    doOne("C:/data/trajectory/135_raw.nc", true);
    //doOne(TestAll.testdataDir +"station/ldm/20050520_metar.nc", false);
  }

}