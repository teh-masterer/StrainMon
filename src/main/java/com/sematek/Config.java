package com.sematek;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Config {

    // Deklarere attributter
    final double CORRECTION_FACTOR;
    final double CORRECTION_OFFSET;
    final int SERIAL_GET_DATA_INTERVAL; // 6.5Hz refresh rate is equal to ~154ms
    final int BAUD_RATE;
    final int DATA_BITS;
    final int STOP_BITS;
    final int PARITY_BITS;
    final boolean AUTO_ZERO_ON_START;
    final double USER_OFFSET;
    final int SMOOTHING_BUFFER_LENGTH;
    String TIMESTAMP;
    String testId, operator, specimenType, specimenName, locale, customer, specimenDetails;

    public Config() {
        // Standardverdier i tilfelle config-filen ikke inneholder verdiene
        CORRECTION_FACTOR = 1.505706984;
        CORRECTION_OFFSET = 140;
        SERIAL_GET_DATA_INTERVAL = 200; // 6.5Hz refresh rate is equal to ~154ms
        BAUD_RATE = 38400;
        DATA_BITS = 8;
        STOP_BITS = 1;
        PARITY_BITS = 0;
        USER_OFFSET = 0;
        SMOOTHING_BUFFER_LENGTH = 15;
        AUTO_ZERO_ON_START = true;
        TIMESTAMP = "default";

    }



    // DON'T TOUCH THE FOLLOWING CODE
    private static Config instance;

    public static Config getInstance() {
        if (instance == null) {
            instance = fromDefaults();
        }
        return instance;
    }

    public static void load(File file) {
        instance = fromFile(file);

        // no config file found
        if (instance == null) {
            instance = fromDefaults();
            System.out.println("Config loaded default values");
        } else {
            System.out.println("Config loaded value from file.");
        }
    }
    public static void load() {
        load(new File(Utils.CONFIG_PATH + File.separator + Utils.CONFIG_FILENAME));
    }

    public static void load(String file) {
        load(new File(file));
    }

    private static Config fromDefaults() {
        return new Config();
    }

    public void toFile(String file) {
        toFile(new File(file));
    }

    public void toFile(File file) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonConfig = gson.toJson(this);
        FileWriter writer;
        try {
            writer = new FileWriter(file);
            writer.write(jsonConfig);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
                //Creating the directory
            boolean bool = false;
            bool = file.getParentFile().getParentFile().mkdir();
            bool = file.getParentFile().mkdir();
                if(bool){
                    System.out.println("Config directory created successfully");
                }else{
                    System.out.println("Sorry, couldn’t create config directory");
                    e.printStackTrace();
                }
            }
        }

    public void toFile() {
        toFile(new File(Utils.CONFIG_PATH + File.separator + Utils.CONFIG_FILENAME));
    }

    private static Config fromFile(File configFile) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
            return gson.fromJson(reader, Config.class);
        } catch (FileNotFoundException e) {
            if (new File(Utils.CONFIG_PATH + File.separator + Utils.CONFIG_FILENAME).exists()) {
                System.out.println("Config file not found at" + Utils.CONFIG_PATH + File.separator + Utils.CONFIG_FILENAME);
            }
            System.out.println("Error initializing config data.");
            return null;
        }
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}