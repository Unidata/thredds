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

package ucar.unidata.io.s3;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import com.amazonaws.auth.*;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;


/**
 * @author James McClain, based on work by John Caron and Donald Denbo
 */

public class S3RandomAccessFile extends ucar.unidata.io.RandomAccessFile {
  static public final int defaultS3BufferSize = 1<<19;
  static private final int cacheBlockSize = 1<<20;
  static private final int maxCacheBlocks = 1<<5;

  private AmazonS3URI uri = null;
  private ClientConfiguration config = null;
  private AmazonS3 client = null;
  private String bucket = null;
  private String key = null;
  private ObjectMetadata metadata = null;

  private java.util.Map<Long, byte[]> cache = new java.util.HashMap<Long, byte[]>();
  private java.util.LinkedList<Long> index = new java.util.LinkedList<Long>();

  public S3RandomAccessFile(String url) throws IOException {
    this(url, defaultS3BufferSize);
  }

  public S3RandomAccessFile(String url, int bufferSize) throws IOException {
    super(bufferSize);
    file = null;
    location = url;

    uri = new AmazonS3URI(url);
    bucket = uri.getBucket();
    key = uri.getKey();

    config = new com.amazonaws.ClientConfiguration();
    config.setMaxConnections(128);
    config.setMaxErrorRetry(16);
    config.setConnectionTimeout(100000);
    config.setSocketTimeout(100000);
    config.setRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(32));

    try {
      client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain(), config);
      metadata = client.getObjectMetadata(bucket, key);
    }
    catch (Exception e) {
      client = new AmazonS3Client(new AnonymousAWSCredentials(), config);
      metadata = client.getObjectMetadata(bucket, key);
    }

  }

  public void close() throws IOException {
    uri = null;
    config = null;
    client = null;
    bucket = null;
    key = null;
    metadata = null;
  }

 /**
  * After execution of this function, the given block is guranteed to
  * be in the cache.
  */
  private void ensure(Long key) throws IOException {
    if (!cache.containsKey(key)) {
      long position = key.longValue() * cacheBlockSize;
      int toEOF = (int)(length() - position);
      int bytes = toEOF < cacheBlockSize ? toEOF : cacheBlockSize;
      byte[] buffer = new byte[bytes];

      read__(position, buffer, 0, cacheBlockSize);
      cache.put(key, buffer);
      index.add(key);
      assert(cache.size() == index.size());
      while(cache.size() > maxCacheBlocks) {
	cache.remove(index.pop());
      }

      return;
    }
  }

  /**
   * Read directly from S3 [1], without going through the buffer.
   * All reading goes through here or readToByteChannel;
   *
   * 1. https://docs.aws.amazon.com/AmazonS3/latest/dev/RetrievingObjectUsingJava.html
   *
   * @param pos    start here in the file
   * @param buff   put data into this buffer
   * @param offset buffer offset
   * @param len    this number of bytes
   * @return actual number of bytes read
   * @throws IOException on io error
   */
  @Override
  protected int read_(long pos, byte[] buff, int offset, int len) throws IOException {
    long start = pos / cacheBlockSize;
    long end = (pos+len-1) / cacheBlockSize;

    if (pos >= length()) { // Do not read past end of the file
      return 0;
    }
    else if (end - start > 1) { // If the request touches more than two cache blocks, punt (should never happen)
      return read__(pos, buff, offset, len);
    }
    else if (end - start == 1) { // If the request touches two cache blocks, split it
      int length1 = (int)((end*cacheBlockSize) - pos);
      int length2 = (int)((pos+len) - (end*cacheBlockSize));
      return read_(pos, buff, offset, length1) + read_(pos+length1, buff, offset+length1, length2);
    }

    // Service a request that touches only one cache block
    Long key = new Long(start);
    ensure(key);

    byte[] src = (byte[])cache.get(key);
    int srcPos = (int)(pos - (key.longValue() * cacheBlockSize));
    int toEOB = src.length - srcPos;
    int length = toEOB < len ? toEOB : len;
    System.arraycopy(src, srcPos, buff, offset, length);

    return len;
  }

  private int read__(long pos, byte[] buff, int offset, int len) throws IOException {
    GetObjectRequest rangeObjectRequest = new GetObjectRequest(bucket, key);
    rangeObjectRequest.setRange(pos, pos+len);

    S3Object objectPortion = client.getObject(rangeObjectRequest);
    InputStream objectData = objectPortion.getObjectContent();
    int bytes = 0;
    int totalBytes = 0;

    bytes = objectData.read(buff, offset + totalBytes, len - totalBytes);
    while ((bytes > 0) && ((len - totalBytes) > 0)) {
      totalBytes += bytes;
      bytes = objectData.read(buff, offset + totalBytes, len - totalBytes);
    }

    objectData.close();
    objectPortion.close();

    return totalBytes;
  }

  @Override
  public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {
    int n = (int) nbytes;
    byte[] buff = new byte[n];
    int done = read_(offset, buff, 0, n);
    dest.write(ByteBuffer.wrap(buff));
    return done;
  }

  @Override
  public long length() throws IOException {
    return metadata.getContentLength();
  }

  @Override
  public long getLastModified() {
    return metadata.getLastModified().getTime();
  }
}
