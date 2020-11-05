package kmedoids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public abstract class Point{

    private String element;

    private String originText;

    private Long businessKey;

    private Set<String> element_seg;

    private int pointID;

    private ArrayList<Double> embedding;

    private HashMap<Integer,Float> senSim = new HashMap<Integer, Float>();

    private HashMap<Integer,Double> jwDistance = new HashMap<Integer, Double>();

    public abstract double calculatorScore(Point element2);

    public Long getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(Long businessKey) {
        this.businessKey = businessKey;
    }

    public Point(String text,Long businessKey){
        element = text;
        this.businessKey = businessKey;
    }

    public void setEmbedding(ArrayList<Double> embedding){
        this.embedding = embedding;

    }
    public void setSenSim(int id, float score){
        senSim.put(id,score);
    }
    public float getSenSim(int id){
        return senSim.get(id);
    }
    public String getElement() {
        return element;
    }
    public String getOriginText() {
        return originText;
    }
    public Set<String> getElement_seg() {
        return element_seg;
    }

    public void setElement(String element) {
        this.element = element;
    }
    public void setElement(Set<String> element) {
        this.element_seg = element;
    }
    public void setOriginText(String s) {
        originText = s;
    }
    public int getId(){return pointID;}
    public void setId(int Id){ pointID=Id;}
    public void setJW(int id, double score){jwDistance.put(id,score);}
    public double getJW(int id){ return jwDistance.get(id);}
    public HashMap<Integer,Double> JwMap(){return jwDistance;}
    public HashMap<Integer,Float> SimMap(){return senSim;}

}
