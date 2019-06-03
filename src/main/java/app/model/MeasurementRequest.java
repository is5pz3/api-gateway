package app.model;

public class MeasurementRequest {
    private String sensor_id;
    private String time_window;
    private String calculation_frequency;
    private String token;

    public MeasurementRequest() {}

    public MeasurementRequest(String sensor_id, String time_window, String calculation_frequency, String token) {
        this.sensor_id = sensor_id;
        this.time_window = time_window;
        this.calculation_frequency = calculation_frequency;
        this.token = token;
    }

    public String getSensor_id() {
        return sensor_id;
    }

    public void setSensor_id(String sensor_id) {
        this.sensor_id = sensor_id;
    }

    public String getTime_window() {
        return time_window;
    }

    public void setTime_window(String time_window) {
        this.time_window = time_window;
    }

    public String getCalculation_frequency() {
        return calculation_frequency;
    }

    public void setCalculation_frequency(String calculation_frequency) {
        this.calculation_frequency = calculation_frequency;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String toJson() {
        return  "{" +
                    "\"sensor_id\":\"" + sensor_id + "\"," +
                    "\"time_window\":" + time_window + "," +
                    "\"calculation_frequency\":" + calculation_frequency + "," +
                    "\"token\":\"" + token + "\"" +
                "}";
    }
}
