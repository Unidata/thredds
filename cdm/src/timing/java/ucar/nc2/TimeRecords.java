// $Id: TimeRecords.java 51 2006-07-12 17:13:13Z caron $
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
package ucar.nc2;

import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.dt.*;

import java.io.IOException;
import java.util.List;

import ucar.nc2.constants.FeatureType;

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
    TrajectoryObsDataset tob = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, netcdfFileURI, null, new StringBuffer());
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
    PointObsDataset tob = (PointObsDataset) TypedDatasetFactory.open(FeatureType.POINT, netcdfFileURI, null, new StringBuffer());

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
    doOne(TestAll.upcShareTestDataDir +"station/ldm/20050520_metar.nc", false);
  }

}