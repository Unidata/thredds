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
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FractalHeap.class);

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


  private java.io.PrintStream debugOut = System.out;
  static boolean debugDetail, debugFractalHeap, debugPos;

  private final H5header h5;
  private final RandomAccessFile raf;

  int version;
  short heapIdLen;
  byte flags;
  int maxSizeOfObjects;
  long nextHugeObjectId, freeSpace, managedSpace, allocatedManagedSpace, offsetDirectBlock,
          nManagedObjects, sizeHugeObjects, nHugeObjects, sizeTinyObjects, nTinyObjects;
  long btreeAddressHugeObjects, freeSpaceTrackerAddress;

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
  BTree2 btreeHugeObjects;


  FractalHeap(H5header h5, String forWho, long address, MemTracker memTracker) throws IOException {
    this.h5 = h5;
    this.raf = h5.raf;

    // header information is in le byte order
    raf.order(RandomAccessFile.LITTLE_ENDIAN);
    raf.seek(h5.getFileOffset(address));

    if (debugDetail) debugOut.println("-- readFractalHeap position=" + raf.getFilePointer());

    // header
    String magic = raf.readString(4);
    if (!magic.equals("FRHP"))
      throw new IllegalStateException(magic + " should equal FRHP");

    version = raf.readByte();
    heapIdLen = raf.readShort(); // bytes
    ioFilterLen = raf.readShort();  // bytes
    flags = raf.readByte();

    maxSizeOfObjects = raf.readInt(); // greater than this are huge objects
    nextHugeObjectId = h5.readLength(); // next id to use for a huge object
    btreeAddressHugeObjects = h5.readOffset(); // v2 btee to track huge objects
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
    rootBlockAddress = h5.readOffset(); // This is the address of the root block for the heap.
                                        // It can be the undefined address if there is no data in the heap.
                                        // It either points to a direct block (if the Current # of Rows in the Root Indirect Block value is 0), or an indirect block.
    currentNumRows = raf.readShort(); // current number of rows of the root indirect block, 0 = direct block

    boolean hasFilters = (ioFilterLen > 0);
    if (hasFilters) {
      sizeFilteredRootDirectBlock = h5.readLength();
      ioFilterMask = raf.readInt();
      ioFilterInfo = new byte[ioFilterLen];
      raf.readFully(ioFilterInfo);
    }
    int checksum = raf.readInt();

    if (debugDetail || debugFractalHeap) {
      debugOut.println("FractalHeap for " + forWho + " version=" + version + " heapIdLen=" + heapIdLen + " ioFilterLen=" + ioFilterLen + " flags= " + flags);
      debugOut.println(" maxSizeOfObjects=" + maxSizeOfObjects + " nextHugeObjectId=" + nextHugeObjectId + " btreeAddress="
              + btreeAddressHugeObjects + " managedSpace=" + managedSpace + " allocatedManagedSpace=" + allocatedManagedSpace + " freeSpace=" + freeSpace);
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
      dblock.size = startingBlockSize; // - dblock.extraBytes;  // removed 10/1/2013
      rootBlock.add(dblock);

    } else {

      readIndirectBlock(rootBlock, h5.getFileOffset(rootBlockAddress), address, hasFilters);

      // read in the direct blocks
      for (DataBlock dblock : doublingTable.blockList) {
        if (dblock.address > 0) {
          readDirectBlock(h5.getFileOffset(dblock.address), address, dblock);
          // dblock.size -= dblock.extraBytes;  // removed 10/1/2013
        }
      }
    }

  }

  void showDetails(Formatter f) {
    f.format("FractalHeap version=" + version + " heapIdLen=" + heapIdLen + " ioFilterLen=" + ioFilterLen + " flags= " + flags + "%n");
    f.format(" maxSizeOfObjects=" + maxSizeOfObjects + " nextHugeObjectId=" + nextHugeObjectId + " btreeAddress="
            + btreeAddressHugeObjects + " managedSpace=" + managedSpace + " allocatedManagedSpace=" + allocatedManagedSpace + " freeSpace=" + freeSpace + "%n");
    f.format(" nManagedObjects=" + nManagedObjects + " nHugeObjects= " + nHugeObjects + " nTinyObjects=" + nTinyObjects +
            " maxDirectBlockSize=" + maxDirectBlockSize + " maxHeapSize= 2^" + maxHeapSize + "%n");
    f.format(" rootBlockAddress=" + rootBlockAddress + " startingNumRows=" + startingNumRows + " currentNumRows=" + currentNumRows + "%n%n");
    rootBlock.showDetails(f);
    // doublingTable.showDetails(f);
  }


  DHeapId getFractalHeapId(byte[] heapId) throws IOException {
    return new DHeapId(heapId);
  }

  class DHeapId {
    int type;
    int subtype;  // 1 = indirect no filter, 2 = indirect, filter 3 = direct, no filter, 4 = direct, filter
    int n;        // the offset field size
    int m;
    int offset; // This field is the offset of the object in the heap.
    int size;   // This field is the length of the object in the heap

    DHeapId(byte[] heapId) throws IOException {
      type = (heapId[0] & 0x30) >> 4;

      if (type == 0) {
        n = maxHeapSize / 8;      // This field's size is the minimum number of bytes necessary to encode the Maximum Heap Size value
        m = h5.getNumBytesFromMax(maxDirectBlockSize - 1);  // This field is the length of the object in the heap.
        // It is determined by taking the minimum value of Maximum Direct Block Size and Maximum Size of Managed Objects in the Fractal Heap Header.
        // Again, the minimum number of bytes needed to encode that value is used for the size of this field.

        offset = h5.makeIntFromBytes(heapId, 1, n);
        size = h5.makeIntFromBytes(heapId, 1 + n, m);
      }

      else if (type == 1) {
        // how fun to guess the subtype
        boolean hasBtree = (btreeAddressHugeObjects > 0);
        boolean hasFilters = (ioFilterLen > 0);
        if (hasBtree)
          subtype = hasFilters ? 2 : 1;
        else
          subtype = hasFilters ? 4 : 3;

        switch (subtype) {
          case 1:
            n = h5.getNumBytesFromMax(nManagedObjects);      // guess
            offset = h5.makeIntFromBytes(heapId, 1, n);      // [16,1,0,0,0,0,0,0]
            break;
        }
      } else if (type == 2) {
        /* The sub-type for tiny heap IDs depends on whether the heap ID is large enough to store objects greater than 16 bytes or not.
          If the heap ID length is 18 bytes or smaller, the "normal" tiny heap ID form is used. If the heap ID length is greater than 18 bytes in length,
          the "extented" form is used. */
        subtype = (heapId.length <= 18) ? 1 : 2; // 0 == normal, 1 = extended
      }

      else  {
        throw new UnsupportedOperationException(); // "DHeapId subtype ="+subtype);
      }


    }

    long getPos() throws IOException {
      switch (type) {
        case 0:
          return doublingTable.getPos(offset);
        case 1: {
          switch (subtype) {
            case 1:
            case 2:
              if (btreeHugeObjects == null) {
                btreeHugeObjects = new BTree2(h5, "FractalHeap btreeHugeObjects", btreeAddressHugeObjects);
                assert btreeHugeObjects.btreeType == subtype;
              }
              BTree2.Record1 record1 = btreeHugeObjects.getEntry1(offset);
              if (record1 == null) {
                btreeHugeObjects.getEntry1(offset); // debug
                throw new RuntimeException("Cant find DHeapId="+offset);
              }
              return record1.hugeObjectAddress;

            case 3:
            case 4:
              return offset;     // guess
          }
        }
        default:
          throw new RuntimeException("Unknown DHeapId type ="+type);
      }
    }

    public String toString() {
      return type + "," + n + "," + m + "," + offset + "," + size;
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
      this.blockList = new ArrayList<>(tableWidth * currentNumRows);
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
      //return -1; // temporary skip
      throw new IllegalStateException("offset=" + offset);
    }

    void showDetails(Formatter f) {
      f.format(" DoublingTable: tableWidth= %d startingBlockSize = %d managedSpace=%d maxDirectBlockSize=%d%n",
              tableWidth, startingBlockSize, managedSpace, maxDirectBlockSize);
      //sbuff.append(" nrows=" + nrows + " nDirectRows=" + nDirectRows + " nIndirectRows=" + nIndirectRows+"%n");
      f.format(" DataBlocks:%n");
      f.format("  address            dataPos            offset size%n");
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
        directBlocks = new ArrayList<>();
      directBlocks.add(dblock);
    }

    void add(IndirectBlock iblock) {
      if (indirectBlocks == null)
        indirectBlocks = new ArrayList<>();
      indirectBlocks.add(iblock);
    }

    void showDetails(Formatter f) {
      f.format("%n IndirectBlock: nrows= %d directRows = %d indirectRows=%d startingSize=%d%n",
              nrows, directRows, indirectRows, size);
      //sbuff.append(" nrows=" + nrows + " nDirectRows=" + nDirectRows + " nIndirectRows=" + nIndirectRows+"%n");
      f.format(" DataBlocks:%n");
      f.format("  address            dataPos            offset size end%n");
      if (directBlocks != null)
        for (DataBlock dblock : directBlocks)
          f.format("  %#-18x %#-18x %5d  %4d %5d %n", dblock.address, dblock.dataPos, dblock.offset, dblock.size,
                  (dblock.offset + dblock.size));
      if (indirectBlocks != null)
        for (IndirectBlock iblock : indirectBlocks)
          iblock.showDetails(f);
    }
  }

  private static class DataBlock {
    long address;
    long sizeFilteredDirectBlock;
    int filterMask;

    long dataPos;
    long offset;
    long size;
    int extraBytes;
    boolean wasRead; // when empty, object exists, but fields are not init. not yet sure where to use.

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
    String magic = raf.readString(4);
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

        directBlock.address = h5.readOffset();  // This field is the address of the child direct block. The size of the [uncompressed] direct block can be computed by its offset in the heap's linear address space.
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
    if (pos < 0) return; // means its empty
    raf.seek(pos);

    // header
    String magic = raf.readString(4);
    if (!magic.equals("FHDB"))
      throw new IllegalStateException(magic + " should equal FHDB");

    byte version = raf.readByte();
    long heapHeaderAddress = h5.readOffset(); // This is the address for the fractal heap header that this block belongs to. This field is principally used for file integrity checking.
    if (heapAddress != heapHeaderAddress)
      throw new IllegalStateException();

    dblock.extraBytes = 5; // keep track of how much room is taken out of block size, that is, how much is left for the object
    dblock.extraBytes += h5.isOffsetLong ? 8 : 4;

    int nbytes = maxHeapSize / 8;
    if (maxHeapSize % 8 != 0) nbytes++;
    dblock.offset = h5.readVariableSizeUnsigned(nbytes); // This is the offset of the block within the fractal heap's address space (in bytes).
    dblock.dataPos = pos; // raf.getFilePointer();  // offsets are from the start of the block

    dblock.extraBytes += nbytes;
    if ((flags & 2) != 0) dblock.extraBytes += 4; // ?? size of checksum
    //dblock.size -= size; // subtract space used by other fields

    dblock.wasRead = true;
    if (debugDetail || debugFractalHeap)
      debugOut.println("  DirectBlock offset= " + dblock.offset + " dataPos = " + dblock.dataPos);
  }

} // FractalHeap
