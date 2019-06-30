package voxtric.com.diabetescontrol.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {DataEntry.class, Event.class, TargetChange.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase
{
  public abstract DataEntriesDao dataEntriesDao();

  public abstract EventsDao eventsDao();

  public abstract TargetChangesDao targetChangesDao();
}
