package voxtric.com.diabetescontrol.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;

@Entity(tableName = "target_changes", primaryKeys = {"time_stamp"})
public class TargetChange
{
  @ColumnInfo(name = "time_stamp")
  public long timeStamp;

  @ColumnInfo(name = "pre_meal_lower")
  public float preMealLower;

  @ColumnInfo(name = "pre_meal_upper")
  public float preMealUpper;

  @ColumnInfo(name = "post_meal_lower")
  public float postMealLower;

  @ColumnInfo(name = "post_meal_upper")
  public float postMealUpper;
}
