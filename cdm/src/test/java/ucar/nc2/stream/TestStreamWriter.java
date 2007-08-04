/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
