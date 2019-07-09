package voxtric.com.diabetescontrol.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {DataEntry.class, Event.class, TargetChange.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase
{
  public abstract DataEntriesDao dataEntriesDao();

  public abstract EventsDao eventsDao();

  public abstract TargetChangesDao targetChangesDao();
}
