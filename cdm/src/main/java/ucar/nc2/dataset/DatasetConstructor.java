/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
    transferGroup(src, src.getRootGroup(), target.getRootGroup(), replaceCheck);
  }

  static private void transferGroup(NetcdfFile ds, Group src, Group target, ReplaceVariableCheck replaceCheck) {
    boolean unlimitedOK = false; // LOOK why not allowed?

    // group attributes
    transferGroupAttributes(src, target);

    // dimensions
    for (Dimension d : src.getDimensions()) {
      if (null == target.findDimensionLocal(d.getName())) {
        Dimension newd = new Dimension(d.getName(), d.getLength(), d.isShared(), unlimitedOK && d.isUnlimited(), d.isVariableLength());
        target.addDimension(newd);
      }
    }

    // variables
    for (Variable v : src.getVariables()) {
      Variable targetV = target.findVariable(v.getShortName());
      VariableEnhanced targetVe = (VariableEnhanced) targetV;
      boolean replace = (replaceCheck != null) && replaceCheck.replace(v); // replace not currently used

      if (replace || (null == targetV)) { // replace it
        if ((v instanceof Structure) && !(v instanceof StructureDS)) {
           v = new StructureDS(target, (Structure) v);

        } else if (!(v instanceof VariableDS)) {
          v = new VariableDS(target, v, false);  // enhancement done by original variable, this is just to reparent to target dataset.
        }

        if (null != targetV) target.remove(targetV);
        target.addVariable(v); // reparent group
        v.resetDimensions(); // dimensions will be different

      } else if (!targetV.hasCachedData() && (targetVe.getOriginalVariable() == null)) {
        // this is the case where we defined the variable, but didnt set its data. we now set it with the first nested
        // dataset that has a variable with the same name
        targetVe.setOriginalVariable(v);
      }
    }

    // nested groups
    for (Group srcNested : src.getGroups()) {
      Group nested = new Group(ds, target, srcNested.getShortName());
      target.addGroup(nested);
      transferGroup(ds, srcNested, nested, replaceCheck);
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
