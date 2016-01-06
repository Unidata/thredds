/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.writer;

import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.unidata.io.RandomAccessFile;

import java.io.Closeable;
import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 8/4/2014
 */
public class Grib2NetcdfWriter implements Closeable {
  String fileIn;
  NetcdfFileWriter writer;

  public Grib2NetcdfWriter(String fileIn, String fileOut) throws IOException {
    this.fileIn = fileIn;

    Nc4Chunking chunker = Nc4ChunkingStrategy.factory(Nc4ChunkingStrategy.Strategy.grib, 9, true);
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
          if (i % 10 == 0)
            System.out.println();
        }
      }
    }
  }

  static public int getBitMask(int bitN) {
    if (bitN >= 23) return allOnes;
    return allOnes << 23-bitN;
  }

  static private int allOnes = 0xffffffff;
  static private int mask23 = 0xffffffff;
  static private int mask19 = 0xfffffff0;
  static private int mask15 = 0xffffff00;
  static private int mask11 = 0xfffff000;
  static private int mask07 = 0xffff0000;
  static private int mask03 = 0xfff00000;
  static private int mask00 = 0xff800000;

  // set all the bits=0 in bitMask to 0,

  /**
   * Shave n bits off the float
   * @param value    original floating point
   * @param bitMask  bitMask from getBitMask()
   * @return modified float
   */
  static public float bitShave(float value, int bitMask) {
    if (Float.isNaN(value)) return value;   // ??

    int bits = Float.floatToRawIntBits(value);
    int shave = bits & bitMask;
    //System.out.printf("0x%s -> 0x%s : ", Integer.toBinaryString(bits), Integer.toBinaryString(shave));
    float result = Float.intBitsToFloat(shave);
    //System.out.printf("%f -> %f %n", value, result);
    return result;
  }


  @Override
  public void close() throws IOException {
    writer.close();
  }

  public static void main2(String[] args) {
    String fileIn = "Q:/cdmUnitTest/formats/grib2/ds.mint.bin";
    String fileOut = "C:/tmp/ds.mint.bin";

    try (Grib2NetcdfWriter writer = new Grib2NetcdfWriter(fileIn, fileOut)) {
      writer.write();

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static void main(String[] args) {
    for (int i=0; i<24; i++)
      System.out.printf("%d == %8d == 0x%s == 0x%s%n", i, getBitMask(i), Integer.toHexString(getBitMask(i)), Integer.toBinaryString(getBitMask(i)));
  }

}
