package com.voxtric.diabetescontrol.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "target_changes", primaryKeys = {"timestamp"})
public class TargetChange
{
  @ColumnInfo(name = "timestamp")
  public long timestamp;

  @ColumnInfo(name = "pre_meal_lower")
  public float preMealLower;

  @ColumnInfo(name = "pre_meal_upper")
  public float preMealUpper;

  @ColumnInfo(name = "post_meal_lower")
  public float postMealLower;

  @ColumnInfo(name = "post_meal_upper")
  public float postMealUpper;
}
