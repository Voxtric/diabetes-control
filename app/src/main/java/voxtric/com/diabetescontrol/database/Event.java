package voxtric.com.diabetescontrol.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class Event
{
  @PrimaryKey(autoGenerate = true)
  public int id;

  @ColumnInfo(name = "name")
  public String name;

  @ColumnInfo(name = "time_in_day")
  public long timeInDay;

  @ColumnInfo(name = "order")
  public int order;
}
