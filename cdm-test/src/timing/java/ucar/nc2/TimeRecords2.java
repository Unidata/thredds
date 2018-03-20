/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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