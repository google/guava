package com.google.common.base;

import static com.google.common.truth.Truth.assertThat;

import junit.framework.TestCase;

/**
 * Unit test for {@link EquivalenceRelation}.
 *
 * @author Ivan Osipov
 */
public class EquivalenceRelationTest extends TestCase {

    public void testEquivalenceRelation_equalRelation() {
        EquivalenceRelation firstRelation = EquivalenceRelation.of(-42, 42);
        EquivalenceRelation secondRelation = EquivalenceRelation.of(42, -42);

        assertThat(firstRelation).isEqualTo(secondRelation);
    }

    public void testEquivalenceRelation_equalWithNulls() {
        EquivalenceRelation firstRelation = EquivalenceRelation.of(42, null);
        EquivalenceRelation secondRelation = EquivalenceRelation.of(null, 42);

        assertThat(firstRelation).isEqualTo(secondRelation);
    }

    public void testEquivalenceRelation_notEqual() {
        EquivalenceRelation firstRelation = EquivalenceRelation.of(43, 42);
        EquivalenceRelation secondRelation = EquivalenceRelation.of(41, 42);

        assertThat(firstRelation).isNotEqualTo(secondRelation);
    }

    public void testEquivalenceRelation_notEqualWithNulls() {
        EquivalenceRelation firstRelation = EquivalenceRelation.of(43, null);
        EquivalenceRelation secondRelation = EquivalenceRelation.of(null, 42);

        assertThat(firstRelation).isNotEqualTo(secondRelation);
    }

    public void testEquivalenceRelation_equalWithOnlyNulls() {
        EquivalenceRelation firstRelation = EquivalenceRelation.of(null, null);
        EquivalenceRelation secondRelation = EquivalenceRelation.of(null, null);

        assertThat(firstRelation).isEqualTo(secondRelation);
    }

    public void testEquivalenceRelation_notEqualWithOnlyNulls() {
        EquivalenceRelation firstRelation = EquivalenceRelation.of(null, null);
        EquivalenceRelation secondRelation = EquivalenceRelation.of(24, null);
        EquivalenceRelation thirdRelation = EquivalenceRelation.of(null, 24);

        assertThat(firstRelation).isNotEqualTo(secondRelation);
        assertThat(firstRelation).isNotEqualTo(thirdRelation);
    }

    public void testEquivalenceRelation_hashCode() {
        assertThat(EquivalenceRelation.of(42, 41).hashCode())
                .isEqualTo(EquivalenceRelation.of(42, 41).hashCode());
        assertThat(EquivalenceRelation.of(42, 41).hashCode())
                .isEqualTo(EquivalenceRelation.of(41, 42).hashCode());
        assertThat(EquivalenceRelation.of(42, 43).hashCode())
                .isNotEqualTo(EquivalenceRelation.of(42, 41).hashCode());
        assertThat(EquivalenceRelation.of(42, 43).hashCode())
                .isNotEqualTo(EquivalenceRelation.of(41, 42).hashCode());

        assertThat(EquivalenceRelation.of(42, null).hashCode())
                .isEqualTo(EquivalenceRelation.of(null, 42).hashCode());
        assertThat(EquivalenceRelation.of(43, null).hashCode())
                .isNotEqualTo(EquivalenceRelation.of(42, null).hashCode());
        assertThat(EquivalenceRelation.of(null, null).hashCode())
                .isNotEqualTo(EquivalenceRelation.of(42, null).hashCode());

        //a collision
        assertThat(EquivalenceRelation.of(null, null).hashCode())
                .isEqualTo(EquivalenceRelation.of(null, 0).hashCode());
        assertThat(EquivalenceRelation.of(null, null))
                .isNotEqualTo(EquivalenceRelation.of(null, 0));
    }

}
