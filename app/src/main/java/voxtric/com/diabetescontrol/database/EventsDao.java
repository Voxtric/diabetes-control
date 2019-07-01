package voxtric.com.diabetescontrol.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface EventsDao
{
  @Query("SELECT * FROM events ORDER BY \"order\" ASC")
  List<Event> getEvents();

  @Query("UPDATE events SET 'order' = 'order' - :amount WHERE 'order' > :greaterThan")
  void shuffleOrders(int amount, int greaterThan);

  @Update
  void updateEvent(Event event);

  @Delete
  void deleteEvent(Event event);

  @Insert
  void insert(Event event);
}
