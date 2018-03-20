/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.geoloc;

import javax.swing.*;

public class ProjectionParamPanel extends JPanel {
    String name = "ProjectionParamPanel";

    public String toString() {
      return name;
    }

  ProjectionParamPanel() {
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      //setLayout(new FlowLayout());
      //setProjection("ucar.unidata.geoloc.projection.Stereographic");
    }

    // construct input fields based on Projection Class
    public void setProjection(ProjectionManager.ProjectionClass pc)  {
      // clear out any fields
      removeAll();

      for (ProjectionManager.ProjectionParam pp : pc.paramList) {
        // construct the label
        JPanel thisPanel = new JPanel();
        thisPanel.add(new JLabel(pp.name + ": "));

        // text input field
        JTextField tf = new JTextField();
        pp.setTextField(tf);
        tf.setColumns(12);
        thisPanel.add(tf);
        add(thisPanel);
      }
      revalidate();
    }

  }
