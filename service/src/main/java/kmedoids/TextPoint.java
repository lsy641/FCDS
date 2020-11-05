package kmedoids;

import org.apache.commons.text.similarity.JaroWinklerDistance;


public class TextPoint extends Point {
    private static Cilin cilin = new Cilin();
    private static JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
    public TextPoint(String text,Long businessKey) {
        super(text,businessKey);
    }

    @Override
    public double calculatorScore(Point element2) {
        double score = 0.0;
        float score_bow = 0;
        double score_jw = 0.0;
        double score_em = 0.0;
        String key = "True";
        if(this.JwMap().keySet().contains(element2.getId())){
            score_jw = this.getJW(element2.getId());
        }else {
            score_jw = jaroWinklerDistance.apply(element2.getElement(),this.getElement());
            element2.setJW(this.getId(),score_jw);
            this.setJW(element2.getId(),score_jw);
        }
//            score_jw = this.getJW(element2.getId());
//            score_bow = (double) this.getSenSim(element2.getId());
        if(key.equals("True")){
//            System.out.println("a:"+this.getElement_seg()+"b:"+element2.getElement_seg());
            if(this.SimMap().keySet().contains(element2.getId())){
                //                System.out.println("hit");
                score_bow = this.getSenSim(element2.getId());
            }else {
//                System.out.println("lose");
//                Long pret = System.nanoTime();
                score_bow = cilin.sensim(element2.getElement_seg(),this.getElement_seg());
//                Long now = System.nanoTime();
//                System.out.println("time consume:"+(now-pret));
                element2.setSenSim(this.getId(), score_bow);
                this.setSenSim(element2.getId(), score_bow);
            }

//            System.out.println("score_bow:"+score_bow+"score_jw:"+score_jw);
            score = (double)score_bow*0.3 + score_jw*0.7;
        }else{
            score = score_jw;
        }
//            score_em = (double) 1.0;
        return score;
    }
}
