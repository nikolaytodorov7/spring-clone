package spring.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ControllerAdvice {

    /**
     * Alias for the {@link #basePackages} attribute.
     * <p>Allows for more concise annotation declarations &mdash; for example,
     * {@code @ControllerAdvice("org.my.pkg")} is equivalent to
     * {@code @ControllerAdvice(basePackages = "org.my.pkg")}.
     * @since 4.0
     * @see #basePackages
     */
    @AliasFor("basePackages")
    String[] value() default {};

    /**
     * Array of base packages.
     * <p>Controllers that belong to those base packages or sub-packages thereof
     * will be included &mdash; for example,
     * {@code @ControllerAdvice(basePackages = "org.my.pkg")} or
     * {@code @ControllerAdvice(basePackages = {"org.my.pkg", "org.my.other.pkg"})}.
     * <p>{@link #value} is an alias for this attribute, simply allowing for
     * more concise use of the annotation.
     * <p>Also consider using {@link #basePackageClasses} as a type-safe
     * alternative to String-based package names.
     * @since 4.0
     */
    @AliasFor("value")
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages} for specifying the packages
     * in which to select controllers to be advised by the {@code @ControllerAdvice}
     * annotated class.
     * <p>Consider creating a special no-op marker class or interface in each package
     * that serves no purpose other than being referenced by this attribute.
     * @since 4.0
     */
    Class<?>[] basePackageClasses() default {};

    /**
     * Array of classes.
     * <p>Controllers that are assignable to at least one of the given types
     * will be advised by the {@code @ControllerAdvice} annotated class.
     * @since 4.0
     */
    Class<?>[] assignableTypes() default {};

    /**
     * Array of annotation types.
     * <p>Controllers that are annotated with at least one of the supplied annotation
     * types will be advised by the {@code @ControllerAdvice} annotated class.
     * <p>Consider creating a custom composed annotation or use a predefined one,
     * like {@link RestController @RestController}.
     * @since 4.0
     */
    Class<? extends Annotation>[] annotations() default {};

}
