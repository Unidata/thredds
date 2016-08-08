package ucar.nc2.iosp.hdf5;

import ucar.ma2.Section;
import ucar.nc2.Variable;
import ucar.nc2.iosp.LayoutTiled;
import ucar.nc2.util.Misc;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This holds the chunked data storage.
 * level 1A
 * A B-tree, version 1, used for data (node type 1)
 *
 * Version 1 B-trees in HDF5 files an implementation of the B-link tree, in which the sibling nodes at a particular level
 * in the tree are stored in a doubly-linked list
 * The B-link trees implemented by the file format contain one more key than the number of children.
 * In other words, each child pointer out of a B-tree node has a left key and a right key.
 * The pointers out of internal nodes point to sub-trees while the pointers out of leaf nodes point to symbol nodes and
 * raw data chunks. Aside from that difference, internal nodes and leaf nodes are identical.
 *
 * @see "http://www.hdfgroup.org/HDF5/doc/H5.format.html#Btrees"
 * @author caron
 * @since 6/27/12
 */
public class DataBTree {
  private static final boolean debugDataBtree = false;
  private static final boolean debugDataChunk = false;
  private static final boolean debugChunkOrder = false;
  private static java.io.PrintStream debugOut = System.out;

  private final H5header h5;
  private final MemTracker memTracker;

  private final long rootNodeAddress;
  private final Tiling tiling;
  private final int ndimStorage, wantType;

  private Variable owner;

  DataBTree(H5header h5, long rootNodeAddress, int[] varShape, int[] storageSize, MemTracker memTracker) throws IOException {
    this.h5 = h5;
    this.rootNodeAddress = rootNodeAddress;
    this.tiling = new Tiling(varShape, storageSize);
    this.ndimStorage = storageSize.length;
    this.memTracker = memTracker;

    wantType = 1;
  }

  void setOwner(Variable owner) {
    this.owner = owner;
  }

  // used by H5tiledLayoutBB
  DataChunkIterator getDataChunkIteratorFilter(Section want) throws IOException {
    return new DataChunkIterator(want);
  }

  // used by H5tiledLayout
  LayoutTiled.DataChunkIterator getDataChunkIteratorNoFilter(Section want, int nChunkDim) throws IOException {
    /*
    if (if (debugChunkOrder) ) {
    DataChunkIteratorNoFilter iter = new DataChunkIteratorNoFilter(null, nChunkDim);
    int count = 0;
    int last = -1;
    while (iter.hasNext()) {
      LayoutTiled.DataChunk chunk = iter.next();
      System.out.printf("%d : %d%n", count++, tiling.order(chunk.offset));
      if (tiling.order(chunk.offset) <= last)
        System.out.println("HEY");
      last = tiling.order(chunk.offset);
    }
    }*/

    return new DataChunkIteratorNoFilter(want, nChunkDim);
  }

  // An Iterator over the DataChunks in the btree.
  // returns the actual data from the btree leaf (level 0) nodes.
  // used by H5tiledLayout, when there are no filters
  class DataChunkIteratorNoFilter implements LayoutTiled.DataChunkIterator {
    private Node root;
    private int nChunkDim;

    /**
     * Constructor
     *
     * @param want      skip any nodes that are before this section
     * @param nChunkDim number of chunk dimensions - may be less than the offset[] length
     * @throws IOException on error
     */
    DataChunkIteratorNoFilter(Section want, int nChunkDim) throws IOException {
      this.nChunkDim = nChunkDim;
      root = new Node(rootNodeAddress, -1); // should we cache the nodes ???
      int[] wantOrigin = (want != null) ? want.getOrigin() : null;
      root.first(wantOrigin);
    }

    public boolean hasNext() {
      return root.hasNext(); //  && !node.greaterThan(wantOrigin);
    }

    public LayoutTiled.DataChunk next() throws IOException {
      DataChunk dc = root.next();
      int[] offset = dc.offset;
      if (offset.length > nChunkDim) { // may have to eliminate last offset
        offset = new int[nChunkDim];
        System.arraycopy(dc.offset, 0, offset, 0, nChunkDim);
      }
      if (debugChunkOrder) System.out.printf("LayoutTiled.DataChunk next order %d%n", tiling.order(dc.offset));

      return new LayoutTiled.DataChunk(offset, dc.filePos);
    }
  }

  // An Iterator over the DataChunks in the btree.
  // returns the data chunck info from the btree leaf (level 0) nodes
  // used by H5tiledLayoutBB, when there are filters
  class DataChunkIterator {
    private Node root;
    private int[] wantOrigin;

    /**
     * Constructor
     *
     * @param want skip any nodes that are before this section
     * @throws IOException on error
     */
    DataChunkIterator(Section want) throws IOException {
      root = new Node(rootNodeAddress, -1); // should we cache the nodes ???
      wantOrigin = (want != null) ? want.getOrigin() : null;
      root.first(wantOrigin);
    }

    public boolean hasNext() {
      return root.hasNext(); //  && !node.greaterThan(wantOrigin);
    }

    public DataChunk next() throws IOException {
      return root.next();
    }
  }

  // Btree nodes
  class Node {
    private long address;
    private int level, nentries;
    private Node currentNode;

    // level 0 only
    private List<DataChunk> myEntries;
    // level > 0 only
    private int[][] offset; // int[nentries][ndim]; // other levels

    // "For raw data chunk nodes, the child pointer is the address of a single raw data chunk"
    private long[] childPointer; // long[nentries];

    private int currentEntry; // track iteration; LOOK this seems fishy - why not an iterator ??

    Node(long address, long parent) throws IOException {
      if (debugDataBtree) debugOut.println("\n--> DataBTree read tree at address=" + address + " parent= " + parent +
              " owner= " + owner.getNameAndDimensions());

      h5.raf.order(RandomAccessFile.LITTLE_ENDIAN); // header information is in le byte order
      h5.raf.seek( h5.getFileOffset(address));
      this.address = address;

      String magic = h5.raf.readString(4);
      if (!magic.equals("TREE"))
        throw new IllegalStateException("DataBTree doesnt start with TREE");

      int type = h5.raf.readByte();
      level = h5.raf.readByte();
      nentries = h5.raf.readShort();
      if (type != wantType)
        throw new IllegalStateException("DataBTree must be type " + wantType);

      long size = 8 + 2 * h5.getSizeOffsets() + ((long)nentries) * (8 + h5.getSizeOffsets() + 8 + ndimStorage);
      if (memTracker != null) memTracker.addByLen("Data BTree (" + owner + ")", address, size);
      if (debugDataBtree) debugOut.println("    type=" + type + " level=" + level + " nentries=" + nentries + " size = " + size);

      long leftAddress = h5.readOffset();
      long rightAddress = h5.readOffset();
      if (debugDataBtree) debugOut.println("    leftAddress=" + leftAddress + " =0x" + Long.toHexString(leftAddress) +
                " rightAddress=" + rightAddress + " =0x" + Long.toHexString(rightAddress));

      if (level == 0) {
        // read all entries as a DataChunk
        myEntries = new ArrayList<DataChunk>();
        for (int i = 0; i <= nentries; i++) {
          DataChunk dc = new DataChunk(ndimStorage, (i == nentries));
          myEntries.add(dc);
          if (debugDataChunk) debugOut.println(dc);
        }
      } else { // just track the offsets and node addresses
        offset = new int[nentries + 1][ndimStorage];
        childPointer = new long[nentries + 1];
        for (int i = 0; i <= nentries; i++) {
          h5.raf.skipBytes(8); // skip size, filterMask
          for (int j = 0; j < ndimStorage; j++) {
            long loffset = h5.raf.readLong();
            assert loffset < Integer.MAX_VALUE;
            offset[i][j] = (int) loffset;
          }
          this.childPointer[i] = (i == nentries) ? -1 : h5.readOffset();
          if (debugDataBtree) {
            debugOut.print("    childPointer=" + childPointer[i] + " =0x" + Long.toHexString(childPointer[i]));
            for (long anOffset : offset[i]) debugOut.print(" " + anOffset);
            debugOut.println();
          }
        }
      }
    }

    // this finds the first entry we dont want to skip.
    // entry i goes from [offset(i),offset(i+1))
    // we want to skip any entries we dont need, namely those where want >= offset(i+1)
    // so keep skipping until want < offset(i+1)
    void first(int[] wantOrigin) throws IOException {
      if (debugChunkOrder && wantOrigin != null) System.out.printf("Level %d: Tile want %d%n", level, tiling.order(wantOrigin));
      if (level == 0) {
        currentEntry = 0;
        // note nentries-1 - assume dont skip the last one
       for (currentEntry = 0; currentEntry < nentries-1; currentEntry++) {
         DataChunk entry = myEntries.get(currentEntry + 1); // look at the next one
         if (debugChunkOrder) System.out.printf(" Entry=%d: Tile ending order= %d%n", currentEntry, tiling.order(entry.offset));
         if ((wantOrigin == null) || tiling.compare(wantOrigin, entry.offset) < 0) break;
       }
        if (debugChunkOrder) System.out.printf("Level %d use entry= %d%n", level, currentEntry);

      } else {
        currentNode = null;
        for (currentEntry = 0; currentEntry < nentries; currentEntry++) {
          if (debugChunkOrder) System.out.printf(" Entry=%3d offset [%-15s]: Tile order %d-%d%n", currentEntry,
                  Misc.showInts(offset[currentEntry]),
                  tiling.order(offset[currentEntry]), tiling.order(offset[currentEntry + 1]));
          if ((wantOrigin == null) || tiling.compare(wantOrigin, offset[currentEntry + 1]) < 0) {
            currentNode = new Node(childPointer[currentEntry], this.address);
            if (debugChunkOrder) System.out.printf("Level %d use entry= %d%n", level, currentEntry);
            currentNode.first(wantOrigin);
            break;
          }
        }

        // heres the case where its the last entry we want; the tiling.compare() above may fail
        if (currentNode == null) {
          currentEntry = nentries - 1;
          currentNode = new Node(childPointer[currentEntry], this.address);
          currentNode.first(wantOrigin);
        }
      }

      //if (currentEntry >= nentries)
      //  System.out.println("hah");
      assert (nentries == 0) || (currentEntry < nentries) : currentEntry + " >= " + nentries;
    }

    // LOOK - wouldnt be a bad idea to terminate if possible instead of running through all subsequent entries
    boolean hasNext() {
      if (level == 0) {
        return currentEntry < nentries;

      } else {
        if (currentNode.hasNext()) return true;
        return currentEntry < nentries - 1;
      }
    }

    DataChunk next() throws IOException {
      if (level == 0) {
        return myEntries.get(currentEntry++);

      } else {
        if (currentNode.hasNext())
          return currentNode.next();

        currentEntry++;
        currentNode = new Node(childPointer[currentEntry], this.address);
        currentNode.first(null);
        return currentNode.next();
      }
    }
  }

  /* private void dump(DataType dt, List<DataChunk> entries) {
   try {
     for (DataChunk node : entries) {
       if (dt == DataType.STRING) {
         HeapIdentifier heapId = new HeapIdentifier(node.address);
         GlobalHeap.HeapObject ho = heapId.getHeapObject();
         byte[] pa = new byte[(int) ho.dataSize];
         raf.seek(ho.dataPos);
         raf.read(pa);
         debugOut.println(" data at " + ho.dataPos + " = " + new String(pa));
       }
     }
   }
   catch (IOException e) {
     e.printStackTrace();
   }
 } */

  // these are part of the level 1A data structure, type 1
  // see http://www.hdfgroup.org/HDF5/doc/H5.format.html#V1Btrees,
  // see "Key" field (type 1) p 10
  // this is only for leaf nodes (level 0)
  class DataChunk {
    int size;       // size of chunk in bytes; need storage layout dimensions to interpret
    int filterMask; // bitfield indicating which filters have been skipped for this chunk
    int[] offset;   // offset index of this chunk, reletive to entire array
    long filePos;   // filePos of a single raw data chunk, already shifted by the offset if needed

    DataChunk(int ndim, boolean last) throws IOException {
      this.size = h5.raf.readInt();
      this.filterMask = h5.raf.readInt();
      offset = new int[ndim];
      for (int i = 0; i < ndim; i++) {
        long loffset = h5.raf.readLong();
        assert loffset < Integer.MAX_VALUE;
        offset[i] = (int) loffset;
      }
      this.filePos = last ? -1 : h5.readAddress(); //
      if (memTracker != null) memTracker.addByLen("Chunked Data (" + owner + ")", filePos, size);
    }

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("  ChunkedDataNode size=").append(size).append(" filterMask=").append(filterMask).append(" filePos=").append(filePos).append(" offsets= ");
      for (long anOffset : offset) sbuff.append(anOffset).append(" ");
      return sbuff.toString();
    }
  }

}
