package com.example.uploadsmsongoogledrive;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.SmsViewHolder> {

    private List<Sms> smsList;

    public SmsAdapter(List<Sms> smsList) {
        this.smsList = smsList;
    }

    @NonNull
    @Override
    public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sms_item, parent, false);
        return new SmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
        Sms sms = smsList.get(position);
        holder.address.setText(sms.getAddress());
        holder.date.setText(sms.getDate());
        holder.body.setText(sms.getBody());
    }

    @Override
    public int getItemCount() {
        return smsList.size();
    }

    static class SmsViewHolder extends RecyclerView.ViewHolder {
        TextView address, date, body;

        public SmsViewHolder(@NonNull View itemView) {
            super(itemView);
            address = itemView.findViewById(R.id.address);
            date = itemView.findViewById(R.id.date);
            body = itemView.findViewById(R.id.body);
        }
    }
}