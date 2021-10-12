package net.schwarzbaer.java.games.snowrunner;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Function;

class MapTypes {

	public static class SetMap<MapKeyType,SetValueType> extends HashMap<MapKeyType,HashSet<SetValueType>> {
		private static final long serialVersionUID = -6897179951968079373L;
		private final Comparator<? super MapKeyType> compMapKeyType;
		private final Comparator<? super SetValueType> compSetValueType;
		
		SetMap(Comparator<? super MapKeyType> compMapKeyType, Comparator<? super SetValueType> compSetValueType) {
			this.compMapKeyType = compMapKeyType;
			this.compSetValueType = compSetValueType;
		}
		
		void add(MapKeyType key, SetValueType value) {
			if (key==null) new IllegalArgumentException();
			HashSet<SetValueType> set = get(key);
			if (set==null) put(key, set = new HashSet<>());
			set.add(value);
		}
		
		void print(PrintStream out, String label) {
			out.printf("%s:%n", label);
			Vector<MapKeyType> keys = new Vector<>(keySet());
			keys.sort(compMapKeyType);
			for (MapKeyType key : keys) {
				out.printf("   %s%n", key);
				Vector<SetValueType> values = new Vector<>(get(key));
				values.sort(compSetValueType);
				for (SetValueType value : values)
					out.printf("      %s%n", value);
			}
		}
	}
	
	public static class StringVectorMap<ValueType> extends VectorMap<String,ValueType> {
		private static final long serialVersionUID = -8709491018088867713L;

		void printTo(PrintStream out, Function<ValueType, String> valueToStr) {
			super.printTo(out, valueToStr, key->String.format("\"%s\"", key), null);
		}

		void removeEmptyLists() {
			for (String key : new Vector<>(keySet())) {
				Vector<ValueType> list = get(key);
				if (list.isEmpty())
					remove(key);
			}
		}
	}
	
	public static class VectorMap<KeyType,ValueType> extends HashMap<KeyType,Vector<ValueType>> {
		private static final long serialVersionUID = -5963711992044437609L;

		void add(KeyType key, ValueType value) {
			Vector<ValueType> list = get(key);
			if (list==null) put(key, list = new Vector<>());
			list.add(value);
		}
		
		void addAll(KeyType key, Collection<ValueType> values) {
			Vector<ValueType> list = get(key);
			if (list==null) put(key, list = new Vector<>());
			list.addAll(values);
		}

		void forEachPair(BiConsumer<KeyType,ValueType> action) {
			forEach((key,list)->list.forEach(value->action.accept(key, value)));
		}

		void printTo(PrintStream out, Function<ValueType,String> valueToStr, Function<KeyType,String> keyToStr, Comparator<KeyType> keyOrder) {
			Vector<KeyType> keys = new Vector<>( keySet() );
			keys.sort(keyOrder);
			for (KeyType key : keys) {
				Vector<ValueType> list = get(key);
				out.printf("   \"%s\" [%d]%n", keyToStr.apply(key), list.size());
				for (ValueType value : list)
					out.printf("      %s%n", valueToStr.apply(value));
			}
		}
	}
	
	public static class VectorMapMap<KeyType1,KeyType2,ValueType> extends HashMap<KeyType1,HashMap<KeyType2,Vector<ValueType>>> {
		private static final long serialVersionUID = -1085811850916454661L;

		void add(KeyType1 key1, KeyType2 key2, ValueType value) {
			
			HashMap<KeyType2, Vector<ValueType>> map2 = get(key1);
			if (map2==null) put(key1, map2 = new HashMap<>());
			
			Vector<ValueType> vector = map2.get(key2);
			if (vector==null) map2.put(key2, vector = new Vector<>());
			
			vector.add(value);
		}
	}
	
}
