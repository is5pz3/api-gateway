package app.controller;

import app.model.MeasurementRequest;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RestController
public class GatewayController {
    static private final List<String> monitors = Arrays.asList(
            "https://monitor-prodd.herokuapp.com/",
            "https://monitor2-prodd.herokuapp.com/"
    );
    static private final String authenticationService = ""; // TODO: fill correct address

    static private final String HOSTS = "hosts";
    static private final String MEASUREMENTS = "measurements";

    private Map<String, String> userToMeasurement = new TreeMap<>();

    @GetMapping("/" + HOSTS)
    public ResponseEntity<String> getHosts(@RequestParam("hostName") String hostName) {
        return queryGetAllMonitors(HOSTS + "?hostName=" + hostName);
    }

    @GetMapping("/" + MEASUREMENTS)
    public ResponseEntity<String> getAllMeasurements() {
        return queryGetAllMonitors(MEASUREMENTS);
    }

    @GetMapping("/" + MEASUREMENTS + "/{sensor_id}")
    public ResponseEntity<String> getSensorMeasurements(
            @PathVariable(value = "sensor_id") String sensorId,
            @RequestParam(value = "data_count", defaultValue = "10") String dataCount,
            @RequestParam(value = "since", required = false) String since,
            @RequestParam(value = "to", required = false) String to) {
        return queryGetAllMonitors(
                MEASUREMENTS + "/" + sensorId + "?data_count=" + dataCount + "&since=" + since + "&to=" + to
        );
    }

    @PostMapping("/" + MEASUREMENTS)
    public ResponseEntity<String> createMeasurement(@RequestBody MeasurementRequest request) {
        ResponseEntity<String> authenticationResponse = authenticate(request.getToken());
        JSONObject obj = new JSONObject(authenticationResponse.getBody());
        String user = obj.getString("login");

        ResponseEntity<String> response;
        if (authenticationResponse.getStatusCode() == HttpStatus.OK) {
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

    @DeleteMapping("/" + MEASUREMENTS + "/{sensor_id}")
    public ResponseEntity<String> deleteMeasurement(
            @PathVariable(value = "sensor_id") String sensorId,
            @RequestBody String token) {
        ResponseEntity<String> authenticationResponse = authenticate(token);
        JSONObject obj = new JSONObject(authenticationResponse.getBody());
        String user = obj.getString("login");

        if (userToMeasurement.get(user).equals(sensorId)) {
            return queryDeleteAllMonitors();
        }
        else
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<String> authenticate(String token) {
        String authenticationUri = authenticationService + "/users/auth?authToken=" + token;
        RestTemplate query = new RestTemplate();

        return query.getForEntity(authenticationUri, String.class);
    }

    private ResponseEntity<String> queryGetAllMonitors(String uri) {
        String result = "";
        RestTemplate request = new RestTemplate();

        for (String monitor : monitors) {
            System.out.println("DBG: querying " + monitor + uri);

            ResponseEntity<String> response = request.getForEntity(monitor + uri, String.class);
            result += response.getBody();

            System.out.println("DBG: response body [" + response.getBody() + "]");
            System.out.println("DBG: response status [" + response.getStatusCode() + "]");
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
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
