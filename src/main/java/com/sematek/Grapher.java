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

    private StrainTestObject strainTestObject;

    public Grapher() {
        initUI();
    }

    //Clicking the "Start" button determines if the Strain Test Object is populated with real data or not, before this method is called
    private void startSerialReader() {
        if (s != null && s.isComPortAvailable() && s.comPort.isOpen()) {
            s.closePort();
        }
        s = new SerialReader(strainTestObject);//Make a new reader connection, give it access to data storage object
        if (s.isComPortAvailable()) {
            new Thread(s).start();
            strainTestObject.removeAllSeries(); //reset the data if any, on start
            strainTestObject.setMaxValue(0);

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
            valueLabel.setText(String.valueOf(strainTestObject.getCurrentValue()));
            offsetLabel.setText(String.valueOf(strainTestObject.getOffsetValue()));
            maxLabel.setText(String.valueOf(strainTestObject.getMaxValue()));
        } else {
            JOptionPane.showMessageDialog(null, "Ingen serielinje-tilkobling funnet! (Du har glemt å plugge i kabelen)");
        }
    }

        private void initUI() {
            chart = createChart(dataset);

            setLayout(new BorderLayout());
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            chartPanel.setBackground(Color.white);
            add(chartPanel, BorderLayout.NORTH);

            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());

            BlinkButton startBtn = new BlinkButton("Start...");
            BlinkButton stopBtn = new BlinkButton("Stopp");
            JButton saveBtn=new JButton("Lagre...");
            JButton zeroBtn = new JButton("Nullstill");
            JButton infoBtn = new JButton("Enhetsinfo...");





            startBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            startBtn.addActionListener(e -> {
                String tauString = "Tau";
                String kjettingString = "Kjetting";
                String annetString = "Annet";
                String specimenType = tauString;

                if (s != null && s.isPaused()) {
                    stopBtn.setBlinking(false);
                    s.setPaused(false);
                } else {

                    JTextField testIDField = new JTextField(5);
                    JTextField customerField = new JTextField(5);
                    //JTextField specimenTypeField = new JTextField(5);
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


                    if (kjettingRadioButton.isSelected()) {
                        specimenType = kjettingString;
                    } else if (tauRadioButton.isSelected()){
                        specimenType = tauString;
                    } else {
                        specimenType = annetString;
                    }

                    try {
                        testIDField.setText(String.valueOf(Integer.parseInt(Utils.findPreviousTestId()) + 1));
                    } catch (IOException | NumberFormatException ex) {
                        System.out.println("Could not find previous test ID!");
                        try {
                            testIDField.setText(Utils.findPreviousTestId());
                        } catch (IOException exc) {
                            exc.printStackTrace();
                        }
                        ex.printStackTrace();
                    }

                    JPanel radioButtonPanel = new JPanel();
                    radioButtonPanel.setLayout(new FlowLayout());
                    radioButtonPanel.add(tauRadioButton);
                    radioButtonPanel.add(kjettingRadioButton);
                    radioButtonPanel.add(annetRadioButton);
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
                    myPanel.add(radioButtonPanel);
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

                    String[][] metadata = Utils.loadMetadata(this);
                    if (metadata[6][0].length() > 0) {
                        testIDField.setText(metadata[0][1]);
                        customerField.setText(metadata[2][1]);
                        localeField.setText(metadata[3][1]);
                        specimenNameField.setText(metadata[4][1]);
                        testCommentField.setText(metadata[5][1]);
                        operatorField.setText(metadata[6][1]);
                    }
                    int result = JOptionPane.showConfirmDialog(null, myPanel,
                            "Sleng inn testdata!", JOptionPane.OK_CANCEL_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        strainTestObject = new StrainTestObject(testIDField.getText(), customerField.getText(), localeField.getText(), specimenType, specimenNameField.getText(), testCommentField.getText(), operatorField.getText(), this);
                        JOptionPane.showMessageDialog(null, strainTestObject.validateInput());
                        if (strainTestObject.validateInput().equals("OK")) {
                            startSerialReader();
                            startBtn.setBlinking(true);
                            Utils.saveMetadata(strainTestObject);
                        } else {
                            startBtn.doClick();
                        }
                    /*System.out.println("ID: " + testIDField.getText() + "\tKunde: " + customerField.getText() + "\tLokalitet: " + localeField.getText() + "\tType: " +
                            specimenTypeField.getText() + "\tNavn: " + specimenNameField.getText() + "\tKommentar: " + testCommentField.getText() +
                            "\tOperatør: " + operatorField.getText());
                    //Copy text from fields to String variables in global scope */

                    } else {
                        strainTestObject = new StrainTestObject(this);
                        startSerialReader();
                        startBtn.setBlinking(true);
                    }
                }
            });


            rangeAxis.setAutoRangeMinimumSize(200);
            stopBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            stopBtn.addActionListener(e -> {
                if (s.isPaused()) {
                    stopBtn.setBlinking(false);
                    s.setPaused(false);
                    s.end();
                } else {
                    stopBtn.setBlinking(true);
                    startBtn.setBlinking(false);
                    s.pause();
                }
            });
            saveBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
            saveBtn.addActionListener(e -> {
                File directory = new File(System.getProperty("user.home") + File.separator + "tests");
                if (! directory.exists()) {
                    directory.mkdir();
                }
                String filenameString = "new_test";

                LocalDateTime myDateObj = LocalDateTime.now();
                DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH_mm_ss");
                String timestamp = myDateObj.format(myFormatObj);

                if (strainTestObject.validateInput().equals("OK")) {
                    filenameString = (strainTestObject.getTestID() +1);
                }

                final JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new File(System.getProperty("user.home") + File.separator + "tests"));
                fc.setDialogTitle("Specify a file to save");
                fc.setSelectedFile(new File(filenameString + "__" + timestamp));


                int userSelection = fc.showSaveDialog(saveBtn);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fc.getSelectedFile();
                    System.out.println("Save as file: " + fileToSave.getAbsolutePath());

                    Utils.appendToExcelDatabase(strainTestObject, timestamp);
                    Utils.storeDataSet(chart,fileToSave.getAbsolutePath(),strainTestObject);
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
                    series.delete(0, series.getItemCount() - 1);
                    s.setActivateZeroBalance(true);
                    maxLabel.setText("0.00");
                } else {
                    JOptionPane.showMessageDialog(null,"Start prosessen for å kunne nullstille!");
                }
            });



            infoBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
            infoBtn.addActionListener(e -> {
                if (s.getRunning()) {
                JOptionPane.showMessageDialog(null," \t\t---- DEVICE ID CONFIG ----\n"+
                        "ID: " + s.deviceId + "\t BAUDRATE: " + s.baudRate + "\t REFRESH RATE: " + s.digitalFilter +
                        "\nFW: " + s.instrumentVersion + "\t RESOLUTION: " + s.resolution + "\t SCALE: " + s.fullScale);
                } else {
                    JOptionPane.showMessageDialog(null,"Start prosessen for å vise enhetsinformasjon!");
                }
            });

            JButton zeroExtBtn = new JButton("Null ekst.");
            zeroExtBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
            zeroExtBtn.addActionListener(e -> {
                strainTestObject.setExtUserOffset(Double.parseDouble(extDataLbl.getText()));
            });


            JLabel valueTxtLbl1 =new JLabel("Verdi ");
            valueLabel = new JLabel("0.00");
            valueLabel.setPreferredSize(new Dimension(150,40));
            valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
            valueLabel.setOpaque(true);
            valueLabel.setForeground(Color.blue);
            valueLabel.setBackground(Color.lightGray);
            JLabel valueTxtLbl2 =new JLabel("kg   ");
            JLabel maxTxtLbl1 =new JLabel("Maks ");
            maxLabel =new JLabel("0.00");
            maxLabel.setPreferredSize(new Dimension(150,40));
            maxLabel.setHorizontalAlignment(SwingConstants.CENTER);
            maxLabel.setOpaque(true);
            maxLabel.setForeground(Color.blue);
            maxLabel.setBackground(Color.lightGray);

            JLabel maxTxtLbl2 =new JLabel("kg   ");
            JLabel offsetTxtLbl1 = new JLabel("Offset ");
            offsetLabel = new JLabel(("0.00"));
            offsetLabel.setPreferredSize(new Dimension(150,40));
            offsetLabel.setHorizontalAlignment(SwingConstants.CENTER);
            JLabel offsetTxtLbl2 = new JLabel("kg   ");

            float fontSz = 26.0f;
            valueLabel.setFont (valueLabel.getFont ().deriveFont (fontSz));
            maxLabel.setFont (maxLabel.getFont ().deriveFont (fontSz));
            offsetLabel.setFont (offsetLabel.getFont ().deriveFont (fontSz));
            offsetLabel.setOpaque(true);
            offsetLabel.setForeground(Color.blue);
            offsetLabel.setBackground(Color.lightGray);
            valueTxtLbl1.setFont (valueTxtLbl1.getFont ().deriveFont (fontSz));
            valueTxtLbl2.setFont (valueTxtLbl2.getFont ().deriveFont (fontSz));
            maxTxtLbl1.setFont (maxTxtLbl1.getFont ().deriveFont (fontSz));
            maxTxtLbl2.setFont (maxTxtLbl2.getFont ().deriveFont (fontSz));
            offsetTxtLbl1.setFont (offsetTxtLbl1.getFont ().deriveFont (fontSz));
            offsetTxtLbl2.setFont (offsetTxtLbl1.getFont ().deriveFont (fontSz));

            extDataLbl = new JLabel("0.0");
            JLabel extUnitLbl = new JLabel("mm");
            JLabel extTextLbl = new JLabel("Ekst. ");
            extDataLbl.setPreferredSize(new Dimension(150,40));
            extDataLbl.setHorizontalAlignment(SwingConstants.CENTER);
            extDataLbl.setOpaque(true);
            extDataLbl.setForeground(Color.blue);
            extDataLbl.setBackground(Color.lightGray);
            extDataLbl.setFont(extDataLbl.getFont().deriveFont(fontSz));

            extUnitLbl.setFont(extUnitLbl.getFont().deriveFont(fontSz));
            extTextLbl.setFont(extTextLbl.getFont().deriveFont(fontSz));



            JPanel labelPanel = new JPanel();
            labelPanel.add(valueTxtLbl1,BorderLayout.WEST);
            labelPanel.add(valueLabel,BorderLayout.WEST);
            labelPanel.add(valueTxtLbl2,BorderLayout.WEST);
            labelPanel.add(maxTxtLbl1,BorderLayout.WEST);
            labelPanel.add(maxLabel,BorderLayout.WEST);
            labelPanel.add(maxTxtLbl2,BorderLayout.WEST);

            labelPanel.add(offsetTxtLbl1,BorderLayout.EAST);
            labelPanel.add(offsetLabel,BorderLayout.EAST);
            labelPanel.add(offsetTxtLbl2,BorderLayout.EAST);

            labelPanel.add(extTextLbl,BorderLayout.EAST);
            labelPanel.add(extDataLbl,BorderLayout.EAST);
            labelPanel.add(extUnitLbl,BorderLayout.EAST);


            JPanel buttonPanel = new JPanel(); //Make the button panel
            buttonPanel.add(startBtn);
            buttonPanel.add(stopBtn);
            buttonPanel.add(zeroBtn);


            buttonPanel.add(saveBtn);
            buttonPanel.add(infoBtn);

            buttonPanel.add(zeroExtBtn);

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
