package com.crowdsiege.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.service.gateway.RandomNicknameGenerator;
import org.junit.jupiter.api.Test;

class NicknameRulesTest {

    @Test
    void normalizeFoldsFullWidthCaseAndOuterWhitespace() {
        assertThat(NicknameRules.normalize("Ｂｏｂ")).isEqualTo(NicknameRules.normalize("Bob")).isEqualTo("bob");
        assertThat(NicknameRules.normalize("　 Bob 　")).isEqualTo("bob");
        assertThat(NicknameRules.normalize("ＡＢＣ１２３")).isEqualTo("abc123");
    }

    @Test
    void rejectsControlFormatAndSpecialSpaceCharacters() {
        for (String bad : new String[]{"a​b", "a‌b", "a‍b", "﻿ab", "a b", "a　b",
                "a b", "a b", "a\u0007b", "a\tb", "ab"}) {
            assertThat(NicknameRules.hasIllegalCharacter(bad)).as("U+%04X", (int) bad.chars()
                    .filter(c -> c > 127 || c < 32).findFirst().orElse(0)).isTrue();
        }
        assertThat(NicknameRules.hasIllegalCharacter("勇者 a b 😀 Ａ")).isFalse();
    }

    @Test
    void defaultNameShapeIsTheGeneratorShape() {
        RandomNicknameGenerator generator = new RandomNicknameGenerator();
        for (int i = 0; i < 500; i++) {
            assertThat(RandomNicknameGenerator.isDefaultName(generator.next())).isTrue();
        }
        assertThat(RandomNicknameGenerator.isDefaultName("勇者99")).isFalse();
        assertThat(RandomNicknameGenerator.isDefaultName("勇者1000")).isFalse();
        assertThat(RandomNicknameGenerator.isDefaultName("勇者099")).isFalse();
        assertThat(RandomNicknameGenerator.isDefaultName("勇者abc")).isFalse();
        assertThat(RandomNicknameGenerator.isDefaultName("老兵512")).isFalse();
        assertThat(RandomNicknameGenerator.isDefaultName(null)).isFalse();
        assertThat(RandomNicknameGenerator.isDefaultName(NicknameRules.normalize("勇者５００"))).isTrue();
    }
}
