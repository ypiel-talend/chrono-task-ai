module com.chrono.task {
    requires transitive javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires static lombok;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires flexmark;
    requires org.slf4j;

    requires java.net.http;
    requires java.desktop;

    opens com.chrono.task to javafx.fxml, javafx.graphics;
    opens com.chrono.task.controller to javafx.fxml;
    opens com.chrono.task.model to com.fasterxml.jackson.databind;
    opens com.chrono.task.persistence to com.fasterxml.jackson.databind;

    exports com.chrono.task;
    exports com.chrono.task.controller;
    exports com.chrono.task.model;
    exports com.chrono.task.service;
    exports com.chrono.task.persistence;
}