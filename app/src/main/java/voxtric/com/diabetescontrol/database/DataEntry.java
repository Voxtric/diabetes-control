package voxtric.com.diabetescontrol.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;

@Entity(tableName = "data_entries", primaryKeys = {"time_stamp"})
public class DataEntry
{
  @ColumnInfo(name = "time_stamp")
  public long timeStamp;

  @ColumnInfo(name = "event")
  public String event;

  @ColumnInfo(name = "insulin_name")
  public String insulinName;

  @ColumnInfo(name = "insulin_dose")
  public String insulinDose;

  @ColumnInfo(name = "blood_glucose_level")
  public float bloodGlucoseLevel;

  @ColumnInfo(name = "food_eaten")
  public String foodEaten;

  @ColumnInfo(name = "additional_notes")
  public String additionalNotes;
}
