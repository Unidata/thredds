/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.writer;

import java.io.Closeable;
import java.io.IOException;

import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.unidata.io.RandomAccessFile;

/**
 * Experimental code to write GRIB to netcdf4.
 *
 * @author caron
 * @since 8/4/2014
 */
public class GribToNetcdfWriter implements Closeable {
  String fileIn;
  NetcdfFileWriter writer;

  public GribToNetcdfWriter(String fileIn, String fileOut) throws IOException {
    this.fileIn = fileIn;

    Nc4Chunking chunker = Nc4ChunkingStrategy.factory(Nc4Chunking.Strategy.grib, 9, true);
    writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, fileOut, chunker);
  }

  public void write() throws IOException {

    try (RandomAccessFile raf = RandomAccessFile.acquire(fileIn)) {

      Grib2RecordScanner scanner = new Grib2RecordScanner(raf);
      while (scanner.hasNext()) {
        Grib2Record gr = scanner.next();
        float[] data = gr.readData(raf);
        for (int i=0; i<data.length; i++) {
          data[i] = bitShave(data[i], mask11);
        }
      }
    }
  }

  public static int getBitMask(int bitN) {
    if (bitN >= 23) return allOnes;
    return allOnes << 23-bitN;
  }

  private static final int allOnes = 0xffffffff;
  private static final int mask23 = 0xffffffff;
  private static final int mask19 = 0xfffffff0;
  private static final int mask15 = 0xffffff00;
  private static final int mask11 = 0xfffff000;
  private static final int mask07 = 0xffff0000;
  private static final int mask03 = 0xfff00000;
  private static final int mask00 = 0xff800000;

  // set all the bits=0 in bitMask to 0,

  /**
   * Shave n bits off the float
   * @param value    original floating point
   * @param bitMask  bitMask from getBitMask()
   * @return modified float
   */
  public static float bitShave(float value, int bitMask) {
    if (Float.isNaN(value)) return value;   // ??

    int bits = Float.floatToRawIntBits(value);
    int shave = bits & bitMask;
    return Float.intBitsToFloat(shave);
  }


  @Override
  public void close() throws IOException {
    writer.close();
  }

  // Write Grib file to a netcdf4 file. Experimental.
  public static void main(String[] args) {
    String fileIn = (args.length > 0) ? args[0] : "Q:/cdmUnitTest/formats/grib2/LMPEF_CLM_050518_1200.grb";
    String fileOut = (args.length > 1) ? args[1] : "C:/tmp/ds.mint.bi";

    try (GribToNetcdfWriter writer = new GribToNetcdfWriter(fileIn, fileOut)) {
      writer.write();

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}
