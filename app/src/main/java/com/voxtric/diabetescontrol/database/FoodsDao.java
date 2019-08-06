package com.voxtric.diabetescontrol.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FoodsDao
{
  @Query("SELECT * FROM foods WHERE data_entry_timestamp IS :dataEntryTimestamp ORDER BY name ASC")
  List<Food> getFoods(long dataEntryTimestamp);

  @Query("SELECT * FROM foods WHERE data_entry_timestamp < :before AND name LIKE :name ORDER BY data_entry_timestamp DESC LIMIT 1")
  Food getFoodBefore(long before, String name);

  @Query("SELECT * FROM foods WHERE data_entry_timestamp > :after AND name LIKE :name ORDER BY data_entry_timestamp ASC LIMIT 1")
  Food getFoodAfter(long after, String name);

  @Insert
  void insert(Food food);
}
