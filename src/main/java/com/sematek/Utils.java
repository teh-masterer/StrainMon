package com.sematek;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.chart.ChartUtils;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;


//Utility and IO class

public class Utils {

    static final String FILENAME = System.getProperty("user.home") + File.separator + "tests" + File.separator + "update.xls";
    static final String METADATA_PATH = System.getProperty("user.home") + File.separator + "tests" + File.separator + "metadata";


    public static void storeDataSet(JFreeChart chart, String filename, StrainTestObject sto) {
        java.util.List<String> csv = new ArrayList<>();
        csv.add(String.format("%s, %s", "testId", sto.getTestID()));
        csv.add(String.format("%s, %s", "customer", sto.getCustomer()));
        csv.add(String.format("%s, %s", "locale", sto.getLocale()));
        csv.add(String.format("%s, %s", "specimenType", sto.getSpecimenType()));
        csv.add(String.format("%s, %s", "specimenName", sto.getSpecimenName()));
        csv.add(String.format("%s, %s", "testComment", sto.getTestComment()));
        csv.add(String.format("%s, %s", "operator", sto.getOperator()));
        csv.add(String.format("%s, %s", "maxValue", sto.getMaxValue()));

        storeDataSet(chart, filename, csv);
    }

    public static void saveMetadata(StrainTestObject sto) {
        java.util.List<String> csv = new ArrayList<>();
        csv.add(String.format("%s, %s", "testId", sto.getTestID()));
        csv.add(String.format("%s, %s", "customer", sto.getCustomer()));
        csv.add(String.format("%s, %s", "locale", sto.getLocale()));
        csv.add(String.format("%s, %s", "specimenType", sto.getSpecimenType()));
        csv.add(String.format("%s, %s", "specimenName", sto.getSpecimenName()));
        csv.add(String.format("%s, %s", "testComment", sto.getTestComment()));
        csv.add(String.format("%s, %s", "operator", sto.getOperator()));
        csv.add(String.format("%s, %s", "maxValue", sto.getMaxValue()));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(METADATA_PATH + ".csv"))) {
            for (String line : csv) {
                writer.append(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write dataset", e);
        }

    }
    public static String[][] loadMetadata(Grapher g) {
        String csvFile = METADATA_PATH + ".csv";
        String line = "";
        String cvsSplitBy = ",";
        String[][] metadata = new String[10][2];
        int lineCounter = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {

            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] newLine = line.split(cvsSplitBy);
                for (String s : newLine) {
                    System.out.println(s);
                }
                metadata[lineCounter] = newLine;
                lineCounter++;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Couldn't find metadata file!");
        }
        return metadata;
    }


    private static void storeDataSet(JFreeChart chart, String filename, List<String> csv) {
        if (chart.getPlot() instanceof XYPlot) {
            XYDataset xyDataset = chart.getXYPlot().getDataset();
            int seriesCount = xyDataset.getSeriesCount();
            for (int i = 0; i < seriesCount; i++) {
                int itemCount = xyDataset.getItemCount(i);
                for (int j = 0; j < itemCount; j++) {
                    Number x = xyDataset.getX(i, j);
                    Number y = xyDataset.getY(i, j);
                    csv.add(String.format("%s, %s", x, y));
                }
            }

        }  else {
            throw new IllegalStateException("Unknown dataset");
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename + ".csv"))) {
            for (String line : csv) {
                writer.append(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write dataset", e);
        }
    }

    static void saveChartAsPng (ChartPanel chartPanel, String filename) throws IOException {
        OutputStream out = new FileOutputStream(filename  + ".png");
        ChartUtils.writeChartAsPNG(out,
                chartPanel.getChart(),
                chartPanel.getWidth(),
                chartPanel.getHeight());
    }


    static void appendToExcelDatabase(StrainTestObject sto, String timestamp) {
        if (sto.validateInput().equals("OK")) {
            try {
                FileInputStream file = new FileInputStream(new File(FILENAME));

                HSSFWorkbook workbook = new HSSFWorkbook(file);
                HSSFSheet sheet = workbook.getSheetAt(0);
                int insertionRowNo = sheet.getLastRowNum();
                if (sheet.getPhysicalNumberOfRows()>0) {
                    insertionRowNo++;
                }
                Row newRow = sheet.createRow(insertionRowNo);
                newRow.createCell(0).setCellValue(sto.getTestID());
                newRow.createCell(1).setCellValue(sto.getCustomer());
                newRow.createCell(2).setCellValue(sto.getLocale());
                newRow.createCell(3).setCellValue(sto.getSpecimenType());
                newRow.createCell(4).setCellValue(sto.getSpecimenName());
                newRow.createCell(5).setCellValue(sto.getTestComment());
                newRow.createCell(6).setCellValue(sto.getOperator());
                newRow.createCell(7).setCellValue(sto.getMaxValue());
                newRow.createCell(8).setCellValue(timestamp);
                file.close();

                FileOutputStream outFile = new FileOutputStream(new File("update.xls"));
                workbook.write(outFile);
                outFile.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Data from StrainTestObject failed validation, skipping Excel export!");
        }
    }
    public static String findPreviousTestId () throws IOException {
        String err = "Ukjent";
        String ret;
        FileInputStream file;
        try {
            file = new FileInputStream(new File(FILENAME));
        } catch (FileNotFoundException e) {
            System.out.println("Excel file not found at " + FILENAME);
            return err;
        }
        HSSFSheet sheet = new HSSFWorkbook(file).getSheetAt(0);
        try {
            ret = sheet.getRow(sheet.getLastRowNum()).getCell(0).toString();
        } catch (NumberFormatException e) {
            System.out.println("Could not read number from test log excel-file!");
            return err;
        }
        return ret;
    }
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
