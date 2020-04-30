package com.voxtric.diabetescontrol.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(tableName = "foods",
    primaryKeys = {"data_entry_timestamp", "name"},
    foreignKeys = @ForeignKey(entity = DataEntry.class, parentColumns = "actual_timestamp", childColumns = "data_entry_timestamp", onDelete = CASCADE))
public class Food
{
  public static final String TAG = "food_item";

  @ColumnInfo(name = "data_entry_timestamp")
  public long dataEntryTimestamp;

  @NonNull
  @ColumnInfo(name = "name")
  public String name = "Null Food";
}
