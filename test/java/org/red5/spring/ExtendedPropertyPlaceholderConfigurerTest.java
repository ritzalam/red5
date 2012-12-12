package org.red5.spring;

import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.*;

public class ExtendedPropertyPlaceholderConfigurerTest {

	protected ApplicationContext context;
	protected Properties testProperties;
	protected Properties testAProperties;
	protected Properties testBProperties;
	
	@Before
	public void loadSpringContext() throws IOException {
		context = new ClassPathXmlApplicationContext( "/org/red5/spring/placeholder_context.xml" );
		
		testProperties = new Properties();
		testProperties.load( this.getClass().getResourceAsStream( "/org/red5/spring/test.properties" ) );
		
		testAProperties = new Properties();
		testAProperties.load( this.getClass().getResourceAsStream( "/org/red5/spring/test_a.properties" ) );
		
		testBProperties = new Properties();
		testBProperties.load( this.getClass().getResourceAsStream( "/org/red5/spring/test_b.properties" ) );
		
	}
	
	@Test
	public void testLocationsProperty() {
		ExtendedPropertyPlaceholderConfigurer configurer = 
			(ExtendedPropertyPlaceholderConfigurer)context.getBean( "boringPlaceholderConfig" );
		
		assertEquals( testProperties, configurer.getMergedProperties() );
	}
	
	@Test
	public void testWildcardLocationsProperty() {
		ExtendedPropertyPlaceholderConfigurer configurer = 
			(ExtendedPropertyPlaceholderConfigurer)context.getBean( "wildcard1PlaceholderConfig" );
		
		Properties mergedProp = new Properties();
		mergedProp.putAll( testProperties );
		mergedProp.putAll( testAProperties );
		mergedProp.putAll( testBProperties );
		
		assertEquals( mergedProp, configurer.getMergedProperties() );
		
		configurer = 
			(ExtendedPropertyPlaceholderConfigurer)context.getBean( "wildcard2PlaceholderConfig" );
		
		mergedProp = new Properties();
		mergedProp.putAll( testAProperties );
		mergedProp.putAll( testBProperties );
		mergedProp.putAll( testProperties );
		
		assertEquals( mergedProp, configurer.getMergedProperties() );
	}
	
	@Test
	public void testLocationsPropertyOverridesWildcardLocationsProperty() {
		ExtendedPropertyPlaceholderConfigurer configurer = 
			(ExtendedPropertyPlaceholderConfigurer)context.getBean( "locationsOverridesWildcardLocationsPlaceholderConfig" );
		
		Properties mergedProp = new Properties();
		mergedProp.putAll( testProperties );
		
		assertEquals( mergedProp, configurer.getMergedProperties() );
	}
	
	@Test
	public void testRuntimeProperties() {
		ExtendedPropertyPlaceholderConfigurer.addGlobalProperty( "runtime_key1", "value1" );
		ExtendedPropertyPlaceholderConfigurer.addGlobalProperty( "runtime_key2", "value2" );
		context = new ClassPathXmlApplicationContext( "/org/red5/spring/placeholder_context.xml" );
		ExtendedPropertyPlaceholderConfigurer configurer = 
			(ExtendedPropertyPlaceholderConfigurer)context.getBean( "locationsOverridesWildcardLocationsPlaceholderConfig" );
		
		Properties mergedProp = new Properties();
		mergedProp.putAll( testProperties );
		mergedProp.put( "runtime_key1", "value1" );
		mergedProp.put( "runtime_key2", "value2" );
		
		assertEquals( 
				mergedProp, configurer.getMergedProperties() );
	}
	
	@Test
	public void testRuntimePropertiesOverrideLocationsProperty() {
		ExtendedPropertyPlaceholderConfigurer.addGlobalProperty( "override_key", "runtime config" );
		context = new ClassPathXmlApplicationContext( "/org/red5/spring/placeholder_context.xml" );
		ExtendedPropertyPlaceholderConfigurer configurer = 
			(ExtendedPropertyPlaceholderConfigurer)context.getBean( "wildcard2PlaceholderConfig" );
		
		assertEquals( 
				"runtime config", configurer.getMergedProperties().getProperty( "override_key" ) );
	}
}
