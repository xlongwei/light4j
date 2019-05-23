package com.xlongwei.light4j.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/**
 * relation util
 * @author wjb
 *
 */
public final class RelationUtil {
	/**
	 * pack two objects
	 * @param <L>
	 * @param <R>
	 */
	public static class RelationPair<L, R> {
		public L l;
		public R r;

		public RelationPair(L l, R r) {
			set(l, r);
		}

		public void set(L l, R r) {
			this.l = l;
			this.r = r;
		}

		@Override
		public boolean equals(Object obj) {
			return l.equals(obj) && r.equals(obj);
		}

		@Override
		public String toString() {
			return "[" + l.toString() + "," + r.toString() + "]";
		}

		public void print(PrintStream out) {
			print(new PrintWriter(out));
		}

		public void print(PrintWriter out) {
			out.print(l.toString());
			out.print("\t+\t");
			out.println(r.toString());
			out.flush();
		}
	}

	public static interface Relation<L, R> {
		/** add 
		 * @param lo lo
		 * @param ro ro
		 */
		public void add(L lo, R ro);

		/** all @return all */
		public ArrayList<RelationPair<L, R>> all();

		/** clear */
		public void clear();

		/** contains 
		 * @param lo 
		 * @param ro 
		 * @return contains
		 */
		public boolean contains(L lo, R ro);

		/** containsL 
		 * @param lo
		 * @return containsL
		 */
		public boolean containsL(L lo);

		/** containsR 
		 * @param ro
		 * @return containsR
		 */
		public boolean containsR(R ro);

		/** lsize 
		 * @return lsize
		 */
		public int lsize();

		/** print 
		 * @param out
		 * @param isCompact
		 */
		public void print(PrintStream out, boolean isCompact);

		/** print 
		 * @param out
		 * @param isCompact
		 */
		public void print(PrintWriter out, boolean isCompact);

		/** remove 
		 * @param lo
		 * @param ro
		 */
		public void remove(L lo, R ro);

		/** rsize 
		 * @return rsize 
		 */
		public int rsize();
	}

	public static class Relation11<L, R> implements Relation<L, R> {
		public Relation11() {
			// 注意这里不能包含L和R
			init();
		}

		@Override
		public void add(L lo, R ro) {
			// add to lmap
			lmap.put(lo, ro);

			// add to rmap
			rmap.put(ro, lo);
		}

		@Override
		public ArrayList<RelationPair<L, R>> all() {
			ArrayList<RelationPair<L, R>> r = new ArrayList<RelationPair<L, R>>();
			for (L lo : lmap.keySet()) {
				R ro = lmap.get(lo);
				r.add(new RelationPair<L, R>(lo, ro));
			}
			return r;
		}

		public Set<L> allL() {
			return lmap.keySet();
		}

		public Set<R> allR() {
			return rmap.keySet();
		}

		@Override
		public void clear() {
			lmap.clear();
			rmap.clear();
		}

		@Override
		public boolean contains(L lo, R ro) {
			L lo2 = rmap.get(ro);
			if (lo2 == null) {
				return false;
			}
			return lo == lo2;
		}

		@Override
		public boolean containsL(L lo) {
			return lmap.containsKey(lo);
		}

		@Override
		public boolean containsR(R ro) {
			return rmap.containsKey(ro);
		}

		public L getL(R ro) {
			return rmap.get(ro);
		}

		public R getR(L lo) {
			return lmap.get(lo);
		}

		@Override
		public int lsize() {
			return lmap.size();
		}

		@Override
		public void print(PrintStream out, boolean isCompact) {
			print(new PrintWriter(out), isCompact);
		}

		@Override
		public void print(PrintWriter out, boolean isCompact) {
			// print lmap
			out.println("[LMAP]");
			for (L lo : lmap.keySet()) {
				out.print(lo.toString());
				out.print("\t:");
				R ro = lmap.get(lo);
				out.print("\t");
				out.print(ro.toString());
				out.print("\n");
			}

			// print rmap
			out.println("[RMAP]");
			for (R ro : rmap.keySet()) {
				out.print(ro.toString());
				out.print("\t:");
				L lo = rmap.get(ro);
				out.print("\t");
				out.print(lo.toString());
				out.print("\n");
			}

			out.flush();
		}

		@Override
		public void remove(L lo, R ro) {
			if (!contains(lo, ro)) {
				return;
			}
			lmap.remove(lo);
			rmap.remove(ro);
		}

		public void removeL(L lo) {
			R ro = lmap.get(lo);
			if (ro == null) {
				return;
			}
			lmap.remove(lo);
			rmap.remove(ro);
		}

		public void removeR(R ro) {
			L lo = rmap.get(ro);
			if (lo == null) {
				return;
			}
			lmap.remove(lo);
			rmap.remove(ro);
		}

		@Override
		public int rsize() {
			return rmap.size();
		}

		private void init() {
			if (lmap == null) {
				lmap = new Hashtable<L, R>();
			}
			if (rmap == null) {
				rmap = new Hashtable<R, L>();
			}
		}

		Hashtable<L, R> lmap = null;

		Hashtable<R, L> rmap = null;
	}

	/**
	 * this class represents a one way map. L <-> R is a one to many relationship.
	 * @param <L>
	 * @param <R>
	 */
	public static class Relation1N<L, R> implements Relation<L, R> {
		public Relation1N() {
			// 注意这里不能包含L和R
			init();
		}

		@Override
		public void add(L lo, R ro) {
			// add to lmap
			HashSet<R> rd = lmap.get(lo);
			if (rd == null) {
				rd = new HashSet<R>();
				lmap.put(lo, rd);
			}
			rd.add(ro);

			// add to rmap
			rmap.put(ro, lo);
		}

		@Override
		public ArrayList<RelationPair<L, R>> all() {
			ArrayList<RelationPair<L, R>> r = new ArrayList<RelationPair<L, R>>();
			for (R ro : rmap.keySet()) {
				L lo = rmap.get(ro);
				r.add(new RelationPair<L, R>(lo, ro));
			}
			return r;
		}

		public Set<L> allL() {
			return lmap.keySet();
		}

		public Set<R> allR() {
			return rmap.keySet();
		}

		@Override
		public void clear() {
			lmap.clear();
			rmap.clear();
		}

		@Override
		public boolean contains(L lo, R ro) {
			L lo2 = rmap.get(ro);
			if (lo2 == null) {
				return false;
			}
			return lo == lo2;
		}

		@Override
		public boolean containsL(L lo) {
			return lmap.containsKey(lo);
		}

		@Override
		public boolean containsR(R ro) {
			return rmap.containsKey(ro);
		}

		public L getL(R ro) {
			return rmap.get(ro);
		}

		public HashSet<R> getR(L lo) {
			return lmap.get(lo);
		}

		@Override
		public int lsize() {
			return lmap.size();
		}

		@Override
		public void print(PrintStream out, boolean isCompact) {
			print(new PrintWriter(out), isCompact);
		}

		@Override
		public void print(PrintWriter out, boolean isCompact) {
			// print lmap
			out.println("[LMAP]");
			for (L lo : lmap.keySet()) {
				out.print(lo.toString());
				out.print("\t:");
				HashSet<R> rd = lmap.get(lo);
				for (R ro : rd) {
					if (!isCompact) {
						out.print("\n");
					}
					out.print("\t");
					out.print(ro.toString());
				}
				out.print("\n");
			}

			// print rmap
			out.println("[RMAP]");
			for (R ro : rmap.keySet()) {
				out.print(ro.toString());
				out.print("\t:");
				L lo = rmap.get(ro);
				out.print("\t");
				out.print(lo.toString());
				out.print("\n");
			}

			out.flush();
		}

		@Override
		public void remove(L lo, R ro) {
			if (!contains(lo, ro)) {
				return;
			}
			removeRObjectFromLMap(lo, ro);
			removeLObjectFromRMap(lo, ro);
		}

		public void removeL(L lo) {
			HashSet<R> rd = lmap.get(lo);
			if (rd == null) {
				return;
			}
			for (R ro : rd) {
				removeLObjectFromRMap(lo, ro);
			}
			lmap.remove(lo);
		}

		public void removeR(R ro) {
			L lo = rmap.get(ro);
			if (lo == null) {
				return;
			}
			removeRObjectFromLMap(lo, ro);
			rmap.remove(ro);
		}

		@Override
		public int rsize() {
			return rmap.size();
		}

		private void init() {
			if (lmap == null) {
				lmap = new Hashtable<L, HashSet<R>>();
			}
			if (rmap == null) {
				rmap = new Hashtable<R, L>();
			}
		}

		private void removeLObjectFromRMap(L lo, R ro) {
			// here the relation contains lo and ro
			rmap.remove(ro);
		}

		private void removeRObjectFromLMap(L lo, R ro) {
			// here the relation contains lo and ro
			HashSet<R> rd = lmap.get(lo);
			rd.remove(ro);
			if (rd.size() == 0) {
				lmap.remove(lo);
			}
		}

		Hashtable<L, HashSet<R>> lmap = null;

		Hashtable<R, L> rmap = null;
	}

	/**
	 *  this class represents a dual map. L <-> R is a many to many relationship.
	 * @param <L>
	 * @param <R>
	 */
	public static class RelationNn<L, R> implements Relation<L, R> {
		Hashtable<L, HashSet<R>> lmap = null;
		Hashtable<R, HashSet<L>> rmap = null;

		public RelationNn() {
			// 注意这里不能包含L和R
			init();
		}

		private void init() {
			if (lmap == null) {
				lmap = new Hashtable<L, HashSet<R>>();
			}
			if (rmap == null) {
				rmap = new Hashtable<R, HashSet<L>>();
			}
		}

		@Override
		public void add(L lo, R ro) {
			// add to lmap
			HashSet<R> rd = lmap.get(lo);
			if (rd == null) {
				rd = new HashSet<R>();
				lmap.put(lo, rd);
			}
			rd.add(ro);

			// add to rmap
			HashSet<L> ld = rmap.get(ro);
			if (ld == null) {
				ld = new HashSet<L>();
				rmap.put(ro, ld);
			}
			ld.add(lo);
		}

		public Set<L> allL() {
			return lmap.keySet();
		}

		public Set<R> allR() {
			return rmap.keySet();
		}

		@Override
		public boolean contains(L lo, R ro) {
			HashSet<L> ld = rmap.get(ro);
			if (ld == null) {
				return false;
			}
			return ld.contains(lo);
		}

		@Override
		public boolean containsL(L lo) {
			return lmap.containsKey(lo);
		}

		@Override
		public boolean containsR(R ro) {
			return rmap.containsKey(ro);
		}

		public HashSet<L> getL(R ro) {
			return rmap.get(ro);
		}

		public HashSet<R> getR(L lo) {
			return lmap.get(lo);
		}

		private void removeRObjectFromLMap(L lo, R ro) {
			// here the relation contains lo and ro
			HashSet<R> rd = lmap.get(lo);
			rd.remove(ro);
			if (rd.size() == 0) {
				lmap.remove(lo);
			}
		}

		private void removeLObjectFromRMap(L lo, R ro) {
			// here the relation contains lo and ro
			HashSet<L> ld = rmap.get(ro);
			ld.remove(lo);
			if (ld.size() == 0) {
				rmap.remove(ro);
			}
		}

		@Override
		public void remove(L lo, R ro) {
			if (!contains(lo, ro)) {
				return;
			}
			removeRObjectFromLMap(lo, ro);
			removeLObjectFromRMap(lo, ro);
		}

		public void removeL(L lo) {
			HashSet<R> rd = lmap.get(lo);
			if (rd == null) {
				return;
			}
			for (R ro : rd) {
				removeLObjectFromRMap(lo, ro);
			}
			lmap.remove(lo);
		}

		public void removeR(R ro) {
			HashSet<L> ld = rmap.get(ro);
			if (ld == null) {
				return;
			}
			for (L lo : ld) {
				removeRObjectFromLMap(lo, ro);
			}
			rmap.remove(ro);
		}

		@Override
		public void print(PrintStream out, boolean isCompact) {
			print(new PrintWriter(out), isCompact);
		}

		@Override
		public void print(PrintWriter out, boolean isCompact) {
			// print lmap
			out.println("[LMAP]");
			for (L lo : lmap.keySet()) {
				out.print(lo.toString());
				out.print("\t:");
				HashSet<R> rd = lmap.get(lo);
				for (R ro : rd) {
					if (!isCompact) {
						out.print("\n");
					}
					out.print("\t");
					out.print(ro.toString());
				}
				out.print("\n");
			}

			// print rmap
			out.println("[RMAP]");
			for (R ro : rmap.keySet()) {
				out.print(ro.toString());
				out.print("\t:");
				HashSet<L> ld = rmap.get(ro);
				for (L lo : ld) {
					if (!isCompact) {
						out.print("\n");
					}
					out.print("\t");
					out.print(lo.toString());
				}
				out.print("\n");
			}

			out.flush();
		}

		@Override
		public ArrayList<RelationPair<L, R>> all() {
			ArrayList<RelationPair<L, R>> r = new ArrayList<RelationPair<L, R>>();
			for (L lo : lmap.keySet()) {
				HashSet<R> rd = lmap.get(lo);
				for (R ro : rd) {
					r.add(new RelationPair<L, R>(lo, ro));
				}
			}
			return r;
		}

		@Override
		public void clear() {
			lmap.clear();
			rmap.clear();
		}

		@Override
		public int lsize() {
			return lmap.size();
		}

		@Override
		public int rsize() {
			return rmap.size();
		}
	}

}
