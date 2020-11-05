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
        String path = ".\\src\\main\\java\\kmedoids\\testCase-3000.txt";
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
        ClusterAnalysis.CurrentBestSolve Bestsolve = service.startClassifyStream(l,200);
        int log =0;
        for(int index=0;index<Bestsolve.getClusters().length;index++){
            if (Bestsolve.getClusters()[index] !=null) {
                System.out.println("cluster " + log + " cluster score:" + Bestsolve.getClusters()[index].getAvgScore()+"medoid:"+Bestsolve.getClusters()[index].getMedoid().getDataPoint().getPoint().getElement());
                log++;
                for (DataPoint point : Bestsolve.getClusters()[index].getDataPoints()) {
                    System.out.println(point.getPoint().getElement());
                }
            }
        }



        System.out.println("total time:"+(System.currentTimeMillis()-startT));
    }

    public ClusterAnalysis.CurrentBestSolve startClassifyStream(List<FeedbackDO> feeds, int iter){

        /**
         * 循环N次，将每次中心点没变或者相似度很高的取出
         */
        ClusterAnalysis.CurrentBestSolve bestSolve = chooseOnceCluster(iter, feeds);

        if(iter %2 ==0) {
            HashMap<String, Integer> medoidsMap = new HashMap<>();
            //找出所有的中心点并确定其index，便于后面发现不稳定点后再对其删除（null）
            for (int i = 0; i < bestSolve.getClusters().length; i++) {
                Cluster tmp = bestSolve.getClusters()[i];
                medoidsMap.put(tmp.getMedoid().getDataPoint().getPoint().getElement(), i);
            }
            //重复多次  去掉中心点不稳定的元素,如果减去自身平均值大于0.52则保留
            for (int i = 0; i < 6; i++) {

                ClusterAnalysis.CurrentBestSolve bestSolveTmp = chooseOnceCluster(iter, feeds);
                Cluster[] tmpCluster = bestSolveTmp.getClusters();

                Iterator it = medoidsMap.keySet().iterator();

                while (it.hasNext()) {
                    String objStr = (String) it.next();
                    boolean isRemove = true;
                    for (int j = 0; j < tmpCluster.length; j++) {
                        DataPoint bestDataPoint = tmpCluster[j].getMedoid().getDataPoint();

                        //找出不稳定点,这里只做比较，直接将businessKey设置为null
                        if (bestDataPoint.calculatorScore(new TextPoint(objStr,null)) > 0.5) {
                            isRemove = false;
                            break;
                        }
                    }
                    //避免之前已经被剔除掉后为null,不稳定点必须保证均值大于avgScore
                    if(bestSolve.getClusters()[medoidsMap.get(objStr)]!=null && isRemove) {

                        //类簇里面元素小于等于3个，则均值要大于0.70，不然删除
                        if (bestSolve.getClusters()[medoidsMap.get(objStr)].getDataPoints().size()==3 &&
                                bestSolve.getClusters()[medoidsMap.get(objStr)].getAvgScore() < 0.70){

                            bestSolve.removeIndex(medoidsMap.get(objStr));

                        }else if(bestSolve.getClusters()[medoidsMap.get(objStr)].getDataPoints().size()==1){
                            bestSolve.removeIndex(medoidsMap.get(objStr));
                        }else if(bestSolve.getClusters()[medoidsMap.get(objStr)].getDataPoints().size()==2 &&
                                bestSolve.getClusters()[medoidsMap.get(objStr)].getAvgScore() < 0.80){
                            bestSolve.removeIndex(medoidsMap.get(objStr));

                        }else if (bestSolve.getClusters()[medoidsMap.get(objStr)].getDataPoints().size()>3 &&
                                bestSolve.getClusters()[medoidsMap.get(objStr)].getAvgScore() < 0.6) {

                            bestSolve.removeIndex(medoidsMap.get(objStr));
                        }
                    }

                }
            }
        }
        return bestSolve;
    }

    /**
     *
     * @param iter
     * @param feeds
     * @return
     */
    private ClusterAnalysis.CurrentBestSolve chooseOnceCluster(int iter,List<FeedbackDO> feeds){
        ArrayList<DataPoint> dataPoints = new ArrayList<DataPoint>();
        JiebaSegmenter segmenter = new JiebaSegmenter();
        if(feeds.size() == 0){
            return (new ClusterAnalysis()).new CurrentBestSolve();
        }
        Pattern pattern = Pattern.compile("[\u4e00-\u9fa5]");

        /**
         * 客满数据细分
         */
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
                            dataPoints.add(new DataPoint(replaceStopWord(segmenter, new TextPoint(desc, feedBackDO.getId())), new TextPoint(feedBackDO.getFeedDesc() + "-transform-" + desc, feedBackDO.getId())));
                        }
                    }
                }
            }else {
                //必须汉字
                Matcher matcher = pattern.matcher(feedBackDO.getFeedDesc());
                if(matcher.find() && feedBackDO.getFeedDesc().length() > 6) {
                    dataPoints.add(new DataPoint(replaceStopWord(segmenter, new TextPoint(feedBackDO.getFeedDesc(), feedBackDO.getId())), new TextPoint(feedBackDO.getFeedDesc(), feedBackDO.getId())));
                }
            }
        }
        /**
         * k值动态确定算法
         */
        double pre = 0;
        double max = 0;
        //如果不满足Math.sqrt(dataPoints.size()/2)+1条件，默认聚类数目为1
        int index = 1;
        //找出最大的斜率
        for(int k=2;k<=Math.sqrt(dataPoints.size()/2)+1;k++) {
            ClusterAnalysis clusterAnalysis = new ClusterAnalysis(iter, dataPoints, k);
            clusterAnalysis.analysisCluster();
            if(pre>0) {
                double cur = sumSi(clusterAnalysis.getCurrentBestSolve());
                double tmp = Math.abs(cur - pre);
                if(tmp > max){
                    max = tmp;
                    index = k;
                }
                pre = cur;
            }else {//第一次
                pre = sumSi(clusterAnalysis.getCurrentBestSolve());
            }
        }

        ClusterAnalysis clusterAnalysis = new ClusterAnalysis(iter, dataPoints, index);
        clusterAnalysis.analysisCluster();

        ClusterAnalysis.CurrentBestSolve bestSolve = clusterAnalysis.getCurrentBestSolve();

        return bestSolve;
    }


    /**
     * 计算每个簇族的散列程度
     * @param bestSolve
     * @return
     */
    private double[] Si(ClusterAnalysis.CurrentBestSolve bestSolve){
        double []res = new double[bestSolve.getClusters().length];
        JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();

        int n=0;
        for(Cluster cluster : bestSolve.getClusters()){
            double ss = 0;
            for(DataPoint dataPoint : cluster.getDataPoints()){
                double distance = jaroWinklerDistance.apply(cluster.getMedoid().getDataPoint().getPoint().getElement(),dataPoint.getPoint().getElement());
                ss = ss + Math.pow(1-distance,2);
            }
            if(cluster.getDataPoints().size()>0) {
                res[n] = Math.sqrt(ss / cluster.getDataPoints().size());
            }else {
                //避免分母为0的情况
                res[n] = 0;
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
        List<SegToken> sgs = segmenter.process(textPoint.getElement(), JiebaSegmenter.SegMode.INDEX);
        String real = "";
        for(SegToken sg : sgs){
            if(!stopwords.contains(sg.word)){
                real = real + sg.word;
            }
        }
        //长度大于5才过滤停顿词，不然原文本返回
        if(real.length() > 5) {
            return new TextPoint(real,textPoint.getBusinessKey());
        }else {
            return textPoint;
        }
    }

    private double sumSi(ClusterAnalysis.CurrentBestSolve bestSolve){
        double []si = Si(bestSolve);
        double sum = 0;
        for(double sx : si){
            sum = sum + sx;
        }
        return sum;
    }


}
