/*
 * Created by JFormDesigner on Sun May 02 18:11:58 MDT 2010
 */

package thredds.ui.monitor;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

import ucar.nc2.ui.widget.StopButton;
import ucar.util.prefs.PersistenceManager;
import ucar.util.prefs.ui.*;

/**
 * @author John Caron
 */
public class ManageForm extends JPanel {

  public class Data {
    public String server;
    public boolean wantAccess, wantServlet, wantRoots, useHttps;

    private Data(String server, boolean access, boolean servlet, boolean roots, boolean useHttps) {
      this.server = server == null ? "" : server.trim();
      this.wantAccess = access;
      this.wantServlet = servlet;
      this.wantRoots = roots;
      this.useHttps = useHttps;
    }

    public String getServerPrefix() {
      return useHttps ? "https://" : "http://" + server;
    }
  }

  public ManageForm(PersistenceManager prefs) {
    initComponents();
    serverCB.setPreferences( prefs);
  }

  public JTextArea getTextArea() {
    return textArea1;
  }

  public StopButton getStopButton() {
    return stopButton;
  }

  public ComboBox getServersCB() {
    return serverCB;
  }

  public ManageForm() {
    initComponents();
  }

  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    label1 = new JLabel();
    wantAccess = new JRadioButton();
    wantServlet = new JRadioButton();
    acceptButton = new JButton();
    label2 = new JLabel();
    scrollPane1 = new JScrollPane();
    textArea1 = new JTextArea();
    wantRoots = new JRadioButton();
    stopButton = new StopButton();
    serverCB = new ComboBox();
    useHttps = new JToggleButton();
    downloadAction = new DownloadAction();

    //======== this ========

    //---- label1 ----
    label1.setText("server:");
    label1.setFont(label1.getFont().deriveFont(Font.BOLD|Font.ITALIC));

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

    //---- useHttps ----
    useHttps.setText("Use https:");

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
                    .addGroup(layout.createSequentialGroup()
                      .addComponent(serverCB, GroupLayout.PREFERRED_SIZE, 283, GroupLayout.PREFERRED_SIZE)
                      .addGap(18, 18, 18)
                      .addComponent(useHttps))
                    .addGroup(layout.createSequentialGroup()
                      .addGroup(layout.createParallelGroup()
                        .addComponent(wantServlet)
                        .addComponent(wantAccess))
                      .addGap(30, 30, 30)
                      .addComponent(wantRoots)))))
              .addGap(223, 223, 223))
            .addGroup(layout.createSequentialGroup()
              .addGap(15, 15, 15)
              .addComponent(scrollPane1, GroupLayout.DEFAULT_SIZE, 696, Short.MAX_VALUE)))
          .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup()
        .addGroup(layout.createSequentialGroup()
          .addGap(21, 21, 21)
          .addComponent(label2)
          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(label1)
            .addComponent(serverCB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addComponent(useHttps))
          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(wantRoots)
            .addComponent(wantAccess))
          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addComponent(wantServlet)
          .addGap(18, 18, 18)
          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(acceptButton)
            .addComponent(stopButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
          .addGap(18, 18, 18)
          .addComponent(scrollPane1, GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
          .addContainerGap())
    );
    // JFormDesigner - End of component initialization  //GEN-END:initComponents
  }

  // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
  // Generated using JFormDesigner non-commercial license
  private JLabel label1;
  private JRadioButton wantAccess;
  private JRadioButton wantServlet;
  private JButton acceptButton;
  private JLabel label2;
  private JScrollPane scrollPane1;
  private JTextArea textArea1;
  private JRadioButton wantRoots;
  private StopButton stopButton;
  private ComboBox serverCB;
  private JToggleButton useHttps;
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
      serverCB.addItem(server);
      Data data =  new Data(server, wantAccess.isSelected(), wantServlet.isSelected(), wantRoots.isSelected(), useHttps.isSelected());
      ManageForm.this.firePropertyChange("Download", null, data);
    }
  }
}
