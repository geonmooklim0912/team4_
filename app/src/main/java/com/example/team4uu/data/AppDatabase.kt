package com.example.team4uu.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 테스트 계정(test/test2) 우회 로그인을 없애면서, 그 계정들 밑에 있던 기존 친구 데이터를
// 실제 계정(geonmooklim0912)으로 옮겨줌. 스키마 자체는 안 바뀌고 데이터만 옮기는 마이그레이션.
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "UPDATE friends SET ownerUsername = 'geonmooklim0912' WHERE ownerUsername IN ('test', 'test2')"
        )
    }
}

@Database(entities = [Friend::class, MealSession::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao
    abstract fun mealSessionDao(): MealSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "team4uu.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    // 아직 정식 출시 전이라, 위에 명시적으로 작성한 경로 이외의 스키마 변경은
                    // 마이그레이션 대신 로컬 DB를 새로 만듦. 실 사용자 데이터가 쌓이기 시작하면
                    // 이 방식 대신 모든 버전 변경에 정식 Migration을 작성해야 함.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}