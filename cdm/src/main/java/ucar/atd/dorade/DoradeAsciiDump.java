/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.atd.dorade;

import ucar.nc2.constants.CDM;

import java.io.*;

/**
 * A dumper for DORADE radar data
 */
public class DoradeAsciiDump {

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Error: usage: java ucar.unidata.data.radar.DoradeDump <parameter (e.g., VR,ZDR,DBZ)> <spol filename>");
      System.exit(1);
    }
    String paramName = args[0];
    for (int i = 2; i < args.length; i++) {
      String filename = args[i];
      File sourceFile = new File(filename);
      File destFile = new File(sourceFile.getName() + ".txt");
      DoradeSweep sweep = new DoradeSweep(filename);
      if (sweep.getScanMode() != ScanMode.MODE_SUR) {
        System.err.println("Skipping:" + sourceFile);
        continue;
      }
      int nRays = sweep.getNRays();
      int nCells = sweep.getNCells(0);
      DoradePARM param = sweep.lookupParamIgnoreCase(paramName);
      if (param == null) {
        System.err.println("Error: Could not find given paramter:" + paramName);
        System.exit(1);
      }
      float[] azimuths = sweep.getAzimuths();
      float[] elevations = sweep.getElevations();

      StringBuilder sb = new StringBuilder();
      System.err.println("File:" + sourceFile + " #rays:" + nRays + " #cells:" + nCells);
      for (int rayIdx = 0; rayIdx < nRays; rayIdx++) {
        sb.append("ray:").append(rayIdx).append(" ").append(elevations[rayIdx]).append(" ").append(azimuths[rayIdx]).append("\n");
        float[] rayValues = sweep.getRayData(param, rayIdx);
        for (int cellIdx = 0; cellIdx < rayValues.length; cellIdx++) {
          if (cellIdx > 0)
            sb.append(",");
          sb.append(rayValues[cellIdx]);
        }
        sb.append("\n");
      }

      try (FileOutputStream out = new FileOutputStream(destFile)) {
        PrintWriter ps = new PrintWriter( new OutputStreamWriter(out, CDM.utf8Charset));
        ps.append(sb.toString());
        out.flush();
      }
    }

  }


}
