/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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
 * Created by JFormDesigner on Tue Jan 26 16:33:46 MST 2010
 */

package ucar.nc2.ui.dialog;

import ucar.nc2.ft.fmrc.Fmrc;
import ucar.nc2.units.DateFormatter;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author unknown
 */
public class Fmrc2Dialog extends JDialog {

  public class Data {
    public String type;
    public Object param;
    public String where;

    private Data(String type, Object param, String where) {
      this.type = type;
      this.param = param;
      this.where = where;
    }

    @Override
    public String toString() {
      return "Data{" +
              "type='" + type + '\'' +
              "param='" + param + '\'' +
              ", where='" + where + '\'' +
              '}';
    }
  }

  private Fmrc fmrc;
  public Fmrc2Dialog(Frame owner) {
    super(owner);
    initComponents();
  }

  public void setFmrc(Fmrc fmrc) {
    this.fmrc = fmrc;
  }
  
  private void okButtonActionPerformed(ActionEvent e) {
    Data data =  new Data((String) comboBox1.getSelectedItem(), comboBox2.getSelectedItem(), (String) list1.getSelectedValue());
    firePropertyChange("OK", null, data);
    setVisible(false);
  }

  private void cancelButtonActionPerformed(ActionEvent e) {
    setVisible(false);
  }

  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    dialogPane = new JPanel();
    contentPanel = new JPanel();
    label1 = new JLabel();
    comboBox1 = new JComboBox();
    label2 = new JLabel();
    list1 = new JList();
    comboBox2 = new JComboBox();
    label3 = new JLabel();
    buttonBar = new JPanel();
    okButton = new JButton();
    cancelButton = new JButton();
    datasetCB = new datasetCBaction();

    //======== this ========
    setTitle("Show Dataset in another Tab");
    Container contentPane = getContentPane();
    contentPane.setLayout(new BorderLayout());

    //======== dialogPane ========
    {
      dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
      dialogPane.setLayout(new BorderLayout());

      //======== contentPanel ========
      {

        //---- label1 ----
        label1.setText("Dataset:");
        label1.setFont(new Font("Dialog", Font.BOLD, 12));

        //---- comboBox1 ----
        comboBox1.setModel(new DefaultComboBoxModel(new String[] {
          "Dataset2D",
          "Best",
          "Run",
          "ConstantForecast",
          "ConstantOffset"
        }));
        comboBox1.setAction(datasetCB);

        //---- label2 ----
        label2.setText("SendTo:");
        label2.setFont(new Font("Dialog", Font.BOLD, 12));

        //---- list1 ----
        list1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list1.setModel(new AbstractListModel() {
          String[] values = {
            "NetcdfFile Viewer",
            "CoordSys Tab",
            "Grid FeatureType",
            "Detail Info"
          };
          public int getSize() { return values.length; }
          public Object getElementAt(int i) { return values[i]; }
        });
        list1.setVisibleRowCount(7);

        //---- comboBox2 ----
        comboBox2.setModel(new DefaultComboBoxModel(new String[] {
          "N/A"
        }));

        //---- label3 ----
        label3.setText("Param:");
        label3.setFont(label3.getFont().deriveFont(label3.getFont().getStyle() | Font.BOLD));

        GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(
          contentPanelLayout.createParallelGroup()
            .addGroup(contentPanelLayout.createSequentialGroup()
              .addContainerGap()
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                .addGroup(contentPanelLayout.createParallelGroup()
                  .addComponent(label1)
                  .addComponent(label2))
                .addComponent(label3))
              .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
              .addGroup(contentPanelLayout.createParallelGroup()
                .addComponent(comboBox2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(comboBox1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(list1))
              .addContainerGap(133, Short.MAX_VALUE))
        );
        contentPanelLayout.setVerticalGroup(
          contentPanelLayout.createParallelGroup()
            .addGroup(contentPanelLayout.createSequentialGroup()
              .addContainerGap()
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(label1)
                .addComponent(comboBox1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
              .addGap(18, 18, 18)
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(comboBox2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(label3))
              .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(list1, GroupLayout.PREFERRED_SIZE, 72, GroupLayout.PREFERRED_SIZE)
                .addComponent(label2))
              .addContainerGap())
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
    contentPane.add(dialogPane, BorderLayout.NORTH);
    pack();
    setLocationRelativeTo(getOwner());
    // JFormDesigner - End of component initialization  //GEN-END:initComponents
  }

  // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
  // Generated using JFormDesigner non-commercial license
  private JPanel dialogPane;
  private JPanel contentPanel;
  private JLabel label1;
  private JComboBox comboBox1;
  private JLabel label2;
  private JList list1;
  private JComboBox comboBox2;
  private JLabel label3;
  private JPanel buttonBar;
  private JButton okButton;
  private JButton cancelButton;
  private datasetCBaction datasetCB;
  // JFormDesigner - End of variables declaration  //GEN-END:variables

  private class datasetCBaction extends AbstractAction {
    private datasetCBaction() {
      // JFormDesigner - Action initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
      // Generated using JFormDesigner non-commercial license
      putValue(NAME, "test");
      // JFormDesigner - End of action initialization  //GEN-END:initComponents
    }

    public void actionPerformed(ActionEvent e) {
      DefaultComboBoxModel m = new DefaultComboBoxModel();
      String type = (String) comboBox1.getSelectedItem();
      if (type.equals("Run")) {
        java.util.List<Date> dates = null;
        try {
          dates = fmrc.getRunDates();
        } catch (IOException e1) {
          return;
        }
        DateFormatter df = new DateFormatter();
        for (Date d : dates)
          m.addElement(df.toDateTimeStringISO(d));

      } else if (type.equals("ConstantForecast")) {
          java.util.List<Date> dates = null;
          try {
            dates = fmrc.getForecastDates();
          } catch (IOException e1) {
            return;
          }
          DateFormatter df = new DateFormatter();
          for (Date d : dates)
            m.addElement(df.toDateTimeStringISO(d));

      } else if (type.equals("ConstantOffset")) {
          double [] offs  = null;
          try {
            offs = fmrc.getForecastOffsets();
          } catch (IOException e1) {
            return;
          }
          for (double d : offs)
            m.addElement(d);

      } else {
        m.addElement("N/A");
      }

      comboBox2.setModel(m);
    }
  }
}
