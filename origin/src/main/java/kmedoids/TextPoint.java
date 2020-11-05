package kmedoids;


import org.apache.commons.text.similarity.JaroWinklerDistance;


public class TextPoint extends Point {

    public TextPoint(String text,Long businessKey) {
        super(text,businessKey);
    }
    @Override
    public double calculatorScore(Point element2) {
        double score = 0.0;
        JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
        score = jaroWinklerDistance.apply(this.getElement(), element2.getElement());
        return score;
    }
}
