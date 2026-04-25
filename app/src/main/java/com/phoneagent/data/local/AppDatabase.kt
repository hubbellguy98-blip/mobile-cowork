package com.phoneagent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.phoneagent.data.local.dao.ActionLogDao
import com.phoneagent.data.local.dao.ConversationDao
import com.phoneagent.data.local.dao.SessionDao
import com.phoneagent.data.local.entities.ActionLogEntity
import com.phoneagent.data.local.entities.ConversationEntity
import com.phoneagent.data.local.entities.SessionEntity

@Database(
    entities = [ConversationEntity::class, ActionLogEntity::class, SessionEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun actionLogDao(): ActionLogDao
    abstract fun sessionDao(): SessionDao
}
