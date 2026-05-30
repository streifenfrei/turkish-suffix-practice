package com.example.suffixtrainer.data

import androidx.room.Embedded
import androidx.room.Relation
import com.example.suffixtrainer.model.Sentence
import com.example.suffixtrainer.model.Suffix
import com.example.suffixtrainer.model.Token

/**
 * Room relation rows for the corpus JOIN. These keep the Room wiring out of the plain
 * [CardData] DTO: [RoomCardRepository] maps these into [CardData]/[TokenWithSuffixes], which is
 * the shape the fake also produces, so the rest of the app is unaware of the source.
 */
data class TokenWithSuffixesRow(
    @Embedded val token: Token,
    @Relation(parentColumn = "id", entityColumn = "tokenId")
    val suffixes: List<Suffix>,
)

data class SentenceWithTokensRow(
    @Embedded val sentence: Sentence,
    @Relation(entity = Token::class, parentColumn = "id", entityColumn = "sentenceId")
    val tokens: List<TokenWithSuffixesRow>,
)
