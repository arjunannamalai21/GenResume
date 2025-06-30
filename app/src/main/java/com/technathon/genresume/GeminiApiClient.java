package com.technathon.genresume;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiApiClient {

    private static final String TAG = "GeminiApiClient";
    // *** IMPORTANT CHANGE HERE ***
    // Use the direct model-specific generateContent endpoint
    // Let's stick with gemini-1.5-flash-latest for now as it's efficient
    private static final String API_URL_BASE = "https://generativelanguage.googleapis.com/v1/models/";
    private static final String GENERATE_CONTENT_ENDPOINT = ":generateContent?key=";
    private static final String MODEL_NAME = "gemini-1.5-flash"; // Use the base model name
    // Alternatively, try "gemini-1.5-pro" if you want a more powerful model.

    private final String apiKey;
    private final OkHttpClient client;
    private final Gson gson;

    public GeminiApiClient(String apiKey) {
        this.apiKey = apiKey;
        this.gson = new Gson();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public interface GeminiApiResponseCallback {
        void onSuccess(String resume, String atsScore);
        void onFailure(String error);
    }

    private String buildResumePrompt(String name, String email, String phone, String linkedIn, String summary,
                                     String education, String experience, String skills, String projects, String certifications) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional resume in plain text format based on the following details. " +
                "Ensure it's well-formatted for readability. After the resume, provide an estimated ATS (Applicant Tracking System) " +
                "pass percentage for this resume, and clearly label it 'ATS Pass Percentage: XX%'. " +
                "Explain briefly why you gave that percentage, focusing on common ATS factors (keywords, formatting, structure).");
        prompt.append("\n\n---\n\n");
        prompt.append("Personal Details:\n");
        prompt.append("Name: ").append(name).append("\n");
        prompt.append("Email: ").append(email).append("\n");
        prompt.append("Phone: ").append(phone).append("\n");
        if (!linkedIn.isEmpty()) {
            prompt.append("LinkedIn: ").append(linkedIn).append("\n");
        }

        prompt.append("\nSummary/Objective:\n").append(summary).append("\n");
        prompt.append("\nEducation:\n").append(education).append("\n");
        prompt.append("\nWork Experience:\n").append(experience).append("\n");
        prompt.append("\nSkills:\n").append(skills).append("\n");

        if (!projects.isEmpty()) {
            prompt.append("\nProjects:\n").append(projects).append("\n");
        }
        if (!certifications.isEmpty()) {
            prompt.append("\nCertifications:\n").append(certifications).append("\n");
        }
        prompt.append("\n---\n");
        prompt.append("\nResume Format should be in plain text. Conclude with 'ATS Pass Percentage: [XX%] Reason: [Brief explanation]'\n");
        return prompt.toString();
    }

    public void generateResumeAndAts(String name, String email, String phone, String linkedIn, String summary,
                                     String education, String experience, String skills, String projects, String certifications,
                                     GeminiApiResponseCallback callback) {

        String prompt = buildResumePrompt(name, email, phone, linkedIn, summary, education, experience, skills, projects, certifications);

        new Thread(() -> {
            try {
                JsonObject root = new JsonObject();
                // No need to add "model" in the body if it's in the URL path.
                // This was the issue from previous attempt. Revert this.
                // root.addProperty("model", MODEL_NAME); // REMOVE THIS LINE

                JsonArray contents = new JsonArray();
                JsonObject content = new JsonObject();
                JsonArray parts = new JsonArray();
                JsonObject part = new JsonObject();

                part.addProperty("text", prompt);
                parts.add(part);
                content.add("parts", parts);
                contents.add(content);
                root.add("contents", contents);

                MediaType JSON = MediaType.get("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(root.toString(), JSON);

                // Construct the full URL using the new parts
                String fullUrl = API_URL_BASE + MODEL_NAME + GENERATE_CONTENT_ENDPOINT + apiKey;

                Request request = new Request.Builder()
                        .url(fullUrl) // Use the constructed fullUrl
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "API Call Failed: " + response.code() + " - " + errorBody);
                        callback.onFailure("API Call failed: " + response.code() + "\n" + errorBody);
                        return;
                    }

                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Gemini Raw Response: " + responseBody);

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray candidates = jsonResponse.getJSONArray("candidates");
                    if (candidates.length() > 0) {
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject contentPart = firstCandidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0);
                        String generatedText = contentPart.getString("text");

                        String resumeContent;
                        String atsPercentage = "N/A";

                        int atsIndex = generatedText.indexOf("ATS Pass Percentage:");
                        if (atsIndex != -1) {
                            String atsSection = generatedText.substring(atsIndex);
                            int nextNewlineIndex = atsSection.indexOf("\n", "ATS Pass Percentage:".length());
                            if (nextNewlineIndex != -1) {
                                atsPercentage = atsSection.substring(0, nextNewlineIndex).trim();
                            } else {
                                atsPercentage = atsSection.trim();
                            }
                            resumeContent = generatedText.substring(0, atsIndex).trim();
                        } else {
                            resumeContent = generatedText.trim();
                        }

                        callback.onSuccess(resumeContent, atsPercentage);

                    } else {
                        callback.onFailure("No candidates found in Gemini response.");
                    }

                }
            } catch (IOException e) {
                Log.e(TAG, "Network Error: " + e.getMessage(), e);
                callback.onFailure("Network Error: " + e.getMessage());
            } catch (JSONException e) {
                Log.e(TAG, "JSON Parsing Error: " + e.getMessage(), e);
                callback.onFailure("Failed to parse API response: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "An unexpected error occurred: " + e.getMessage(), e);
                callback.onFailure("An unexpected error occurred: " + e.getMessage());
            }
        }).start();
    }
}