/*
 * $Id$
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.atd.dorade;



import java.io.File;
import java.io.FileOutputStream;



/**
 * A dunper for DORADE radar data
 * @author IDV Development Team @ ATD
 * @version $Revision$
 */
public class DoradeAsciiDump {

    public static void main(String[] args) throws Exception {
	if(args.length<2) {
	    System.err.println("Error: usage: java ucar.unidata.data.radar.DoradeDump <parameter (e.g., VR,ZDR,DBZ)> <spol filename>");
	    System.exit(1);
	}
	String paramName = args[0];
	for(int i=2;i<args.length;i++) {
	    String filename = args[i];
	    File sourceFile = new File(filename);
	    File destFile = new File(sourceFile.getName()+".txt");
	    DoradeSweep sweep = new DoradeSweep(filename);
	    if(sweep.getScanMode()  != ScanMode.MODE_SUR) {
                System.err.println("Skipping:" + sourceFile);
                continue;
	    }
	    int nRays   = sweep.getNRays();
	    int nCells  = sweep.getNCells(0);
	    DoradePARM param = sweep.lookupParamIgnoreCase(paramName);
	    if(param == null ) {
		System.err.println ("Error: Could not find given paramter:" + paramName);
		System.exit(1);
	    }
	    float[]azimuths = sweep.getAzimuths();
	    float[]elevations = sweep.getElevations();
	    StringBuffer sb   =new StringBuffer();

            System.err.println("File:" + sourceFile + " #rays:" + nRays + " #cells:" + nCells);
	    for(int rayIdx=0;rayIdx<nRays;rayIdx++) {
		sb.append("ray:" + rayIdx + " " + elevations[rayIdx] + " " + azimuths[rayIdx]+"\n");
		float[] rayValues = sweep.getRayData(param, rayIdx);
		for(int cellIdx=0;cellIdx<rayValues.length;cellIdx++) {
		    if(cellIdx>0)
			sb.append(",");
		    sb.append(""+rayValues[cellIdx]);
		}
		sb.append("\n");
	    }

	    FileOutputStream out = new FileOutputStream(destFile);
	    out.write(sb.toString().getBytes());	    
	    out.flush();
	    out.close();
	}

    }


}
