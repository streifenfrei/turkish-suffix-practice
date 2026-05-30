package com.example.suffixtrainer.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CardRepository] backed by the prepackaged Room database (see DatabaseModule's
 * `createFromAsset`). Emits the same [CardData] shape as [FakeCardRepository] by mapping the
 * JOIN rows from [SentenceDao.observeCorpus], so the ViewModel and domain layer are unchanged.
 */
@Singleton
class RoomCardRepository @Inject constructor(
    private val dao: SentenceDao,
) : CardRepository {
    override fun observeCorpus(): Flow<List<CardData>> =
        dao.observeCorpus().map { rows ->
            rows.map { row ->
                CardData(
                    sentence = row.sentence,
                    tokens = row.tokens.map { TokenWithSuffixes(it.token, it.suffixes) },
                )
            }
        }
}
