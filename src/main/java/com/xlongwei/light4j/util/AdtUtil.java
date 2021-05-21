package com.xlongwei.light4j.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.RandomUtils;

import com.xlongwei.light4j.util.AdtUtil.Node.PrevNode;
import com.xlongwei.light4j.util.AdtUtil.Node.PriorityNode;

/**
 * @author Hongwei
 * non-threadsafe
 */
@SuppressWarnings("unchecked")
public final class AdtUtil {
	/**
	 * single direction node with data [data, next]
	 */
	public static class Node<E> {
		public Node(E data, Node<E> next) {
			this.data = data;
			this.next = next;
		}
		private E data; 
		private Node<E> next;

		public E getData() {
			return data;
		}
		public void setData(E data) {
			this.data = data;
		}
		public Node<E> getNext() {
			return next;
		}
		public void setNext(Node<E> next) {
			this.next = next;
		}

		/**
		 * double direction node [data, next, prev]
		 */
		public static class PrevNode<E> extends Node<E> {
			public PrevNode(E data, PrevNode<E> next, PrevNode<E> prev) {
				super(data, next);
				this.prev = prev;
			}
			private PrevNode<E> prev;
			@Override
			public PrevNode<E> getNext() {
				return (PrevNode<E>)super.getNext();
			}
			public PrevNode<E> getPrev() {
				return prev;
			}
			public void setPrev(PrevNode<E> prev) {
				this.prev = prev;
			}
		}

		/**
		 * double direction node with data and priority [data, priority, next, prev]
		 */
		public static class PriorityNode<E, P> extends PrevNode<E> {
			public PriorityNode(E data) {
				this(data, null);
			}
			public PriorityNode(E data, P priority) {
				super(data, null, null);
				this.priority = priority;
			}
			@Override
			public PriorityNode<E, P> getNext() {
				return (PriorityNode<E, P>) super.getNext();
			}
			@Override
			public PriorityNode<E, P> getPrev() {
				return (PriorityNode<E, P>) super.getPrev();
			}
			private P priority;
			public P getPriority() {
				return priority;
			}
			public void setPriority(P priority) {
				this.priority = priority;
			} 
		}

		/**
		 * tree node [data, left, right]
		 */
		public static class TreeNode<E> {
			private E data;
			private TreeNode<E> left;
			private TreeNode<E> right;
			public TreeNode(E data, TreeNode<E> left, TreeNode<E> right){
				this.data = data;
				this.left = left;
				this.right = right;
			}
			public E getData() {
				return data;
			}
			public void setData(E data) {
				this.data = data;
			}
			public TreeNode<E> getLeft() {
				return left;
			}
			public void setLeft(TreeNode<E> left) {
				this.left = left;
			}
			public TreeNode<E> getRight() {
				return right;
			}
			public void setRight(TreeNode<E> right) {
				this.right = right;
			}
		}
	}

	/**
	 * 链式堆栈，后进先出，first in last out
	 */
	public static class Stack<E> {
		public Stack() {
			top = null;
			size = 0;
		}

		public boolean isEmpty() {
			return size == 0;
		}

		public E peek() {
			if (isEmpty()) {
				return null;
			} else {
				return top.getData();
			}
		}

		public E pop() {
			if (isEmpty()) {
				return null;
			} else {
				E data = top.getData();
				top = top.getNext();
				size--;
				return data;
			}
		}

		public void push(E data) {
			Node<E> t = new Node<E>(data, top);
			top = t;
			size++;
		}

		public int size() {
			return size;
		}

		@Override
		public String toString() {
			if (size==0) {
				return "[]";
			} else {
				StringBuilder sb = new StringBuilder();
				Node<E> t = top;
				while (t != null) {
					sb.insert(0, t.getData());
					sb.insert(0, ',');
					t = t.getNext();
				}
				sb.setCharAt(0, '[');
				sb.append(']');
				return sb.toString();
			}
		}

		private int size;
		private Node<E> top;
	}

	/**
	 * 链式队列，先进先出，first in first out
	 */
	public static class Queue<E> {
		public Queue() {
			head = tail = null;
			size = 0;
		}

		public E deQueue() {
			if (isEmpty()) {
				return null;
			} else {
				E data = head.getData();
				if (size == 1) {
					head = tail = null;
				} else {
					head = head.getNext();
				}
				size--;
				return data;
			}
		}

		public void enQueue(E data) {
			Node<E> t = new Node<E>(data, null);
			if (isEmpty()) {
				head = tail = t;
			} else {
				tail.setNext(t);
				tail = t;
			}
			size++;
		}

		public boolean isEmpty() {
			return size == 0;
		}

		public E peek() {
			if (isEmpty()) {
				return null;
			} else {
				return head.getData();
			}
		}

		public int size() {
			return size;
		}

		@Override
		public String toString() {
			if (isEmpty()) {
				return "[]";
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append('[');
				Node<E> t = head;
				while (t != null) {
					sb.append(t.getData());
					sb.append(',');
					t = t.getNext();
				}
				sb.setCharAt(sb.length() - 1, ']');
				return sb.toString();
			}
		}

		private int size;
		private Node<E> head;
		private Node<E> tail;

		/**
		 * 双向队列，两端都可进出
		 */
		public static class PrevQueue<E> {
			public PrevQueue() {
				head = tail = null;
				size = 0;
			}
			public void insertHead(E data) {
				PrevNode<E> t = new PrevNode<E>(data, null, null);
				if (isEmpty()) {
					head = tail = t;
				} else {
					head.setPrev(t);
					t.setNext(head);
					head = t;
				}
				size++;
			}
			public void insertTail(E data) {
				PrevNode<E> t = new PrevNode<E>(data, null, null);
				if (isEmpty()) {
					head = tail = t;
				} else {
					tail.setNext(t);
					t.setPrev(tail);
					tail = t;
				}
				size++;
			}
			public boolean isEmpty() {
				return size == 0;
			}
			public E peekHead() {
				if (isEmpty()) {
					return null;
				} else {
					return head.getData();
				}
			}
			public E peekTail() {
				if (isEmpty()) {
					return null;
				} else {
					return tail.getData();
				}
			}
			public E removeHead() {
				if (isEmpty()) {
					return null;
				} else {
					E data = head.getData();
					if (size == 1) {
						head = tail = null;
					} else {
						head = head.getNext();
					}
					size--;
					return data;
				}
			}
			public E removeTail() {
				if (isEmpty()) {
					return null;
				} else {
					E data = tail.getData();
					if (size == 1) {
						head = tail = null;
					} else {
						tail = tail.getPrev();
					}
					size--;
					return data;
				}
			}
			public int size() {
				return size;
			}
			@Override
			public String toString() {
				if (isEmpty()) {
					return "[]";
				} else {
					StringBuilder sb = new StringBuilder();
					sb.append('[');
					Node<E> t = head;
					while (t != null) {
						sb.append(t.getData());
						sb.append(',');
						t = t.getNext();
					}
					sb.setCharAt(sb.length() - 1, ']');
					return sb.toString();
				}

			}

			private int size;
			private PrevNode<E> head;
			private PrevNode<E> tail;
		}

		/**
		 * 优先级队列
		 */
		public static class PriorityQueue<E extends Comparable<E>> extends Queue<E> {
			@Override
			public void enQueue(E data) {
				if (isEmpty()) {
					super.enQueue(data);
				} else {
					Node<E> n = new Node<E>(data, null);
					Node<E> t = super.head;
					if (data.compareTo(t.getData()) < 0) {
						n.setNext(t);
						super.head = n;
					} else {
						while ((t.getNext() != null)
								&& (data.compareTo(t.getNext().getData()) >= 0)) {
							t = t.getNext();
						}
						n.setNext(t.getNext());
						t.setNext(n);
						if(n.getNext() == null) {
							super.tail = n;
						}
					}
					super.size++;
				}
			}
		}

		/**
		 * 带比较器的优先级队列
		 */
		public static class PriorityQueueWithComparator<E> extends Queue<E> {
			public PriorityQueueWithComparator(Comparator<E> comp) {
				this.comp = comp;
			}

			@Override
			public void enQueue(E data) {
				if (isEmpty()) {
					super.enQueue(data);
				} else {
					Node<E> n = new Node<E>(data, null);
					Node<E> t = super.head;
					if (comp.compare(data, t.getData()) < 0) {
						n.setNext(t);
						super.head = n;
					} else {
						while ((t.getNext() != null)
								&& (comp.compare(data, t.getNext().getData()) >= 0)) {
							t = t.getNext();
						}
						n.setNext(t.getNext());
						t.setNext(n);
						if(n.getNext() == null) {
							super.tail = n;
						}
					}
					super.size++;
				}
			}

			private Comparator<E> comp;
		}

		public static class WrapperQueue<E> extends Queue<E> {
			protected Queue<E> queue = null;
			public WrapperQueue(Queue<E> queue) {
				this.queue = queue;
			}
			@Override
			public E deQueue() {
				return queue.deQueue();
			}
			@Override
			public void enQueue(E data) {
				queue.enQueue(data);
			}
			@Override
			public boolean isEmpty() {
				return queue.isEmpty();
			}
			@Override
			public E peek() {
				return queue.peek();
			}
			@Override
			public int size() {
				return queue.size;
			}
			@Override
			public String toString() {
				return queue.toString();
			}
		}
		
		/** 限制大小的队列 */
		public static class LimitSizeQueue<E> extends WrapperQueue<E> {
			private int limitSize = 100;
			public LimitSizeQueue(int limitSize, Queue<E> queue) {
				super(queue);
				this.limitSize = limitSize;
			}
			@Override
			public void enQueue(E data) {
				super.enQueue(data);
				if(queue.size > limitSize) {
					removeTail();
				}
			}
			public E removeTail() {
				if (isEmpty()) {
					return null;
				} else {
					E data = queue.tail.getData();
					if (queue.size == 1) {
						queue.head = queue.tail = null;
					} else {
						Node<E> tmp = queue.head;
						while(tmp.getNext() != queue.tail) {
							tmp = tmp.getNext();
						}
						tmp.setNext(null);
						queue.tail = tmp;
					}
					queue.size--;
					return data;
				}
			}
		}
	}

	/**
	 * a pair<E, P> linkedList
	 */
	public static class PairList<E, P> {
		public PairList() {
			head = tail = null;
			size = 0;
			reset();
		}

		/**
		 * convert map<E, P> to pairlist<E, P>
		 */
		public PairList(Map<E, P> map) {
			this();
			this.putAll(map);
		}

		/**
		 * new pairlist contains something
		 */
		public PairList(PairList<E, P> pairList) {
			this();
			this.putAll(pairList);
		}

		public void clear() {
			head = tail = null;
			size = 0;
			reset();
		}
		public boolean contains(E data) {
			reset();
			while (moveNext()) {
				if (data.equals(getData())) {
					return true;
				}
			}
			return false;
		}
		public boolean contains(E data, P priority) {
			reset();
			while (moveNext()) {
				if (data.equals(getData()) && priority.equals(getPriority())) {
					return true;
				}
			}
			return false;
		}
		public E getData() {
			return cur == null ? null : cur.getData();
		}
		public P getPriority() {
			return cur != null ? cur.getPriority() : null;
		}
		public boolean isEmpty() {
			return size == 0;
		}

		/**
		 * iterate pairlist, usually {@code reset} first
		 * 
		 * @return true if next exists
		 */
		public boolean moveNext() {
			if (reseted) {
				reseted = false;
				return (cur = head) != null;
			} else {
				return cur == null ? false : (cur = cur.getNext()) != null;
			}
		}

		/**
		 * iterate pairlist from end
		 * 
		 * @return true if prev exists
		 */
		public boolean movePrev() {
			if (reseted) {
				reseted = false;
				return (cur = tail) != null;
			} else {
				return cur == null ? false : (cur = cur.getPrev()) != null;
			}
		}

		/**
		 * add pair(data, priority) to pairlist
		 */
		public void put(E data, P priority) {
			PriorityNode<E, P> temp = new PriorityNode<E, P>(data, priority);
			if (size == 0) {
				head = tail = temp;
			} else {
				PriorityNode<E, P> guard = tail;
				while ((guard != null) && (compare(guard, temp) > 0)) {
					guard = guard.getPrev();
				}
				if (guard != null) {
					temp.setNext(guard.getNext());
					if (temp.getNext() != null) {
						temp.getNext().setPrev(temp);
					}
					guard.setNext(temp);
					temp.setPrev(guard);
					if (guard == tail) {
						tail = temp;
					}
				} else {
					temp.setNext(head);
					head.setPrev(temp);
					head = temp;
				}
			}
			size++;
			reset();
		}

		/**
		 * put pair from head
		 * 
		 * @param data
		 * @param priority
		 */
		public void putFromHead(E data, P priority) {
			PriorityNode<E, P> temp = new PriorityNode<E, P>(data, priority);
			if (size == 0) {
				head = tail = temp;
			} else {
				PriorityNode<E, P> guard = head;
				while ((guard != null) && (compare(temp, guard) > 0)) {
					guard = guard.getNext();
				}
				if (guard != null) {
					temp.setPrev(guard.getPrev());
					if (temp.getPrev() != null) {
						temp.getPrev().setNext(temp);
					}
					guard.setPrev(temp);
					temp.setNext(guard);
					if (guard == head) {
						head = temp;
					}
				} else {
					temp.setPrev(tail);
					tail.setNext(temp);
					tail = temp;
				}
			}
			size++;
			reset();
		}

		/**
		 * add a map
		 */
		public void putAll(Map<E, P> map) {
			for (Entry<E, P> entry : map.entrySet()) {
				this.put(entry.getKey(), entry.getValue());
			}
		}

		/**
		 * add a pairlist
		 */
		public void putAll(PairList<E, P> pairList) {
			pairList.reset();
			while (pairList.moveNext()) {
				this.put(pairList.getData(), pairList.getPriority());
			}
		}

		/**
		 * remove data
		 * 
		 * @return priority if data exists, or null
		 */
		public P remove(E data) {
			if (contains(data)) {
				removeCur();
				reset();
				return getPriority();
			} else {
				return null;
			}
		}

		/**
		 * remove pair(data, priority)
		 * 
		 * @return true if target exists and be removed, or false
		 */
		public boolean remove(E data, P priority) {
			if (contains(data, priority)) {
				removeCur();
				reset();
				return true;
			} else {
				return false;
			}
		}

		/**
		 * remove all pairs with data of E
		 */
		public void removeAll(E data) {
			reset();
			while (moveNext()) {
				if (data.equals(getData())) {
					removeCur();
				}
			}
			reset();
		}

		/**
		 * reset for iterate
		 */
		public void reset() {
			cur = null;
			reseted = true;
		}
		public int size() {
			return size;
		}

		/**
		 * @return hash map(E, P)
		 */
		public Map<E, P> toMap() {
			Map<E, P> map = new HashMap<E, P>(16);
			reset();
			while (moveNext()) {
				if (!map.containsKey(getData())) {
					map.put(getData(), getPriority());
				}
			}
			return map;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			reset();
			while (moveNext()) {
				sb.append(getData());
				sb.append(':');
				sb.append(getPriority());
				sb.append(',');
			}
			if (sb.length() > 1) {
				sb.deleteCharAt(sb.length() - 1);
			}
			sb.append(']');
			return sb.toString();
		}

		/**
		 * make data of E unique
		 */
		public void unique() {
			Map<E, P> map = toMap();
			clear();
			putAll(map);
		}

		/**
		 * remove current node
		 */
		private void removeCur() {
			if (cur.getPrev() != null) {
				cur.getPrev().setNext(cur.getNext());
			}
			if (cur.getNext() != null) {
				cur.getNext().setPrev(cur.getPrev());
			}
			if (cur == head) {
				head = cur.getNext();
			}
			if (cur == tail) {
				tail = cur.getPrev();
			}
			size--;
		}

		protected int compare(PriorityNode<E, P> guard, PriorityNode<E, P> temp) {
			return -1;
		}

		private PriorityNode<E, P> head;
		private PriorityNode<E, P> tail;

		private PriorityNode<E, P> cur;

		private int size;

		private boolean reseted;

		/**
		 * sort by priority
		 */
		public static class PriorityPairList<E, P extends Comparable<P>>
				extends PairList<E, P> {

			public PriorityPairList() {
				this(PriorityComparator.ASCEND);
			}

			public PriorityPairList(PriorityComparator priorityComparator) {
				this.priorityComparator = priorityComparator;
			}

			@Override
			protected int compare(PriorityNode<E, P> guard, PriorityNode<E, P> temp) {
				int result = guard.getPriority().compareTo(temp.getPriority());
				switch (priorityComparator) {
				case DESEND:
					result = -result;
				case ASCEND:
				default:
					// remain the result
					break;
				}
				return result;
			}

			private PriorityComparator priorityComparator;

			public static enum PriorityComparator {
				/**
				 * 排序
				 */
				ASCEND,
				DESEND,
			}
		}

		/**
		 * sort by E,P,PE(default),EP in which E means data,P means priority
		 */
		public static class PriorityPairListCompound<E extends Comparable<E>, P extends Comparable<P>>
				extends PairList<E, P> {
			public PriorityPairListCompound() {
				this(CompoundComparator.PE);
			}

			public PriorityPairListCompound(
					CompoundComparator compoundComparator) {
				switch (compoundComparator) {
				case E:
					this.comparator = (o1, o2) -> {
							return o1.getData().compareTo(o2.getData());
					};
					break;
				case P:
					this.comparator = (o1, o2) -> {
							return o1.getPriority().compareTo(o2.getPriority());
					};
					break;
				case PE:
					this.comparator = (o1, o2) -> {
							int pResult = o1.getPriority().compareTo(
									o2.getPriority());
							return pResult == 0 ? o1.getData().compareTo(
									o2.getData()) : pResult;
					};
					break;
				case EP:
				default:
					// first order by E,second order by P
					this.comparator = (o1, o2) -> {
							int eResult = o1.getData().compareTo(o2.getData());
							return eResult == 0 ? o1.getPriority().compareTo(
									o2.getPriority()) : eResult;
					};
					break;
				}
			}

			@Override
			protected int compare(PriorityNode<E, P> guard, PriorityNode<E, P> temp) {
				return comparator.compare(guard, temp);
			}

			private Comparator<PriorityNode<E, P>> comparator;

			public static enum CompoundComparator {
				/**
				 * 组合排序
				 */
				E,
				P,
				EP,
				PE,
			}
		}

		public static class PriorityPairListWithComparator<E, P> extends
				PairList<E, P> {

			public PriorityPairListWithComparator(Comparator<P> comparator) {
				this.comparator = comparator;
			}

			@Override
			protected int compare(PriorityNode<E, P> guard, PriorityNode<E, P> temp) {
				return comparator.compare(guard.getPriority(),
						temp.getPriority());
			}

			private Comparator<P> comparator;

		}

		public static class PriorityPairListWithCompoundComparator<E, P>
				extends PairList<E, P> {
			public PriorityPairListWithCompoundComparator(
					Comparator<PriorityNode<E, P>> comparator) {
				this.comparator = comparator;
			}

			@Override
			protected int compare(PriorityNode<E, P> guard, PriorityNode<E, P> temp) {
				return comparator.compare(guard, temp);
			}

			private Comparator<PriorityNode<E, P>> comparator;
		}
	}

	/**
	 * 排序类
	 */
	public static class SortUtil {
		/**
		 * 冒泡排序，O(n^2)
		 */
		public static <E extends Comparable<? super E>> E[] bubbleSort(E[] arr) {
			// 外层从后向前，每次最大的都浮到最后面
			for (int out = arr.length - 1; out > 1; out--) {
				for (int in = 0; in < out; in++) {
					if (arr[in].compareTo(arr[in + 1]) > 0) {
						swap(arr, in, in + 1);
					}
				}
			}
			return arr;
		}

		/**
		 * 冒泡排序，O(n^2)
		 */
		public static <E> E[] bubbleSort(E[] arr, Comparator<? super E> comp) {
			// 外层从后向前，每次最大的都浮到最后面
			for (int out = arr.length - 1; out > 1; out--) {
				for (int in = 0; in < out; in++) {
					if (comp.compare(arr[in], arr[in + 1]) > 0) {
						swap(arr, in, in + 1);
					}
				}
			}
			return arr;
		}

		/**
		 * 插入排序，O(n^2)，复制的次数太多
		 */
		public static <E extends Comparable<? super E>> E[] insertSort(E[] arr) {
			insertSort(arr, 0, arr.length - 1);
			return arr;
		}

		/**
		 * 插入排序，O(n^2)，复制的次数太多
		 */
		public static <E> E[] insertSort(E[] arr, Comparator<? super E> comp) {
			insertSort(arr, 0, arr.length - 1, comp);
			return arr;
		}

		/**
		 * 归并排序，O(n*log(n))，需要额外的一倍空间
		 * 
		 * @param arr
		 *            需要排序的数组
		 * @return 排好序的数组
		 */
		public static <E extends Comparable<? super E>> E[] mergeSort(E[] arr) {
			E[] temp = arr.clone();
			recMergeSort(arr, temp, 0, arr.length - 1);
			return arr;
		}

		/**
		 * 归并排序，O(n*log(n))，需要额外的一倍空间
		 * 
		 * @param arr
		 *            需要排序的数组
		 * @param comp
		 *            比较器
		 * @return 排好序的数组
		 */
		public static <E> E[] mergeSort(E[] arr, Comparator<? super E> comp) {
			E[] temp = arr.clone();
			recMergeSort(arr, temp, 0, arr.length - 1, comp);
			return arr;
		}

		/**
		 * 划分是快速排序的根本机制，1962年由C.A.R. Hoare发现，O(n*log(n))
		 */
		public static <E extends Comparable<? super E>> E[] quickSort(E[] arr) {
			recQuickSort(arr, 0, arr.length - 1);
			return arr;
		}

		/**
		 * 划分是快速排序的根本机制，1962年由C.A.R. Hoare发现，O(n*log(n))
		 */
		public static <E> E[] quickSort(E[] arr, Comparator<? super E> comp) {
			recQuickSort(arr, 0, arr.length - 1, comp);
			return arr;
		}

		/**
		 * 反转数组
		 */
		public static <E> E[] reverse(E[] arr) {
			// 前arr.length/2个元素与后面交换位置
			int two = 2;
			for (int i = 0; i < arr.length / two; i++) {
				swap(arr, i, arr.length - 1 - i);
			}
			return arr;
		}

		/**
		 * 选择排序，O(n^2)
		 */
		public static <E extends Comparable<? super E>> E[] selectSort(E[] arr) {
			for (int out = arr.length - 1; out > 0; out--) {
				int max = -1;
				// 选择最大的
				for (int in = 0; in <= out; in++) {
					if (max == -1) {
						max = in;
					} else if (arr[in].compareTo(arr[max]) > 0) {
						max = in;
					}
				}
				if (max != out) {
					swap(arr, max, out);
				}
			}
			return arr;
		}

		/**
		 * 选择排序，O(n^2)
		 */
		public static <E> E[] selectSort(E[] arr, Comparator<? super E> comp) {
			for (int out = arr.length - 1; out > 0; out--) {
				int max = -1;
				// 选择最大的
				for (int in = 0; in <= out; in++) {
					if (max == -1) {
						max = in;
					} else if (comp.compare(arr[in], arr[max]) > 0) {
						max = in;
					}
				}
				if (max != out) {
					swap(arr, max, out);
				}
			}
			return arr;
		}

		/**
		 * 希尔排序，1959年由Donald
		 * L.Shell发现，基于插入排序，但时间复杂度为O(n*log(n)^2)，n-增量排序，间隔序列由Knuth提出，计算式为h=3*h+1
		 */
		public static <E extends Comparable<? super E>> E[] shellSort(E[] arr) {
			int inner, outer;
			E temp;
			int h = 1, len = arr.length;
			int three = 3;
			while (h <= len / three) {
				h = three * h + 1;
			}
			while (h > 0) {
				for (outer = h; outer < len; outer++) {
					temp = arr[outer];
					inner = outer;
					while ((inner > h - 1)
							&& (arr[inner - h].compareTo(temp) >= 0)) {
						arr[inner] = arr[inner - h];
						inner -= h;
					}
					arr[inner] = temp;
				}
				h = (h - 1) / three;
			}
			return arr;
		}

		/**
		 * 希尔排序，1959年由Donald
		 * L.Shell发现，基于插入排序，但时间复杂度为O(n*log(n)^2)，n-增量排序，间隔序列由Knuth提出，计算式为h=3*h+1
		 */
		public static <E> E[] shellSort(E[] arr, Comparator<? super E> comp) {
			int inner, outer;
			E temp;
			int h = 1, len = arr.length;
			int three = 3;
			while (h <= len / three) {
				h = three * h + 1;
			}
			while (h > 0) {
				for (outer = h; outer < len; outer++) {
					temp = arr[outer];
					inner = outer;
					while ((inner > h - 1)
							&& (comp.compare(arr[inner - h], temp) >= 0)) {
						arr[inner] = arr[inner - h];
						inner -= h;
					}
					arr[inner] = temp;
				}
				h = (h - 1) / three;
			}
			return arr;
		}

		/**
		 * 交换数组元素
		 */
		public static <E> void swap(E[] arr, int i, int j) {
			E t = arr[i];
			arr[i] = arr[j];
			arr[j] = t;
		}

		private static <E extends Comparable<? super E>> void insertSort(
				E[] arr, int left, int right) {
			int in, out;
			for (out = left + 1; out <= right; out++) {
				E t = arr[out];
				in = out;
				// 把比arr[out]大的都向后挪一个位置
				while ((in > left) && (arr[in - 1].compareTo(t) > 0)) {
					arr[in] = arr[in - 1];
					in--;
				}
				arr[in] = t;
			}
		}

		private static <E> void insertSort(E[] arr, int left, int right,
				Comparator<? super E> comp) {
			int in, out;
			for (out = left + 1; out <= right; out++) {
				E t = arr[out];
				in = out;
				// 把比arr[out]大的都向后挪一个位置
				while ((in > left) && (comp.compare(arr[in - 1], t) > 0)) {
					arr[in] = arr[in - 1];
					in--;
				}
				arr[in] = t;
			}
		}

		/**
		 * 快速函数辅助：三数据项取中法
		 */
		private static <E extends Comparable<? super E>> E medianOfThree(
				E[] arr, int left, int right) {
			int center = (left + right) / 2;
			if (arr[left].compareTo(arr[center]) > 0) {
				swap(arr, left, center);
			}
			if (arr[left].compareTo(arr[right]) > 0) {
				swap(arr, left, right);
			}
			if (arr[center].compareTo(arr[right]) > 0) {
				swap(arr, center, right);
			}
			swap(arr, center, right - 1);
			return arr[right - 1];
		}

		/**
		 * 快速函数辅助：三数据项取中法
		 */
		private static <E> E medianOfThree(E[] arr, int left, int right,
				Comparator<? super E> comp) {
			int center = (left + right) / 2;
			if (comp.compare(arr[left], arr[center]) > 0) {
				swap(arr, left, center);
			}
			if (comp.compare(arr[left], arr[right]) > 0) {
				swap(arr, left, right);
			}
			if (comp.compare(arr[center], arr[right]) > 0) {
				swap(arr, center, right);
			}
			swap(arr, center, right - 1);
			return arr[right - 1];
		}

		/**
		 * 归并辅助函数：合并两个片段
		 */
		private static <E extends Comparable<? super E>> void merge(E[] arr,
				E[] temp, int lowerPtr, int highPtr, int upperBound) {
			int j = 0;
			int lowerBound = lowerPtr;
			int mid = highPtr - 1;
			int n = upperBound - lowerBound + 1;
			while ((lowerPtr <= mid) && (highPtr <= upperBound)) {
				if (arr[lowerPtr].compareTo(arr[highPtr]) < 0) {
					temp[j++] = arr[lowerPtr++];
				} else {
					temp[j++] = arr[highPtr++];
				}
			}
			while (lowerPtr <= mid) {
				temp[j++] = arr[lowerPtr++];
			}
			while (highPtr <= upperBound) {
				temp[j++] = arr[highPtr++];
			}
			for (j = 0; j < n; j++) {
				arr[lowerBound + j] = temp[j];
			}
		}

		/**
		 * 归并辅助函数：合并两个片段
		 */
		private static <E> void merge(E[] arr, E[] temp, int lowerPtr,
				int highPtr, int upperBound, Comparator<? super E> comp) {
			int j = 0;
			int lowerBound = lowerPtr;
			int mid = highPtr - 1;
			int n = upperBound - lowerBound + 1;
			while ((lowerPtr <= mid) && (highPtr <= upperBound)) {
				if (comp.compare(arr[lowerPtr], arr[highPtr]) < 0) {
					temp[j++] = arr[lowerPtr++];
				} else {
					temp[j++] = arr[highPtr++];
				}
			}
			while (lowerPtr <= mid) {
				temp[j++] = arr[lowerPtr++];
			}
			while (highPtr <= upperBound) {
				temp[j++] = arr[highPtr++];
			}
			for (j = 0; j < n; j++) {
				arr[lowerBound + j] = temp[j];
			}
		}

		/**
		 * 快速排序基础，划分并初步排序
		 */
		private static <E extends Comparable<? super E>> int partitionIt(
				E[] arr, int left, int right, E pivot) {
			int leftPtr = left;
			int rightPtr = right - 1;
			while (true) {
				while (arr[++leftPtr].compareTo(pivot) < 0) {
					;
				}
				while (arr[--rightPtr].compareTo(pivot) > 0) {
					;
				}
				if (leftPtr >= rightPtr) {
					break;
				} else {
					swap(arr, leftPtr, rightPtr);
				}
			}
			// 交换leftPtr和pivot
			swap(arr, leftPtr, right - 1);
			return leftPtr;
		}

		/**
		 * 快速排序基础，划分并初步排序
		 */
		private static <E> int partitionIt(E[] arr, int left, int right,
				E pivot, Comparator<? super E> comp) {
			int leftPtr = left;
			int rightPtr = right - 1;
			while (true) {
				while (comp.compare(arr[++leftPtr], pivot) < 0) {
					;
				}
				while (comp.compare(arr[--rightPtr], pivot) > 0) {
					;
				}
				if (leftPtr >= rightPtr) {
					break;
				} else {
					swap(arr, leftPtr, rightPtr);
				}
			}
			swap(arr, leftPtr, right - 1);
			return leftPtr;
		}

		/**
		 * 归并辅助函数：分治递归然后合并
		 */
		private static <E extends Comparable<? super E>> void recMergeSort(
				E[] arr, E[] temp, int lowerBound, int upperBound) {
			if (lowerBound == upperBound) {
				return;
			} else {
				int mid = (lowerBound + upperBound) / 2;
				recMergeSort(arr, temp, lowerBound, mid);
				recMergeSort(arr, temp, mid + 1, upperBound);
				merge(arr, temp, lowerBound, mid + 1, upperBound);
			}
		}

		/**
		 * 归并辅助函数：分治递归然后合并
		 */
		private static <E> void recMergeSort(E[] arr, E[] temp, int lowerBound,
				int upperBound, Comparator<? super E> comp) {
			if (lowerBound == upperBound) {
				return;
			} else {
				int mid = (lowerBound + upperBound) / 2;
				recMergeSort(arr, temp, lowerBound, mid, comp);
				recMergeSort(arr, temp, mid + 1, upperBound, comp);
				merge(arr, temp, lowerBound, mid + 1, upperBound, comp);
			}
		}

		/**
		 * 快速排序辅助函数：划分内排序然后递归
		 */
		private static <E extends Comparable<? super E>> void recQuickSort(
				E[] arr, int left, int right) {
			int size = right - left + 1;
			// Knuth建议切割点为9
			int ten = 10;
			if (size < ten) {
				insertSort(arr, left, right);
			} else {
				// 三数据项取中，解决逆序的低效问题，while中>0的比较也不用了(因为left<pivot)
				E pivot = medianOfThree(arr, left, right);
				int partition = partitionIt(arr, left, right, pivot);
				recQuickSort(arr, left, partition - 1);
				recQuickSort(arr, partition + 1, right);
			}
		}

		/**
		 * 快速排序辅助函数：划分内排序然后递归
		 */
		private static <E> void recQuickSort(E[] arr, int left, int right,
				Comparator<? super E> comp) {
			int size = right - left + 1;
			// Knuth建议切割点为9
			int ten = 10;
			if (size < ten) {
				insertSort(arr, left, right, comp);
			} else {
				// 三数据项取中，解决逆序的低效问题，while中>0的比较也不用了(因为left<pivot)
				E pivot = medianOfThree(arr, left, right, comp);
				int partition = partitionIt(arr, left, right, pivot, comp);
				recQuickSort(arr, left, partition - 1, comp);
				recQuickSort(arr, partition + 1, right, comp);
			}
		}
	}

	public interface Get<E> {
		/**
		 * get E
		 * @return
		 */
		E get();
		
		/** 随机策略选择目标 */
		public static class Random<E> implements Get<E> {
			private List<E> list = new ArrayList<>();
			private int last = 0;
			public Random(Collection<E> es){
				list.addAll(es);
			}
			@Override
			public E get() {
				int p=RandomUtils.nextInt(0, list.size());
				while(p==last && list.size()>1) {
					p=RandomUtils.nextInt(0, list.size());
				}
				return list.get(last=p);
			}
			public void add(E e) {
				list.add(e);
			}
			public void remove(E e) {
				boolean remove = list.remove(e);
				while(remove) {
					remove = list.remove(e);
				}
			}
			public void set(Collection<E> es) {
				list.clear();
				list.addAll(es);
			}
			public int size() { return list.size(); }
		}
		/** 权重负载均衡策略 */
		public static class Balance<E> implements Get<E> {
			private Random<E> random = null;
			public Balance(Collection<E> es){
				random = new Random<>(es);
			}
			@Override
			public E get() {
				return random.get();
			}
			/**
			 * @param weight 0-off 1-on 2-weight
			 */
			public void weight(E e, int weight) {
				random.remove(e);
				while(weight-->0) {
					random.add(e);
				}
			}
		}
		/** 环形无限策略 */
		public static class Round<E> implements Get<E> {
			private PrevNode<E> node;
			public Round(Collection<E> es) {
				for(E e : es) {
					add(e);
				}
			}
			public void add(E e) {
				if(node == null) {
					node = new PrevNode<>(e, null, null);
					node.setPrev(node);
					node.setNext(node);
				}else {
					PrevNode<E> temp = new PrevNode<>(e, node.getNext(), node);
					node.setNext(temp);
					temp.getNext().setPrev(temp);
					node = temp;
				}
			}
			@Override
			public synchronized E get() {
				E e = node.getData();
				node = node.getNext();
				return e;
			}
		}
	}
}
