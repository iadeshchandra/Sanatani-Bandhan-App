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
import com.itextpdf.layout.properties.TextAlignment; 
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfReportService {
    
    private static final DeviceRgb SAFFRON = new DeviceRgb(230, 81, 0);

    // ✨ NEW: Enterprise Standard Naming Convention (CommunityName_ReportType_Date_Time.pdf)
    private static String getFormattedFileName(String communityName, String reportType) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault());
        String safeCommunity = communityName.replaceAll("[^a-zA-Z0-9]", "");
        String safeReport = reportType.replaceAll("[^a-zA-Z0-9]", "_");
        return safeCommunity + "_" + safeReport + "_" + sdf.format(new Date()) + ".pdf";
    }

    private static void addSanataniHeader(Context context, Document document, String communityName, String reportType) {
        document.add(new Paragraph(communityName.toUpperCase()).setFontColor(SAFFRON).setBold().setFontSize(24).setTextAlignment(TextAlignment.CENTER));
        
        // ✨ NEW: Uses the PDF-Safe English/Sanskrit version so no blank boxes appear!
        document.add(new Paragraph(ShlokaEngine.getPdfSafeShloka(context)).setFontSize(11).setItalic().setFontColor(new DeviceRgb(117, 117, 117)).setTextAlignment(TextAlignment.CENTER));
        
        document.add(new Paragraph(reportType).setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER).setMarginTop(8f));
        document.add(new Paragraph("Generated: " + new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date())).setFontSize(10).setItalic().setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("--------------------------------------------------\n").setTextAlignment(TextAlignment.CENTER));
    }

    public static void generateFinancialReport(Context context, String communityName, List<String> dates, List<String> names, List<Float> amounts, List<String> notes, float totalDonations, String reportRange) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); 
            File file = new File(path, getFormattedFileName(communityName, "Income_Statement"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            
            addSanataniHeader(context, document, communityName, "Official Income Statement");
            document.add(new Paragraph(reportRange).setFontSize(12).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10f));
            document.add(new Paragraph("Total Chanda Collected: ৳" + totalDonations).setBold().setFontSize(14));
            document.add(new Paragraph("\n"));
            
            if (names.isEmpty()) { document.add(new Paragraph("No transactions recorded.")); } else {
                for(int i = 0; i < names.size(); i++) {
                    document.add(new Paragraph("Date: " + dates.get(i) + "  |  Amount: ৳" + amounts.get(i)).setBold());
                    document.add(new Paragraph("Donor: " + names.get(i))); document.add(new Paragraph("Note: " + notes.get(i))); document.add(new Paragraph("----------------------------------------"));
                }
            }
            document.close(); shareFile(context, file, communityName + " - Income Report");
        } catch (Exception e) {}
    }

    public static void generateExpenseReport(Context context, String communityName, List<Expense> expenseList, float totalExpense, String reportRange) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); 
            File file = new File(path, getFormattedFileName(communityName, "Utsav_Expenses"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            
            addSanataniHeader(context, document, communityName, "Official Utsav Expense Ledger");
            document.add(new Paragraph(reportRange).setFontSize(12).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10f));
            document.add(new Paragraph("Total Expenses: ৳" + totalExpense).setBold().setFontSize(14).setFontColor(new DeviceRgb(211, 47, 47))); document.add(new Paragraph("\n"));
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            for(Expense exp : expenseList) {
                document.add(new Paragraph("🪔 Utsav/Puja: " + exp.eventName).setBold().setFontSize(13).setFontColor(SAFFRON));
                document.add(new Paragraph("Date: " + sdf.format(new Date(exp.timestamp)) + "  |  Amount: ৳" + exp.amount).setBold());
                document.add(new Paragraph("Item/Seva: " + exp.itemName)); document.add(new Paragraph("Handled By: " + exp.involvedPerson));
                document.add(new Paragraph("----------------------------------------"));
            }
            document.close(); shareFile(context, file, communityName + " - Expense Ledger");
        } catch (Exception e) {}
    }

    public static void generateDonorReceipt(Context context, String communityName, String name, float amount, String note, String date) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); 
            File file = new File(path, getFormattedFileName(communityName, "Chanda_Receipt"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            
            addSanataniHeader(context, document, communityName, "Official Chanda Receipt");
            document.add(new Paragraph("\nTransaction Date: " + date).setFontSize(12)); document.add(new Paragraph("Received With Gratitude From:").setBold().setFontSize(12));
            document.add(new Paragraph(name).setFontSize(16).setFontColor(SAFFRON)); document.add(new Paragraph("\nContribution Amount: ৳" + amount).setBold().setFontSize(14));
            document.add(new Paragraph("Purpose/Note: " + note).setFontSize(12)); document.add(new Paragraph("\n\nMay Ishvara shower you with infinite blessings.").setItalic().setFontSize(11).setTextAlignment(TextAlignment.CENTER));
            
            document.close(); shareFile(context, file, communityName + " - Receipt");
        } catch (Exception e) {}
    }

    public static void generateDonorStatement(Context context, String communityName, TransactionActivity.GroupedDonation gd) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); 
            String safeName = gd.displayName.replace("[Member]", "").replace("[Guest]", "").trim();
            File file = new File(path, getFormattedFileName(communityName, safeName + "_Statement"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            
            addSanataniHeader(context, document, communityName, "Official Donor Statement");
            document.add(new Paragraph("Donor: " + gd.displayName).setBold().setFontSize(16).setFontColor(SAFFRON));
            document.add(new Paragraph("Lifetime Contribution: ৳" + gd.totalDonated).setBold().setFontSize(14).setFontColor(new DeviceRgb(46, 125, 50)));
            document.add(new Paragraph("Total Records: " + gd.history.size() + "\n").setFontSize(12).setMarginBottom(10f));
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            gd.history.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

            for(TransactionActivity.SingleDonation sd : gd.history) {
                document.add(new Paragraph("Date: " + sdf.format(new Date(sd.timestamp)) + "  |  Amount: ৳" + sd.amount).setBold().setFontSize(12));
                document.add(new Paragraph("Note/Purpose: " + sd.note).setFontSize(11));
                if (sd.collectedBy != null && !sd.collectedBy.isEmpty()) document.add(new Paragraph("Handled By: " + sd.collectedBy).setFontSize(10).setItalic());
                document.add(new Paragraph("----------------------------------------"));
            }
            document.close(); shareFile(context, file, gd.displayName + " Statement");
        } catch (Exception e) {}
    }

    public static void generateUtsavStatement(Context context, String communityName, ExpenseActivity.GroupedExpense ge) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); 
            File file = new File(path, getFormattedFileName(communityName, ge.eventDisplayName + "_Ledger"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            
            addSanataniHeader(context, document, communityName, "Official Utsav Expense Ledger");
            document.add(new Paragraph("Utsav/Event: " + ge.eventDisplayName).setBold().setFontSize(16).setFontColor(SAFFRON));
            document.add(new Paragraph("Total Cost: ৳" + ge.totalSpent).setBold().setFontSize(14).setFontColor(new DeviceRgb(211, 47, 47)));
            document.add(new Paragraph("Total Recorded Items: " + ge.history.size() + "\n").setFontSize(12).setMarginBottom(10f));
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            ge.history.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

            for(Expense exp : ge.history) {
                document.add(new Paragraph("Date: " + sdf.format(new Date(exp.timestamp)) + "  |  Amount: ৳" + exp.amount).setBold().setFontSize(12));
                document.add(new Paragraph("Item/Seva: " + exp.itemName).setFontSize(11));
                document.add(new Paragraph("Handled By: " + exp.involvedPerson).setFontSize(10).setItalic());
                document.add(new Paragraph("----------------------------------------"));
            }
            document.close(); shareFile(context, file, ge.eventDisplayName + " Ledger");
        } catch (Exception e) {}
    }

    public static void generateEventItinerary(Context context, String communityName, Event event) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); 
            File file = new File(path, getFormattedFileName(communityName, "Event_Itinerary"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            
            addSanataniHeader(context, document, communityName, "Official Event Itinerary");
            document.add(new Paragraph("Event: " + event.title).setBold().setFontSize(18).setFontColor(SAFFRON));
            document.add(new Paragraph("Date & Time: " + event.dateStr).setBold().setFontSize(14));
            document.add(new Paragraph("Location: " + event.location).setFontSize(14).setFontColor(new DeviceRgb(25, 118, 210)));
            document.add(new Paragraph("\nDescription / Agenda:\n" + event.description).setFontSize(12));
            
            document.add(new Paragraph("\nScheduled By: " + event.createdBy).setItalic().setFontSize(10).setFontColor(new DeviceRgb(117, 117, 117)));
            document.close(); shareFile(context, file, event.title + " Itinerary");
        } catch (Exception e) {}
    }

    private static void shareFile(Context context, File file, String subject) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND); shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri); shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF via..."));
    }
}
