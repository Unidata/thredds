/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import junit.framework.TestCase;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * @author caron
 * @since Jan 2, 2008
 */
public class TestIndexChunker {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testFull() throws InvalidRangeException {
    int[] shape = new int[] {123,22,92,12};
    Section section = new Section(shape);
    IndexChunker index = new IndexChunker(shape, section);
    assert index.getTotalNelems() == section.computeSize();
    IndexChunker.Chunk chunk = index.next();
    assert chunk.getNelems() == section.computeSize();
    assert !index.hasNext();
  }

  @Test
  public void testPart() throws InvalidRangeException {
    int[] full = new int[] {2, 10, 20};
    int[] part = new int[] {2, 5, 20};
    Section section = new Section(part);
    IndexChunker index = new IndexChunker(full, section);
    assert index.getTotalNelems() == section.computeSize();
    IndexChunker.Chunk chunk = index.next();
    assert chunk.getNelems() == section.computeSize()/2;
  }

  @Test
  public void testChunkerTiled() throws InvalidRangeException {
    Section dataSection = new Section("0:0, 20:39,  0:1353 ");
    Section wantSection = new Section("0:2, 22:3152,0:1350");
    IndexChunkerTiled index = new IndexChunkerTiled(dataSection, wantSection);
    while(index.hasNext()) {
      Layout.Chunk chunk = index.next();
      System.out.println(" "+chunk);
    }
  }

  @Test
  public void testChunkerTiled2() throws InvalidRangeException {
    Section dataSection = new Section("0:0, 40:59,  0:1353  ");
    Section wantSection = new Section("0:2, 22:3152,0:1350");
    IndexChunkerTiled index = new IndexChunkerTiled(dataSection, wantSection);
    while(index.hasNext()) {
      Layout.Chunk chunk = index.next();
      System.out.println(" "+chunk);
    }
  }

  // @Test
  public void testDean() throws IOException {
    Nc4Chunking chunkingStrategy = Nc4ChunkingStrategy.factory(Nc4Chunking.Strategy.standard, 6, false);
    NetcdfFileWriter ncSubsetFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, "C:/tmp/test.nc4", chunkingStrategy);
  }

}
