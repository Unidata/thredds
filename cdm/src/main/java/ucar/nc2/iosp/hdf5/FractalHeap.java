package ucar.nc2.iosp.hdf5;

import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.SpecialMathFunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * HDF5 fractal heaps
 *
 * @author caron
 * @since 6/27/12
 */
public class FractalHeap {

    // level 1E "Fractal Heap" used for both Global and Local heaps in 1.8.0+
  /*
  1) the root indirect block knows how many rows it has from the header, which i can divide into
direct and indirect using:

 int maxrows_directBlocks = (log2(maxDirectBlockSize) - log2(startingBlockSize)) + 2;

in the example file i have, maxDirectBlockSize = 216, startingBlockSize = 2^10, tableWidth = 4, so
maxrows = 8. So I will see 8 rows, with direct sizes:
	2^10, 2^10, 2^11, 2^12, 2^13, 2^14, 2^15, 2^16

So if nrows > 8, I will see indirect rows of size
	2^17, 2^18, .....

this value is the <indirect block size>.

2) now read a 1st level indirect block of size 217:

<iblock_nrows> = lg2(<indirect block size>) - lg2(<starting block size) - lg2(<doubling_table_width>)) + 1

<iblock_nrows> = 17 - 10 - 2 + 1 = 6.

 All indirect blocks of "size" 2^17 will have: (for the parameters above)
        row 0: (direct blocks): 4 x 2^10 = 2^12
        row 1: (direct blocks): 4 x 2^10 = 2^12
        row 2: (direct blocks): 4 x 2^11 = 2^13
        row 3: (direct blocks): 4 x 2^12 = 2^14
        row 4: (direct blocks): 4 x 2^13 = 2^15
        row 5: (direct blocks): 4 x 2^14 = 2^16
                    ===============
                       Total size: 2^17

Then there are 7 rows for indirect block of size 218, 8 rows for indirect block of size 219, etc.
An indirect block of size 2^20 will have nine rows, the last one of which are indirect blocks that are size 2^17,
an indirect block of size 2^21 will have ten rows, the last two rows of which are indirect blocks that are size
2^17 & 2^18, etc.

One still uses

 int maxrows_directBlocks = (log2(maxDirectBlockSize) - log2(startingBlockSize)) + 2

Where startingBlockSize is from the header, ie the same for all indirect blocks.


*/

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FractalHeap.class);

  private java.io.PrintStream debugOut = System.out;
  private boolean debugDetail, debugFractalHeap, debugPos;

  public void setMemTracker(MemTracker memTracker) {
    this.memTracker = memTracker;
  }

  private MemTracker memTracker;

  int version;
  short heapIdLen;
  byte flags;
  int maxSizeOfObjects;
  long nextHugeObjectId, freeSpace, managedSpace, allocatedManagedSpace, offsetDirectBlock,
          nManagedObjects, sizeHugeObjects, nHugeObjects, sizeTinyObjects, nTinyObjects;
  long btreeAddress, freeSpaceTrackerAddress;

  short maxHeapSize, startingNumRows, currentNumRows;
  long maxDirectBlockSize;
  short tableWidth;
  long startingBlockSize;

  long rootBlockAddress;
  IndirectBlock rootBlock;

  // filters
  short ioFilterLen;
  long sizeFilteredRootDirectBlock;
  int ioFilterMask;
  byte[] ioFilterInfo;

  DoublingTable doublingTable;

  H5header h5;
  RandomAccessFile raf;

  FractalHeap(H5header h5, String forWho, long address) throws IOException {
    this.h5 = h5;
    this.raf = h5.raf;

    // header information is in le byte order
    raf.order(RandomAccessFile.LITTLE_ENDIAN);
    raf.seek(h5.getFileOffset(address));

    if (debugDetail) debugOut.println("-- readFractalHeap position=" + raf.getFilePointer());

    // header
    byte[] heapname = new byte[4];
    raf.read(heapname);
    String magic = new String(heapname);
    if (!magic.equals("FRHP"))
      throw new IllegalStateException(magic + " should equal FRHP");

    version = raf.readByte();
    heapIdLen = raf.readShort(); // bytes
    ioFilterLen = raf.readShort();  // bytes
    flags = raf.readByte();

    maxSizeOfObjects = raf.readInt(); // greater than this are huge objects
    nextHugeObjectId = h5.readLength(); // next id to use for a huge object
    btreeAddress = h5.readOffset(); // v2 btee to track huge objects
    freeSpace = h5.readLength();  // total free space in managed direct blocks
    freeSpaceTrackerAddress = h5.readOffset();
    managedSpace = h5.readLength(); // total amount of managed space in the heap
    allocatedManagedSpace = h5.readLength(); // total amount of managed space in the heap actually allocated
    offsetDirectBlock = h5.readLength(); // linear heap offset where next direct block should be allocated
    nManagedObjects = h5.readLength();  // number of managed objects in the heap
    sizeHugeObjects = h5.readLength(); // total size of huge objects in the heap (in bytes)
    nHugeObjects = h5.readLength(); // number huge objects in the heap
    sizeTinyObjects = h5.readLength(); // total size of tiny objects packed in heap Ids (in bytes)
    nTinyObjects = h5.readLength(); // number of tiny objects packed in heap Ids

    tableWidth = raf.readShort(); // number of columns in the doubling table for managed blocks, must be power of 2
    startingBlockSize = h5.readLength(); // starting direct block size in bytes, must be power of 2
    maxDirectBlockSize = h5.readLength(); // maximum direct block size in bytes, must be power of 2
    maxHeapSize = raf.readShort(); // log2 of the maximum size of heap's linear address space, in bytes
    startingNumRows = raf.readShort(); // starting number of rows of the root indirect block, 0 = maximum needed
    rootBlockAddress = h5.readOffset(); // can be undefined if no data
    currentNumRows = raf.readShort(); // current number of rows of the root indirect block, 0 = direct block

    boolean hasFilters = (ioFilterLen > 0);
    if (hasFilters) {
      sizeFilteredRootDirectBlock = h5.readLength();
      ioFilterMask = raf.readInt();
      ioFilterInfo = new byte[ioFilterLen];
      raf.read(ioFilterInfo);
    }
    int checksum = raf.readInt();

    if (debugDetail || debugFractalHeap) {
      debugOut.println("FractalHeap for " + forWho + " version=" + version + " heapIdLen=" + heapIdLen + " ioFilterLen=" + ioFilterLen + " flags= " + flags);
      debugOut.println(" maxSizeOfObjects=" + maxSizeOfObjects + " nextHugeObjectId=" + nextHugeObjectId + " btreeAddress="
              + btreeAddress + " managedSpace=" + managedSpace + " allocatedManagedSpace=" + allocatedManagedSpace + " freeSpace=" + freeSpace);
      debugOut.println(" nManagedObjects=" + nManagedObjects + " nHugeObjects= " + nHugeObjects + " nTinyObjects=" + nTinyObjects +
              " maxDirectBlockSize=" + maxDirectBlockSize + " maxHeapSize= 2^" + maxHeapSize);
      debugOut.println(" DoublingTable: tableWidth=" + tableWidth + " startingBlockSize=" + startingBlockSize);
      debugOut.println(" rootBlockAddress=" + rootBlockAddress + " startingNumRows=" + startingNumRows + " currentNumRows=" + currentNumRows);
    }
    if (debugPos) debugOut.println("    *now at position=" + raf.getFilePointer());

    long pos = raf.getFilePointer();
    if (debugDetail) debugOut.println("-- end FractalHeap position=" + raf.getFilePointer());
    int hsize = 8 + 2 * h5.sizeLengths + h5.sizeOffsets;
    if (memTracker != null) memTracker.add("Group FractalHeap (" + forWho + ")", address, pos);

    doublingTable = new DoublingTable(tableWidth, startingBlockSize, allocatedManagedSpace, maxDirectBlockSize);

    // data
    rootBlock = new IndirectBlock(currentNumRows, startingBlockSize);

    if (currentNumRows == 0) {
      DataBlock dblock = new DataBlock();
      doublingTable.blockList.add(dblock);
      readDirectBlock(h5.getFileOffset(rootBlockAddress), address, dblock);
      dblock.size = startingBlockSize; // - dblock.extraBytes;  LOOK scary bug - retrofit to 4.3
      rootBlock.add(dblock);

    } else {

      readIndirectBlock(rootBlock, h5.getFileOffset(rootBlockAddress), address, hasFilters);

      // read in the direct blocks
      for (DataBlock dblock : doublingTable.blockList) {
        if (dblock.address > 0) {
          readDirectBlock(h5.getFileOffset(dblock.address), address, dblock);
          // dblock.size -= dblock.extraBytes;  LOOK scary bug - retrofit to 4.3
        }
      }
    }

  }

  void showDetails(Formatter f) {
    f.format("FractalHeap version=" + version + " heapIdLen=" + heapIdLen + " ioFilterLen=" + ioFilterLen + " flags= " + flags + "\n");
    f.format(" maxSizeOfObjects=" + maxSizeOfObjects + " nextHugeObjectId=" + nextHugeObjectId + " btreeAddress="
            + btreeAddress + " managedSpace=" + managedSpace + " allocatedManagedSpace=" + allocatedManagedSpace + " freeSpace=" + freeSpace + "\n");
    f.format(" nManagedObjects=" + nManagedObjects + " nHugeObjects= " + nHugeObjects + " nTinyObjects=" + nTinyObjects +
            " maxDirectBlockSize=" + maxDirectBlockSize + " maxHeapSize= 2^" + maxHeapSize + "\n");
    f.format(" rootBlockAddress=" + rootBlockAddress + " startingNumRows=" + startingNumRows + " currentNumRows=" + currentNumRows + "\n\n");
    rootBlock.showDetails(f);
    // doublingTable.showDetails(f);
  }


  DHeapId getHeapId(byte[] heapId) throws IOException {
    return new DHeapId(heapId);
  }

  class DHeapId {
    int type;
    int n, m;
    int offset;
    int size;

    DHeapId(byte[] heapId) throws IOException {
      type = (heapId[0] & 0x30) >> 4;
      n = maxHeapSize / 8;
      m = h5.getNumBytesFromMax(maxDirectBlockSize - 1);

      offset = h5.makeIntFromBytes(heapId, 1, n);
      size = h5.makeIntFromBytes(heapId, 1 + n, m);
      // System.out.println("Heap id =" + showBytes(heapId) + " type = " + type + " n= " + n + " m= " + m + " offset= " + offset + " size= " + size);
    }

    long getPos() {
      return doublingTable.getPos(offset);
    }

    public String toString() {
      return type + " " + n + " " + m + " " + offset + " " + size;
    }
  }

  private class DoublingTable {
    int tableWidth;
    long startingBlockSize, managedSpace, maxDirectBlockSize;
    // int nrows, nDirectRows, nIndirectRows;
    List<DataBlock> blockList;

    DoublingTable(int tableWidth, long startingBlockSize, long managedSpace, long maxDirectBlockSize) {
      this.tableWidth = tableWidth;
      this.startingBlockSize = startingBlockSize;
      this.managedSpace = managedSpace;
      this.maxDirectBlockSize = maxDirectBlockSize;

      /* nrows = calcNrows(managedSpace);
     int maxDirectRows = calcNrows(maxDirectBlockSize);
     if (nrows > maxDirectRows) {
       nDirectRows = maxDirectRows;
       nIndirectRows = nrows - maxDirectRows;
     } else {
       nDirectRows = nrows;
       nIndirectRows = 0;
     } */

      blockList = new ArrayList<DataBlock>(tableWidth * currentNumRows);
    }

    private int calcNrows(long max) {
      int n = 0;
      long sizeInBytes = 0;
      long blockSize = startingBlockSize;
      while (sizeInBytes < max) {
        sizeInBytes += blockSize * tableWidth;
        n++;
        if (n > 1) blockSize *= 2;
      }
      return n;
    }

    private void assignSizes() {
      int block = 0;
      long blockSize = startingBlockSize;
      for (DataBlock db : blockList) {
        db.size = blockSize;
        block++;
        if ((block % tableWidth == 0) && (block / tableWidth > 1))
          blockSize *= 2;
      }
    }

    long getPos(long offset) {
      int block = 0;
      for (DataBlock db : blockList) {
        if (db.address < 0) continue;
        if ((offset >= db.offset) && (offset <= db.offset + db.size)) {
          long localOffset = offset - db.offset;
          //System.out.println("   heap ID find block= "+block+" db.dataPos " + db.dataPos+" localOffset= "+localOffset);
          return db.dataPos + localOffset;
        }
        block++;
      }

      log.error("DoublingTable: illegal offset=" + offset);
      return -1; // LOOK temporary skip
      // throw new IllegalStateException("offset=" + offset);
    }

    void showDetails(Formatter f) {
      f.format(" DoublingTable: tableWidth= %d startingBlockSize = %d managedSpace=%d maxDirectBlockSize=%d%n",
              tableWidth, startingBlockSize, managedSpace, maxDirectBlockSize);
      //sbuff.append(" nrows=" + nrows + " nDirectRows=" + nDirectRows + " nIndirectRows=" + nIndirectRows+"\n");
      f.format(" DataBlocks:\n");
      f.format("  address            dataPos            offset size\n");
      for (DataBlock dblock : blockList) {
        f.format("  %#-18x %#-18x %5d  %4d%n", dblock.address, dblock.dataPos, dblock.offset, dblock.size);
      }
    }
  }

  private class IndirectBlock {
    long size;
    int nrows, directRows, indirectRows;
    List<DataBlock> directBlocks;
    List<IndirectBlock> indirectBlocks;

    IndirectBlock(int nrows, long iblock_size) {
      this.nrows = nrows;
      this.size = iblock_size;

      if (nrows < 0) {
        double n = SpecialMathFunction.log2(iblock_size) - SpecialMathFunction.log2(startingBlockSize * tableWidth) + 1;
        nrows = (int) n;
      }

      int maxrows_directBlocks = (int) (SpecialMathFunction.log2(maxDirectBlockSize) - SpecialMathFunction.log2(startingBlockSize)) + 2;
      if (nrows < maxrows_directBlocks) {
        directRows = nrows;
        indirectRows = 0;
      } else {
        directRows = maxrows_directBlocks;
        indirectRows = (nrows - maxrows_directBlocks);
      }
      if (debugFractalHeap)
        debugOut.println("  readIndirectBlock directChildren" + directRows + " indirectChildren= " + indirectRows);
    }

    void add(DataBlock dblock) {
      if (directBlocks == null)
        directBlocks = new ArrayList<DataBlock>();
      directBlocks.add(dblock);
    }

    void add(IndirectBlock iblock) {
      if (indirectBlocks == null)
        indirectBlocks = new ArrayList<IndirectBlock>();
      indirectBlocks.add(iblock);
    }

    void showDetails(Formatter f) {
      f.format("%n IndirectBlock: nrows= %d directRows = %d indirectRows=%d startingSize=%d%n",
              nrows, directRows, indirectRows, size);
      //sbuff.append(" nrows=" + nrows + " nDirectRows=" + nDirectRows + " nIndirectRows=" + nIndirectRows+"\n");
      f.format(" DataBlocks:\n");
      f.format("  address            dataPos            offset size end\n");
      if (directBlocks != null)
        for (DataBlock dblock : directBlocks)
          f.format("  %#-18x %#-18x %5d  %4d %5d %n", dblock.address, dblock.dataPos, dblock.offset, dblock.size,
                  (dblock.offset + dblock.size));
      if (indirectBlocks != null)
        for (IndirectBlock iblock : indirectBlocks)
          iblock.showDetails(f);
    }
  }

  private class DataBlock {
    long address;
    long sizeFilteredDirectBlock;
    int filterMask;

    long dataPos;
    long offset;
    long size;
    int extraBytes;


    @Override
    public String toString() {
      return "DataBlock{" +
              "offset=" + offset +
              ", size=" + size +
              ", dataPos=" + dataPos +
              '}';
    }
  }

  void readIndirectBlock(IndirectBlock iblock, long pos, long heapAddress, boolean hasFilter) throws IOException {
    raf.seek(pos);

    // header
    byte[] heapname = new byte[4];
    raf.read(heapname);
    String magic = new String(heapname);
    if (!magic.equals("FHIB"))
      throw new IllegalStateException(magic + " should equal FHIB");

    byte version = raf.readByte();
    long heapHeaderAddress = h5.readOffset();
    if (heapAddress != heapHeaderAddress)
      throw new IllegalStateException();

    int nbytes = maxHeapSize / 8;
    if (maxHeapSize % 8 != 0) nbytes++;
    long blockOffset = h5.readVariableSizeUnsigned(nbytes);

    if (debugDetail || debugFractalHeap) {
      debugOut.println(" -- FH IndirectBlock version=" + version + " blockOffset= " + blockOffset);
    }

    long npos = raf.getFilePointer();
    if (debugPos) debugOut.println("    *now at position=" + npos);

    // child direct blocks
    long blockSize = startingBlockSize;
    for (int row = 0; row < iblock.directRows; row++) {

      if (row > 1)
        blockSize *= 2;

      for (int i = 0; i < doublingTable.tableWidth; i++) {
        DataBlock directBlock = new DataBlock();
        iblock.add(directBlock);

        directBlock.address = h5.readOffset();
        if (hasFilter) {
          directBlock.sizeFilteredDirectBlock = h5.readLength();
          directBlock.filterMask = raf.readInt();
        }
        if (debugDetail || debugFractalHeap)
          debugOut.println("  DirectChild " + i + " address= " + directBlock.address);

        directBlock.size = blockSize;

        //if (directChild.address >= 0)
        doublingTable.blockList.add(directBlock);
      }
    }

    // child indirect blocks
    for (int row = 0; row < iblock.indirectRows; row++) {
      blockSize *= 2;
      for (int i = 0; i < doublingTable.tableWidth; i++) {
        IndirectBlock iblock2 = new IndirectBlock(-1, blockSize);
        iblock.add(iblock2);

        long childIndirectAddress = h5.readOffset();
        if (debugDetail || debugFractalHeap)
          debugOut.println("  InDirectChild " + row + " address= " + childIndirectAddress);
        if (childIndirectAddress >= 0)
          readIndirectBlock(iblock2, childIndirectAddress, heapAddress, hasFilter);
      }
    }

  }

  void readDirectBlock(long pos, long heapAddress, DataBlock dblock) throws IOException {
    raf.seek(pos);

    // header
    byte[] heapname = new byte[4];
    raf.read(heapname);
    String magic = new String(heapname);
    if (!magic.equals("FHDB"))
      throw new IllegalStateException(magic + " should equal FHDB");

    byte version = raf.readByte();
    long heapHeaderAddress = h5.readOffset();
    if (heapAddress != heapHeaderAddress)
      throw new IllegalStateException();

    dblock.extraBytes = 5; // keep track of how much room is taken out of blocak size
    dblock.extraBytes += h5.isOffsetLong ? 8 : 4;

    int nbytes = maxHeapSize / 8;
    if (maxHeapSize % 8 != 0) nbytes++;
    dblock.offset = h5.readVariableSizeUnsigned(nbytes);
    dblock.dataPos = pos; // raf.getFilePointer();  // offsets are from the start of the block

    dblock.extraBytes += nbytes;
    if ((flags & 2) != 0) dblock.extraBytes += 4; // ?? size of checksum
    //dblock.size -= size; // subtract space used by other fields

    if (debugDetail || debugFractalHeap)
      debugOut.println("  DirectBlock offset= " + dblock.offset + " dataPos = " + dblock.dataPos);
  }

} // FractalHeap
