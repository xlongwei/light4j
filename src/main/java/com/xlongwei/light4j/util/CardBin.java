package com.xlongwei.light4j.util;

/**
 * 卡bin搜索
 * @author xlongwei
 */
public class CardBin {
	
	/** 匹配bin码 */
	public String bin(String bin) {
		if(bin != null && bin.length() > 0) {
			Node cn = root;
			for(char c : bin.toCharArray()) {
				if(cn.child == null) {
					break;
				}else {
					boolean found = false;
					Node n = cn.child;
					while(n != null) {
						if(n.c == c) {
							cn = n;
							found = true;
							break;
						}
						n = n.sibling;
					}
					if(found == false) {
						break;
					}
				}
			}
			return cn == null || cn.child != null ? null : cn.toString();
		}
		return null;
	}

	/** 添加bin码对应数据 */
	public void add(String bin) {
		char[] cs = bin.toCharArray();
		Node cn = root;
		for(char c : cs) {
			if(cn.child != null) {
				boolean found = false;
				Node n = cn.child;
				while(n != null) {
					if(n.c == c) {
						cn = n;
						found = true;
						break;
					}
					n = n.sibling;
				}
				if(found == false) {
					n = cn.child;
					while(n.sibling != null) {
						n = n.sibling;
					}
					n.sibling = new Node();
					n.sibling.c = c;
					n.sibling.parent = cn;
					cn = n.sibling;
				}
			}else {
				cn.child = new Node();
				cn.child.c = c;
				cn.child.parent = cn;
				cn = cn.child;
			}
		}
	}
	
	Node root = new Node();
	static class Node {
		char c = 0;
		Node parent;
		Node child;
		Node sibling;
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			Node cn = this;
			while(cn.parent != null) {
				sb.append(cn.c);
				cn = cn.parent;
			};
			return sb.reverse().toString();
		}
	}
}
