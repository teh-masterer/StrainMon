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
    private String thickness, length, preInnerDiameter, postInnerDiameter, fractureDescription;



    public void setOffsetExtValue(double offsetExtValue) {
        this.offsetExtValue = offsetExtValue;
        g.extDataLbl.setText(String.valueOf(Utils.round((currentExtValue - offsetExtValue),1)));
    }


    private Grapher g;

    public StrainTestObject(String testID, String customer, String locale, String specimenType, String specimenName, String testComment, String operator, String diameter) {
        this.testID = testID;
        this.customer = customer;
        this.specimenType = specimenType;
        this.specimenName = specimenName;
        this.testComment = testComment;
        this.operator = operator;
        this.locale = locale;
        this.thickness = diameter;
        preInnerDiameter = "Ukjent";
        length = "Ukjent";
        postInnerDiameter = "Ukjent";
        fractureDescription = "Ukjent";;

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

    public StrainTestObject(String testID, String customer, String locale, String specimenType, String specimenName, String testComment, String operator, String diameter, Grapher g) {
        this(testID, customer, locale, specimenType, specimenName, testComment, operator, diameter);
        this.g = g;
    }

    public StrainTestObject() {
        this("unknown","unknown","unknown","unknown","unknown","unknown","unknown","unknown");
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
        String ret ="";
        if (testComment.isEmpty()) {
            testComment = "Ingen merknad.";
        }
        if (operator.isEmpty()) {
            operator = "Sematek";
        }
        if (!testID.matches("[0-9]+")) {
            ret.concat("Test ID can only be numbers!\n");
        }
        if (customer.isEmpty()) {
            ret.concat("Customer name cannot be empty!\n");
        }
        if (locale.isEmpty()) {
            ret.concat("Locale cannot be empty!\n");
        }
        if (!preInnerDiameter.matches("[0-9]+")) {
            ret.concat("Test ID can only be numbers!\n");
        }
        if (specimenName.isEmpty()) {
            ret.concat("Specimen name cannot be empty!\n");
        }
        if (ret.length() == 0) {
            ret = "OK";
        }
        return ret;
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


    public String getThickness() {
        return thickness;
    }

    public void setThickness(String thickness) {
        this.thickness = thickness;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getPreInnerDiameter() {
        return preInnerDiameter;
    }

    public void setPreInnerDiameter(String preInnerDiameter) {
        this.preInnerDiameter = preInnerDiameter;
    }

    public String getPostInnerDiameter() {
        return postInnerDiameter;
    }

    public void setPostInnerDiameter(String postInnerDiameter) {
        this.postInnerDiameter = postInnerDiameter;
    }

    public String getFractureDescription() {
        return fractureDescription;
    }

    public void setFractureDescription(String fractureDescription) {
        this.fractureDescription = fractureDescription;
    }
}
