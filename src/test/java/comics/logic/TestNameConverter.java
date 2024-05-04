package comics.logic;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestNameConverter {

    private void testImplementation(Map<String, String> data) {
        var converter = new NameConverter();
        data.entrySet().forEach(entry -> {
            assertEquals(entry.getValue(), converter.normalizeFileName(entry.getKey()));
        });
    }

    @Test
    public void testRandomUseOfPoints() {
        testImplementation(
            Map.of(
                "Some.COMIC.-.for.WhatEver.reaSON.I.DID.NOT.WANT.TO.USE.blanks.3434.cbr",
                    "Some Comic - For Whatever Reason I Did Not Want To Use Blanks - 3434.cbr"
            )
        );
    }

    @Test
    public void testCollections() {
        testImplementation(
            Map.of(
                "Some comic 1 dE 54",
                    "Some Comic - 1 de 54",
                "Some cOMIC - 34 of 545",
                    "Some Comic - 34 of 545",
                "Some CoMIC 35 de 435  ",
                    "Some Comic - 35 de 435"
            )
        );
    }

    @Test
    public void testBracketsInNames() {
        testImplementation(
            Map.of(
                "Some comic - Random title With caps 06 [TM][por someGuy y Another guy][Some guys together].cbr",
                    "Some Comic - Random Title With Caps - 06.cbr",
                "Some comic - Random title With caps 07a [TM][por someGuy y Another guy][Some guys together].cbz",
                    "Some Comic - Random Title With Caps 07a.cbz",
                "SOME comic - Random title  With CAPS 11 [TM][por  someGuy y Another guy][Some guys together].cbz",
                    "Some Comic - Random Title With Caps - 11.cbz",
                "Some Comic - RANDOM title With caps 12 [TM][por  someGuy y Another guy][Some guys together].cbz",
                    "Some Comic - Random Title With Caps - 12.cbz",
                " Some comic - Random TITLE With caps 13 [TM][Whatever Comics].cbr",
                    "Some Comic - Random Title With Caps - 13.cbr",
                "Some comic - Random title With caps 15 [TM][Whatever Comics].cbz",
                    "Some Comic - Random Title With Caps - 15.cbz",
                "Some comic - Random title With caps 78 [TM[Whatever Comics].cbz",
                    "Some Comic - Random Title With Caps - 78.cbz",
                "Some comic - Random One Shot [Blah].cbr",
                    "Some Comic - Random One Shot.cbr",
                "Some comic - Blàh bláh BlÂh - Subseries 01 (Never mind 08)    [José y Pàco].cbz",
                    "Some Comic - Blàh Bláh Blâh - Subseries - 01.cbz",
                "SOME  COMIC 84 #001 (1994) -  some Title.cbz",
                    "Some Comic 84 #001 - Some Title.cbz"
            )
        );
    }
}
