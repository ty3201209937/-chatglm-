package com.ttpfx.model;

import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Chatglm {
    public String getAnswer(String question) {
        String response = "";
        try {
            // URL to send the POST request to
            URL url = new URL("http://192.168.0.127:8000");

            // Open connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method to POST
            connection.setRequestMethod("POST");

            // Set headers
            connection.setRequestProperty("Content-Type", "application/json");

            // Enable input and output streams
            connection.setDoOutput(true);

            // JSON payload
            String jsonInputString = "{\"prompt\": \"" + question + "\", \"history\": []}";

            // Write the JSON data to the request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Get the response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Read the response
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder responseBuilder = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    responseBuilder.append(responseLine.trim());
                }
                response = responseBuilder.toString();
                System.out.println("Response: " + response);
                JSONObject jsonObject = new JSONObject(response);
                response = jsonObject.getString("response");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public static void main(String[] args) {
        Chatglm example = new Chatglm();
        String question = "你好";
        String answer = example.getAnswer(question);
        System.out.println("Answer: " + answer);
    }
}
