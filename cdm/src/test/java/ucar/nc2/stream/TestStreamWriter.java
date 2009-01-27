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
package ucar.nc2.stream;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;

import java.io.*;

/**
 * @author caron
 * @since Jul 19, 2007
 */
public class TestStreamWriter {

  static public void main(String args[]) throws IOException, InvalidRangeException {

    long start = System.currentTimeMillis();
    //String filenameIn = "C:/data/metars/Surface_METAR_20070329_0000.nc";
    String filenameIn = "C:/dev/thredds/cdm/src/test/data/testWriteRecord.nc";
    File f = new File(filenameIn);
    long size = f.length();
    //String filenameIn = "C:/data/test2.nc";
    String filenameStream = "C:/temp/stream.ncs";
    String filenameOut = "C:/temp/copy.nc";
    NetcdfFile ncfile = NetcdfFile.open(filenameIn);

    DataOutputStream streamFile = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filenameStream), 10 * 1000));
    StreamWriter writer = new StreamWriter(ncfile, streamFile, true);
    ncfile.close();
    streamFile.close();

    long took = System.currentTimeMillis() - start;
    double rate = 0.001 * size / took;
    System.out.println(" write to stream took = " + took + " msec = " + rate + " Mb/sec ");
    start = System.currentTimeMillis();

    DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(filenameStream), 10 * 1000));
    NetcdfFileWriteable ncfilew = NetcdfFileWriteable.createNew(filenameOut, false);
    Stream2Netcdf ncWriter = new Stream2Netcdf(ncfilew, in);
    in.close();
    ncfilew.close();

    took = System.currentTimeMillis() - start;
    rate = 0.001 * size / took;
    System.out.println(" write stream to netcdf took = " + took + " msec = " + rate + " Mb/sec ");

    NetcdfFile ncfileOrg = NetcdfFile.open(filenameIn);
    NetcdfFile ncfileCopy = NetcdfFile.open(filenameOut);

    ucar.nc2.TestCompare.compareFiles(ncfileOrg, ncfileCopy, true, true, true);
  }

}
