package com.sematek;

import com.fazecast.jSerialComm.SerialPort;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeriesDataItem;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SerialReader implements Runnable {
    SerialPort comPort;
    static final String requestReading = "$DA02?\r";
    static long lastRead;

    SerialReader() {
        lastRead = System.currentTimeMillis();
        initSerialReader();
    }

    public void run() {
        while (true){
            if (comPort.isOpen()) {
                readSerial();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    boolean initSerialReader () {
            comPort = SerialPort.getCommPorts()[0];
            comPort.setComPortParameters(38400, 8, 1, 0);
            comPort.openPort();

            if (comPort.openPort()) {
                System.out.println("Port is open :)");
                return true;
            } else {
                System.out.println("Failed to open port :(");
                return false;
            }
        }

        void readSerial () {
            if (comPort.openPort()) {
                try {
                    comPort.writeBytes(requestReading.getBytes(), requestReading.getBytes().length);
                    while (comPort.bytesAvailable() == 0)
                        Thread.sleep(20);
                        processReadData(new byte[comPort.bytesAvailable()]);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


    TimeSeriesDataItem addDataToGraph(long val) {
        return new TimeSeriesDataItem(new Millisecond(),val);
    }

    void processReadData (byte[] readBuffer) throws UnsupportedEncodingException {
        int numRead = comPort.readBytes(readBuffer, readBuffer.length);
        String data = new String(readBuffer, "ASCII");

        String pattern = "(?<=.{5})[1-9]([0-9]*)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(data);
        if (m.find()) {
            System.out.println("Found value " + m.group(0) + " at time " + ((System.currentTimeMillis() - lastRead)/1000) + "s");
            addDataToGraph(Long.parseLong(m.group(0)));
        }
    }

    void closePort() {
        comPort.closePort();

        if (comPort.closePort()) {
            System.out.println("Port is closed :)");
        } else {
            System.out.println("Failed to close port :(");
        }

    }
}


