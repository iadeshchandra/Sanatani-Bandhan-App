    private void processTransaction() {
        String amountStr = inputAmount.getText().toString().trim();
        String note = inputNote.getText().toString().trim();
        boolean isMember = radioMember.isChecked();
        
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        float amount = Float.parseFloat(amountStr);
        String nameToSave = "";
        String memberIdToUpdate = null;

        if (isMember) {
            String selection = autoCompleteMember.getText().toString().trim();
            if (selection.isEmpty() || !selection.contains("(") || !selection.contains(")")) {
                Toast.makeText(this, "Please select a valid member from the list", Toast.LENGTH_SHORT).show();
                return;
            }
            nameToSave = selection;
            memberIdToUpdate = selection.substring(selection.indexOf("(") + 1, selection.indexOf(")"));
        } else {
            nameToSave = inputGuestName.getText().toString().trim() + " (Guest)";
            if (nameToSave.equals(" (Guest)")) {
                Toast.makeText(this, "Please enter Guest Name", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Action 1: Save to Global Transaction Logs
        HashMap<String, Object> logData = new HashMap<>();
        logData.put("name", nameToSave);
        logData.put("amount", amount);
        logData.put("note", note);
        logData.put("timestamp", System.currentTimeMillis());
        db.child("logs").child("Donation").push().setValue(logData);

        // Action 2: Update Dashboard Graph
        db.child("finances").child("Donation").get().addOnSuccessListener(snap -> {
            float currentTotal = snap.exists() ? snap.getValue(Float.class) : 0f;
            db.child("finances").child("Donation").setValue(currentTotal + amount);
        });

        // Action 3: Update Member's Personal Lifetime Total
        if (isMember && memberIdToUpdate != null) {
            db.child("members").child(memberIdToUpdate).child("totalDonated").get().addOnSuccessListener(snap -> {
                float currentMemberTotal = snap.exists() ? snap.getValue(Float.class) : 0f;
                db.child("members").child(memberIdToUpdate).child("totalDonated").setValue(currentMemberTotal + amount);
            });
        }

        Toast.makeText(this, "Chanda Recorded! Generating Receipt...", Toast.LENGTH_LONG).show();
        
        // NEW COMMAND: Generate the instant personalized receipt
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault());
        String dateStr = sdf.format(new java.util.Date());
        PdfReportService.generateDonorReceipt(this, nameToSave, amount, note, dateStr);
        
        // Note: Removed finish() here so the app doesn't close before the Share Menu pops up!
    }
