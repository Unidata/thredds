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
package ucar.nc2;

import ucar.nc2.util.Stat;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/*
Normal timing/data/writeMMap 200 msec
Normal timing/data/writeMMap10 452 msec
Normal timing/data/writeMMap100 3713 msec

Normal timing/data/writeMMap 38 msec
Normal timing/data/writeMMap10 82 msec
Normal timing/data/writeMMap100 612 msec

Normal timing/data/writeMMap 14 msec
Normal timing/data/writeMMap10 68 msec
Normal timing/data/writeMMap100 1748 msec

Normal timing/data/writeMMap 16 msec
Normal timing/data/writeMMap10 68 msec
Normal timing/data/writeMMap100 543 msec

Normal timing/data/writeMMap 18 msec
Normal timing/data/writeMMap10 100 msec
Normal timing/data/writeMMap100 547 msec
Read1 8 msec
Read10 134 msec
Read100 2377 msec

Read1 153 msec
Read10 150 msec
Read100 483 msec

Read1 11 msec
Read10 56 msec
Read100 464 msec

Read1 28 msec
Read10 43 msec
Read100 436 msec

direct, no preset size, 1K writes
timeWrite to timing/data/writeCD-1139873758; n records = 1000 took 70 msecs
timeWrite to timing/data/writeCD10-1613591214; n records = 10000 took 601 msecs
timeWrite to timing/data/writeCD100-1970697909; n records = 100000 took 19177 msecs

direct, no preset size, 100K writes
timeWrite to timing/data/writeCD-1854260520; n records = 1000 took 70 msecs
timeWrite to timing/data/writeCD101962807619; n records = 10000 took 261 msecs
timeWrite to timing/data/writeCD100556966609; n records = 1000 took 13419 msecs

direct, preset size, 1K writes
timeWrite to timing/data/writeCD-1443784873; n records = 1000 took 50 msecs
timeWrite to timing/data/writeCD10-162465759; n records = 10000 took 110 msecs
timeWrite to timing/data/writeCD100420100935; n records = 100000 took 9253 msecs

direct, preset size, 100K writes
timeWrite to timing/data/writeCD1802409995; n records = 1000 took 50 msecs
timeWrite to timing/data/writeCD101487368372; n records = 10000 took 130 msecs
timeWrite to timing/data/writeCD100-1580991209; n records = 1000 took 8232 msecs

// mmap, preset size
timeWrite to timing/data/writeMM-433578863; n records = 1000 took 10 msecs
timeWrite to timing/data/writeMM10697121967; n records = 10000 took 60 msecs
timeWrite to timing/data/writeMM1001910794267; n records = 100000 took 511 msecs

Read1 180 msec  count= 19    1M/.18 = 5.5 MB/sec
Read10 927 msec  count= 17  10/.927 = 10.8 MB/sec
Read100 14188 msec  count= 13 100/14.2 = 7.0 Mb/sec

Read1 181 msec  count= 19
Read10 1095 msec  count= 17
Read100 21403 msec  count= 13

writing is blazing fast : 100Mb with variable record size:

writeMMap to timing/data/writeMM-1K333286500; n records = 100000 took 711 msecs
writeMMap to timing/data/writeMM-10K-1328095594; n records = 10000 took 591 msecs
writeMMap to timing/data/writeMM-100K-625020937; n records = 1000 took 581 msecs

*/

public class TimeMMap {

  static boolean debug = false, showEach = true, check = false;
  static Random random = new Random(System.currentTimeMillis());

  static void writeChannelDirect(String filename, long fileSize, int recordSize, Stat stat) throws Exception {
    String name = "writeCD ";
    long endTime, size = 0;
    long startTime = System.currentTimeMillis();

    long fileSizeBytes = fileSize * 1000 * 1000; // Mbytes
    int recordSizeBytes = recordSize * 1000; // Kbytes
    int n = (int) (fileSizeBytes/recordSizeBytes); // number of writes

    String rfilename = filename + random.nextInt();
    java.io.RandomAccessFile file = new java.io.RandomAccessFile(rfilename, "rw");
    file.setLength( fileSizeBytes);

    FileChannel channel = file.getChannel();
    if (debug) System.out.println ("Opened file to write:'" + rfilename+ "', size=" + channel.size()+
      " fileSize = "+fileSize+" Mb; recSize = "+recordSize+" Kb; nrecs = "+n);

    int nelems = recordSizeBytes/4;
    int[] data = new int[nelems];
    for (int i=0;i<nelems;i++) data[i] = i;

    ByteBuffer buff = ByteBuffer.allocate(recordSizeBytes);
    // ByteBuffer buff = ByteBuffer.allocateDirect(recordSizeBytes);
    IntBuffer src = buff.asIntBuffer();
    //showBufferInfo(" start bb ", buff);
    //showBufferInfo(" start ib ", src);
    //n = 2;
    for (int i=0; i<n; i++) {
      buff.clear();
      src.clear();
      src.put( data);
      channel.write(buff);
      showBufferInfo(" end bb ", buff);
      showBufferInfo(" end ib ", src);
      System.out.println(" channel pos= "+channel.position());
    }

    channel.close();
    file.close();

    endTime = System.currentTimeMillis();
    long diff = endTime - startTime;
    if (stat != null)
      stat.avg(name+filename, diff);
    if (showEach || stat == null) System.out.println("timeWrite to "+rfilename+ "; n records = "+n+" took "+diff+ " msecs");

    if (check) checkWrite( rfilename);
  }

  static void showBufferInfo( String name, Buffer b) {
    System.out.println(" buffer= "+name+" pos= "+b.position()+" limit= "+b.limit());
  }

  static void timeWriteMMap(String filename, long fileSize, int recordSize, Stat stat) throws Exception {
    String name = "WriteMM ";
    long endTime;
    long startTime = System.currentTimeMillis();

    long fileSizeBytes = fileSize * 1000 * 1000; // Mbytes
    int recordSizeBytes = recordSize * 1000; // Kbytes
    int n = (int) (fileSizeBytes/recordSizeBytes); // number of writes
    long size = fileSizeBytes + n * 4;

    String rfilename = filename + random.nextInt();
    java.io.RandomAccessFile file = new java.io.RandomAccessFile(rfilename, "rw");
    FileChannel channel = file.getChannel();
    if (debug) System.out.println ("Opened file to write:'" + rfilename+ "', size=" + channel.size()+
      " fileSize = "+fileSize+" Mb; recSize = "+recordSize+" Kb; nrecs = "+n);
    ByteBuffer mbuff = channel.map(FileChannel.MapMode.READ_WRITE, (long) 0, size); // ?? fileSize

    int nelems = recordSizeBytes/4;
    int[] data = new int[nelems];
    for (int i=0;i<nelems;i++) data[i] = i;

    int pos = 0;
    for (int i=0; i<n; i++) {
      mbuff.position( pos);
      mbuff.putInt(nelems);
      IntBuffer intBuff = mbuff.asIntBuffer();
      intBuff.put(data);
      pos += (nelems + 1) * 4;
    }

    channel.close();
    file.close();

    endTime = System.currentTimeMillis();
    long diff = endTime - startTime;
    if (stat != null)
      stat.avg(name+filename, diff);
    if (showEach || stat == null) System.out.println("writeMMap to "+rfilename+ "; n records = "+n+" took "+diff+ " msecs");

    if (check) checkWrite( rfilename);
  }

  static void checkWrite(String filename) throws Exception {
    java.io.RandomAccessFile file = new java.io.RandomAccessFile(filename, "r");
    FileChannel channel = file.getChannel();
    int size = (int) channel.size();

    if (debug) System.out.println ("Opened file to read:'" + filename+ "', size=" + size);
    ByteBuffer mbuff = channel.map(FileChannel.MapMode.READ_ONLY, (long) 0, size);

    System.out.println("checking "+filename);
    int pos = 0;
out:while (pos < size) {
      mbuff.position( pos);
      int nelems = mbuff.getInt();
      int[] data = new int[nelems];
      IntBuffer intBuff = mbuff.asIntBuffer();
      intBuff.get(data);
      for (int j=0; j<nelems; j++) {
        if (data[j] != j) System.out.println(" bad at pos "+pos);
        break out;
      }
      pos += (nelems + 1) * 4;
    }
    channel.close();
    file.close();
  }

  //////////////////////////////////////////////////////////////////////////////
  // read

  static long timeReadMMap(String filename, Stat stat) throws Exception {
    long endTime;
    long startTime = System.currentTimeMillis();

    java.io.RandomAccessFile file = new java.io.RandomAccessFile(filename, "r");
    FileChannel channel = file.getChannel();
    long fileSizeBytes = channel.size();
    int sm = (int) fileSizeBytes / 1000 / 1000;
    if (debug) System.out.println ("Try to map file:'" + filename+ "', size=" + fileSizeBytes);
    ByteBuffer mbuff = channel.map(FileChannel.MapMode.READ_ONLY, (long) 0, fileSizeBytes);

    int nreads = 0;
    int pos = 0;
out:while (pos < fileSizeBytes) {
      mbuff.position( pos);
      int nelems = mbuff.getInt();
      int[] data = new int[nelems];
      IntBuffer intBuff = mbuff.asIntBuffer();
      intBuff.get(data);
      for (int j=0; j<nelems; j++) {
        if (data[j] != j) {
          System.out.println(" bad at pos "+pos+" file= "+filename);
          break out;
        }
      }
      pos += (nelems + 1) * 4;
      nreads++;
    }

    channel.close();
    file.close();

    endTime = System.currentTimeMillis();
    long diff = endTime - startTime;
    if (stat != null)
      stat.avg("readMM "+sm, diff);
    if (showEach || stat == null) System.out.println("readMM from "+filename+" nbytes= "+pos+
      " nreads = "+nreads+ " took "+diff+ " msecs");

    return fileSizeBytes;
  }

  static long timeReadCD(String filename, Stat stat) throws Exception {
    long endTime;
    long startTime = System.currentTimeMillis();

    java.io.RandomAccessFile file = new java.io.RandomAccessFile(filename, "r");
    FileChannel channel = file.getChannel();
    long fileSizeBytes = channel.size();
    int sm = (int) fileSizeBytes / 1000 / 1000;
    if (debug) System.out.println ("readCD file:'" + filename+ "', size=" + fileSizeBytes);

    int nreads = 0;
    int pos = 0;
out:while (pos < fileSizeBytes) {
      file.seek( pos);
      int nelems = file.readInt();
      ByteBuffer bbuff = ByteBuffer.allocateDirect( nelems * 4);
      IntBuffer ibuff = bbuff.asIntBuffer();
      channel.read(bbuff);
      int[] data = new int[nelems]; // another copy !!
      ibuff.get(data);
      for (int j=0; j<nelems; j++) {
        if (data[j] != j) {
          System.out.println(" bad at pos "+pos+" file= "+filename);
          break out;
        }
      }
      pos += (nelems + 1) * 4;
      nreads++;
    }

    channel.close();
    file.close();

    endTime = System.currentTimeMillis();
    long diff = endTime - startTime;
    if (stat != null)
      stat.avg("readCD "+sm, diff);
    if (showEach || stat == null) System.out.println("readCD from "+filename+" nbytes= "+pos+
      " nreads = "+nreads +" took "+diff+ " msecs");

    return fileSizeBytes;
  }

  /////////////////////////////////////////////////////////////////////////////////

  public static void testWriteMM(Stat s, int n) throws Exception {
    for (int i=0; i<n; i++) {
      timeWriteMMap( "timing/data/writeMM-1K", 1, 1, s);
      timeWriteMMap( "timing/data/writeMM-4K", 1, 4, s);
      timeWriteMMap( "timing/data/writeMM-8K", 1, 8, s);
      timeWriteMMap( "timing/data/writeMM-16K", 1, 16, s);
      timeWriteMMap( "timing/data/writeMM-32K", 1, 32, s);
      timeWriteMMap( "timing/data/writeMM-100K", 1, 100, s);

      timeWriteMMap( "timing/data/writeMM10-1K", 10, 1, s);
      timeWriteMMap( "timing/data/writeMM10-4K", 10, 4, s);
      timeWriteMMap( "timing/data/writeMM10-8K", 10, 8, s);
      timeWriteMMap( "timing/data/writeMM10-16K", 10, 16, s);
      timeWriteMMap( "timing/data/writeMM10-32K", 10, 32, s);
      timeWriteMMap( "timing/data/writeMM10-100K", 10, 100, s);

      timeWriteMMap( "timing/data/writeMM100-1K", 100, 1, s);
      timeWriteMMap( "timing/data/writeMM100-4K", 100, 4, s);
      timeWriteMMap( "timing/data/writeMM100-8K", 100, 8, s);
      timeWriteMMap( "timing/data/writeMM100-16K", 100, 16, s);
      timeWriteMMap( "timing/data/writeMM100-32K", 100, 32, s);
      timeWriteMMap( "timing/data/writeMM100-100K", 100, 100, s);

      if (s != null) s.print();
    }
  }

  public static void testWriteCD(Stat s) throws Exception {
    //writeChannelDirect( "timing/data/writeCD", 1, 1, s);
    //writeChannelDirect( "timing/data/writeCD10", 10, 1, s);
    writeChannelDirect( "timing/data/writeCD100", 100, 100, s);
  }

  public static void testReadMM(Stat s, int n) throws Exception {
    long totalRead = 0;
    for (int i=0; i<n; i++) {
      try {
        File dir = new File("timing/data/");
        String[] flist = dir.list();
        for (int j=0; j<flist.length; j++)
          totalRead += timeReadMMap( "timing/data/"+flist[j], s);

      } catch (IOException ioe) {
        ioe.printStackTrace();
        System.out.println("MM total before error= " + totalRead);
        break;
      }
      if (s != null) s.print();
      System.out.println("MM total done = " + totalRead);
    }
  }

  public static void testReadCD(Stat s, int n) throws Exception {
    long totalRead = 0;
    for (int i=0; i<n; i++) {
      try {
        File dir = new File("timing/data/");
        String[] flist = dir.list();
        for (int j=0; j<flist.length; j++)
          totalRead += timeReadCD( "timing/data/"+flist[j], s);
      } catch (IOException ioe) {
        ioe.printStackTrace();
        System.out.println("CD total before error= " + totalRead);
        break;
      }
      if (s != null) s.print();
      System.out.println("CD total done = " + totalRead);
    }
  }

  public static void main(String[] args) throws Exception  {
    Stat stat = new Stat();

    //testWriteCD(null);
    testWriteMM(stat, 10);

    //testReadMM( stat, 5);
    //testReadCD( stat, 5);

    stat.print();
  }

}


