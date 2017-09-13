/*
 * Copyright (C) 2017 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.collect.Hashing.smear;

import com.google.common.annotations.GwtCompatible;
import junit.framework.TestCase;

/** Tests for {@code Hashing}. */
@GwtCompatible
public class HashingTest extends TestCase {
  public void testSmear() {
    assertEquals(1459320713, smear(754102528));
    assertEquals(-160560296, smear(1234567890));
    assertEquals(-1017931171, smear(1));
    assertEquals(-1350072884, smear(-2000000000));
    assertEquals(-809843551, smear(2000000000));
    assertEquals(-309370926, smear(-1155484576));
    assertEquals(-1645495900, smear(-723955400));
    assertEquals(766424523, smear(1033096058));
    assertEquals(-757003149, smear(-1690734402));
    assertEquals(-245078984, smear(-1557280266));
    assertEquals(-1401957971, smear(1327362106));
    assertEquals(1398564061, smear(-1930858313));
    assertEquals(799903031, smear(502539523));
    assertEquals(587718043, smear(-1728529858));
    assertEquals(1757836855, smear(-938301587));
    assertEquals(1002498708, smear(1431162155));
    assertEquals(52905316, smear(1085665355));
    assertEquals(-1590037357, smear(1654374947));
    assertEquals(-100883544, smear(-1661998771));
    assertEquals(1312247346, smear(-65105105));
    assertEquals(-79641824, smear(-73789608));
    assertEquals(1739416943, smear(-518907128));
    assertEquals(483849880, smear(99135751));
    assertEquals(1797032732, smear(-252332814));
    assertEquals(329701497, smear(755814641));
    assertEquals(-1411506712, smear(1180918287));
    assertEquals(-132448996, smear(1344049776));
    assertEquals(51088191, smear(553609048));
    assertEquals(-322136643, smear(1580443894));
    assertEquals(1443704906, smear(629649304));
    assertEquals(-553641505, smear(-1266264776));
    assertEquals(996203724, smear(99807007));
    assertEquals(-1135153980, smear(5955764));
    assertEquals(-202220609, smear(-1946737912));
    assertEquals(1170627357, smear(39620447));
    assertEquals(666671983, smear(-152527805));
    assertEquals(830549906, smear(-1877116806));
    assertEquals(818272619, smear(448784075));
    assertEquals(743117554, smear(1086124775));
    assertEquals(1631368220, smear(-1609984092));
    assertEquals(-1030514623, smear(1227951724));
    assertEquals(1982371623, smear(1764356251));
    assertEquals(940948840, smear(64111306));
    assertEquals(1789753804, smear(-960731419));
    assertEquals(875698259, smear(-100082026));
    assertEquals(-1958263900, smear(-39845375));
    assertEquals(-1953676635, smear(-1339022546));
    assertEquals(1916180219, smear(2092649110));
    assertEquals(-1364581757, smear(-568315836));
    assertEquals(1918915654, smear(-1089884900));
    assertEquals(938102267, smear(-81839914));
    assertEquals(645819996, smear(-1146103148));
    assertEquals(-1072963524, smear(-1846688624));
    assertEquals(1704102819, smear(-784703072));
    assertEquals(-1183783966, smear(55004124));
    assertEquals(2097842757, smear(-691960657));
    assertEquals(-2139783994, smear(1770461755));
    assertEquals(1305227358, smear(-2032810463));
    assertEquals(-863362476, smear(-1177788003));
    assertEquals(37648593, smear(-432352882));
    assertEquals(1172853257, smear(-65824064));
    assertEquals(1811397990, smear(575267217));
    assertEquals(-91361736, smear(-1949367821));
    assertEquals(770365725, smear(356750287));
    assertEquals(522521211, smear(798819494));
    assertEquals(-37176651, smear(-92022521));
    assertEquals(-645245125, smear(1318001577));
    assertEquals(1460094042, smear(-1192467086));
    assertEquals(-1713924794, smear(-1412716779));
    assertEquals(-587126285, smear(-1223932479));
    assertEquals(2124902646, smear(276053035));
    assertEquals(1660727203, smear(615126903));
    assertEquals(-1851411975, smear(1542603436));
    assertEquals(-150321817, smear(1988388716));
    assertEquals(-1474601337, smear(1177882237));
    assertEquals(173314316, smear(19265476));
    assertEquals(910078796, smear(-1430871151));
    assertEquals(-1788757022, smear(307082914));
    assertEquals(-37217695, smear(-1333570194));
    assertEquals(-1750409108, smear(1496453452));
    assertEquals(-1184297296, smear(-790542135));
    assertEquals(1909334635, smear(1455004595));
    assertEquals(-626278147, smear(-1690249972));
    assertEquals(-1343393583, smear(-604059026));
    assertEquals(-72620618, smear(-290476856));
    assertEquals(-1721662527, smear(-122204761));
    assertEquals(20732956, smear(-1097539750));
    assertEquals(1689535747, smear(-576617062));
    assertEquals(-910174660, smear(-2002123957));
    assertEquals(-451949594, smear(-1663951485));
    assertEquals(-1040666441, smear(193034304));
    assertEquals(-568575382, smear(768747578));
    assertEquals(-1136854138, smear(1337360486));
    assertEquals(-1846303810, smear(934457803));
    assertEquals(560788004, smear(873612482));
    assertEquals(-1639693734, smear(-624972850));
    assertEquals(1401682479, smear(355564760));
    assertEquals(537840760, smear(41547336));
    assertEquals(822662855, smear(1781447028));
    assertEquals(2116379380, smear(-1321591463));
    assertEquals(1651021961, smear(1081281446));
    assertEquals(325386824, smear(-982203381));
    assertEquals(302543644, smear(-222544851));
    assertEquals(-467777650, smear(-1233998085));
    assertEquals(391483003, smear(-1331702554));
  }
}
