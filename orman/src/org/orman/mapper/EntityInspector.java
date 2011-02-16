package org.orman.mapper;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.orman.mapper.annotation.Id;
import org.orman.mapper.annotation.Index;
import org.orman.mapper.exception.NotDeclaredGetterException;
import org.orman.mapper.exception.NotDeclaredSetterException;

public class EntityInspector {
	
	private Class<?> clazz;
	private List<Field> fields;
	
	public EntityInspector(Class<?> forClass){
		this.clazz = forClass;
		fields = new ArrayList<Field>();
	}

	private List<Field> extractFields(){
		this.fields.clear();
		
		for(java.lang.reflect.Field f : this.clazz.getDeclaredFields()){
			// Only non-`transient` (threatened as volatile) fields
			if(!Modifier.isTransient(f.getModifiers())){
				Field newF = new Field(f.getType(), f.getName());
				
				// Find getters and setters for non-public field.
				int accessibility = f.getModifiers();
				if (! (Modifier.isPublic(accessibility))){
					// if setter does not exist, throw exception.
					Method setter = this.findSetterFor(this.clazz, f.getName());
					if(setter == null)
						throw new NotDeclaredSetterException(f.getName(), this.clazz.getName());
					else 
						newF.setSetterMethod(setter); // bind setter.
					
					// if getter does not exist, throw exception.
					Method getter = this.findGetterFor(this.clazz, f.getName());
					if(getter == null)
						throw new NotDeclaredGetterException(f.getName(), this.clazz.getName());
					else 
						newF.setGetterMethod(getter); // bind getter.
				}
				
				// Recognize @Index annotation.
				if(f.isAnnotationPresent(Index.class)){
					Index ann = f.getAnnotation(Index.class);
					newF.setIndex(new FieldIndexHolder(ann.name(), ann.unique()));
				}
				
				// Recognize @Id annotation (covers @Index)
				if(f.isAnnotationPresent(Id.class)){
					newF.makeId(true);
					
					// if no custom @Index defined create a default
					if(newF.getIndex() == null)
						newF.setIndex(new FieldIndexHolder(null, true));
				}
				fields.add(newF);
			}
		}
		return this.fields;
	}
	
	/**
	 * Tries to find a setter method for given field name within
	 * specified {@link Class}. First match (according to the 
	 * Java naming standards) will be returned, null if not
	 * found.
	 * 
	 * Warning: If setter method does not have exactly 1 argument
	 * in parameter list, it will not be matched.
	 */
	private static Method findSetterFor(Class<?> forClass, final String fieldName) {
		if (fieldName == null || "".equals(fieldName))
			throw new IllegalArgumentException("Field name cannot be empty for finding setter method.");
		
		List<String> methodNameCandidates = new ArrayList<String>(){{
			//"set"+FieldName
			add("set"+Character.toUpperCase(fieldName.charAt(0))+(fieldName.length()>1?fieldName.substring(1):""));
			// "set"+fieldName
			add("set"+fieldName);
			// fieldName
			add(fieldName);
		}};
		Method m = findMethodLike(forClass, methodNameCandidates);
		
		if (m.getParameterTypes().length == 1) // only 1 argument
			return m;
		else return null;
	}
	
	/**
	 * Tries to find a getter method for given field name within
	 * specified {@link Class}. First match (according to the 
	 * Java naming standards) will be returned, null if not
	 * found.
	 * 
	 * Warning: If getter method has arguments in its parameter
	 * list, it will not be matched.
	 */
	private static Method findGetterFor(Class<?> forClass, final String fieldName) {
		if (fieldName == null || "".equals(fieldName))
			throw new IllegalArgumentException("Field name cannot be empty for finding getter method.");
		
		List<String> methodNameCandidates = new ArrayList<String>(){{
			//"get"+FieldName
			add("get"+Character.toUpperCase(fieldName.charAt(0))+(fieldName.length()>1?fieldName.substring(1):""));
			// "get"+fieldName
			add("get"+fieldName);
			// fieldName
			add(fieldName);
		}};
		
		Method m = findMethodLike(forClass, methodNameCandidates);
		
		if (m.getParameterTypes().length == 0
				&& !m.getReturnType().equals(Void.TYPE)) // non-void and no
															// arguments
			return m;
		else return null;
	}
	
	private static Method findMethodLike(Class<?> forClass, List<String> candidateNames){
		for(Method m : forClass.getDeclaredMethods()){ // methods declared only in this class
			if (Modifier.isPublic(m.getModifiers())){ // only if method is public.
				if (candidateNames.indexOf(m.getName()) > -1){ // method exists?
					return m;
				}
			}
		}
		return null;
	}

	public List<Field> getFields(){
		if(this.fields.isEmpty()) return extractFields();
		else return this.fields;
	}
}
