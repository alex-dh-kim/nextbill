package dev.lioracode.nextbill.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 행 하나(id = 0)만 씁니다. 데이터 전체 교체·변경 때마다 revision이 오릅니다. */
@Entity(tableName = "dataset_meta")
data class DatasetMetaEntity(@PrimaryKey val id: Int = 0, val revision: Long)
