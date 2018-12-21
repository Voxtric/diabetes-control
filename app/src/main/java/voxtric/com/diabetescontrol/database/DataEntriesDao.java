package voxtric.com.diabetescontrol.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface DataEntriesDao
{

    @Query("SELECT * FROM data_entries ORDER BY time_stamp DESC LIMIT :limit")
    List<DataEntry> getPreviousEntries(int limit);

    @Query("SELECT * FROM data_entries WHERE time_stamp IS :timeStamp")
    List<DataEntry> getEntry(long timeStamp);



    @Query("SELECT * FROM data_entries WHERE time_stamp < :before AND insulin_name LIKE :insulinName ORDER BY time_stamp DESC LIMIT 1")
    List<DataEntry> findPreviousEntryWithInsulinName(long before, String insulinName);

    @Query("SELECT * FROM data_entries WHERE time_stamp < :before AND insulin_dose LIKE :insulinDose ORDER BY time_stamp DESC LIMIT 1")
    List<DataEntry> findPreviousEntryWithInsulinDose(long before, String insulinDose);

    @Query("SELECT * FROM data_entries WHERE time_stamp < :before AND blood_glucose_level IS :bloodGlucoseLevel ORDER BY time_stamp DESC LIMIT 1")
    List<DataEntry> findPreviousEntryWithBloodGlucoseLevel(long before, float bloodGlucoseLevel);

    @Query("SELECT * FROM data_entries WHERE time_stamp < :before AND food_eaten LIKE :foodEaten ORDER BY time_stamp DESC LIMIT 1")
    List<DataEntry> findPreviousEntryWithFoodEaten(long before, String foodEaten);

    @Query("SELECT * FROM data_entries WHERE time_stamp < :before AND additional_notes LIKE :additionalNotes ORDER BY time_stamp DESC LIMIT 1")
    List<DataEntry> findPreviousEntryWithAdditionalNotes(long before, String additionalNotes);



    @Query("SELECT * FROM data_entries WHERE time_stamp > :after AND insulin_name LIKE :insulinName ORDER BY time_stamp ASC LIMIT 1")
    List<DataEntry> findNextEntryWithInsulinName(long after, String insulinName);

    @Query("SELECT * FROM data_entries WHERE time_stamp > :after AND insulin_dose LIKE :insulinDose ORDER BY time_stamp ASC LIMIT 1")
    List<DataEntry> findNextEntryWithInsulinDose(long after, String insulinDose);

    @Query("SELECT * FROM data_entries WHERE time_stamp > :after AND blood_glucose_level IS :bloodGlucoseLevel ORDER BY time_stamp ASC LIMIT 1")
    List<DataEntry> findNextEntryWithBloodGlucoseLevel(long after, float bloodGlucoseLevel);

    @Query("SELECT * FROM data_entries WHERE time_stamp > :after AND food_eaten LIKE :foodEaten ORDER BY time_stamp ASC LIMIT 1")
    List<DataEntry> findNextEntryWithFoodEaten(long after, String foodEaten);

    @Query("SELECT * FROM data_entries WHERE time_stamp > :after AND additional_notes LIKE :additionalNotes ORDER BY time_stamp ASC LIMIT 1")
    List<DataEntry> findNextEntryWithAdditionalNotes(long after, String additionalNotes);



    @Query("SELECT * FROM data_entries WHERE time_stamp < :timeStamp AND event IS :event AND time_stamp IS NOT :notTimeStamp ORDER BY time_stamp DESC LIMIT 1")
    List<DataEntry> findFirstBefore(long timeStamp, String event, long notTimeStamp);

    @Query("SELECT * FROM data_entries WHERE time_stamp < :timeStamp AND event IS :event ORDER BY time_stamp DESC LIMIT 1")
    List<DataEntry> findFirstBefore(long timeStamp, String event);

    @Query("SELECT * FROM data_entries WHERE time_stamp < :timeStamp ORDER BY time_stamp DESC LIMIT 1")
    List<DataEntry> findFirstBefore(long timeStamp);

    @Query("SELECT * FROM data_entries WHERE time_stamp > :timeStamp ORDER BY time_stamp ASC LIMIT 1")
    List<DataEntry> findFirstAfter(long timeStamp);

    @Query("SELECT * FROM data_entries WHERE time_stamp >= :startTimeStamp AND time_stamp <= :endTimeStamp ORDER BY time_stamp DESC")
    List<DataEntry> findAllBetween(long startTimeStamp, long endTimeStamp);



    @Delete
    void delete(DataEntry entry);

    @Insert
    void insert(DataEntry entry);
}
