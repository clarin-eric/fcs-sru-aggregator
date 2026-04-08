package eu.clarin.sru.fcs.aggregator.app.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class IgnoreNonNullIfNotEnabledValidator implements ConstraintValidator<IgnoreNotNullIfNotEnabled, Object> {

    // https://github.com/hibernate/hibernate-validator/blob/main/engine/src/main/java/org/hibernate/validator/internal/constraintvalidators/bv/NotNullValidator.java
    // https://github.com/hibernate/hibernate-validator/blob/main/engine/src/main/java/org/hibernate/validator/internal/metadata/core/ConstraintHelper.java

    private String enabledFieldName;

    @Override
    public void initialize(IgnoreNotNullIfNotEnabled constraintAnnotation) {
        enabledFieldName = constraintAnnotation.enabledFieldName();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // null values are valid
        if (value == null) {
            return true;
        }

        final boolean isEnabled;
        try {
            isEnabled = (boolean) getPropertyValue(value, enabledFieldName);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | IntrospectionException e) {
            // we did not find the property?
            return true;
        }

        if (!isEnabled) {
            return true;
        }

        Set<Field> fields = findFieldsWithAnnotation(value.getClass(),
                IgnoreNotNullIfNotEnabled.IgnorableNotNull.class);

        for (final Field field : fields) {
            try {
                Object fieldValue = getFieldValue(value, field);

                if (fieldValue == null) {
                    createViolation(context, field.getName());
                    return false;
                }

            } catch (IllegalArgumentException | IllegalAccessException e) {
                // ignore if we fail?
                return true;
            }
        }

        Set<Method> methods = findGettersWithAnnotation(value.getClass(),
                IgnoreNotNullIfNotEnabled.IgnorableNotNull.class);

        for (final Method method : methods) {
            try {
                Object getterValue = getGetterValue(value, method);

                if (getterValue == null) {
                    createViolation(context, method.getName());
                    return false;
                }

            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                // ignore if we fail?
                return true;
            }
        }

        return true;
    }

    private static void createViolation(ConstraintValidatorContext context, String propertyNodeName) {
        context.disableDefaultConstraintViolation();

        // HibernateConstraintValidatorContext hibernateContext =
        // context.unwrap(HibernateConstraintValidatorContext.class);

        // TODO: check how to use default messages from hibernate

        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(propertyNodeName)
                .addConstraintViolation();
    }

    // ----------------------------------------------------------------------

    private static Set<Field> findFieldsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        Set<Field> fields = new HashSet<>();

        Class<?> curClazz = clazz;
        while (curClazz != null) {
            for (final Field field : curClazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(annotation)) {
                    fields.add(field);
                }
            }
            curClazz = curClazz.getSuperclass();
        }

        return fields;
    }

    private static Set<Method> findGettersWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        Set<Method> methods = new HashSet<>();

        Class<?> curClazz = clazz;
        while (curClazz != null) {
            for (final Method method : curClazz.getMethods()) {
                // if (!method.getName().startsWith("get")) { continue; }
                if (method.getParameterCount() > 0) {
                    continue;
                }

                if (method.isAnnotationPresent(annotation)) {
                    methods.add(method);
                }
            }
            curClazz = curClazz.getSuperclass();
        }

        return methods;
    }

    private static Object getFieldValue(Object bean, Field field)
            throws IllegalArgumentException, IllegalAccessException {
        field.setAccessible(true);
        return field.get(bean);
    }

    public static Object getGetterValue(Object bean, Method method)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        method.setAccessible(true);
        return method.invoke(bean);
    }

    // ----------------------------------------------------------------------
    // credit: https://stackoverflow.com/a/19402398/9360161, CC BY-SA 3.0

    // https://commons.apache.org/proper/commons-beanutils/

    private Object getPropertyValue(Object bean, String property)
            throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?> beanClass = bean.getClass();
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(beanClass, property);
        if (propertyDescriptor == null) {
            throw new IllegalArgumentException("No such property " + property + " for " + beanClass + " exists");
        }

        Method readMethod = propertyDescriptor.getReadMethod();
        if (readMethod == null) {
            throw new IllegalStateException("No getter available for property " + property + " on " + beanClass);
        }
        return readMethod.invoke(bean);
    }

    private PropertyDescriptor getPropertyDescriptor(Class<?> beanClass, String propertyName)
            throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        PropertyDescriptor propertyDescriptor = null;
        for (int i = 0; i < propertyDescriptors.length; i++) {
            PropertyDescriptor currentPropertyDescriptor = propertyDescriptors[i];
            if (currentPropertyDescriptor.getName().equals(propertyName)) {
                propertyDescriptor = currentPropertyDescriptor;
            }
        }
        return propertyDescriptor;
    }

}
