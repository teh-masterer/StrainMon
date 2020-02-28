package com.sematek;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

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
    final boolean AUTO_ZERO_ON_START;
    final long lastReadTime;
    boolean activateZeroBalance;
    boolean paused;
    final double[] recentValues;

    String deviceId, digitalFilter, instrumentVersion, resolution; //all of these are read
    int fullScale, inputSensibility, baudRate;

    SerialPort comPort;
    SerialPort comPortArduino;
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
        this.SMOOTHING_BUFFER_LENGTH = Config.getInstance().SMOOTHING_BUFFER_LENGTH;
        //this.AUTO_ZERO_ON_START = Config.getInstance().AUTO_ZERO_ON_START;
        this.AUTO_ZERO_ON_START = false;

        System.out.println("Loaded config file with timestamp " + Config.getInstance().TIMESTAMP);

        this.sto = sto;
        lastReadTime = System.currentTimeMillis();
        recentValues = new double[SMOOTHING_BUFFER_LENGTH];
        Arrays.fill(recentValues, 0);
        paused = false;
    }

    public void run() {
        initSerialReader();
        running.set(true); // setting a boolean so that the GUI can know if running, and kill the process
        try {
            getDeviceConfig();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (running.get()) {
            if (!paused) {
                try {
                    //readAndWriteFromSerial(comPort,"$DA" + deviceId + "?\r");
                    processReadData(readAndWriteFromSerial(comPort,"$DA" + deviceId + "?\r"));
                    //sto.updateExtensiometerData(readFromSerial(comPortArduino));
                    Thread.sleep(SERIAL_GET_DATA_INTERVAL);

                    if (comPortArduino != null && !comPortArduino.isOpen()) {
                        comPortArduino.openPort();
                    }
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

    public void pause() {
        paused = true;
    }

    boolean isComPortAvailable() {
        return (SerialPort.getCommPorts().length > 0);
    }

    void initSerialReader() {
        for (SerialPort s : SerialPort.getCommPorts()) {
            System.out.println("Connected device found: " + s.getDescriptivePortName());
            if (s.getDescriptivePortName().contains("Arduino Uno") || s.getDescriptivePortName().contains("USB-Based Serial Port")) {
                comPortArduino = s;
                comPortArduino.setComPortParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY_BITS);
                System.out.println("Arduino COM connected to " + comPortArduino.getDescriptivePortName() + ", " + comPortArduino.getPortDescription() + " at " + comPortArduino.getBaudRate());

                if (comPortArduino.openPort()) {
                    System.out.println("Arduino COM port is open :)");

                    comPortArduino.addDataListener(new SerialPortDataListener() {
                        private String messages = "";

                        @Override
                        public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_RECEIVED; }

                        @Override
                        public void serialEvent(SerialPortEvent event) {
                            messages += new String(event.getReceivedData());
                            while (messages.contains("\n")) {
                                String[] message = messages.split("\\n", 2);
                                messages = (message.length > 1) ? message[1] : "";
                                sto.addExtData(message[0]);
                            }
                        }
                    });
                    /*
                    comPortArduino.addDataListener(new SerialPortDataListener() {
                        @Override
                        public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
                        @Override
                        public void serialEvent(SerialPortEvent event)
                        {
                            if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                                return;
                            byte[] newData = new byte[comPortArduino.bytesAvailable()];
                            sto.updateExtensiometerData(new String(newData, StandardCharsets.US_ASCII)); ;
                        }
                    });
                     */
                } else {
                    System.out.println("Failed to open Arduino COM port");
                }
            } else if (s.getDescriptivePortName().contains("ATEN USB to Serial Bridge") || s.getDescriptivePortName().contains("USB-to-Serial Port")) {
                comPort = s;
                comPort.setComPortParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY_BITS);
                System.out.println("Main COM connected to " + comPort.getDescriptivePortName() + ", " + comPort.getPortDescription() + " at " + comPort.getBaudRate());
                if (comPort.openPort()) {
                    System.out.println("Main COM port is open :)");

                    /*
                    comPort.addDataListener(new SerialPortDataListener() {
                        private String messages = "";

                        @Override
                        public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_RECEIVED; }

                        @Override
                        public void serialEvent(SerialPortEvent event) {
                            messages += new String(event.getReceivedData());
                            while (messages.contains("\n")) {
                                String[] message = messages.split("\\n", 2);
                                messages = (message.length > 1) ? message[1] : "";
                                System.out.println("Message: " + message[0]);
                            }
                        }
                    });


                    comPort.addDataListener(new SerialPortDataListener() {
                        @Override
                        public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_RECEIVED; }
                        @Override
                        public void serialEvent(SerialPortEvent event)
                        {
                            if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                                return;
                            byte[] newData = new byte[comPort.bytesAvailable()];
                            processReadData(new String(newData, StandardCharsets.US_ASCII)); ;
                        }
                    });
                     */
                } else {
                    System.out.println("Failed to open main COM port :(");
                }
            }
        }
            activateZeroBalance = AUTO_ZERO_ON_START;
    }

    void getDeviceConfig() throws InterruptedException {
        String digitalFilterRaw, baudRateRaw, fullScaleRaw, inputSensibilityRaw; //config data from connected board

        deviceId = readAndWriteFromSerial(comPort,"$ID?\r").substring(1, 3);
        digitalFilterRaw = trim(readAndWriteFromSerial(comPort,"$FD" + deviceId + "?\r").substring(3));
        baudRateRaw = trim(readAndWriteFromSerial(comPort,"$BD" + deviceId + "?\r").substring(3));
        resolution = trim(readAndWriteFromSerial(comPort,"$RD" + deviceId + "?\r").substring(3));
        fullScaleRaw = trim(readAndWriteFromSerial(comPort,"$CP" + deviceId + "?\r").substring(3));
        inputSensibilityRaw = trim(readAndWriteFromSerial(comPort,"$SE" + deviceId + "?\r").substring(3));
        instrumentVersion = trim(readAndWriteFromSerial(comPort,"$TY" + deviceId + "?\r").substring(3));

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

    String readFromSerial(SerialPort p) throws InterruptedException {
        if (p.isOpen()) {
            while (p.bytesAvailable() == 0) {
                Thread.sleep(20);
            }
            byte[] readBuffer = new byte[p.bytesAvailable()];
            p.readBytes(readBuffer, readBuffer.length);
            return new String(readBuffer, StandardCharsets.US_ASCII);
        } else {
            return null;
        }
    }
    void writeToSerial(SerialPort p, String out){
        if (p.isOpen()) {
            p.writeBytes(out.getBytes(), out.getBytes().length);
        }
    }

    String readAndWriteFromSerial(SerialPort p, String out) throws InterruptedException {
        if (p.isOpen()) {
            p.writeBytes(out.getBytes(), out.getBytes().length);
        }
        return  readFromSerial(comPort);
    }

    void processReadData(String in) {
        String pattern = "(?<=.{5})[1-9]([0-9]*)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(in);
        if (m.find()) {
            double correctedValue = Double.parseDouble(m.group(0)) / CORRECTION_FACTOR + CORRECTION_OFFSET; //All values are corrected by a a*x+c factor found by experimentation
            System.out.println("Val:" + Utils.round(correctedValue,2) + " kg, at " + ((System.currentTimeMillis() - lastReadTime) / 1000) + "s");
            if (activateZeroBalance) {
                double rollingSum = 0;
                for (int i = recentValues.length - 2; i >= 0; i--) {
                    recentValues[i + 1] = recentValues[i];
                    rollingSum += recentValues[i + 1];
                }
                rollingSum += recentValues[0] = correctedValue;

                if (recentValues[recentValues.length - 1] != 0) {
                    double userOffset = rollingSum / recentValues.length;
                    activateZeroBalance = false;
                    sto.setOffsetValue(userOffset);
                }
            } else {
                sto.addDataToGraph(correctedValue - sto.getOffsetValue());
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

