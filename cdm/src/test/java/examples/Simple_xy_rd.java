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
package examples;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.ma2.*;

import java.io.IOException;

/**
 * User: yuanho
 * Date: Oct 16, 2006
 * Time: 10:16:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class Simple_xy_rd {

    public static void main(String args[]) throws Exception, java.lang.NullPointerException
    {

       final int NX = 6;
       final int NY = 12;
       // This is the array we will read.
       int[][] dataIn = new int[NX][NY];

       // Open the file. The ReadOnly parameter tells netCDF we want
       // read-only access to the file.
       NetcdfFile dataFile = null;
       String filename = "simple_xy.nc";
       // Open the file.
       try {

           dataFile = NetcdfFile.open(filename, null);

           // Retrieve the variable named "data"
            Variable dataVar = dataFile.findVariable("data");

            if (dataVar == null) {
                System.out.println("Cant find Variable data");
                return;
            }

           // Read all the values from the "data" variable into memory.
            int [] shape = dataVar.getShape();
            int[] origin = new int[2];

            ArrayInt.D2 dataArray;
             
            dataArray = (ArrayInt.D2) dataVar.read(origin, shape);

           // Check the values.
            assert shape[0] == NX;
            assert shape[1] == NY;

            for (int j=0; j<shape[0]; j++) {
               for (int i=0; i<shape[1]; i++) {
                  dataIn[j][i] = dataArray.get(j,i);
               }
            }


       // The file is closed no matter what by putting inside a try/catch block.
       } catch (java.io.IOException e) {
                e.printStackTrace();
                return;
       }  catch (InvalidRangeException e) {
                e.printStackTrace();
       } finally {
           if (dataFile != null)
           try {
             dataFile.close();
           } catch (IOException ioe) {
             ioe.printStackTrace();
           }
        }

    System.out.println( "*** SUCCESS reading example file simple_xy.nc!");

    }

}
