package application;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TableValue {

    private IntegerProperty id;
    private StringProperty name, os, ol, is, il;

    public TableValue(String n, int anID, Values v, double mmperpx) {
    	if(n == null) {
    		name = new SimpleStringProperty("null");;
    	}
    	name = new SimpleStringProperty(n);
        id = new SimpleIntegerProperty(anID);
        os = new SimpleStringProperty(String.format("%1$.2f", v.getShorterOuterDiameter() * mmperpx));
        ol = new SimpleStringProperty(String.format("%1$.2f", v.getLongerOuterDiameter() * mmperpx));
        is = new SimpleStringProperty(String.format("%1$.2f", v.getShorterinnerDiameter() * mmperpx));
        il = new SimpleStringProperty(String.format("%1$.2f", v.getLongerInnerDiameter() * mmperpx));
    }

    public StringProperty nameProperty() {
        return name;
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty osProperty() {
        return os;
    }
    public StringProperty olProperty() {
        return ol;
    }
    public StringProperty isProperty() {
        return is;
    }
    public StringProperty ilProperty() {
        return il;
    }
}
