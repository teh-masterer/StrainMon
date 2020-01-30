package com.sematek;

import com.fazecast.jSerialComm.SerialPort;
import org.jfree.data.time.Millisecond;
import java.io.InvalidObjectException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.awt.EventQueue.invokeLater;


public class SerialReader implements Runnable {

    private final AtomicBoolean running = new AtomicBoolean(false);
    final double CORRECTION_FACTOR = 1.505706984;
    final double CORRECTION_OFFSET = 140;
    static final int SMOOTHING_BUFFER_LENGTH = 15;
    static final int SERIAL_GET_DATA_INTERVAL = 154; // 6.5Hz refresh rate is equal to ~154ms
    static final int BAUD_RATE = 38400;
    static final int DATA_BITS = 8;
    static final int STOP_BITS = 1;
    static final int PARITY_BITS = 0;
    static final String requestReading = "$DA01?\r";
    static long lastReadTime;
    static boolean smoothGraph;
    boolean activateZeroBalance;
    double[] recentValues;
    static double offset;

    SerialPort comPort;
    final Grapher g;

    SerialReader(Grapher g) {
        this.g = g;
        offset = 0;
        smoothGraph = false;
        lastReadTime = System.currentTimeMillis();
        initSerialReader();
        recentValues = new double[SMOOTHING_BUFFER_LENGTH];
        Arrays.fill(recentValues, 0);
        currentMax = 0;
        activateZeroBalance = false;
    }

    public void run() {
        running.set(true); // setting a boolean so that the GUI can know if running, and kill the process
        while (running.get()){
            if (comPort.isOpen()) {
                readSerial();
                try {
                    Thread.sleep(SERIAL_GET_DATA_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("Thread was interrupted!");

                }
            }
        }
        System.out.println("Running parameter is now set to " + running.get());
    }
    public void end() {
        running.set(false);
        closePort();
    }

    void initSerialReader () {
            comPort = SerialPort.getCommPorts()[0]; //Open whatever's available
            comPort.setComPortParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY_BITS);
            comPort.openPort();

            if (comPort.openPort()) {
                System.out.println("Port is open :)");
            } else {
                System.out.println("Failed to open port :(");
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

    void processReadData (byte[] readBuffer) {
        comPort.readBytes(readBuffer, readBuffer.length);
        String data = new String(readBuffer, StandardCharsets.US_ASCII);

        String pattern = "(?<=.{5})[1-9]([0-9]*)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(data);
        if (m.find()) {
            double correctedValue = Double.parseDouble(m.group(0))/CORRECTION_FACTOR+CORRECTION_OFFSET; //All values are corrected by a a*x+c factor found by experimentation
            System.out.println("Found value " + m.group(0) + " / " + correctedValue + " at time " + ((System.currentTimeMillis() - lastReadTime) / 1000) + "s");
            if (smoothGraph || activateZeroBalance) {
                double rollingSum = 0;
                for (int i = recentValues.length - 2; i >= 0; i--) {
                    recentValues[i + 1] = recentValues[i];
                    rollingSum += recentValues[i+1];
                }
                rollingSum += recentValues[0] = correctedValue;

                if (recentValues[recentValues.length - 1] != 0) {
                    double dataToBeInserted = rollingSum / recentValues.length;
                    if (activateZeroBalance) {
                        offset = dataToBeInserted;
                        activateZeroBalance = false;
                        g.labelOffsetValue.setText(String.valueOf(offset));
                    }
                    if (smoothGraph) {
                        addDataToGraph(dataToBeInserted - offset, correctedValue - offset);
                    }
                }

            } else {
                addDataToGraph(correctedValue - offset);
            }
        }
    }

    void addDataToGraph(double val) { //Without smooth graph
        final double finalVal = round(val,2);
        invokeLater(() -> {
            g.series.add(new Millisecond(),val);
            updateValueLabels(val);
        });
    }

    void addDataToGraph(double val, double rawVal) { //For smooth graph
        final double finalVal = round(rawVal,2);
        invokeLater(() -> {
            g.series.add(new Millisecond(),(int) val);
            updateValueLabels(rawVal);
        });
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private void updateValueLabels (double val) {
        g.labelCurrentValue.setText(String.valueOf(val));
        if (currentMax< val) {
            g.labelMaxValue.setText(String.valueOf(val));
            currentMax = val;
        }
        try {
            g.dataset.validateObject();
        } catch (InvalidObjectException e) {
            e.printStackTrace();
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
    public boolean getRunning() {
        return running.get();
    }

    public void setCurrentMax(double currentMax) {
        this.currentMax = currentMax;
    }

    double currentMax;

    public void setActivateZeroBalance(boolean activateZeroBalance) {
        this.activateZeroBalance = activateZeroBalance;
    }

    public static boolean isSmoothGraph() {
        return smoothGraph;
    }

    public static void setSmoothGraph(boolean smoothGraph) {
        SerialReader.smoothGraph = smoothGraph;
    }
}

