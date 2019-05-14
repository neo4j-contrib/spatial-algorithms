package org.neo4j.spatial.neo4j;

import java.util.ArrayList;
import java.util.List;

class ForestRoot extends TreeNode {
    private List<TreeNode> children;

    public ForestRoot() {
        this.children = new ArrayList<>();
    }

    public void addChild(TreeNode treeNode) {
        this.children.add(treeNode);
        treeNode.setParent(this);
    }

    public void removeChild(TreeNode treeNode) {
        this.children.remove(treeNode);
    }

    public List<TreeNode> getChildren() {
        return this.children;
    }

    public boolean addPolygon(TreeNode otherTreeNode) {
        for (TreeNode child : children) {
            boolean inserted = child.addPolygon(otherTreeNode);
            if (inserted) {
                return true;
            }
        }

        this.addChild(otherTreeNode);
        return true;
    }
}