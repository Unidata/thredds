/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib;

import ucar.ma2.DataType;
import ucar.nc2.util.IO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Formatter;
import java.util.zip.Deflater;

/**
 * Abstraction for handling Grib data
 *
 * @author John
 * @since 9/1/2014
 */
public class GribData {

  public static enum InterpolationMethod {none, cubic, linear}


  private static GribData.InterpolationMethod useInterpolationMethod = GribData.InterpolationMethod.linear; // default

  public static GribData.InterpolationMethod getInterpolationMethod() {
    return useInterpolationMethod;
  }

  public static void setInterpolationMethod(GribData.InterpolationMethod interpolationMethod) {
    useInterpolationMethod = interpolationMethod;
  }


  public interface Bean {

    public float[] readData() throws IOException;

    public int getNBits();

    public long getDataLength();

    public long getMsgLength();

    public int getBinScale();

    public int getDecScale();

    public double getMinimum();

    public double getMaximum();

    public double getScale();

  }

  /*
  Code table 11 – Flag
  Bit No. Value Meaning
  1 0 Grid-point data
    1 Spherical harmonic coefficients
  2 0 Simple packing
    1 Complex or second-order packing
  3 0 Floating point values (in the original data) are represented
    1 Integer values (in the original data) are represented
  4 0 No additional flags at octet 14
    1 Octet 14 contains additional flag bits
  The following gives the meaning of the bits in octet 14 ONLY if bit 4 is set to 1. Otherwise octet 14 contains
  regular binary data.
  Bit No. Value Meaning
  5 Reserved – set to zero
  6 0 Single datum at each grid point
  1 Matrix of values at each grid point
  7 0 No secondary bit-maps
  1 Secondary bit-maps present
  8 0 Second-order values constant width
  1 Second-order values different widths
  9–12 Reserved for future use
  Notes:
  (1) Bit 4 shall be set to 1 to indicate that bits 5 to 12 are contained in octet 14 of the Binary data section.
  (2) Bit 3 shall be set to 1 to indicate that the data represented are integer values; where integer values are
  represented, any reference values, if not zero, should be rounded to integer before being applied.
  (3) Where secondary bit-maps are present in the data (used in association with second-order packing and, optionally,
  with a matrix of values at each point), this shall be indicated by setting bit 7 to 1.
  (4) The indicated meaning of bit 6 shall be retained in anticipation of the future reintroduction of a system to define a
  matrix of values at each grid point.
  ____________
   */

  public static class Info {
    public int bitmapLength;     // length of the bitmap section if any
    public long msgLength;       // length of the entire GRIB message
    public long dataLength;       // length of the data section
    public int ndataPoints;      // for Grib1, gds.getNumberPoints; for GRIB2, n data points stored
    public int nPoints;         //  number of points in the GRID
    public float referenceValue;
    public int binaryScaleFactor, decimalScaleFactor, numberOfBits;
    public int originalType;  // code table 5.1

    // GRIB-1 only
    public int flag;

    public int getGridPoint() {
      return (flag & GribNumbers.bitmask[0]);
    }

    public int getPacking() {
      return (flag & GribNumbers.bitmask[1]);
    }

    public int getDataType() {
      return (flag & GribNumbers.bitmask[2]);
    }

    public boolean hasMore() {
      return (flag & GribNumbers.bitmask[3]) != 0;
    }

    public String getGridPointS() {
      return getGridPoint() == 0 ? "grid point" : "Spherical harmonic coefficients";
    }

    public String getPackingS() {
      return getPacking() == 0 ? "simple" : "Complex / second order";
    }

    public String getDataTypeS() {
      return getDataType() == 0 ? "float" : "int";
    }

    private float DD, EE;
    private int missing_value;
    private boolean init = false;
    public float convert(int val) {
      if (!init) {
        DD = (float) java.lang.Math.pow((double) 10, decimalScaleFactor);
        EE = (float) java.lang.Math.pow( 2.0, binaryScaleFactor);
        missing_value = (2 << numberOfBits - 1) - 1;       // all ones - reserved for missing value
        init = true;
      }

      if (val == missing_value) return Float.NaN;
      return (referenceValue + val * EE) / DD;
    }
  }

  public static byte[] calcScaleOffset(GribData.Bean bean1, Formatter f) {
    float[] data;
    try {
      data = bean1.readData();
    } catch (IOException e) {
      f.format("IOException %s", e.getMessage());
      return null;
    }
    int npoints = data.length;

    // we always use unsigned packed
    // "If the packed values are intended to be interpreted as signed/unsigned integers"
    // http://www.unidata.ucar.edu/software/netcdf/docs/BestPractices.html
    int nbits = bean1.getNBits();
    int width = (2 << nbits - 1) - 2;                // unsigned
    int missing_value = (2 << nbits - 1) - 1;       // all ones - reserved for missing value

    // int width2 = (2 << (nbits-1)) - 1;  // signed
    f.format(" nbits = %d%n", nbits);
    f.format(" npoints = %d%n", npoints);
    f.format(" width = %d (0x%s) %n", width, Long.toHexString(width));
    f.format(" scale = %g %n", bean1.getScale());
    f.format(" resolution = %g %n", bean1.getScale() / 2);
    f.format(" range = %f %n%n", bean1.getMaximum() - bean1.getMinimum());

    float dataMin = Float.MAX_VALUE;
    float dataMax = -Float.MAX_VALUE;
    for (float fd : data) {
      if (Float.isNaN(fd)) continue;
      dataMin = Math.min(dataMin, fd);
      dataMax = Math.max(dataMax, fd);
    }
    f.format("           actual    computed%n");
    f.format(" dataMin = %8f %8f%n", dataMin, bean1.getMinimum());
    f.format(" dataMax = %8f %8f%n", dataMax, bean1.getMaximum());

    f.format(" actual range = %f%n", (dataMax - dataMin));

    // scale_factor =(dataMax - dataMin) / (2^n - 1)
    // add_offset = dataMin + 2^(n-1) * scale_factor

    double scale_factor = (dataMax - dataMin) / width;
    // float add_offset = dataMin + width2 * scale_factor / 2; // signed
    double add_offset = dataMin;                             // unsigned

    f.format(" scale_factor = %g%n", scale_factor);
    f.format(" add_offset = %g%n", add_offset);

    // unpacked_data_value = packed_data_value * scale_factor + add_offset
    // packed_data_value = nint((unpacked_data_value - add_offset) / scale_factor)

    ByteBuffer bb = ByteBuffer.allocate(4 * npoints);
    IntBuffer intBuffer = bb.asIntBuffer();
    double diffMax = -Double.MAX_VALUE;
    double diffTotal = 0;
    double diffTotal2 = 0;
    for (float fd : data) {
      if (Float.isNaN(fd)) {
        intBuffer.put(missing_value);
        continue;
      }

      // otherwise pack it
      int packed_data = (int) Math.round((fd - add_offset) / scale_factor);   // nint((unpacked_data_value - add_offset) / scale_factor)
      double unpacked_data = packed_data * scale_factor + add_offset;
      double diff = Math.abs(fd - unpacked_data);
      if (diff > scale_factor / 2)
        f.format("***   org=%g, packed_data=%d unpacked=%g diff = %g%n", fd, packed_data, unpacked_data, diff);

      diffMax = Math.max(diffMax, diff);
      diffTotal += diff;
      diffTotal2 += diff * diff;
      intBuffer.put(packed_data);
    }

    f.format("%n max_diff = %g%n", diffMax);
    f.format(" avg_diff = %g%n", diffTotal / data.length);

    // Math.sqrt( sumsq/n - avg * avg)
    double mean = diffTotal / npoints;
    double var = (diffTotal2 / npoints - mean * mean);
    f.format(" std_diff = %g%n", Math.sqrt(var));

    f.format("%nCompression%n");
    f.format(" number of values = %d%n", npoints);
    f.format(" uncompressed as floats = %d%n", npoints * 4);
    int packedBitsLen = npoints * nbits / 8;
    f.format(" uncompressed packed bits = %d%n", packedBitsLen);
    f.format(" grib data length = %d%n", bean1.getDataLength());
    f.format(" grib msg length = %d%n", bean1.getMsgLength());

    byte[] bdata = convertToBytes(data);
    byte[] scaledData = bb.array();

    ////////////////////////////////////////////
    f.format("%ndeflate (float)%n");
    Deflater deflater = new Deflater();
    deflater.setInput(bdata);
    deflater.finish();
    int compressedSize = deflater.deflate(new byte[10 * npoints]);
    deflater.end();
    f.format(" compressedSize = %d%n", compressedSize);
    f.format(" ratio floats / size = %f%n", (float) (npoints * 4) / compressedSize);
    f.format(" ratio packed bits / size = %f%n", (float) packedBitsLen / compressedSize);
    f.format(" ratio size / grib = %f%n", (float) compressedSize / bean1.getMsgLength());

    /////////////////////////////////////////////////////////
    f.format("%ndeflate (scaled ints)%n");
    deflater = new Deflater();
    deflater.setInput(scaledData);
    deflater.finish();
    compressedSize = deflater.deflate(new byte[10 * npoints]);
    deflater.end();

    f.format(" compressedSize = %d%n", compressedSize);
    f.format(" ratio floats / size = %f%n", (float) (npoints * 4) / compressedSize);
    f.format(" ratio packed bits / size = %f%n", (float) packedBitsLen / compressedSize);
    f.format(" ratio size / grib = %f%n", (float) compressedSize / bean1.getMsgLength());

    //////////////////////////////////////////////////////////////
    f.format("%nbzip2 (floats)%n");
    try (ByteArrayOutputStream out = new ByteArrayOutputStream(2 * compressedSize)) {
      org.itadaki.bzip2.BZip2OutputStream zipper = new org.itadaki.bzip2.BZip2OutputStream(out);
      InputStream fin = new ByteArrayInputStream(bdata);
      IO.copy(fin, zipper);
      zipper.close();
      compressedSize = out.size();
      f.format(" compressedSize = %d%n", compressedSize);
      f.format(" ratio floats / size = %f%n", (float) (npoints * 4) / compressedSize);
      f.format(" ratio packed bits / size = %f%n", (float) packedBitsLen / compressedSize);
      f.format(" ratio size / grib = %f%n", (float) compressedSize / bean1.getMsgLength());

    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    //////////////////////////////////////////////////////////////
    f.format("%nbzip2 (scaled ints)%n");
    try (ByteArrayOutputStream out = new ByteArrayOutputStream(2 * compressedSize)) {
      org.itadaki.bzip2.BZip2OutputStream zipper = new org.itadaki.bzip2.BZip2OutputStream(out);
      InputStream fin = new ByteArrayInputStream(scaledData);
      IO.copy(fin, zipper);
      zipper.close();
      compressedSize = out.size();
      f.format(" compressedSize = %d%n", compressedSize);
      f.format(" ratio floats / size = %f%n", (float) (npoints * 4) / compressedSize);
      f.format(" ratio packed bits / size = %f%n", (float) packedBitsLen / compressedSize);
      f.format(" ratio size / grib = %f%n", (float) compressedSize / bean1.getMsgLength());

    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    return scaledData;
  }

  static public byte[] compressScaled(GribData.Bean bean) throws IOException {
    float[] data = bean.readData();
    int npoints = data.length;

    // we always use unsigned packed
    // "If the packed values are intended to be interpreted as signed/unsigned integers"
    // http://www.unidata.ucar.edu/software/netcdf/docs/BestPractices.html
    int nbits = bean.getNBits();
    int width = (2 << nbits - 1) - 2;                // unsigned
    int missing_value = (2 << nbits - 1) - 1;       // all ones - reserved for missing value

    float dataMin = Float.MAX_VALUE;
    float dataMax = -Float.MAX_VALUE;
    for (float fd : data) {
      if (Float.isNaN(fd)) continue;
      dataMin = Math.min(dataMin, fd);
      dataMax = Math.max(dataMax, fd);
    }

    // scale_factor =(dataMax - dataMin) / (2^n - 1)
    // add_offset = dataMin + 2^(n-1) * scale_factor

    double scale_factor = (dataMax - dataMin) / width;
    // float add_offset = dataMin + width2 * scale_factor / 2; // signed
    double add_offset = dataMin;                             // unsigned

    // unpacked_data_value = packed_data_value * scale_factor + add_offset
    // packed_data_value = nint((unpacked_data_value - add_offset) / scale_factor)

    ByteBuffer bb = ByteBuffer.allocate(4 * npoints + 24);
    bb.putDouble(scale_factor);
    bb.putDouble(add_offset);

    bb.putInt(nbits);
    bb.putInt(npoints);

    for (float fd : data) {
      if (Float.isNaN(fd)) {
        bb.putInt(missing_value);
      } else {
        int packed_data = (int) Math.round((fd - add_offset) / scale_factor);   // nint((unpacked_data_value - add_offset) / scale_factor)
        bb.putInt(packed_data);
      }
    }

    byte[] scaledData = bb.array();
    try (ByteArrayOutputStream out = new ByteArrayOutputStream(4 * npoints + 24)) {
      org.itadaki.bzip2.BZip2OutputStream zipper = new org.itadaki.bzip2.BZip2OutputStream(out);
      InputStream fin = new ByteArrayInputStream(scaledData);
      IO.copy(fin, zipper);
      zipper.close();
      return out.toByteArray();

    } catch (IOException ioe) {
      ioe.printStackTrace();
      return null;
    }
  }

  // only used by test code
  private static byte[] buffer;  // LOOK optimize
  static public float[] uncompressScaled(byte[] bdata) throws IOException {
    if (buffer == null)
      buffer = new byte[524288];

    int outLength = Math.max(20 * bdata.length, 8000);
    ByteArrayOutputStream out = new ByteArrayOutputStream(outLength);
    ByteArrayInputStream in = new ByteArrayInputStream(bdata);
    try (org.itadaki.bzip2.BZip2InputStream bzIn = new org.itadaki.bzip2.BZip2InputStream(in, false)) {
      int bytesRead;
      while ((bytesRead = bzIn.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
      out.close();

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

    ByteBuffer bb = ByteBuffer.wrap(out.toByteArray());
    double scale_factor = bb.getDouble();
    double add_offset = bb.getDouble();

    int nbits = bb.getInt();
    int npoints = bb.getInt();
    int missing_value = (2 << nbits - 1) - 1;       // all ones - reserved for missing value

    // unpacked_data_value = packed_data_value * scale_factor + add_offset
    // packed_data_value = nint((unpacked_data_value - add_offset) / scale_factor)

    float[] result = new float[npoints];
    int count = 0;
    while (bb.hasRemaining()) {
      int packed_data = bb.getInt();
      if (packed_data == missing_value) result[count++] = Float.NaN;
      else result[count++] = (float) (scale_factor * packed_data + add_offset);
    }

    return result;
  }


  public static byte[] convertToBytes(float[] data) {
    ByteBuffer bb = ByteBuffer.allocate(data.length * 4);
    for (float val : data) bb.putFloat(val);
    return bb.array();
  }

  static public byte[] convertToBytes(int[] data) {
    ByteBuffer bb = ByteBuffer.allocate(data.length * 4);
    for (int val : data) bb.putInt(val);
    return bb.array();
  }

  static public double entropy(byte[] data) {
    int[] p = new int[256];

    // count occurrences
    for (byte b : data) {
      short s = DataType.unsignedByteToShort(b);
      p[s]++;
    }

    double n = data.length;
    double iln2 = 1.0 / Math.log(2.0);
    double sum = 0.0;
    for (int i = 0; i < 256; i++) {
      if (p[i] == 0) continue;
      double prob = ((double) p[i]) / n;
      sum += Math.log(prob) * prob * iln2;
    }

    return (sum == 0) ? 0.0 : -sum;
  }

  static public double entropy(int nbits, int[] data) {
    if (data == null) return 0.0;

    int n = (int) Math.pow(2, nbits);
    int[] p = new int[n];

    // count occurrences
    int count = 0;
    for (int b : data) {
      if (b < 0 || b > n - 1) {
        //System.out.printf("BAD %d at index %d; max=%d%n", b, count, n - 1);
        // just skip return Double.NaN;
      } else {
        p[b]++;
      }
      count++;
    }

    double len = data.length;
    double iln2 = 1.0 / Math.log(2.0);
    double sum = 0.0;
    for (int i = 0; i < n; i++) {
      if (p[i] == 0) continue;
      double prob = ((double) p[i]) / len;
      sum += Math.log(prob) * prob * iln2;
    }

    return (sum == 0) ? 0.0 : -sum;
  }
}
