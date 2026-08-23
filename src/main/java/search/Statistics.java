package search;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Statistics implements Serializable {
    
    private int numSearches;
    private int activeBarrels;
    private long averageResponseTime;
    private List<String> top10Searches; 
    private Map<String, Integer> barrelIndexSizes; 



    public Statistics(int numSearches, int activeBarrels, long averageResponseTime){
        this.numSearches = numSearches;
        this.activeBarrels = activeBarrels;
        this.averageResponseTime = averageResponseTime;
    }

    public int getNumSearches() {
        return numSearches;
    }

    public void setNumSearches(int numSearches) {
        this.numSearches = numSearches;
    }

    public int getActiveBarrels() {
        return activeBarrels;
    }

    public void setActiveBarrels(int activeBarrels) {
        this.activeBarrels = activeBarrels;
    }

    public long getAverageResponseTime() {
        return averageResponseTime;
    }
     void setAverageResponseTime(long averageResponseTime){
        this.averageResponseTime = averageResponseTime;
     }

     public List<String> getTop10Searches() {
        return top10Searches;
    }

    public void setTop10Searches(List<String> top10Searches) {
        this.top10Searches = top10Searches;
    }

    public Map<String, Integer> getBarrelIndexSizes() {
        return barrelIndexSizes;
    }

    public void setBarrelIndexSizes(Map<String, Integer> barrelIndexSizes) {
        this.barrelIndexSizes = barrelIndexSizes;
    }

    public String toString(){
        return "Statistics [numSearches=" + numSearches 
                + ", activeBarrels=" + activeBarrels 
                + ", averageResponseTime=" + averageResponseTime 
                + ", top10Searches=" + top10Searches
                + ", barrelIndexSizes=" + barrelIndexSizes + "]";
    }
    
}
