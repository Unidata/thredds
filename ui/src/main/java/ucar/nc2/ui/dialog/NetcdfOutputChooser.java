/*
 * Created by JFormDesigner on Thu Nov 15 14:02:02 MST 2012
 */

package ucar.nc2.ui.dialog;

import java.awt.event.*;
import ucar.nc2.NetcdfFileWriter;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.*;
import ucar.util.prefs.ui.*;

/**
 * @author John Caron
 */
public class NetcdfOutputChooser extends JDialog {
  public NetcdfOutputChooser(Frame owner) {
    super(owner);
    initComponents();
  }

  public NetcdfOutputChooser(Dialog owner) {
    super(owner);
    initComponents();
  }

  public void setOutputFilename(String filename) {
    outputFilename.setText(filename);
  }

  public class Data {
     public String outputFilename;
     public NetcdfFileWriter.Version version;

     private Data(String outputFilename, NetcdfFileWriter.Version version) {
       this.outputFilename = outputFilename;
       this.version = version;
     }
   }

   private void okButtonActionPerformed(ActionEvent e) {
     Data data =  new Data( outputFilename.getText(), (NetcdfFileWriter.Version) netcdfVersion.getSelectedItem() );
     firePropertyChange("OK", null, data);
     setVisible(false);
   }

   private void createUIComponents() {
     // TODO: add custom component creation code here
   }


  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    dialogPane = new JPanel();
    contentPanel = new JPanel();
    label1 = new JLabel();
    outputFilename = new JTextField();
    label2 = new JLabel();
    netcdfVersion = new JComboBox(NetcdfFileWriter.Version.values());
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
        label1.setText("Output Filename:");

        //---- label2 ----
        label2.setText("NetCDF Format:");

        GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(
          contentPanelLayout.createParallelGroup()
            .addGroup(contentPanelLayout.createSequentialGroup()
              .addGroup(contentPanelLayout.createParallelGroup()
                .addGroup(contentPanelLayout.createSequentialGroup()
                  .addComponent(label1)
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(outputFilename, GroupLayout.DEFAULT_SIZE, 466, Short.MAX_VALUE))
                .addGroup(contentPanelLayout.createSequentialGroup()
                  .addComponent(label2)
                  .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(netcdfVersion, GroupLayout.PREFERRED_SIZE, 243, GroupLayout.PREFERRED_SIZE)
                  .addGap(0, 222, Short.MAX_VALUE)))
              .addContainerGap())
        );
        contentPanelLayout.setVerticalGroup(
          contentPanelLayout.createParallelGroup()
            .addGroup(contentPanelLayout.createSequentialGroup()
              .addContainerGap()
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(label1)
                .addComponent(outputFilename, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
              .addGap(18, 18, 18)
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(label2)
                .addComponent(netcdfVersion, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
              .addContainerGap(28, Short.MAX_VALUE))
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
  private JTextField outputFilename;
  private JLabel label2;
  private JComboBox netcdfVersion;
  private JPanel buttonBar;
  private JButton okButton;
  private JButton cancelButton;
  // JFormDesigner - End of variables declaration  //GEN-END:variables
}
