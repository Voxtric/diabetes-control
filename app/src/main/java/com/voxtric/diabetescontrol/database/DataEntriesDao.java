package com.voxtric.diabetescontrol.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DataEntriesDao
{

  @Query("SELECT * FROM data_entries WHERE actual_timestamp < :timestamp ORDER BY actual_timestamp DESC LIMIT :limit")
  List<DataEntry> getPreviousEntries(long timestamp, int limit);



  @Query("SELECT * FROM data_entries WHERE actual_timestamp < :before AND blood_glucose_level IS :bloodGlucoseLevel ORDER BY actual_timestamp DESC LIMIT 1")
  DataEntry findPreviousEntryWithBloodGlucoseLevel(long before, float bloodGlucoseLevel);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp < :before AND insulin_name LIKE :insulinName ORDER BY actual_timestamp DESC LIMIT 1")
  DataEntry findPreviousEntryWithInsulinName(long before, String insulinName);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp < :before AND insulin_dose IS :insulinDose ORDER BY actual_timestamp DESC LIMIT 1")
  DataEntry findPreviousEntryWithInsulinDose(long before, int insulinDose);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp < :before AND additional_notes LIKE :additionalNotes ORDER BY actual_timestamp DESC LIMIT 1")
  DataEntry findPreviousEntryWithAdditionalNotes(long before, String additionalNotes);



  @Query("SELECT * FROM data_entries WHERE actual_timestamp > :after AND blood_glucose_level IS :bloodGlucoseLevel ORDER BY actual_timestamp ASC LIMIT 1")
  DataEntry findNextEntryWithBloodGlucoseLevel(long after, float bloodGlucoseLevel);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp > :after AND insulin_name LIKE :insulinName ORDER BY actual_timestamp ASC LIMIT 1")
  DataEntry findNextEntryWithInsulinName(long after, String insulinName);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp > :after AND insulin_dose LIKE :insulinDose ORDER BY actual_timestamp ASC LIMIT 1")
  DataEntry findNextEntryWithInsulinDose(long after, int insulinDose);

  @Query("SELECT * FROM data_entries WHERE actual_timestamp > :after AND additional_notes LIKE :additionalNotes ORDER BY actual_timestamp ASC LIMIT 1")
  DataEntry findNextEntryWithAdditionalNotes(long after, String additionalNotes);



  @Query("SELECT * FROM data_entries WHERE actual_timestamp IS :timeStamp")
  DataEntry getEntry(long timeStamp);

  @Query("SELECT * FROM data_entries WHERE day_timestamp IS :dayTimeStamp AND event IS :event")
  DataEntry findOverlapping(long dayTimeStamp, String event);



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



  @Query("SELECT AVG(blood_glucose_level) FROM data_entries WHERE actual_timestamp >= :startTimestamp AND actual_timestamp <= :endTimestamp")
  float getAverageBloodGlucoseLevel(long startTimestamp, long endTimestamp);

  @Query("SELECT MIN(blood_glucose_level) FROM data_entries WHERE actual_timestamp >= :startTimestamp AND actual_timestamp <= :endTimestamp")
  float getMinBloodGlucoseLevel(long startTimestamp, long endTimestamp);

  @Query("SELECT MAX(blood_glucose_level) FROM data_entries WHERE actual_timestamp >= :startTimestamp AND actual_timestamp <= :endTimestamp")
  float getMaxBloodGlucoseLevel(long startTimestamp, long endTimestamp);

  @Query("SELECT MAX(blood_glucose_level) FROM data_entries")
  float getMaxBloodGlucoseLevel();
}
