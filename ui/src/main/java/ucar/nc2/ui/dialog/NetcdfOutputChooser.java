/*
 * Created by JFormDesigner on Thu Nov 15 14:02:02 MST 2012
 */

package ucar.nc2.ui.dialog;

import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.util.ListenerManager;
import ucar.nc2.write.Nc4Chunking;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author John Caron
 */
public class NetcdfOutputChooser extends JDialog {
  private ListenerManager lm = new ListenerManager("java.awt.event.ItemListener", "java.awt.event.ItemEvent", "itemStateChanged");

  public NetcdfOutputChooser(Frame owner) {
    super(owner);
    initComponents();
  }

  public NetcdfOutputChooser(Dialog owner) {
    super(owner);
    initComponents();
  }

  public void setOutputFilename(String filename) {
    if (filename == null) filename = "test";
    String location = filename;
    if (location.startsWith("file:")) location = location.substring(5);
    int pos = location.lastIndexOf(".");
    if (pos > 0)
      location = location.substring(0, pos);

    // change suffix
    NetcdfFileWriter.Version version = (NetcdfFileWriter.Version) netcdfVersion.getSelectedItem();
    String suffix = (version == null) ? ".nc" : version.getSuffix();
    if (filename.endsWith(".nc") && suffix.equals(".nc"))
      suffix = ".sub.nc";
    location += suffix;

    outputFilename.setText(location);
  }

  public static class Data {
    public String outputFilename;
    public NetcdfFileWriter.Version version;
    public Nc4Chunking.Strategy chunkerType;
    public int deflate;
    public boolean shuffle;

    private Data(String outputFilename, NetcdfFileWriter.Version version, Nc4Chunking.Strategy chunkerType,
                 boolean deflate, boolean shuffle) {
      this.outputFilename = outputFilename;
      this.version = version;
      this.chunkerType = chunkerType;
      this.deflate = deflate ? 5 : 0;
      this.shuffle = shuffle;
    }
  }

  private void okButtonActionPerformed(ActionEvent e) {
    Data data = new Data(outputFilename.getText(),
            (NetcdfFileWriter.Version) netcdfVersion.getSelectedItem(),
            (Nc4Chunking.Strategy) chunking.getSelectedItem(),
            deflate.isSelected(), shuffle.isSelected());
    firePropertyChange("OK", null, data);
    setVisible(false);
  }

  private void createUIComponents() {
    // TODO: add custom component creation code here
  }

  public void addEventListener(ItemListener l) {
    lm.addListener(l);
  }

  public void removeEventListener(ItemListener l) {
    lm.removeListener(l);
  }

  private void chunkingItemStateChanged(ItemEvent e) {
    lm.sendEvent(e);
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
    outputFilename = new JTextField();
    label2 = new JLabel();
    netcdfVersion = new JComboBox(NetcdfFileWriter.Version.values());
    panel1 = new JPanel();
    label3 = new JLabel();
    chunking = new JComboBox(Nc4Chunking.Strategy.values());
    deflate = new JCheckBox();
    shuffle = new JCheckBox();
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

        //======== panel1 ========
        {
          panel1.setBorder(new TitledBorder(null, "netCDF4 options", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
          panel1.setLayout(new GridBagLayout());
          ((GridBagLayout)panel1.getLayout()).columnWidths = new int[] {0, 0, 0};
          ((GridBagLayout)panel1.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
          ((GridBagLayout)panel1.getLayout()).columnWeights = new double[] {0.0, 1.0, 1.0E-4};
          ((GridBagLayout)panel1.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0, 1.0E-4};

          //---- label3 ----
          label3.setText("Chunking:");
          panel1.add(label3, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

          //---- chunking ----
          chunking.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
              chunkingItemStateChanged(e);
            }
          });
          panel1.add(chunking, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

          //---- deflate ----
          deflate.setText("deflate");
          panel1.add(deflate, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

          //---- shuffle ----
          shuffle.setText("shuffle");
          panel1.add(shuffle, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        }

        GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(
          contentPanelLayout.createParallelGroup()
            .addGroup(contentPanelLayout.createSequentialGroup()
              .addGroup(contentPanelLayout.createParallelGroup()
                .addGroup(contentPanelLayout.createSequentialGroup()
                  .addComponent(label1)
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(outputFilename, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE))
                .addGroup(contentPanelLayout.createSequentialGroup()
                  .addGroup(contentPanelLayout.createParallelGroup()
                    .addGroup(contentPanelLayout.createSequentialGroup()
                      .addGap(18, 18, 18)
                      .addComponent(label2)
                      .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                      .addComponent(netcdfVersion, GroupLayout.PREFERRED_SIZE, 243, GroupLayout.PREFERRED_SIZE))
                    .addGroup(contentPanelLayout.createSequentialGroup()
                      .addGap(33, 33, 33)
                      .addComponent(panel1, GroupLayout.PREFERRED_SIZE, 383, GroupLayout.PREFERRED_SIZE)))
                  .addGap(0, 143, Short.MAX_VALUE)))
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
              .addGap(18, 18, 18)
              .addComponent(panel1, GroupLayout.PREFERRED_SIZE, 86, GroupLayout.PREFERRED_SIZE)
              .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
      }
      dialogPane.add(contentPanel, BorderLayout.CENTER);

      //======== buttonBar ========
      {
        buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
        buttonBar.setLayout(new FlowLayout(FlowLayout.LEFT));

        //---- okButton ----
        okButton.setText("Write File");
        okButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            okButtonActionPerformed(e);
          }
        });
        buttonBar.add(okButton);

        //---- cancelButton ----
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            cancelButtonActionPerformed(e);
          }
        });
        buttonBar.add(cancelButton);
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
  private JPanel panel1;
  private JLabel label3;
  private JComboBox chunking;
  private JCheckBox deflate;
  private JCheckBox shuffle;
  private JPanel buttonBar;
  private JButton okButton;
  private JButton cancelButton;
  // JFormDesigner - End of variables declaration  //GEN-END:variables
}
