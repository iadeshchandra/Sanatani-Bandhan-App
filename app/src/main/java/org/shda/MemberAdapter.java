package org.shda;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private Context context;
    private List<Member> memberList;
    private String communityId;
    private String userRole;

    public MemberAdapter(Context context, List<Member> memberList, String communityId, String userRole) {
        this.context = context;
        this.memberList = memberList;
        this.communityId = communityId;
        this.userRole = userRole;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member member = memberList.get(position);
        holder.tvName.setText(member.name);
        holder.tvPhone.setText("📞 " + member.phone);
        holder.tvGotra.setText("Gotra: " + (member.gotra != null && !member.gotra.isEmpty() ? member.gotra : "Not specified"));

        // WhatsApp Click Action
        holder.btnWhatsApp.setOnClickListener(v -> {
            try {
                String phoneStr = member.phone;
                if (!phoneStr.startsWith("+88")) phoneStr = "+88" + phoneStr;
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://wa.me/" + phoneStr));
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            }
        });

        // Delete Click Action (Only Admins can delete)
        if ("ADMIN".equals(userRole)) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                    .setTitle("Remove Devotee")
                    .setMessage("Are you sure you want to remove " + member.name + "?")
                    .setPositiveButton("Remove", (dialog, which) -> {
                        FirebaseDatabase.getInstance().getReference()
                            .child("communities").child(communityId)
                            .child("members").child(member.id).removeValue();
                        Toast.makeText(context, "Devotee removed", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvGotra;
        ImageButton btnWhatsApp, btnDelete;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvPhone = itemView.findViewById(R.id.tvMemberPhone);
            tvGotra = itemView.findViewById(R.id.tvMemberGotra);
            btnWhatsApp = itemView.findViewById(R.id.btnMemberWhatsApp);
            btnDelete = itemView.findViewById(R.id.btnMemberDelete);
        }
    }

    // Static Inner Class for Data Model
    public static class Member {
        public String id, name, phone, gotra;
        public long timestamp;
        public Member() {} 
        public Member(String id, String name, String phone, String gotra, long timestamp) {
            this.id = id; this.name = name; this.phone = phone; this.gotra = gotra; this.timestamp = timestamp;
        }
    }
}
