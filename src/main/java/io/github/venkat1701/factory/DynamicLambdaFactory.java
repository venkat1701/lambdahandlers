package io.github.venkat1701.factory;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.lang.invoke.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;

/**
 * Factory for creating dynamic lambda implementation by intercepting and redirection of lambda expressions
 * This is another way we can go aboutt generating call site dynamically for the bootstrap method.
 */
public class DynamicLambdaFactory {
    private static final Logger logger = Logger.getLogger(DynamicLambdaFactory.class.getName());
    public static CallSite createLambda(MethodHandles.Lookup caller,
                                        String invokedName,
                                        MethodType invokedType,
                                        MethodType sameMethodType,
                                        MethodHandle implementationMethod,
                                        MethodType instantiatedMethodType) throws Exception {
        logger.info("[DynamicLambdaFactory] Creating lambda for: "+invokedName);
        logger.info("[DynamicLambdaFactory] Functional interface method type: "+sameMethodType);
        logger.info("[DynamicLambdaFactory] Implementation method: "+implementationMethod);

        Class<?> funcInterface = invokedType.returnType();
        Method samMethod = findSAM(funcInterface);

        Class<?> implClass = new ByteBuddy()
                .subclass(Object.class)
                .implement(funcInterface)
                .method(net.bytebuddy.matcher.ElementMatchers.named(samMethod.getName()))
                .intercept(MethodDelegation.to(new DynamicLambdaImplementation(implementationMethod)))
                .make()
                .load(funcInterface.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();

        Object instance = implClass.getDeclaredConstructor().newInstance();
        return new ConstantCallSite(MethodHandles.constant(funcInterface, instance));
    }

    private static Method findSAM(Class<?> funcInterface) {
        for(Method method : funcInterface.getMethods()) {
            if(Modifier.isAbstract(method.getModifiers())) {
                return method;
            }
        }
        throw new RuntimeException("SAM method not found");
    }

    public static class DynamicLambdaImplementation implements InvocationHandler {

        private final MethodHandle methodHandle;
        public DynamicLambdaImplementation(MethodHandle methodHandle) {
            this.methodHandle = methodHandle;
        }

        @RuntimeType
        public Object invoke(@AllArguments Object[] args) throws Throwable {
            return this.methodHandle.invokeWithArguments(args);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return null;
        }
    }
}
