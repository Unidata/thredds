/*
 * Created by JFormDesigner on Sun May 02 18:11:58 MDT 2010
 */

package thredds.ui.monitor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import javax.swing.*;

import ucar.nc2.ui.widget.StopButton;

/**
 * @author John Caron
 */
public class ManageForm extends JPanel {
  java.util.List<String> servers = new ArrayList<String>();

  public class Data {
    public String server;
    public boolean wantAccess, wantServlet, wantRoots;

    private Data(String server, boolean access, boolean servlet, boolean roots) {
      this.server = server;
      this.wantAccess = access;
      this.wantServlet = servlet;
      this.wantRoots = roots;
    }
  }

  public ManageForm(List<String> servers) {
    this.servers = servers;
    initComponents();
    serverCB.setModel( new DefaultComboBoxModel(servers.toArray()));
  }

  public JTextArea getTextArea() {
    return textArea1;
  }

  public StopButton getStopButton() {
    return stopButton;
  }

  public ComboBoxModel getServersCB() {
    return serverCB.getModel();
  }

  public void setServers(java.util.List servers) {
    serverCB.setModel( new DefaultComboBoxModel(servers.toArray()));
  }

  public ManageForm() {
    initComponents();
  }

  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    label1 = new JLabel();
    serverCB = new JComboBox();
    wantAccess = new JRadioButton();
    wantServlet = new JRadioButton();
    acceptButton = new JButton();
    label2 = new JLabel();
    scrollPane1 = new JScrollPane();
    textArea1 = new JTextArea();
    wantRoots = new JRadioButton();
    stopButton = new StopButton();
    downloadAction = new DownloadAction();

    //======== this ========

    //---- label1 ----
    label1.setText("server:");
    label1.setFont(label1.getFont().deriveFont(Font.BOLD|Font.ITALIC));

    //---- serverCB ----
    serverCB.setModel(new DefaultComboBoxModel(new String[] {
      "localhost:8080"
    }));
    serverCB.setEditable(true);

    //---- wantAccess ----
    wantAccess.setText("access logs");
    wantAccess.setFont(wantAccess.getFont().deriveFont(wantAccess.getFont().getStyle() | Font.BOLD));

    //---- wantServlet ----
    wantServlet.setText("servlet logs");
    wantServlet.setFont(wantServlet.getFont().deriveFont(wantServlet.getFont().getStyle() | Font.BOLD));

    //---- acceptButton ----
    acceptButton.setAction(downloadAction);

    //---- label2 ----
    label2.setText("Download log files:");
    label2.setFont(label2.getFont().deriveFont(label2.getFont().getStyle() | Font.BOLD, label2.getFont().getSize() + 2f));

    //======== scrollPane1 ========
    {

      //---- textArea1 ----
      textArea1.setFont(new Font("Courier New", Font.PLAIN, 12));
      scrollPane1.setViewportView(textArea1);
    }

    //---- wantRoots ----
    wantRoots.setText("data roots");
    wantRoots.setFont(wantRoots.getFont().deriveFont(wantRoots.getFont().getStyle() | Font.BOLD));

    //---- stopButton ----
    stopButton.setText("Stop");

    GroupLayout layout = new GroupLayout(this);
    setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup()
        .addGroup(layout.createSequentialGroup()
          .addGroup(layout.createParallelGroup()
            .addGroup(layout.createSequentialGroup()
              .addGap(39, 39, 39)
              .addGroup(layout.createParallelGroup()
                .addGroup(layout.createSequentialGroup()
                  .addComponent(acceptButton)
                  .addGap(64, 64, 64)
                  .addComponent(stopButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addComponent(label2)
                .addGroup(layout.createSequentialGroup()
                  .addComponent(label1, GroupLayout.PREFERRED_SIZE, 55, GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup()
                    .addComponent(serverCB, GroupLayout.PREFERRED_SIZE, 294, GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                      .addComponent(wantAccess)
                      .addGap(77, 77, 77)
                      .addComponent(wantRoots))
                    .addComponent(wantServlet)))))
            .addGroup(layout.createSequentialGroup()
              .addGap(15, 15, 15)
              .addComponent(scrollPane1, GroupLayout.DEFAULT_SIZE, 649, Short.MAX_VALUE)))
          .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup()
        .addGroup(layout.createSequentialGroup()
          .addGap(21, 21, 21)
          .addComponent(label2)
          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(serverCB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addComponent(label1))
          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(wantAccess)
            .addComponent(wantRoots))
          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addComponent(wantServlet)
          .addGap(18, 18, 18)
          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(acceptButton)
            .addComponent(stopButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
          .addGap(18, 18, 18)
          .addComponent(scrollPane1, GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
          .addContainerGap())
    );
    // JFormDesigner - End of component initialization  //GEN-END:initComponents
  }

  // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
  // Generated using JFormDesigner non-commercial license
  private JLabel label1;
  private JComboBox serverCB;
  private JRadioButton wantAccess;
  private JRadioButton wantServlet;
  private JButton acceptButton;
  private JLabel label2;
  private JScrollPane scrollPane1;
  private JTextArea textArea1;
  private JRadioButton wantRoots;
  private StopButton stopButton;
  private DownloadAction downloadAction;
  // JFormDesigner - End of variables declaration  //GEN-END:variables

  private class DownloadAction extends AbstractAction {
    private DownloadAction() {
      // JFormDesigner - Action initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
      // Generated using JFormDesigner non-commercial license
      putValue(NAME, "Download");
      // JFormDesigner - End of action initialization  //GEN-END:initComponents
    }

    public void actionPerformed(ActionEvent e) {
      String server = (String) serverCB.getSelectedItem();
      Data data =  new Data(server, wantAccess.isSelected(), wantServlet.isSelected(),
              wantRoots.isSelected());
      ManageForm.this.firePropertyChange("Download", null, data);
    }
  }
}
