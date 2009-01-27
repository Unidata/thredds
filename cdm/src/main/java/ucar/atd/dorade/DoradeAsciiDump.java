/*
 * $Id:DoradeAsciiDump.java 51 2006-07-12 17:13:13Z caron $
 *
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

package ucar.atd.dorade;



import java.io.File;
import java.io.FileOutputStream;



/**
 * A dunper for DORADE radar data
 * @author IDV Development Team @ ATD
 * @version $Revision:51 $
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
