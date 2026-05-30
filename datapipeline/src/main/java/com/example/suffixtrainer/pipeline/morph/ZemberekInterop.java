package com.example.suffixtrainer.pipeline.morph;

import java.util.ArrayList;
import java.util.List;
import zemberek.morphology.analysis.SingleAnalysis;

/**
 * Tiny Java bridge for the parts of Zemberek's API that are awkward from Kotlin.
 *
 * {@code SingleAnalysis.MorphemeData} exposes both a public {@code morpheme}
 * field (a {@code Morpheme}) and a {@code getMorpheme()} getter, which Kotlin
 * resolves ambiguously. Reading it in Java is unambiguous, so we flatten each
 * analysis into a flat list of (morpheme id, surface) pairs here.
 */
public final class ZemberekInterop {

    private ZemberekInterop() {}

    /** One morpheme: its Zemberek id (e.g. "Loc") and surface text (e.g. "de"). */
    public static final class Morph {
        public final String id;
        public final String surface;

        public Morph(String id, String surface) {
            this.id = id;
            this.surface = surface;
        }
    }

    /** Flattens an analysis into ordered (id, surface) morpheme pairs. */
    public static List<Morph> morphemes(SingleAnalysis analysis) {
        List<Morph> result = new ArrayList<>();
        for (SingleAnalysis.MorphemeData md : analysis.getMorphemeDataList()) {
            result.add(new Morph(md.morpheme.id, md.surface));
        }
        return result;
    }
}
