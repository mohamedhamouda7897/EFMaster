package com.example.efinancemaster;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface  TransactionDao {
    @Query("SELECT * FROM transactions")
    List<TransactionEntity> getAll();

    @Insert
    void insertAll(TransactionEntity... transactions);

    @Delete
    void delete(TransactionEntity transactions);

    @Query("SELECT * FROM transactions WHERE reference_number LIKE :query")
    List<TransactionEntity> searchByQuery(String query);
}
