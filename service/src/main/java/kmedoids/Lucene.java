package kmedoids;
import java.util.*;

public class Lucene {
    private Map<String,HashSet<Integer>> indexMap = new HashMap<String,HashSet<Integer>>();

    void addItem (String s, int idx){
        if(!indexMap.containsKey(s)){
            indexMap.put(s,new HashSet<Integer>());
        }
        indexMap.get(s).add(idx);
    }

    HashMap<Integer, Integer> findItem(HashSet<String> set){
        HashMap<Integer,Integer> res = new HashMap<Integer, Integer>();
        for(String seg:set){
            if(indexMap.containsKey(seg)){
               HashSet<Integer> tmp = indexMap.get(seg);
                for(int t:tmp){
                    res.put(t,res.getOrDefault(t,0)+1);
                }
            }
        }
        return res;
    }


}
