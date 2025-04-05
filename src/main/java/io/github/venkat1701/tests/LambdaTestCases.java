package io.github.venkat1701.tests;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class LambdaTestCases {
    public void testSimpleLambda() {
        Runnable r = () -> System.out.println("Hello, Lambda!");
        r.run();
    }

    public void testLambdaWithParameter() {
        Function<String, Integer> lengthFunc = s -> s.length();
        int length = lengthFunc.apply("test string");
        System.out.println("Length: " + length);
    }

    public void testLambdaWithCapture() {
        String prefix = "Message: ";
        Function<String, String> prefixer = s -> prefix + s;
        System.out.println(prefixer.apply("Hello"));
    }

    public void testMethodReference() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        names.forEach(System.out::println);
    }

    public void testStreamWithLambda() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        int sum = numbers.stream()
                .filter(n -> n % 2 == 0)
                .mapToInt(n -> n * 2)
                .sum();
        System.out.println("Sum of doubled even numbers: " + sum);
    }
}
