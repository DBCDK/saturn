/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;


import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TransfileGeneratorTest {
    @Test
    public void test_validateTransfileTemplate() {
        final String transfileTemplate = "b=databroendpr3,t=abmxml," +
            "c=latin-1,o=littsiden,m=kildepost@dbc.dk";
        assertThat(TransfileGenerator.validateTransfileTemplate(
            transfileTemplate), is(true));
    }

    @Test
    public void test_validateTransfileTemplate_containsF() {
        final String transfileTemplate = "b=databroendpr3,t=abmxml," +
            "c=latin-1,o=littsiden,m=kildepost@dbc.dk,f=spongebob-secretpants";
        assertThat(TransfileGenerator.validateTransfileTemplate(
            transfileTemplate), is(false));
    }

    @Test
    public void test_generateTransfile() {
        final String transfileTemplate = "b=databroendpr3,t=abmxml," +
            "c=latin-1,o=littsiden,m=kildepost@dbc.dk";
        final List<String> filenames = Stream.of("fish-paste!", "barnacles!",
            "tartar sauce!").collect(Collectors.toList());
        final String transfile = TransfileGenerator.generateTransfile(
            transfileTemplate, filenames,false);
        final String expectedTransfile = "b=databroendpr3,t=abmxml," +
            "c=latin-1,o=littsiden,m=kildepost@dbc.dk,f=fish-paste!\n" +
            "b=databroendpr3,t=abmxml," +
            "c=latin-1,o=littsiden,m=kildepost@dbc.dk,f=barnacles!\n" +
            "b=databroendpr3,t=abmxml," +
            "c=latin-1,o=littsiden,m=kildepost@dbc.dk,f=tartar sauce!\n" +
            "slut";
        assertThat(transfile, is(expectedTransfile));
    }
}
