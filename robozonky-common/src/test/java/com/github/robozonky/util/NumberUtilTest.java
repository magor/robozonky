/*
 * Copyright 2017 The RoboZonky Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robozonky.util;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class NumberUtilTest {

    @Test
    public void rounding() {
        final double result = NumberUtil.toCurrency(new BigDecimal("0.554"));
        Assertions.assertThat(result).isEqualTo(0.55);
        final double result2 = NumberUtil.toCurrency(new BigDecimal("0.556"));
        Assertions.assertThat(result2).isEqualTo(0.56);
    }
}
