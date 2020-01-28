package com.sematek;

import java.awt.*;

import static javax.swing.SwingUtilities.isEventDispatchThread;


public class Main {

    public static void main(String[] args) {
        final Grapher g = new Grapher();
        EventQueue.invokeLater(() -> {
            System.out.println("inni invokeLater? " + isEventDispatchThread());
            g.setVisible(true);
        });
        new Thread(new SerialReader(g),"Serial Reader").start();
    }
}
