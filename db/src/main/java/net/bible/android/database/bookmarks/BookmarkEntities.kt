/*
 * Copyright (c) 2020-2022 Martin Denham, Tuomas Airaksinen and the AndBible contributors.
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

package net.bible.android.database.bookmarks

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.passage.VerseRange
import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.system.SystemKJVA
import org.crosswire.jsword.versification.system.Versifications
import android.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Ignore
import kotlinx.serialization.Serializable
import net.bible.android.common.toV11n
import net.bible.android.database.IdType
import net.bible.android.misc.OsisFragment
import org.crosswire.jsword.book.basic.AbstractPassageBook
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.NoSuchKeyException
import org.crosswire.jsword.passage.RangedPassage
import java.util.*
import kotlin.math.abs

val KJVA = Versifications.instance().getVersification(SystemKJVA.V11N_NAME)

const val SPEAK_LABEL_NAME = "__SPEAK_LABEL__"
const val UNLABELED_NAME = "__UNLABELED__"

/**
 * How to represent bookmarks
 *
 * @author Martin Denham [mjdenham at gmail dot com]
 */


fun intToColorArray(colorInt: Int): ArrayList<Int> {
    val ar = ArrayList<Int>()
    ar.add(Color.red(colorInt))
    ar.add(Color.green(colorInt))
    ar.add(Color.blue(colorInt))
    ar.add(Color.alpha(colorInt))
    return ar
}

enum class BookmarkStyle(val backgroundColor: Int) {
    YELLOW_STAR(Color.argb(255, 255, 255, 0)),
    RED_HIGHLIGHT(Color.argb(255, 213, 0, 0)),
    YELLOW_HIGHLIGHT(Color.argb(255, 255, 255, 0)),
    GREEN_HIGHLIGHT(Color.argb(255, 0, 255, 0)),
    BLUE_HIGHLIGHT(Color.argb(255, 145, 167, 255)),
    ORANGE_HIGHLIGHT(Color.argb(255, 255, 165, 0)),
    PURPLE_HIGHLIGHT(Color.argb(255, 128, 0, 128)),
    UNDERLINE(Color.argb(255, 128, 99, 128)),

    // Special hard-coded style for Speak bookmarks. This must be last one here.
    // This is removed from the style lists.
    SPEAK(Color.argb(255, 255, 0, 0));
    val colorArray: List<Int> get() = intToColorArray(backgroundColor)
}

val defaultLabelColor = BookmarkStyle.BLUE_HIGHLIGHT.backgroundColor

enum class BookmarkSortOrder {
    BIBLE_ORDER, CREATED_AT, CREATED_AT_DESC, LAST_UPDATED, ORDER_NUMBER;
}

interface VerseRangeUser {
    val verseRange: VerseRange
}

enum class LabelType {HIGHLIGHT, EXAMPLE}
enum class BookmarkType {EXAMPLE}

class BookmarkEntities {
    @Serializable
    class TextRange(val start: Int, val end: Int) {
        val clientList get() = listOf(start, end)
    }

    interface BaseBookmark {
        var id: IdType
        var createdAt: Date

        var ordinalStart: Int
        var ordinalEnd: Int
        var startOffset: Int?
        var endOffset: Int?

        var primaryLabelId: IdType?
        var lastUpdatedOn: Date
        var wholeVerse: Boolean
        var playbackSettings: PlaybackSettings?
    }

    interface BaseBookmarkNotes {
        var bookmarkId: IdType
        val notes: String
    }

    interface BaseBookmarkToLabel {
        val bookmarkId: IdType
        val labelId: IdType
        var orderNumber: Int
        var indentLevel: Int
        var expandContent: Boolean
        val type: String
    }

    interface BaseBookmarkWithNotes {
        val bookmarkEntity: BaseBookmark
        val noteEntity: BaseBookmarkNotes?
        var ordinalStart: Int
        var ordinalEnd: Int
        var id: IdType
        var createdAt: Date
        var startOffset: Int?
        var endOffset: Int?
        var primaryLabelId: IdType?
        var notes: String?
        var lastUpdatedOn: Date
        var wholeVerse: Boolean
        var textRange: TextRange?
        var playbackSettings: PlaybackSettings?
        var new: Boolean

        var labelIds: List<IdType>?
        var text: String?

        var fullText: String?
        var startText: String?
        var endText: String?
        val highlightedText: String

        val speakBook: Book?

        fun setBaseBookmarkToLabels(l: List<BaseBookmarkToLabel>)
    }

    @DatabaseView("SELECT b.*, bn.notes FROM BibleBookmark b LEFT OUTER JOIN BibleBookmarkNotes bn ON b.id = bn.bookmarkId")
    data class BibleBookmarkWithNotes(
        var kjvOrdinalStart: Int,
        var kjvOrdinalEnd: Int,
        override var ordinalStart: Int,
        override var ordinalEnd: Int,
        var v11n: Versification,
        override var playbackSettings: PlaybackSettings?,
        override var id: IdType = IdType(),
        override var createdAt: Date = Date(System.currentTimeMillis()),
        var book: AbstractPassageBook? = null,
        override var startOffset: Int?,
        override var endOffset: Int?,
        override var primaryLabelId: IdType? = null,
        override var notes: String? = null,
        override var lastUpdatedOn: Date = Date(System.currentTimeMillis()),
        override var wholeVerse: Boolean = false,
        var type: BookmarkType? = null,
        override var new: Boolean = false,
    ): VerseRangeUser, BaseBookmarkWithNotes {
        constructor(
            kjvOrdinalStart: Int = 0,
            kjvOrdinalEnd: Int = 0,
            ordinalStart: Int = 0,
            ordinalEnd: Int = 0,
            v11n: Versification = KJVA,
            playbackSettings: PlaybackSettings? = null,
            id: IdType = IdType(),
            createdAt: Date = Date(System.currentTimeMillis()),
            book: AbstractPassageBook? = null,
            startOffset: Int? = null,
            endOffset: Int? = null,
            primaryLabelId: IdType? = null,
            notes: String? = null,
            lastUpdatedOn: Date = Date(System.currentTimeMillis()),
            wholeVerse: Boolean = true,
            type: BookmarkType? = null,
        ): this(
            kjvOrdinalStart = kjvOrdinalStart,
            kjvOrdinalEnd = kjvOrdinalEnd,
            ordinalStart = ordinalStart,
            ordinalEnd = ordinalEnd,
            v11n = v11n,
            playbackSettings = playbackSettings,
            id = id,
            createdAt = createdAt,
            book = book,
            startOffset = startOffset,
            endOffset = endOffset,
            primaryLabelId = primaryLabelId,
            notes = notes,
            lastUpdatedOn = lastUpdatedOn,
            wholeVerse = wholeVerse,
            type = type,
            new = false,
        )

        constructor(verseRange: VerseRange, textRange: TextRange?, wholeVerse: Boolean, book: AbstractPassageBook?): this(
            kjvOrdinalStart = verseRange.toV11n(KJVA).start.ordinal,
            kjvOrdinalEnd = verseRange.toV11n(KJVA).end.ordinal,
            ordinalStart = verseRange.start.ordinal,
            ordinalEnd = verseRange.end.ordinal,
            v11n = verseRange.versification,
            playbackSettings = null,
            book = book,
            startOffset = textRange?.start,
            endOffset = textRange?.end,
            wholeVerse = wholeVerse,
            new = true,
        )

        override var textRange: TextRange?
            get() = if(startOffset != null && endOffset != null) {
                TextRange(startOffset!!, endOffset!!)
            } else null
            set(value) {
                if(value == null) {
                    startOffset = null
                    endOffset = null
                } else {
                    startOffset = value.start
                    endOffset = value.end
                }
            }

        override var verseRange: VerseRange
            get() {
                val begin = Verse(v11n, ordinalStart)
                val end = Verse(v11n, ordinalEnd)
                return VerseRange(v11n, begin, end)
            }
            set(value) {
                v11n = value.versification
                ordinalStart = value.start.ordinal
                ordinalEnd = value.end.ordinal
                kjvVerseRange = value
            }

        var kjvVerseRange: VerseRange
            get() {
                val begin = Verse(KJVA, kjvOrdinalStart)
                val end = Verse(KJVA, kjvOrdinalEnd)
                return VerseRange(KJVA, begin, end)
            }
            private set(value) {
                kjvOrdinalStart = value.toV11n(KJVA).start.ordinal
                kjvOrdinalEnd = value.toV11n(KJVA).end.ordinal
            }

        override val speakBook: Book?
            get() = if (playbackSettings != null && playbackSettings!!.bookId != null) {
                Books.installed().getBook(playbackSettings!!.bookId)
            } else {
                null
            }
        @Ignore override var labelIds: List<IdType>? = null
        @Ignore var bookmarkToLabels: List<BibleBookmarkToLabel>? = null
        @Ignore override var text: String? = null
        override fun setBaseBookmarkToLabels(l: List<BaseBookmarkToLabel>) {
            bookmarkToLabels = l as List<BibleBookmarkToLabel>
        }

        override val highlightedText: String get() = "$startText<b>$text</b>$endText"

        @Ignore
        override var startText: String? = null
        @Ignore
        override var endText: String? = null

        @Ignore
        override var fullText: String? = null
        @Ignore var osisFragment: OsisFragment? = null

        override val bookmarkEntity get() = BibleBookmark(
            kjvOrdinalStart,
            kjvOrdinalEnd,
            ordinalStart,
            ordinalEnd,
            v11n,
            playbackSettings,
            id,
            createdAt,
            book,
            startOffset,
            endOffset,
            primaryLabelId,
            lastUpdatedOn,
            wholeVerse,
            type,
        )
        override val noteEntity get() = if(notes == null) null else BibleBookmarkNotes(id, notes!!)
    }
    @Entity(
        foreignKeys = [
            ForeignKey(entity = BibleBookmark::class, parentColumns = ["id"], childColumns = ["bookmarkId"], onDelete = ForeignKey.CASCADE),
        ],
    )
    data class BibleBookmarkNotes(
        @PrimaryKey override var bookmarkId: IdType = IdType(),
        override val notes: String
    ): BaseBookmarkNotes

    @Entity(
        indices = [
            Index("kjvOrdinalStart"), Index("kjvOrdinalEnd"), Index("primaryLabelId"),
        ],
        foreignKeys = [
            ForeignKey(entity = Label::class, parentColumns = ["id"], childColumns = ["primaryLabelId"], onDelete = ForeignKey.SET_NULL),
        ],
    )
    data class BibleBookmark(
        // Verse range in KJV ordinals. For generic lookups, we must store verse ranges in a "standard"
        // versification. We store also verserange in original versification, as it conveys the more exact
        // versification-specific information.

        var kjvOrdinalStart: Int,
        var kjvOrdinalEnd: Int,

        override var ordinalStart: Int,
        override var ordinalEnd: Int,

        var v11n: Versification,

        override var playbackSettings: PlaybackSettings?,

        @PrimaryKey override var id: IdType = IdType(),

        override var createdAt: Date = Date(System.currentTimeMillis()),

        var book: AbstractPassageBook? = null,

        override var startOffset: Int?,
        override var endOffset: Int?,

        @ColumnInfo(defaultValue = "NULL") override var primaryLabelId: IdType? = null,

        @ColumnInfo(defaultValue = "0") override var lastUpdatedOn: Date = Date(System.currentTimeMillis()),
        @ColumnInfo(defaultValue = "0") override var wholeVerse: Boolean = false,
        @ColumnInfo(defaultValue = "NULL") var type: BookmarkType? = null,
    ): BaseBookmark

    @Entity(
        primaryKeys = ["bookmarkId", "labelId"],
        foreignKeys = [
            ForeignKey(entity = BibleBookmark::class, parentColumns = ["id"], childColumns = ["bookmarkId"], onDelete = ForeignKey.CASCADE),
            ForeignKey(entity = Label::class, parentColumns = ["id"], childColumns = ["labelId"], onDelete = ForeignKey.CASCADE)
        ],
        indices = [
            Index("labelId")
        ]
    )
    @Serializable
    data class BibleBookmarkToLabel(
        override val bookmarkId: IdType,
        override val labelId: IdType,

        // Studypad display variables
        @ColumnInfo(defaultValue = "-1") override var orderNumber: Int = -1,
        @ColumnInfo(defaultValue = "0") override var indentLevel: Int = 0,
        @ColumnInfo(defaultValue = "0") override var expandContent: Boolean = true,
    ): BaseBookmarkToLabel {
        constructor(bookmark: BibleBookmark, label: Label): this(bookmark.id, label.id)
        @Ignore override val type: String = "BibleBookmarkToLabel"
    }

    @DatabaseView("SELECT b.*, bn.notes FROM GenericBookmark b LEFT OUTER JOIN GenericBookmarkNotes bn ON b.id = bn.bookmarkId")
    data class GenericBookmarkWithNotes(
        override var id: IdType = IdType(),
        var key: String,
        override var createdAt: Date = Date(System.currentTimeMillis()),
        var bookInitials: String,

        override var ordinalStart: Int,
        override var ordinalEnd: Int,
        override var startOffset: Int?,
        override var endOffset: Int?,

        override var primaryLabelId: IdType? = null,
        override var notes: String? = null,
        override var lastUpdatedOn: Date = Date(System.currentTimeMillis()),
        override var wholeVerse: Boolean = false,
        override var playbackSettings: PlaybackSettings?,
        override var new: Boolean = false,
    ): BaseBookmarkWithNotes {
        constructor(
            id: IdType = IdType(),
            key: String,
            createdAt: Date = Date(System.currentTimeMillis()),
            bookInitials: String,
            ordinalStart: Int,
            ordinalEnd: Int,
            startOffset: Int?,
            endOffset: Int?,
            primaryLabelId: IdType? = null,
            notes: String? = null,
            wholeVerse: Boolean = false,
            playbackSettings: PlaybackSettings? = null,
            lastUpdatedOn: Date = Date(System.currentTimeMillis()),
        ): this(
            id = id,
            key = key,
            createdAt = createdAt,
            bookInitials = bookInitials,
            ordinalStart = ordinalStart,
            ordinalEnd = ordinalEnd,
            startOffset = startOffset,
            endOffset = endOffset,
            primaryLabelId = primaryLabelId,
            notes = notes,
            lastUpdatedOn = lastUpdatedOn,
            wholeVerse = wholeVerse,
            playbackSettings = playbackSettings,
            new = false
        )
        constructor(
            id: IdType = IdType(),
            key: String,
            createdAt: Date = Date(System.currentTimeMillis()),
            book: Book,
            ordinalStart: Int,
            ordinalEnd: Int,
            textRange: TextRange?,
            primaryLabelId: IdType? = null,
            notes: String? = null,
            lastUpdatedOn: Date = Date(System.currentTimeMillis()),
            wholeVerse: Boolean = false,
            playbackSettings: PlaybackSettings? = null,
            new: Boolean = false
        ): this(
            id = id,
            key = key,
            createdAt = createdAt,
            bookInitials = book.initials,
            ordinalStart = ordinalStart,
            ordinalEnd = ordinalEnd,
            startOffset = textRange?.start,
            endOffset = textRange?.end,
            primaryLabelId = primaryLabelId,
            notes = notes,
            lastUpdatedOn = lastUpdatedOn,
            wholeVerse = wholeVerse,
            playbackSettings = playbackSettings,
            new = new
        )
        
        constructor(key: Key, book: Book, textRange: TextRange?, ordinalStart: Int): this(
            key = key.osisRef,
            playbackSettings = null,
            ordinalStart = ordinalStart,
            ordinalEnd = ordinalStart,
            book = book,
            textRange = textRange,
            wholeVerse = true,
            new = true,            
        )

        override var textRange: TextRange?
            get() = if(startOffset != null && endOffset != null) {
                TextRange(startOffset!!, endOffset!!)
            } else null
            set(value) {
                if(value == null) {
                    startOffset = null
                    endOffset = null
                } else {
                    startOffset = value.start
                    endOffset = value.end
                }
            }

        @Ignore override var labelIds: List<IdType>? = null
        @Ignore var bookmarkToLabels: List<GenericBookmarkToLabel>? = null
        @Ignore override var text: String? = null
        @Ignore override var fullText: String? = null
        @Ignore override var startText: String? = null
        @Ignore override var endText: String? = null
        override val highlightedText: String get() = "$startText<b>$text</b>$endText"

        override fun setBaseBookmarkToLabels(l: List<BaseBookmarkToLabel>) {
            bookmarkToLabels = l as List<GenericBookmarkToLabel>
        }

        val book: Book? get() = Books.installed().getBook(bookInitials)
        val originalKey: Key? get() = try { book?.getKey(key) } catch (e: NoSuchKeyException) { null }
        val bookKey: Key? get() = originalKey?.let {if(it is RangedPassage) it.firstOrNull() else it }

        override val speakBook: Book? get() = book

        override val bookmarkEntity get() = GenericBookmark(
            id = id,
            key = key,
            ordinalStart = ordinalStart,
            ordinalEnd = ordinalEnd,
            createdAt = createdAt,
            bookInitials = bookInitials,
            startOffset = startOffset,
            endOffset = endOffset,
            primaryLabelId = primaryLabelId,
            lastUpdatedOn = lastUpdatedOn,
            wholeVerse = wholeVerse,
            playbackSettings = playbackSettings,
        )
        override val noteEntity get() = if(notes == null) null else GenericBookmarkNotes(id, notes!!)
    }

    @Entity(
        foreignKeys = [
            ForeignKey(entity = GenericBookmark::class, parentColumns = ["id"], childColumns = ["bookmarkId"], onDelete = ForeignKey.CASCADE),
        ],
    )
    data class GenericBookmarkNotes(
        @PrimaryKey override var bookmarkId: IdType = IdType(),
        override val notes: String
    ): BaseBookmarkNotes

    @Entity(
        indices = [Index(value = ["bookInitials", "key"]), Index("primaryLabelId")],
        foreignKeys = [
            ForeignKey(entity = Label::class, parentColumns = ["id"], childColumns = ["primaryLabelId"], onDelete = ForeignKey.SET_NULL),
        ],
    )
    data class GenericBookmark(
        @PrimaryKey override var id: IdType = IdType(),
        var key: String,
        override var createdAt: Date = Date(System.currentTimeMillis()),
        @ColumnInfo(defaultValue = "''") var bookInitials: String,

        override var ordinalStart: Int,
        override var ordinalEnd: Int,
        override var startOffset: Int?,
        override var endOffset: Int?,

        @ColumnInfo(defaultValue = "NULL") override var primaryLabelId: IdType? = null,
        @ColumnInfo(defaultValue = "0") override var lastUpdatedOn: Date = Date(System.currentTimeMillis()),
        @ColumnInfo(defaultValue = "0") override var wholeVerse: Boolean = false,
        override var playbackSettings: PlaybackSettings? = null,
    ): BaseBookmark

    @Entity(
        primaryKeys = ["bookmarkId", "labelId"],
        foreignKeys = [
            ForeignKey(entity = GenericBookmark::class, parentColumns = ["id"], childColumns = ["bookmarkId"], onDelete = ForeignKey.CASCADE),
            ForeignKey(entity = Label::class, parentColumns = ["id"], childColumns = ["labelId"], onDelete = ForeignKey.CASCADE)
        ],
        indices = [
            Index("labelId")
        ]
    )
    @Serializable
    data class GenericBookmarkToLabel(
        override val bookmarkId: IdType,
        override val labelId: IdType,

        // Studypad display variables
        @ColumnInfo(defaultValue = "-1") override var orderNumber: Int = -1,
        @ColumnInfo(defaultValue = "0") override var indentLevel: Int = 0,
        @ColumnInfo(defaultValue = "0") override var expandContent: Boolean = true,
    ): BaseBookmarkToLabel {
        constructor(bookmark: GenericBookmark, label: Label): this(bookmark.id, label.id)
        @Ignore override val type: String = "GenericBookmarkToLabel"
    }

    @Entity(
        foreignKeys = [
            ForeignKey(entity = Label::class, parentColumns = ["id"], childColumns = ["labelId"], onDelete = ForeignKey.CASCADE)
        ],
        indices = [
            Index("labelId")
        ]
    )
    data class StudyPadTextEntry(
        @PrimaryKey val id: IdType = IdType(),
        val labelId: IdType,
        var orderNumber: Int,
        var indentLevel: Int = 0,
    )

    @Entity(
        foreignKeys = [
            ForeignKey(entity = StudyPadTextEntry::class, parentColumns = ["id"], childColumns = ["studyPadTextEntryId"], onDelete = ForeignKey.CASCADE)
        ],
    )
    data class StudyPadTextEntryText(
        @PrimaryKey val studyPadTextEntryId: IdType = IdType(),
        val text: String = "",
    )

    @DatabaseView("SELECT e.*, t.text FROM StudyPadTextEntry e INNER JOIN StudyPadTextEntryText t ON e.id = t.studyPadTextEntryId")
    @Serializable
    data class StudyPadTextEntryWithText(
        @PrimaryKey val id: IdType = IdType(),
        val labelId: IdType,
        var orderNumber: Int,
        var indentLevel: Int = 0,
        val text: String = "",
    ) {
        @Ignore val type: String = "journal"
        @Ignore val hashCode: Int = abs(id.hashCode())
        val studyPadTextEntryEntity get() = StudyPadTextEntry(id, labelId, orderNumber, indentLevel)
        val studyPadTextEntryTextEntity get() = StudyPadTextEntryText(id, text)
    }

    @Entity(indices = [Index("favourite")])
    @Serializable
    data class Label(
        @PrimaryKey var id: IdType = IdType(),
        var name: String = "",
        @ColumnInfo(defaultValue = "0") var color: Int = defaultLabelColor,
        @ColumnInfo(defaultValue = "0") var markerStyle: Boolean = false,
        @ColumnInfo(defaultValue = "0") var markerStyleWholeVerse: Boolean = false,
        @ColumnInfo(defaultValue = "0") var underlineStyle: Boolean = false,
        @ColumnInfo(defaultValue = "0") var underlineStyleWholeVerse: Boolean = true,
        @ColumnInfo(defaultValue = "0") var hideStyle: Boolean = false,
        @ColumnInfo(defaultValue = "0") var hideStyleWholeVerse: Boolean = false,
        @ColumnInfo(defaultValue = "0") var favourite: Boolean = false,
        @ColumnInfo(defaultValue = "NULL") var type: LabelType? = null,
        @Ignore var new: Boolean = false
    ) {
        override fun toString() = name
        val isSpeakLabel get() = name == SPEAK_LABEL_NAME
        val isUnlabeledLabel get() = name == UNLABELED_NAME
        val isSpecialLabel get() = isSpeakLabel || isUnlabeledLabel
    }
}
