/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.ui.OpPanel;
import ucar.ui.widget.TextHistoryPane;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.units.TimeUnit;
import ucar.ui.prefs.Debug;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.JButton;

/**
 *
 */
public class UnitConvert extends OpPanel {
    private TextHistoryPane ta;

    private DateFormatter formatter = new DateFormatter();

/**
 *
 */
    public UnitConvert(final PreferencesExt prefs) {
        super(prefs, "unit:", false, false);

        ta = new TextHistoryPane(true);
        add(ta, BorderLayout.CENTER);

        final JButton compareButton = new JButton("Compare");
        compareButton.addActionListener(e -> compare(cb.getSelectedItem()));
        buttPanel.add(compareButton);

        final JButton dateButton = new JButton("UdunitDate");
        dateButton.addActionListener(e -> checkUdunits(cb.getSelectedItem()));
        buttPanel.add(dateButton);

        final JButton cdateButton = new JButton("CalendarDate");
        cdateButton.addActionListener(e -> checkCalendarDate(cb.getSelectedItem()));
        buttPanel.add(cdateButton);
    }

/** */
    @Override
    public boolean process(Object o) {
        final String command = (String) o;
        try {
            final SimpleUnit su = SimpleUnit.factoryWithExceptions(command);
            ta.setText("parse=" + command + "\n");
            ta.appendLine("SimpleUnit.toString()          =" + su.toString() + "\n");
            ta.appendLine("SimpleUnit.getCanonicalString  =" + su.getCanonicalString());
            ta.appendLine("SimpleUnit.getImplementingClass= " + su.getImplementingClass());
            ta.appendLine("SimpleUnit.isUnknownUnit       = " + su.isUnknownUnit());

            return true;
        }
        catch (Exception e) {
            if (Debug.isSet("Xdeveloper")) {
                final StringWriter sw = new StringWriter(10000);
                e.printStackTrace(new PrintWriter(sw));
                ta.setText(sw.toString());
            }
            else {
                ta.setText(e.getClass().getName() + ":" + e.getMessage() + "\n" + command);
            }
            return false;
        }
    }

/** */
    @Override
    public void closeOpenFiles() {
        // Nothing to do here.
    }

/**
 *
 */
    public void compare(Object o) {
        final String command = (String) o;
        final StringTokenizer stoke = new StringTokenizer(command);
        final List<String> list = new ArrayList<>();

        while (stoke.hasMoreTokens()) {
            list.add(stoke.nextToken());
        }

        try {
            final String unitS1 = list.get(0);
            final String unitS2 = list.get(1);
            final SimpleUnit su1 = SimpleUnit.factoryWithExceptions(unitS1);
            final SimpleUnit su2 = SimpleUnit.factoryWithExceptions(unitS2);
            ta.setText("<" + su1.toString() + "> isConvertable to <" + su2.toString() + ">=" +
                SimpleUnit.isCompatibleWithExceptions(unitS1, unitS2));

        }
        catch (final Exception e) {
            if (Debug.isSet("Xdeveloper")) {
                final StringWriter sw = new StringWriter(10000);
                e.printStackTrace(new PrintWriter(sw));
                ta.setText(sw.toString());
            }
            else {
                ta.setText(e.getClass().getName() + ":" + e.getMessage() + "\n" + command);
            }
        }
    }

/**
 *
 */
    private void checkUdunits(Object o) {
        String command = (String) o;

        boolean isDate = false;
        try {
            final DateUnit du = new DateUnit(command);
            ta.appendLine("\nFrom udunits:\n <" + command + "> isDateUnit = " + du);
            final Date d = du.getDate();
            ta.appendLine("getStandardDateString = " + formatter.toDateTimeString(d));
            ta.appendLine("getDateOrigin = " + formatter.toDateTimeString(du.getDateOrigin()));
            isDate = true;

            final Date d2 = DateUnit.getStandardOrISO(command);
            if (d2 == null) {
                ta.appendLine("\nDateUnit.getStandardOrISO = false");
            }
            else {
                ta.appendLine("\nDateUnit.getStandardOrISO = " + formatter.toDateTimeString(d2));
            }
        }
        catch (Exception e) {
          // ok to fall through
        }
        ta.appendLine("isDate = " + isDate);

        if (!isDate) {
            try {
                final SimpleUnit su = SimpleUnit.factory(command);
                final boolean isTime = su instanceof TimeUnit;
                ta.setText("<" + command + "> isTimeUnit= " + isTime);
                if (isTime) {
                    TimeUnit du = (TimeUnit) su;
                    ta.appendLine("\nTimeUnit = " + du);
                }
            }
            catch (final Exception e) {
                if (Debug.isSet("Xdeveloper")) {
                    final StringWriter sw = new StringWriter(10000);
                    e.printStackTrace(new PrintWriter(sw));
                    ta.setText(sw.toString());
                }
                else {
                    ta.setText(e.getClass().getName() + ":" + e.getMessage() + "\n" + command);
                }
            }
        }
    }

/**
 *
 */
    private void checkCalendarDate(Object o) {
        String command = (String) o;

        try {
            ta.setText("\nParse CalendarDate: <" + command + ">\n");
            CalendarDate cd = CalendarDate.parseUdunits(null, command);
            ta.appendLine("CalendarDate = " + cd);
        }
        catch (Throwable t) {
            ta.appendLine("not a CalendarDateUnit= " + t.getMessage());
        }

        try {
            /* int pos = command.indexOf(' ');
            if (pos < 0) return;
            String valString = command.substring(0, pos).trim();
            String unitString = command.substring(pos+1).trim();  */

            ta.appendLine("\nParse CalendarDateUnit: <" + command + ">\n");

            CalendarDateUnit cdu = CalendarDateUnit.of(null, command);
            ta.appendLine("CalendarDateUnit = " + cdu);
            ta.appendLine(" Calendar        = " + cdu.getCalendar());
            ta.appendLine(" PeriodField     = " + cdu.getCalendarPeriod().getField());
            ta.appendLine(" PeriodValue     = " + cdu.getCalendarPeriod().getValue());
            ta.appendLine(" Base            = " + cdu.getBaseCalendarDate());
            ta.appendLine(" isCalendarField = " + cdu.isCalendarField());
        }
        catch (Exception e) {
            ta.appendLine("not a CalendarDateUnit= " + e.getMessage());

            try {
                String[] s = command.split("%");
                if (s.length == 2) {
                    final Double val = Double.parseDouble(s[0].trim());
                    ta.appendLine("\nval= " + val + " unit=" + s[1]);
                    CalendarDateUnit cdu = CalendarDateUnit.of(null, s[1].trim());
                    ta.appendLine("CalendarDateUnit= " + cdu);
                    CalendarDate cd = cdu.makeCalendarDate(val);
                    ta.appendLine(" CalendarDate = " + cd);
                    final Date d = cd.toDate();
                    ta.appendLine(" Date.toString() = " + d);
                    DateFormatter format = new DateFormatter();
                    ta.appendLine(" DateFormatter= " + format.toDateTimeString(cd.toDate()));
                }
            }
            catch (Exception ee) {
                ta.appendLine("Failed on CalendarDateUnit " + ee.getMessage());
            }
        }
    }
}
