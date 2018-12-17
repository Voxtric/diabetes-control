package voxtric.com.diabetescontrol.database;

import android.arch.persistence.room.Room;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public abstract class DatabaseActivity extends AppCompatActivity
{
    protected AppDatabase m_database = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        m_database = Room.databaseBuilder(this, AppDatabase.class, "diabetes-control.db").build();
    }

    public AppDatabase getDatabase()
    {
        return m_database;
    }
}
