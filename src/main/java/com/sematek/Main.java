package com.sematek;

import java.awt.*;


public class Main {

    public static void main(String[] args) {
        final Grapher g = new Grapher();
        EventQueue.invokeLater(() -> {
            g.setVisible(true);
        });
    }
}
