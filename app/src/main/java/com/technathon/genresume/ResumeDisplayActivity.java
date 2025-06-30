package com.technathon.genresume;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResumeDisplayActivity extends AppCompatActivity {

    private TextView tvResumeContent, tvAtsScore;
    private Button btnRegenerate, btnHome, btnSavePdf;
    private ProgressBar progressBar;

    // Store received resume data
    private String name, email, phone, linkedIn, summary, education,
            experience, skills, projects, certifications;

    private GeminiApiClient geminiApiClient;
    private Handler uiHandler;

    // WARNING: HARDCODING API KEYS IS NOT RECOMMENDED FOR PRODUCTION APPS.
    // Replace "YOUR_ACTUAL_GEMINI_API_KEY_HERE" with your real Gemini API key.
    private static final String GEMINI_API_KEY = "AIzaSyC_aKJBZIu9MLUKght5TEtwWByWVkrYdAg"; // <<< IMPORTANT: REPLACE WITH YOUR KEY

    private static final int CREATE_PDF_REQUEST_CODE = 42; // Unique request code for PDF intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume_display);

        // Initialize UI elements
        tvResumeContent = findViewById(R.id.tvResumeContent);
        tvAtsScore = findViewById(R.id.tvAtsScore);
        btnRegenerate = findViewById(R.id.btnRegenerate);
        btnHome = findViewById(R.id.btnHome);
        btnSavePdf = findViewById(R.id.btnSavePdf); // Initialize Save PDF button
        progressBar = findViewById(R.id.progressBar);

        uiHandler = new Handler(Looper.getMainLooper());

        // Initialize GeminiApiClient with your hardcoded API key
        geminiApiClient = new GeminiApiClient(GEMINI_API_KEY);

        // Get data from the Intent
        Intent intent = getIntent();
        if (intent != null) {
            name = intent.getStringExtra("name");
            email = intent.getStringExtra("email");
            phone = intent.getStringExtra("phone");
            linkedIn = intent.getStringExtra("linkedIn");
            summary = intent.getStringExtra("summary");
            education = intent.getStringExtra("education");
            experience = intent.getStringExtra("experience");
            skills = intent.getStringExtra("skills");
            projects = intent.getStringExtra("projects");
            certifications = intent.getStringExtra("certifications");

            // Trigger resume generation on activity creation
            generateResumeAndAts();

        } else {
            tvResumeContent.setText("Error: No resume data received.");
            tvAtsScore.setText("ATS Pass Percentage: N/A");
            progressBar.setVisibility(View.GONE);
        }

        // Set OnClickListener for Regenerate Button
        btnRegenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (name != null && !name.isEmpty()) {
                    generateResumeAndAts();
                } else {
                    Toast.makeText(ResumeDisplayActivity.this, "Resume data not available for regeneration.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set OnClickListener for Home Button
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(ResumeDisplayActivity.this, MainActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(homeIntent);
                finish();
            }
        });

        // Set OnClickListener for Save PDF Button
        btnSavePdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveResumeAsPdf();
            }
        });
    }

    private void generateResumeAndAts() {
        progressBar.setVisibility(View.VISIBLE);
        tvResumeContent.setText("Generating resume, please wait...");
        tvAtsScore.setText("ATS Pass Percentage: Calculating...");
        btnRegenerate.setEnabled(false);
        btnSavePdf.setEnabled(false); // Disable save PDF button during API call

        geminiApiClient.generateResumeAndAts(
                name, email, phone, linkedIn, summary,
                education, experience, skills, projects, certifications,
                new GeminiApiClient.GeminiApiResponseCallback() {
                    @Override
                    public void onSuccess(final String resume, final String atsScore) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvResumeContent.setText(resume);
                                tvAtsScore.setText(atsScore);
                                progressBar.setVisibility(View.GONE);
                                btnRegenerate.setEnabled(true);
                                btnSavePdf.setEnabled(true); // Re-enable save PDF button
                            }
                        });
                    }

                    @Override
                    public void onFailure(final String error) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvResumeContent.setText("Error generating resume: " + error);
                                tvAtsScore.setText("ATS Pass Percentage: Error");
                                Toast.makeText(ResumeDisplayActivity.this, "API Error: " + error, Toast.LENGTH_LONG).show();
                                progressBar.setVisibility(View.GONE);
                                btnRegenerate.setEnabled(true);
                                btnSavePdf.setEnabled(false); // Keep disabled on failure
                            }
                        });
                    }
                }
        );
    }

    private void saveResumeAsPdf() {
        String resumeText = tvResumeContent.getText().toString();
        if (resumeText.isEmpty() || resumeText.equals("Generating resume, please wait...") || resumeText.startsWith("Error")) {
            Toast.makeText(this, "No resume to save yet!", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String filename = "Resume_" + timestamp + ".pdf";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, filename);

        startActivityForResult(intent, CREATE_PDF_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_PDF_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                createPdf(uri);
            } else {
                Toast.makeText(this, "File creation cancelled or failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Helper class to manage page drawing state
    private static class PageDrawResult {
        PdfDocument.Page page;
        Canvas canvas;
        int currentY;
        int pageNumber;

        PageDrawResult(PdfDocument.Page page, Canvas canvas, int currentY, int pageNumber) {
            this.page = page;
            this.canvas = canvas;
            this.currentY = currentY;
            this.pageNumber = pageNumber;
        }
    }

    // Helper method to draw text and manage page breaks
    private PageDrawResult drawTextWithPagination(PdfDocument document, PdfDocument.Page currentPage, Canvas currentCanvas,
                                                  TextPaint paint, String text, int startY, int margin, int usableWidth,
                                                  int pageHeight, float lineSpacingMultiplier, int currentPageNumber) {
        if (text == null || text.trim().isEmpty()) {
            return new PageDrawResult(currentPage, currentCanvas, startY, currentPageNumber);
        }

        StaticLayout staticLayout;
        // Use StaticLayout.Builder for API 23+, otherwise use older constructor
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { // M is API 23
            staticLayout = StaticLayout.Builder.obtain(text, 0, text.length(), paint, usableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, lineSpacingMultiplier)
                    .build();
        } else {
            // Deprecated constructor for API < 23
            staticLayout = new StaticLayout(
                    text,
                    paint,
                    usableWidth,
                    Layout.Alignment.ALIGN_NORMAL,
                    lineSpacingMultiplier,
                    0f,
                    false
            );
        }

        int textHeight = staticLayout.getHeight();
        int currentY = startY;

        // Check if content fits on current page, leaving space for next section.
        // Add a buffer (e.g., paint.getTextSize() * 2) to avoid text being too close to the bottom.
        if (currentY + textHeight + (int)(paint.getTextSize() * 2) > pageHeight) {
            document.finishPage(currentPage);
            currentPageNumber++;
            PdfDocument.PageInfo newPageInfo = new PdfDocument.PageInfo.Builder(
                    pageInfo.getPageWidth(), pageInfo.getPageHeight(), currentPageNumber).create();
            currentPage = document.startPage(newPageInfo);
            currentCanvas = currentPage.getCanvas();
            currentY = margin; // Reset Y for new page
        }

        currentCanvas.save();
        currentCanvas.translate(margin, currentY);
        staticLayout.draw(currentCanvas);
        currentCanvas.restore();

        currentY += textHeight; // Update Y after drawing the text block
        return new PageDrawResult(currentPage, currentCanvas, currentY, currentPageNumber);
    }

    // PDF page info (can be instance variable or passed around)
    private PdfDocument.PageInfo pageInfo;


    private void createPdf(Uri uri) {
        PdfDocument document = new PdfDocument();
        // A4 size in points (1 point = 1/72 inch)
        // 595 width x 842 height for portrait A4
        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        int currentPageNumber = 1;

        // --- Define TextPaint objects for different styles ---
        TextPaint namePaint = new TextPaint();
        namePaint.setColor(android.graphics.Color.BLACK);
        namePaint.setTextSize(28f); // Larger for Name
        namePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        namePaint.setTextAlign(Paint.Align.CENTER); // Center align name

        TextPaint contactPaint = new TextPaint();
        contactPaint.setColor(android.graphics.Color.DKGRAY);
        contactPaint.setTextSize(14f); // For contact details
        contactPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        contactPaint.setTextAlign(Paint.Align.CENTER); // Center align contact info

        TextPaint bodyPaint = new TextPaint();
        bodyPaint.setColor(android.graphics.Color.BLACK);
        bodyPaint.setTextSize(12f); // Standard body text
        bodyPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)); // Regular font for body

        TextPaint atsPaint = new TextPaint();
        atsPaint.setColor(android.graphics.Color.BLUE); // Distinct color for ATS score
        atsPaint.setTextSize(14f);
        atsPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));

        // --- Page dimensions and margins ---
        int margin = 40; // Margins from the page edges
        int pageHeight = pageInfo.getPageHeight();
        int usableWidth = pageInfo.getPageWidth() - (2 * margin);
        float lineSpacingMultiplier = 1.2f; // Default line spacing for body text
        int currentY = margin; // Starting Y position for drawing

        // Page drawing result helper
        PageDrawResult result;

        // --- 1. Draw Name (Centered) ---
        // For centered text, we need to draw it at pageInfo.getPageWidth() / 2
        canvas.drawText(name, pageInfo.getPageWidth() / 2, currentY + namePaint.getTextSize(), namePaint);
        currentY += namePaint.getTextSize() + 10; // Space after name

        // --- 2. Draw Contact Details (Centered) ---
        StringBuilder contactDetails = new StringBuilder();
        contactDetails.append(email);
        if (phone != null && !phone.isEmpty()) {
            contactDetails.append(" | ").append(phone);
        }
        if (linkedIn != null && !linkedIn.isEmpty()) {
            contactDetails.append(" | ").append(linkedIn);
        }
        canvas.drawText(contactDetails.toString(), pageInfo.getPageWidth() / 2, currentY + contactPaint.getTextSize(), contactPaint);
        currentY += contactPaint.getTextSize() + 30; // Space after contact details


        // --- Horizontal line separator ---
        // Ensure the line fits on the current page before drawing
        if (currentY + 20 > pageHeight) { // Check if separator fits
            document.finishPage(page);
            currentPageNumber++;
            pageInfo = new PdfDocument.PageInfo.Builder(pageInfo.getPageWidth(), pageInfo.getPageHeight(), currentPageNumber).create();
            page = document.startPage(pageInfo);
            canvas = page.getCanvas();
            currentY = margin; // Reset Y for new page
        } else {
            currentY += 10; // Small space before the separator
        }
        // Draw a line using the bodyPaint color (or define a new linePaint if desired)
        canvas.drawLine(margin, currentY, pageInfo.getPageWidth() - margin, currentY, bodyPaint);
        currentY += 20; // Space after line


        // --- 3. Draw AI Generated Resume Content ---
        String generatedResumeContent = tvResumeContent.getText().toString(); // Get AI generated resume
        result = drawTextWithPagination(document, page, canvas, bodyPaint, generatedResumeContent,
                currentY, margin, usableWidth, pageHeight, lineSpacingMultiplier, currentPageNumber);
        page = result.page; canvas = result.canvas; currentY = result.currentY; currentPageNumber = result.pageNumber;
        currentY += 20;


        // --- 4. Draw ATS Score ---
        String atsScoreContent = tvAtsScore.getText().toString(); // Get ATS score
        result = drawTextWithPagination(document, page, canvas, atsPaint, atsScoreContent,
                currentY, margin, usableWidth, pageHeight, 1.0f, currentPageNumber);
        page = result.page; canvas = result.canvas; currentY = result.currentY; currentPageNumber = result.pageNumber;


        // Always finish the last page
        document.finishPage(page);

        FileOutputStream fos = null;
        ParcelFileDescriptor pfd = null;
        try {
            pfd = getContentResolver().openFileDescriptor(uri, "w");
            if (pfd != null) {
                fos = new FileOutputStream(pfd.getFileDescriptor());
                document.writeTo(fos);
                Toast.makeText(this, "Resume saved as PDF successfully!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to get file descriptor.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (document != null) {
                    document.close();
                }
                if (fos != null) {
                    fos.close();
                }
                if (pfd != null) {
                    pfd.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}