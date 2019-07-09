package voxtric.com.diabetescontrol.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface TargetChangesDao
{
  @Query("SELECT * FROM target_changes WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC LIMIT 1")
  TargetChange findChangeBetween(long start, long end);

  @Query("SELECT * FROM target_changes WHERE timestamp < :timeStamp ORDER BY timestamp DESC LIMIT 1")
  TargetChange findFirstBefore(long timeStamp);

  @Insert
  void insert(TargetChange targetChange);
}
