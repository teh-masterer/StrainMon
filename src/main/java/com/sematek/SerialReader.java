package com.sematek;

import com.fazecast.jSerialComm.SerialPort;
import org.jfree.data.time.Millisecond;
import java.io.InvalidObjectException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.awt.EventQueue.invokeLater;


public class SerialReader implements Runnable {
    private final AtomicBoolean running = new AtomicBoolean(false);
    SerialPort comPort;
    Grapher g;
    static final String requestReading = "$DA02?\r";
    static long lastRead;

    public static double getOffset() {
        return offset;
    }

    public static void setOffset(double offset) {
        SerialReader.offset = offset;
    }

    static double offset;

    SerialReader(Grapher g) {
        this.g = g;
        offset = 0;
        lastRead = System.currentTimeMillis();
        initSerialReader();
    }

    public void run() {
        running.set(true);
        while (running.get()){
            if (comPort.isOpen()) {
                readSerial();
                try {
                    Thread.sleep(154);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("Thread was interrupted, possibly by user activated Stop button");

                }
            }
        }
    }
    public void end() {
        running.set(false);
        closePort();
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


    void addDataToGraph(long val) {
        invokeLater(() -> {
            g.series.add(new Millisecond(),val);
            g.l2.setText(String.valueOf(val));
            if (Long.parseLong(g.l5.getText())<val) {
                g.l5.setText(String.valueOf(val));
            }
            try {
                g.dataset.validateObject();
            } catch (InvalidObjectException e) {
                e.printStackTrace();
            }
                });
    }

    void processReadData (byte[] readBuffer) throws UnsupportedEncodingException {
        int numRead = comPort.readBytes(readBuffer, readBuffer.length);
        String data = new String(readBuffer, "ASCII");

        String pattern = "(?<=.{5})[1-9]([0-9]*)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(data);
        if (m.find()) {
            System.out.println("Found value " + m.group(0) + " at time " + ((System.currentTimeMillis() - lastRead)/1000) + "s");
            addDataToGraph((long) (Long.parseLong(m.group(0))-offset));
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


