package ucar.nc2.iosp.hdf5;

import ucar.nc2.constants.CDM;
import ucar.nc2.util.Misc;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * // Level 1A2
 *
 * These are used for symbols, not data i think. Version 1 is H5header.GroupBTree.
 *
 * Version 2 B-trees are "traditional" B-trees, with one major difference. Instead of just using a simple pointer
 * (or address in the file) to a child of an internal node, the pointer to the child node contains two additional
 * pieces of information: the number of records in the child node itself, and the total number of records in the child
 * node and all its descendents. Storing this additional information allows fast array-like indexing to locate the n'th
 * record in the B-tree.

 The entry into a version 2 B-tree is a header which contains global information about the structure of the B-tree.
 The root node address field in the header points to the B-tree root node, which is either an internal or leaf node,
 depending on the value in the header's depth field. An internal node consists of records plus pointers to further leaf
 or internal nodes in the tree. A leaf node consists of solely of records. The format of the records depends on the
 B-tree type (stored in the header).

 *
 * @author caron
 * @since 6/27/12
 */
public class BTree2 {
  private boolean debugBtree2 = false, debugPos = false;
  private java.io.PrintStream debugOut = System.out;

  byte btreeType;
  private int nodeSize; // size in bytes of btree nodes
  private short recordSize; // size in bytes of btree records

  private String owner;
  private H5header h5;
  private RandomAccessFile raf;

  List<Entry2> entryList = new ArrayList<>();

  BTree2(H5header h5, String owner, long address) throws IOException {
    this.h5 = h5;
    this.raf = h5.raf;
    this.owner = owner;

    raf.seek(h5.getFileOffset(address));

    // header
    byte[] heapname = new byte[4];
    raf.readFully(heapname);
    String magic = new String(heapname, CDM.utf8Charset);
    if (!magic.equals("BTHD"))
      throw new IllegalStateException(magic + " should equal BTHD");

    byte version = raf.readByte();
    btreeType = raf.readByte();
    nodeSize = raf.readInt();
    recordSize = raf.readShort();
    short treeDepth = raf.readShort();
    byte split = raf.readByte();
    byte merge = raf.readByte();
    long rootNodeAddress = h5.readOffset();
    short numRecordsRootNode = raf.readShort();
    long totalRecords = h5.readLength(); // total in entire btree
    int checksum = raf.readInt();

    if (debugBtree2) {
      debugOut.printf("BTree2 (%s) version=%d type=%d treeDepth=%d nodeSize=%d recordSize=%d numRecordsRootNode=%d totalRecords=%d rootNodeAddress=%d%n",
              owner, version, btreeType, treeDepth, nodeSize, recordSize, numRecordsRootNode, totalRecords, rootNodeAddress);
    }

    if (treeDepth > 0) {
      InternalNode node = new InternalNode(rootNodeAddress, numRecordsRootNode, recordSize, treeDepth);
      node.recurse();
    } else {
      LeafNode leaf = new LeafNode(rootNodeAddress, numRecordsRootNode);
      leaf.addEntries(entryList);
    }
  }

  BTree2.Record1 getEntry1(int hugeObjectID) {
    for (Entry2 entry : entryList) {
      BTree2.Record1 record1 = (BTree2.Record1) entry.record;
      if (record1.hugeObjectID == hugeObjectID) return record1;
    }
    return null;
  }

  // these are part of the level 1A data structure, type = 0
  static class Entry2 {
    long childAddress, nrecords, totNrecords;
    Object record;
  }

  class InternalNode {
    Entry2[] entries;
    int depth;

    InternalNode(long address, short nrecords, short recordSize, int depth) throws IOException {
      this.depth = depth;
      raf.seek(h5.getFileOffset(address));

      if (debugPos) debugOut.println("--Btree2 InternalNode position=" + raf.getFilePointer());

      // header
      byte[] sig = new byte[4];
      raf.readFully(sig);
      String magic = new String(sig, CDM.utf8Charset);
      if (!magic.equals("BTIN"))
        throw new IllegalStateException(magic + " should equal BTIN");

      byte version = raf.readByte();
      byte nodeType = raf.readByte();
      if (nodeType != btreeType)
        throw new IllegalStateException();

      if (debugBtree2)
        debugOut.println("   BTree2 InternalNode version=" + version + " type=" + nodeType + " nrecords=" + nrecords);

      entries = new Entry2[nrecords + 1]; // did i mention theres actually n+1 children?
      for (int i = 0; i < nrecords; i++) {
        entries[i] = new Entry2();
        entries[i].record = readRecord(btreeType);
      }
      entries[nrecords] = new Entry2();

      int maxNumRecords = nodeSize / recordSize; // LOOK ?? guessing
      int maxNumRecordsPlusDesc = nodeSize / recordSize; // LOOK ?? guessing
      for (int i = 0; i < nrecords + 1; i++) {
        Entry2 e = entries[i];
        e.childAddress = h5.readOffset();
        e.nrecords = h5.readVariableSizeUnsigned(1); // readVariableSizeMax(maxNumRecords);
        if (depth > 1)
          e.totNrecords = h5.readVariableSizeUnsigned(2); // readVariableSizeMax(maxNumRecordsPlusDesc);

        if (debugBtree2)
          debugOut.println(" BTree2 entry childAddress=" + e.childAddress + " nrecords=" + e.nrecords + " totNrecords=" + e.totNrecords);
      }

      // skip
      raf.readInt();
    }

    void recurse() throws IOException {
      for (Entry2 e : entries) {
        if (depth > 1) {
          InternalNode node = new InternalNode(e.childAddress, (short) e.nrecords, recordSize, depth - 1);
          node.recurse();
        } else {
          long nrecs = e.nrecords;
          LeafNode leaf = new LeafNode(e.childAddress, (short) nrecs);
          leaf.addEntries(entryList);
        }
        if (e.record != null) // last one is null
          entryList.add(e);
      }
    }
  }

  class LeafNode {
    Entry2[] entries;

    LeafNode(long address, short nrecords) throws IOException {
      raf.seek(h5.getFileOffset(address));

      if (debugPos) debugOut.println("--Btree2 InternalNode position=" + raf.getFilePointer());

      // header
      byte[] sig = new byte[4];
      raf.readFully(sig);
      String magic = new String(sig, CDM.utf8Charset);
      if (!magic.equals("BTLF"))
        throw new IllegalStateException(magic + " should equal BTLF");

      byte version = raf.readByte();
      byte nodeType = raf.readByte();
      if (nodeType != btreeType)
        throw new IllegalStateException();

      if (debugBtree2)
        debugOut.println("   BTree2 LeafNode version=" + version + " type=" + nodeType + " nrecords=" + nrecords);

      entries = new Entry2[nrecords];
      for (int i = 0; i < nrecords; i++) {
        entries[i] = new Entry2();
        entries[i].record = readRecord(btreeType);
      }

      // skip
      raf.readInt();
    }

    void addEntries(List<Entry2> list) {
      Collections.addAll(list, entries);
    }
  }

  Object readRecord(int type) throws IOException {
    switch (type) {
      case 1:
        return new Record1();
      case 2:
        return new Record2();
      case 3:
        return new Record3();
      case 4:
        return new Record4();
      case 5:
        return new Record5();
      case 6:
        return new Record6();
      case 7: {
        return new Record70();  // LOOK wrong
      }
      case 8:
        return new Record8();
      case 9:
        return new Record9();
      default:
        throw new IllegalStateException();
    }
  }

  class Record1 {
    long hugeObjectAddress, hugeObjectLength, hugeObjectID;

    Record1() throws IOException {
      hugeObjectAddress = h5.readOffset();
      hugeObjectLength = h5.readLength();
      hugeObjectID = h5.readLength();
    }
  }

  class Record2 {
    long hugeObjectAddress, hugeObjectLength, hugeObjectID, hugeObjectSize;
    int filterMask;

    Record2() throws IOException {
      hugeObjectAddress = h5.readOffset();
      hugeObjectLength = h5.readLength();
      filterMask = raf.readInt();
      hugeObjectSize = h5.readLength();
      hugeObjectID = h5.readLength();
    }
  }

  class Record3 {
    long hugeObjectAddress, hugeObjectLength;

    Record3() throws IOException {
      hugeObjectAddress = h5.readOffset();
      hugeObjectLength = h5.readLength();
    }
  }

  class Record4 {
    long hugeObjectAddress, hugeObjectLength, hugeObjectID, hugeObjectSize;
    int filterMask;

    Record4() throws IOException {
      hugeObjectAddress = h5.readOffset();
      hugeObjectLength = h5.readLength();
      filterMask = raf.readInt();
      hugeObjectSize = h5.readLength();
    }
  }

  class Record5 {
    int nameHash;
    byte[] heapId = new byte[7];

    Record5() throws IOException {
      nameHash = raf.readInt();
      raf.readFully(heapId);

      if (debugBtree2)
        debugOut.println("  record5 nameHash=" + nameHash + " heapId=" + Misc.showBytes(heapId));
    }
  }

  class Record6 {
    long creationOrder;
    byte[] heapId = new byte[7];

    Record6() throws IOException {
      creationOrder = raf.readLong();
      raf.readFully(heapId);
      if (debugBtree2)
        debugOut.println("  record6 creationOrder=" + creationOrder + " heapId=" + Misc.showBytes(heapId));
    }
  }

  class Record70 {
    byte location;
    int refCount;
    byte[] id = new byte[8];

    Record70() throws IOException {
      location = raf.readByte();
      refCount = raf.readInt();
      raf.readFully(id);
    }
  }

  class Record71 {
    byte location, messtype;
    short index;
    long address;

    Record71() throws IOException {
      location = raf.readByte();
      raf.readByte(); // skip a byte
      messtype = raf.readByte();
      index = raf.readShort();
      address = h5.readOffset();
    }
  }

  class Record8 {
    byte flags;
    int creationOrder, nameHash;
    byte[] heapId = new byte[8];

    Record8() throws IOException {
      raf.readFully(heapId);
      flags = raf.readByte();
      creationOrder = raf.readInt();
      nameHash = raf.readInt();
      if (debugBtree2)
        debugOut.println("  record8 creationOrder=" + creationOrder + " heapId=" + Misc.showBytes(heapId));
    }
  }

  class Record9 {
    byte flags;
    int creationOrder;
    byte[] heapId = new byte[8];

    Record9() throws IOException {
      raf.readFully(heapId);
      flags = raf.readByte();
      creationOrder = raf.readInt();
    }
  }

} // BTree2
