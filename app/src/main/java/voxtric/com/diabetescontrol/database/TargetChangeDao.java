package voxtric.com.diabetescontrol.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

@Dao
public interface TargetChangeDao
{
    @Query("SELECT * FROM target_changes WHERE time_stamp >= :start AND time_stamp <= :end ORDER BY time_stamp DESC LIMIT 1")
    TargetChange findChangeBetween(long start, long end);

    @Query("SELECT * FROM target_changes WHERE time_stamp < :timeStamp ORDER BY time_stamp DESC LIMIT 1")
    TargetChange findFirstBefore(long timeStamp);

    @Insert
    void insert(TargetChange targetChange);
}
