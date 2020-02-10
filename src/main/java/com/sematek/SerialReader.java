package com.sematek;

import com.fazecast.jSerialComm.SerialPort;

import java.io.InvalidObjectException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.trim;


public class SerialReader implements Runnable {

    private final AtomicBoolean running = new AtomicBoolean(false);
    final double CORRECTION_FACTOR;
    final double CORRECTION_OFFSET;
    final int SMOOTHING_BUFFER_LENGTH;
    final int SERIAL_GET_DATA_INTERVAL; // 6.5Hz refresh rate is equal to ~154ms
    final int BAUD_RATE;
    final int DATA_BITS;
    final int STOP_BITS;
    final int PARITY_BITS;
    boolean AUTO_ZERO_ON_START;
    final long lastReadTime;
    boolean activateZeroBalance;
    boolean paused;
    final double[] recentValues;
    double userOffset;

    String deviceId, digitalFilter, instrumentVersion, resolution; //all of these are read
    int fullScale, inputSensibility, baudRate;

    SerialPort comPort;
    final StrainTestObject sto;

    SerialReader(StrainTestObject sto) {
        Config.load("config.json");
        this.BAUD_RATE = Config.getInstance().BAUD_RATE;
        this.CORRECTION_OFFSET = Config.getInstance().CORRECTION_OFFSET;
        this.CORRECTION_FACTOR = Config.getInstance().CORRECTION_FACTOR;
        this.SERIAL_GET_DATA_INTERVAL = Config.getInstance().SERIAL_GET_DATA_INTERVAL;
        this.DATA_BITS = Config.getInstance().DATA_BITS;
        this.STOP_BITS = Config.getInstance().STOP_BITS;
        this.PARITY_BITS = Config.getInstance().PARITY_BITS;
        this.userOffset = Config.getInstance().USER_OFFSET;
        this.SMOOTHING_BUFFER_LENGTH = Config.getInstance().SMOOTHING_BUFFER_LENGTH;
        this.activateZeroBalance = Config.getInstance().AUTO_ZERO_ON_START;
        System.out.println("Loaded config file with timestamp " + Config.getInstance().TIMESTAMP);

        this.sto = sto;
        lastReadTime = System.currentTimeMillis();
        initSerialReader();
        recentValues = new double[SMOOTHING_BUFFER_LENGTH];
        Arrays.fill(recentValues, 0);
        paused = false;
    }

    public void run() {
        running.set(true); // setting a boolean so that the GUI can know if running, and kill the process
        try {
            getDeviceConfig();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (running.get()) {
            if (!paused) {
                try {
                    processReadData(readAndWriteFromSerial("$DA" + deviceId + "?\r"));
                    Thread.sleep(SERIAL_GET_DATA_INTERVAL);
                } catch (InterruptedException e) {
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

    void initSerialReader() {
        comPort = SerialPort.getCommPorts()[0]; //Open whatever's available
        comPort.setComPortParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY_BITS);
        comPort.openPort();

        if (comPort.openPort()) {
            System.out.println("Port is open :)");
        } else {
            System.out.println("Failed to open port :(");
        }
    }

    void getDeviceConfig() throws InterruptedException {
        String digitalFilterRaw, baudRateRaw, fullScaleRaw, inputSensibilityRaw; //config data from connected board

        deviceId = readAndWriteFromSerial("$ID?\r").substring(1, 3);
        digitalFilterRaw = trim(readAndWriteFromSerial("$FD" + deviceId + "?\r").substring(3));
        baudRateRaw = trim(readAndWriteFromSerial("$BD" + deviceId + "?\r").substring(3));
        resolution = trim(readAndWriteFromSerial("$RD" + deviceId + "?\r").substring(3));
        fullScaleRaw = trim(readAndWriteFromSerial("$CP" + deviceId + "?\r").substring(3));
        inputSensibilityRaw = trim(readAndWriteFromSerial("$SE" + deviceId + "?\r").substring(3));
        instrumentVersion = trim(readAndWriteFromSerial("$TY" + deviceId + "?\r").substring(3));

        fullScale = Integer.parseInt(fullScaleRaw);
        inputSensibility = Integer.parseInt(inputSensibilityRaw);

        switch (Integer.parseInt(digitalFilterRaw)) {
            case 0:
                digitalFilter = "300Hz";
                break;
            case 1:
                digitalFilter = "150Hz";
                break;
            case 2:
                digitalFilter = "75Hz";
                break;
            case 3:
                digitalFilter = "37.5Hz";
                break;
            case 4:
                digitalFilter = "19Hz";
                break;
            case 5:
                digitalFilter = "6.5Hz";
                break;
            case 6:
                digitalFilter = "3.25Hz";
                break;
            case 7:
                digitalFilter = "0.62Hz";
                break;
            case 8:
                digitalFilter = "0.4Hz";
                break;
            default:
                digitalFilter = "Unknown refresh rate";
                break;
        }
        switch (Integer.parseInt(baudRateRaw)) {
            case 0:
                baudRate = 4800;
                break;
            case 1:
                baudRate = 9600;
                break;
            case 2:
                baudRate = 19200;
                break;
            case 3:
                baudRate = 38400;
                break;
            case 4:
                baudRate = 115200;
            default:
                baudRate = 0;
                break;
        }
        System.out.println("\t\t---- DEVICE ID CONFIG ----");
        System.out.println("ID: " + deviceId + "\tBAUDRATE: " + baudRate + "\tREFRESH RATE: " + digitalFilter);
        System.out.println("FW: " + instrumentVersion + "\tRESOLUTION: " + resolution + "\tSCALE: " + fullScale);
    }

    String readAndWriteFromSerial(String out) throws InterruptedException {
        if (comPort.isOpen()) {
            comPort.writeBytes(out.getBytes(), out.getBytes().length);
            while (comPort.bytesAvailable() == 0) {
                Thread.sleep(20);
            }
            byte[] readBuffer = new byte[comPort.bytesAvailable()];
            comPort.readBytes(readBuffer, readBuffer.length);
            System.out.println(new String(readBuffer, StandardCharsets.US_ASCII));
            return new String(readBuffer, StandardCharsets.US_ASCII);
        } else {
            return null;
        }
    }

    void processReadData(String in) {
        String pattern = "(?<=.{5})[1-9]([0-9]*)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(in);
        if (m.find()) {
            double correctedValue = Double.parseDouble(m.group(0)) / CORRECTION_FACTOR + CORRECTION_OFFSET; //All values are corrected by a a*x+c factor found by experimentation
            System.out.println("Found value " + m.group(0) + " / " + correctedValue + " at time " + ((System.currentTimeMillis() - lastReadTime) / 1000) + "s");
            if (activateZeroBalance) {
                double rollingSum = 0;
                for (int i = recentValues.length - 2; i >= 0; i--) {
                    recentValues[i + 1] = recentValues[i];
                    rollingSum += recentValues[i + 1];
                }
                rollingSum += recentValues[0] = correctedValue;

                if (recentValues[recentValues.length - 1] != 0) {
                    userOffset = rollingSum / recentValues.length;
                    activateZeroBalance = false;
                    sto.setOffsetValue(userOffset);
                }
            } else {
                sto.addDataToGraph(correctedValue - userOffset);
            }
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

    public void setActivateZeroBalance(boolean activateZeroBalance) {
        this.activateZeroBalance = activateZeroBalance;
    }

    public boolean getRunning() {
        return running.get();
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }
}

