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

import java.io.IOException;

/**
 * @author john
 */
public class TimeRecords2 {

  static void doOne(String filename, String varName) throws IOException {
    System.out.println("\nTime " + filename);
    NetcdfFile ncfile = NetcdfFile.open(filename);
    readOneVariable( ncfile, varName);
    readColumns(ncfile);
    ncfile.close();
  }

  static private void readColumns(NetcdfFile ncfile) throws IOException {
    long start = System.currentTimeMillis();
    long total = 0;
    for (Variable variable : ncfile.getVariables()) {
      Array data = variable.read();
      total += data.getSize();
    }
    double took = (System.currentTimeMillis() - start) * .001;
    System.out.println("   nvars = " + ncfile.getVariables().size());
    System.out.println(" readCols took=" + took + " secs ("+total+")");
  }

  static private void readOneVariable(NetcdfFile ncfile, String varName) throws IOException {
    long start = System.currentTimeMillis();
    long total = 0;
    Variable variable = ncfile.findVariable(varName);
      Array data = variable.read();
      total += data.getSize();
    double took = (System.currentTimeMillis() - start) * .001;
    System.out.println("   read var = " + varName+" from "+ncfile.getLocation());
    System.out.println(" readOneVariable took=" + took + " secs ("+total+")");
  }

  static public void main(String[] args) throws IOException {
    doOne("C:/data/metars/Surface_METAR_20070326_0000.col.nc", "parent_index");
    doOne("C:/data/metars/Surface_METAR_20070326_0000.nc", "parent_index");
  }

}