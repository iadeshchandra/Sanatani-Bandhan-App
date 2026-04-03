package org.shda;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.io.File;

public class PdfReportService {
    public static void generateReport(Context context) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, "Sanatani_Bandhan_Report.pdf");

            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Sanatani Bandhan Community Report"));
            document.add(new Paragraph("--------------------------------"));
            document.add(new Paragraph("Generated from Secure Admin Portal."));
            
            document.close();
            Toast.makeText(context, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
