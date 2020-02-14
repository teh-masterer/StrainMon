package com.sematek;

import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;

import java.util.HashMap;

import static java.awt.EventQueue.invokeLater;


// Data storage class for tests
public class StrainTestObject {
    private final String testID;
    private final String customer;
    private final String specimenType;
    private final String specimenName;
    private String testComment;
    private String operator;
    private final String locale;

    private double currentValue;
    private double offsetValue;

    final JLabel maxValueLabel;
    final JLabel currentValueLabel;
    final JLabel offsetValueLabel;

    private final TimeSeries series;
    public final TimeSeriesCollection dataset;


    double recentExtVal;


    public void setExtUserOffset(double extUserOffset) {
        this.extUserOffset = extUserOffset;
        g.extDataLbl.setText(String.valueOf(Utils.round((recentExtVal-extUserOffset),1)));
    }

    double extUserOffset;

    private Grapher g;

    public StrainTestObject(String testID, String customer, String locale, String specimenType, String specimenName, String testComment, String operator) {
        this.testID = testID;
        this.customer = customer;
        this.specimenType = specimenType;
        this.specimenName = specimenName;
        this.testComment = testComment;
        this.operator = operator;
        this.locale = locale;

        series = new TimeSeries("Strekk");
        dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        currentValueLabel = new JLabel("0.00");
        maxValueLabel = new JLabel("0.00");
        offsetValueLabel = new JLabel("0.00");
   }

    public StrainTestObject(String testID, String customer, String locale, String specimenType, String specimenName, String testComment, String operator, Grapher g) {
        this(testID, customer, locale, specimenType, specimenName, testComment, operator);
        this.g = g;
    }


    public StrainTestObject() {
        this.testID = "unknown";
        this.customer = "unknown";
        this.specimenType = "unknown";
        this.specimenName = "unknown";
        this.testComment = "unknown";
        this.operator = "unknown";
        this.locale = "unknown";

        series = new TimeSeries("Strekk");
        dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        currentValueLabel = new JLabel("0.00");
        maxValueLabel = new JLabel("0.00");
        offsetValueLabel = new JLabel("0.00");

        recentExtVal = 0;
        extUserOffset = 0;
    }

    public StrainTestObject(Grapher g) {
        this();
        this.g = g;
    }

    void addDataToGraph(double val) {
        invokeLater(() -> g.series.add(new Millisecond(), val));
        updateValueLabels(val);
    }



    private void updateValueLabels (double val) {
        setCurrentValue(val);
        if (getMaxValue()< val) {
            setMaxValue(val);
        }
    }

    void updateExtensiometerData(String s) {
        recentExtVal = Double.parseDouble(s);
        g.extDataLbl.setText(String.valueOf(Utils.round((recentExtVal-extUserOffset),2)));
        System.out.println("Extension: " + s + " " + recentExtVal + ", displayed: " + (recentExtVal-extUserOffset));
    }


    public void removeAllSeries() {
        dataset.removeAllSeries();
    }
    public String validateInput() {
        if (testComment.isEmpty()) {
            testComment = "Ingen merknad.";
        }
        if (operator.isEmpty()) {
            operator = "Sematek";
        }
        if (!testID.matches("[0-9]+")) {
            return "Test ID can only be numbers!";
        } else if (customer.isEmpty()) {
            return "Customer name cannot be empty!";
        } else if (locale.isEmpty()) {
            return "Locale cannot be empty!";
        } else if (!(specimenType.equals("Tau")) && !(specimenType.equals("Kjetting"))) {
            return "Specimen type can either be \"Tau\" or \"Kjetting\", input is " + specimenType;
        } else if (specimenName.isEmpty()) {
            return "Specimen name cannot be empty!";

        } else {
            return "OK";
        }

    }


    public String getTestID() {
        return testID;
    }

    public String getCustomer() {
        return customer;
    }

    public String getSpecimenType() {
        return specimenType;
    }

    public String getSpecimenName() {
        return specimenName;
    }

    public String getTestComment() {
        return testComment;
    }

    public String getOperator() {
        return operator;
    }

    public String getLocale() {
        return locale;
    }

    public double getMaxValue() {
        return Double.parseDouble(g.maxLabel.getText());
    }

    public void setMaxValue(double maxValue) {
        maxValueLabel.setText(String.valueOf(maxValue));
        g.maxLabel.setText(String.valueOf(Utils.round(maxValue,2)));

    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        currentValueLabel.setText(String.valueOf(Utils.round(currentValue,2)));
        this.currentValue = currentValue;
        g.valueLabel.setText(String.valueOf(Utils.round(currentValue,2)));

    }

    public double getOffsetValue() {
        return offsetValue;
    }

    public void setOffsetValue(double offsetValue) {
        offsetValueLabel.setText(String.valueOf(offsetValue));
        this.offsetValue = offsetValue;
        g.offsetLabel.setText(String.valueOf(Utils.round(offsetValue,2)));
    }

}
