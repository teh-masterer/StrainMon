package com.sematek;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Grapher extends JFrame {

    public TimeSeries series;
    public JFreeChart chart;
    public XYPlot plot;
    public TimeSeriesCollection dataset;
    public SerialReader s;

    public JLabel valueLabel;   //These labels are updated from SerialReader class
    public JLabel maxLabel;
    public JLabel offsetLabel;
    public JLabel extDataLbl;
    public JLabel elongatedDistanceLabel;
    public JLabel elongatedValueLabel;

    public JPanel p;

    boolean anonymousTest;

    JButton strainStartBtn; //needs access from STO

    private StrainTestObject sto;

    public Grapher() {
        initUI();
    }

    //Clicking the "Start" button determines if the Strain Test Object is populated with real data or not, before this method is called
    private void startSerialReader() {
        if (s != null && s.isComPortAvailable() && s.comPort.isOpen()) {
            s.closePort();
        }
        s = new SerialReader(sto);//Make a new reader connection, give it access to data storage object
        if (s.isComPortAvailable()) {
            new Thread(s).start();
            sto.removeAllSeries(); //reset the data if any, on start
            sto.setMaxValue(0);
            if (Utils.loadMetadata(this)[8][0].length() > 0) {
                sto.setOffsetValue(Double.parseDouble(Utils.loadMetadata(this)[8][1]));
            }

            series = new TimeSeries("Strekk");
            dataset = new TimeSeriesCollection();
            dataset.addSeries(series);
            chart.getXYPlot().setDataset(dataset);
            plot.getRangeAxis().setRange(-200,1000);

            try {
                dataset.validateObject();
            } catch (InvalidObjectException e) {
                e.printStackTrace();
            }
            valueLabel.setText(String.valueOf(sto.getCurrentValue()));
            offsetLabel.setText(String.valueOf(sto.getOffsetValue()));
            maxLabel.setText(String.valueOf(sto.getMaxValue()));
        } else {
            JOptionPane.showMessageDialog(null, "Ingen serielinje-tilkobling funnet! (Du har glemt å plugge i kabelen)");
        }
    }

        private void initUI() {
            chart = createChart(dataset);
            anonymousTest = true;

            setLayout(new BorderLayout());
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            chartPanel.setBackground(Color.white);
            add(chartPanel, BorderLayout.NORTH);

            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());

            JButton startBtn = new JButton("Start...");
            JButton stopBtn = new JButton("Pause");
            JButton saveBtn=new JButton("Lagre...");
            JButton zeroBtn = new JButton("Nullstill");
            JButton infoBtn = new JButton("Enhetsinfo...");

            startBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            startBtn.addActionListener(e -> {
                String tauString = "Tau";
                String kjettingString = "Kjetting";
                String annetString = "Annet";
                String specimenType;

                if (s != null && s.isPaused()) {
                    stopBtn.setBackground(null);
                    startBtn.setBackground(Color.yellow);
                    s.setPaused(false);
                } else {

                    JTextField testIDField = new JTextField(5);
                    JTextField customerField = new JTextField(5);
                    JRadioButton tauRadioButton = new JRadioButton(tauString);
                    tauRadioButton.setActionCommand(tauString);
                    tauRadioButton.setSelected(true);
                    JRadioButton kjettingRadioButton = new JRadioButton(kjettingString);
                    kjettingRadioButton.setActionCommand(kjettingString);
                    JRadioButton annetRadioButton = new JRadioButton(annetString);
                    JTextField specimenNameField = new JTextField(5);
                    JTextField testCommentField = new JTextField(5);
                    JTextField operatorField = new JTextField(5);
                    JTextField localeField = new JTextField(5);

                    ButtonGroup specimenTypeGroup = new ButtonGroup();
                    specimenTypeGroup.add(tauRadioButton);
                    specimenTypeGroup.add(kjettingRadioButton);
                    specimenTypeGroup.add(annetRadioButton);


                    JPanel radioButtonPanel = new JPanel();
                    radioButtonPanel.setLayout(new FlowLayout());
                    radioButtonPanel.add(tauRadioButton);
                    radioButtonPanel.add(kjettingRadioButton);
                    radioButtonPanel.add(annetRadioButton);
                    JPanel myPanel = new JPanel();
                    myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));

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
                    myPanel.add(radioButtonPanel);
                    myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                    myPanel.add(new JLabel("Linenummer: "));
                    myPanel.add(specimenNameField);
                    myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                    myPanel.add(new JLabel("Beskrivelse: "));
                    myPanel.add(testCommentField);
                    myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                    myPanel.add(new JLabel("Operatør: "));
                    myPanel.add(operatorField);
                    myPanel.add(Box.createHorizontalStrut(15)); // a spacer

                    String[][] metadata = Utils.loadMetadata(this);
                    if (metadata[6][0].length() > 0) {
                        testIDField.setText(metadata[0][1]);
                        customerField.setText(metadata[1][1]);
                        localeField.setText(metadata[2][1]);
                        if (metadata[3][1].equals(tauString)) {
                            tauRadioButton.setSelected(true);
                        } else if (metadata[3][1].equals(kjettingString)) {
                            kjettingRadioButton.setSelected(true);
                        } else {
                            annetRadioButton.setSelected(true);
                        }
                        specimenNameField.setText(metadata[4][1]);
                        testCommentField.setText(metadata[5][1]);
                        operatorField.setText(metadata[6][1]);
                    }
                    try {
                        testIDField.setText(String.valueOf(Utils.getNextTestId()));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    int result = JOptionPane.showConfirmDialog(null, myPanel,
                            "Sleng inn testdata!", JOptionPane.OK_CANCEL_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        if (kjettingRadioButton.isSelected()) {
                            specimenType = kjettingString;
                        } else if (tauRadioButton.isSelected()) {
                            specimenType = tauString;
                        } else {
                            specimenType = annetString;
                        }
                        sto = new StrainTestObject(testIDField.getText(), customerField.getText(), localeField.getText(), specimenType, specimenNameField.getText(), testCommentField.getText(), operatorField.getText(), this);
                        if (sto.validateInput().equals("OK")) {
                            startSerialReader();
                            startBtn.setBackground(Color.yellow);
                            Utils.saveMetadata(sto);
                            anonymousTest = false;
                        } else {
                            JOptionPane.showMessageDialog(null, sto.validateInput());
                            startBtn.doClick();
                        }

                    } else {
                        sto = new StrainTestObject(this);
                        startSerialReader();
                        startBtn.setBackground(Color.yellow);
                    }
                }
            });


            rangeAxis.setRange(-100,500);
            stopBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            stopBtn.addActionListener(e -> {
                if (s.isPaused()) { //this section is commented out because of poor performance after restart
                   // stopBtn.setBackground(null);
                   // s.setPaused(false);
                   // s.end();
                } else {
                    stopBtn.setBackground(Color.yellow);
                    startBtn.setBackground(null);
                    s.pause();
                    if (!anonymousTest) {
                        Utils.saveMetadata(sto);
                    }
                }
            });
            saveBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
            saveBtn.addActionListener(e -> {
                if (sto.getSpecimenType().equals("Kjetting")) {
                    JPanel myPanel = new JPanel();
                    JTextField preB = new JTextField("0"), preD = new JTextField("0"), preL = new JTextField("0"), postB = new JTextField("0");
                    JTextField fractureDescription = new JTextField(5);
                    myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));
                    myPanel.add(new JLabel("Beskriv bruddtype:"));
                    myPanel.add(fractureDescription);
                    myPanel.add(new JLabel("Skriv inn opprinnelig B-mål"));
                    myPanel.add(preB);
                    myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                    myPanel.add(new JLabel("Skriv inn opprinnelig D-mål"));
                    myPanel.add(preD);
                    myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                    myPanel.add(new JLabel("Skriv inn opprinnelig L-mål"));
                    myPanel.add(preL);
                    myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                    myPanel.add(new JLabel("Skriv inn strukket D-mål"));
                    myPanel.add(postB);


                    int result = JOptionPane.showConfirmDialog(this,myPanel,"Kjetting-parametre",JOptionPane.OK_CANCEL_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        sto.setPreB(preB.getText());
                        sto.setPreD(preD.getText());
                        sto.setPreL(preL.getText());
                        sto.setPostB(postB.getText());
                        sto.setFractureDescription(fractureDescription.getText());
                    }

                } else if (sto.getSpecimenType().equals("Tau")) {
                JPanel myPanel = new JPanel();
                JTextField fractureDescription = new JTextField(5);
                myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));
                myPanel.add(new JLabel("Beskriv bruddtype:"));
                myPanel.add(fractureDescription);

                    int result = JOptionPane.showConfirmDialog(this,myPanel,"Tau-parametre",JOptionPane.OK_CANCEL_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        sto.setFractureDescription(fractureDescription.getText());
                    }
            }
                Utils.saveMetadata(sto);
                String filenameString = "new_test";

                LocalDateTime timeNow = LocalDateTime.now();
                DateTimeFormatter filenameTimeFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH_mm_ss");
                DateTimeFormatter exportTimeFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

                if (sto.validateInput().equals("OK")) {
                    filenameString = (sto.getTestID());
                }

                final JFileChooser fc = new JFileChooser();
                File file = new File(Utils.DATA_PATH);

                if (file.getParentFile().mkdir()) {
                    System.out.println(file.getParentFile() + " directory created successfully");
                } else {
                    System.out.println("Sorry, couldn’t create " + file.getParentFile().toString());
                }
                    if(file.mkdir()) {
                    System.out.println(file.getParentFile() + " directory created successfully");
                } else {
                        System.out.println("Sorry, couldn’t create " + file.toString());
                    }

                fc.setCurrentDirectory(file);
                fc.setDialogTitle("Specify a file to save");
                fc.setSelectedFile(new File(filenameString + "__" + timeNow.format(filenameTimeFormat)));


                int userSelection = fc.showSaveDialog(saveBtn);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fc.getSelectedFile();
                    System.out.println("Save as file: " + fileToSave.getAbsolutePath());

                    Utils.appendToExcelDatabase(sto, timeNow.format(exportTimeFormat));
                    Utils.storeDataSet(chart,fileToSave.getAbsolutePath(), sto);
                    try {
                        Utils.saveChartAsPng(chartPanel,fileToSave.getAbsolutePath());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                }
                });
            zeroBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            zeroBtn.addActionListener(e -> {
                if (s.getRunning()) {
                    maxLabel.setText("0.00");
                    s.setActivateZeroBalance(true);
                } else {
                    JOptionPane.showMessageDialog(null,"Start prosessen for å kunne nullstille!");
                }
            });
            JButton deleteSeriesBtn = new JButton("Fjern data");
            deleteSeriesBtn.addActionListener(e -> {
                series.delete(0, series.getItemCount() - 1);
            });


            infoBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
            infoBtn.addActionListener(e -> {
                if (s.getRunning()) {
                JOptionPane.showMessageDialog(null," \t\t---- DEVICE ID CONFIG ----\n"+
                        "ID: " + s.deviceId + "\t BAUDRATE: " + s.baudRate + "\t REFRESH RATE: " + s.digitalFilter +
                        "\nFW: " + s.instrumentVersion + "\t RESOLUTION: " + s.resolution + "\t SCALE: " + s.fullScale +
                        "\nKODET AV VEGARD GUTTORMSEN FOR SEMATEK (2020)");
                } else {
                    JOptionPane.showMessageDialog(null,"Start prosessen for å vise enhetsinformasjon!");
                }
            });

            JButton zeroExtBtn = new JButton("Null ekst.");
            zeroExtBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
            zeroExtBtn.addActionListener(e -> sto.zeroExtOffset());

            strainStartBtn = new JButton("Start 1%-måling");
            strainStartBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
            strainStartBtn.addActionListener(e -> {
                JTextField measLengthField = new JTextField(5);

                JPanel myPanel = new JPanel();
                myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));

                myPanel.add(new JLabel("Målt lengde: "));
                myPanel.add(measLengthField);

                int result = JOptionPane.showConfirmDialog(null, myPanel,
                        "Sleng inn testdata!", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    if (Utils.isInteger(measLengthField.getText())) {
                        JOptionPane.showMessageDialog(null, "Du skrev inn et tall. Veldig bra! Applaus.");
                        sto.startElongationTest(measLengthField.getText());
                    } else {
                        JOptionPane.showMessageDialog(null, "Du skrev ikke et tall. Svakt.");
                    }
                    }
            });

            JLabel valueTxtLbl1 =new JLabel("Verdi ");
            valueLabel = new JLabel("0");
            valueLabel.setPreferredSize(new Dimension(200,50));
            valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
            valueLabel.setOpaque(true);
            valueLabel.setForeground(Color.blue);
            valueLabel.setBackground(Color.lightGray);
            JLabel valueTxtLbl2 =new JLabel("kg   ");
            JLabel maxTxtLbl1 =new JLabel("Maks ");
            maxLabel =new JLabel("0");
            maxLabel.setPreferredSize(new Dimension(200,50));
            maxLabel.setHorizontalAlignment(SwingConstants.CENTER);
            maxLabel.setOpaque(true);
            maxLabel.setForeground(Color.blue);
            maxLabel.setBackground(Color.lightGray);

            JLabel maxTxtLbl2 =new JLabel("kg   ");
            JLabel offsetTxtLbl1 = new JLabel("Offset ");
            offsetLabel = new JLabel(("0"));
            offsetLabel.setPreferredSize(new Dimension(200,50));
            offsetLabel.setHorizontalAlignment(SwingConstants.CENTER);
            JLabel offsetTxtLbl2 = new JLabel("kg   ");

            float fontSzRegular = 36.0f;
            float fontSzSmall = 28.0f;

            valueLabel.setFont (valueLabel.getFont ().deriveFont (fontSzRegular));
            maxLabel.setFont (maxLabel.getFont ().deriveFont (fontSzRegular));
            offsetLabel.setFont (offsetLabel.getFont ().deriveFont (fontSzRegular));
            offsetLabel.setOpaque(true);
            offsetLabel.setForeground(Color.blue);
            offsetLabel.setBackground(Color.lightGray);
            valueTxtLbl1.setFont (valueTxtLbl1.getFont ().deriveFont (fontSzRegular));
            valueTxtLbl2.setFont (valueTxtLbl2.getFont ().deriveFont (fontSzRegular));
            maxTxtLbl1.setFont (maxTxtLbl1.getFont ().deriveFont (fontSzRegular));
            maxTxtLbl2.setFont (maxTxtLbl2.getFont ().deriveFont (fontSzRegular));
            offsetTxtLbl1.setFont (offsetTxtLbl1.getFont ().deriveFont (fontSzRegular));
            offsetTxtLbl2.setFont (offsetTxtLbl1.getFont ().deriveFont (fontSzRegular));

            JLabel extTextLbl = new JLabel("Ekst. ");
            extTextLbl.setFont(extTextLbl.getFont().deriveFont(fontSzSmall));

            extDataLbl = new JLabel("0.0");
            extDataLbl.setPreferredSize(new Dimension(150,30));
            extDataLbl.setHorizontalAlignment(SwingConstants.CENTER);
            extDataLbl.setOpaque(true);
            extDataLbl.setForeground(Color.blue);
            extDataLbl.setBackground(Color.lightGray);
            extDataLbl.setFont(extDataLbl.getFont().deriveFont(fontSzSmall));


            elongatedValueLabel = new JLabel("---");
            elongatedValueLabel.setPreferredSize(new Dimension(150,30));
            elongatedValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
            elongatedValueLabel.setOpaque(true);
            elongatedValueLabel.setForeground(Color.blue);
            elongatedValueLabel.setBackground(Color.lightGray);
            elongatedValueLabel.setFont(elongatedValueLabel.getFont().deriveFont(fontSzSmall));


            JLabel elongatedDistanceTextLabel = new JLabel("Mål ");
            elongatedDistanceTextLabel.setFont(extTextLbl.getFont().deriveFont(fontSzSmall));

            JLabel elongatedValueTextLabel = new JLabel("Last ved 1% ");
            elongatedValueTextLabel.setFont(elongatedValueTextLabel.getFont().deriveFont(fontSzSmall));

            elongatedDistanceLabel = new JLabel("---");
            elongatedDistanceLabel.setPreferredSize(new Dimension(150,30));
            elongatedDistanceLabel.setHorizontalAlignment(SwingConstants.CENTER);
            elongatedDistanceLabel.setOpaque(true);
            elongatedDistanceLabel.setForeground(Color.blue);
            elongatedDistanceLabel.setBackground(Color.lightGray);
            elongatedDistanceLabel.setFont(elongatedDistanceLabel.getFont().deriveFont(fontSzSmall));


            JPanel labelPanelUpper = new JPanel();
            labelPanelUpper.add(valueTxtLbl1,BorderLayout.WEST);
            labelPanelUpper.add(valueLabel,BorderLayout.WEST);
            labelPanelUpper.add(valueTxtLbl2,BorderLayout.WEST);
            labelPanelUpper.add(maxTxtLbl1,BorderLayout.WEST);
            labelPanelUpper.add(maxLabel,BorderLayout.WEST);
            labelPanelUpper.add(maxTxtLbl2,BorderLayout.WEST);

            labelPanelUpper.add(offsetTxtLbl1,BorderLayout.EAST);
            labelPanelUpper.add(offsetLabel,BorderLayout.EAST);
            labelPanelUpper.add(offsetTxtLbl2,BorderLayout.EAST);

            JPanel labelPanelLower = new JPanel();
            labelPanelLower.add(extTextLbl,BorderLayout.EAST);
            labelPanelLower.add(extDataLbl,BorderLayout.EAST);
            labelPanelLower.add(elongatedDistanceTextLabel);
            labelPanelLower.add(elongatedDistanceLabel);
            labelPanelLower.add(elongatedValueTextLabel);
            labelPanelLower.add(elongatedValueLabel);


            JPanel buttonPanel = new JPanel(); //Make the button panel
            buttonPanel.add(startBtn);
            buttonPanel.add(stopBtn);
            buttonPanel.add(zeroBtn);
            buttonPanel.add(deleteSeriesBtn);
            buttonPanel.add(saveBtn);
            buttonPanel.add(infoBtn);
            buttonPanel.add(zeroExtBtn);
            buttonPanel.add(strainStartBtn);

            p = new JPanel();
            p.setLayout(new BorderLayout());
            p.add(labelPanelUpper,BorderLayout.NORTH);
            p.add(labelPanelLower,BorderLayout.CENTER);
            p.add(buttonPanel,BorderLayout.SOUTH);

            add(p,BorderLayout.SOUTH);
            //add(labelPanelUpper,BorderLayout.CENTER);
            //add(labelPanelLower,BorderLayout.SOUTH);
            //add(buttonPanel,BorderLayout.SOUTH);
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
            renderer.setDefaultShapesVisible(false);

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

            return chart;
        }


}
