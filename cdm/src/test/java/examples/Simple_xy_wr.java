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

import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.ma2.*;

import java.io.IOException;
import java.util.ArrayList;

/**
 * User: yuanho
 * Date: Oct 16, 2006
 * Time: 2:11:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class Simple_xy_wr {


     public static void main(String args[])
    {
        // We are writing 2D data, a 6 x 12 grid.
       final int NX = 6;
       final int NY = 12;


       // Create the file.
       String filename = "simple_xy.nc";
       NetcdfFileWriteable dataFile = null;

       try {
           dataFile = NetcdfFileWriteable.createNew(filename, false);

           // Create netCDF dimensions,
            Dimension xDim = dataFile.addDimension("x", NX );
            Dimension yDim = dataFile.addDimension("y", NY );

            ArrayList dims =  new ArrayList();

            // define dimensions
            dims.add( xDim);
            dims.add( yDim);


           // Define a netCDF variable. The type of the variable in this case
           // is ncInt (32-bit integer).
           dataFile.addVariable("data", DataType.INT, dims);
                 
           // create the file
           dataFile.create();

            // This is the data array we will write. It will just be filled
            // with a progression of numbers for this example.
           ArrayInt.D2 dataOut = new ArrayInt.D2( xDim.getLength(), yDim.getLength());

           // Create some pretend data. If this wasn't an example program, we
           // would have some real data to write, for example, model output.
           int i,j;

           for (i=0; i<xDim.getLength(); i++) {
                for (j=0; j<yDim.getLength(); j++) {
                    dataOut.set(i,j, i * NY + j);
                }
           }

           // Write the pretend data to the file. Although netCDF supports
           // reading and writing subsets of data, in this case we write all
           // the data in one operation.
          dataFile.write("data", dataOut);


       } catch (IOException e) {
              e.printStackTrace();
       } catch (InvalidRangeException e) {
              e.printStackTrace();
       } finally {
            if (null != dataFile)
            try {
                dataFile.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
       }

        System.out.println( "*** SUCCESS writing example file simple_xy.nc!");
    }

}
