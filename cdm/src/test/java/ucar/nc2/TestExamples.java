// $Id: $
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
