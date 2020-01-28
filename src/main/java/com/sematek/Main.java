package com.sematek;

import java.awt.*;


public class Main {

    public static void main(String[] args) {

        EventQueue.invokeLater(() -> {
        SerialReader s = null;
        try {
            Grapher g = new Grapher();
            g.setVisible(true);
            s = new SerialReader(g);
            while (true) {
                s.readSerial();
                g.series.fireSeriesChanged();
                System.out.println("Series size: " + g.series.getItemCount());
                Thread.sleep(2000);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
            s.closePort();
        }

            });
    }

}
