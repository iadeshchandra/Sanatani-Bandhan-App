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
            // ✨ FIX: Using generic Color type for the ternary operator!
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
            table.addCell(new Cell().add(new Paragraph("SB-ID
