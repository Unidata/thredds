/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.ldm;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * Class Description.
 *
 * @author caron
 * @since Aug 14, 2008
 */
public class MessageIndexer {
  byte[] magic = {0x0b, 117, 0x0f, 114, 0x01, 110, 0x0d, 120};
  int version = 0;
  Indexer iwriter = new Indexer();

  MessageIndexer() throws IOException {
    iwriter.writeBytes(magic);
    iwriter.writeVInt(version);
    iwriter.writeShort(iwriter.size);
  }

  public void add(long offset, int nobs) throws IOException {
    iwriter.writeVLong(offset);
    iwriter.writeVInt(nobs);    
  }

  void close(String filename) throws IOException {
    File file = new File(filename);
    FileOutputStream fos = new FileOutputStream(file);
    WritableByteChannel wbc = fos.getChannel();
    add(0,0);
    System.out.println(" bb size= "+iwriter.bb.position());
    iwriter.bb.flip();
    wbc.write(iwriter.bb);
    wbc.close();
  }

  void dump(String filename) throws IOException {
    File file = new File(filename);
    FileInputStream fos = new FileInputStream(file);
    ReadableByteChannel rbc = fos.getChannel();

    Indexer ireader = new Indexer();
    rbc.read(ireader.bb);
    ireader.bb.flip();

    byte[] m = new byte[8];
    ireader.readBytes(m);
    for (int i=0; i<8; i++)
      if (m[i] != magic[i]) System.out.printf("BAD MAGIC %d %d %n", i, m[i]);
    System.out.printf("MAGIC ok %n");
    System.out.printf(" version = %d %n", ireader.readVInt());
    short size = ireader.readShort();
    System.out.printf(" size = %d %n", size);
    int count = 0;
    while (count < size) {
      long offset = ireader.readVLong();
      int nobs = ireader.readVInt();         
      System.out.printf("  offset= %d nobs= %s %n", offset, nobs);
      if (offset == 0) break;
    }

    rbc.close();
  }


  private class Indexer extends VlenIO {
    ByteBuffer bb;
    short size = 512;

    Indexer() {
      bb = ByteBuffer.allocate(size);
    }

    protected void write(byte b) throws IOException {
      bb.put(b);
    }

    protected void write(byte[] b, int offset, int length) throws IOException {
      for (int i=0; i<length; i++)
        bb.put(b[offset+i]);
    }

    protected byte readByte() throws IOException {
      return bb.get();
    }

    protected void readBytes(byte[] dest) throws IOException {
      bb.get(dest);
    }
  }

  public static void main(String args[]) throws IOException {
    MessageIndexer mi = new MessageIndexer();
    mi.add(16, 1);
    mi.add(32, 2);
    mi.add(48, 3);
    mi.add(12345678, 4321);
    for (int i=1; i<101; i++) {
      mi.add(i*3000, 23);      
    }
    mi.close("D:/bufr/out/test.bufx");
    mi.dump("D:/bufr/out/test.bufx");
  }

}
