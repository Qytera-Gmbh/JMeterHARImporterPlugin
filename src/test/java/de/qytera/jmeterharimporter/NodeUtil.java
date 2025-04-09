package de.qytera.jmeterharimporter;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import javax.swing.tree.TreeNode;
import org.apache.jmeter.gui.tree.JMeterTreeNode;

public class NodeUtil {

    private NodeUtil() {
        // Utility class.
    }

    /**
     * Retrieves the first child of a given node which is of the given class.
     *
     * @param node       the node
     * @param childClass the child's class
     * @param <T>        the expected class of the child node
     * @return the child
     */
    public static <T> T getChild(TreeNode node, Class<T> childClass) {
        Enumeration<? extends TreeNode> children = node.children();
        while (children.hasMoreElements()) {
            TreeNode child = children.nextElement();
            if (child instanceof JMeterTreeNode) {
                Object userObject = ((JMeterTreeNode) child).getUserObject();
                if (childClass.isInstance(userObject)) {
                    return childClass.cast(userObject);
                }
            }
        }
        throw new NoSuchElementException(
            String.format("Node does not contain a child of class %s", childClass.getName()));
    }

}
