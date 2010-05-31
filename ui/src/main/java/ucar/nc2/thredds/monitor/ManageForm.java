/*
 * Created by JFormDesigner on Sun May 02 18:11:58 MDT 2010
 */

package ucar.nc2.thredds.monitor;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import ucar.nc2.ui.*;

/**
 * @author John Caron
 */
public class ManageForm extends JPanel {

  public class Data {
    public String server;
    public boolean wantAccess, wantServlet;

    private Data(String server, boolean access, boolean servlet) {
      this.server = server;
      this.wantAccess = access;
      this.wantServlet = servlet;
    }
  }

  public JTextArea getTextArea() {
    return textArea1;
  }

  public StopButton getStopButton() {
    return stopButton;
  }

  public JComboBox getServers() {
    return server;
  }

  public ManageForm() {
    initComponents();
  }

  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    label1 = new JLabel();
    server = new JComboBox();
    wantAccess = new JRadioButton();
    wantServlet = new JRadioButton();
    acceptButton = new JButton();
    label2 = new JLabel();
    scrollPane1 = new JScrollPane();
    textArea1 = new JTextArea();
    stopButton = new StopButton();
    downloadAction = new DownloadAction();

    //======== this ========

    //---- label1 ----
    label1.setText("server:");
    label1.setFont(label1.getFont().deriveFont(Font.BOLD|Font.ITALIC));

    //---- server ----
    server.setModel(new DefaultComboBoxModel(new String[] {
      "motherlode.ucar.edu:8080",
      "motherlode.ucar.edu:8081",
      "motherlode.ucar.edu:9080"
    }));

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

    //---- stopButton ----
    stopButton.setToolTipText("stop download");

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
                  .addGap(18, 18, 18)
                  .addComponent(stopButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addComponent(label2)
                .addGroup(layout.createSequentialGroup()
                  .addComponent(label1, GroupLayout.PREFERRED_SIZE, 55, GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup()
                    .addComponent(server, GroupLayout.PREFERRED_SIZE, 294, GroupLayout.PREFERRED_SIZE)
                    .addComponent(wantAccess)
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
            .addComponent(server, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addComponent(label1))
          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addComponent(wantAccess)
          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addComponent(wantServlet)
          .addGap(18, 18, 18)
          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(acceptButton)
            .addComponent(stopButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
          .addGap(18, 18, 18)
          .addComponent(scrollPane1, GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE)
          .addContainerGap())
    );
    // JFormDesigner - End of component initialization  //GEN-END:initComponents
  }

  // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
  // Generated using JFormDesigner non-commercial license
  private JLabel label1;
  private JComboBox server;
  private JRadioButton wantAccess;
  private JRadioButton wantServlet;
  private JButton acceptButton;
  private JLabel label2;
  private JScrollPane scrollPane1;
  private JTextArea textArea1;
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
      Data data =  new Data((String) server.getSelectedItem(), wantAccess.isSelected(), wantServlet.isSelected());
      ManageForm.this.firePropertyChange("Download", null, data);
    }
  }
}
