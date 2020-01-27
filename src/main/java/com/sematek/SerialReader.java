package com.sematek;
import com.fazecast.jSerialComm.SerialPort;
import org.jfree.data.time.Millisecond;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Thread.yield;

public class SerialReader implements Runnable {
    Grapher grapher;
    SerialPort comPort;
    boolean initSerialReaderState;
    static final String requestReading = "$DA02?\r";
    private static long start;
    private static long lastRead;
    static HashMap<Long, Long> dataStorage = new HashMap<Long, Long>();

    SerialReader(Grapher grapher) throws IOException, InterruptedException {
        this.grapher = grapher;
        initSerialReaderState = initSerialReader();
        lastRead = System.currentTimeMillis();
    }

    @Override
    public void run() {
        if (System.currentTimeMillis() > (lastRead + 2000) && initSerialReaderState) {
            readSerial();
            System.out.println("Inside SerialReader run, last ran at " + (System.currentTimeMillis()-lastRead) + "ms ago.");

            lastRead = System.currentTimeMillis();
        } else {
            yield();
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
            start = System.currentTimeMillis();
            try {
                comPort.writeBytes(requestReading.getBytes(), requestReading.getBytes().length);
                while (comPort.bytesAvailable() == 0)
                processReadData(new byte[comPort.bytesAvailable()]);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    void addDataToGraph(long val) {
        grapher.series.add(new Millisecond(),val);

    }

    void processReadData (byte[] readBuffer) throws UnsupportedEncodingException {
        int numRead = comPort.readBytes(readBuffer, readBuffer.length);
        System.out.println("Read " + numRead + " bytes.");
        String data = new String(readBuffer, "ASCII");
        System.out.println("Data:" + data);

        String pattern = "(?<=.{5})[1-9]([0-9]*)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(data);
        if (m.find()) {
            System.out.println("Found value: " + m.group(0));
            dataStorage.put(Long.parseLong(m.group(0)), System.currentTimeMillis() - start);
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


