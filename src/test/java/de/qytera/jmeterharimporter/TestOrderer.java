package de.qytera.jmeterharimporter;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Comparator;

// Define the custom test orderer for alphanumeric sorting
public class TestOrderer implements ClassOrderer {

    @Override
    public void orderClasses(ClassOrdererContext context) {
        // Sort the test classes by their class names in alphanumeric order
        Collections.sort(context.getClassDescriptors(), Comparator.comparing(
            descriptor -> descriptor.getTestClass().getName()));
    }
}

