package com.example.efinancemaster;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<TransactionEntity> transactions = new ArrayList<>();

    public void setTransactions(List<TransactionEntity> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged(); // Notify the adapter of data change
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_item, parent, false);
        return new TransactionViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionEntity transaction = transactions.get(position);
        holder.amountTextView.setText(String.valueOf(transaction.amount));
        holder.transactionNumberTextView.setText(transaction.transactionId);
        holder.referenceNumberTextView.setText(transaction.referenceNumber);
        holder.authIdTextView.setText(transaction.authNumber);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView amountTextView, transactionNumberTextView, referenceNumberTextView, authIdTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            transactionNumberTextView = itemView.findViewById(R.id.transactionNumberTextView);
            referenceNumberTextView = itemView.findViewById(R.id.referenceNumberTextView);
            authIdTextView = itemView.findViewById(R.id.authIdTextView);
        }
    }
}
