package com.sematek;

import java.awt.*;

import static javax.swing.SwingUtilities.isEventDispatchThread;


public class Main {

    public static void main(String[] args) {


        EventQueue.invokeLater(() -> {
            System.out.println("inni invokeLater? " + isEventDispatchThread());
            Grapher g = new Grapher();
            new Thread(new SerialReader(g),"t1").start();
            g.setVisible(true);
        });

    }
}
