package kmedoids;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import org.apache.commons.text.similarity.JaroWinklerDistance;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClassifyTService {
    private static Set<String> stopwords = new HashSet<String>();
    static{
        readStopWord();
    }
    public static void readStopWord(){
        String path = ".\\src\\main\\java\\kmedoids\\stopwords.txt";
        FileReader fr = null;
        try {
            fr = new FileReader(path);
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        }
        BufferedReader br = new BufferedReader(fr);
        String str = null;
        while(true) {
            try {
                str = br.readLine();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            if(str!=null){
                stopwords.add(str);
            }else break;
        }
    }

    public static void main(String[] args){
        Long startT = System.currentTimeMillis();
        List<FeedbackDO> l = new ArrayList<FeedbackDO>();
        String root = System.getProperty("user.dir");
        String path = ".\\src\\main\\java\\kmedoids\\testCase-10000.txt";
        FileReader fr = null;
        try {
            fr = new FileReader(path);
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        }
        BufferedReader br = new BufferedReader(fr);
        String str = null;
        while(true) {
            try {
                str = br.readLine();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            if(str!=null){
                FeedbackDO item = new FeedbackDO(str,(long)1);
                l.add(item);
            }else break;
        }
        ClassifyTService service = new ClassifyTService();
        ClusterAnalysis.CurrentBestSolve res = null;
        if(l.size()>2000) {
            res = service.miniStartClassifyStream(l, Math.max(((int)(Math.sqrt((double)l.size()))/2)*2,20));
        }else{
            res = service.startClassifyStream(l, Math.max(((int)(Math.sqrt((double)l.size()))/2)*2,20));
        }
        System.out.println("primary num of cluster:"+res.getClusters().length);
        List<Cluster> cleanCluster = new ArrayList<Cluster>();
        int totalnum = 0;
        if(Math.max(((int)(Math.sqrt((double)l.size()))/2)*2,20) %2 ==0) {
            for (int i = 0; i < res.getClusters().length; i++) {
                Cluster tmp = res.getClusters()[i];
                totalnum += tmp.getNumDataPoints();
                if (tmp.getAvgScore() < 0) {
                    res.removeIndex(i);
                }
                tmp = res.getClusters()[i];
                if(tmp!=null){
                    cleanCluster.add(tmp);
                }
            }
        }
        System.out.println("total num:"+totalnum);
        Cluster[] res_clusters = cleanCluster.stream().sorted(Comparator.comparing(Cluster::getNumDataPoints)).collect(Collectors.toList()).toArray(new Cluster[res.getClusters().length]);
        int log =0;
        for(int index=0;index<res_clusters.length;index++){
            if (res_clusters[index] !=null) {
                System.out.println("cluster " + log + " cluster score:" + res_clusters[index].getAvgScore()+"medoid:"+res_clusters[index].getMedoid().getDataPoint().getPoint().getOriginText()+"segment:"+res_clusters[index].getMedoid().getDataPoint().getPoint().getElement_seg());
                log++;
                for (DataPoint point : res_clusters[index].getDataPoints()) {
                    System.out.println(point.getPoint().getOriginText());
                }
            }
        }
        System.out.println("total time:"+(System.currentTimeMillis()-startT));
    }
    public ClusterAnalysis.CurrentBestSolve miniStartClassifyStream(List<FeedbackDO> feeds, int iter){
        Long startT = System.currentTimeMillis();
        //借鉴mini batch kmeans
        ClusterAnalysis.CurrentBestSolve bestSolve=null;
        ClusterAnalysis.CurrentBestSolve finalSolve=null;
        ArrayList<DataPoint> dps = processFeeds(feeds);
        double best_si = Double.POSITIVE_INFINITY;
        ArrayList<DataPoint> valid_set = new ArrayList<DataPoint>();
        Random random = new Random();
        int index = random.nextInt(dps.size());
        while (!valid_set.contains(dps.get(index)) || valid_set.size()<Math.max((int)Math.sqrt(dps.size()),800)){
            valid_set.add(dps.get(index));
            index = random.nextInt(dps.size());
        }
        ArrayList<DataPoint> arrived_set = null;
        for(int it=0;it<Math.min((int)(feeds.size()/valid_set.size()),5);it++) {
            index = 0;
            ArrayList<DataPoint> m = new ArrayList<DataPoint>();
            while (!m.contains(dps.get(index)) || m.size() < Math.max((int) Math.sqrt(dps.size()), 800)) {
                index = random.nextInt(dps.size());
                m.add(dps.get(index));
            }
            bestSolve = chooseOnceCluster((int) Math.max(((int)(Math.sqrt((double) m.size())/2))*2, 20), m);
            //建立倒排索引表
            Lucene reverseIndex = new Lucene();
            int cluster_id = 0;
            for (Cluster cluster : bestSolve.getClusters()) {
                if (cluster != null) {
                    HashSet<String> segs = (HashSet<String>) cluster.getMedoid().getDataPoint().getPoint().getElement_seg();
                    for (String seg : segs) {
                        reverseIndex.addItem(seg, cluster_id);
                    }
                }
                cluster_id++;
            }
            for (DataPoint point : valid_set) {
                double bestScore = Double.NEGATIVE_INFINITY;
                cluster_id = 0;
                int best_id = 0;
                HashMap<Integer, Integer> index_res = reverseIndex.findItem((HashSet<String>) point.getPoint().getElement_seg());
                List<Map.Entry<Integer, Integer>> entryList = new ArrayList<>(index_res.entrySet());
                entryList = entryList.stream().sorted((o1, o2) -> o2.getValue().compareTo(o1.getValue())).collect(Collectors.toList());
                if(entryList.size()>5) entryList = FindK.find_k(5,entryList,0,entryList.size()-1);
                Iterator<Map.Entry<Integer, Integer>> entryIter = entryList.iterator();
                Map.Entry<Integer, Integer> tempEntry = null;
                int stop = 5;
                while (entryIter.hasNext() && stop > 0) {
                    stop--;
                    tempEntry = entryIter.next();
                    cluster_id = tempEntry.getKey();
                    if (bestSolve.getClusters()[cluster_id].getDataPoints().contains(point)) continue;
                    double score = point.calculatorScore(bestSolve.getClusters()[cluster_id].getMedoid().getDataPoint().getPoint());

                    if (score > bestScore ) {
                        bestScore = score;
                        best_id = cluster_id;
                    }
                    if (bestScore > 0.8) break;
                }
                if (!m.contains(point)) {
                    bestSolve.getClusters()[best_id].addDataPoint(point);
                    point.setCluster(bestSolve.getClusters()[best_id]);
                }
            }
            double cur = sumSi(bestSolve) / bestSolve.getClusters().length;
            if (cur < best_si) {
                best_si = cur;
                finalSolve = bestSolve;
                arrived_set = new ArrayList<DataPoint>(valid_set);
                arrived_set.addAll(m);
            }
        }
        //建立倒排索引表
        Lucene reverseIndex = new Lucene();
        int cluster_id = 0;
        for(Cluster cluster:finalSolve.getClusters()){
            if(cluster!=null) {
                HashSet<String> segs =(HashSet<String>) cluster.getMedoid().getDataPoint().getPoint().getElement_seg();
                for(String seg:segs){
                    reverseIndex.addItem(seg,cluster_id);
                }
            }
            cluster_id ++;
        }
        System.out.println("time:"+(System.currentTimeMillis()-startT));
        for(DataPoint point:dps){
            double bestScore = Double.NEGATIVE_INFINITY;
            cluster_id = 0;
            int best_id = -1;
            HashMap<Integer,Integer> index_res = reverseIndex.findItem((HashSet<String>) point.getPoint().getElement_seg());
            List<Map.Entry<Integer,Integer>> entryList = new ArrayList<>(index_res.entrySet());
            entryList = entryList.stream().sorted((o1,o2)->o2.getValue().compareTo(o1.getValue())).collect(Collectors.toList());
            if(entryList.size()>5) entryList = FindK.find_k(5,entryList,0,entryList.size()-1);
            Iterator<Map.Entry<Integer,Integer>> entryIter = entryList.iterator();
            Map.Entry<Integer,Integer> tempEntry = null;
            int stop = 5;
            while(entryIter.hasNext() && stop>0){
                stop--;
                tempEntry = entryIter.next();
                cluster_id = tempEntry.getKey();
                if(finalSolve.getClusters()[cluster_id].getDataPoints().contains(point))continue;
                double score = point.calculatorScore(finalSolve.getClusters()[cluster_id].getMedoid().getDataPoint().getPoint());
                double limitScore = 0.5;
                if (score > bestScore && score > limitScore){
                        bestScore =score;
                        best_id = cluster_id;
                    }
                    if(bestScore>0.8)break;
                }
            if (!arrived_set.contains(point) && best_id!=-1){
                finalSolve.getClusters()[best_id].addDataPoint(point);
                point.setCluster(finalSolve.getClusters()[best_id]);}
        };

        return finalSolve;
    }
    public ClusterAnalysis.CurrentBestSolve startClassifyStream(List<FeedbackDO> feeds, int iter){
        /**
         * 循环N次，将每次中心点没变或者相似度很高的取出
         */
        if(feeds.size() == 0){
            return (new ClusterAnalysis()).new CurrentBestSolve();
        }
        ArrayList<DataPoint> inputData = processFeeds(feeds);
        ClusterAnalysis.CurrentBestSolve bestSolve = chooseOnceCluster(iter, inputData);
        return bestSolve;
    }
    public ArrayList<DataPoint> processFeeds(List<FeedbackDO> feeds){
        /**
         * 客满数据细分
         */
        ArrayList<DataPoint> dataPoints = new ArrayList<DataPoint>();
        JiebaSegmenter segmenter = new JiebaSegmenter();
        Pattern pattern = Pattern.compile("[\u4e00-\u9fa5]");
        int pointTd = 0;
        for(FeedbackDO feedBackDO : feeds){
            if(feedBackDO.getSourceFrom() == 3){
                String regex = "((http|ftp|https)://)(([a-zA-Z0-9\\._-]+\\.[a-zA-Z]{2,6})|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(:[0-9]{1,4})*(/[a-zA-Z0-9\\&%_\\./-~-]*)?";
                //去掉所以的url
                for(String desc : feedBackDO.getFeedDesc().replaceAll(regex,"").split("&")){
                    Matcher m = pattern.matcher(desc);
                    //必须包含中文，且大于6个汉字
                    if(desc.length() > 6 && m.find()) {
                        //避免旗舰店这个关键字被影响，主要是客满数据会有干扰
                        if(desc.length()>30 || (!desc.contains("旗舰店")&&!desc.contains("专营店"))) {
                            TextPoint a = new TextPoint(desc, feedBackDO.getId());
//                            a.setEmbedding();
                            TextPoint b = new TextPoint(feedBackDO.getFeedDesc() + "-transform-" + desc, feedBackDO.getId());
//                            b.setEmbedding();
                            dataPoints.add(new DataPoint(replaceStopWord(segmenter, a), b, pointTd));
                        }
                    }
                }
            }else {
                //必须汉字
                Matcher matcher = pattern.matcher(feedBackDO.getFeedDesc());
                if(matcher.find() && feedBackDO.getFeedDesc().length() > 0) {
                    TextPoint a = new TextPoint(feedBackDO.getFeedDesc(), feedBackDO.getId());
//                    a.setEmbedding();
                    TextPoint b = new TextPoint(feedBackDO.getFeedDesc(), feedBackDO.getId());
//                    b.setEmbedding();
                    dataPoints.add(new DataPoint(replaceStopWord(segmenter, new TextPoint(feedBackDO.getFeedDesc(), feedBackDO.getId())), new TextPoint(feedBackDO.getFeedDesc(), feedBackDO.getId()),pointTd));
                }
            }
            pointTd += 1;}
        return dataPoints;
    }


    private ClusterAnalysis.CurrentBestSolve    chooseOnceCluster(int iter,ArrayList<DataPoint> dataPoints){
        /**
         * k值动态确定算法
         */
        System.out.println("datapoint size:"+dataPoints.size());

        ArrayList<Integer> fib = new ArrayList<Integer>();
        fib.add(0);
        fib.add(1);
        while(fib.get(fib.size()-1) < dataPoints.size()-1){
            fib.add(fib.get(fib.size()-1)+fib.get(fib.size()-2));
        }
        ClusterAnalysis.CurrentBestSolve tmp = null ;
        ClusterAnalysis.CurrentBestSolve bestSolve = null;
        if (dataPoints.size()<=200) {
            bestSolve = linearsearch(iter, dataPoints);
        }else{
            bestSolve = fibsearch(fib,0,fib.size()-1, iter, dataPoints);
        }
        System.out.println("final cluster num:"+bestSolve.clusterSize());
        return bestSolve;
    }

    public ClusterAnalysis.CurrentBestSolve linearsearch(int iter, ArrayList<DataPoint> datapoints){
        ClusterAnalysis clusterAnalysis = new ClusterAnalysis(iter, datapoints, 2);
        for(int num=2; num<=datapoints.size(); num++){
            clusterAnalysis = new ClusterAnalysis(iter, datapoints, num);
            clusterAnalysis.analysisCluster();
            double cur = sumSi(clusterAnalysis.getCurrentBestSolve());
            if (cur > 0.25 || ((double)clusterAnalysis.getDelCount()/(double)datapoints.size())>0.2 ) {continue;}
            return clusterAnalysis.getCurrentBestSolve();
        }
        return clusterAnalysis.getCurrentBestSolve();
    }

    public ClusterAnalysis.CurrentBestSolve fibsearch(ArrayList<Integer> fib, int low, int k, int iter, ArrayList<DataPoint> datapoints) {
        if (k <= 2 || low + fib.get(k - 2) + 1 > datapoints.size()) {
            ClusterAnalysis clusterAnalysis = new ClusterAnalysis(iter, datapoints, low + 2);
            clusterAnalysis.analysisCluster();
            double cur = sumSi(clusterAnalysis.getCurrentBestSolve());
            return clusterAnalysis.getCurrentBestSolve();
        }
        int mid = low + fib.get(k - 2) - 1;
        ClusterAnalysis clusterAnalysis = new ClusterAnalysis(iter, datapoints, mid + 2);
        clusterAnalysis.analysisCluster();
        double cur = sumSi(clusterAnalysis.getCurrentBestSolve());
        if (Math.abs(cur-0.25)<=0.01 && clusterAnalysis.getDelCount()/datapoints.size()<=0.2){return clusterAnalysis.getCurrentBestSolve();}
        if (cur < 0.25 && clusterAnalysis.getDelCount()/datapoints.size()<=0.2 ) {
            return fibsearch(fib, low, k - 2, iter, datapoints);
        } else  {
            return fibsearch(fib, mid, k - 1, iter, datapoints);

        }
    }



    public ClusterAnalysis.CurrentBestSolve bisearch(int low,int high,int iter, ArrayList<DataPoint> datapoints){
        int mid = (low+high)/2;
        if(low>=high){
            mid = low;
        }

        ClusterAnalysis clusterAnalysis = new ClusterAnalysis(iter,datapoints,mid+2);
        clusterAnalysis.analysisCluster();
        if(low>=high){
            return clusterAnalysis.getCurrentBestSolve();
        }

        double cur = sumSi(clusterAnalysis.getCurrentBestSolve())/mid;
        System.out.println("num of cluster: "+mid+" cur score:"+cur);
        if(Math.abs(cur-0.2)<=0.01) return clusterAnalysis.getCurrentBestSolve();
        if (cur < 0.2) {
            return bisearch(low,mid-1,iter,datapoints);
        } else if (cur >= 0.2) {
            return bisearch(mid+1,high,iter,datapoints);
        }
        return clusterAnalysis.getCurrentBestSolve();
    }

    /**
     * 计算每个簇族的散列程度
     * @param bestSolve
     * @return
     */
    private double[] Si(ClusterAnalysis.CurrentBestSolve bestSolve) {
        Cilin cilin = new Cilin();
        JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
        double[] res = new double[bestSolve.getClusters().length];
        int n = 0;
        double distance = 0;
        for (Cluster cluster : bestSolve.getClusters()) {
            double ss = 0;
            int clusterMID = cluster.getMedoid().getDataPoint().getId();
            for (DataPoint dataPoint : cluster.getDataPoints()) {
                double score_bow = 0;
                double score_jw = 0;
                try {
                    if (!dataPoint.getPoint().SimMap().keySet().contains(clusterMID)) {
                        score_bow = (double) cilin.sensim(cluster.getMedoid().getDataPoint().getPoint().getElement_seg(), dataPoint.getPoint().getElement_seg());
                        cluster.getMedoid().getDataPoint().getPoint().setSenSim(dataPoint.getPoint().getId(), (float) score_bow);
                        dataPoint.getPoint().setSenSim(cluster.getMedoid().getDataPoint().getPoint().getId(), (float) score_bow);

                    } else score_bow = (double) dataPoint.getPoint().getSenSim(clusterMID);
                    if (!dataPoint.getPoint().JwMap().keySet().contains(clusterMID)) {
                        score_jw = jaroWinklerDistance.apply(cluster.getMedoid().getDataPoint().getPoint().getElement(), dataPoint.getPoint().getElement());
                        cluster.getMedoid().getDataPoint().getPoint().setJW(dataPoint.getPoint().getId(), score_jw);
                        dataPoint.getPoint().setJW(cluster.getMedoid().getDataPoint().getPoint().getId(), score_jw);
                    } else score_jw = dataPoint.getPoint().getJW(clusterMID);
//                    System.out.println(dataPoint.getPoint().getOriginText()+"~"+cluster.getMedoid().getDataPoint().getPoint().getOriginText()+score_bow+"~"+score_jw);
//                    score_bow = (double) dataPoint.getPoint().getSenSim(clusterMID);
//                    score_jw = dataPoint.getPoint().getJW(clusterMID);
                    String key = "True";
//                    String key = DiamondSpringProperties.getByKey("");
                    if (key.equals("True")) {
                        distance = score_bow * 0.3 + score_jw * 0.7;
                    } else {
                        distance = score_jw;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ss = ss + Math.pow(1 - distance, 2);
            }
                if (cluster.getDataPoints().size() > 0) {
                    res[n] = Math.sqrt(ss / cluster.getDataPoints().size());
                } else {
                    //避免分母为0的情况
                    res[n] = -1;
                }
                n++;
            }
            return res;
        }
    /**
     * 停顿词过滤
     * @param segmenter
     * @param textPoint
     * @return
     */
    private TextPoint replaceStopWord(JiebaSegmenter segmenter, TextPoint textPoint){
        List<SegToken> sgs = segmenter.process(textPoint.getElement(), JiebaSegmenter.SegMode.SEARCH);
        Set<String> segs = new HashSet<String>();
        String real = "";
        for(SegToken sg : sgs){
            if(!stopwords.contains(sg.word)){
                real = real + sg.word;
                segs.add(sg.word);
            }
        }
        //长度大于0才过滤停顿词，不然原文本返回
        if(real.length() > 0) {
            TextPoint new_point = new TextPoint(real,textPoint.getBusinessKey());
            new_point.setOriginText(textPoint.getElement());
            new_point.setElement(segs);
            return new_point;
        }else {
            textPoint.setOriginText(textPoint.getElement());
            textPoint.setElement(segs);
            return textPoint;
        }
    }

    private double sumSi(ClusterAnalysis.CurrentBestSolve bestSolve){
        double []si = Si(bestSolve);
        double sum = 0;

        int den = 0;
        for(double sx : si){
            if(sx > -1) {
                den++;
                sum = sum + sx;
            }
        }
        return sum/den;
    }

    private  double calinski_harabasz_score(ClusterAnalysis.CurrentBestSolve bestSolve){
        double n_samples = 0.0;
        double n_labels = 0.0;
        double sum = Double.NEGATIVE_INFINITY;
        List<DataPoint> all_point = new ArrayList<DataPoint>();
        for(Cluster cluster:bestSolve.getClusters()){
            for(DataPoint point:cluster.getDataPoints()){
                all_point.add(point);
            }
            n_samples += cluster.getNumDataPoints();
            n_labels += 1;
        }
        DataPoint center = null;
        double extra_disp = (double)0;
        double intra_disp = (double)0;
        double temp = 0;
        int c = 0;
        for(int cluster_i=0;cluster_i<bestSolve.getClusters().length;cluster_i++) {
            for (int cluste_j=cluster_i+1;cluste_j<bestSolve.getClusters().length;cluste_j++) {
                for(DataPoint point : bestSolve.getClusters()[cluster_i].getDataPoints())
                    for(DataPoint point1:bestSolve.getClusters()[cluste_j].getDataPoints()) {
                        c++;
                        temp += Math.pow(1 - point.calculatorScore(point1), 2);
                    }
            }
        }
        extra_disp = temp/c*n_samples;
        int count = 0;
        for(Cluster cluster:bestSolve.getClusters()){
            for(DataPoint point:cluster.getDataPoints()){
                count++;
                intra_disp += Math.pow(1-Math.max(point.calculatorSumDataPoint()/cluster.getDataPoints().size(),0.00001),2.0);
            }
        }
        if (intra_disp == 0.0)return 1.0;
        else{
            return ((extra_disp/Math.pow(intra_disp,2)));
        }
    }

}
