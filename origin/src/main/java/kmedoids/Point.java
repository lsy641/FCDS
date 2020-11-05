package kmedoids;

public abstract class Point{

    private String element;

    private Long businessKey;

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

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }
}
