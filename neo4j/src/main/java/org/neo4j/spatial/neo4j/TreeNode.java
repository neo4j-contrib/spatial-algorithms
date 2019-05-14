package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Node;
import org.neo4j.spatial.algo.Within;
import org.neo4j.spatial.core.Polygon;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    private Polygon.SimplePolygon polygon;
    private Node startWay;
    private TreeNode parent;
    private List<TreeNode> children;

    public TreeNode() {}

    public TreeNode(Polygon.SimplePolygon polygon, Node startWay) {
        this.polygon = polygon;
        this.startWay = startWay;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    public boolean addPolygon(TreeNode otherTreeNode) {
        if (Within.within(otherTreeNode.getPolygon(), this.getPolygon().getPoints()[0])) {
            if (this.parent != null) {
                this.parent.removeChild(this);
                this.parent.addChild(otherTreeNode);
            }
            otherTreeNode.setParent(this.parent);
            otherTreeNode.addChild(this);
            return true;
        } else if (!Within.within(this.getPolygon(), otherTreeNode.getPolygon().getPoints()[0])) {
            return false;
        }

        List<TreeNode> containedInOther = new ArrayList<>();
        for (TreeNode child : this.children) {
            if (Within.within(child.getPolygon(), otherTreeNode.getPolygon().getPoints()[0])) {
                child.addPolygon(otherTreeNode);
                return true;
            } else if (Within.within(otherTreeNode.getPolygon(), child.getPolygon().getPoints()[0])) {
                containedInOther.add(child);
            }
        }

        if (containedInOther.size() > 0) {
            for (TreeNode child : containedInOther) {
                otherTreeNode.addChild(child);
                this.removeChild(child);
            }
        }

        this.addChild(otherTreeNode);
        return true;
    }

    public Node getStartWay() {
        return this.startWay;
    }

    public Polygon.SimplePolygon getPolygon() {
        return this.polygon;
    }

    public void setParent(TreeNode treeNode) {
        this.parent = treeNode;
    }

    public void addChild(TreeNode treeNode) {
        this.children.add(treeNode);
        treeNode.setParent(this);
    }

    public void removeChild(TreeNode treeNode) {
        this.children.remove(treeNode);
    }

    public TreeNode getParent() {
        return this.parent;
    }

    public List<TreeNode> getChildren() {
        return this.children;
    }

    @Override
    public String toString() {
        return "TreeNode{" + polygon.toWKT() + '}';
    }
}