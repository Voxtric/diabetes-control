package voxtric.com.diabetescontrol.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "events")
public class Event
{
  @PrimaryKey(autoGenerate = true)
  public int id;

  @ColumnInfo(name = "name")
  public String name;

  @ColumnInfo(name = "time_in_day")
  public long timeInDay;
}
