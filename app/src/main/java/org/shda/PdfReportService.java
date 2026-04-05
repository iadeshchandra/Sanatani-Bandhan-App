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
        
        // ✨ DYNAMIC SHLOKA INJECTED INTO EVERY PDF
        document.add(new Paragraph(ShlokaEngine.getDailyShloka()).setFontSize(11).setItalic().setFontColor(new DeviceRgb(117, 117, 117)).setTextAlignment(TextAlignment.CENTER));
        
        document.add(new Paragraph(reportType).setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER).setMarginTop(8f));
        document.add(new Paragraph(getGenerationStamp()).setFontSize(10).setItalic().setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("--------------------------------------------------\n").setTextAlignment(TextAlignment.CENTER));
    }

    public static void generateLoginCredentialsPdf(Context context, String communityName, String memberName, String memberId, String password, String role) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, getFormattedFileName(memberId + "_Credentials"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            Document document = new Document(new PdfDocument(writer));

            addSanataniHeader(document, communityName, "CONFIDENTIAL: Access Credentials");

            document.add(new Paragraph("\nWelcome to the digital portal of " + communityName + "!").setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\nName: " + memberName).setFontSize(14));
            document.add(new Paragraph("Assigned Role: " + role).setFontSize(14).setFontColor(new DeviceRgb(25, 118, 210)));
            
            document.add(new Paragraph("\n--- YOUR LOGIN DETAILS ---").setBold().setFontSize(14).setMarginTop(10f));
            document.add(new Paragraph("SB-ID (Login ID): " + memberId).setBold().setFontSize(18).setFontColor(SAFFRON));
            document.add(new Paragraph("Secure PIN: " + password).setBold().setFontSize(18).setFontColor(new DeviceRgb(211, 47, 47)));
            
            document.add(new Paragraph("\n* Important: Please keep this document secure. You will need the Workspace Admin's email along with these credentials to access the app.")
                .setItalic().setFontSize(10).setFontColor(new DeviceRgb(97, 97, 97)).setMarginTop(20f));

            document.close();
            shareFile(context, file, memberName + " - Login Credentials");
        } catch (Exception e) {
            Toast.makeText(context, "Failed to create Credentials PDF", Toast.LENGTH_SHORT).show();
        }
    }

    public static void generateFinancialReport(Context context, String communityName, List<String> dates, List<String> names, List<Float> amounts, List<String> notes, float totalDonations, String reportRange) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); File file = new File(path, getFormattedFileName("Income_Statement"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            
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
        } catch (Exception e) {}
    }

    public static void generateExpenseReport(Context context, String communityName, List<Expense> expenseList, float totalExpense, String reportRange) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); File file = new File(path, getFormattedFileName("Utsav_Expenses"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            
            addSanataniHeader(document, communityName, "Official Utsav Expense Ledger");
            document.add(new Paragraph(reportRange).setFontSize(12).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10f));
            document.add(new Paragraph("Total Expenses: ৳" + totalExpense).setBold().setFontSize(14).setFontColor(new DeviceRgb(211, 47, 47))); document.add(new Paragraph("\n"));
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            for(Expense exp : expenseList) {
                document.add(new Paragraph("🪔 Utsav/Puja: " + exp.eventName).setBold().setFontSize(13).setFontColor(SAFFRON));
                document.add(new Paragraph("Date: " + sdf.format(new Date(exp.timestamp)) + "  |  Amount: ৳" + exp.amount).setBold());
                document.add(new Paragraph("Item/Seva: " + exp.itemName)); 
                document.add(new Paragraph("Handled By: " + exp.involvedPerson));
                document.add(new Paragraph("Logged By: " + exp.loggedBy).setFontSize(10).setItalic()); 
                document.add(new Paragraph("----------------------------------------"));
            }
            document.close(); shareFile(context, file, communityName + " - Expense Ledger");
        } catch (Exception e) {}
    }

    public static void generateDonorReceipt(Context context, String communityName, String name, float amount, String note, String date) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); File file = new File(path, getFormattedFileName("Receipt"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            
            addSanataniHeader(document, communityName, "Official Chanda Receipt");
            document.add(new Paragraph("\nTransaction Date: " + date).setFontSize(12)); 
            document.add(new Paragraph("Received With Gratitude From:").setBold().setFontSize(12));
            document.add(new Paragraph(name).setFontSize(16).setFontColor(SAFFRON)); 
            document.add(new Paragraph("\nContribution Amount: ৳" + amount).setBold().setFontSize(14));
            document.add(new Paragraph("Purpose/Note: " + note).setFontSize(12)); 
            document.add(new Paragraph("\n\nMay Ishvara shower you and your family with infinite blessings. Thank you for your devoted contribution.").setItalic().setFontSize(11).setTextAlignment(TextAlignment.CENTER).setFontColor(new DeviceRgb(97, 97, 97)));
            
            document.close(); shareFile(context, file, communityName + " - Receipt");
        } catch (Exception e) {}
    }

    // ✨ NEW: MASTER DONOR STATEMENT LOGIC
    public static void generateDonorStatement(Context context, String communityName, TransactionActivity.GroupedDonation gd) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); 
            String safeName = gd.displayName.replace("[Member]", "").replace("[Guest]", "").replaceAll("[^a-zA-Z0-9]", "_").trim();
            File file = new File(path, getFormattedFileName(safeName + "_Statement"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            
            addSanataniHeader(document, communityName, "Official Donor Statement");
            
            document.add(new Paragraph("Donor: " + gd.displayName).setBold().setFontSize(16).setFontColor(SAFFRON));
            document.add(new Paragraph("Lifetime Contribution: ৳" + gd.totalDonated).setBold().setFontSize(14).setFontColor(new DeviceRgb(46, 125, 50)));
            document.add(new Paragraph("Total Records: " + gd.history.size() + "\n").setFontSize(12).setMarginBottom(10f));
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            
            gd.history.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

            for(TransactionActivity.SingleDonation sd : gd.history) {
                document.add(new Paragraph("Date: " + sdf.format(new Date(sd.timestamp)) + "  |  Amount: ৳" + sd.amount).setBold().setFontSize(12));
                document.add(new Paragraph("Note/Purpose: " + sd.note).setFontSize(11));
                
                if (sd.phone != null && !sd.phone.isEmpty()) document.add(new Paragraph("Phone: " + sd.phone).setFontSize(10));
                if (sd.address != null && !sd.address.isEmpty()) document.add(new Paragraph("Address: " + sd.address).setFontSize(10));
                if (sd.collectedBy != null && !sd.collectedBy.isEmpty()) document.add(new Paragraph("Handled By: " + sd.collectedBy).setFontSize(10).setItalic());
                
                document.add(new Paragraph("System Log: " + sd.loggedBy).setFontSize(9).setItalic().setFontColor(new DeviceRgb(158, 158, 158)));
                document.add(new Paragraph("----------------------------------------"));
            }
            
            document.add(new Paragraph("\nMay Ishvara shower you and your family with infinite blessings.").setItalic().setFontSize(11).setTextAlignment(TextAlignment.CENTER).setFontColor(new DeviceRgb(97, 97, 97)));
            
            document.close(); shareFile(context, file, gd.displayName + " Statement");
        } catch (Exception e) {
            Toast.makeText(context, "Failed to create Statement PDF", Toast.LENGTH_SHORT).show();
        }
    }

    public static void generateMemberDirectory(Context context, String communityName, List<Member> memberList) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); File file = new File(path, getFormattedFileName("Member_Directory"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            addSanataniHeader(document, communityName, "Official Member Directory");
            for (Member m : memberList) {
                document.add(new Paragraph(m.id + " | " + m.name + " | Phone: " + m.phone + " | Total Donated: ৳" + m.totalDonated)); document.add(new Paragraph("--------------------------------------------------"));
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
            
            if(member.role != null) { document.add(new Paragraph("Community Role: " + member.role).setFontSize(14).setFontColor(new DeviceRgb(25, 118, 210))); }
            
            document.add(new Paragraph("Phone: " + member.phone).setFontSize(14)); 
            document.add(new Paragraph("Gotra: " + member.gotra).setFontSize(14));
            document.add(new Paragraph("Blood Group: " + member.bloodGroup).setFontSize(14).setFontColor(new DeviceRgb(211, 47, 47)));
            
            if (member.fatherName != null && !member.fatherName.isEmpty()) document.add(new Paragraph("Father's Name: " + member.fatherName).setFontSize(12));
            if (member.motherName != null && !member.motherName.isEmpty()) document.add(new Paragraph("Mother's Name: " + member.motherName).setFontSize(12));
            if (member.nid != null && !member.nid.isEmpty()) document.add(new Paragraph("NID Number: " + member.nid).setFontSize(12));
            if (member.address != null && !member.address.isEmpty()) document.add(new Paragraph("Address: " + member.address).setFontSize(12));

            document.add(new Paragraph("\nJoin Date: " + joinDate).setFontSize(12)); 
            document.add(new Paragraph("Total Chanda Contributed: ৳" + member.totalDonated).setBold().setFontSize(14).setFontColor(new DeviceRgb(46, 125, 50)));
            
            if (member.addedBySignature != null && !member.addedBySignature.isEmpty()) {
                document.add(new Paragraph("\n[Profile verified and logged by: " + member.addedBySignature + "]").setItalic().setFontSize(10).setFontColor(new DeviceRgb(117, 117, 117)));
            }

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
                for (String logEntry : logs) { document.add(new Paragraph(logEntry).setFontSize(11)); document.add(new Paragraph("----------------------------------------")); }
            }
            document.close(); shareFile(context, file, communityName + " - Audit Report");
        } catch (Exception e) {}
    }

    public static void generatePollReport(Context context, String communityName, Poll poll, boolean isSuperAdmin) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, getFormattedFileName("Panchayat_Insight"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            
            addSanataniHeader(document, communityName, "Community Panchayat Insight");
            
            String status = System.currentTimeMillis() > poll.endTimestamp ? "[STATUS: CLOSED]" : "[STATUS: ACTIVE]";
            document.add(new Paragraph(status).setBold().setFontSize(12).setFontColor(System.currentTimeMillis() > poll.endTimestamp ? new DeviceRgb(211, 47, 47) : new DeviceRgb(46, 125, 50)).setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph("Topic: " + poll.question).setBold().setFontSize(16).setMarginTop(10f));
            if (poll.officialComment != null && !poll.officialComment.isEmpty()) {
                document.add(new Paragraph("Official Remark: " + poll.officialComment).setFontSize(11).setItalic().setFontColor(new DeviceRgb(117, 117, 117)).setMarginBottom(10f));
            }
            
            List<String> vA = new ArrayList<>(); List<String> vB = new ArrayList<>(); List<String> vC = new ArrayList<>(); List<String> vD = new ArrayList<>();
            if (poll.votes != null) {
                for (String uid : poll.votes.keySet()) {
                    if (poll.votes.get(uid).equals("A")) vA.add(uid); else if (poll.votes.get(uid).equals("B")) vB.add(uid);
                    else if (poll.votes.get(uid).equals("C")) vC.add(uid); else if (poll.votes.get(uid).equals("D")) vD.add(uid);
                }
            }
            
            document.add(new Paragraph("1. " + poll.optionA + " (" + vA.size() + " votes)").setFontSize(14));
            if (isSuperAdmin && !vA.isEmpty()) document.add(new Paragraph("   Voters: " + String.join(", ", vA)).setFontSize(10));
            
            document.add(new Paragraph("2. " + poll.optionB + " (" + vB.size() + " votes)").setFontSize(14));
            if (isSuperAdmin && !vB.isEmpty()) document.add(new Paragraph("   Voters: " + String.join(", ", vB)).setFontSize(10));

            if (poll.optionC != null && !poll.optionC.isEmpty()) {
                document.add(new Paragraph("3. " + poll.optionC + " (" + vC.size() + " votes)").setFontSize(14));
                if (isSuperAdmin && !vC.isEmpty()) document.add(new Paragraph("   Voters: " + String.join(", ", vC)).setFontSize(10));
            }
            if (poll.optionD != null && !poll.optionD.isEmpty()) {
                document.add(new Paragraph("4. " + poll.optionD + " (" + vD.size() + " votes)").setFontSize(14));
                if (isSuperAdmin && !vD.isEmpty()) document.add(new Paragraph("   Voters: " + String.join(", ", vD)).setFontSize(10));
            }
            
            if (!isSuperAdmin) document.add(new Paragraph("\n[Secret Ballot: Voter identities protected]").setFontSize(10).setItalic());

            document.close(); shareFile(context, file, communityName + " - Poll Insight");
        } catch (Exception e) { Toast.makeText(context, "Failed to create Poll PDF", Toast.LENGTH_SHORT).show(); }
    }

    public static void generateMultiplePollsReport(Context context, String communityName, List<Poll> polls, String reportRange, boolean isSuperAdmin) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); 
            File file = new File(path, getFormattedFileName("Panchayat_Summary"));
            PdfWriter writer = new PdfWriter(file.getAbsolutePath()); Document document = new Document(new PdfDocument(writer));
            
            addSanataniHeader(document, communityName, "Community Panchayat Summary");
            document.add(new Paragraph(reportRange).setFontSize(12).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10f));
            
            for (Poll poll : polls) {
                String status = System.currentTimeMillis() > poll.endTimestamp ? "[CLOSED]" : "[ACTIVE]";
                document.add(new Paragraph("Q: " + poll.question + " " + status).setBold().setFontSize(14));
                if (poll.officialComment != null && !poll.officialComment.isEmpty()) {
                    document.add(new Paragraph("Remark: " + poll.officialComment).setFontSize(10).setItalic().setFontColor(new DeviceRgb(117, 117, 117)));
                }
                
                List<String> vA = new ArrayList<>(); List<String> vB = new ArrayList<>(); List<String> vC = new ArrayList<>(); List<String> vD = new ArrayList<>();
                if (poll.votes != null) {
                    for (String uid : poll.votes.keySet()) {
                        if (poll.votes.get(uid).equals("A")) vA.add(uid); else if (poll.votes.get(uid).equals("B")) vB.add(uid);
                        else if (poll.votes.get(uid).equals("C")) vC.add(uid); else if (poll.votes.get(uid).equals("D")) vD.add(uid);
                    }
                }
                
                document.add(new Paragraph(" • " + poll.optionA + " (" + vA.size() + " votes)").setFontSize(12));
                if (isSuperAdmin && !vA.isEmpty()) document.add(new Paragraph("   Voters: " + String.join(", ", vA)).setFontSize(9));
                
                document.add(new Paragraph(" • " + poll.optionB + " (" + vB.size() + " votes)").setFontSize(12));
                if (isSuperAdmin && !vB.isEmpty()) document.add(new Paragraph("   Voters: " + String.join(", ", vB)).setFontSize(9));
                
                if (poll.optionC != null && !poll.optionC.isEmpty()) {
                    document.add(new Paragraph(" • " + poll.optionC + " (" + vC.size() + " votes)").setFontSize(12));
                    if (isSuperAdmin && !vC.isEmpty()) document.add(new Paragraph("   Voters: " + String.join(", ", vC)).setFontSize(9));
                }
                if (poll.optionD != null && !poll.optionD.isEmpty()) {
                    document.add(new Paragraph(" • " + poll.optionD + " (" + vD.size() + " votes)").setFontSize(12));
                    if (isSuperAdmin && !vD.isEmpty()) document.add(new Paragraph("   Voters: " + String.join(", ", vD)).setFontSize(9));
                }
                
                if (!isSuperAdmin) document.add(new Paragraph("   [Voter identities protected]").setFontSize(9).setItalic());
                document.add(new Paragraph("----------------------------------------"));
            }
            document.close(); shareFile(context, file, communityName + " - Panchayat Summary");
        } catch (Exception e) { Toast.makeText(context, "Failed to create Summary PDF", Toast.LENGTH_SHORT).show(); }
    }

    private static void shareFile(Context context, File file, String subject) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND); shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri); shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF via..."));
    }
}
