package ucar.unidata.util;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: May 3, 2007
 * Time: 11:37:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class DateSelectionInfo {
    public Date start;
    public Date end;
    public int interval;
    public int roundTo;
    public int preInt;
    public int postInt;

    public DateSelectionInfo(){
        this.start = null;
        this.end = null;
        this.interval = 0;
        this.roundTo = 0;
        this.preInt = 0;
        this.postInt = 0;
    }

    public DateSelectionInfo(Date start, Date end, int interval, int roundTo, int preInt, int postInt){
        this.start = start;
        this.end = end;
        this.interval = interval;
        this.roundTo = roundTo;
        this.preInt = preInt;
        this.postInt = postInt;
    }

    public DateSelectionInfo(Date start, Date end){
        this.start = start;
        this.end = end;
        this.interval = 0;
        this.roundTo = 0;
        this.preInt = 0;
        this.postInt = 0;
    }

    public boolean equals(Object oo) {
        if (this == oo) {
            return true;
        }
        if ( !(oo instanceof DateSelectionInfo)) {
            return false;
        }
        return false;
    }

    public Date setStartDateInfo() {
        return this.start;
    }

    public Date setEndDateInfo() {
        return this.end;
    }
    
}
