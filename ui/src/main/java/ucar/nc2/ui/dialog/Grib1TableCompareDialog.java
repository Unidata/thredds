/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

/*
 * Created by JFormDesigner on Thu Aug 25 08:13:12 MDT 2011
 */

package ucar.nc2.ui.dialog;

import ucar.grib.grib1.GribPDSParamTable;
import ucar.nc2.ui.Grib1TablesViewer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.GroupLayout;
import javax.swing.LayoutStyle;
import javax.swing.border.*;

/**
 * @author John Caron
 */
public class Grib1TableCompareDialog extends JDialog {
  Grib1TablesViewer.TableBean table1bean;

  public class Data {
    public Grib1TablesViewer.TableBean table1bean;
    public boolean compareNames, compareUnits, compareDesc, showMissing;

    private Data(Grib1TablesViewer.TableBean table1bean, boolean compareNames, boolean compareUnits, boolean compareDesc, boolean showMissing) {
      this.table1bean = table1bean;
      this.compareNames = compareNames;
      this.compareUnits = compareUnits;
      this.compareDesc = compareDesc;
      this.showMissing = showMissing;
    }
  }

  public Grib1TableCompareDialog(Frame owner) {
    super(owner);
    initComponents();
  }

  public Grib1TableCompareDialog(Dialog owner) {
    super(owner);
    initComponents();
  }

  public void setTable1(Grib1TablesViewer.TableBean bean) {
    this.table1bean = bean;
    table1.setText(bean.getPath());
  }

  private void fileBrowserActionPerformed(ActionEvent e) {
    // TODO add your code here
  }

  private void cancelButtonActionPerformed(ActionEvent e) {
    // TODO add your code here
  }

  private void okButtonActionPerformed(ActionEvent e) {
    Data data =  new Data(table1bean, compareNames.isSelected(), compareUnits.isSelected(), compareDesc.isSelected(), showMissing.isSelected() );
    firePropertyChange("OK", null, data);
    setVisible(false);
  }

  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    dialogPane = new JPanel();
    contentPanel = new JPanel();
    label1 = new JLabel();
    buttonBar = new JPanel();
    compareDesc = new JCheckBox();
    compareUnits = new JCheckBox();
    compareNames = new JCheckBox();
    cancelButton = new JButton();
    okButton = new JButton();
    table1 = new JTextField();
    showMissing = new JCheckBox();

    //======== this ========
    setTitle("Compare Grib1 tables");
    Container contentPane = getContentPane();
    contentPane.setLayout(new BorderLayout());

    //======== dialogPane ========
    {
      dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
      dialogPane.setLayout(new BorderLayout());

      //======== contentPanel ========
      {

        //---- label1 ----
        label1.setText("table:");
        label1.setFont(new Font("Dialog", Font.BOLD, 12));

        //======== buttonBar ========
        {
          buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
          buttonBar.setLayout(new GridBagLayout());
          ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
          ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};
        }

        //---- compareDesc ----
        compareDesc.setText("compare Desc");

        //---- compareUnits ----
        compareUnits.setText("compareUnits");

        //---- compareNames ----
        compareNames.setText("compareNames");
        compareNames.setSelected(true);

        //---- cancelButton ----
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            cancelButtonActionPerformed(e);
          }
        });

        //---- okButton ----
        okButton.setText("OK");
        okButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            okButtonActionPerformed(e);
          }
        });

        //---- table1 ----
        table1.setEditable(false);

        //---- showMissing ----
        showMissing.setText("showMissing");

        GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(
          contentPanelLayout.createParallelGroup()
            .addGroup(contentPanelLayout.createSequentialGroup()
              .addContainerGap()
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                .addGroup(contentPanelLayout.createSequentialGroup()
                  .addComponent(label1)
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(contentPanelLayout.createParallelGroup()
                    .addGroup(contentPanelLayout.createSequentialGroup()
                      .addComponent(compareNames)
                      .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                      .addComponent(compareUnits)
                      .addGap(18, 18, 18)
                      .addComponent(compareDesc)
                      .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                      .addComponent(showMissing))
                    .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                      .addGroup(contentPanelLayout.createSequentialGroup()
                        .addComponent(okButton, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
                      .addComponent(table1, GroupLayout.PREFERRED_SIZE, 611, GroupLayout.PREFERRED_SIZE)))
                  .addGap(111, 111, 111))
                .addComponent(buttonBar, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 779, GroupLayout.PREFERRED_SIZE))
              .addContainerGap())
        );
        contentPanelLayout.setVerticalGroup(
          contentPanelLayout.createParallelGroup()
            .addGroup(contentPanelLayout.createSequentialGroup()
              .addGap(11, 11, 11)
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(label1)
                .addComponent(table1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
              .addGap(46, 46, 46)
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(compareNames)
                .addComponent(compareUnits)
                .addComponent(compareDesc)
                .addComponent(showMissing))
              .addGap(22, 22, 22)
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(cancelButton)
                .addComponent(okButton))
              .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 22, Short.MAX_VALUE)
              .addComponent(buttonBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
              .addContainerGap())
        );
      }
      dialogPane.add(contentPanel, BorderLayout.SOUTH);
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
  private JPanel buttonBar;
  private JCheckBox compareDesc;
  private JCheckBox compareUnits;
  private JCheckBox compareNames;
  private JButton cancelButton;
  private JButton okButton;
  private JTextField table1;
  private JCheckBox showMissing;
  // JFormDesigner - End of variables declaration  //GEN-END:variables
}
