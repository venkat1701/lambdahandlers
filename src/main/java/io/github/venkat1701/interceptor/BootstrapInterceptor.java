package io.github.venkat1701.interceptor;

import net.bytebuddy.implementation.bytecode.Throw;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;


/**
 * This class is responsible for intercepting any lambda expressions in our code during the class loading and rewrite their bootstrap method to use our custom implementation of
 * lambda factory. It makes use of the ASM library for manipulating with bytecode. It serves as the entry point and hook itself into the JVM at startup to modify bytecode before the app runs.
 * @author Krish Jaiswal
 * @version 1.0z
 */
public class BootstrapInterceptor {

    /**
     * This method is automatically invoked by the JVM before the main() method when we use -javaagent. Here we
     * are trying to register a ClassFileTransformer that intercepts and modifies classes as they are loaded in the class loader.
     * @param args
     * @param instrumentation
     */
    public static void premain(String args, Instrumentation instrumentation) {
        instrumentation.addTransformer(new LambdaTransformer());
    }

    /**
     * This class implements the ClassFileTransformer, meaning that it's basically a hook into the JVm's class loading phase.
     */
    public static class LambdaTransformer implements ClassFileTransformer {
        /**
         * At the core of our transformer, for every loaded class, this method is called with its raw bytecode.
         * @param loader                the defining loader of the class to be transformed,
         *                              may be {@code null} if the bootstrap loader
         * @param className             the name of the class in the internal form of fully
         *                              qualified class and interface names as defined in
         *                              <i>The Java Virtual Machine Specification</i>.
         *                              For example, <code>"java/util/List"</code>.
         * @param classBeingRedefined   if this is triggered by a redefine or retransform,
         *                              the class being redefined or retransformed;
         *                              if this is a class load, {@code null}
         * @param protectionDomain      the protection domain of the class being defined or redefined
         * @param classfileBuffer       the input byte buffer in class file format - must not be modified
         *
         * @return
         * @throws IllegalClassFormatException
         */
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            // we don't want to tamper with critical JVM internal classes, which might break things.
            if(className.startsWith("java/") || className.startsWith("jdk/") || className.startsWith("sun")) {
                return null;
            }

            try {
                // parses  the byte array of the class
                ClassReader cr = new ClassReader(classfileBuffer);

                // to write the modified version of the class.
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

                // we wrap the reader and writer with a vissitor that can intercept specific events like visiting methods, fields, etc
                ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, writer) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                        return new MethodVisitor(Opcodes.ASM9, mv) {
                            @Override
                            public void visitInvokeDynamicInsn(String name, String descriptor, Handle bsm, Object... bsmArgs) {
                                if(bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory") &&
                                bsm.getName().equals("metafactory")) {
                                    // we check if the current invokedynamic is backed by the standard LambdaMetafactory, if it is, then we create a custom bootstrap method handle to replace the default one.
                                    Handle newBootstrap = new Handle(
                                            Opcodes.H_INVOKESTATIC,
                                            "io/github/venkat1701/DynamicLambdaFactory",
                                            "createLambda",
                                            bsm.getDesc(),
                                            false
                                    );

                                    super.visitInvokeDynamicInsn(name, descriptor, newBootstrap, bsmArgs);
                                } else {
                                    super.visitInvokeDynamicInsn(name, descriptor, bsm, bsmArgs);
                                }
                            }
                        };
                    }
                };

                cr.accept(cv, 0);
                // returns the bytecode that the JVM will load instead of the original one.
                return writer.toByteArray();
            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
