package com.example.studycoroutines.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.studycoroutines.model.SingletonHolder

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object : SingletonHolder<AppDatabase, Context>({
        Room.databaseBuilder(it.applicationContext,
            AppDatabase::class.java, "StudyCoroutinesDatabaseName.db")
            .build()
    })
}