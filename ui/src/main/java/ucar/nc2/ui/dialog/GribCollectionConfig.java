/*
 * Created by JFormDesigner on Tue Dec 03 06:59:11 MST 2013
 */

package ucar.nc2.ui.dialog;

import java.awt.*;
import javax.swing.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import thredds.featurecollection.FeatureCollectionConfig;

/**
 * @author John Caron
 */
public class GribCollectionConfig extends JDialog {
  public GribCollectionConfig() {
    initComponents();
  }

  public FeatureCollectionConfig.GribConfig getGribConfig() {
    FeatureCollectionConfig.GribConfig config = new FeatureCollectionConfig.GribConfig();
    if (excludeZero.isSelected())
      config.setExcludeZero(true);
    return config;
  }

  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    label1 = new JLabel();
    excludeZero = new JCheckBox();

    //======== this ========
    setTitle("GribConfig");
    Container contentPane = getContentPane();
    contentPane.setLayout(new FormLayout(
      "default, $lcgap, default",
      "2*(default, $lgap), default"));

    //---- label1 ----
    label1.setText("Interval Filter");
    contentPane.add(label1, CC.xy(3, 3));

    //---- excludeZero ----
    excludeZero.setText("excludeZero");
    contentPane.add(excludeZero, CC.xy(3, 5));
    pack();
    setLocationRelativeTo(getOwner());
    // JFormDesigner - End of component initialization  //GEN-END:initComponents
  }

  // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
  // Generated using JFormDesigner non-commercial license
  private JLabel label1;
  private JCheckBox excludeZero;
  // JFormDesigner - End of variables declaration  //GEN-END:variables
}
