package voxtric.com.diabetescontrol.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FoodsDao
{
  @Query("SELECT * FROM foods WHERE data_entry_timestamp IS :dataEntryTimestamp")
  List<Food> getFoods(long dataEntryTimestamp);

  @Insert
  void insert(Food food);
}
