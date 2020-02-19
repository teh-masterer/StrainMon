package com.sematek;

import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {

    public static void main(String[] args) {
        final Grapher g = new Grapher();

        File jarPath=new File(Config.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String configPath=jarPath.getParentFile().getAbsolutePath() + File.separator + "config.json";
        //Config.load("config.json");
        Config.load();

        //System.out.println("Config file at " + configPath + " exists? " + new File(configPath).exists());
        Config.getInstance().TIMESTAMP = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
        //Config.getInstance().toFile(configPath);
        Config.getInstance().toFile();

        EventQueue.invokeLater(() -> g.setVisible(true));

    }
}
