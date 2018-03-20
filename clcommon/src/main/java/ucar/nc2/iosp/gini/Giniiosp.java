/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.gini;

import ucar.nc2.constants.DataFormatType;
import ucar.ma2.*;
import ucar.nc2.Variable;
import ucar.nc2.iosp.AbstractIOServiceProvider;

import java.io.*;
import java.awt.image.*;
import java.util.List;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

/**
 * IOServiceProvider for GINI files.
 */

public class Giniiosp extends AbstractIOServiceProvider {
  final static int Z_DEFLATED = 8;
  final static int DEF_WBITS = 15;

  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) {
    return Giniheader.isValidFile(raf);
  }

  /////////////////////////////////////////////////////////////////////////////
  // reading

  public void open(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {

    super.open(raf, ncfile, cancelTask);

    Giniheader headerParser = new Giniheader();
    headerParser.read(raf, ncfile);

    ncfile.finish();
  }

  public Array readData(ucar.nc2.Variable v2, Section section) throws IOException, InvalidRangeException {
    // subset
    List<Range> ranges = Section.fill(section, v2.getShape()).getRanges();
    Giniheader.Vinfo vinfo = (Giniheader.Vinfo) v2.getSPobject();
    int[] levels = vinfo.levels;

    if (vinfo.compression == 0)
      return readData(v2, vinfo.begin, ranges, levels);
    else if (vinfo.compression == 2)
      return readCompressedData(v2, vinfo.begin, ranges, levels);
    else if (vinfo.compression == 1)
      return readCompressedZlib(v2, vinfo.begin, vinfo.nx, vinfo.ny, ranges, levels);
    else
      return null;
  }

  private float[] handleLevels(byte[] data,  int[] levels) {
    int level = levels[0];
    float[] a = new float[level];
    float[] b = new float[level];
    float[] fdata = new float[data.length];
    int scale = 1;

    for (int i = 0; i < level; i++) {
      int numer = levels[1 + 5 * i] - levels[2 + 5 * i];
      int denom = levels[3 + 5 * i] - levels[4 + 5 * i];
      a[i] = (numer * 1.f) / (1.f * denom);
      b[i] = levels[1 + 5 * i] - a[i] * levels[3 + 5 * i];
    }

    for (int i = 0; i < data.length; i++) {
      int ival = DataType.unsignedByteToShort(data[i]);
      int k = -1;
      for (int j = 0; j < level; j++) {
        if (levels[3 + (j * 5)] <= ival && ival <= levels[4 + (j * 5)]) {
          k = j;
          scale = levels[5 + j * 5];
        }
      }

      if (k >= 0)
        fdata[i] = (a[k] * ival + b[k]) / scale;
      else
        fdata[i] = 0;

    }
    return fdata;
  }

  private Array makeArray(byte[] data, int[] levels, int[] shape)
  {
    // Default (if no level data) is to just return an array from the bytes.
    Object store = data;
    DataType dt = DataType.BYTE;

    // If have levels, convert data to float and set-up to use that for array
    if (levels != null) {
      store = handleLevels(data, levels);
      dt = DataType.FLOAT;
    }

    // Create array and return
    return Array.factory(dt, shape, store);
  }

  // all the work is here, so can be called recursively
  private Array readData(ucar.nc2.Variable v2, long dataPos, List<Range> ranges,
                         int[] levels) throws IOException, InvalidRangeException {
    // Get to the proper offset and read in the data
    raf.seek(dataPos);
    int data_size = (int) (raf.length() - dataPos);
    byte[] data = new byte[data_size];
    raf.readFully(data);

    // Turn it into an array
    Array array = makeArray(data, levels, v2.getShape());
    return array.sectionNoReduce(ranges);
  }

  // for the compressed data read all out into a array and then parse into requested
  private Array readCompressedData(ucar.nc2.Variable v2, long dataPos, List<Range> ranges,
                                   int[] levels) throws IOException, InvalidRangeException {
    // Get to the proper offset and read in the rest of the compressed data
    raf.seek(dataPos);
    int data_size = (int) (raf.length() - dataPos);
    byte[] data = new byte[data_size];
    raf.readFully(data);

    // Send the compressed data to ImageIO (to handle PNG)
    ByteArrayInputStream ios = new ByteArrayInputStream(data);
    BufferedImage image = javax.imageio.ImageIO.read(ios); // LOOK why ImageIO ??
    DataBuffer db = image.getData().getDataBuffer();

    // If the image had byte data, turn into an array
    if (db instanceof DataBufferByte) {
      DataBufferByte dbb = (DataBufferByte) db;
      Array array = makeArray(dbb.getData(), levels, v2.getShape());
      if (levels == null)
        v2.setCachedData(array, false);
      return array.sectionNoReduce(ranges);
    }

    return null;
  }

  private Array readCompressedZlib(ucar.nc2.Variable v2, long dataPos, int nx, int ny,
                                   List<Range> ranges, int[] levels) throws IOException, InvalidRangeException {
    // Get to the proper offset and read in the rest of the compressed data
    raf.seek(dataPos);
    int data_size = (int) (raf.length() - dataPos);     //  or 5120 as read buffer size
    byte[] data = new byte[data_size];
    raf.readFully(data);

    // Buffer for decompressing data
    byte[] uncomp = new byte[nx * ny];
    int offset = 0;

    // Set-up zlib decompression (inflation)
    Inflater inflater = new Inflater(false);
    inflater.setInput(data);

    // Loop while the inflater has data and we have space in final buffer
    // This will end up ignoring the last few compressed bytes, which
    // correspond to the end of file marker, which is a single row of pixels
    // of alternating 0/255.
    while (inflater.getRemaining() > 0 && offset < uncomp.length) {
      // Try to decompress what's left, which ends up decompressing one block
      try {
        offset += inflater.inflate(uncomp, offset, uncomp.length - offset);
      } catch (DataFormatException ex) {
        System.out.println("ERROR on inflation " + ex.getMessage());
        ex.printStackTrace();
        throw new IOException(ex.getMessage());
      }

      // If the last block finished...
      if (inflater.finished()) {
        // See if anything's left
        int bytesLeft = inflater.getRemaining();
        if (bytesLeft > 0) {
          // Figure out where we are in the input data
          int inputOffset = data_size - bytesLeft;

          // Check if remaining data are zlib--if not copy out and bail
          byte[] b2 = new byte[2];
          System.arraycopy(data, inputOffset, b2, 0, b2.length);
          if (!isZlibHed(b2)) {
            System.arraycopy(data, inputOffset, uncomp, offset, bytesLeft);
            break;
          }

          // Otherwise, set up for decompressing next block
          inflater.reset();
          inflater.setInput(data, inputOffset, bytesLeft);
        }
      }
    }
    inflater.end();

    // Turn the decompressed data into an array, caching as appropriate
    Array array = makeArray(uncomp, levels, v2.getShape());
    if (levels == null && array.getSize() < Variable.defaultSizeToCache)
      v2.setCachedData(array, false);
    return array.sectionNoReduce(ranges);
  }

  static boolean isZlibHed(byte[] buf) {
    short b0 = DataType.unsignedByteToShort(buf[0]);
    short b1 = DataType.unsignedByteToShort(buf[1]);

    if ((b0 & 0xf) == Z_DEFLATED) {
      if ((b0 >> 4) + 8 <= DEF_WBITS) {
        if ((((b0 << 8) + b1) % 31) == 0) {
          return true;
        }
      }
    }

    return false;
  }

  public String getFileTypeId() {
    return DataFormatType.GINI.getDescription();
  }

  public String getFileTypeDescription() {
    return "GOES Ingest and NOAAPORT Interface";
  }

}
