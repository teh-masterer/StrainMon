package com.sematek;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.MovingAverage;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;


public class Grapher extends JFrame {

    public TimeSeries series;
    public TimeSeries series2; //Moving average
    public JFreeChart chart;
    public XYPlot plot;
    public TimeSeriesCollection dataset;
    public SerialReader serialReader;
    public JLabel l2;
    public JLabel l5;
    public JLabel l8;
    public long startTimeMillis;
    public boolean needToCalculateAverage;

    public Grapher() {
        initUI();
        startTimeMillis = System.currentTimeMillis();
        l2.setText("0");
        l5.setText("0");
    }

    private void startSerialReader() throws InterruptedException {

        serialReader = new SerialReader(this);
        new Thread(serialReader).start();
        dataset.removeAllSeries();
        series = new TimeSeries("Strekk");
        dataset.addSeries(series);
        try {
            dataset.validateObject();
        } catch (InvalidObjectException e) {
            e.printStackTrace();
        }

    }


        private void initUI() {

            series = new TimeSeries("Strekk");
            dataset = new TimeSeriesCollection();
            dataset.addSeries(series);
            chart = createChart(dataset);

            setLayout(new BorderLayout());
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            chartPanel.setBackground(Color.white);
            add(chartPanel,BorderLayout.CENTER);


            JButton b1=new JButton("Start");
            b1.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        startSerialReader();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            });
            JButton b2=new JButton("Stop");
            b2.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    serialReader.end();
                }
            });
            JButton b3=new JButton("Eksporter...");
            b3.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final JFileChooser fc = new JFileChooser();
                    fc.setCurrentDirectory(new File(System.getProperty("user.home")));
                    fc.setDialogTitle("Specify a file to save");

                    int userSelection = fc.showSaveDialog(b3);

                    if (userSelection == JFileChooser.APPROVE_OPTION) {
                        File fileToSave = fc.getSelectedFile();
                        System.out.println("Save as file: " + fileToSave.getAbsolutePath());
                        storeDataSet(chart,fileToSave.getAbsolutePath());

                    }
                }
            });
            JButton b4 = new JButton("Nullstill");
            b4.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int itemCount = series.getItemCount();
                    if (itemCount < 10) {
                        JOptionPane.showMessageDialog(b4,
                                "Vent litt!!!",
                                "Alvorlig advarsel",
                                JOptionPane.WARNING_MESSAGE);
                    } else {
                        double sumOfLastTenReadings = 0;
                        for (int i = 1; i < 11; i++) {
                            sumOfLastTenReadings += (double) series.getValue(itemCount - i);
                        }
                        double averageLastTenReadings = sumOfLastTenReadings / 10;
                        System.out.println("Average of last ten readings is: " + averageLastTenReadings);
                        serialReader.setOffset(averageLastTenReadings);
                        l8.setText(String.valueOf(averageLastTenReadings));
                        l5.setText("0");
                        series.delete(0, series.getItemCount());
                    }
                }
            });
            JLabel l1 =new JLabel("Verdi: ");
            l2 =new JLabel("0");
            JLabel l3 =new JLabel("kg");
            JLabel l4 =new JLabel("Maks: ");
            l5 =new JLabel("0");
            JLabel l6 =new JLabel("kg");
            JLabel l7 = new JLabel("Kalkulert nullpunkt: ");
            l8 = new JLabel(("0.0"));


            JPanel jPanel = new JPanel();
            jPanel.add(b1);
            jPanel.add(b2);
            jPanel.add(b4);
            jPanel.add(l1);
            jPanel.add(l2);
            jPanel.add(l3);
            jPanel.add(l4);
            jPanel.add(l5);
            jPanel.add(l6);
            jPanel.add(l7);
            jPanel.add(l8);
            jPanel.add(b3);

            add(jPanel,BorderLayout.SOUTH);
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

            var renderer = new XYSplineRenderer();
            renderer.setSeriesPaint(0, Color.RED);
            renderer.setSeriesStroke(0, new BasicStroke(2.0f));

            plot.setRenderer(renderer);
            plot.setBackgroundPaint(Color.lightGray);

            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.BLACK);

            plot.setDomainGridlinesVisible(true);
            plot.setDomainGridlinePaint(Color.BLACK);


            chart.setTitle(new TextTitle("Målte strekkrefter",
                            new Font("Serif", java.awt.Font.BOLD, 18)
                    )
            );

            return chart;
        }

    private void storeDataSet(JFreeChart chart, String filename) {
        java.util.List<String> csv = new ArrayList<>();
        if (chart.getPlot() instanceof XYPlot) {
            Dataset dataset = chart.getXYPlot().getDataset();
            XYDataset xyDataset = (XYDataset) dataset;
            int seriesCount = xyDataset.getSeriesCount();
            for (int i = 0; i < seriesCount; i++) {
                int itemCount = xyDataset.getItemCount(i);
                for (int j = 0; j < itemCount; j++) {
                    Comparable key = xyDataset.getSeriesKey(i);
                    Number x = xyDataset.getX(i, j);
                    Number y = xyDataset.getY(i, j);
                    csv.add(String.format("%s, %s, %s", key, x, y));
                }
            }

        } else if (chart.getPlot() instanceof CategoryPlot) {
            Dataset dataset = chart.getCategoryPlot().getDataset();
            CategoryDataset categoryDataset = (CategoryDataset) dataset;
            int columnCount = categoryDataset.getColumnCount();
            int rowCount = categoryDataset.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < columnCount; j++) {
                    Comparable key = categoryDataset.getRowKey(i);
                    Number n = categoryDataset.getValue(i, j);
                    csv.add(String.format("%s, %s", key, n));
                }
            }
        } else {
            throw new IllegalStateException("Unknown dataset");
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename + ".csv"));) {
            for (String line : csv) {
                writer.append(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write dataset", e);
        }
    }

}
