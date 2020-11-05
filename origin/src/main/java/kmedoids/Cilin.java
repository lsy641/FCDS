package kmedoids;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Cilin {
    private float a = (float) 0.65;
    private float b = (float) 0.8;
    private float c = (float) 0.9;
    private float d = (float) 0.96;
    private float e = (float) 0.5;
    private float f = (float) 0.1;
    private int degree = 100;
    private  float PI = (float) Math.PI;
    private Map code_word = new HashMap();
    private Map word_code = new HashMap<String,ArrayList>();
    private int N = 0;
    private Set vocab = new HashSet<String>();
    public static void main(String []args) {
        Cilin test = new Cilin();
        float a = test.sim2016("老婆","哈哈哈");
        System.out.println(a);
    }
    public Cilin(){
        read_cilin();
    }

    public Map read_cilin(){
        String path = "./cilin_ex.txt";
        FileReader fr = null;
        try {
            fr = new FileReader(path);
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        }
        BufferedReader br = new BufferedReader(fr);
        String str = null;
        while (true) {
            try {
                str = br.readLine();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            if (str != null) {
                String[] res = str.split(" ");
                String code = res[0];
                String[] words = new String[res.length - 1];
                System.arraycopy(res, 1, words, 0, res.length-1);
                for (String i : words) {
                    vocab.add(i);
                }
                code_word.put(code, words);
                N += words.length;
                for (String w : words) {
                    if (word_code.keySet().contains(w)) {
                        ArrayList l = (ArrayList) word_code.get(w);
                        l.add(code);
                        word_code.put(w,l);
                    } else {
                        ArrayList l = new ArrayList<String>();
                        l.add(code);
                        word_code.put(w, l);
                    }
                }
            }else{
                break;
            }

        }
        try {
            br.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        try {
            fr.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return code_word;
    }
    public float sim2016(String w1, String w2){
        if(! vocab.contains(w1) && ! vocab.contains(w2)){
            return (float)0.0;
        }
        float sim_max=0;
        float cur_sim = 0;
        ArrayList code1 = (ArrayList) word_code.get(w1);
        ArrayList code2 = (ArrayList) word_code.get(w2);
        if (code1==null || code2==null){
            return sim_max;
        }
        for(Object c1:code1){
            for(Object c2:code2){
                cur_sim = sim2016_by_code((String)c1, (String)c2);
                if (cur_sim > sim_max){
                    sim_max = cur_sim;
                    System.out.println("sim_max:"+sim_max);

                }
            }
        }
        return sim_max;
    }

    public float sim2016_by_code(String c1, String c2){
        ArrayList clayer1 = code_layer(c1);
        ArrayList clayer2 = code_layer(c2);
        String common_str = get_common_str(c1, c2);
//        System.out.println("common:"+common_str);
        int length = common_str.length();
        if(c1.endsWith("@") || c2.endsWith("@") || 0==length){
            return f;
        }
        float cur_sim = 0;
        if(7<= length){
            if(c1.endsWith("=") && c2.endsWith("=")) {
                cur_sim = 1;
            }else{
                if(c1.endsWith("#") && c2.endsWith("#")){
                    cur_sim = e;
                }
            }
        }else{
            float k = (float)get_k(clayer1,clayer2);
            float n = (float)get_n(common_str);
//            System.out.println("k:"+k);
//            System.out.println("n:"+n);
            float d = dist2016(common_str);
//            System.out.println("d:"+d);
            float e = (float) Math.sqrt((double) Math.exp((double)(-1*k/(2*n))));
//            System.out.println((-1*k/(2*n)));
//            System.out.println("e:"+e);
            cur_sim = (float) ((1.05 - 0.05*d)*e);
            return cur_sim;
        }
        return cur_sim;
    }
    public float dist2016(String common_str){
        //计算两个编码的距离
        float w1 = (float)0.5;
        float w2 = 1;
        float w3 = (float)2.5;
        float w4 = (float)2.5;
        List<Float> namesList = Arrays.asList( w1, w2, w3, w4);
        ArrayList weights = new ArrayList();
        weights.addAll(namesList);
        int layer = get_layer(common_str);
        if(0==layer){
            return 18;
        }else{
            float res = 0;
            for(int i=0;i<4-layer+1;i++){
                res += (float) weights.get(i);
//                System.out.println("res:"+res);
            }
            return 2*res;
        }


    }
    public String get_common_str(String c1, String c2){
        String res = "";
        for (int i=0;i<c1.length();i++){
//            System.out.println(c1.substring(i,i+1)+c2.substring(i,i+1));
            if(c1.substring(i,i+1).equals(c2.substring(i,i+1))){
                res = res+c1.substring(i,i+1);
            }
            else{
                break;
            }
        }
        if (res.length()==3 || res.length()==6){
            res = res.substring(0,res.length());
        }
        return res;
    }

    public ArrayList code_layer(String c){
        //将编码按层次结构化
        //        Aa01A01=
        //        第三层和第五层是两个数字表示
        //第一、二、四层分别是一个字母
        // 最后一个字符用来去分所有字符相同的情况
        ArrayList l = new ArrayList();
        l.add(c.substring(0,1));
        l.add(c.substring(1,2));
        l.add(c.substring(2,4));
        l.add(c.substring(4,5));
        l.add(c.substring(5,7));
        l.add(c.substring(7,8));
        return l;
    }
    public int get_layer(String common_str){
        int length = common_str.length();
        if(1==length){
            return 1;
        }else if (2==length){
            return 2;
        }else if (4==length){
            return 3;
        }else if(5==length){
            return 4;
        }else if(7==length){
            return 5;
        }else{
            return 0;
        }

    }
    public int get_k(ArrayList c1, ArrayList c2){
        for(int i=0;i<5;i++){
            if(c1.get(i) == c2.get(i) || i==4){
                if(i==2)System.out.println("number:"+Integer.valueOf((Integer) c1.get(i)));
                return Math.abs(Integer.valueOf((String) c1.get(i))-Integer.valueOf((String) c2.get(i)));
            }
        }
//        if(c1.get(0)!=c2.substring(0,1)){
//            return Math.abs(Integer.valueOf(c1.charAt(0))-Integer.valueOf(c2.charAt(0)));
//        }else if(c1.substring(1,2)!=c2.substring(1,2)){
//            return Math.abs(Integer.valueOf(c1.charAt(1))-Integer.valueOf(c2.charAt(1)));
//        }else if(c1.substring(2,3)!=c2.substring(2,3)){
//            System.out.println(c1.substring(2,3));
//            System.out.println(Integer.valueOf(c1.charAt(2)));
//            return Math.abs(Integer.valueOf(c1.charAt(2))-Integer.valueOf(c2.charAt(2)));
//        }else if(c1.substring(3,4)!=c2.substring(3,4)){
//            return Math.abs(Integer.valueOf(c1.charAt(3))-Integer.valueOf(c2.charAt(3)));
//        }else{
//            return Math.abs(Integer.valueOf(c1.charAt(4))-Integer.valueOf(c2.charAt(4)));
//        }
        return Math.abs(Integer.valueOf((Integer) c1.get(4))-Integer.valueOf((Integer) c2.get(4)));
    }
    public int get_n(String common_str){
        //        计算所在分支层的分支数
        //        即计算分支的父节点总共有多少个子节点
        //        两个编码的common_str决定了它们共同处于哪一层
        //        例如，它们的common_str为前两层，则它们共同处于第三层，则我们统计前两层为common_str的第三层编码个数就好了
        if (0==common_str.length()){
            return 0;
        }
        Set siblings = new HashSet();
        int layer = get_layer(common_str);
        for(Object c:code_word.keySet()){
            String c1 = (String) c;
            if(c1.startsWith(common_str)){
                ArrayList clayer = code_layer(c1);
                siblings.add(clayer.get(layer));
            }
        }
        return siblings.size();
    }
}
