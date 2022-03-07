package AST;

import java.util.ArrayList;

public class TreeNode {

    public NodeType nodeType;
    public String token; //name
    public String symbol;//节点文法
    public ArrayList<TreeNode> children = new ArrayList<>();
    public TreeNode father = null;
    public int num; //参数内容
    public boolean constnum = false;

    public TreeNode(NodeType tp) {
        this.nodeType = tp;
    }
    public void addTree(TreeNode node) {
        node.father = this;
        children.add(node);
    }

    @Override
    public String toString() {
        String str = "" + this.nodeType + ": " + "'" + this.token + "'" + " = " + this.num + "\n";
        String length = " ";
        for (int i = 0; i < this.children.size(); i ++) {
            length += " ";
        }
        if (this.children == null || this.children.size() == 0) {
            return str;
        }
        for (int i = 0; i < this.children.size(); i ++) {
            str += length + "|______" + this.children.get(i).toString();
        }
        return str;
    }
}
