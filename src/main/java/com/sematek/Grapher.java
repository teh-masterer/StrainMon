package com.sematek;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;

import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class Grapher extends JFrame {

    public TimeSeries series;
    public JFreeChart chart;
    public XYPlot plot;
    public TimeSeriesCollection dataset;
    public SerialReader s;

    public JLabel labelCurrentValue;   //These labels are updated from SerialReader class
    public JLabel labelMaxValue;
    public JLabel labelOffsetValue;

    private StrainTestObject strainTestObject;

    public Grapher() {
        initUI();
    }

    //Clicking the "Start" button determines if the Strain Test Object is populated with real data or not, before this method is called
    private void startSerialReader()  {
        if (s != null) {
            if (s.comPort.isOpen()) { //Ensure port is ready
                s.closePort();
            }
        }
        s = new SerialReader(strainTestObject); //Make a new reader connection, give it access to data storage object
        new Thread(s).start();
        strainTestObject.removeAllSeries(); //reset the data if any, on start

        //Following three lines only here for debug of STO

        series = new TimeSeries("Strekk");
        dataset = new TimeSeriesCollection();
        dataset.addSeries(series);
        chart.getXYPlot().setDataset(dataset);

        //dataset = strainTestObject.getDataset();
        //series = (TimeSeries) dataset.getSeries();
        //chart.getXYPlot().setDataset(dataset);
        //dataset = strainTestObject.getDataset(); //load the dataset containing series to the graph
        try {
            dataset.validateObject();
        } catch (InvalidObjectException e) {
            e.printStackTrace();
        }
        labelCurrentValue.setText(String.valueOf(strainTestObject.getCurrentValue()));
        labelOffsetValue.setText(String.valueOf(strainTestObject.getOffsetValue()));
        labelMaxValue.setText(String.valueOf(strainTestObject.getMaxValue()));
    }

        private void initUI() {
            chart = createChart(dataset);

            setLayout(new BorderLayout());
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            chartPanel.setBackground(Color.white);
            add(chartPanel,BorderLayout.NORTH);

            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits ());
            rangeAxis.setAutoRangeMinimumSize(200);
            JButton b2=new JButton("Stopp");
            b2.setAlignmentX(Component.LEFT_ALIGNMENT);
            b2.addActionListener(e -> s.end());
            JButton b3=new JButton("Eksporter...");
            b3.setAlignmentX(Component.RIGHT_ALIGNMENT);
            b3.addActionListener(e -> {
                Utils.appendToExcelDatabase(strainTestObject);
                final JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new File(System.getProperty("user.home")));
                fc.setDialogTitle("Specify a file to save");

                int userSelection = fc.showSaveDialog(b3);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fc.getSelectedFile();
                    System.out.println("Save as file: " + fileToSave.getAbsolutePath());
                    Utils.storeDataSet(chart,fileToSave.getAbsolutePath());

                }
                });
            JButton b4 = new JButton("Nullstill");
            b4.setAlignmentX(Component.LEFT_ALIGNMENT);
            b4.addActionListener(e -> {
                if (s.getRunning()) {
                    series.delete(0, series.getItemCount() - 1);
                    s.setActivateZeroBalance(true);
                    labelMaxValue.setText("0.00");
                } else {
                    JOptionPane.showMessageDialog(null,"Start prosessen for å kunne nullstille!");
                }
            });

            JButton b7 = new JButton("Start...");
            b7.setAlignmentX(Component.LEFT_ALIGNMENT);
            b7.addActionListener(e -> {
                JTextField testIDField = new JTextField(5);
                JTextField customerField = new JTextField(5);
                JTextField specimenTypeField = new JTextField(5);
                JTextField specimenNameField = new JTextField(5);
                JTextField testCommentField = new JTextField(5);
                JTextField operatorField = new JTextField(5);
                JTextField localeField = new JTextField(5);

                try {
                    testIDField.setText(Utils.findPreviousTestId());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                //Perhaps all of this should be dynamically pulled from StrainTestObject? For now it isn't.
                JPanel myPanel = new JPanel();
                myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.PAGE_AXIS));

                myPanel.add(new JLabel("Test-ID: "));
                myPanel.add(testIDField);

                myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                myPanel.add(new JLabel("Kunde: "));
                myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                myPanel.add(customerField);
                myPanel.add(new JLabel("Lokalitet: "));
                myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                myPanel.add(localeField);
                myPanel.add(new JLabel("Prøvetype: "));
                myPanel.add(specimenTypeField);
                myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                myPanel.add(new JLabel("Linenummer: "));
                myPanel.add(specimenNameField);
                myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                myPanel.add(new JLabel("Kommentar: "));
                myPanel.add(testCommentField);
                myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                myPanel.add(new JLabel("Operatør: "));
                myPanel.add(operatorField);
                myPanel.add(Box.createHorizontalStrut(15)); // a spacer


                int result = JOptionPane.showConfirmDialog(null, myPanel,
                        "Sleng inn testdata her", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    strainTestObject = new StrainTestObject(testIDField.getText(), customerField.getText(),localeField.getText(),specimenTypeField.getText(),specimenNameField.getText(),testCommentField.getText(),operatorField.getText(),this);
                    JOptionPane.showMessageDialog(null,strainTestObject.validateInput());
                    if (strainTestObject.validateInput().equals("OK")) {
                        startSerialReader();
                    } else {
                        b7.doClick();
                    }
                    /*System.out.println("ID: " + testIDField.getText() + "\tKunde: " + customerField.getText() + "\tLokalitet: " + localeField.getText() + "\tType: " +
                            specimenTypeField.getText() + "\tNavn: " + specimenNameField.getText() + "\tKommentar: " + testCommentField.getText() +
                            "\tOperatør: " + operatorField.getText());
                    //Copy text from fields to String variables in global scope */

                } else {
                    strainTestObject = new StrainTestObject(this);
                    startSerialReader();
                }
            });


            JButton b6 = new JButton("Enhetsinfo...");
            b6.setAlignmentX(Component.RIGHT_ALIGNMENT);
            b6.addActionListener(e -> {
                if (s.getRunning()) {
                JOptionPane.showMessageDialog(null," \t\t---- DEVICE ID CONFIG ----\n"+
                        "ID: " + s.deviceId + "\t BAUDRATE: " + s.baudRate + "\t REFRESH RATE: " + s.digitalFilter +
                        "\nFW: " + s.instrumentVersion + "\t RESOLUTION: " + s.resolution + "\t SCALE: " + s.fullScale);
                } else {
                    JOptionPane.showMessageDialog(null,"Start prosessen for å vise enhetsinformasjon!");
                }
            });

            JButton b8 = new JButton("Pause...");
            b8.addActionListener(e -> {
                if (s.getRunning()) {
                    if (s.isPaused()) {
                        s.setPaused(false);
                        b8.setBackground(null);
                    } else {
                        s.setPaused(true);
                        b8.setOpaque(true);
                        b8.setBackground(Color.yellow);
                    }
                } else {
                    JOptionPane.showMessageDialog(null,"Kan ikke sette på pause, måling ikke aktiv");

                }
                });
            JLabel l1 =new JLabel("Verdi: ");
            labelCurrentValue = new JLabel("0.00");
            labelCurrentValue.setPreferredSize(new Dimension(120,40));
            labelCurrentValue.setHorizontalAlignment(SwingConstants.CENTER);
            labelCurrentValue.setOpaque(true);
            labelCurrentValue.setForeground(Color.blue);
            labelCurrentValue.setBackground(Color.lightGray);
            JLabel l3 =new JLabel("kg   ");
            JLabel l4 =new JLabel("Maks: ");
            labelMaxValue =new JLabel("0.00");
            labelMaxValue.setPreferredSize(new Dimension(120,40));
            labelMaxValue.setHorizontalAlignment(SwingConstants.CENTER);
            labelMaxValue.setOpaque(true);
            labelMaxValue.setForeground(Color.blue);
            labelMaxValue.setBackground(Color.lightGray);

            JLabel l6 =new JLabel("kg   ");
            JLabel l7 = new JLabel("Offset: ");
            labelOffsetValue = new JLabel(("0.00"));
            labelOffsetValue.setPreferredSize(new Dimension(120,40));
            labelOffsetValue.setHorizontalAlignment(SwingConstants.CENTER);
            JLabel l9 = new JLabel("kg   ");

            float fontSz = 26.0f;
            labelCurrentValue.setFont (labelCurrentValue.getFont ().deriveFont (fontSz));
            labelMaxValue.setFont (labelMaxValue.getFont ().deriveFont (fontSz));
            labelOffsetValue.setFont (labelOffsetValue.getFont ().deriveFont (fontSz));
            labelOffsetValue.setOpaque(true);
            labelOffsetValue.setForeground(Color.blue);
            labelOffsetValue.setBackground(Color.lightGray);
            l1.setFont (l1.getFont ().deriveFont (fontSz));
            l3.setFont (l3.getFont ().deriveFont (fontSz));
            l4.setFont (l4.getFont ().deriveFont (fontSz));
            l6.setFont (l6.getFont ().deriveFont (fontSz));
            l7.setFont (l7.getFont ().deriveFont (fontSz));
            l9.setFont (l7.getFont ().deriveFont (fontSz));


            JPanel labelPanel = new JPanel();
            labelPanel.add(l1,BorderLayout.WEST);
            labelPanel.add(labelCurrentValue,BorderLayout.WEST);
            labelPanel.add(l3,BorderLayout.WEST);
            labelPanel.add(l4,BorderLayout.WEST);
            labelPanel.add(labelMaxValue,BorderLayout.WEST);
            labelPanel.add(l6,BorderLayout.WEST);

            labelPanel.add(l7,BorderLayout.EAST);
            labelPanel.add(labelOffsetValue,BorderLayout.EAST);
            labelPanel.add(l9,BorderLayout.EAST);


            JPanel buttonPanel = new JPanel(); //Make the button panel
            buttonPanel.add(b7);
            buttonPanel.add(b8);
            buttonPanel.add(b2);
            buttonPanel.add(b4);


            buttonPanel.add(b3);
            buttonPanel.add(b6);

            add(labelPanel,BorderLayout.CENTER);
            add(buttonPanel,BorderLayout.SOUTH);
            pack();
            setTitle("Sematek Horisontal Strekkbenk");
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);



        }

        private JFreeChart createChart(XYDataset dataset) {
            JFreeChart chart = ChartFactory.createTimeSeriesChart(
                    "Målte strekkrefter",
                    "Tid (s)",
                    "Strekk (kg)",
                    dataset,
                    false,
                    true,
                    false
            );

            plot = chart.getXYPlot();

            DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
            renderer.setSeriesPaint(0, Color.RED);
            renderer.setSeriesStroke(0, new BasicStroke(2.0f));

            plot.setRenderer(renderer);
            plot.setBackgroundPaint(Color.lightGray);

            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.BLACK);

            plot.setDomainGridlinesVisible(true);
            plot.setDomainGridlinePaint(Color.BLACK);

            AxisSpace space = new AxisSpace();
            space.setRight(25);
            space.setLeft(50);
            plot.setFixedRangeAxisSpace(space);


            chart.setTitle(new TextTitle("Målte strekkrefter",
                            new Font("Serif", java.awt.Font.BOLD, 18)
                    )
            );
            return chart;
        }


}
