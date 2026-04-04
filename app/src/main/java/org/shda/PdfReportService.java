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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfReportService {
    
    private static final DeviceRgb SAFFRON = new DeviceRgb(230, 81, 0);
    private static final String VEDIC_SHLOKA = "“Dharmo Rakshati Rakshitah”\n(Dharma protects those who protect it)";

    private static String getFormattedFileName(String prefix) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd_MMM_yyyy_hh_mm_a", Locale.getDefault());
        return sdf.format(new Date()) + "_" + prefix.replace(" ", "_") + ".pdf";
    }

    private static String getGenerationStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return "Generated on: " + sdf.format(new Date());
    }

    private static void addSanataniHeader(Document document, String communityName, String reportType) {
        document.add(new Paragraph(communityName.toUpperCase()).setFontColor(SAFFRON).setBold().setFontSize(24).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph(VEDIC_SHLOKA).setFontSize(11).setItalic().setFontColor(new DeviceRgb(117, 117, 117)).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph(reportType).setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER).setMarginTop(8f));
        document.add(new Paragraph(getGenerationStamp()).setFontSize(10).setItalic().setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("--------------------------------------------------\n").setTextAlignment(TextAlignment.CENTER));
    }

    public static void generateFinancialReport(Context context, String communityName, List<String> dates, List<String> names, List<Float> amounts, List<String> notes, float totalDonations, String reportRange) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, getFormattedFileName("Income_Statement"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            Document document = new Document(new PdfDocument(writer));
            addSanataniHeader(document, communityName, "Official Income Statement");
            document.add(new Paragraph(reportRange).setFontSize(12).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10f));
            document.add(new Paragraph("Total Chanda Collected: ৳" + totalDonations).setBold().setFontSize(14));
            document.add(new Paragraph("\n"));
            if (names.isEmpty()) { document.add(new Paragraph("No transactions recorded.")); } else {
                for(int i = 0; i < names.size(); i++) {
                    document.add(new Paragraph("Date: " + dates.get(i) + "  |  Amount: ৳" + amounts.get(i)).setBold());
                    document.add(new Paragraph("Donor: " + names.get(i)));
                    document.add(new Paragraph("Note: " + notes.get(i)));
                    document.add(new Paragraph("----------------------------------------"));
                }
            }
            document.close(); shareFile(context, file, communityName + " - Income Report");
        } catch (Exception e) { Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show(); }
    }

    // NEW: EXPENSE REPORT PDF
    public static void generateExpenseReport(Context context, String communityName, List<Expense> expenseList, float totalExpense, String reportRange) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, getFormattedFileName("Expense_Statement"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            Document document = new Document(new PdfDocument(writer));
            addSanataniHeader(document, communityName, "Official Expense Statement");
            document.add(new Paragraph(reportRange).setFontSize(12).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10f));
            document.add(new Paragraph("Total Expenses: ৳" + totalExpense).setBold().setFontSize(14).setFontColor(new DeviceRgb(211, 47, 47)));
            document.add(new Paragraph("\n"));
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            for(Expense exp : expenseList) {
                document.add(new Paragraph("Date: " + sdf.format(new Date(exp.timestamp)) + "  |  Amount: ৳" + exp.amount).setBold());
                document.add(new Paragraph("Purpose: " + exp.purpose));
                document.add(new Paragraph("Note: " + (exp.comment.isEmpty() ? "N/A" : exp.comment)));
                document.add(new Paragraph("Authorized By: " + exp.loggedBy).setFontSize(10).setItalic());
                document.add(new Paragraph("----------------------------------------"));
            }
            document.close(); shareFile(context, file, communityName + " - Expense Report");
        } catch (Exception e) { Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show(); }
    }

    public static void generateDonorReceipt(Context context, String communityName, String name, float amount, String note, String date) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, getFormattedFileName(name + "_Receipt"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            addSanataniHeader(document, communityName, "Official Chanda Receipt");
            document.add(new Paragraph("\nTransaction Date: " + date).setFontSize(12));
            document.add(new Paragraph("Received With Gratitude From:").setBold().setFontSize(12));
            document.add(new Paragraph(name).setFontSize(16).setFontColor(SAFFRON));
            document.add(new Paragraph("\nContribution Amount: ৳" + amount).setBold().setFontSize(14));
            document.add(new Paragraph("Purpose/Note: " + note).setFontSize(12));
            document.add(new Paragraph("\n\nMay Ishvara shower you and your family with infinite blessings. Thank you for your devoted contribution.").setItalic().setFontSize(11).setTextAlignment(TextAlignment.CENTER).setFontColor(new DeviceRgb(97, 97, 97)));
            document.close(); shareFile(context, file, communityName + " - Receipt");
        } catch (Exception e) { Toast.makeText(context, "Failed to create Receipt", Toast.LENGTH_SHORT).show(); }
    }

    public static void generateMemberDirectory(Context context, String communityName, List<Member> memberList) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); File file = new File(path, getFormattedFileName("Member_Directory"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            addSanataniHeader(document, communityName, "Official Member Directory");
            for (Member m : memberList) {
                document.add(new Paragraph(m.id + " | " + m.name + " | Phone: " + m.phone + " | Total Donated: ৳" + m.totalDonated));
                document.add(new Paragraph("--------------------------------------------------"));
            }
            document.close(); shareFile(context, file, communityName + " Directory");
        } catch (Exception e) {}
    }

    public static void generateMemberProfile(Context context, String communityName, Member member) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); File file = new File(path, getFormattedFileName(member.id + "_Profile"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            addSanataniHeader(document, communityName, "Official Member Profile");
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()); String joinDate = sdf.format(new Date(member.timestamp));
            document.add(new Paragraph("Member ID: " + member.id).setBold().setFontSize(14));
            document.add(new Paragraph("Name: " + member.name).setFontSize(14));
            document.add(new Paragraph("Phone: " + member.phone).setFontSize(14));
            document.add(new Paragraph("Gotra: " + member.gotra).setFontSize(14));
            document.add(new Paragraph("Blood Group: " + member.bloodGroup).setFontSize(14).setFontColor(new DeviceRgb(211, 47, 47)));
            if(member.role != null) { document.add(new Paragraph("Community Role: " + member.role).setFontSize(14).setFontColor(new DeviceRgb(25, 118, 210))); }
            document.add(new Paragraph("\nJoin Date: " + joinDate).setFontSize(12));
            document.add(new Paragraph("Total Chanda Contributed: ৳" + member.totalDonated).setBold().setFontSize(14).setFontColor(new DeviceRgb(46, 125, 50)));
            document.close(); shareFile(context, file, member.name + " Profile");
        } catch (Exception e) {}
    }

    public static void generateAuditReport(Context context, String communityName, List<String> logs, String reportRange) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); File file = new File(path, getFormattedFileName("Audit_Report"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            addSanataniHeader(document, communityName, "SECURITY AUDIT: Manager Activity Log");
            document.add(new Paragraph(reportRange).setFontSize(12).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10f));
            if (logs.isEmpty()) { document.add(new Paragraph("No manager activity recorded for this period.").setTextAlignment(TextAlignment.CENTER)); } else {
                for (String logEntry : logs) {
                    document.add(new Paragraph(logEntry).setFontSize(11)); document.add(new Paragraph("----------------------------------------"));
                }
            }
            document.close(); shareFile(context, file, communityName + " - Audit Report");
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
