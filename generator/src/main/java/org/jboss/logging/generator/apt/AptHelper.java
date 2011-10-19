package org.jboss.logging.generator.apt;

import org.jboss.logging.generator.Annotations.FormatType;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Date: 24.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface AptHelper {

    /**
     * Returns the method format type.
     *
     * @param method the method with the Message annotation.
     *
     * @return the format type of the message or {@code null} if the format type
     *         was not found.
     */
    FormatType messageFormat(ExecutableElement method);

    /**
     * The project code from the interface.
     *
     * @param intf the interface to find the project code on.
     *
     * @return the project code or {@code null} if one was not found.
     */
    String projectCode(TypeElement intf);

    /**
     * Checks to see if the method has a message id.
     *
     * @param method the method to check.
     *
     * @return {@code true} if the message id was found, otherwise {@code false}.
     */
    boolean hasMessageId(ExecutableElement method);

    /**
     * Checks to see if the method should inherit the message id from a different method if applicable.
     *
     * @param method the method to check.
     *
     * @return {@code true} if the message id should be inherited, otherwise {@code false}.
     */
    boolean inheritsMessageId(ExecutableElement method);


    /**
     * Returns the message id.
     *
     * @param method the method to check.
     *
     * @return the message id or 0 if one was not found.
     */
    int messageId(ExecutableElement method);

    /**
     * Returns the message value for the method.
     *
     * @param method the method to check.
     *
     * @return the message for the method, if no method found {@code null} is
     *         returned.
     */
    String messageValue(ExecutableElement method);

    /**
     * Returns the logger method name to use or an empty string if the method is not a logger method.
     *
     * @param formatType the format type for the method.
     *
     * @return the name of the logger method or an empty string.
     */
    String loggerMethod(FormatType formatType);

    /**
     * Returns the log level enum. For example Logger.Level.INFO.
     *
     * @param method the method used to determine the log method.
     *
     * @return the log level.
     */
    String logLevel(ExecutableElement method);

    /**
     * Returns the target field or method name for the annotated parameter. If the parameter is not annotated with
     * either {@link org.jboss.logging.generator.Annotations#field()} or
     * {@link org.jboss.logging.generator.Annotations#property()} an empty string should be returned.
     * <p/>
     * If the parameter is annotated with {@link org.jboss.logging.generator.Annotations#property()}, the name should
     * be prepended with {@code set}. For example a property name of {@code value} should return {@code setValue}.
     * <p/>
     * If the annotation does not have a defined value, the parameter name should be returned.
     *
     * @param param the parameter to check for the annotation.
     *
     * @return the field, method name or an empty string.
     */
    String targetName(VariableElement param);
}
