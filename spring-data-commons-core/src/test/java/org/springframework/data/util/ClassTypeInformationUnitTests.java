package org.springframework.data.util;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.Map;

import org.junit.Test;
import org.springframework.data.mapping.Person;

/**
 * @author Oliver Gierke
 */
public class ClassTypeInformationUnitTests {

  @Test
  public void discoversTypeForSimpleGenericField() {

    TypeInformation discoverer = new ClassTypeInformation(
        ConcreteType.class);
    assertEquals(ConcreteType.class, discoverer.getType());
    assertEquals(String.class, discoverer.getProperty("content").getType());
  }

  @Test
  public void discoversTypeForNestedGenericField() {

    TypeInformation discoverer = new ClassTypeInformation(
        ConcreteWrapper.class);
    assertEquals(ConcreteWrapper.class, discoverer.getType());
    TypeInformation wrapper = discoverer.getProperty("wrapped");
    assertEquals(GenericType.class, wrapper.getType());
    TypeInformation content = wrapper.getProperty("content");

    assertEquals(String.class, content.getType());
    assertEquals(String.class,
        discoverer.getProperty("wrapped").getProperty("content").getType());
    assertEquals(String.class, discoverer.getProperty("wrapped.content")
        .getType());
  }

  @Test
  public void discoversBoundType() {

    ClassTypeInformation information = new ClassTypeInformation(
        GenericTypeWithBound.class);
    assertEquals(Person.class, information.getProperty("person").getType());
  }

  @Test
  public void discoversBoundTypeForSpecialization() {

    ClassTypeInformation information = new ClassTypeInformation(
        SpecialGenericTypeWithBound.class);
    assertEquals(SpecialPerson.class, information.getProperty("person")
        .getType());
  }

  @Test
  public void discoversBoundTypeForNested() {

    ClassTypeInformation information = new ClassTypeInformation(
        AnotherGenericType.class);
    assertEquals(GenericTypeWithBound.class, information.getProperty("nested")
        .getType());
    assertEquals(Person.class, information.getProperty("nested.person")
        .getType());
  }
  
  @Test
  public void discoversArrays() {
    ClassTypeInformation information = new ClassTypeInformation(CollectionContainer.class);
    Class<?> type = information.getProperty("array").getType();
    assertEquals(String[].class, type);
    assertThat(type.isArray(), is(true));
  }
  
  @Test
  public void discoversMapValueType() {
    
    ClassTypeInformation information = new ClassTypeInformation(StringMapContainer.class);
    TypeInformation genericMap = information.getProperty("genericMap");
    assertEquals(Map.class, genericMap.getType());
    assertEquals(String.class, genericMap.getMapValueType());

    TypeInformation map = information.getProperty("map");
    assertEquals(Map.class, map.getType());
    assertEquals(Calendar.class, map.getMapValueType());
  }
  
  private class StringMapContainer extends MapContainer<String> {
    
  }
  
  private class MapContainer<T> {
    Map<String, T> genericMap;
    Map<String, Calendar> map;
  }
  
  private class CollectionContainer {
    
    String[] array;
  }

  private class GenericTypeWithBound<T extends Person> {

    T person;
  }

  private class AnotherGenericType<T extends Person, S extends GenericTypeWithBound<T>> {

    S nested;
  }

  private class SpecialGenericTypeWithBound extends
      GenericTypeWithBound<SpecialPerson> {

  }

  private abstract class SpecialPerson extends Person {

    protected SpecialPerson(Integer ssn, String firstName, String lastName) {
      super(ssn, firstName, lastName);
    }
  }

  private class GenericType<T, S> {

    Long index;
    T content;
  }

  private class ConcreteType extends GenericType<String, Object> {

  }

  private class GenericWrapper<S> {

    GenericType<S, Object> wrapped;
  }

  private class ConcreteWrapper extends GenericWrapper<String> {

  }
}
