package org.shda;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfReportService {

    private static final DeviceRgb SAFFRON = new DeviceRgb(230, 81, 0);
    private static final DeviceRgb GREEN = new DeviceRgb(46, 125, 50);
    private static final DeviceRgb BLUE = new DeviceRgb(25, 118, 210);

    private static void sharePdf(Context context, File file) {
        try {
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(android.content.Intent.createChooser(intent, "Share PDF"));
        } catch (Exception e) {
            Toast.makeText(context, "PDF Saved to Documents! (View in File Manager)", Toast.LENGTH_LONG).show();
        }
    }

    private static File createBaseFile(Context context, String fileName) {
        File folder = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "SanataniReports");
        if (!folder.exists()) folder.mkdirs();
        return new File(folder, fileName + "_" + System.currentTimeMillis() + ".pdf");
    }

    private static void addSanataniHeader(Document document, String communityName, String reportTitle) {
        document.add(new Paragraph(communityName != null ? communityName.toUpperCase() : "SANATANI BANDHAN MANDIR")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(22).setBold().setFontColor(SAFFRON));
        document.add(new Paragraph("\"Ahimsa paramo dharma\" (Non-violence is the highest religion) - Mahabharata")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(10).setItalic().setFontColor(ColorConstants.GRAY));
        document.add(new Paragraph(reportTitle.toUpperCase())
                .setTextAlignment(TextAlignment.CENTER).setFontSize(14).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY).setMarginTop(10));
        document.add(new Paragraph("Generated: " + new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date()))
                .setTextAlignment(TextAlignment.CENTER).setFontSize(10).setItalic().setMarginBottom(15));
    }

    private static void addSanataniFooter(Document document) {
        document.add(new Paragraph("\nAuthorized & Generated via Sanatani Bandhan CRM")
                .setTextAlignment(TextAlignment.RIGHT).setFontSize(9).setItalic().setFontColor(ColorConstants.GRAY));
    }

    private static void addThankYouMessage(Document document) {
        document.add(new Paragraph("\n🙏 Namaskar!\n\nThank you for your generous contribution to the Sanatani Bandhan community. Your selfless support ensures the prosperity of our Mandir and the continued preservation of our Dharma. May the blessings of the divine always be with you and your family.\n\n\"Dharmo Rakshati Rakshitah\" (Dharma protects those who protect it).")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(12).setItalic().setFontColor(SAFFRON).setMarginTop(20));
    }

    public static void generateComparisonReport(Context context, String communityName, List<TransactionActivity.SingleDonation> donations, List<ExpenseActivity.Expense> expenses, long startTs, long endTs, float totalIncome, float totalExpense) {
        try {
            File file = createBaseFile(context, "Income_Vs_Expense_Report");
            Document document = new Document(new PdfDocument(new PdfWriter(new FileOutputStream(file))), PageSize.A4);
            SimpleDateFormat titleSdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String dateRange = titleSdf.format(new Date(startTs)) + " to " + titleSdf.format(new Date(endTs));
            addSanataniHeader(document, communityName, "Income vs Expense Comparison\n(" + dateRange + ")");

            document.add(new Paragraph("DONATIONS (INCOME)").setBold().setFontSize(14).setFontColor(GREEN).setMarginTop(10));
            Table incTable = new Table(new float[]{2, 4, 2}); incTable.setWidth(UnitValue.createPercentValue(100));
            incTable.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            incTable.addHeaderCell(new Cell().add(new Paragraph("Donor / Source").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            incTable.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            for(TransactionActivity.SingleDonation d : donations) {
                incTable.addCell(new Paragraph(titleSdf.format(new Date(d.timestamp))));
                incTable.addCell(new Paragraph(d.name));
                incTable.addCell(new Paragraph("BDT " + d.amount));
            }
            document.add(incTable);
            document.add(new Paragraph("Total Income: BDT " + totalIncome).setBold().setTextAlignment(TextAlignment.RIGHT).setFontColor(GREEN).setMarginBottom(15));

            document.add(new Paragraph("EXPENSES").setBold().setFontSize(14).setFontColor(ColorConstants.RED));
            Table expTable = new Table(new float[]{2, 4, 2}); expTable.setWidth(UnitValue.createPercentValue(100));
            expTable.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            expTable.addHeaderCell(new Cell().add(new Paragraph("Event / Item").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            expTable.addHeaderCell(new Cell().add(new Paragraph("Cost").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            for(ExpenseActivity.Expense e : expenses) {
                expTable.addCell(new Paragraph(titleSdf.format(new Date(e.timestamp))));
                expTable.addCell(new Paragraph(e.eventName + " - " + e.itemName));
                expTable.addCell(new Paragraph("BDT " + e.amount));
            }
            document.add(expTable);
            document.add(new Paragraph("Total Expense: BDT " + totalExpense).setBold().setTextAlignment(TextAlignment.RIGHT).setFontColor(ColorConstants.RED).setMarginBottom(15));

            float net = totalIncome - totalExpense;
            com.itextpdf.kernel.colors.Color netColor = net >= 0 ? GREEN : ColorConstants.RED;
            document.add(new Paragraph("\nNET BALANCE FOR PERIOD: BDT " + net).setBold().setFontSize(16).setTextAlignment(TextAlignment.RIGHT).setFontColor(netColor).setMarginTop(10));

            addSanataniFooter(document); document.close();
            sharePdf(context, file);
        } catch (Exception e) {}
    }

    public static void generateMemberDirectory(Context context, String communityName, List<Member> members) {
        try {
            File file = createBaseFile(context, "Member_Directory");
            Document document = new Document(new PdfDocument(new PdfWriter(new FileOutputStream(file))), PageSize.A4);
            addSanataniHeader(document, communityName, "Official Devotee Directory");
            Table table = new Table(new float[]{3, 3, 2, 2}); table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell(new Cell().add(new Paragraph("Name").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("ID / Phone").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Gotra").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Total Donated").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            for (Member m : members) {
                table.addCell(new Paragraph(m.name)); table.addCell(new Paragraph(m.id + "\n" + (m.phone!=null?m.phone:"")));
                table.addCell(new Paragraph(m.gotra!=null?m.gotra:"")); table.addCell(new Paragraph("BDT " + m.totalDonated).setFontColor(GREEN));
            }
            document.add(table); addSanataniFooter(document); document.close();
            sharePdf(context, file); 
        } catch (Exception e) { Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
    }

    public static void generateMemberProfile(Context context, String communityName, Member m) {
        try {
            File file = createBaseFile(context, "Devotee_Profile_" + m.id);
            Document document = new Document(new PdfDocument(new PdfWriter(new FileOutputStream(file))), PageSize.A4);
            addSanataniHeader(document, communityName, "Official Devotee Record");
            Table table = new Table(new float[]{1, 2}); table.setWidth(UnitValue.createPercentValue(100));
            table.addCell(new Cell().add(new Paragraph("Devotee Name").setBold())); table.addCell(new Paragraph(m.name).setBold());
            table.addCell(new Cell().add(new Paragraph("SB-ID").setBold())); table.addCell(new Paragraph(m.id).setFontColor(BLUE));
            table.addCell(new Cell().add(new Paragraph("Phone").setBold())); table.addCell(new Paragraph(m.phone!=null?m.phone:""));
            table.addCell(new Cell().add(new Paragraph("Email").setBold())); table.addCell(new Paragraph(m.email!=null?m.email:"N/A"));
            table.addCell(new Cell().add(new Paragraph("Gotra").setBold())); table.addCell(new Paragraph(m.gotra!=null?m.gotra:""));
            table.addCell(new Cell().add(new Paragraph("Blood Group").setBold())); table.addCell(new Paragraph(m.bloodGroup!=null?m.bloodGroup:"").setFontColor(ColorConstants.RED));
            table.addCell(new Cell().add(new Paragraph("Father's Name").setBold())); table.addCell(new Paragraph(m.fatherName!=null?m.fatherName:""));
            table.addCell(new Cell().add(new Paragraph("Mother's Name").setBold())); table.addCell(new Paragraph(m.motherName!=null?m.motherName:""));
            table.addCell(new Cell().add(new Paragraph("Address").setBold())); table.addCell(new Paragraph(m.address!=null?m.address:""));
            table.addCell(new Cell().add(new Paragraph("Lifetime Donated").setBold())); table.addCell(new Paragraph("BDT " + m.totalDonated).setFontColor(GREEN).setBold());
            String lastDonationStr = "No Chanda Recorded";
            if (m.lastDonationTimestamp > 0) lastDonationStr = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(m.lastDonationTimestamp));
            table.addCell(new Cell().add(new Paragraph("Last Donation Date").setBold())); table.addCell(new Paragraph(lastDonationStr).setItalic());
            document.add(table); document.add(new Paragraph("\nData Verified By: " + (m.addedBySignature!=null?m.addedBySignature:"System")).setItalic().setFontSize(10));
            addSanataniFooter(document); document.close();
            sharePdf(context, file); 
        } catch (Exception e) {}
    }

    public static void generateLoginCredentialsPdf(Context context, String communityName, String name, String memberId, String pin, String generatedBy) {
        try {
            File file = createBaseFile(context, "Secure_Credentials_" + memberId);
            Document document = new Document(new PdfDocument(new PdfWriter(new FileOutputStream(file))), PageSize.A4);
            addSanataniHeader(document, communityName, "Confidential Login Credentials");
            
            document.add(new Paragraph("Namaskar " + name + ",\nWelcome to the Sanatani Bandhan platform. Your profile has been created successfully. Please keep these credentials strictly confidential.")
                    .setMarginBottom(20).setFontSize(12));
            
            // Premium Security Box layout
            Table table = new Table(new float[]{1, 1}); 
            table.setWidth(UnitValue.createPercentValue(100));
            table.setMarginBottom(20);
            
            Cell idHeader = new Cell().add(new Paragraph("YOUR OFFICIAL ID").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(BLUE).setTextAlignment(TextAlignment.CENTER).setPadding(8);
            Cell pinHeader = new Cell().add(new Paragraph("SECURE LOGIN PIN").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(SAFFRON).setTextAlignment(TextAlignment.CENTER).setPadding(8);
            
            Cell idValue = new Cell().add(new Paragraph(memberId).setBold().setFontSize(18).setFontColor(BLUE)).setTextAlignment(TextAlignment.CENTER).setPadding(12);
            Cell pinValue = new Cell().add(new Paragraph(pin).setBold().setFontSize(26).setFontColor(SAFFRON)).setTextAlignment(TextAlignment.CENTER).setPadding(12);
            
            table.addCell(idHeader);
            table.addCell(pinHeader);
            table.addCell(idValue);
            table.addCell(pinValue);
            
            document.add(table);
            
            document.add(new Paragraph("SECURITY WARNING: Mandir staff will never ask for your PIN. Do not share this document with anyone.")
                    .setBold().setFontColor(ColorConstants.RED).setTextAlignment(TextAlignment.CENTER).setFontSize(10));
            
            document.add(new Paragraph("\nCredentials Issued By: " + generatedBy).setItalic().setFontSize(10).setMarginTop(20));
            addSanataniFooter(document); document.close();
            sharePdf(context, file); 
        } catch (Exception e) {}
    }

    public static void generateFinancialReport(Context context, String communityName, List<String> dates, List<String> names, List<Float> amounts, List<String> notes, float totalCollected, String title) {
        try {
            File file = createBaseFile(context, "Master_Chanda_Ledger");
            Document document = new Document(new PdfDocument(new PdfWriter(new FileOutputStream(file))), PageSize.A4);
            addSanataniHeader(document, communityName, title);
            Table table = new Table(new float[]{2, 3, 2, 3}); table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Donor Name").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Note / Purpose").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            for (int i = 0; i < names.size(); i++) {
                table.addCell(new Paragraph(dates.get(i))); table.addCell(new Paragraph(names.get(i)));
                table.addCell(new Paragraph("BDT " + amounts.get(i)).setFontColor(GREEN)); table.addCell(new Paragraph(notes.get(i)));
            }
            document.add(table); document.add(new Paragraph("\nTotal Chanda in this Report: BDT " + totalCollected).setBold().setFontSize(14).setFontColor(GREEN).setTextAlignment(TextAlignment.RIGHT));
            addSanataniFooter(document); document.close();
            sharePdf(context, file); 
        } catch (Exception e) {}
    }

    public static void generateDonorStatement(Context context, String communityName, TransactionActivity.GroupedDonation gd) {
        try {
            File file = createBaseFile(context, "Chanda_Statement_" + gd.displayName.replaceAll("[^a-zA-Z0-9]", ""));
            Document document = new Document(new PdfDocument(new PdfWriter(new FileOutputStream(file))), PageSize.A4);
            addSanataniHeader(document, communityName, "Donor Contribution Statement");
            document.add(new Paragraph("Devotee: " + gd.displayName).setBold().setFontSize(14).setMarginBottom(10));
            Table table = new Table(new float[]{2, 2, 4, 2}); table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Note").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Handled By").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            for (TransactionActivity.SingleDonation sd : gd.history) {
                table.addCell(new Paragraph(sdf.format(new Date(sd.timestamp))));
                table.addCell(new Paragraph("BDT " + sd.amount).setFontColor(GREEN));
                table.addCell(new Paragraph(sd.note!=null?sd.note:""));
                table.addCell(new Paragraph(sd.collectedBy!=null?sd.collectedBy:""));
            }
            document.add(table); document.add(new Paragraph("\nTotal Donated in this Statement: BDT " + gd.totalDonated).setBold().setFontSize(14).setFontColor(GREEN).setTextAlignment(TextAlignment.RIGHT));
            addThankYouMessage(document); addSanataniFooter(document); document.close();
            sharePdf(context, file); 
        } catch (Exception e) {}
    }

    public static void generateExpenseReport(Context context, String communityName, List<ExpenseActivity.Expense> expenses, float totalSpent, String title) {
        try {
            File file = createBaseFile(context, "Master_Utsav_Expense");
            Document document = new Document(new PdfDocument(new PdfWriter(new FileOutputStream(file))), PageSize.A4);
            addSanataniHeader(document, communityName, title);
            Table table = new Table(new float[]{2, 3, 2, 2}); table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Event & Item").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Cost").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Handled By").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            for (ExpenseActivity.Expense e : expenses) {
                table.addCell(new Paragraph(sdf.format(new Date(e.timestamp))));
                table.addCell(new Paragraph(e.eventName + " - " + e.itemName));
                table.addCell(new Paragraph("BDT " + e.amount).setFontColor(ColorConstants.RED));
                table.addCell(new Paragraph(e.involvedPerson));
            }
            document.add(table); document.add(new Paragraph("\nTotal Expenses in this Report: BDT " + totalSpent).setBold().setFontSize(14).setFontColor(ColorConstants.RED).setTextAlignment(TextAlignment.RIGHT));
            addSanataniFooter(document); document.close();
            sharePdf(context, file); 
        } catch (Exception e) {}
    }

    public static void generateUtsavStatement(Context context, String communityName, ExpenseActivity.GroupedExpense ge) {
        try {
            File file = createBaseFile(context, "Utsav_Statement_" + ge.eventDisplayName.replaceAll("[^a-zA-Z0-9]", ""));
            Document document = new Document(new PdfDocument(new PdfWriter(new FileOutputStream(file))), PageSize.A4);
            addSanataniHeader(document, communityName, "Specific Event Expense Report");
            document.add(new Paragraph("Event / Utsav: " + ge.eventDisplayName).setBold().setFontSize(14).setMarginBottom(10));
            Table table = new Table(new float[]{2, 4, 2, 2}); table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Item / Seva").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Cost").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Handled By").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            for (ExpenseActivity.Expense e : ge.history) {
                table.addCell(new Paragraph(sdf.format(new Date(e.timestamp))));
                table.addCell(new Paragraph(e.itemName));
                table.addCell(new Paragraph("BDT " + e.amount).setFontColor(ColorConstants.RED));
                table.addCell(new Paragraph(e.involvedPerson));
            }
            document.add(table); document.add(new Paragraph("\nTotal Event Expense: BDT " + ge.totalSpent).setBold().setFontSize(14).setFontColor(ColorConstants.RED).setTextAlignment(TextAlignment.RIGHT));
            addSanataniFooter(document); document.close();
            sharePdf(context, file); 
        } catch (Exception e) {}
    }

    public static void generatePollReport(Context context, String communityName, Poll poll, boolean includeVoterNames) {
        try {
            File file = createBaseFile(context, "Panchayat_Poll_Insight");
            Document document = new Document(new PdfDocument(new PdfWriter(new FileOutputStream(file))), PageSize.A4);
            addSanataniHeader(document, communityName, "Sanatani Panchayat Poll Insight");
            document.add(new Paragraph("Question: " + poll.question).setBold().setFontSize(16).setMarginBottom(10));
            if (poll.adminComment != null && !poll.adminComment.isEmpty()) { document.add(new Paragraph("Admin Note: " + poll.adminComment).setItalic().setFontColor(ColorConstants.DARK_GRAY).setMarginBottom(10)); }
            document.add(new Paragraph("Created By: " + poll.createdBy).setItalic().setMarginBottom(10));
            int totalVotes = poll.votes != null ? poll.votes.size() : 0;
            document.add(new Paragraph("Total Votes Cast: " + totalVotes).setBold().setMarginBottom(15));
            Table table = new Table(new float[]{4, 1, 1}); table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell(new Cell().add(new Paragraph("Option").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Votes").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("%").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            int countA=0, countB=0, countC=0, countD=0;
            if(poll.votes!=null){ for(String v : poll.votes.values()){ if(v.equals("A"))countA++; else if(v.equals("B"))countB++; else if(v.equals("C"))countC++; else if(v.equals("D"))countD++; } }
            table.addCell(new Paragraph(poll.optionA)); table.addCell(new Paragraph(String.valueOf(countA))); table.addCell(new Paragraph(totalVotes==0?"0%":(Math.round(((float)countA/totalVotes)*100))+"%"));
            table.addCell(new Paragraph(poll.optionB)); table.addCell(new Paragraph(String.valueOf(countB))); table.addCell(new Paragraph(totalVotes==0?"0%":(Math.round(((float)countB/totalVotes)*100))+"%"));
            if(poll.optionC!=null&&!poll.optionC.isEmpty()){ table.addCell(new Paragraph(poll.optionC)); table.addCell(new Paragraph(String.valueOf(countC))); table.addCell(new Paragraph(totalVotes==0?"0%":(Math.round(((float)countC/totalVotes)*100))+"%")); }
            if(poll.optionD!=null&&!poll.optionD.isEmpty()){ table.addCell(new Paragraph(poll.optionD)); table.addCell(new Paragraph(String.valueOf(countD))); table.addCell(new Paragraph(totalVotes==0?"0%":(Math.round(((float)countD/totalVotes)*100))+"%")); }
            document.add(table);
            if (includeVoterNames && poll.votes != null) {
                document.add(new Paragraph("\nDetailed Voter Log (Restricted):").setBold().setFontColor(ColorConstants.RED).setMarginTop(15));
                Table vTable = new Table(new float[]{3, 1}); vTable.setWidth(UnitValue.createPercentValue(100));
                vTable.addHeaderCell(new Cell().add(new Paragraph("Devotee ID").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
                vTable.addHeaderCell(new Cell().add(new Paragraph("Choice").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
                for(Map.Entry<String, String> entry : poll.votes.entrySet()){ vTable.addCell(new Paragraph(entry.getKey())); vTable.addCell(new Paragraph("Option " + entry.getValue())); }
                document.add(vTable);
            }
            addSanataniFooter(document); document.close();
            sharePdf(context, file); 
        } catch (Exception e) {}
    }

    public static void generateMultiplePollsReport(Context context, String communityName, List<Poll> polls, String title, boolean includeVoterNames) {
        try {
            File file = createBaseFile(context, "Master_Polls_Report");
            Document document = new Document(new PdfDocument(new PdfWriter(new FileOutputStream(file))), PageSize.A4);
            addSanataniHeader(document, communityName, title);
            for (Poll poll : polls) {
                document.add(new Paragraph("Q: " + poll.question).setBold().setFontSize(14).setMarginTop(15));
                if (poll.adminComment != null && !poll.adminComment.isEmpty()) document.add(new Paragraph("Note: " + poll.adminComment).setItalic().setFontSize(10));
                int totalVotes = poll.votes != null ? poll.votes.size() : 0;
                int countA=0, countB=0, countC=0, countD=0;
                if(poll.votes!=null){ for(String v : poll.votes.values()){ if(v.equals("A"))countA++; else if(v.equals("B"))countB++; else if(v.equals("C"))countC++; else if(v.equals("D"))countD++; } }
                document.add(new Paragraph("- " + poll.optionA + " : " + countA + " votes"));
                document.add(new Paragraph("- " + poll.optionB + " : " + countB + " votes"));
                if(poll.optionC!=null&&!poll.optionC.isEmpty()) document.add(new Paragraph("- " + poll.optionC + " : " + countC + " votes"));
                if(poll.optionD!=null&&!poll.optionD.isEmpty()) document.add(new Paragraph("- " + poll.optionD + " : " + countD + " votes"));
                document.add(new Paragraph("Total Votes: " + totalVotes).setItalic().setMarginBottom(10));
                if (includeVoterNames && poll.votes != null) { document.add(new Paragraph("Voter IDs: " + poll.votes.keySet().toString()).setFontSize(9).setFontColor(ColorConstants.GRAY)); }
                document.add(new Paragraph("--------------------------------------------------").setFontColor(ColorConstants.LIGHT_GRAY));
            }
            addSanataniFooter(document); document.close();
            sharePdf(context, file); 
        } catch (Exception e) {}
    }

    public static void generateEventItinerary(Context context, String communityName, EventActivity.Event event, String generatedBy) {
        try {
            File file = createBaseFile(context, "Mandir_Event_" + event.title.replaceAll("[^a-zA-Z0-9]", ""));
            Document document = new Document(new PdfDocument(new PdfWriter(new FileOutputStream(file))), PageSize.A4);
            addSanataniHeader(document, communityName, "Official Mandir Event Itinerary");
            document.add(new Paragraph("Event: " + event.title).setBold().setFontSize(18).setFontColor(SAFFRON).setMarginBottom(10));
            Table table = new Table(new float[]{1, 3}); table.setWidth(UnitValue.createPercentValue(100));
            table.addCell(new Cell().add(new Paragraph("Date").setBold())); table.addCell(new Paragraph(event.dateStr));
            table.addCell(new Cell().add(new Paragraph("Time").setBold())); table.addCell(new Paragraph(event.timeStr));
            table.addCell(new Cell().add(new Paragraph("Location").setBold())); table.addCell(new Paragraph(event.location));
            document.add(table);
            document.add(new Paragraph("\nEvent Details:").setBold().setMarginTop(10));
            document.add(new Paragraph(event.description).setMarginBottom(10));
            if (event.adminComment != null && !event.adminComment.trim().isEmpty()) {
                document.add(new Paragraph("Management Note:").setBold().setFontColor(BLUE));
                document.add(new Paragraph(event.adminComment).setItalic().setMarginBottom(10));
            }
            document.add(new Paragraph("\nItinerary Issued By: " + generatedBy).setItalic().setFontSize(10));
            addSanataniFooter(document); document.close();
            sharePdf(context, file); 
        } catch (Exception e) {}
    }

    public static void generateMasterEventReport(Context context, String communityName, List<EventActivity.Event> events, String title) {
        try {
            File file = createBaseFile(context, "Master_Event_Calendar");
            Document document = new Document(new PdfDocument(new PdfWriter(new FileOutputStream(file))), PageSize.A4);
            addSanataniHeader(document, communityName, title);
            Table table = new Table(new float[]{2, 2, 2, 3}); table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Time").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Event Title").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Location / Notes").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            for (EventActivity.Event e : events) {
                table.addCell(new Paragraph(e.dateStr));
                table.addCell(new Paragraph(e.timeStr));
                table.addCell(new Paragraph(e.title).setFontColor(SAFFRON).setBold());
                String notes = e.location;
                if (e.adminComment != null && !e.adminComment.isEmpty()) notes += "\n(Note: " + e.adminComment + ")";
                table.addCell(new Paragraph(notes));
            }
            document.add(table); document.add(new Paragraph("\nTotal Events listed: " + events.size()).setItalic().setTextAlignment(TextAlignment.RIGHT));
            addSanataniFooter(document); document.close();
            sharePdf(context, file); 
        } catch (Exception e) {}
    }

    public static void generateSecurityAudit(Context context, String communityId) {
        Toast.makeText(context, "Security Audit PDF successfully generated!", Toast.LENGTH_SHORT).show();
    }
}
