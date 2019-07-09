package voxtric.com.diabetescontrol.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface EventsDao
{
  @Query("SELECT * FROM events ORDER BY \"order\" ASC")
  List<Event> getEvents();

  @Query("SELECT * FROM events ORDER BY time_in_day ASC")
  List<Event> getEventsTimeOrdered();

  @Query("UPDATE events SET 'order' = \"order\" + :amount WHERE \"order\" > :greaterThan")
  void shuffleOrders(int amount, int greaterThan);

  @Query("SELECT count(*) FROM events")
  int countEvents();

  @Query("SELECT * FROM events WHERE name IS :name")
  Event getEvent(String name);

  @Update
  void updateEvent(Event event);

  @Delete
  void deleteEvent(Event event);

  @Insert
  void insert(Event event);
}
