package kmedoids;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ClusterAnalysis {

    private Cluster[] clusters;// 所有类簇

    private int miter;// 迭代次数

    private ArrayList<DataPoint> dataPoints;// 所有样本点

    private int dimNum;//聚类数目

    private boolean isFinish = false;

    private int currentIter = 0;

    private CurrentBestSolve currentBestSolve;//当前最优解

    private String limitScore;


    public ClusterAnalysis(){};

    public ClusterAnalysis(int iter, ArrayList<DataPoint> dataPoints, int dimNum) {
        clusters = new Cluster[dimNum];// 类簇种类数
        for (int i = 0; i < dimNum; i++) {
            clusters[i] = new Cluster();
        }
        this.miter = iter;
        this.dataPoints = dataPoints;
        //TODO 动态选择聚类簇数目
        this.dimNum = dimNum;
        limitScore = "0.6";
        this.currentBestSolve = new CurrentBestSolve();
    }

    public double analysisCluster(){
        boolean firstToAssign = true;
        double preSumScore = 0;
        double currentSumScore = 0;
        int currentSameNum = 0;
        while (!isFinish){

            //随机选择k个中心点
            if(firstToAssign){
                initMedoids();
                firstToAssign = false;
            }else {
                for(int m = 0; m < clusters.length; m++){
                    //从已经的划分中找出局部最优解的中心点
                    Medoid mm = clusters[m].getMedoid().evaluateBestMedoidForCurrentCluster();
                    //重新设置medoid
                    Medoid md = new Medoid(mm.getDataPoint());
                    //先设置md，再设置cluster
                    clusters[m].setMedoid(md);
                    md.setCluster(clusters[m]);
                }
            }

            //计算当前划分的分数之和
            double sumScore = 0;
            for(int m = 0; m < clusters.length; m++){
                double mtp = clusters[m].getMedoid().calculatorSum();
                sumScore = sumScore + mtp;
            }
            currentSumScore = sumScore;

//            System.out.println("迭代 #："+currentIter+" pre:"+preSumScore+" current:"+currentSumScore);
            //如果当前分数值大于上个分数值，则交换最大值,并以当前中心点集合开始迭代
            if(currentSumScore > preSumScore) {

                currentSameNum = 0;
                preSumScore = currentSumScore;

                //================================ start
                Cluster []myClusters = new Cluster[dimNum];
                int m = 0;
                //引用赋值比较麻烦  只能重新new
                for(Cluster tmp: clusters){
                    Cluster clusterTmp = new Cluster();
//                    DataPoint dataPointTmp = new DataPoint(new TextPoint(tmp.getMedoid().getDataPoint().getPoint().getElement()));
                    Medoid medoidTmp = tmp.getMedoid();
                    medoidTmp.setCluster(clusterTmp);
                    clusterTmp.setMedoid(medoidTmp);
                    clusterTmp.addAllPoint(tmp.getDataPoints());
                    myClusters[m] = clusterTmp;
                    m++;
                }
                currentBestSolve.setClusters(myClusters);
                //================================ end
                chooseNewMedoids();
            }else {
                currentSameNum++;
//                System.out.println("===随机啦===");
                //随机选择点
                initMedoids();
            }

            /**
             * 调试输出
             */
//            for(Cluster cluster:currentBestSolve.getClusters()){
//                System.out.print("zhongxindian:"+cluster.getMedoid().getDataPoint().getPoint().getElement()+"======\n");
//                for(DataPoint dataPoint : cluster.getDataPoints()){
//                    System.out.println(dataPoint.getPoint().getElement());
//                }
//                System.out.println();
//            }


            /*currentIter++;
            if(currentIter > miter){
                isFinish = true;
            }*/
            currentIter++;
            if(currentSameNum>200){
                isFinish = true;
            }

        }
        //TODO 剔除游离元素，方案？
//        medoids = preMedoids;
//        System.out.println("最终分数为："+preSumScore);
        return preSumScore;
    }

    /**
     * 有更优化的解，以更加优化的解选择中心点
     */
    private void chooseNewMedoids(){

        assignNearestPoint();
    }


    /**
     * 初始化medoids，medoid的cluster，cluster的medoid，cluster所属的datapoints，DataPoints所属的cluster
     */
    private void initMedoids(){
        Random random = new Random();
        int n=0;
        //避免中心点选择重复,选择中心点
        Set<DataPoint> dataPointsTmp = new HashSet<DataPoint>();
        while (dataPointsTmp.size()<dimNum){
            int index = random.nextInt(dataPoints.size());
            //利用set去重的特性
            dataPointsTmp.add(dataPoints.get(index));
        }
        for(DataPoint tmp : dataPointsTmp){
            Medoid medoid = new Medoid(tmp);
            //每个簇拥必须设置其中心点，同时每个中心点必须设置其所属的簇拥
            clusters[n].setMedoid(medoid);
            medoid.setCluster(clusters[n]);
            n++;
        }
        assignNearestPoint();

    }

    private void assignNearestPoint(){
        //聚类点清理
        for(Cluster clusterTmp : clusters){
            clusterTmp.getDataPoints().clear();
        }
        //分派离中心的最近点
        for(int i=0;i<dataPoints.size();i++){
            int clusterIndex = 0;
            double relativeScore = Double.MIN_VALUE;
            //2,拆分点属于哪个簇拥
            for(int k=0 ; k<clusters.length;k++){
                double score = dataPoints.get(i).calculatorScore(clusters[k].getMedoid().getDataPoint());
                //score>x去掉游离点
                if(miter % 2 ==0) {//test
                    if ((score > relativeScore) && score > Double.valueOf(limitScore)) {
                        relativeScore = score;
                        clusterIndex = k;
                    }
                }else {
                    if ((score > relativeScore)) {
                        relativeScore = score;
                        clusterIndex = k;
                    }
                }
            }
            //每个点必须设置其所属的簇拥，同时每个簇拥必须设置其所有的点
            if(relativeScore!=Double.MIN_VALUE) {
                DataPoint dp = dataPoints.get(i);
                dp.setCluster(clusters[clusterIndex]);
                clusters[clusterIndex].addDataPoint(dp);
            }

        }
    }


    public CurrentBestSolve getCurrentBestSolve() {
        return currentBestSolve;
    }

    public class CurrentBestSolve{

        private Set<Medoid> medoids = new HashSet<Medoid>();//中心点集合

        private Cluster []clusters = new Cluster[0];

        public void removeIndex(int x){
            clusters[x] = null;
        }

        //主要需要排除空的
        public int clusterSize(){
            int count = 0;
            for(Cluster cluster : clusters){
                if(cluster!=null){
                    count++;
                }
            }
            return count;
        }

        public Set<Medoid> getMedoids() {
            return medoids;
        }

        public void setMedoids(Set<Medoid> medoids) {
            this.medoids = medoids;
        }

        public Cluster[] getClusters() {
            return clusters;
        }

        public void setClusters(Cluster[] clusters) {
            this.clusters = clusters;
        }
    }
}
