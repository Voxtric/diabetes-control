package voxtric.com.diabetescontrol.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {DataEntry.class, Event.class, TargetChange.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase
{
    public abstract DataEntriesDao dataEntriesDao();

    public abstract EventDao eventsDao();

    public abstract TargetChangeDao targetChangesDao();
}
