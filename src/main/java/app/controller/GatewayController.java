package app.controller;

import app.model.Measurement;
import app.model.MeasurementRequest;
import app.model.SensorMeasurements;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
public class GatewayController {
    static private final List<String> monitors = Arrays.asList(
            "https://monitor-prodd.herokuapp.com/",
            "https://monitor2-prodd.herokuapp.com/"
    );
    static private final String authenticationService = "https://auth-prodd.herokuapp.com/";

    static private final String HOSTS = "hosts";
    static private final String MEASUREMENTS = "measurements";

    private Map<String, String> userToMeasurement = new TreeMap<>();

    @GetMapping(value = "/" + HOSTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getHosts(@RequestParam("hostName") String hostName) {
        return queryGetAllMonitors(HOSTS + "?hostName=" + hostName);
    }

    @GetMapping(value = "/" + MEASUREMENTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAllMeasurements() {
        return queryGetAllMonitors(MEASUREMENTS);
    }

    @GetMapping(value = "/" + MEASUREMENTS + "/{sensor_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSensorMeasurements(
            @PathVariable(value = "sensor_id") String sensorId,
            @RequestParam(value = "data_count", defaultValue = "10") String dataCount,
            @RequestParam(value = "since", required = false) String since,
            @RequestParam(value = "to", required = false) String to) {
        String uri = MEASUREMENTS + "/" + sensorId + "?data_count=" + dataCount;
        if (since != null)
            uri += "&since=" + since;
        if (to != null)
            uri += "&to=" + to;

        return queryGetAllSensorMeasurements(uri);
    }

    @PostMapping(value = "/" + MEASUREMENTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createMeasurement(@RequestBody MeasurementRequest request) {
        ResponseEntity<String> authenticationResponse = authenticate(request.getToken());
        ResponseEntity<String> response;

        if (authenticationResponse.getStatusCode() == HttpStatus.OK) {
            JSONObject obj = new JSONObject(authenticationResponse.getBody());
            String user = obj.getString("login");
            response = new ResponseEntity<>(queryPostAllMonitors(request.toJson(), user), HttpStatus.CREATED);
        }
        else if (authenticationResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
            response = new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        else {
            response = new ResponseEntity<>(authenticationResponse.getStatusCode());
        }

        return response;
    }

    @DeleteMapping(value = "/" + MEASUREMENTS + "/{sensor_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deleteMeasurement(
            @PathVariable(value = "sensor_id") String sensorId,
            @RequestBody String token) {
        ResponseEntity<String> authenticationResponse = authenticate(token);

        if (authenticationResponse.getStatusCode() == HttpStatus.OK) {
            JSONObject obj = new JSONObject(authenticationResponse.getBody());
            String user = obj.getString("login");

            if (userToMeasurement.get(user).equals(sensorId)) {
                return queryDeleteAllMonitors();
            }
            else
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        else
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<String> authenticate(String token) {
        String authenticationUri = authenticationService + "/users/auth?authToken=" + token;
        RestTemplate query = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
        HttpEntity<String> entity = new HttpEntity<>("", httpHeaders);

        System.out.println("DBG: authentication querying get on " + authenticationUri);
        ResponseEntity<String> response;
        try {
            response = query.exchange(
                    authenticationUri,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
        } catch (HttpClientErrorException ex) {
            System.out.println("ERR: authentication query on " + authenticationUri + " returned " + ex.getMessage());
            response =  new ResponseEntity<>(ex.getStatusCode());
        }

        return response;
    }

    private ResponseEntity<String> queryGetAllMonitors(String uri) {
        Gson gson = new Gson();
        RestTemplate request = new RestTemplate();
        List<Measurement> measurements = new ArrayList<>();

        for (String monitor : monitors) {
            System.out.println("DBG: querying get on " + monitor + uri);

            ResponseEntity<String> response;
            try {
                response = request.getForEntity(monitor + uri, String.class);
            } catch (HttpClientErrorException ex) {
                System.out.println("ERR: query on " + monitor + uri + " returned " + ex.getMessage());
                response = new ResponseEntity<>(ex.getStatusCode());
            }

            measurements.addAll(Arrays.asList(gson.fromJson(response.getBody(), Measurement[].class)));

            System.out.println("DBG: response body [" + response.getBody() + "]");
            System.out.println("DBG: response status [" + response.getStatusCode() + "]");
        }


        return new ResponseEntity<>(gson.toJson(measurements), HttpStatus.OK);
    }

    private ResponseEntity<String> queryGetAllSensorMeasurements(String uri) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        RestTemplate request = new RestTemplate();
        List<SensorMeasurements> measurement = new ArrayList<>();

        for (String monitor : monitors) {
            System.out.println("DBG: querying get on " + monitor + uri);

            ResponseEntity<String> response;
            try {
                response = request.getForEntity(monitor + uri, String.class);
            } catch (HttpClientErrorException ex) {
                System.out.println("ERR: query on " + monitor + uri + " returned " + ex.getMessage());
                response = new ResponseEntity<>(ex.getStatusCode());
            }

            measurement.add(gson.fromJson(response.getBody(), SensorMeasurements.class));

            System.out.println("DBG: response body [" + response.getBody() + "]");
            System.out.println("DBG: response status [" + response.getStatusCode() + "]");
        }

        measurement.removeIf(Objects::isNull);
        return new ResponseEntity<>(gson.toJson(measurement.size() != 0 ? measurement.get(0) : new SensorMeasurements()), HttpStatus.OK);
    }

    private String queryPostAllMonitors(String requestBody, String user) {
        String result = "";
        RestTemplate query = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
        HttpEntity<String> entity = new HttpEntity<>(requestBody, httpHeaders);

        for (String monitor : monitors) {
            System.out.println("DBG: querying post on " + monitor + MEASUREMENTS);

            ResponseEntity<String> response = query.exchange(
                    monitor + MEASUREMENTS,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            result += response.getBody();

            if (response.getStatusCode() == HttpStatus.CREATED) {
                JSONObject obj = new JSONObject(requestBody);
                String sensorId = obj.getString("sensor_id");
                userToMeasurement.put(user, sensorId);
            }

            System.out.println("DBG: response body [" + response.getBody() + "]");
            System.out.println("DBG: response status [" + response.getStatusCode() + "]");
        }

        return result;
    }

    private ResponseEntity<String> queryDeleteAllMonitors() {
        String result = "";
        RestTemplate query = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<>("", new HttpHeaders());

        for (String monitor : monitors) {
            System.out.println("DBG: querying delete on " + monitor + MEASUREMENTS);

            ResponseEntity<String> response = query.exchange(
                    monitor + MEASUREMENTS,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );
            //query.delete(monitor + MEASUREMENTS);
            result += response.getBody();

            System.out.println("DBG: response body [" + response.getBody() + "]");
            System.out.println("DBG: response status [" + response.getStatusCode() + "]");
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
