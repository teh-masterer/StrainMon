package com.sematek;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {

    public static void main(String[] args) {
        final Grapher g = new Grapher();

        Config.load();
        Config.getInstance().TIMESTAMP = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
        Config.getInstance().toFile();

        EventQueue.invokeLater(() -> g.setVisible(true));

    }
}
