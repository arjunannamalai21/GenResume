package com.technathon.genresume; // Make sure your package name matches

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent; // Import for Intent
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast; // Import for Toast messages

public class MainActivity extends AppCompatActivity {

    // Declare EditText variables
    private EditText etName, etEmail, etPhone, etLinkedIn, etSummary,
            etEducation, etExperience, id_etSkills, etProjects, etCertifications;
    private Button btnGenerateResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize EditText fields
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etLinkedIn = findViewById(R.id.etLinkedIn);
        etSummary = findViewById(R.id.etSummary);
        etEducation = findViewById(R.id.etEducation);
        etExperience = findViewById(R.id.etExperience);
        id_etSkills = findViewById(R.id.id_etSkills); // Corrected ID from XML
        etProjects = findViewById(R.id.etProjects);
        etCertifications = findViewById(R.id.etCertifications);

        // Initialize the Button
        btnGenerateResume = findViewById(R.id.btnGenerateResume);

        // Set an OnClickListener for the button
        btnGenerateResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collectAndSendResumeData();
            }
        });
    }

    private void collectAndSendResumeData() {
        // Get text from all EditText fields
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String linkedIn = etLinkedIn.getText().toString().trim();
        String summary = etSummary.getText().toString().trim();
        String education = etEducation.getText().toString().trim();
        String experience = etExperience.getText().toString().trim();
        String skills = id_etSkills.getText().toString().trim(); // Use the corrected ID
        String projects = etProjects.getText().toString().trim();
        String certifications = etCertifications.getText().toString().trim();

        // Basic validation: Ensure required fields are not empty
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || summary.isEmpty() ||
                education.isEmpty() || experience.isEmpty() || skills.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please fill in all required fields.", Toast.LENGTH_LONG).show();
            return; // Stop execution if validation fails
        }

        // Create an Intent to start ResumeDisplayActivity
        Intent intent = new Intent(MainActivity.this, ResumeDisplayActivity.class);

        // Put all the collected data into the Intent as extras
        intent.putExtra("name", name);
        intent.putExtra("email", email);
        intent.putExtra("phone", phone);
        intent.putExtra("linkedIn", linkedIn);
        intent.putExtra("summary", summary);
        intent.putExtra("education", education);
        intent.putExtra("experience", experience);
        intent.putExtra("skills", skills);
        intent.putExtra("projects", projects);
        intent.putExtra("certifications", certifications);

        // Start the new Activity
        startActivity(intent);
    }
}