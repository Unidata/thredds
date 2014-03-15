/* This file is Copyright (c) 2012 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.util;

import java.io.OutputStream;

/**
 * An outputStream that sends messages to String2.log. 
 */
public class String2LogOutputStream extends OutputStream {

    //use StringBuffer (not StringBuilder) for thread safety
    StringBuffer sb = new StringBuffer();

    public void write(int b) {
        if (b <= 13) { //one test
            if (b == 10) { // \n
                String2.log(sb.toString());
                sb.setLength(0);
                return;
            }

            if (b == 13)  // \r
                return;

            //else fall through
        }

        sb.append((char)b);
    }

} //End of String2LogOutputStream class.
