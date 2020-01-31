package com.sematek;

import java.awt.*;
import java.io.File;

public class Main {

    public static void main(String[] args) {
        final Grapher g = new Grapher();

        File jarPath=new File(Config.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String configPath=jarPath.getParentFile().getAbsolutePath();
        Config.load(configPath + "/config.json");
        Config.getInstance().toFile(configPath + "./config_new.json");
        System.out.println(Config.getInstance().toString());

        EventQueue.invokeLater(() -> {
            g.setVisible(true);
        });
        // Konfigurationsdatei laden, falls vorhanden
        // ansonsten werden die Werte im Konstruktor verwendet
        //Config.load("config.json");

    }
}
