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
package ucar.nc2.iosp.bufr;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Convert a list of data descriptors to a tree of DataDescriptor onjects.
 * Expand Table D, process table C operators.
 *
 * @author caron
 * @since Jul 14, 2008
 */
public class DataDescriptorTreeConstructor {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataDescriptorTreeConstructor.class);

  //////////////////////////////////////////////////////////////////////////////////
  private DataDescriptor root;

  public DataDescriptor factory(TableLookup lookup, BufrDataDescriptionSection dds) {
    root = new DataDescriptor();

    // convert ids to DataDescriptor
    List<DataDescriptor> keys = convert(dds.getDataDescriptors(), lookup);

    // make replicated keys into subKeys, constituting a tree
    List<DataDescriptor> tree = replicate(keys);

    // flatten the compounds
    root.subKeys = new ArrayList<DataDescriptor>();
    flatten(root.subKeys, tree);

    // process the operators
    operate( root.subKeys);

    // count the size
    root.total_nbits = root.countBits();

    return root;
  }

  // convert ids to DataDescriptors, expand table D
  private List<DataDescriptor> convert(List<Short> keyDesc, TableLookup lookup) {
    if (keyDesc == null) return null;

    List<DataDescriptor> keys = new ArrayList<DataDescriptor>();
    for (short id : keyDesc) {
      DataDescriptor dd = new DataDescriptor(id, lookup);
      keys.add( dd);
      if (dd.f == 3) {
        List<Short> subDesc = lookup.getDescriptorsTableD(dd.fxy);
        if (subDesc == null)
          dd.bad = true;
        else
          dd.subKeys = convert(subDesc, lookup);
      }
    }
    return keys;
  }

  // look for replication, move replicated items into subtree
  private List<DataDescriptor> replicate(List<DataDescriptor> keys) {
    List<DataDescriptor> tree = new ArrayList<DataDescriptor>();
    Iterator<DataDescriptor> dkIter = keys.iterator();
    while (dkIter.hasNext()) {
      DataDescriptor dk = dkIter.next();
      if (dk.f == 1) {
        dk.subKeys = new ArrayList<DataDescriptor>();
        dk.replication = dk.y; // replication count

        if (dk.replication == 0) { // delayed repliction
          root.isVarLength = true; // variable sized data == defered replication == sequence data

          // the next one is the replication count size
          DataDescriptor replication = dkIter.next();
          if (replication.y == 0)
            dk.replicationCountSize = 1; // ??
          else if (replication.y == 1)
            dk.replicationCountSize = 8;
          else if (replication.y == 2)
            dk.replicationCountSize = 16;
          else if (replication.y == 11)
            dk.repetitionCountSize = 8;
          else if (replication.y == 12)
            dk.repetitionCountSize = 16;
          else
            log.error("Unknown replication type= "+replication);
        }

        // transfer to the subKey list
        for (int j = 0; j < dk.x && dkIter.hasNext(); j++) {
          dk.subKeys.add( dkIter.next());
        }

        // recurse
        dk.subKeys = replicate(dk.subKeys);

      } else if ((dk.f == 3) && (dk.subKeys != null)) {
        dk.subKeys = replicate( dk.subKeys); // do at all levels
      }

      tree.add(dk);
    }

    return tree;
  }

  // flatten the compounds (type 3) remove any bad ones
  private void flatten(List<DataDescriptor> result, List<DataDescriptor> tree) {

    for (DataDescriptor key : tree) {
      if (key.bad) {
        root.isBad = true;
        continue;
      }

      if ((key.f == 3) && (key.subKeys != null)) {
        flatten(result, key.subKeys);

      } else if (key.f == 1) { // flatten the subtrees
        List<DataDescriptor> subTree = new ArrayList<DataDescriptor>();
        flatten(subTree, key.subKeys);
        key.subKeys = subTree;
        result.add(key);

      } else {
        result.add(key);
      }
    }
  }

  private DataDescriptor changeWidth = null; // 02 01 Y
  private DataDescriptor changeScale = null; // 02 02 Y
  private DataDescriptor changeRefval = null; // 02 03 Y

  private void operate(List<DataDescriptor> tree) {
    if (tree == null) return;
    boolean hasAssFields = false;
    DataDescriptor.AssociatedField assField = null; // 02 04 Y

    Iterator<DataDescriptor> iter = tree.iterator();
    while (iter.hasNext()) {
      DataDescriptor dd = iter.next();

      if (dd.f == 2) {
        if (dd.x == 1) {
          changeWidth = (dd.y == 0) ? null : dd;
          iter.remove();

        } else if (dd.x == 2) {
          changeScale = (dd.y == 0) ? null : dd;
          iter.remove();
          // throw new UnsupportedOperationException("2-2-Y (change scale)");

        } else if (dd.x == 3) {
          changeRefval = (dd.y == 255) ? null : dd;
          iter.remove();
          // throw new UnsupportedOperationException("2-3-Y (change reference values)");  // untested - no examples

        } else if (dd.x == 4) {
          assField = (dd.y == 0) ? null : new DataDescriptor.AssociatedField(dd.y);
          iter.remove();
          hasAssFields = true;

        } else if (dd.x == 5) { // char data
          dd.type = 1; // String
          dd.bitWidth = dd.y * 8;
          dd.name = "Note";

        } else if (dd.x == 6) {
          // see L3-82 (3.1.6.5)
          // "Y bits of data are described by the immediately following descriptor". could they speak English?
          iter.remove();
          if (iter.hasNext()) {
            DataDescriptor next = iter.next();
            next.bitWidth = dd.y;
          }

        } else {
          iter.remove();          
        }

      } else if (dd.subKeys != null) {
        operate(dd.subKeys);

      } else if (dd.f == 0) {

        if (dd.type == 0) {
          if (changeWidth != null)
            dd.bitWidth += changeWidth.y-128;
          if (changeScale != null)
            dd.scale += changeScale.y-128;
          if (changeRefval != null)
            dd.refVal += changeRefval.y-128;  // LOOK wrong
        }

        if ((dd.f == 0) && (assField != null)) {
          assField.nfields++;
          dd.assField = assField;
          assField.dataFldName = dd.name;
        }
      }
    }

    if (hasAssFields) addAssFields(tree);
  }

  private void addAssFields(List<DataDescriptor> tree) {
    if (tree == null) return;

    int index = 0;
    while (index < tree.size()) {
      DataDescriptor dd = tree.get(index);
      if (dd.assField != null) {
        DataDescriptor.AssociatedField assField = dd.assField;

        if ((dd.f == 0) && (dd.x == 31) && (dd.y == 21)) {  // the meaning field
          dd.name = assField.dataFldName+"_associated_field_significance";
          dd.assField = null;

        } else {
          DataDescriptor assDD = new DataDescriptor(dd, assField.nbits);
          tree.add(index, assDD);
          index++;
        }
      }

      index++;
    }
  }
}
