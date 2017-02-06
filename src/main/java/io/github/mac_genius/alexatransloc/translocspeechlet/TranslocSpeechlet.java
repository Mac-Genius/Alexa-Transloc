package io.github.mac_genius.alexatransloc.translocspeechlet;

import com.amazon.speech.speechlet.*;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/**
 * Created by Mac Watson on 2/5/2017.
 */
public class TranslocSpeechlet implements Speechlet {

    private static List<String> currentlyArriving;

    private String agency = "28";

    private String route = "8001780";

    private String stop = "8047872";

    static {
        currentlyArriving = new ArrayList<>();
        currentlyArriving.add("Your bus is currently loading. Better move quick!");
        currentlyArriving.add("Your bus is at the stop right now! Go! Go! Go!");
        currentlyArriving.add("Awe snap, it's here!");
    }

    public void onSessionStarted(SessionStartedRequest sessionStartedRequest, Session session) throws SpeechletException {

    }

    public SpeechletResponse onLaunch(LaunchRequest launchRequest, Session session) throws SpeechletException {
        SpeechletResponse response = new SpeechletResponse();
        String message = "";
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(message);
        response.setOutputSpeech(speech);
        return response;
    }

    public SpeechletResponse onIntent(IntentRequest intentRequest, Session session) throws SpeechletException {
        SpeechletResponse response = new SpeechletResponse();
        if (intentRequest.getIntent().getName().equals("GetBusArrival")) {
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
            int timeLeft = getTimeLeft();
            System.out.println(timeLeft + "");
            if (timeLeft == 0) {
                speech.setText(selectMessage(currentlyArriving));
            } else if (timeLeft < 0) {
                speech.setText("There are no buses running right now.");
            } else {
                int hours = timeLeft / 60;
                int minutes = timeLeft % 60;
                if (hours <= 0) {
                    speech.setText(String.format("The next bus will be here in %d minute%s.", minutes, minutes == 1 ? "" : "s"));
                } else if (hours == 1) {
                    speech.setText(String.format("The next bus is in 1 hour and %d minute%s.", minutes, minutes == 1 ? "" : "s"));
                } else {
                    speech.setText(String.format("The next bus is in %d hours and %d minute%s.", hours, minutes, minutes == 1 ? "" : "s"));
                }
            }
            response.setOutputSpeech(speech);
        } else {
            String message = "I'm not sure what you said";
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
            speech.setText(message);
            response.setOutputSpeech(speech);
        }
        return response;
    }

    public void onSessionEnded(SessionEndedRequest sessionEndedRequest, Session session) throws SpeechletException {

    }

    private int getTimeLeft() {
        HttpClient client = HttpClientBuilder.create().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
        HttpGet get = new HttpGet(String.format("https://transloc-api-1-2.p.mashape.com/arrival-estimates.json?agencies=%s&routes=%s&stops=%s", agency, route, stop));
        get.addHeader("X-Mashape-Key", System.getenv("TRANSLOC_KEY"));
        try {
            HttpResponse response = client.execute(get);
            BasicResponseHandler responseHandler = new BasicResponseHandler();
            JsonObject data = new JsonParser().parse(responseHandler.handleResponse(response)).getAsJsonObject();
            if (data.get("data").getAsJsonArray().size() <= 0) {
                return -1;
            } else {
                JsonArray jsonArray = data.get("data").getAsJsonArray();
                String initTime = data.get("generated_on").getAsString();
                String latestTime = "";
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject arrival = jsonArray.get(i).getAsJsonObject();
                    if (arrival.get("stop_id").getAsString().equals("8047872")) {
                        JsonArray arrivalTimes = arrival.get("arrivals").getAsJsonArray();
                        if (arrivalTimes.size() > 0) {
                            latestTime = arrivalTimes.get(0).getAsJsonObject().get("arrival_at").getAsString();
                        }
                    }
                }
                if (latestTime.equals("")) {
                    return -1;
                }
                return getTimeOffset(initTime, latestTime);
            }
        } catch (Exception e) {
            System.out.println((e.getMessage()));
            return -1;
        }
    }

    private int getTimeOffset(String initTime, String otherTime) {
        LocalDateTime init = LocalDateTime.parse(getNormalTime(initTime));
        init.withHourOfDay((init.getHourOfDay() + getTimeOffset(initTime)) % 24);
        System.out.println(init.toString());
        LocalDateTime first = LocalDateTime.parse(getNormalTime(otherTime));
        int timeOffset = getTimeOffset(otherTime);
        if (timeOffset < 0) {
            first = first.minusHours(-1 * getTimeOffset(otherTime));
        } else {
            first = first.plusHours(getTimeOffset(otherTime));
        }
        System.out.println(first.toString());
        int totalTime = 0;
        totalTime += (first.getHourOfDay() - init.getHourOfDay()) * 60;
        totalTime += first.getMinuteOfHour() - init.getMinuteOfHour();
        return totalTime;
    }

    private String getNormalTime(String time) {
        return time.substring(0, 19);
    }

    private int getTimeOffset(String time) {
        String offset = time.substring(19);
        System.out.println(offset);
        if (offset.charAt(0) == '-') {
            return Integer.parseInt(offset.substring(1, 3));
        } else {
            return -1 * Integer.parseInt(offset.substring(1, 3));
        }
    }

    private String selectMessage(List<String> messages) {
        int number = new Random().nextInt(messages.size());
        return messages.get(number);
    }
}
