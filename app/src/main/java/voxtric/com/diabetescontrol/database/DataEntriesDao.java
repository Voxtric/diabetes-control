package voxtric.com.diabetescontrol.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface DataEntriesDao
{

  @Query("SELECT * FROM data_entries ORDER BY actual_timestamp DESC LIMIT :limit")
  List<DataEntry> getPreviousEntries(int limit);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp IS :timeStamp")
  List<DataEntry> getEntry(long timeStamp);



  @Query("SELECT * FROM data_entries WHERE actual_timestamp < :before AND insulin_name LIKE :insulinName ORDER BY actual_timestamp DESC LIMIT 1")
  List<DataEntry> findPreviousEntryWithInsulinName(long before, String insulinName);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp < :before AND insulin_dose LIKE :insulinDose ORDER BY actual_timestamp DESC LIMIT 1")
  List<DataEntry> findPreviousEntryWithInsulinDose(long before, String insulinDose);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp < :before AND blood_glucose_level IS :bloodGlucoseLevel ORDER BY actual_timestamp DESC LIMIT 1")
  List<DataEntry> findPreviousEntryWithBloodGlucoseLevel(long before, float bloodGlucoseLevel);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp < :before AND food_eaten LIKE :foodEaten ORDER BY actual_timestamp DESC LIMIT 1")
  List<DataEntry> findPreviousEntryWithFoodEaten(long before, String foodEaten);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp < :before AND additional_notes LIKE :additionalNotes ORDER BY actual_timestamp DESC LIMIT 1")
  List<DataEntry> findPreviousEntryWithAdditionalNotes(long before, String additionalNotes);



  @Query("SELECT * FROM data_entries WHERE actual_timestamp > :after AND insulin_name LIKE :insulinName ORDER BY actual_timestamp ASC LIMIT 1")
  List<DataEntry> findNextEntryWithInsulinName(long after, String insulinName);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp > :after AND insulin_dose LIKE :insulinDose ORDER BY actual_timestamp ASC LIMIT 1")
  List<DataEntry> findNextEntryWithInsulinDose(long after, String insulinDose);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp > :after AND blood_glucose_level IS :bloodGlucoseLevel ORDER BY actual_timestamp ASC LIMIT 1")
  List<DataEntry> findNextEntryWithBloodGlucoseLevel(long after, float bloodGlucoseLevel);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp > :after AND food_eaten LIKE :foodEaten ORDER BY actual_timestamp ASC LIMIT 1")
  List<DataEntry> findNextEntryWithFoodEaten(long after, String foodEaten);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp > :after AND additional_notes LIKE :additionalNotes ORDER BY actual_timestamp ASC LIMIT 1")
  List<DataEntry> findNextEntryWithAdditionalNotes(long after, String additionalNotes);



  @Query("SELECT * FROM data_entries WHERE day_timestamp IS :dayTimeStamp AND event IS :event")
  DataEntry findOverlapping(long dayTimeStamp, String event);



  @Query("SELECT * FROM data_entries WHERE actual_timestamp < :timeStamp AND event IS :event AND actual_timestamp IS NOT :notTimeStamp ORDER BY actual_timestamp DESC LIMIT 1")
  DataEntry findFirstBefore(long timeStamp, String event, long notTimeStamp);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp < :timeStamp AND event IS :event ORDER BY actual_timestamp DESC LIMIT 1")
  DataEntry findFirstBefore(long timeStamp, String event);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp < :timeStamp ORDER BY actual_timestamp DESC LIMIT 1")
  DataEntry findFirstBefore(long timeStamp);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp > :timeStamp ORDER BY actual_timestamp ASC LIMIT 1")
  DataEntry findFirstAfter(long timeStamp);



  @Query("SELECT * FROM data_entries WHERE actual_timestamp >= :startTimeStamp AND actual_timestamp <= :endTimeStamp ORDER BY actual_timestamp DESC")
  List<DataEntry> findAllBetween(long startTimeStamp, long endTimeStamp);



  @Delete
  void delete(DataEntry entry);

  @Insert
  void insert(DataEntry entry);
}
