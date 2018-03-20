/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.stream;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by rmay on 8/13/15.
 */
public class NcStreamCompressedOutputStream extends DataOutputStream {
  private OutputStream writer;
  private DeflaterOutputStream dout;
  private ByteArrayOutputStream buffer;

  public NcStreamCompressedOutputStream(OutputStream out, int bufferSize, int level) {
    super(out);

    // Save the original out for use when we flush
    writer = out;

    // write to an internal buffer, so we can find out the size when
    // compressed
    buffer = new ByteArrayOutputStream(bufferSize);
    if (level >= 0)
      dout = new DeflaterOutputStream(buffer, new Deflater(level), 4 * 1024);
    else
      dout = new DeflaterOutputStream(buffer, new Deflater(), 4 * 1024);

    // Override out to point to our compressed stream
    this.out = new BufferedOutputStream(dout, 1024 * 1024);
  }

  @Override
  public void flush() throws IOException {
    // Make sure we flush out our stream
    out.flush();

    // Have to finish the deflater in order to get proper block.
    dout.finish();

    // Grab size of compressed data, write this out and then the block
    // of compressed data. Set number of bytes written to this value.
    int compressedSize = buffer.size();
    written = compressedSize;
    written += NcStream.writeVInt(writer, compressedSize);
    buffer.writeTo(writer);

    // Reset buffer so that in theory we could continue to write to this
    // stream.
    buffer.reset();
  }
}
