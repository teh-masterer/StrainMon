package com.sematek;

import java.awt.*;
import java.io.IOException;

import static java.lang.Thread.sleep;

public class Main {

    public static void main(String[] args) {



        EventQueue.invokeLater(() -> {
            try {
                Grapher g = new Grapher();
                g.setVisible(true);
                SerialReader s = new SerialReader(g);
                while (true) {
                    s.run();
                    g.run();
                    sleep(1500);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        });

    }
}
