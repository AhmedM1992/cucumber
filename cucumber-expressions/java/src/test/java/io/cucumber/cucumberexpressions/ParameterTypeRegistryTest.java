package io.cucumber.cucumberexpressions;

import io.cucumber.cucumberexpressions.AmbiguousParameterTypeException.AmbiguousRegularExpressionException;
import io.cucumber.cucumberexpressions.AmbiguousParameterTypeException.AmbiguousTypeException;
import org.junit.Test;

import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class ParameterTypeRegistryTest {

    private static final String CAPITALISED_WORD = "[A-Z]+\\w+";
    private static final String TITLE_NAME = "Sir|Madam " + CAPITALISED_WORD;

    static class Name {
        Name(String s) {
        }
    }

    static class Person {
        Person(String s) {
        }
    }

    static class Place {
        Place(String s) {
        }
    }

    private final ParameterTypeRegistry registry = new ParameterTypeRegistry(Locale.ENGLISH);

    @Test
    public void does_not_allow_more_than_one_preferential_parameter_type_for_each_regexp() {
        registry.defineParameterType(new ParameterType<>("name", CAPITALISED_WORD, Name.class, new SingleTransformer<>(new Function<String, Name>() {
            @Override
            public Name apply(String s) {
                return new Name(s);
            }
        }), false, true));
        registry.defineParameterType(new ParameterType<>("person", CAPITALISED_WORD, Person.class, new SingleTransformer<>(new Function<String, Person>() {
            @Override
            public Person apply(String s) {
                return new Person(s);
            }
        }), false, false));
        try {
            registry.defineParameterType(new ParameterType<>("place", CAPITALISED_WORD, Place.class, new SingleTransformer<>(new Function<String, Place>() {
                @Override
                public Place apply(String s) {
                    return new Place(s);
                }
            }), false, true));
            fail("Expected an exception");
        } catch (CucumberExpressionException e) {
            assertEquals("There can only be one preferential parameter type per regexp. The regexp /[A-Z]+\\w+/ is used for two preferential parameter types, {name} and {place}", e.getMessage());
        }
    }

    @Test
    public void looks_up_preferential_parameter_type_by_regexp() {
        ParameterType<Name> name = new ParameterType<>("name", CAPITALISED_WORD, Name.class, new SingleTransformer<>(new Function<String, Name>() {
            @Override
            public Name apply(String s) {
                return new Name(s);
            }
        }), false, false);
        ParameterType<Person> person = new ParameterType<>("person", CAPITALISED_WORD, Person.class, new SingleTransformer<>(new Function<String, Person>() {
            @Override
            public Person apply(String s) {
                return new Person(s);
            }
        }), false, true);
        ParameterType<Place> place = new ParameterType<>("place", CAPITALISED_WORD, Place.class, new SingleTransformer<>(new Function<String, Place>() {
            @Override
            public Place apply(String s) {
                return new Place(s);
            }
        }), false, false);
        registry.defineParameterType(name);
        registry.defineParameterType(person);
        registry.defineParameterType(place);
        assertSame(person, registry.lookupByRegexp(CAPITALISED_WORD, Pattern.compile("([A-Z]+\\w+) and ([A-Z]+\\w+)"), "Lisa and Bob"));
    }

    @Test
    public void throws_ambiguous_exception_on_lookup_when_no_parameter_types_are_preferential() {
        ParameterType<Name> name = new ParameterType<>("name", CAPITALISED_WORD, Name.class, new SingleTransformer<>(new Function<String, Name>() {
            @Override
            public Name apply(String s) {
                return new Name(s);
            }
        }), true, false);
        ParameterType<Person> person = new ParameterType<>("person", CAPITALISED_WORD, Person.class, new SingleTransformer<>(new Function<String, Person>() {
            @Override
            public Person apply(String s) {
                return new Person(s);
            }
        }), true, false);
        ParameterType<Place> place = new ParameterType<>("place", CAPITALISED_WORD, Place.class, new SingleTransformer<>(new Function<String, Place>() {
            @Override
            public Place apply(String s) {
                return new Place(s);
            }
        }), true, false);
        registry.defineParameterType(name);
        registry.defineParameterType(person);
        registry.defineParameterType(place);
        try {
            registry.lookupByRegexp(CAPITALISED_WORD, Pattern.compile("([A-Z]+\\w+) and ([A-Z]+\\w+)"), "Lisa and Bob");
            fail("Expected an exception");
        } catch (AmbiguousRegularExpressionException e) {
            String expected = "" +
                    "Your Regular Expression /([A-Z]+\\w+) and ([A-Z]+\\w+)/\n" +
                    "matches multiple parameter types with regexp /[A-Z]+\\w+/:\n" +
                    "   {name}\n" +
                    "   {person}\n" +
                    "   {place}\n" +
                    "\n" +
                    "I couldn't decide which one to use. You have two options:\n" +
                    "\n" +
                    "1) Use a Cucumber Expression instead of a Regular Expression. Try one of these:\n" +
                    "   {name} and {name}\n" +
                    "   {name} and {person}\n" +
                    "   {name} and {place}\n" +
                    "   {person} and {name}\n" +
                    "   {person} and {person}\n" +
                    "   {person} and {place}\n" +
                    "   {place} and {name}\n" +
                    "   {place} and {person}\n" +
                    "   {place} and {place}\n" +
                    "\n" +
                    "2) Make one of the parameter types preferential and continue to use a Regular Expression.\n" +
                    "\n";
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void throws_ambiguous_exception_on_lookup() {
        ParameterType<Name> name = new ParameterType<>("name", CAPITALISED_WORD, Name.class, new SingleTransformer<>(new Function<String, Name>() {
            @Override
            public Name apply(String s) {
                return new Name(s);
            }
        }), true, false);
        ParameterType<Name> title = new ParameterType<>("title", TITLE_NAME, Name.class, new SingleTransformer<>(new Function<String, Name>() {
            @Override
            public Name apply(String s) {
                return new Name(s);
            }
        }), true, false);

        registry.defineParameterType(name);
        registry.defineParameterType(title);

        try {
            registry.lookupByType(Name.class);
            fail("Expected an exception");
        } catch (AmbiguousTypeException e) {
            String expected = "" +
                    "There are multiple parameter types for class io.cucumber.cucumberexpressions.ParameterTypeRegistryTest$Name:\n" +
                    "   {name}\n" +
                    "   {title}\n" +
                    "\n" +
                    "I couldn't decide which one to use. You have two options:\n" +
                    "\n" +
                    "1) Use a single parameter type instead of multiple and combine their regular expressions.\n" +
                    "\n" +
                    "2) Create a wrapper for class io.cucumber.cucumberexpressions.ParameterTypeRegistryTest$Name to make the difference explicit.\n";
            assertEquals(expected, e.getMessage());
        }
    }

}
