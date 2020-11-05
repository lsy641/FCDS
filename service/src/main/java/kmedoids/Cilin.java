package kmedoids;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.text.*;


public class Cilin {
    private static float a = (float) 0.65;
    private static float b = (float) 0.8;
    private static float c = (float) 0.9;
    private static float d = (float) 0.96;
    private static float e = (float) 0.5;
    private static float f = (float) 0.1;
    private static int degree = 100;
    private static float PI = (float) Math.PI;
    private static Map code_word = new HashMap();
    private static Map word_code = new HashMap<String,ArrayList>();
    private static int N = 0;
    private static Set vocab = new HashSet<String>();
    private static Comparator cmp = Collator.getInstance(java.util.Locale.CHINA);
    private static Map<String,Float> records = new HashMap<String, Float>();
    private static double hit = 0;
    private static double deno =0;
    private static Map<String,Integer> getN = new HashMap<String, Integer>();
    private static Map<String,Set<String>> TotalSiblings = new HashMap<String, Set<String>>();
    static{
        read_cilin();
    }
    public static void main(String []args) {
        Cilin test = new Cilin();
        Set a = new HashSet<String>();
        Set b = new HashSet<String>();
        a.add("买家");
        b.add("顾客");
        float c = test.sensim(a, b);
        System.out.println(c);
    }
    public Cilin(){

    }

    public float sensim(Set<String> a, Set<String> b){
        float row_sum = 0;
        float column_sum = 0;
        float temp = 0;
        List<Float> bag = new ArrayList<Float>();
        int count1 = 0;
//        System.out.println(a+"~~"+b);
        for (String a_word : a){
            float res;
            temp = 0;
            for(String b_word:b){
                count1++;
                String query =null;
                if(a_word.equals(b_word)){
                    res = 1;
                }else {
                    if (records.keySet().contains(a_word + "," + b_word)) {
                        query = a_word + "," + b_word;
                    } else if (records.keySet().contains(b_word + "," + a_word)) {
                        query = b_word + "," + a_word;
                    }
                    if (query != null) {
                        hit++;
                        deno++;
                        res = records.get(query);
                    } else {
                        deno++;
                        res = sim2016(a_word, b_word);
                        query = a_word + "," + b_word;
                        records.put(query, res);
                    }
                }
                temp = Math.max(temp, res);
                bag.add(res);
//                System.out.println(a_word+"~"+b_word+":"+res);
            }
//            System.out.println("res:"+temp);
//            System.out.println("res:"+temp);
            row_sum += temp;
        }
        row_sum = row_sum/a.size();
        for(int i=0;i<b.size();i++){
            temp =0;
            for(int j=0;j<a.size();j++){
                temp=Math.max(temp,bag.get(j*b.size()+i));
            }
            column_sum += temp;
//            System.out.println("b:"+temp);
        }
        //大约占40MB内存，可设置
        if(records.size()>400000){
//            System.out.println("命中率："+hit/deno);
            int count = 0;
            for(Iterator<Map.Entry<String,Float>> it=records.entrySet().iterator();it.hasNext();count++){
                Map.Entry<String,Float> entry = it.next();
                it.remove();
                if(count>=200000)break;
            }
        }
        column_sum = column_sum/b.size();
        return (column_sum+row_sum)/2;
    }
//
//    public static Map read_cilin(){
//            System.out.println("coming to read cilin-----------");
//            try {
//                List<String> cilinList = CilinStringProperties.strings;
//                for (String c : cilinList) {
//                    if (c != null) {
//                        String[] res = c.split(" ");
//                        String code = res[0];
//                        String[] words = new String[res.length - 1];
//                        System.arraycopy(res, 1, words, 0, res.length - 1);
//                        for (String i : words) {
//                            vocab.add(i);
//                        }
//                        code_word.put(code, words);
//                        N += words.length;
//                        for (String w : words) {
//                            if (word_code.keySet().contains(w)) {
//                                ArrayList l = (ArrayList) word_code.get(w);
//                                l.add(code);
//                                word_code.put(w, l);
//                            } else {
//                                ArrayList l = new ArrayList<String>();
//                                l.add(code);
//                                word_code.put(w, l);
//                            }
//                        }
//                    } else {
//                        break;
//                    }
//                }
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        return code_word;
//    }

    public static Map read_cilin(){
        String path = "C:\\Users\\axiang\\IdeaProjects\\service\\src\\main\\java\\kmedoids\\cilin_ex.txt";
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
        if(! vocab.contains(w1) || ! vocab.contains(w2)){
            return (float)0.0;
        }
        float sim_max=0;
        float cur_sim = 0;
        ArrayList code1 = (ArrayList) word_code.get(w1);
        ArrayList code2 = (ArrayList) word_code.get(w2);
        if (code1==null || code2==null){
            return sim_max;
        }
        label:for(Object c1:code1){
            for(Object c2:code2){
                cur_sim = sim2016_by_code((String)c1, (String)c2);
                if (cur_sim > sim_max){
                    sim_max = cur_sim;
                }
                //差不多就停，0.9是相似度阈值
                if(sim_max>0.9)break label;
            }
        }
        return sim_max;
    }

    public float sim2016_by_code(String c1, String c2){
        String common_str = get_common_str(c1, c2);
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
            ArrayList clayer1 = code_layer(c1);
            ArrayList clayer2 = code_layer(c2);
            float k = (float)get_k(clayer1,clayer2);
            float n = (float)get_n(common_str);
            float d = dist2016(common_str);
            float e = (float) Math.sqrt((double) Math.exp((double)(-1*k/(2*n))));
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
        List<Float> weights = Arrays.asList(w1, w2, w3, w4);
        int layer = get_layer(common_str);
        if(0==layer){
            return 18;
        }else{
            float res = 0;
            for(int i=0;i<4-layer+1;i++){
                res += weights.get(i);
            }
            return 2*res;
        }


    }
    public String get_common_str(String c1, String c2){
        String res = "";
        for (int i=0;i<c1.length();i++){
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
            String can1 = (String)c1.get(i);
            String can2 = (String)c2.get(i);
            if(can1 == can2 || i==4){
                return Math.abs(Integer.valueOf(can1)-Integer.valueOf(can2));
            }
        }
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
        if(getN.keySet().contains(common_str)){
            return getN.get(common_str);
        }else{
            for(Object c:code_word.keySet()){
                String c1 = (String) c;
                Set<String> temp;
                List<List<Integer>> indexes = Arrays.asList(Arrays.asList(0,1),Arrays.asList(1,2),Arrays.asList(2,3),Arrays.asList(3,4),Arrays.asList(4,5),Arrays.asList(5,6),Arrays.asList(6,7),Arrays.asList(7,8));
                for (int i=0;i<indexes.size()-1;i++){
                    if(TotalSiblings.keySet().contains(c1.substring(0,indexes.get(i).get(1)))){
                        temp = TotalSiblings.get(c1.substring(0,indexes.get(i).get(1)));
                        temp.add(c1.substring(indexes.get(i+1).get(0),indexes.get(i+1).get(1)));
                    }
                    else{
                        temp = new HashSet<String>();
                        temp.add(c1.substring(indexes.get(i+1).get(0),indexes.get(i+1).get(1)));
                        TotalSiblings.put(c1.substring(0,indexes.get(i).get(1)),temp);
                    }
                }
            }
            for(String key:TotalSiblings.keySet()){
                getN.put(key,TotalSiblings.get(key).size());
            }
            TotalSiblings = null;
        }
        return getN.get(common_str);
    }
}
