/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/*
 * Created by JFormDesigner on Mon Mar 24 16:21:05 MDT 2014
 */

package ucar.nc2.ui.dialog;

import java.awt.event.*;
import ucar.nc2.util.DiskCache2;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author John Caron
 */
public class DiskCache2Form extends JDialog {
  DiskCache2 diskCache;
  public DiskCache2Form(Frame owner, DiskCache2 diskCache) {
    super(owner);
    this.diskCache = diskCache;
    initComponents();

    rootDir.setText(diskCache.getRootDirectory());
    policyCB.setSelectedItem("yes");
  }


  private void cancelButtonActionPerformed(ActionEvent e) {
    setVisible(false);
  }

  private void okButtonActionPerformed(ActionEvent e) {
    diskCache.setRootDirectory( rootDir.getText());
    setVisible(false);
  }

  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    dialogPane = new JPanel();
    contentPanel = new JPanel();
    label1 = new JLabel();
    rootDir = new JTextField();
    label2 = new JLabel();
    label3 = new JLabel();
    policyCB = new JComboBox<>();
    buttonBar = new JPanel();
    okButton = new JButton();
    cancelButton = new JButton();

    //======== this ========
    Container contentPane = getContentPane();
    contentPane.setLayout(new BorderLayout());

    //======== dialogPane ========
    {
      dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
      dialogPane.setLayout(new BorderLayout());

      //======== contentPanel ========
      {

        //---- label1 ----
        label1.setText("Root Directory:");

        //---- label2 ----
        label2.setText("Policy:");

        //---- label3 ----
        label3.setText("Policy:");

        //---- policyCB ----
        policyCB.setModel(new DefaultComboBoxModel<>(new String[] {
          "yes",
          "no"
        }));

        GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(
          contentPanelLayout.createParallelGroup()
            .addGroup(contentPanelLayout.createSequentialGroup()
              .addContainerGap()
              .addGroup(contentPanelLayout.createParallelGroup()
                .addGroup(contentPanelLayout.createSequentialGroup()
                  .addComponent(label1)
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED))
                .addGroup(contentPanelLayout.createSequentialGroup()
                  .addGap(41, 41, 41)
                  .addComponent(label2, GroupLayout.PREFERRED_SIZE, 0, GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(label3)
                  .addGap(2, 2, 2)))
              .addGroup(contentPanelLayout.createParallelGroup()
                .addGroup(contentPanelLayout.createSequentialGroup()
                  .addComponent(rootDir)
                  .addContainerGap())
                .addGroup(contentPanelLayout.createSequentialGroup()
                  .addComponent(policyCB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                  .addGap(248, 248, 248))))
        );
        contentPanelLayout.setVerticalGroup(
          contentPanelLayout.createParallelGroup()
            .addGroup(contentPanelLayout.createSequentialGroup()
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(rootDir, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(label1))
              .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(label2, GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE)
                .addComponent(label3)
                .addComponent(policyCB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
              .addGap(0, 145, Short.MAX_VALUE))
        );
      }
      dialogPane.add(contentPanel, BorderLayout.CENTER);

      //======== buttonBar ========
      {
        buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
        buttonBar.setLayout(new GridBagLayout());
        ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
        ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

        //---- okButton ----
        okButton.setText("OK");
        okButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            okButtonActionPerformed(e);
          }
        });
        buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
          GridBagConstraints.CENTER, GridBagConstraints.BOTH,
          new Insets(0, 0, 0, 5), 0, 0));

        //---- cancelButton ----
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            cancelButtonActionPerformed(e);
          }
        });
        buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
          GridBagConstraints.CENTER, GridBagConstraints.BOTH,
          new Insets(0, 0, 0, 0), 0, 0));
      }
      dialogPane.add(buttonBar, BorderLayout.SOUTH);
    }
    contentPane.add(dialogPane, BorderLayout.CENTER);
    pack();
    setLocationRelativeTo(getOwner());
    // JFormDesigner - End of component initialization  //GEN-END:initComponents
  }

  // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
  // Generated using JFormDesigner non-commercial license
  private JPanel dialogPane;
  private JPanel contentPanel;
  private JLabel label1;
  private JTextField rootDir;
  private JLabel label2;
  private JLabel label3;
  private JComboBox<String> policyCB;
  private JPanel buttonBar;
  private JButton okButton;
  private JButton cancelButton;
  // JFormDesigner - End of variables declaration  //GEN-END:variables

  private class OkAction extends AbstractAction {
    private OkAction() {
      // JFormDesigner - Action initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
      // Generated using JFormDesigner non-commercial license
      // JFormDesigner - End of action initialization  //GEN-END:initComponents
    }

    public void actionPerformed(ActionEvent e) {
      diskCache.setRootDirectory( rootDir.getText());
    }
  }

}
