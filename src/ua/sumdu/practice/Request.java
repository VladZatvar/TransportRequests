package ua.sumdu.practice;

import java.time.LocalDate;

public class Request {
    private final int requestNumber;
    private final String vehicle;
    private final String route;
    private final LocalDate date;

    public Request(int requestNumber, String vehicle, String route, LocalDate date) {
        this.requestNumber = requestNumber;
        this.vehicle = vehicle;
        this.route = route;
        this.date = date;
    }

    public int getRequestNumber() {
        return requestNumber;
    }

    public String getVehicle() {
        return vehicle;
    }

    public String getRoute() {
        return route;
    }

    public LocalDate getDate() {
        return date;
    }
}
