/*
 * Created by JFormDesigner on Tue Dec 03 06:59:11 MST 2013
 */

package ucar.nc2.ui.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import thredds.featurecollection.FeatureCollectionConfig;

/**
 * @author John Caron
 */
public class GribCollectionConfig extends JDialog {
  public GribCollectionConfig() {
    initComponents();
  }

  public FeatureCollectionConfig getConfig() {
    FeatureCollectionConfig config = new FeatureCollectionConfig();
    FeatureCollectionConfig.GribConfig gconfig = config.gribConfig;
    if (excludeZero.isSelected())
      gconfig.setExcludeZero(true);
    TableModel tm = intvLenTable.getModel();
    for (int row = 0; row < tm.getRowCount(); row++) {
      Object len = tm.getValueAt(row, 0);
      Object id = tm.getValueAt(row, 1);
      if (len != null && id != null) {
        int intvLen = (Integer) len;
        String ids = (String) id;
        gconfig.setIntervalLength(intvLen, ids);
      }
    }
    return config;
  }

  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    label1 = new JLabel();
    excludeZero = new JCheckBox();
    panel1 = new JPanel();
    scrollPane1 = new JScrollPane();
    intvLenTable = new JTable();
    ApplyButton = new JButton();
    actionApply = new ActionApply();

    //======== this ========
    setTitle("GribConfig");
    Container contentPane = getContentPane();

    //---- label1 ----
    label1.setText("Interval Filter");

    //---- excludeZero ----
    excludeZero.setText("excludeZero");

    //======== panel1 ========
    {
      panel1.setLayout(new BorderLayout());

      //======== scrollPane1 ========
      {
        scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        //---- intvLenTable ----
        intvLenTable.setModel(new DefaultTableModel(
          new Object[][] {
            {null, ""},
            {null, null},
            {null, null},
            {null, null},
          },
          new String[] {
            "intvLength", "paramID"
          }
        ) {
          Class<?>[] columnTypes = new Class<?>[] {
            Integer.class, String.class
          };
          @Override
          public Class<?> getColumnClass(int columnIndex) {
            return columnTypes[columnIndex];
          }
        });
        intvLenTable.setFillsViewportHeight(true);
        intvLenTable.setPreferredScrollableViewportSize(new Dimension(450, 500));
        intvLenTable.setPreferredSize(new Dimension(150, 400));
        scrollPane1.setViewportView(intvLenTable);
      }
      panel1.add(scrollPane1, BorderLayout.CENTER);
    }

    //---- ApplyButton ----
    ApplyButton.setAction(actionApply);

    GroupLayout contentPaneLayout = new GroupLayout(contentPane);
    contentPane.setLayout(contentPaneLayout);
    contentPaneLayout.setHorizontalGroup(
      contentPaneLayout.createParallelGroup()
        .addGroup(contentPaneLayout.createSequentialGroup()
          .addGap(17, 17, 17)
          .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
            .addComponent(label1, GroupLayout.DEFAULT_SIZE, 241, Short.MAX_VALUE)
            .addComponent(excludeZero, GroupLayout.DEFAULT_SIZE, 241, Short.MAX_VALUE)
            .addComponent(panel1, GroupLayout.DEFAULT_SIZE, 241, Short.MAX_VALUE))
          .addGap(126, 126, 126))
        .addGroup(GroupLayout.Alignment.TRAILING, contentPaneLayout.createSequentialGroup()
          .addContainerGap()
          .addComponent(ApplyButton)
          .addGap(38, 38, 38))
    );
    contentPaneLayout.setVerticalGroup(
      contentPaneLayout.createParallelGroup()
        .addGroup(contentPaneLayout.createSequentialGroup()
          .addComponent(label1)
          .addGap(5, 5, 5)
          .addComponent(excludeZero)
          .addGroup(contentPaneLayout.createParallelGroup()
            .addGroup(contentPaneLayout.createSequentialGroup()
              .addGap(98, 98, 98)
              .addComponent(ApplyButton))
            .addGroup(contentPaneLayout.createSequentialGroup()
              .addGap(5, 5, 5)
              .addComponent(panel1, GroupLayout.PREFERRED_SIZE, 92, GroupLayout.PREFERRED_SIZE)))
          .addContainerGap())
    );
    pack();
    setLocationRelativeTo(getOwner());
    // JFormDesigner - End of component initialization  //GEN-END:initComponents
  }

  // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
  // Generated using JFormDesigner non-commercial license
  private JLabel label1;
  private JCheckBox excludeZero;
  private JPanel panel1;
  private JScrollPane scrollPane1;
  private JTable intvLenTable;
  private JButton ApplyButton;
  private ActionApply actionApply;
  // JFormDesigner - End of variables declaration  //GEN-END:variables

  private class ActionApply extends AbstractAction {
    private ActionApply() {
      // JFormDesigner - Action initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
      // Generated using JFormDesigner non-commercial license
      putValue(NAME, "Apply");
      // JFormDesigner - End of action initialization  //GEN-END:initComponents
    }

    public void actionPerformed(ActionEvent e) {
      setVisible(false);
    }
  }
}
