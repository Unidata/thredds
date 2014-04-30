package com.cohort.util;

import java.io.*;

/**
 * This class attempts to erase characters echoed to the console.
 * This is slighly modified from 
 * http://java.sun.com/developer/technicalArticles/Security/pwordmask/ .
 */
public class MaskingThread extends Thread {
    private volatile boolean keepGoing;
    private char echochar = '*'; 

   /**
    *@param prompt The prompt displayed to the user
    */
    public MaskingThread(String prompt) {
        System.out.print(prompt + ' ');
    }

   /**
    * Begin masking until asked to keepGoing.
    */
    public void run() {
 
        int priority = Thread.currentThread().getPriority();
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        try {
            keepGoing = true;
            while(keepGoing) {
                System.out.print("\010" + echochar);
                try {
                    //attempt masking at this rate
                    Thread.sleep(1);
                } catch (InterruptedException iex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } finally { // restore the original priority
            Thread.currentThread().setPriority(priority);
        }
    }

    /**
     * Instruct the thread to stop masking.
     */
    public void stopMasking() {
        this.keepGoing = false;
    }
}