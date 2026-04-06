package org.shda;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class TransactionActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout transactionsContainer;
    private EditText inputSearch;
    private TextView tvTotalDonations;
    
    private List<SingleDonation> fullDonationList = new ArrayList<>();
    private List<Member> autocompleteMembers = new ArrayList<>();
    private float totalCollected = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getCommunityId() == null) { finish(); return; }

        transactionsContainer = findViewById(R.id.transactionsContainer);
        inputSearch = findViewById(R.id.inputSearch);
        tvTotalDonations = findViewById(R.id.tvTotalDonations);

        View btnAdd = findViewById(R.id.btnAddTransaction);
        if ("ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole())) {
            btnAdd.setVisibility(View.VISIBLE);
            btnAdd.setOnClickListener(v -> showAddDonationDialog());
        } else {
            btnAdd.setVisibility(View.GONE);
        }

        loadMembersForAutocomplete();
        loadDonations();
        setupSearch();
    }

    private void loadMembersForAutocomplete() {
        db.child("communities").child(session.getCommunityId()).child("members").keepSynced(true);
        db.child("communities").child(session.getCommunityId()).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                autocompleteMembers.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member m = data.getValue(Member.class);
                    if (m != null) autocompleteMembers.add(m);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadDonations() {
        DatabaseReference donationsRef = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation");
        donationsRef.keepSynced(true);
        donationsRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullDonationList.clear(); totalCollected = 0f;
                for (DataSnapshot data : snapshot.getChildren()) {
                    SingleDonation sd = data.getValue(SingleDonation.class);
                    if (sd != null) { fullDonationList.add(sd); totalCollected += sd.amount; }
                }
                tvTotalDonations.setText("Total Collected: ৳" + totalCollected);
                processAndRenderList(fullDonationList);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupSearch() {
        if (inputSearch == null) return;
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                List<SingleDonation> filtered = new ArrayList<>();
                for (SingleDonation sd : fullDonationList) {
                    if ((sd.name != null && sd.name.toLowerCase().contains(query)) || 
                        (sd.note != null && sd.note.toLowerCase().contains(query))) {
                        filtered.add(sd);
                    }
                }
                processAndRenderList(filtered);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void processAndRenderList(List<SingleDonation> rawList) {
        HashMap<String, GroupedDonation> groupedMap = new HashMap<>();
        for (SingleDonation sd : rawList) {
            String key = sd.name.trim().toLowerCase();
            if (!groupedMap.containsKey(key)) groupedMap.put(key, new GroupedDonation(sd.name));
            groupedMap.get(key).addDonation(sd);
        }
        
        List<GroupedDonation> groupedList = new ArrayList<>(groupedMap.values());
        Collections.sort(groupedList, (a, b) -> Float.compare(b.totalDonated, a.totalDonated));
        
        transactionsContainer.removeAllViews();
        for (GroupedDonation gd : groupedList) {
            try {
                View view = LayoutInflater.from(this).inflate(R.layout.item_transaction, transactionsContainer, false);
                ((TextView) view.findViewById(R.id.tvTransName)).setText(gd.displayName);
                ((TextView) view.findViewById(R.id.tvTransAmount)).setText("Lifetime: ৳" + gd.totalDonated);
                ((TextView) view.findViewById(R.id.tvTransNote)).setText(gd.history.size() + " Contributions recorded");
                
                view.setOnClickListener(v -> PdfReportService.generateDonorStatement(this, session.getCommunityName(), gd));
                transactionsContainer.addView(view);
            } catch (Exception e) {}
        }
    }

    private void showAddDonationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record New Chanda");
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 0);

        // AutoComplete for Members / Guests
        final AutoCompleteTextView inputName = new AutoCompleteTextView(this);
        inputName.setHint("Donor Name / Member ID");
        List<String> suggestions = new ArrayList<>();
        for (Member m : autocompleteMembers) { suggestions.add(m.name); suggestions.add(m.id); }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, suggestions);
        inputName.setAdapter(adapter); inputName.setThreshold(1);

        final EditText inputAmt = new EditText(this); inputAmt.setHint("Amount (৳)"); inputAmt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final EditText inputNote = new EditText(this); inputNote.setHint("Purpose / Gotra / Note");

        layout.addView(inputName); layout.addView(inputAmt); layout.addView(inputNote);
        builder.setView(layout);
        builder.setPositiveButton("RECORD", null); builder.setNegativeButton("CANCEL", null);

        AlertDialog dialog = builder.create(); dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nameRaw = inputName.getText().toString().trim();
            String amtStr = inputAmt.getText().toString().trim();
            String note = inputNote.getText().toString().trim();

            if (nameRaw.isEmpty() || amtStr.isEmpty()) { Toast.makeText(this, "Name and Amount required", Toast.LENGTH_SHORT).show(); return; }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Saving...");
            
            float amt = Float.parseFloat(amtStr);
            String finalName = nameRaw + " [Guest]";
            String matchedId = null;

            for (Member m : autocompleteMembers) {
                if (m.name.equalsIgnoreCase(nameRaw) || m.id.equalsIgnoreCase(nameRaw)) {
                    finalName = m.name + " [Member]";
                    matchedId = m.id; break;
                }
            }

            String transId = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").push().getKey();
            SingleDonation sd = new SingleDonation(transId, finalName, amt, note, "", "", session.getUserName(), System.currentTimeMillis(), session.getRole());
            db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").child(transId).setValue(sd);

            if (matchedId != null) {
                db.child("communities").child(session.getCommunityId()).child("members").child(matchedId).child("totalDonated").setValue(ServerValue.increment(amt));
            }

            Toast.makeText(this, "Chanda Recorded Locally!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    public static class SingleDonation {
        public String id, name, note, method, phone, collectedBy, role; public float amount; public long timestamp;
        public SingleDonation() {}
        public SingleDonation(String id, String name, float amt, String note, String method, String phone, String collectedBy, long ts, String role) {
            this.id = id; this.name = name; this.amount = amt; this.note = note; this.method = method; this.phone = phone; this.collectedBy = collectedBy; this.timestamp = ts; this.role = role;
        }
    }

    public static class GroupedDonation {
        public String displayName; public float totalDonated = 0f; public List<SingleDonation> history = new ArrayList<>();
        public GroupedDonation(String name) { this.displayName = name; }
        public void addDonation(SingleDonation d) { this.history.add(d); this.totalDonated += d.amount; }
    }
}
