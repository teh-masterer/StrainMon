package com.sematek;

import java.awt.Color;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class BlinkButton extends JButton {
    private static final long serialVersionUID = 1L;

    private static final int BLINKING_RATE = 1000; // in ms

    private boolean blinkingOn = false;

    public BlinkButton(String text) {
        super(text);
        Timer timer = new Timer(BLINKING_RATE, new TimerListener(this));
        timer.setInitialDelay(0);
        timer.start();
    }

    public void setBlinking(boolean flag) {
        this.blinkingOn = flag;
    }

    public boolean getBlinking(boolean flag) {
        return this.blinkingOn;
    }


    private class TimerListener implements ActionListener {
        private final BlinkButton bl;
        private Color bg;
        private Color fg;
        private boolean isForeground = true;

        public TimerListener(BlinkButton bl) {
            this.bl = bl;
        }

        public void actionPerformed(ActionEvent e) {
            if (bl.blinkingOn) {
                if (isForeground) {
                    bl.setBackground(Color.yellow);
                } else {
                    bl.setBackground(null);
                }
                isForeground = !isForeground;
            } else {
                // here we want to make sure that the label is visible
                // if the blinking is off.
                if (isForeground) {
                    bl.setBackground(null);
                    isForeground = false;
                }
            }
        }

    }
}