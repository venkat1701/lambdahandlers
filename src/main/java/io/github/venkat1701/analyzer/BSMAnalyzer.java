package io.github.venkat1701.analyzer;

import org.objectweb.asm.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;

public class BSMAnalyzer {

    private static Logger logger = Logger.getLogger(BSMAnalyzer.class.getName());
    public static void analyze(String classname) throws IOException {
        String classPath = "target/classes/" + classname;
        byte[] classBytes = Files.readAllBytes(Paths.get(classPath));

        ClassReader reader = new ClassReader(classBytes);
        reader.accept(new ClassVisitor(Opcodes.ASM9){
            @Override
            public MethodVisitor visitMethod(int access, String name, String description, String signature, String[] exceptions) {
                logger.info("Method: " + name + " " + description + " " + signature + " " + Arrays.toString(exceptions));
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
                        logger.info("InvokeDynamic: "+name+" : "+desc);
                        logger.info("BSM: "+bsm.getOwner() + " " + bsm.getName() + " " + bsm.getDesc());

                    }
                };
            }
        }, 0);
    }

    public static void main(String[] args) throws IOException {
        analyze("io/github/venkat1701/tests/LambdaTestCases.class");
    }
}
