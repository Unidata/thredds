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
 * Convert a list of data descriptors to a tree of DataDescriptor objects.
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
    List<DataDescriptor> keys = decode(dds.getDataDescriptors(), lookup);

    // unwind replications inside compound (!!??)
    keys = preflatten(keys);

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
  private List<DataDescriptor> decode(List<Short> keyDesc, TableLookup lookup) {
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
          dd.subKeys = decode(subDesc, lookup);
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

  /* Use case:
   3-62-1  : HEADR
      0-4-194 : FORECAST TIME
      0-1-205 : STATION NUMBER -- 6 DIGITS
      0-1-198 : REPORT IDENTIFIER
      0-5-2   : Latitude (coarse accuracy)
      0-6-2   : Longitude (coarse accuracy)
      0-10-194: GRID-POINT ELEVATION
      0-2-196 : CLASS OF PROFILE OUTPUT
    3-60-2  :
      1-01-000: replication
      0-31-1  : Delayed descriptor replication factor
    3-62-2  : PROFILE
      0-10-4  : Pressure
      0-12-1  : Temperature/dry-bulb temperature
      0-11-3  : u-component

     where the 3-62-2 should be replicated.
     This is from NCEP bufrtab.ETACLS1. Not sure if others use this idiom.
   */
  // preflatten replications inside a top level compound (!)
  private List<DataDescriptor> preflatten(List<DataDescriptor> tree) {
    List<DataDescriptor> result = new ArrayList<DataDescriptor>(tree.size());
    for (DataDescriptor key : tree) {
      boolean preflatten = false;

      if ((key.f == 3) && (key.subKeys != null)) {
        List<DataDescriptor> subkeys = key.subKeys;
        for (int i=0; i<subkeys.size();i++) {
          DataDescriptor subkey = subkeys.get(i);
          if (subkey.f == 1)  {
            int need = subkey.x;
            int have = subkeys.size() - i - 1;
            if (subkey.y == 0) have--;
            if (need > have) {
              preflatten = true;
              // ScannerSystem.out.printf("preflatten replication %d > %d%n", need, have);
            }
          }
        }
      }

      if (preflatten)
        result.addAll(key.subKeys);
      else
       result.add(key);

    }
    return result;
  }

  // flatten the compounds (type 3); but dont remove bad ones
  private void flatten(List<DataDescriptor> result, List<DataDescriptor> tree) {

    for (DataDescriptor key : tree) {
      if (key.bad) {
        root.isBad = true;
        result.add(key); // add it anyway so we can see it in debug
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
  private DataPresentIndicator dpi = null; // assume theres only one in effect at a time

  private void operate(List<DataDescriptor> tree) {
    if (tree == null) return;
    boolean hasAssFields = false;
    // boolean hasDpiFields = false;
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

        } else if (dd.x == 5) { // char data - this allows arbitrary string to be inserted
          dd.type = 1; // String
          dd.bitWidth = dd.y * 8;
          dd.name = "Note";

        } else if (dd.x == 6) {
          // see L3-82 (3.1.6.5)
          // "Y bits of data are described by the immediately following descriptor". could they speak English?
          iter.remove();
          if ((dd.y != 0) && iter.hasNext()) {  // fnmoc using 2-6-0 as cancel (apparently)
            DataDescriptor next = iter.next();
            next.bitWidth = dd.y;
          }

        } else if (dd.x == 36) { 
          if (iter.hasNext()) {
            DataDescriptor dpi_dd = iter.next(); // this should be a replicated data present field
            dpi = new DataPresentIndicator(tree, dpi_dd);
            dd.dpi = dpi;
            dpi_dd.dpi = dpi;
          }
          
        } else if ((dd.x == 37) && (dd.y == 255)) { // cancel dpi
          dpi = null;

        } else if ((dd.x == 24) && (dd.y == 255)) {
           dd.dpi = dpi;
        }

      } else if (dd.subKeys != null) {
        operate(dd.subKeys);

      } else if (dd.f == 0) {

        if (dd.type != 3) {  // numeric or string or enum, not compound
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
    // if (hasDpiFields) addDpiFields(tree);
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
          DataDescriptor assDD = dd.makeAssociatedField(assField.nbits);
          tree.add(index, assDD);
          index++;
        }
      }

      index++;
    }
  }

  /* private void addDpiFields(List<DataDescriptor> tree) {
    if (tree == null) return;

    Iterator<DataDescriptor> iter = tree.iterator();
    while (iter.hasNext()) {
      DataDescriptor dd = iter.next();
      if (dd.dpi != null) { // make this into a compound type
        dd.name = "firstOrderStatistics";
        dd.type = 3;
        dd.replication = 1;
        dd.subKeys = new ArrayList<DataDescriptor>();
        while (iter.hasNext()) {
          DataDescriptor dd2 = iter.next();
          iter.remove();
          dd.subKeys.add(dd2);
          if (dd2 == dd.dpi.stop) {
            dd2.subKeys = new ArrayList<DataDescriptor>();

            dd2.subKeys.add(dd.dpi.statField);
            dd.dpi.statField.dpi = dd.dpi; // also need it here
            //dd.dpi = null;
           // DataDescriptor extra = dd.dpi.linear.get(184); // temp kludge
          //  for (int i=0; i< dd2.replication; i++)
           //   dd.subKeys.add(extra);
            break;
          }
        }
        System.out.printf("addDpiFields for %s %n", dd);
      }
    }
  }

  /* private DataPresentIndicator processDataPresentIndicator(List<DataDescriptor> tree, int hellRealmIndex) {
    DataPresentIndicator dpi = new DataPresentIndicator();
    Iterator<DataDescriptor> iter = tree.iterator();
    while (iter.hasNext()) {
      DataDescriptor dd = iter.next();
      if ((dd.f == 2) && (dd.x == hellRealmIndex) && (dd.y == 0)) {
        dpi.start = dd;

        while (iter.hasNext()) {
          dd = iter.next();
          if (dd.f == 1) {
            for (DataDescriptor dd2 : dd.getSubKeys()){
              if ((dd2.f == 2) && (dd2.x == hellRealmIndex) && (dd2.y == 255)) {
                dpi.stop = dd;
                dpi.statField = dd2;
              } else if ((dd2.f == 0) && (dd2.x == 31) && (dd2.y == 31)) {
                dpi.dataPresent = dd;
              }
            }
          }
        }
      }
    }

    dpi.linear = new ArrayList<DataDescriptor>();
    linearize(tree, dpi);
    // LOOK System.out.printf("DPI: data count = %d linear count = %d %n", dpi.dataPresent.replication, dpi.linear.size());

    return dpi;
  }  */

  static class DataPresentIndicator {
    DataDescriptor dataPresent; // replication of bit present field
    List<DataDescriptor> linear = new ArrayList<DataDescriptor>(); // linear list of dds

    DataPresentIndicator(List<DataDescriptor> tree, DataDescriptor dpi_dd) {
      this.dataPresent = dpi_dd;

      linear = new ArrayList<DataDescriptor>();
      linearize(tree);
    }

    int getNfields() { return dataPresent.replication; }

    private void linearize(List<DataDescriptor> tree) {
      for (DataDescriptor dd : tree) {
        if (dd.f == 0) {
          linear.add(dd);

        } else if (dd.f == 1) {
          for (int i = 0; i < dd.replication; i++) // whut about defered replication hahahahahah
            linearize(dd.getSubKeys());
        }
      }
    }
  }

}
