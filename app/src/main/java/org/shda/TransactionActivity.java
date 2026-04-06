package org.shda;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class TransactionActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout transactionsContainer;
    private TextView tvTotalChanda;
    private EditText inputSearch;
    private Spinner spinnerSort;

    private float totalChandaValue = 0f;
    private boolean isAdminOrManager;

    // Auto-complete lists
    private List<String> memberSearchList = new ArrayList<>();
    private HashMap<String, String> memberIdMap = new HashMap<>(); // Maps "Name (ID)" -> "ID"

    private HashMap<String, GroupedDonation> groupedMap = new HashMap<>();
    private List<GroupedDonation> displayList = new ArrayList<>();
    private String currentSort = "Recent First";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        transactionsContainer = findViewById(R.id.transactionsContainer);
        tvTotalChanda = findViewById(R.id.tvTotalChanda);
        inputSearch = findViewById(R.id.inputSearch);
        spinnerSort = findViewById(R.id.spinnerSort);

        if (session.getCommunityId() == null) { finish(); return; }
        isAdminOrManager = "ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole());

        setupRBAC();
        setupSearchAndSort();
        fetchMembersForAutoComplete();
        loadTransactions();
    }

    private void setupRBAC() {
        View btnAddDonation = findViewById(R.id.btnAddDonation);
        if (btnAddDonation != null) {
            if (!isAdminOrManager) btnAddDonation.setVisibility(View.GONE);
            else btnAddDonation.setOnClickListener(v -> showDonationDialog());
        }
        findViewById(R.id.btnGenerateReport).setOnClickListener(v -> triggerMasterReportGeneration());
    }

    private void setupSearchAndSort() {
        String[] sortOptions = {"Recent First", "Oldest First", "Highest Amount", "Name (A-Z)"};
        spinnerSort.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sortOptions));

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSort = sortOptions[position]; applyFilterAndSort();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilterAndSort(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchMembersForAutoComplete() {
        db.child("communities").child(session.getCommunityId()).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                memberSearchList.clear(); memberIdMap.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member m = data.getValue(Member.class);
                    if (m != null) {
                        String displayText = m.name + " (" + m.id + ")";
                        memberSearchList.add(displayText);
                        memberIdMap.put(displayText, m.id); // Save ID for notifications!
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadTransactions() {
        db.child("communities").child(session.getCommunityId()).child("logs").child("Donation")
          .addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupedMap.clear(); totalChandaValue = 0f;

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        SingleDonation sd = data.getValue(SingleDonation.class);
                        if (sd != null && sd.name != null) {
                            totalChandaValue += sd.amount;
                            String groupKey = sd.name.toLowerCase().trim();

                            if (!groupedMap.containsKey(groupKey)) {
                                GroupedDonation gd = new GroupedDonation();
                                gd.displayName = sd.name; groupedMap.put(groupKey, gd);
                            }

                            GroupedDonation gd = groupedMap.get(groupKey);
                            gd.totalDonated += sd.amount;
                            if (sd.timestamp > gd.lastUpdated) gd.lastUpdated = sd.timestamp;
                            gd.history.add(sd);
                        }
                    } catch (Exception e) {}
                }
                tvTotalChanda.setText("Total Smart Chanda: ৳" + totalChandaValue);
                applyFilterAndSort();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilterAndSort() {
        displayList.clear();
        String query = inputSearch.getText().toString().toLowerCase().trim();

        for (GroupedDonation gd : groupedMap.values()) {
            if (gd.displayName.toLowerCase().contains(query)) displayList.add(gd);
        }

        Collections.sort(displayList, (a, b) -> {
            if (currentSort.equals("Recent First")) return Long.compare(b.lastUpdated, a.lastUpdated);
            if (currentSort.equals("Oldest First")) return Long.compare(a.lastUpdated, b.lastUpdated);
            if (currentSort.equals("Highest Amount")) return Float.compare(b.totalDonated, a.totalDonated);
            return a.displayName.compareToIgnoreCase(b.displayName);
        });
        renderCards();
    }

    private void renderCards() {
        transactionsContainer.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        for (GroupedDonation gd : displayList) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_transaction, transactionsContainer, false);
            ((TextView) view.findViewById(R.id.tvTransName)).setText("👤 " + gd.displayName);
            ((TextView) view.findViewById(R.id.tvTransAmount)).setText("Total Donated: ৳" + gd.totalDonated);
            ((TextView) view.findViewById(R.id.tvTransDate)).setText("Last Chanda: " + sdf.format(new Date(gd.lastUpdated)));
            ((TextView) view.findViewById(R.id.tvTransNote)).setText("Total Records: " + gd.history.size() + "\n👉 Tap to download Full Statement");

            view.setOnClickListener(v -> PdfReportService.generateDonorStatement(this, session.getCommunityName(), gd));
            transactionsContainer.addView(view);
        }
    }

    private void showDonationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record New Chanda");

        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 0);

        final AutoCompleteTextView inputName = new AutoCompleteTextView(this); inputName.setHint("Donor Name / Member ID");
        inputName.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, memberSearchList)); inputName.setThreshold(1);
        
        final EditText inputAmount = new EditText(this); inputAmount.setHint("Amount (৳)"); inputAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final EditText inputNote = new EditText(this); inputNote.setHint("Purpose / Gotra / Note");

        layout.addView(inputName); layout.addView(inputAmount); layout.addView(inputNote);
        builder.setView(layout);

        builder.setPositiveButton("RECORD", (dialog, which) -> {
            String nameInput = inputName.getText().toString().trim();
            String amtStr = inputAmount.getText().toString().trim();
            if (nameInput.isEmpty() || amtStr.isEmpty()) { Toast.makeText(this, "Name and Amount required", Toast.LENGTH_SHORT).show(); return; }

            try {
                float amt = Float.parseFloat(amtStr);
                String transId = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").push().getKey();
                String strictSignature = "ADMIN".equals(session.getRole()) ? "Super Admin - " + session.getUserName() : "Manager - " + session.getUserName();
                long ts = System.currentTimeMillis();

                SingleDonation sd = new SingleDonation(transId, nameInput, amt, inputNote.getText().toString().trim(), "", "", session.getUserName(), ts, strictSignature);
                db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").child(transId).setValue(sd);
                
                // ✨ NOTIFICATION ENGINE: Send Alert to the specific member!
                if (memberIdMap.containsKey(nameInput)) {
                    String targetMemberId = memberIdMap.get(nameInput);
                    String notifId = db.child("communities").child(session.getCommunityId()).child("notifications").child(targetMemberId).push().getKey();
                    
                    AppNotification notif = new AppNotification(notifId, "Chanda Received! 🙏", "We received your donation of ৳" + amt + ". May Ishvara bless you.", ts, "CHANDA");
                    db.child("communities").child(session.getCommunityId()).child("notifications").child(targetMemberId).child(notifId).setValue(notif);
                }

                updateMemberTotal(nameInput, amt);
                PdfReportService.generateDonorReceipt(this, session.getCommunityName(), nameInput, amt, inputNote.getText().toString(), new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date()));
                Toast.makeText(this, "Chanda Recorded & Receipt Generated!", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {}
        });
        builder.setNegativeButton("CANCEL", null); builder.show();
    }

    private void updateMemberTotal(String displayName, float newAmount) {
        String targetMemberId = memberIdMap.get(displayName);
        if (targetMemberId != null) {
            DatabaseReference memRef = db.child("communities").child(session.getCommunityId()).child("members").child(targetMemberId);
            memRef.child("totalDonated").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    float currentTotal = snapshot.getValue(Float.class) != null ? snapshot.getValue(Float.class) : 0f;
                    memRef.child("totalDonated").setValue(currentTotal + newAmount);
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void triggerMasterReportGeneration() {
       // Code hidden for brevity - generates PDF based on date
       Toast.makeText(this, "Use Dashboard to generate Master PDF", Toast.LENGTH_SHORT).show();
    }

    public static class SingleDonation {
        public String id, name, note, phone, address, collectedBy, loggedBy, systemSignature;
        public float amount; public long timestamp;
        public SingleDonation() {}
        public SingleDonation(String id, String name, float amount, String note, String phone, String address, String collectedBy, long timestamp, String systemSignature) {
            this.id = id; this.name = name; this.amount = amount; this.note = note; this.phone = phone; this.address = address; this.collectedBy = collectedBy; this.timestamp = timestamp; this.systemSignature = systemSignature; this.loggedBy = "System";
        }
    }

    public static class GroupedDonation {
        public String displayName; public float totalDonated = 0f; public long lastUpdated = 0L;
        public List<SingleDonation> history = new ArrayList<>();
    }
}
