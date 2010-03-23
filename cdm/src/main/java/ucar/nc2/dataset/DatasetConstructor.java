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
package ucar.nc2.dataset;

import ucar.nc2.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Helper methods for constructing NetcdfDatasets.
 * @author caron
 * @since Jul 6, 2007
 */
public class DatasetConstructor {

  /**
   * Copy contents of "src" to "target". skip ones that already exist (by name).
   * Dimensions and Variables are replaced with equivalent elements, but unlimited dimensions are turned into regular dimensions.
   * Attribute doesnt have to be replaced because its immutable, so its copied by reference.
   *
   * @param src transfer from here. If src is a NetcdfDataset, transferred variables get reparented to target group.
   * @param target transfer to this NetcdfDataset.
   * @param replaceCheck if null, add if a Variable of the same name doesnt already exist, otherwise
   *   replace if replaceCheck.replace( Variable v) is true
   */
  static public void transferDataset(NetcdfFile src, NetcdfDataset target, ReplaceVariableCheck replaceCheck) {
    transferGroup(src, target, src.getRootGroup(), target.getRootGroup(), replaceCheck);
  }

  // transfer the objects in src group to the target group
  static private void transferGroup(NetcdfFile ds, NetcdfDataset targetDs, Group src, Group targetGroup, ReplaceVariableCheck replaceCheck) {
    boolean unlimitedOK = false; // LOOK why not allowed?

    // group attributes
    transferGroupAttributes(src, targetGroup);

    // dimensions
    for (Dimension d : src.getDimensions()) {
      if (null == targetGroup.findDimensionLocal(d.getName())) {
        Dimension newd = new Dimension(d.getName(), d.getLength(), d.isShared(), unlimitedOK && d.isUnlimited(), d.isVariableLength());
        targetGroup.addDimension(newd);
      }
    }

    // variables
    for (Variable v : src.getVariables()) {
      Variable targetV = targetGroup.findVariable(v.getShortName());
      VariableEnhanced targetVe = (VariableEnhanced) targetV;
      boolean replace = (replaceCheck != null) && replaceCheck.replace(v); // replaceCheck not currently used

      if (replace || (null == targetV)) { // replace it
        if ((v instanceof Structure) && !(v instanceof StructureDS)) {
           v = new StructureDS(targetGroup, (Structure) v);

          // else if (!(v instanceof VariableDS) && !(v instanceof StructureDS)) Doug Lindolm
        } else if (!(v instanceof VariableDS)) {
          v = new VariableDS(targetGroup, v, false);  // enhancement done by original variable, this is just to reparent to target dataset.
        }

        if (null != targetV) targetGroup.remove(targetV);
        targetGroup.addVariable(v); // reparent group
        v.resetDimensions(); // dimensions will be different

      } else if (!targetV.hasCachedData() && (targetVe.getOriginalVariable() == null)) {
        // this is the case where we defined the variable, but didnt set its data. we now set it with the first nested
        // dataset that has a variable with the same name
        targetVe.setOriginalVariable(v);
      }
    }

    // nested groups - check if target already has it
    for (Group srcNested : src.getGroups()) {
      Group nested = targetGroup.findGroup(srcNested.getShortName());
      if (null == nested) {
        nested = new Group(ds, targetGroup, srcNested.getShortName());
        targetGroup.addGroup(nested);
      }
      transferGroup(ds, targetDs, srcNested, nested, replaceCheck);
    }
  }

  /**
   * Copy attributes from src to target, skip ones that already exist (by name)
   * @param src copy from here
   * @param target copy to here
   */
  static public void transferVariableAttributes(Variable src, Variable target) {
    for (Attribute a : src.getAttributes()) {
      if (null == target.findAttribute(a.getName()))
        target.addAttribute(a);
    }
  }

  /**
   * Copy attributes from src to target, skip ones that already exist (by name)
   * @param src copy from here
   * @param target copy to here
   */
  static public void transferGroupAttributes(Group src, Group target) {
    for (Attribute a : src.getAttributes()) {
      if (null == target.findAttribute(a.getName()))
        target.addAttribute(a);
    }
  }

  /**
   * Find the Group in newFile that corresponds (by name) with oldGroup
   *
   * @param newFile look in this NetcdfFile
   * @param oldGroup corresponding (by name) with oldGroup
   * @return corresponding Group, or null if no match.
   */
  static public Group findGroup(NetcdfFile newFile, Group oldGroup) {
    List<Group> chain = new ArrayList<Group>(5);
    Group g = oldGroup;
    while ( g.getParentGroup() != null) { // skip the root
      chain.add(0, g); // put in front
      g = g.getParentGroup();
    }

    Group newg = newFile.getRootGroup();
    for (Group oldg : chain) {
      newg = newg.findGroup( oldg.getShortName());
      if (newg == null) return null;
    }
    return newg;
  }
}
