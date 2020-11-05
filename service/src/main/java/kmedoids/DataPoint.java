package kmedoids;

import java.util.ArrayList;


public class DataPoint {
    private Point point;//实际做聚类的point

    private Point showPoint;//显示的point

    private Cluster cluster; //类簇

    //计算与所选点之间的距离
    public double calculatorScore(DataPoint dataPoint){
        return point.calculatorScore(dataPoint.getPoint());
    }

    //计算与所选点之间的距离
    public double calculatorScore(Point point1){
        return point.calculatorScore(point1);
    }

    //未选定的中心点计算其当做中心点时距离其他地方的位置之和
    public double calculatorSumDataPoint() {
        double sum = 0;
        ArrayList<DataPoint> points = cluster.getDataPoints();
        for (DataPoint dataPointTmp : points) {
            double score = dataPointTmp.calculatorScore(this);
            sum = sum + score;
        }
        return sum;
    }

    public DataPoint(Point point, Point showPoint, int pointId){
        this.point = point;
        this.showPoint = showPoint;
        this.point.setId(pointId);
    }


    public Point getShowPoint() {
        return showPoint;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public Point getPoint() {
        return point;
    }

    public int getId(){return point.getId();}

    public void setId(int Id){ point.setId(Id);}


}
