package org.shda;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.TextAlignment;
import java.io.File;
import java.util.List;

public class PdfReportService {
    
    // Official Sanatani Saffron Color for the PDF
    private static final DeviceRgb SAFFRON = new DeviceRgb(230, 81, 0);

    // 1. GLOBAL FINANCIAL STATEMENT (For the Dashboard)
    public static void generateFinancialReport(Context context, List<String> dates, List<String> names, List<Float> amounts, List<String> notes, float totalDonations) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, "Sanatani_Financial_Statement.pdf");
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            Document document = new Document(new PdfDocument(writer));

            // Header
            document.add(new Paragraph("SANATANI BANDHAN").setFontColor(SAFFRON).setBold().setFontSize(24).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Official Monthly Financial Statement").setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("--------------------------------------------------\n").setTextAlignment(TextAlignment.CENTER));

            // Summary
            document.add(new Paragraph("Total Chanda Collected: ৳" + totalDonations).setBold().setFontSize(14));
            document.add(new Paragraph("\n"));

            if (names.isEmpty()) {
                document.add(new Paragraph("No transactions recorded yet."));
            } else {
                for(int i = 0; i < names.size(); i++) {
                    document.add(new Paragraph("Date: " + dates.get(i) + "  |  Amount: ৳" + amounts.get(i)).setBold());
                    document.add(new Paragraph("Donor: " + names.get(i)));
                    document.add(new Paragraph("Note: " + notes.get(i)));
                    document.add(new Paragraph("----------------------------------------"));
                }
            }
            document.close();
            shareFile(context, file, "Monthly Financial Report");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show();
        }
    }

    // 2. INDIVIDUAL DONOR RECEIPT (New Feature for the POS System)
    public static void generateDonorReceipt(Context context, String name, float amount, String note, String date) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, "Chanda_Receipt_" + System.currentTimeMillis() + ".pdf");
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            Document document = new Document(new PdfDocument(writer));

            // Header
            document.add(new Paragraph("SANATANI BANDHAN").setFontColor(SAFFRON).setBold().setFontSize(26).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Official Chanda Receipt").setBold().setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("--------------------------------------------------\n").setTextAlignment(TextAlignment.CENTER));

            // The Shloka on Daan (Charity) from Bhagavad Gita 17.20
            document.add(new Paragraph("“Dātavyam iti yad dānaṁ dīyate 'nupakāriṇe...”").setItalic().setFontColor(SAFFRON).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Charity given to a worthy person simply because it is right to give, without consideration of anything in return, is stated to be in the mode of goodness. (Bhagavad Gita 17.20)\n").setItalic().setFontSize(10).setTextAlignment(TextAlignment.CENTER));

            // Receipt Details
            document.add(new Paragraph("\nDate: " + date).setFontSize(12));
            document.add(new Paragraph("Received With Gratitude From:").setBold().setFontSize(12));
            document.add(new Paragraph(name).setFontSize(16).setFontColor(SAFFRON));
            
            document.add(new Paragraph("\nContribution Amount: ৳" + amount).setBold().setFontSize(14));
            document.add(new Paragraph("Purpose/Note: " + note).setFontSize(12));

            // Thank You Message
            document.add(new Paragraph("\n--------------------------------------------------").setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Dhanyabad! Thank you for your generous contribution to the community. Your support strengthens our Sanatan Bandhan. May Bhagavan bless you and your family with peace and prosperity.").setTextAlignment(TextAlignment.CENTER).setBold());

            document.close();
            shareFile(context, file, "Your Sanatani Bandhan Receipt");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to create Receipt", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to share the PDF
    private static void shareFile(Context context, File file, String subject) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF via..."));
    }
}
