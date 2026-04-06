package org.shda;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CsvExportService {

    public static void exportDonationsToCsv(Context context, String communityName, List<TransactionActivity.GroupedDonation> groupedDonations) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String fileName = communityName.replaceAll("[^a-zA-Z0-9]", "") + "_SmartChanda_" + new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()) + ".csv";
            File file = new File(path, fileName);
            
            FileWriter writer = new FileWriter(file);
            writer.append("Donor Name,Total Donated (BDT),Number of Contributions,Last Contribution Date\n");
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            for (TransactionActivity.GroupedDonation gd : groupedDonations) {
                writer.append("\"").append(gd.displayName).append("\",")
                      .append(String.valueOf(gd.totalDonated)).append(",")
                      .append(String.valueOf(gd.history.size())).append(",")
                      .append(sdf.format(new Date(gd.lastUpdated))).append("\n");
            }
            
            writer.flush(); writer.close();
            shareCsvFile(context, file, "Smart Chanda Excel Report");
        } catch (Exception e) {
            Toast.makeText(context, "Failed to generate Excel file", Toast.LENGTH_SHORT).show();
        }
    }

    private static void shareCsvFile(Context context, File file, String subject) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "Open Excel File Via..."));
    }
}
