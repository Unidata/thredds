/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Section;
import ucar.nc2.write.Nc4ChunkingDefault;
import ucar.nc2.write.Nc4ChunkingStrategy;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 5/9/14
 */
public class TestChunkingIndex {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testChunkingIndex() {
    testOne(new int[]{100,100}, 500);
    testOne(new int[]{100,100}, 499);
    testOne(new int[]{100,100}, 100*100);
    testOne(new int[]{77, 3712, 2332}, 500*1000);
    testOne(new int[]{77, 3712, 2332}, 50*1000);
    testOne(new int[]{77, 3712, 2332}, 5*1000);
  }

  private void testOne(int[] shape, long maxChunkElems) {
    show("shape", shape);
    System.out.printf(" max = %d%n", maxChunkElems);
    FileWriter2.ChunkingIndex index = new FileWriter2.ChunkingIndex(shape);
    int[] result = index.computeChunkShape(maxChunkElems);
    show("chunk", result);
    long shapeSize = new Section(result).computeSize();
    System.out.printf(" size = %d%n%n", shapeSize);
    assert shapeSize <= maxChunkElems;
  }

  private void show(String what, int[] result) {
    System.out.printf("%s= (", what);
    for (int r : result) System.out.printf("%d,", r);
    System.out.printf(")%n");
  }

  @Test
  public void testChunkingStrategy() {
    Dimension d2 = new Dimension("2", 2);
    Dimension d10 = new Dimension("10", 10);
    Dimension d20 = new Dimension("20", 20);
    Dimension dun = new Dimension("u", 0, true, true, false);

    testOneStrategy(new Dimension[]{dun, d10, d20}, 50, 40);
    testOneStrategy(new Dimension[]{dun, d10, d20}, 500, 400);
    testOneStrategy(new Dimension[]{dun}, 500, 500);
    testOneStrategy(new Dimension[]{dun, d2}, 101, 100);
    testOneStrategy(new Dimension[]{dun, d10}, 777, 770);
    testOneStrategy(new Dimension[]{dun, dun}, 100, 100);
    testOneStrategy(new Dimension[]{dun, d10, dun}, 100, 90);
    testOneStrategy(new Dimension[]{dun, dun, d2}, 100, 98);
    testOneStrategy(new Dimension[]{dun, dun, d2}, 400, 392);
    testOneStrategy(new Dimension[]{dun, dun, dun}, 400, 343);
  }

  private void testOneStrategy(Dimension[] shape, int maxChunkElems, long expectSize) {
    List<Dimension> dims = Arrays.asList(shape);
    show("shape", dims);
    System.out.printf(" max = %d%n", maxChunkElems);
    Nc4ChunkingDefault chunker = new Nc4ChunkingDefault();
    chunker.setDefaultChunkSize(maxChunkElems);
    chunker.setMinChunksize(maxChunkElems);

    int[] result = chunker.computeUnlimitedChunking(dims, 1);
    show("chunk", result);
    long shapeSize = new Section(result).computeSize();
    System.out.printf(" size = %d%n%n", shapeSize);
    assert shapeSize <= maxChunkElems;
    assert shapeSize >= maxChunkElems/2;
    assert shapeSize == expectSize : shapeSize +" != "+ expectSize;
  }

  private void show(String what, List<Dimension> dims) {
    System.out.printf("%s= (", what);
    for (Dimension r : dims) System.out.printf("%d,", r.getLength());
    System.out.printf(")%n");
  }

  @Test
  public void testChunkingRealStrategy() {
    Dimension d2 = new Dimension("2", 2);
    Dimension d10 = new Dimension("10", 10);
    Dimension d20 = new Dimension("20", 20);
    Dimension dun = new Dimension("u", 0, true, true, false);

    testRealStrategy(new Dimension[]{dun}, 8);
  }

  private void testRealStrategy(Dimension[] shape, int elemSize) {
    List<Dimension> dims = Arrays.asList(shape);
    show("shape", dims);
    System.out.printf(" elemSize = %d%n", elemSize);
    Nc4ChunkingDefault chunker = new Nc4ChunkingDefault();

    int[] result = chunker.computeUnlimitedChunking(dims, elemSize);
    show("chunk", result);
    long shapeSize = new Section(result).computeSize();
    System.out.printf(" size = %d%n%n", shapeSize);
    int expectSize = chunker.getMinChunksize() / elemSize;
    assert shapeSize == expectSize : shapeSize +" != "+ expectSize;
  }



}
