package com.voxtric.diabetescontrol.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface PreferencesDao
{
  @Query("SELECT * FROM preferences WHERE name is :name")
  Preference getPreference(String name);

  @Update
  void update(Preference preference);

  @Insert
  void insert(Preference preference);

  @Delete
  void delete(Preference preference);
}
