// $Id: $
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

import ucar.ma2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;


public class TestExamples {

  void Nexam1() {

    String filename = "C:/data/my/file.nc";
    NetcdfFile ncfile = null;
    try {
      ncfile = NetcdfFile.open(filename);
      process(ncfile);

    } catch (IOException ioe) {
      log("trying to open " + filename, ioe);

    } finally {
      if (null != ncfile) try {
        ncfile.close();
      } catch (IOException ioe) {
        log("trying to open " + filename, ioe);
      }
    }

    String varName = "first_angle";
    Variable v = ncfile.findVariable(varName);
    try {
      Array data = v.read("0:2:1, 0:19:1");
      NCdump.printArray(data, varName, System.out, null);
    } catch (IOException ioe) {
      log("trying to read " + varName, ioe);

    } catch (InvalidRangeException e) {
      log("Invalid Range for " + varName, e);
    }

    try {
      int[] origin = new int[]{0, 0};
      int[] shape = new int[]{2, 19};
      Array data = v.read(origin, shape);

      Index ima = data.getIndex();

      List ranges = new ArrayList();
      ranges.add(new Range(2, 2));
      ranges.add(new Range(0, 499, 2));
      ranges.add(new Range(0, 719, 2));
      Array data2 = v.read(ranges);

    } catch (Throwable e) {
      log("Invalid Range for " + varName, e);
    }

    try {
      int[] varShape = v.getShape();
      int[] origin = new int[3];
      int[] shape = new int[]{1, varShape[1], varShape[2]};
      for (int i = 0; i < 3; i++) {
        origin[0] = i;
        Array data2D = v.read(origin, shape);
      }

      List ranges = new ArrayList();
      ranges.add(null);
      ranges.add(new Range(0, varShape[1], 2));
      ranges.add(new Range(0, varShape[2], 2));
      for (int i = 0; i < 3; i++) {
        ranges.set(0, new Range(i, i));
        Array data2D = v.read(ranges);
      }


    } catch (Throwable e) {
      log("Invalid Range for " + varName, e);
    }

    try {
      int[] varShape = v.getShape();
      List ranges = new ArrayList();
      for (int i = 0; i < varShape.length; i++)
        Range.appendShape(ranges, varShape[i]);


      Array data = v.read();
      double sum = 0.0;
      IndexIterator ii = data.getIndexIterator();
      while (ii.hasNext())
        sum += ii.getDoubleNext();

      int[] dataShape = data.getShape();
      ranges = new ArrayList();
      for (int i = 0; i < dataShape.length; i++)
        ranges.add(new Range(0, dataShape[i], 5));

      sum = 0.0;
      ii = data.getRangeIterator(ranges);
      while (ii.hasNext())
        sum += ii.getDoubleNext();

    } catch (Throwable e) {
      log("Invalid Range for " + varName, e);
    }


  }

  void Ex3() {
    String filename = "C:/data/my/file.nc";
    NetcdfFile ncfile = null;
    try {
      ncfile = NetcdfDataset.openFile(filename, null);
      process(ncfile);
    } catch (IOException ioe) {
      log("trying to open " + filename, ioe);
    } finally {
      if (null != ncfile) try {
        ncfile.close();
      } catch (IOException ioe) {
        log("trying to close " + filename, ioe);
      }
    }
  }


  void log(String what, Throwable t) {
  }

  void process(NetcdfFile ncfile) {
  }

}
