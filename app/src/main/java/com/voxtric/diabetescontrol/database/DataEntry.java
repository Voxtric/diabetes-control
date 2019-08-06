package com.voxtric.diabetescontrol.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "data_entries", primaryKeys = {"actual_timestamp"})
public class DataEntry
{
  @ColumnInfo(name = "actual_timestamp")
  public long actualTimestamp;

  @ColumnInfo(name = "day_timestamp")
  public long dayTimeStamp;

  @ColumnInfo(name = "event")
  public String event;

  @ColumnInfo(name = "insulin_name")
  public String insulinName;

  @ColumnInfo(name = "insulin_dose")
  public int insulinDose;

  @ColumnInfo(name = "blood_glucose_level")
  public float bloodGlucoseLevel;

  @ColumnInfo(name = "additional_notes")
  public String additionalNotes;
}
