package com.example.suffixtrainer.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A morpheme attached to a [Token]. `startInSurface`/`endInSurface` define the
 * character range within the token's `surface` form that this suffix occupies —
 * the range that gets blanked when its [category] is enabled. Because of Turkish
 * vowel harmony the surface form varies (-de/-da/-te/-ta), so `morpheme` stores
 * the actual surface span, not a canonical form.
 */
@Entity(
    tableName = "suffixes",
    foreignKeys = [
        ForeignKey(
            entity = Token::class,
            parentColumns = ["id"],
            childColumns = ["tokenId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tokenId")],
)
data class Suffix(
    @PrimaryKey val id: Long,
    val tokenId: Long,
    val morpheme: String,
    val category: Category,
    val startInSurface: Int,
    val endInSurface: Int,
)
