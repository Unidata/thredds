/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.ft.cover.impl;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.inventory.MFile;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.ft.cover.Coverage;
import ucar.nc2.ft.cover.CoverageCS;
import ucar.nc2.ft.cover.CoverageDataset;
import ucar.nc2.ft.cover.collection.CoverageProto;
import ucar.nc2.stream.NcStream;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 3/7/2015
 */
public class CoverageIndexWriter {
  static public final int version = 1;
  static private final Logger logger = LoggerFactory.getLogger(CoverageIndexWriter.class);
  public static final String MAGIC_START = "CoverageGridv01Index";  // was Grib1CollectionIndex


  // indexFile is in the cache
  public boolean writeIndex(String name, File idxFile, List<MFile> files, CoverageDataset cds) throws IOException {
    boolean deleteOnClose = false;

    if (idxFile.exists()) {
      RandomAccessFile.eject(idxFile.getPath());
      if (!idxFile.delete())
        logger.warn(" gc1 cant delete index file {}", idxFile.getPath());
    }
    logger.debug(" createIndex for {}", idxFile.getPath());

    try (RandomAccessFile raf = new RandomAccessFile(idxFile.getPath(), "rw")) {
      raf.order(RandomAccessFile.BIG_ENDIAN);

      //// header message
      raf.write(MAGIC_START.getBytes(CDM.utf8Charset));
      raf.writeInt(version);

      CoverageProto.GridCollection.Builder indexBuilder = CoverageProto.GridCollection.newBuilder();
      indexBuilder.setName(name);
      indexBuilder.setTopDir("faike");

      /* directory and mfile list
      File directory = new File(dcm.getRoot());
      List<GcMFile> gcmfiles = GcMFile.makeFiles(directory, files, allFileSet);
      for (MFile gcmfile : files) {
        CoverageProto.MFile.Builder b = CoverageProto.MFile.newBuilder();
        b.setFilename(gcmfile.getName());
        b.setLastModified(gcmfile.getLastModified());
        b.setLength(gcmfile.getLength());
        //b.setIndex(gcmfile.index);  // LOOK ??
        indexBuilder.addMfiles(b.build());
      }   */

      for (CoverageDataset.CoverageSet cset : cds.getCoverageSets())
        indexBuilder.addCsets( writeCoverageSet( cset));

      //for (Attribute att : cds.getAttributes())
      //  indexBuilder.addAtts(writeAttribute(att));

      CoverageProto.GridCollection index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp

      logger.debug("  write GribCollectionIndex= {} bytes", b.length);
      logger.debug("  file size =  %d bytes", raf.length());
      return true;

    } finally {

      // remove it on failure
      if (deleteOnClose && !idxFile.delete())
        logger.error(" gc1 cant deleteOnClose index file {}", idxFile.getPath());
    }
  }

  protected CoverageProto.CoverageSet writeCoverageSet(CoverageDataset.CoverageSet cset) throws IOException {
    CoverageProto.CoverageSet.Builder b = CoverageProto.CoverageSet.newBuilder();

    b.setName("fake");
    b.setCs( writeCoverageCS( cset.getCoverageCS()));

    for (Coverage coverage : cset.getCoverages())
      b.addCoverages(writeCoverage(coverage));

    return b.build();
  }

  protected CoverageProto.CoverageCS writeCoverageCS(CoverageCS coverCS) throws IOException {
    CoverageProto.CoverageCS.Builder b = CoverageProto.CoverageCS.newBuilder();

    b.setName(coverCS.getName());

    for (CoordinateAxis axis : coverCS.getCoordinateAxes())
      b.addCoords(writeCoordinateAxis(axis));

    for (CoordinateTransform transform : coverCS.getCoordinateTransforms())
       b.addTransforms(writeCoordinateTransform(transform));

     return b.build();
  }

  protected CoverageProto.Coverage writeCoordinateAxis(CoordinateAxis axis) throws IOException {
    CoverageProto.Coverage.Builder b = CoverageProto.Coverage.newBuilder();

    b.setName(axis.getShortName());
    b.setDataType(axis.getDataType().ordinal());

    for (Attribute att : axis.getAttributes())
      b.addAtts(writeAttribute(att));

    b.setAxisType(axis.getAxisType().ordinal());

    return b.build();
  }

  protected CoverageProto.CoordTransform writeCoordinateTransform(CoordinateTransform t) throws IOException {
    CoverageProto.CoordTransform.Builder b = CoverageProto.CoordTransform.newBuilder();

    b.setName(t.getName());
    b.setType(t.getTransformType() == TransformType.Projection ? CoverageProto.CoordTransform.Type.HORIZ : CoverageProto.CoordTransform.Type.VERT);

    for (Parameter att : t.getParameters())
      b.addAtts(writeParameter(att));

    return b.build();
  }

  protected CoverageProto.Coverage writeCoverage(Coverage cover) throws IOException {
    CoverageProto.Coverage.Builder b = CoverageProto.Coverage.newBuilder();

    b.setName(cover.getShortName());
    b.setDataType(cover.getDataType().ordinal());

    for (Attribute att : cover.getAttributes())
      b.addAtts(writeAttribute(att));

    return b.build();
  }

  protected CoverageProto.Attribute.Builder writeAttribute(Attribute att) throws IOException {
    CoverageProto.Attribute.Builder b = CoverageProto.Attribute.newBuilder();

    b.setName(att.getShortName());
    b.setDataType(att.getDataType().ordinal());
    b.setLen(att.getLength());
    if (att.isUnsigned())
      b.setUnsigned(true);

    if (att.getLength() > 0) {
      if (att.isString()) {
        for (int i = 0; i < att.getLength(); i++)
          b.addSdata(att.getStringValue(i));

      } else {
        Array data = att.getValues();
        ByteBuffer bb = data.getDataAsByteBuffer();
        b.setData(ByteString.copyFrom(bb.array()));
      }
    }
    return b;
  }

  protected CoverageProto.Attribute.Builder writeParameter(Parameter att) throws IOException {
    CoverageProto.Attribute.Builder b = CoverageProto.Attribute.newBuilder();

    b.setName(att.getName());
    DataType dtype = att.isString() ? DataType.STRING : DataType.DOUBLE;
    b.setDataType(dtype.ordinal());
    b.setLen(att.getLength());

    if (att.isString()) {
      b.addSdata(att.getStringValue());
    } else {
      b.setData(ByteString.copyFrom(getDataAsByteArray(att)));
    }

    return b;
  }

  private byte[] getDataAsByteArray(Parameter p) {
    ByteBuffer bb = ByteBuffer.allocate(8*p.getLength());
    DoubleBuffer ib = bb.asDoubleBuffer();
    for (double dval : p.getNumericValues())
      ib.put( dval);
    return bb.array();
  }

}
