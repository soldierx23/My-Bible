/*
 * Copyright (c) 2023 Martin Denham, Tuomas Airaksinen and the AndBible contributors.
 *
 * This file is part of AndBible: Bible Study (http://github.com/AndBible/and-bible).
 *
 * AndBible is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * AndBible is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with AndBible.
 * If not, see http://www.gnu.org/licenses/.
 */

package net.bible.android.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.bible.android.database.migrations.Migration
import org.jdom2.Element

@Entity
class EpubHtmlToFrag(
    @PrimaryKey val htmlId: String, // contains epub doc id (and possibly #htmlId)
    val fragId: Long,
)

@Entity
class EpubFragment(
    val originalId: String,
    val ordinalStart: Int,
    val ordinalEnd: Int,
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
) {
    @Ignore var element: Element? = null
    val fragFileName: String get() = id.toString().padStart(3, '0') + ".xhtml.gz"
    override fun toString(): String {
        return "$id $originalId $ordinalStart $ordinalEnd"
    }
}

@Entity(indices = [Index("origId")])
class StyleSheet(
    val origId: String,
    val styleSheetFile: String,
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
)

@Dao
interface EpubDao {
    @Insert fun insert(vararg items: EpubFragment): List<Long>
    @Insert fun insert(vararg items: StyleSheet): List<Long>
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(vararg items: EpubHtmlToFrag)

    @Query(
        "SELECT f.* FROM EpubFragment f " +
        "JOIN EpubHtmlToFrag e2f ON e2f.fragId = f.id " +
        "WHERE e2f.htmlId=:htmlId"
    )
    fun getFragment(htmlId: String): EpubFragment

    @Query("SELECT * from EpubFragment WHERE id=:id")
    fun getFragment(id: Long): EpubFragment?

    @Query("SELECT f.* FROM EpubFragment f")
    fun fragments(): List<EpubFragment>

    @Query("SELECT * FROM StyleSheet WHERE origId=:origId")
    fun styleSheets(origId: String): List<StyleSheet>
}


const val EPUB_DATABASE_VERSION = 1

val epubMigrations = arrayOf<Migration>()

@Database(
    entities = [
        EpubHtmlToFrag::class,
        EpubFragment::class,
        StyleSheet::class,
    ],
    version = EPUB_DATABASE_VERSION
)
@TypeConverters(Converters::class)
abstract class EpubDatabase: RoomDatabase() {
    abstract fun epubDao(): EpubDao
}
