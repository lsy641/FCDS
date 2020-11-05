package kmedoids;

import java.util.ArrayList;

public class Medoid {

    private DataPoint dataPoint; // 当前中心点

    private Cluster cluster; //所属类簇

    private double etdDisSum;//Medoid到本类簇中所有的距离之和

    public DataPoint getDataPoint() {
        return dataPoint;
    }

    //在当前簇拥，找出最适合当中心点的点
    public Medoid evaluateBestMedoidForCurrentCluster(){
        calculatorSum();
        double maxScore = etdDisSum;
        for(DataPoint dataPointTmp : cluster.getDataPoints()){
            double tmpScore = dataPointTmp.calculatorSumDataPoint();
            if(tmpScore > maxScore){
                dataPoint = dataPointTmp;
                etdDisSum = tmpScore;
            }
        }
        Medoid medoid = new Medoid(dataPoint);
        medoid.setCluster(cluster);
        return medoid;
    }

    /**
     * 当前中心点到附近节点的值之和
     */
    public double calculatorSum(){
        double sum = 0;
        ArrayList<DataPoint> points = cluster.getDataPoints();
        for(DataPoint dataPointTmp : points){
            double score = dataPointTmp.calculatorScore(dataPoint);
            sum = sum + score;
        }
        etdDisSum = sum;
        return etdDisSum;
    }

    public void setDataPoint(DataPoint dataPoint) {
        this.dataPoint = dataPoint;
    }

    public Medoid(DataPoint point) {
        this.dataPoint = point;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public double getEtdDisSum() {
        return etdDisSum;
    }

    public void setEtdDisSum(double etdDisSum) {
        this.etdDisSum = etdDisSum;
    }
}
