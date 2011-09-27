/*
 * JBoss, Home of Professional Open Source Copyright 2010, Red Hat, Inc., and
 * individual contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.jboss.logging.generator.model;

import com.sun.codemodel.internal.JBlock;
import com.sun.codemodel.internal.JClass;
import com.sun.codemodel.internal.JCodeModel;
import com.sun.codemodel.internal.JExpr;
import com.sun.codemodel.internal.JFieldVar;
import com.sun.codemodel.internal.JInvocation;
import com.sun.codemodel.internal.JMethod;
import com.sun.codemodel.internal.JMod;
import com.sun.codemodel.internal.JVar;
import org.jboss.logging.generator.intf.model.MessageInterface;
import org.jboss.logging.generator.intf.model.Method;
import org.jboss.logging.generator.intf.model.Parameter;
import org.jboss.logging.generator.intf.model.ThrowableType;

import java.io.Serializable;
import java.util.Arrays;

import static org.jboss.logging.generator.model.ClassModelHelper.formatMessageId;
import static org.jboss.logging.generator.model.ClassModelHelper.implementationClassName;

/**
 * An abstract code model to create the source file that implements the
 * interface.
 * <p/>
 * <p>
 * Essentially this uses the com.sun.codemodel.internal.JCodeModel to generate the
 * source files with. This class is for convenience in generating default source
 * files.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class ImplementationClassModel extends ClassModel {

    /**
     * Class constructor.
     *
     * @param messageInterface the message interface to implement.
     */
    ImplementationClassModel(final MessageInterface messageInterface) {
        super(messageInterface, implementationClassName(messageInterface), null);
    }

    @Override
    protected JCodeModel generateModel() throws IllegalStateException {
        JCodeModel codeModel = super.generateModel();
        getDefinedClass()._implements(codeModel.ref(Serializable.class));
        // Add the serializable UID
        JFieldVar serialVersionUID = getDefinedClass().field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, codeModel.LONG, "serialVersionUID");
        serialVersionUID.init(JExpr.lit(1L));

        return codeModel;
    }

    /**
     * Create the bundle method body.
     *
     * @param messageMethod  the message method.
     * @param method         the method to create the body for.
     * @param msgMethod      the message method for retrieving the message.
     * @param projectCodeVar the project code variable
     */
    void createBundleMethod(final Method messageMethod, final JMethod method, final JMethod msgMethod, final JVar projectCodeVar) {
        addThrownTypes(messageMethod, method);
        // Create the body of the method and add the text
        final JBlock body = method.body();
        final JClass returnField = getCodeModel().ref(method.type().fullName());
        final JVar result = body.decl(returnField, "result");
        final Method.Message message = messageMethod.message();
        final JClass formatter = getCodeModel().ref(message.format().formatClass());
        final JInvocation formatterMethod = formatter.staticInvoke(message.format().staticMethod());
        if (messageMethod.allParameters().isEmpty()) {
            // If the return type is an exception, initialize the exception.
            if (messageMethod.returnType().isThrowable()) {
                if (message.hasId() && projectCodeVar != null) {
                    String formattedId = formatMessageId(message.id());
                    formatterMethod.arg(projectCodeVar.plus(JExpr.lit(formattedId)).plus(JExpr.invoke(msgMethod)));
                    initCause(result, returnField, body, messageMethod, formatterMethod);
                } else {
                    initCause(result, returnField, body, messageMethod, JExpr.invoke(msgMethod));
                }
            } else {
                result.init(JExpr.invoke(msgMethod));
            }
        } else {
            if (message.hasId() && projectCodeVar != null) {
                String formattedId = formatMessageId(message.id());
                formatterMethod.arg(projectCodeVar.plus(JExpr.lit(formattedId)).plus(JExpr.invoke(msgMethod)));
            } else {
                formatterMethod.arg(JExpr.invoke(msgMethod));
            }
            // Create the parameters
            for (Parameter param : messageMethod.allParameters()) {
                final JClass paramType = getCodeModel().ref(param.type());
                final JVar var = method.param(JMod.FINAL, paramType, param.name());
                final String formatterClass = param.getFormatterClass();
                if (param.isFormatParam()) {
                    if (formatterClass == null) {
                        formatterMethod.arg(var);
                    } else {
                        formatterMethod.arg(JExpr._new(getCodeModel().ref(formatterClass)).arg(var));
                    }
                }
            }
            // Setup the return type
            if (messageMethod.returnType().isThrowable()) {
                initCause(result, returnField, body, messageMethod, formatterMethod);
            } else {
                result.init(formatterMethod);
            }
        }
        body._return(result);
    }

    /**
     * Initialize the cause (Throwable) return type.
     *
     * @param result          the return variable
     * @param returnField     the return field
     * @param body            the body of the method
     * @param method          the message method
     * @param formatterMethod the formatter method used to format the string cause
     */
    private void initCause(final JVar result, final JClass returnField, final JBlock body, final Method method, final JInvocation formatterMethod) {
        final ThrowableType returnType = method.returnType().throwableReturnType();
        if (returnType.useConstructionParameters()) {
            final JInvocation invocation = JExpr._new(returnField);
            for (Parameter param : returnType.constructionParameters()) {
                if (param.isMessage()) {
                    invocation.arg(formatterMethod);
                } else {
                    invocation.arg(JExpr.ref(param.name()));
                }
            }
            result.init(invocation);
        } else if (returnType.hasStringAndThrowableConstructor() && method.hasCause()) {
            result.init(JExpr._new(returnField).arg(formatterMethod).arg(JExpr.ref(method.cause().name())));
        } else if (returnType.hasThrowableAndStringConstructor() && method.hasCause()) {
            result.init(JExpr._new(returnField).arg(JExpr.ref(method.cause().name())).arg(formatterMethod));
        } else if (returnType.hasStringConstructor()) {
            result.init(JExpr._new(returnField).arg(formatterMethod));
            if (method.hasCause()) {
                JInvocation initCause = body.invoke(result, "initCause");
                initCause.arg(JExpr.ref(method.cause().name()));
            }
        } else if (returnType.hasThrowableConstructor() && method.hasCause()) {
            result.init(JExpr._new(returnField).arg(JExpr.ref(method.cause().name())));
        } else if (returnType.hasStringAndThrowableConstructor() && !method.hasCause()) {
            result.init(JExpr._new(returnField).arg(formatterMethod).arg(JExpr._null()));
        } else if (returnType.hasThrowableAndStringConstructor() && !method.hasCause()) {
            result.init(JExpr._new(returnField).arg(JExpr._null()).arg(formatterMethod));
        } else if (method.hasCause()) {
            result.init(JExpr._new(returnField));
            JInvocation initCause = body.invoke(result, "initCause");
            initCause.arg(JExpr.ref(method.cause().name()));
        } else {
            result.init(JExpr._new(returnField));
        }
        final JClass arrays = getCodeModel().ref(Arrays.class);
        final JClass stClass = getCodeModel().ref(StackTraceElement.class).array();
        final JVar st = body.decl(stClass, "st").init(result.invoke("getStackTrace"));
        final JInvocation setStackTrace = result.invoke("setStackTrace");
        setStackTrace.arg(arrays.staticInvoke("copyOfRange").arg(st).arg(JExpr.lit(1)).arg(st.ref("length")));
        body.add(setStackTrace);
    }

    protected final void addThrownTypes(final Method method, final JMethod jMethod) {
        for (ThrowableType thrownType : method.thrownTypes()) {
            jMethod._throws(getCodeModel().ref(thrownType.name()));
        }
    }
}
