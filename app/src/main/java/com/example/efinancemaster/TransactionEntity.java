package com.example.efinancemaster;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class TransactionEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "transaction_id")
    public String transactionId;

    @ColumnInfo(name = "amount")
    public double amount;

    @ColumnInfo(name = "reference_number")
    public String referenceNumber;

    @ColumnInfo(name = "auth_number")
    public String authNumber;
}
