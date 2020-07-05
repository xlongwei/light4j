package com.xlongwei.light4j.util;

/** 卡bin搜索 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class CardBin<E> {
	
	/** 搜索bin码 */
	public E get(String bin) {
		if(bin != null && bin.length() > 0) {
			Node<E> cn = root;
			for(char c : bin.toCharArray()) {
				if(cn.children == null) {
					break;
				}else {
					boolean found = false;
					for(Node<E> n : cn.children) {
						if(n.c == c) {
							cn = n;
							found = true;
						}
					}
					if(found == false) {
						break;
					}
				}
			}
			while(cn!=null && cn.data==null && cn.parent!=null) cn = cn.parent;
			return cn == null ? null : cn.data;
		}
		return null;
	}

	/** 添加bin码对应数据 */
	public void add(String bin, E data) {
		char[] cs = bin.toCharArray();
		Node<E> cn = root;
		for(char c : cs) {
			if(cn.children != null) {
				boolean found = false;
				for(Node<E> n : cn.children) {
					if(n.c == c) {
						cn = n;
						found = true;
						break;
					}
				}
				if(found == false) {
					Node[] copy = new Node[cn.children.length+1];
					System.arraycopy(cn.children, 0, copy, 0, cn.children.length);
					Node<E> n = new Node<>();
					n.c = c;
					n.parent = cn;
					copy[cn.children.length] = n;
					cn.children = copy;
					cn = n;
				}
			}else {
				Node<E> n = new Node<>();
				n.c = c;
				n.parent = cn;
				cn.children = new Node[] { n };
				cn = n;
			}
		}
		cn.data = data;
	}
	
	Node<E> root = new Node<>();
	static class Node<E> {
		char c = 0;
		Node<E>[] children;
		Node<E> parent;
		E data;
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			Node<?> cn = this;
			do {
				sb.append(cn.c);
				cn = cn.parent;
			}while(cn != null);
			return sb.reverse().toString();
		}
	}
}
