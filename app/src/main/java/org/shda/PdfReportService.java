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

    // Dynamic Auto-Naming for PDF Files
    private static String getFormattedFileName(String prefix) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd_MMM_yyyy_hh_mm_a", Locale.getDefault());
        return sdf.format(new Date()) + "_" + prefix.replace(" ", "_") + ".pdf";
    }

    // Dynamic Timestamp printed inside the PDF
    private static String getGenerationStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return "Generated on: " + sdf.format(new Date());
    }

    // 1. FINANCIAL REPORT (With Date Range)
    public static void generateFinancialReport(Context context, String communityName, List<String> dates, List<String> names, List<Float> amounts, List<String> notes, float totalDonations, String reportRange) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, getFormattedFileName("Financial_Statement"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            Document document = new Document(new PdfDocument(writer));

            document.add(new Paragraph(communityName.toUpperCase()).setFontColor(SAFFRON).setBold().setFontSize(24).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Official Financial Statement").setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(reportRange).setFontSize(12).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(getGenerationStamp()).setFontSize(10).setItalic().setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("--------------------------------------------------\n").setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Total Chanda Collected: ৳" + totalDonations).setBold().setFontSize(14));
            document.add(new Paragraph("\n"));

            if (names.isEmpty()) {
                document.add(new Paragraph("No transactions recorded for this period."));
            } else {
                for(int i = 0; i < names.size(); i++) {
                    document.add(new Paragraph("Date: " + dates.get(i) + "  |  Amount: ৳" + amounts.get(i)).setBold());
                    document.add(new Paragraph("Donor: " + names.get(i)));
                    document.add(new Paragraph("Note: " + notes.get(i)));
                    document.add(new Paragraph("----------------------------------------"));
                }
            }
            document.close();
            shareFile(context, file, communityName + " - Financial Report");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show();
        }
    }

    // 2. INDIVIDUAL DONOR RECEIPT
    public static void generateDonorReceipt(Context context, String communityName, String name, float amount, String note, String date) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, getFormattedFileName(name + "_Receipt"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            Document document = new Document(new PdfDocument(writer));

            document.add(new Paragraph(communityName.toUpperCase()).setFontColor(SAFFRON).setBold().setFontSize(26).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Official Chanda Receipt").setBold().setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(getGenerationStamp()).setFontSize(10).setItalic().setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("--------------------------------------------------\n").setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\nTransaction Date: " + date).setFontSize(12));
            document.add(new Paragraph("Received With Gratitude From:").setBold().setFontSize(12));
            document.add(new Paragraph(name).setFontSize(16).setFontColor(SAFFRON));
            
            document.add(new Paragraph("\nContribution Amount: ৳" + amount).setBold().setFontSize(14));
            document.add(new Paragraph("Purpose/Note: " + note).setFontSize(12));
            document.close();
            shareFile(context, file, communityName + " - Receipt");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to create Receipt", Toast.LENGTH_SHORT).show();
        }
    }

    // 3. FULL MEMBER DIRECTORY
    public static void generateMemberDirectory(Context context, String communityName, List<Member> memberList) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, getFormattedFileName("Member_Directory"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            Document document = new Document(new PdfDocument(writer));

            document.add(new Paragraph(communityName.toUpperCase() + " - Member Directory").setBold().setFontSize(18));
            document.add(new Paragraph(getGenerationStamp()).setFontSize(10).setItalic());
            document.add(new Paragraph("--------------------------------------------------\n"));

            for (Member m : memberList) {
                document.add(new Paragraph(m.id + " | " + m.name + " | Phone: " + m.phone + " | Total Donated: ৳" + m.totalDonated));
                document.add(new Paragraph("--------------------------------------------------"));
            }
            document.close();
            shareFile(context, file, communityName + " Directory");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to create Directory", Toast.LENGTH_SHORT).show();
        }
    }

    // 4. INDIVIDUAL MEMBER PROFILE (Deep Dive)
    public static void generateMemberProfile(Context context, String communityName, Member member) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, getFormattedFileName(member.id + "_Profile"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            Document document = new Document(new PdfDocument(writer));

            document.add(new Paragraph(communityName.toUpperCase()).setFontColor(SAFFRON).setBold().setFontSize(22).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Official Member Profile").setBold().setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(getGenerationStamp()).setFontSize(10).setItalic().setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("--------------------------------------------------\n").setTextAlignment(TextAlignment.CENTER));

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String joinDate = sdf.format(new Date(member.timestamp));

            document.add(new Paragraph("Member ID: " + member.id).setBold().setFontSize(14));
            document.add(new Paragraph("Name: " + member.name).setFontSize(14));
            document.add(new Paragraph("Phone: " + member.phone).setFontSize(14));
            document.add(new Paragraph("Gotra: " + member.gotra).setFontSize(14));
            document.add(new Paragraph("Blood Group: " + member.bloodGroup).setFontSize(14).setFontColor(new DeviceRgb(211, 47, 47)));
            
            // Add Role to PDF if it's set
            if(member.role != null) {
                document.add(new Paragraph("Community Role: " + member.role).setFontSize(14).setFontColor(new DeviceRgb(25, 118, 210)));
            }

            document.add(new Paragraph("\nJoin Date: " + joinDate).setFontSize(12));
            document.add(new Paragraph("Total Chanda Contributed: ৳" + member.totalDonated).setBold().setFontSize(14).setFontColor(new DeviceRgb(46, 125, 50)));

            document.close();
            shareFile(context, file, member.name + " Profile");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to create Profile", Toast.LENGTH_SHORT).show();
        }
    }

    // 5. MANAGER AUDIT TRAIL REPORT (Super Admin Only)
    public static void generateAuditReport(Context context, String communityName, List<String> logs, String reportRange) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, getFormattedFileName("Audit_Report"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            Document document = new Document(new PdfDocument(writer));

            document.add(new Paragraph(communityName.toUpperCase() + " - SECURITY AUDIT").setFontColor(new DeviceRgb(211, 47, 47)).setBold().setFontSize(22).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Manager Activity Log").setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(reportRange).setFontSize(12).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(getGenerationStamp()).setFontSize(10).setItalic().setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("--------------------------------------------------\n").setTextAlignment(TextAlignment.CENTER));

            if (logs.isEmpty()) {
                document.add(new Paragraph("No manager activity recorded for this period.").setTextAlignment(TextAlignment.CENTER));
            } else {
                for (String logEntry : logs) {
                    document.add(new Paragraph(logEntry).setFontSize(11));
                    document.add(new Paragraph("----------------------------------------"));
                }
            }
            document.close();
            shareFile(context, file, communityName + " - Audit Report");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to create Audit PDF", Toast.LENGTH_SHORT).show();
        }
    }

    // System Intent to share/open the generated file
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
