package kmedoids;
import java.util.*;

public class FindK {
    public static int partition(List<Map.Entry<Integer,Integer>> arr,int low,int high){
        Map.Entry<Integer,Integer> temp=arr.get(low);
        while(low<high){
            while(arr.get(high).getValue()<=temp.getValue()&&high>low)
                --high;
            arr.set(low,arr.get(high));
            while(arr.get(low).getValue()>=temp.getValue()&&low<high)
                ++low;
            arr.set(high,arr.get(low));
        }
        arr.set(high,temp);
        return high;
    }

    public static List<Map.Entry<Integer,Integer>> find_k(int k,List<Map.Entry<Integer,Integer>> arr,int low,int high) {
        int temp = partition(arr, low, high);
        if ((temp - low) == k - 1) {
            return arr;
        } else if ((temp - low) > k - 1) {
            return find_k(k, arr, low, temp - 1);
        } else {
            return find_k(k - 1 - (temp-low), arr, temp + 1, high);
        }
    }
}




