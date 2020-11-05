package kmedoids;

public class FeedbackDO {
    private String desc;
    private Long Id;
    public FeedbackDO(String desc, Long Id){
        this.desc = desc;
        this.Id = Id;
    }
    public String getFeedDesc(){return desc;}
    public Long getId(){return Id;}
    public int getSourceFrom(){return 3;}
}
