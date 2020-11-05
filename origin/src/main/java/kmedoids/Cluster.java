package kmedoids;

import java.util.ArrayList;


public class Cluster{
    /**
     * 聚合的中心点
     */
    private Medoid medoid;

    /**
     * 该簇拥聚合下的所有点
     */
    private ArrayList<DataPoint> dataPoints;

    public double getAvgScore(){
        double score = 0.0;
        for(DataPoint dataPoint : dataPoints){
            score = score + dataPoint.calculatorScore(medoid.getDataPoint());
        }
        if(dataPoints.size()>1){
            return (score - 1)/(dataPoints.size() - 1);
        }
        return 0;
    }

    public Cluster(){
        dataPoints = new ArrayList<DataPoint>();
    }

    public String getKeyStringList(){
        String keys = "";
        for(DataPoint dataPoint : dataPoints){
            keys = keys + dataPoint.getPoint().getBusinessKey() + ",";
        }
        if(dataPoints.size() > 0){
            return keys.substring(0,keys.lastIndexOf(",") );
        }
        return keys;
    }

    public void addDataPoint(DataPoint dp) { // called from CAInstance
        dp.setCluster(this);// 标注该类簇属于某点,计算欧式距离
        this.dataPoints.add(dp);
    }

    public void addAllPoint(ArrayList<DataPoint> points){
        dataPoints.clear();
        dataPoints.addAll(points);
    }

    public void removeDataPoint(DataPoint dp) {
        this.dataPoints.remove(dp);
    }

    public int getNumDataPoints() {
        return this.dataPoints.size();
    }


    public Medoid getMedoid() {
        return medoid;
    }

    public void setMedoid(Medoid medoid) {
        this.medoid = medoid;
    }

    public ArrayList<DataPoint> getDataPoints() {
        return dataPoints;
    }
}
