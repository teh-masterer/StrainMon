package com.sematek;

import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.awt.*;

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
    private double currentExtValue;
    private double offsetExtValue;
    private double offsetValue;
    private double elongatedValue;
    private double elongatedDistance;
    public final TimeSeriesCollection dataset;
    private boolean elongationTestRunning;
    private double stopElongation;
    private String preB, preL, preD, postB, fractureDescription;



    public void setOffsetExtValue(double offsetExtValue) {
        this.offsetExtValue = offsetExtValue;
        g.extDataLbl.setText(String.valueOf(Utils.round((currentExtValue - offsetExtValue),1)));
    }


    private Grapher g;

    public StrainTestObject(String testID, String customer, String locale, String specimenType, String specimenName, String testComment, String operator) {
        this.testID = testID;
        this.customer = customer;
        this.specimenType = specimenType;
        this.specimenName = specimenName;
        this.testComment = testComment;
        this.operator = operator;
        this.locale = locale;
        preB = "ukjent";
        preL = "ukjent";
        preD = "ukjent";
        postB = "ukjent";
        fractureDescription = "ukjent";;


        TimeSeries series = new TimeSeries("Strekk");
        dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        currentExtValue = 0;
        offsetExtValue = 0;
        elongatedValue = 0;
        elongatedDistance = 0;

        elongationTestRunning = false;
        offsetValue = Config.getInstance().USER_OFFSET;
   }

    public StrainTestObject(String testID, String customer, String locale, String specimenType, String specimenName, String testComment, String operator, Grapher g) {
        this(testID, customer, locale, specimenType, specimenName, testComment, operator);
        this.g = g;
    }

    public StrainTestObject() {
        this("unknown","unknown","unknown","unknown","unknown","unknown","unknown");
    }

    public StrainTestObject(Grapher g) {
        this();
        this.g = g;
    }

    void addDataToGraph(double val) {
        invokeLater(() -> g.series.add(new Millisecond(), val));
        updateValueLabels(val);
    }
    void startElongationTest(String measuredLength) {
        stopElongation = Double.parseDouble(measuredLength)/100 + currentExtValue; //Finner 1% av målt lengde, plusser på nåværende lengde for å sette mål
        zeroExtOffset();
        elongationTestRunning = true;
        g.elongatedValueLabel.setText("---");
        g.elongatedDistanceLabel.setText(String.valueOf(Utils.round((stopElongation - currentExtValue),2)));
        g.strainStartBtn.setBackground(Color.yellow);
        System.out.println("Elongation test started at " + currentExtValue + " and load value " + currentValue + " kg.");
    }


    private void updateValueLabels (double val) {
        setCurrentValue(Math.round(val));
        if (getMaxValue()< val) {
            setMaxValue(Math.round(val));
        }
    }

    void addExtData(String s) {
        currentExtValue = Double.parseDouble(s);
        g.extDataLbl.setText(String.valueOf(Utils.round((currentExtValue- offsetExtValue),2)));
        System.out.println("Extension: " + s + " " + currentExtValue + ", displayed: " + (currentExtValue - offsetExtValue));

        if (elongationTestRunning) {
            if (currentExtValue >= stopElongation) {
                elongationTestRunning = false;
                System.out.println("Elongation test stopped at " + currentExtValue + " and load value " + currentValue + " kg.");
                setElongatedValue(currentValue);
                setElongatedDistance(currentExtValue - offsetExtValue);
                g.strainStartBtn.setBackground(null);

            } else {
                System.out.println(currentExtValue + " / " + stopElongation);
            }
        }
    }

    void zeroExtOffset() {
        offsetExtValue = currentExtValue;
        System.out.println("Extensiometer zeroed, offset value: " + offsetValue + ", current ext value: " + currentExtValue);
        elongationTestRunning = false;
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
        g.maxLabel.setText(String.valueOf(Utils.round(maxValue, 2)));
        if (g.plot.getRangeAxis().getRange().getUpperBound() <= maxValue) {
            g.plot.getRangeAxis().setRange(-100, maxValue + 1000);
        }
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
        g.valueLabel.setText(String.valueOf(Utils.round(currentValue,1)));

    }
    public void setElongatedValue (double elongatedValue) {
        this.elongatedValue = elongatedValue;
        g.elongatedValueLabel.setText(String.valueOf(Utils.round(elongatedValue,1)));
    }
    public void setElongatedDistance (double elongatedDistance) {
        this.elongatedDistance = elongatedDistance;
        g.elongatedDistanceLabel.setText(String.valueOf(Utils.round(elongatedDistance,1)));

    }

    public double getOffsetValue() {
        return offsetValue;
    }

    public void setOffsetValue(double offsetValue) {
        this.offsetValue = offsetValue;
        g.offsetLabel.setText(String.valueOf(Utils.round(offsetValue,1)));
    }

    public double getElongatedValue() {
        return elongatedValue;
    }

    public double getElongatedDistance() {
        return elongatedDistance;
    }


    public String getPreB() {
        return preB;
    }

    public void setPreB(String preB) {
        this.preB = preB;
    }

    public String getPreL() {
        return preL;
    }

    public void setPreL(String preL) {
        this.preL = preL;
    }

    public String getPreD() {
        return preD;
    }

    public void setPreD(String preD) {
        this.preD = preD;
    }

    public String getPostB() {
        return postB;
    }

    public void setPostB(String postB) {
        this.postB = postB;
    }

    public String getFractureDescription() {
        return fractureDescription;
    }

    public void setFractureDescription(String fractureDescription) {
        this.fractureDescription = fractureDescription;
    }
}
