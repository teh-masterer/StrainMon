package com.sematek;

import java.awt.*;

import static javax.swing.SwingUtilities.isEventDispatchThread;


public class Main {

    public static void main(String[] args) {

        EventQueue.invokeLater(() -> {
            System.out.println("inni invokeLater? " + isEventDispatchThread());
            new Grapher().setVisible(true);
        });
        Thread t1 = new Thread(new SerialReader(),"t1");
        t1.start();
    }
}
