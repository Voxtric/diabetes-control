package voxtric.com.diabetescontrol.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface EventDao
{
  @Query("SELECT * FROM events ORDER BY time_in_day ASC")
  List<Event> getEvents();

  @Update
  void updateEvent(Event event);

  @Delete
  void deleteEvent(Event event);

  @Insert
  void insert(Event event);
}
